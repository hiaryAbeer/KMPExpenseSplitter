package com.abeerapps.kmpexpensesplitter.core.domain.model

import kotlin.jvm.JvmInline

/**
 * An ISO 4217 alphabetic currency code (e.g. `USD`, `EUR`, `JOD`).
 *
 * Modelled as a value class so it costs nothing at runtime but can't be confused
 * with any other `String`. Instances are always valid: use [of] for untrusted
 * input (user input, persisted data) and handle the `null` result.
 */
@JvmInline
value class CurrencyCode private constructor(val value: String) {

    override fun toString(): String = value

    companion object {
        private val ISO_4217 = Regex("^[A-Z]{3}$")

        /**
         * Parses [raw] into a [CurrencyCode].
         *
         * Input is trimmed and upper-cased first, so `" usd "` is accepted.
         * @return the code, or `null` if [raw] is not three ASCII letters.
         */
        fun of(raw: String): CurrencyCode? {
            val normalized = raw.trim().uppercase()
            return if (ISO_4217.matches(normalized)) CurrencyCode(normalized) else null
        }
    }
}
