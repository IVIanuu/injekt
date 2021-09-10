package com.ivianuu.injekt.scope

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.Tag
import com.ivianuu.injekt.common.TypeKey

@Tag private annotation class Parent

@Tag annotation class Framework

@OptIn(InternalScopeApi::class)
@Provide
fun <S : Scope> frameworkScope(
  key: TypeKey<S>,
  parent: @Parent Scope? = null,
  elementsFactory: (
    @Provide S,
    @Provide @Parent Scope?
  ) -> Set<NamedProvidedElement<S>> = { _, _ -> emptySet() },
  observersFactory: (
    @Provide S,
    @Provide @Parent Scope?
  ) -> Set<NamedScopeObserver<S>> = { _, _ -> emptySet() }
): @Framework S {
  val elements = parent?.elements?.toMap(hashMapOf()) ?: hashMapOf()

  val scope = ScopeImpl(elements)
  @Suppress("UNCHECKED_CAST")
  scope as S

  if (parent != null)
    ParentScopeDisposable(scope).bind(parent)

  elementsFactory(scope, scope).forEach {
    elements[it.key.value] = it.factory
  }

  val observers = observersFactory(scope, scope)
  for (observer in observers)
    scope.setScopedValue(observer, observer)

  return scope
}

typealias NamedScopeObserver<N> = ScopeObserver

typealias NamedProvidedElement<N> = ProvidedElement<*>

/**
 * Registers the declaration in the scope [S]
 *
 * Example:
 * ```
 * @Provide
 * @ScopeElement<AppScope>
 * class MyAppDeps(val api: Api, val database: Database)
 *
 * fun runApp(@Inject appScope: AppScope) {
 *   val deps = appScope.element<MyAppDeps>()
 * }
 * ```
 */
@Tag annotation class ScopeElement<S : Scope> {
  companion object {
    @Provide class Module<@com.ivianuu.injekt.Spread T : @ScopeElement<S> U, U : Any, S : Scope> {
      @Provide inline fun providedElement(
        noinline factory: () -> T,
        key: TypeKey<U>
      ): NamedProvidedElement<S> = provideElement(key = key, factory = factory)

      @Provide inline fun elementAccessor(scope: S, key: TypeKey<U>): U = requireElement(key = key)
    }
  }
}

internal class ParentScopeDisposable(scope: DisposableScope) : Disposable {
  private var scope: DisposableScope? = scope

  init {
    // do not leak a reference to the child scope
    onDispose(scope) { this.scope = null }
  }

  override fun dispose() {
    scope?.dispose()
    scope = null
  }
}
