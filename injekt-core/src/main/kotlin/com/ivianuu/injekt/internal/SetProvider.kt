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

package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Provider

class SetProvider<E>(private val providers: Set<@Provider () -> E>) : () -> Set<E> {
    override fun invoke(): Set<E> =
        providers.mapTo(LinkedHashSet(providers.size)) { it() }

    companion object {
        private val EMPTY = SetProvider<Any?>(emptySet())

        fun <E> create(element: @Provider () -> E): SetProvider<E> =
            SetProvider(setOf(element))

        fun <E> create(vararg elements: @Provider () -> E): SetProvider<E> =
            SetProvider(setOf(*elements))

        fun <E> empty(): SetProvider<E> = EMPTY as SetProvider<E>

    }
}
