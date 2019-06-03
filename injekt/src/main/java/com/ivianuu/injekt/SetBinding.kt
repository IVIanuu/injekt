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
 * Set binding
 */
data class SetBinding<T>(
    val setType: Type<T>,
    val setName: Qualifier?,
    val override: Boolean
) {
    val setKey = Key(typeOf<Set<T>>(Set::class, setType), setName)
}

inline fun <reified T> setBinding(
    setType: Type<T> = typeOf(),
    setName: Qualifier? = null,
    override: Boolean = false
): SetBinding<T> = SetBinding(setType, setName, override)