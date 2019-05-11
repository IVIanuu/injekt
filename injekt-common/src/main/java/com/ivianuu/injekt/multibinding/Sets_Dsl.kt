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

package com.ivianuu.injekt.multibinding

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.getOrSet

/**
 * Adds this binding into a set
 */
infix fun <T : V, V> Binding<T>.bindIntoSet(setBinding: SetBinding<V>): Binding<T> {
    attributes.getOrSet(KEY_SET_BINDINGS) {
        mutableMapOf<SetName<V>, SetBinding<V>>()
    }[setBinding.setName] = setBinding
    return this
}

/**
 * Adds this binding into a set
 */
infix fun <T : V, V> Binding<T>.bindIntoSet(setName: SetName<V>): Binding<T> {
    bindIntoSet(SetBinding(setName))
    return this
}