package com.abeerapps.kmpexpensesplitter.di

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.abeerapps.kmpexpensesplitter.config.AppConfig
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test

/**
 * Fails the build if any Koin definition has a constructor dependency that is
 * not provided, catching DI wiring mistakes before the app is launched.
 */
@OptIn(KoinExperimentalAPI::class)
class KoinGraphTest {

    @Test
    fun `dependency graph is complete`() {
        module { includes(allModules(AppConfig(isDebug = true, versionName = "1.0.0"))) }.verify(
            // Context/SavedStateHandle come from the platform at runtime. Boolean and
            // String are AppConfig's constructor args; verify() inspects constructors
            // even for `single { instance }` bindings.
            extraTypes = listOf(Context::class, SavedStateHandle::class, Boolean::class, String::class),
        )
    }
}
