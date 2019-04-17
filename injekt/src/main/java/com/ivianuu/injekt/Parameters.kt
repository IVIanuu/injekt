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
class Parameters(
    /**
     * All values of this params
     */
    val values: List<Any?>
) {

    operator fun <T> component1(): T = get(0)
    operator fun <T> component2(): T = get(1)
    operator fun <T> component3(): T = get(2)
    operator fun <T> component4(): T = get(3)
    operator fun <T> component5(): T = get(4)

    /**
     * Returns the element [i]
     */
    operator fun <T> get(i: Int): T = values[i] as T

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Parameters) return false

        if (values != other.values) return false

        return true
    }

    override fun hashCode(): Int = values.hashCode()

    override fun toString(): String = values.toString()

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