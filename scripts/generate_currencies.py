#!/usr/bin/env python3
"""
Generates the currency catalog used by :core:domain from Unicode CLDR data.

Every currency that is legal tender today in at least one country/territory is
included, with its English name, English symbol and the countries using it
(used for search, e.g. "Jordan" -> Jordanian Dinar).

Usage (Babel is only needed to run this script, not by the app):

    python3 -m venv /tmp/venv && /tmp/venv/bin/pip install babel==2.17.0
    /tmp/venv/bin/python scripts/generate_currencies.py

Output: core/domain/src/commonMain/kotlin/.../model/CurrencyData.kt
"""
import unicodedata
from datetime import date
from pathlib import Path

import babel
from babel import Locale, UnknownLocaleError
from babel.languages import get_official_languages
from babel.numbers import get_currency_symbol, get_territory_currencies

OUTPUT = Path(__file__).resolve().parent.parent / (
    "core/domain/src/commonMain/kotlin/com/abeerapps/kmpexpensesplitter/"
    "core/domain/model/CurrencyData.kt"
)

# CLDR pseudo-territories that are not countries.
NON_COUNTRIES = {"EU", "EZ", "UN", "QO", "XA", "XB", "ZZ"}

# --- Product decisions applied on top of CLDR (keep in sync with SupportedCurrenciesTest) ---

# Territories the app does not list.
EXCLUDED_TERRITORIES = {"IL"}

# Display names that replace CLDR's English territory names.
TERRITORY_NAME_OVERRIDES = {"PS": "Palestine"}

# Currencies that must not be listed under a given territory.
CURRENCY_TERRITORY_EXCLUSIONS = {"JOD": {"PS"}}

# Display names that replace CLDR's English currency names.
CURRENCY_NAME_OVERRIDES = {"ILS": "New Shekel"}


def kotlin_string(value: str) -> str:
    return '"' + value.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def strip_invisible(value: str) -> str:
    """Removes format characters (bidi marks, zero-width spaces) that CLDR embeds in symbols."""
    return "".join(ch for ch in value if unicodedata.category(ch) != "Cf").strip()


def display_symbol(code: str, territories: list[str]) -> str:
    """
    English symbol if it is distinct from the ISO code (e.g. "$", "CA$", "€").
    Otherwise the symbol used in the issuing country's own locale (e.g. KZT -> "₸",
    JOD -> "د.أ."). Empty when no distinct symbol exists, so the UI does not
    show the code twice.
    """
    symbol = strip_invisible(get_currency_symbol(code, "en"))
    if symbol and symbol != code:
        return symbol
    for territory in territories:
        for language in get_official_languages(territory):
            try:
                local = strip_invisible(get_currency_symbol(code, f"{language}_{territory}"))
            except (UnknownLocaleError, ValueError):
                continue
            if local and local != code and len(local) <= 5:
                return local
    return ""


def main() -> None:
    en = Locale("en")
    today = date.today()
    countries_by_code: dict[str, list[str]] = {}
    territories_by_code: dict[str, list[str]] = {}

    for territory, country_name in en.territories.items():
        if len(territory) != 2 or not territory.isalpha():
            continue
        if territory in NON_COUNTRIES or territory in EXCLUDED_TERRITORIES:
            continue
        country_name = TERRITORY_NAME_OVERRIDES.get(territory, country_name)
        for code in get_territory_currencies(territory, start_date=today, tender=True):
            if territory in CURRENCY_TERRITORY_EXCLUSIONS.get(code, set()):
                continue
            countries_by_code.setdefault(code, []).append(country_name)
            territories_by_code.setdefault(code, []).append(territory)

    rows = []
    for code, countries in countries_by_code.items():
        name = CURRENCY_NAME_OVERRIDES.get(code) or en.currencies.get(code)
        if not name:
            continue
        symbol = display_symbol(code, territories_by_code[code])
        rows.append((name, code, symbol, sorted(set(countries))))
    rows.sort(key=lambda row: row[0].lower())

    lines = [
        "// GENERATED FILE - DO NOT EDIT.",
        f"// Source: Unicode CLDR via Babel {babel.__version__}, generated {today.isoformat()}.",
        "// Regenerate with: scripts/generate_currencies.py",
        "package com.abeerapps.kmpexpensesplitter.core.domain.model",
        "",
        "/** Currencies in legal tender today, sorted by English name. */",
        "internal val CURRENCY_DATA: List<Currency> = listOf(",
    ]
    for name, code, symbol, countries in rows:
        country_list = ", ".join(kotlin_string(c) for c in countries)
        lines.append(
            f"    currency({kotlin_string(code)}, {kotlin_string(symbol)}, "
            f"{kotlin_string(name)}, listOf({country_list})),"
        )
    lines += [
        ")",
        "",
        "private fun currency(code: String, symbol: String, name: String, countries: List<String>) =",
        "    Currency(",
        '        code = requireNotNull(CurrencyCode.of(code)) { "Invalid ISO code: $code" },',
        "        symbol = symbol,",
        "        name = name,",
        "        countries = countries,",
        "    )",
        "",
    ]
    OUTPUT.write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {len(rows)} currencies to {OUTPUT}")


if __name__ == "__main__":
    main()
