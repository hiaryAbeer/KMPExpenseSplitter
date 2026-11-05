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
| `:core:domain` | `UserProfile`, `CurrencyCode`, `UserProfileRepository` (interface), use cases, `FeatureFlags` | coroutines only |
| `:core:data` | Repository implementations, persistence, platform file paths (`expect/actual`), Koin `dataModule` | domain |
| `:core:designsystem` | Theme (colors, spacing), shared components, `ObserveAsEvents` | Compose |
| `:feature:*` | One user-facing feature: screen + ViewModel + Koin module | domain, designsystem (**never** data or other features) |
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
   - **Register** → Registration (placeholder, next feature).
   - **Skip** → Home. It is hidden for now via `StaticFeatureFlags.isOnboardingSkipEnabled = false`
     (`shared/.../config/AppConfig.kt`). The ViewModel also ignores Skip while the flag is off.

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

## Adding a feature

1. Create `feature/<name>/build.gradle.kts` with `alias(libs.plugins.expensesplitter.kmpCompose)`.
2. `include(":feature:<name>")` in `settings.gradle.kts`.
3. Add `XxxContract.kt`, `XxxViewModel.kt`, `XxxScreen.kt` and `di/XxxModule.kt`, plus tests in `commonTest`.
4. Register the Koin module in `allModules()` and the route in `AppNavHost`.
