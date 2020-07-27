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

typealias Arguments = Array<out Any?>?

operator fun <T> Arguments.get(index: Int) = this!![index] as T
operator fun <T> Arguments.component0() = get<T>(0)
operator fun <T> Arguments.component1() = get<T>(1)
operator fun <T> Arguments.component2() = get<T>(2)
operator fun <T> Arguments.component3() = get<T>(3)
operator fun <T> Arguments.component4() = get<T>(4)

inline fun emptyArguments() = null
