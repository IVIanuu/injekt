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

import java.util.*

/**
 * Parameters which will be used for assisted injection
 */
/* inline*/ class Parameters(private val values: Array<Any?>) {

    /**
     * Number of contained elements
     */
    val Parameters.size: Int get() = values.size

    /**
     * Returns the element [i]
     */
    operator fun <T> get(i: Int): T = values[i] as T

    operator fun <T> component1(): T = get(0)
    operator fun <T> component2(): T = get(1)
    operator fun <T> component3(): T = get(2)
    operator fun <T> component4(): T = get(3)
    operator fun <T> component5(): T = get(4)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Parameters) return false

        if (!values.contentEquals(other.values)) return false

        return true
    }

    override fun hashCode(): Int = values.contentHashCode()

    override fun toString(): String = Arrays.toString(values)

}

/**
 * Defines [Parameters]
 */
typealias ParametersDefinition = () -> Parameters

/**
 * Returns new [Parameters] which contains all [values]
 */
fun parametersOf(vararg values: Any?): Parameters = Parameters(values as Array<Any?>)

/**
 * Returns new [Parameters] which contains all [values]
 */
fun parametersOf(values: Iterable<Any?>): Parameters = Parameters(values.toList().toTypedArray())

private val emptyParameters = Parameters(emptyArray())

/**
 * Returns empty [Parameters]
 */
fun emptyParameters(): Parameters = emptyParameters