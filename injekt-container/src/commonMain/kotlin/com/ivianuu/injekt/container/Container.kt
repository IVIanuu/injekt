/*
 * Copyright 2021 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.container

import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.*
import com.ivianuu.injekt.scope.*

/**
 * A container connects the DI graph with the outer world by providing access to elements
 */
interface Container<N> {
  /**
   * Returns the element for [key] or throws if it doesn't exist
   */
  fun <T> element(@Inject key: TypeKey<T>): T

  companion object {
    @Tag private annotation class Parent

    @Provide fun <N> default(
      nameKey: TypeKey<N>,
      parent: @Parent Container<*>? = null,
      elementsFactory: (
        @Provide Container<N>,
        @Provide NamedScope<N>,
        @Provide @Parent Container<*>?
      ) -> Set<ContainerElementPair<N>> = { _, _, _ -> emptySet() },
      observersFactory: (
        @Provide Container<N>,
        @Provide NamedScope<N>,
        @Provide @Parent Container<*>?
      ) -> Set<ContainerObserver<N>> = { _, _, _ -> emptySet() }
    ): Container<N> {
      val scope = DisposableScope()
      val container = ContainerImpl(nameKey, parent as? ContainerImpl<*>)

      if (parent is ContainerImpl<*>) {
        val parentScope = parent.elements.values.first().invoke() as Scope
        ParentContainerDisposable(scope).bind(parentScope)
      }

      val elements = elementsFactory(container, scope, container)

      container.elements = LinkedHashMap<String, () -> Any>(elements.size + 1).apply {
        this[typeKeyOf<NamedScope<N>>().value] = { scope }

        for (elementPair in elements)
          this[elementPair.key.value] = elementPair.factory
      }

      val observers = observersFactory(container, scope, container)

      for (observer in observers)
        invokeOnDispose(scope) { observer.onDispose() }

      for (observer in observers)
        observer.onInit()

      return container
    }
  }
}

/**
 * Disposes the scope bound to this container
 */
fun <N> Container<N>.dispose(@Inject nameKey: TypeKey<N>) {
  (element<NamedScope<N>>() as DisposableScope).dispose()
}

/**
 * Lifecycle observer for [Container] of [N]
 */
interface ContainerObserver<N> {
  /**
   * Will be called when the container gets initialized
   */
  fun onInit() {
  }

  /**
   * Will be called when the container gets disposed
   */
  fun onDispose() {
  }
}

typealias NamedScope<N> = Scope

class ContainerElementPair<N>(val key: TypeKey<*>, val factory: () -> Any)

/**
 * Registers the declaration in the [Container] for [N]
 *
 * Example:
 * ```
 * @Provide
 * @ContainerElement<AppContainer>
 * class MyAppDeps(val api: Api, val database: Database)
 *
 * fun runApp(@Inject appContainer: AppContainer) {
 *   val deps = appContainer.element<MyAppDeps>()
 * }
 * ```
 */
@Tag annotation class ContainerElement<N> {
  companion object {
    @Provide class Module<@Spread T : @ContainerElement<N> U, U : Any, N> {
      @Provide inline fun elementPair(
        noinline factory: () -> T,
        key: TypeKey<U>
      ): ContainerElementPair<N> = ContainerElementPair(key, factory)

      @Provide inline fun elementAccessor(container: Container<N>, key: TypeKey<U>): U =
        container.element(key)
    }
  }
}

private class ContainerImpl<N>(
  private val nameKey: TypeKey<N>,
  private val parent: ContainerImpl<*>?
) : Container<N> {
  lateinit var elements: Map<String, () -> Any>

  override fun <T> element(@Inject key: TypeKey<T>): T =
    elementOrNull() ?: error("No element for ${key.value} in container named ${nameKey.value}")

  @Suppress("UNCHECKED_CAST")
  private fun <T> elementOrNull(@Inject key: TypeKey<T>): T? =
    elements[key.value]?.invoke() as? T ?: parent?.elementOrNull(key)
}

private class ParentContainerDisposable(scope: DisposableScope) : Disposable {
  private var scope: DisposableScope? = scope

  init {
    // do not leak a reference to the child scope
    invokeOnDispose(scope) { this.scope = null }
  }

  override fun dispose() {
    scope?.dispose()
    scope = null
  }
}
