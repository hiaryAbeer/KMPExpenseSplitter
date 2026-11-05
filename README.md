# Expense Splitter (Kotlin Multiplatform)

Split shared expenses between friends, on Android and iOS, from one Kotlin codebase.
Business logic, data, presentation (ViewModels) **and UI** (Compose Multiplatform) are shared;
each platform keeps a thin native entry point.

## Architecture

Clean Architecture with feature modules. Dependencies only point inward.

```
androidApp (Android entry)      iosApp (Swift entry, Xcode)
        \                          /
         └──────── :shared ───────┘   app shell: DI graph, navigation, start gate, iOS framework
                 /     |      \
 :feature:onboarding   |   :core:data ──► DataStore (Room/Ktor later)
        |      \       |      |
        |   :core:designsystem │
        └──────────┬───────────┘
              :core:domain       entities, repository contracts, use cases (no frameworks)
```

| Module | Responsibility | May depend on |
|---|---|---|
| `:core:domain` | `UserProfile`, `CurrencyCode`, `SupportedCurrencies`, `UserProfileRepository` (interface), use cases, `FeatureFlags` | coroutines only |
| `:core:data` | Repository implementations, persistence, platform file paths (`expect/actual`), Koin `dataModule` | domain |
| `:core:designsystem` | Theme from the Figma tokens (colors, type, 16dp shapes, spacing), shared icons, components (`PrimaryButton`, `AppTextField`, `AppCard`, `EmptyState`, `AppNavigationBar`), `ObserveAsEvents` | Compose |
| `:feature:*` | One user-facing feature: screen + ViewModel + Koin module (`onboarding`, `registration`, `home`) | domain, designsystem (**never** data or other features) |
| `:shared` | Wires everything: `initKoin`, `AppNavHost`, `AppViewModel` (start destination), `Shared.framework` | everything |

**Presentation pattern (UDF/MVI-lite)**: each screen has `UiState` (`StateFlow`), `Action`s from the UI,
and one-shot `Event`s (navigation) delivered through a `Channel`. Screens are split into a stateful
`XxxScreen` (gets its ViewModel from Koin) and a stateless, previewable `XxxContent`.

**Navigation**: type-safe Navigation Compose routes (`AppDestination`) live in `:shared` only.
Features expose callbacks like `onNavigateToRegistration`, so they don't depend on each other.

**DI**: Koin. Each module contributes a `Module`; `:shared/di/Koin.kt` assembles them.
`KoinGraphTest` checks the whole graph in CI, so a missing binding fails the build.

**Build logic**: `build-logic/convention` holds the convention plugins
`expensesplitter.kmp.library` and `expensesplitter.kmp.compose`. A new module needs about 10 lines
of Gradle. Versions live in `gradle/libs.versions.toml`.

## Current flow: onboarding gate

1. On launch, `AppViewModel` runs `ObserveOnboardingStatusUseCase` once.
2. No stored profile (name + default currency), or invalid data → **Onboarding**; otherwise → **Home**.
3. Onboarding has two actions:
   - **Register** → **Set up your profile** (`:feature:registration`): name + default currency.
     The currency field opens a bottom sheet listing every currency in use worldwide (154),
     with **Jordanian Dinar (JOD) preselected**. Search matches the currency **name or ISO code**
     ("dinar", "jod"); symbols and country names are not searched. Accent-insensitive.
     **Save & Continue** is enabled once the name is valid (1–50 chars after trimming).
     It saves the profile and opens Home, clearing onboarding from the back stack.
     Half-filled input survives process death (`SavedStateHandle`).
   - **Skip** → Home. It is hidden for now via `StaticFeatureFlags.isOnboardingSkipEnabled = false`
     (`shared/.../config/AppConfig.kt`). The ViewModel also ignores Skip while the flag is off.

## Home

After registration the app opens the main shell: bottom navigation with **Home**, **Groups** and
**Settings** (Groups/Settings are placeholders). Home shows the greeting with the registered name,
"You are owed" / "You owe" totals in the default currency, group search, and **Recent Groups**.

Groups are not persisted yet: `InMemoryGroupRepository` (in `:core:data`) starts empty, so Home
shows the generic `EmptyState` ("No groups yet", no button: the "+" button already creates groups).
When group storage is built, replace that one binding in `dataModule`; the group cards from the
design are already implemented (see the `HomeWithGroupsPreview` preview).

Money is stored as integer hundredths (`Money.minorUnits`) and always shown with two decimals,
as in the design. Totals only include groups in the user's default currency until currency
conversion exists.

## Build variants

| | Android | iOS |
|---|---|---|
| Debug | `assembleDebug` · app id `…kmpexpensesplitter.debug` · "Expense Splitter (Debug)" · verbose logs | Xcode *Debug* config · `isDebug = true` |
| Release | `assembleRelease` · R8 minify + resource shrinking · logs ≥ Warn only | Xcode *Release* config · `isDebug = false` |

The flag reaches shared code through `AppConfig(isDebug)`: from `BuildConfig.DEBUG` on Android and the
Swift `#if DEBUG` on iOS.

**Release signing (Android)**: create a git-ignored `keystore.properties` at the repo root with
`storeFile`, `storePassword`, `keyAlias` and `keyPassword`. On CI, set
`EXPENSE_SPLITTER_STOREFILE` and the matching env vars instead. Without these, the build produces
an unsigned APK.

## Security

