/*
 * Copyright 2020 Manuel Wrage
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

package com.ivianuu.injekt

/**
 * Makes the annotated class injectable and generates a single binding for it
 * The class will be created once per [Component]
 *
 * @see Factory
 * @see Qualifier
 * @see ScopeMarker
 * @see InjektConstructor
 * @see ComponentBuilder.single
 */
@BehaviorMarker(SingleBehavior::class)
@Target(AnnotationTarget.CLASS)
annotation class Single

object SingleBehavior : Behavior.Element {
    override fun <T> apply(provider: BindingProvider<T>): BindingProvider<T> =
        SingleProvider(provider)
}

inline fun <reified T> ComponentBuilder.single(
    qualifier: Qualifier = Qualifier.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    eager: Boolean = false,
    noinline provider: BindingProvider<T>
): BindingContext<T> = single(
    key = keyOf(qualifier = qualifier),
    duplicateStrategy = duplicateStrategy,
    eager = eager,
    provider = provider
)

/**
 * Adds a binding for [key] which will be cached after the first request
 *
 * We get the same instance in the following example
 *
 * ´´´
 * val component = Component {
 *     single { Database(get()) }
 * }
 *
 * val db1 = component.get<Database>()
 * val db2 = component.get<Database>()
 * assertEquals(db1, db2) // true
 *
 * ´´´
 * @param key the key to retrieve the instance
 * @param duplicateStrategy the strategy for handling overrides
 * @param eager whether the instance should be created when the [Component] get's created
 * @param provider the definitions which creates instances
 *
 * @see ComponentBuilder.bind
 */
fun <T> ComponentBuilder.single(
    key: Key<T>,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    eager: Boolean = false,
    provider: BindingProvider<T>
): BindingContext<T> = bind(
    Binding(
        key = key,
        behavior = (if (eager) EagerBehavior else Behavior.None) + SingleBehavior + BoundBehavior(),
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )
)

private class SingleProvider<T>(
    private val provider: BindingProvider<T>
) : (Component, Parameters) -> T, ComponentInitObserver {
    private var value: Any? = this

    override fun onInit(component: Component) {
        (provider as? ComponentInitObserver)?.onInit(component)
    }

    override fun invoke(component: Component, parameters: Parameters): T {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    this.value = provider(component, parameters)
                    value = this.value
                }
            }
        }

        return value as T
    }
}
