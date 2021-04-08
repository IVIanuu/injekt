package com.ivianuu.injekt.ktor

import com.ivianuu.injekt.Given
import io.ktor.application.Application
import io.ktor.util.AttributeKey
import com.ivianuu.injekt.scope.AppGivenScope
import com.ivianuu.injekt.scope.GivenScopeElementBinding
import io.ktor.application.ApplicationStopped

val Application.appGivenScope: AppGivenScope
    get() = attributes.getOrNull(AppGivenScopeKey)
        ?: error("No app given scope found. Did you forget to call Application.initializeAppGivenScope?")

fun Application.initializeAppGivenScope(
    @Given scopeFactory: (@Given @GivenScopeElementBinding<AppGivenScope> Application) -> AppGivenScope
) {
    val scope = scopeFactory(this)
    attributes.put(AppGivenScopeKey, scope)
    environment.monitor.subscribe(ApplicationStopped) {
        scope.dispose()
    }
}

private val AppGivenScopeKey = AttributeKey<AppGivenScope>("AppGivenScope")
