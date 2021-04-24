package com.ivianuu.injekt.coroutines

import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.*
import com.ivianuu.injekt.scope.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

/**
 * A [CoroutineScope] which is bound to the lifecycle of the [GivenScope] S
 *
 * [CoroutineContext] of the scope can be specified with a given [GivenCoroutineContext]<S> and
 * defaults to [DefaultDispatcher]
 */
typealias GivenCoroutineScope<S> = CoroutineScope

@Given
fun <S : GivenScope> givenCoroutineScopeElement(
    @Given scope: S,
    @Given context: GivenCoroutineContext<S>,
    @Given typeKey: TypeKey<GivenCoroutineScope<S>>
): @InstallElement<S> GivenCoroutineScope<S> = scope.getOrCreateScopedValue(typeKey) {
    object : CoroutineScope, GivenScopeDisposable {
        override val coroutineContext: CoroutineContext = context + SupervisorJob()
        override fun dispose() {
            coroutineContext.cancel()
        }
    }
}

/**
 * Returns the [CoroutineScope] bound to this scope
 */
val GivenScope.coroutineScope: CoroutineScope get() = element()

@Given
inline fun <S : GivenScope> coroutineScopeElement(
    @Given scope: GivenCoroutineScope<S>
): @InstallElement<S> CoroutineScope = scope

/**
 * [CoroutineContext] of a [GivenCoroutineScope]
 */
typealias GivenCoroutineContext<S> = CoroutineContext

@Given
inline fun <S : GivenScope> givenCoroutineContext(
    @Given dispatcher: DefaultDispatcher
): GivenCoroutineContext<S> = dispatcher
