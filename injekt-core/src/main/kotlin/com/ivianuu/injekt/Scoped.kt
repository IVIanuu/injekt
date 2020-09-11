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

fun <@ForKey T> ContextBuilder.scoped(
    key: Key<T> = keyOf(),
    duplicatePolicy: DuplicatePolicy = DuplicatePolicy.Fail,
    provider: @Reader () -> T
) {
    unscoped(key, duplicatePolicy, provider.scope())
}

fun <T> (@Reader () -> T).scope(): @Reader () -> T {
    return if (this is ScopedProvider) this
    else ScopedProvider(this)
}

private class ScopedProvider<T>(
    provider: @Reader () -> T
) : @Reader () -> T {
    private var _provider: @Reader (() -> T)? = provider
    private var _value: Any? = this

    @Reader
    override fun invoke(): T {
        var value: Any? = _value
        if (value === this) {
            synchronized(this) {
                value = _value
                if (value === this) {
                    value = _provider!!()
                    _value = value
                    _provider = null
                }
            }
        }
        return value as T
    }
}