- **No backup/transfer of user data**: `allowBackup=false` + `data_extraction_rules.xml`.
- **HTTPS only**: `network_security_config.xml` disables cleartext and trusts system CAs only.
- **Storage**: app-private `filesDir` on Android; `Library/Application Support` on iOS (not exposed via
  the Files app, protected by iOS Data Protection). Profile data is name + currency only.
  Secrets such as auth tokens must go to Keystore/Keychain, not DataStore, when sync is added.
- **Validation at the boundaries**: `CurrencyCode.of()` validates ISO 4217 input. Persisted data is
  re-validated on read, and corrupt data falls back to onboarding instead of crashing.
- **No PII in logs**. Release builds log warnings/errors only, and R8 strips `Log.v/d`.

## Testing

Tests are written once in `commonTest` and run on the JVM and the iOS simulator
(kotlin-test, coroutines-test, Turbine, and hand-written fakes in place of mocks).

```bash
./gradlew testAndroidHostTest         # all modules, JVM (fast)
./gradlew iosSimulatorArm64Test       # all modules, iOS simulator
./gradlew :androidApp:assembleDebug :androidApp:assembleRelease
```

| Test | Covers |
|---|---|
| `CurrencyCodeTest` | ISO code parsing/normalization |
| `ObserveOnboardingStatusUseCaseTest` | onboarding rule, de-duplication |
| `DataStoreUserProfileRepositoryTest` | persistence, invalid/corrupt data fallback |
| `OnboardingViewModelTest` | Skip flag, navigation events, event buffering |
| `RegisterUserUseCaseTest` | name rules, trimming, storage failure |
| `SupportedCurrenciesTest` | catalog integrity, JOD default |
| `SearchCurrenciesUseCaseTest` | name/code search, countries & symbols excluded, ranking, accents |
| `RegistrationViewModelTest` | form state, currency menu, save success/failure, state restoration |
| `CalculateBalanceSummaryUseCaseTest` | owed/owe totals, currency isolation |
| `HomeViewModelTest` | greeting, zero balances, empty state, group search |
| `MoneyFormatTest` | `97.75 JOD` / `+85.25 JOD` / `-45.00 JOD` formatting |
| `AppViewModelTest` | start destination, resolved once |
| `KoinGraphTest` | complete DI graph |

## iOS integration

- `Shared.framework` (static) is built by the Xcode run script
  `./gradlew :shared:embedAndSignAppleFrameworkForXcode`; open `iosApp/iosApp.xcodeproj`.
- Swift calls `MainViewControllerKt.startKoin(isDebug:)` in `iOSApp.init()`, then hosts
  `MainViewControllerKt.MainViewController()` inside SwiftUI (`ContentView.swift`).
- Native SwiftUI screens can be added next to the Compose UI when a flow needs platform-specific UX.
- For distribution as a package (SPM/CocoaPods) to another iOS app, add an XCFramework task or the
  `cocoapods` plugin to `:shared`. The direct framework integration is enough for this app.

## Currency data

`core/domain/.../model/CurrencyData.kt` is **generated** from Unicode CLDR: every currency that is
legal tender today, with English names, symbols (the issuing country's local symbol when English
has none, e.g. `د.أ.` for JOD) and the countries using it.

Product decisions applied on top of CLDR (constants at the top of the script, enforced by
`SupportedCurrenciesTest`):

- Israel is not listed; the shekel (ILS) is shown as **New Shekel** under **Palestine**.
- Palestine is always named "Palestine".
- Jordanian Dinar is listed under Jordan only.

To refresh it after a CLDR update:

```bash
python3 -m venv /tmp/venv && /tmp/venv/bin/pip install babel==2.17.0
/tmp/venv/bin/python scripts/generate_currencies.py
```

## Design notes

- **Dark theme** follows the system setting. Dark colors come from the Figma dark Home design:
  background `#111827`, cards/bottom bar `#1F2937`, borders `#374151`, text `#F9FAFB`/`#9CA3AF`,
  primary `#14B8A6`, positive `#34D399`, negative `#F87171`. Icons are tinted from the theme at
  runtime (their SVG colors are placeholders), so they switch automatically. Multi-color art uses a
  `drawable-dark/` variant (see the onboarding illustration).
- **Spacing rule:** every screen uses a 16dp left/right margin (`spacing.screenHorizontal`) and the
  space between views never exceeds 16dp (`spacing.md`, the largest step in the scale). This
  overrides the larger gaps in the Figma exports.
- Values come from the Figma file "Expense splliter" (page "onbording") and live in `:core:designsystem/theme`.
- The design uses **Inter**; the app currently uses the platform font (Roboto / SF Pro). To match exactly,
  add the Inter `.ttf` files to `core/designsystem/src/commonMain/composeResources/font/` and set
  `fontFamily` in `Type.kt`.
- Compose resources in library modules need `androidResources { enable = true }` on Android;
  the `expensesplitter.kmp.compose` convention plugin turns this on for every UI module.

## Adding a feature

1. Create `feature/<name>/build.gradle.kts` with `alias(libs.plugins.expensesplitter.kmpCompose)`.
2. `include(":feature:<name>")` in `settings.gradle.kts`.
3. Add `XxxContract.kt`, `XxxViewModel.kt`, `XxxScreen.kt` and `di/XxxModule.kt`, plus tests in `commonTest`.
4. Register the Koin module in `allModules()` and the route in `AppNavHost`.
