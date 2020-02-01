/*
 * Copyright 2019 Manuel Wrage
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
 * Parameters can be used to inject dynamic data like id's into instances
 *
 * In the following example the presenter depends on a specific user id
 * We can declare the definition as follows:
 *
 * ´´´
 * single { (id: String) ->
 *     MyPresenter(api = get(), id = id)
 * }
 * ´´´
 *
 * We can then inject the presenter as follows:
 *
 * ´´´
 * class MyView : UiView() {
 *
 *     override onAttach() {
 *         val presenter = component.get<MyPresenter> {
 *             parametersOf("user_id")
 *         }
 *
 *         // use presenter
 *     }
 *
 * }
 * ´´´
 *
 */
// todo ir
/* inline */ class Parameters(private val values: Array<Any?>) {

    /**
     * The count of parameters
     */
    val size: Int get() = values.size

    /**
     * Retrieve the parameter at the [index]
     *
     * @param index the index of the parameter
     */
    operator fun <T> get(index: Int): T = values[index] as T

    /**
     * Retrieve the parameter at the [index] or null
     *
     * @param index the index of the parameter
     */
    fun <T> getOrNull(index: Int): T? = values.getOrNull(index) as? T

    /**
     * Retrieve the parameter at 0
     * Enables convenient syntax in definitions like this:
     *
     * ´´´
     * factory { (id: String) ->
     *     MyPresenter(id = id)
     * }
     * ´´´
     *
     * @see get
     */
    operator fun <T> component1(): T = get(0)
    operator fun <T> component2(): T = get(1)
    operator fun <T> component3(): T = get(2)
    operator fun <T> component4(): T = get(3)
    operator fun <T> component5(): T = get(4)

}

/**
 * Creates parameters which contains all [values]
 *
 * @param values the provided parameters
 * @return the newly constructed parameters
 */
fun parametersOf(vararg values: Any?): Parameters = Parameters(values as Array<Any?>)

/**
 * Marks a the annotated constructor parameter in a @[Factory] or @[Single] annotated class as a parameter
 * The generated binding will then use the provided [Parameters] to resolve the dependency
 *
 * Example usage:
 *
 * ´´´
 * class MyViewModel(
 *     @Param private val id: String,
 *     private val api: Api
 * )
 * ´´´
 *
 * Note that [Parameters] will be retrieved in the same order they are specified in the constructor
 *
 * @see Parameters
 * @see Factory
 * @see Single
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Param
