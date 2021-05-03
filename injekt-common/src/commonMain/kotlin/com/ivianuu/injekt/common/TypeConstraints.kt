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

sealed class IsSubType<in A, out B> : (A) -> B {
    companion object {
        private object Singleton : IsSubType<Any?, Any?>() {
            override fun invoke(p1: Any?): Any? = p1
        }
        @Suppress("UNCHECKED_CAST")
        @Given
        fun <A : B, B> instance(): IsSubType<A, B> = Singleton as IsSubType<A, B>
    }
}

sealed class IsEqual<A, B> : (A) -> B {
    companion object {
        private object Singleton : IsEqual<Any?, Any?>() {
            override fun invoke(p1: Any?): Any? = p1
        }
        @Suppress("UNCHECKED_CAST")
        @Given
        fun <A> instance(): IsEqual<A, A> = Singleton as IsEqual<A, A>
    }
}
