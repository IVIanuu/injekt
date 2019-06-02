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
 * Provides dependencies of kind [T]
 */
interface Provider<T> {
    /**
     * Returns a potentially new value of [T] using [parameters]
     */
    fun get(parameters: ParametersDefinition? = null): T
}

/**
 * Returns a [Provider] which invokes the [provider] on [Provider.get]
 */
fun <T> provider(provider: (parameters: ParametersDefinition?) -> T): Provider<T> =
    LambdaProvider(provider)

private class LambdaProvider<T>(private val func: (ParametersDefinition?) -> T) :
    Provider<T> {
    override fun get(parameters: ParametersDefinition?): T = func(parameters)
}