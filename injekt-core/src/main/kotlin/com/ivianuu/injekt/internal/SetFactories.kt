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

class SetOfValueFactory<E>(private val providers: Set<@Provider () -> E>) : @Provider () -> Set<E> {
    override fun invoke(): Set<E> =
        providers.mapTo(LinkedHashSet(providers.size)) { it() }

    companion object {
        private val EMPTY = SetOfValueFactory<Any?>(emptySet())

        fun <E> create(element: @Provider () -> E): SetOfValueFactory<E> =
            SetOfValueFactory(setOf(element))

        fun <E> create(vararg elements: @Provider () -> E): SetOfValueFactory<E> =
            SetOfValueFactory(setOf(*elements))

        fun <E> empty(): SetOfValueFactory<E> = EMPTY as SetOfValueFactory<E>

    }
}

class SetOfProviderFactory<E>(private val providers: Set<@Provider () -> E>) :
    @Provider () -> Set<@Provider () -> E> {
    override fun invoke(): Set<@Provider () -> E> = providers

    companion object {
        private val EMPTY = SetOfProviderFactory<Any?>(emptySet())

        fun <E> create(element: @Provider () -> E): SetOfProviderFactory<E> =
            SetOfProviderFactory(setOf(element))

        fun <E> create(vararg elements: @Provider () -> E): SetOfProviderFactory<E> =
            SetOfProviderFactory(setOf(*elements))

        fun <E> empty(): SetOfProviderFactory<E> = EMPTY as SetOfProviderFactory<E>

    }
}
