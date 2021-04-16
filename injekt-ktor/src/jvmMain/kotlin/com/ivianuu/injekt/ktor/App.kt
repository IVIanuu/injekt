package com.ivianuu.injekt.ktor

import com.ivianuu.injekt.*
import com.ivianuu.injekt.scope.*
import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.util.*

val Application.appGivenScope: AppGivenScope
    get() = attributes.getOrNull(AppGivenScopeKey)
        ?: error("No app given scope found. Did you forget to call Application.initializeAppGivenScope?")

val Routing.appGivenScope: AppGivenScope
    get() = application.appGivenScope

val ApplicationCall.appGivenScope: AppGivenScope
    get() = application.appGivenScope

inline fun Application.initializeAppGivenScope(
    @Given scopeFactory: (@Given @InstallElement<AppGivenScope> Application) -> AppGivenScope
) {
    val scope = scopeFactory(this)
    registerAppGivenScope(scope)
}

@PublishedApi
internal fun Application.registerAppGivenScope(scope: AppGivenScope) {
    attributes.put(AppGivenScopeKey, scope)
    environment.monitor.subscribe(ApplicationStopped) {
        scope.dispose()
    }
}

private val AppGivenScopeKey = AttributeKey<AppGivenScope>("AppGivenScope")
