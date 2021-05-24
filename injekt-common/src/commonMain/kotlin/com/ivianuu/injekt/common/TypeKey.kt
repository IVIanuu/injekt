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

package com.ivianuu.injekt.common

import com.ivianuu.injekt.*

/**
 * A key for a injekt type which can be used as a map key or similar
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class TypeKey<out T>(val value: String) {
  companion object {
    /**
     * Provides a [TypeKey] of [T]
     */
    @Provide inline fun <@ForTypeKey T> typeKey(): TypeKey<T> = typeKeyOf()
  }
}

/**
 * Returns a [TypeKey] of [T]
 */
fun <@ForTypeKey T> typeKeyOf(): TypeKey<T> = throw NotImplementedError("Implemented as intrinsic")

/**
 * Allows to retrieve a [TypeKey] for this type parameter
 */
@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class ForTypeKey
