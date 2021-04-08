package com.ivianuu.injekt.ktor

import com.ivianuu.injekt.Given
import io.ktor.application.Application
import io.ktor.util.AttributeKey
import com.ivianuu.injekt.scope.AppGivenScope
import com.ivianuu.injekt.scope.GivenScopeElementBinding
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationStopped
import io.ktor.routing.Routing

val Application.appGivenScope: AppGivenScope
    get() = attributes.getOrNull(AppGivenScopeKey)
        ?: error("No app given scope found. Did you forget to call Application.initializeAppGivenScope?")

val Routing.appGivenScope: AppGivenScope
    get() = application.appGivenScope

val ApplicationCall.appGivenScope: AppGivenScope
    get() = application.appGivenScope

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
