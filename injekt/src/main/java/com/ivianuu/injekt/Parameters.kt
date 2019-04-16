/*
 * Copyright 2018 Manuel Wrage
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
 * Parameters which will be used for assisted injection
 */
class Parameters(val values: List<Any?>) {

    operator fun <T> component1(): T = elementAt(0)
    operator fun <T> component2(): T = elementAt(1)
    operator fun <T> component3(): T = elementAt(2)
    operator fun <T> component4(): T = elementAt(3)
    operator fun <T> component5(): T = elementAt(4)

    /**
     * Returns the element [i]
     */
    operator fun <T> get(i: Int): T = values[i] as T

    private fun <T> elementAt(i: Int): T =
        if (values.size > i) values[i] as T else throw IllegalArgumentException("Can't get parameter value #$i from $this")

}

/**
 * Number of contained elements
 */
val Parameters.size: Int get() = values.size

/**
 * Defines [Parameters]
 */
typealias ParametersDefinition = () -> Parameters

/**
 * Returns new [Parameters] which contains all [values]
 */
fun parametersOf(vararg values: Any?): Parameters = Parameters(listOf(*values))

/**
 * Returns new [Parameters] which contains the [value]
 */
fun parametersOf(value: Any?): Parameters = Parameters(listOf(value))

private val emptyParameters = Parameters(emptyList())

/**
 * Returns new empty parameters
 */
fun parametersOf(): Parameters = emptyParameters

/**
 * Returns empty [Parameters]
 */
fun emptyParameters(): Parameters = emptyParameters