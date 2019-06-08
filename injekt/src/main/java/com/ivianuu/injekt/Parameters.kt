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
 * Parameters which can be used to pass things like an id
 */
/* inline*/ class Parameters(private val values: Array<Any?>) {

    val size: Int get() = values.size

    operator fun <T> get(i: Int): T = values[i] as T

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Parameters) return false

        if (!values.contentEquals(other.values)) return false

        return true
    }

    override fun hashCode(): Int = values.contentHashCode()

    override fun toString(): String = Arrays.toString(values)

}

typealias ParametersDefinition = () -> Parameters

operator fun <T> Parameters.component1(): T = get(0)
operator fun <T> Parameters.component2(): T = get(1)
operator fun <T> Parameters.component3(): T = get(2)
operator fun <T> Parameters.component4(): T = get(3)
operator fun <T> Parameters.component5(): T = get(4)

fun parametersOf(): Parameters = Parameters(emptyArray())

fun parametersOf(vararg values: Any?): Parameters = Parameters(values as Array<Any?>)

fun parametersOf(values: List<Any?>): Parameters = Parameters(values.toTypedArray())

private val emptyParameters = Parameters(emptyArray())
fun emptyParameters(): Parameters = emptyParameters

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Param