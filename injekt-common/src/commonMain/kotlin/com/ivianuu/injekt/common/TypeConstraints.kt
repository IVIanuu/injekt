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
 * Type class witnessing that every [A] is a sub type of [B]
 */
sealed class IsSubType<A, B> : (A) -> B {
    companion object {
        private object Singleton : IsSubType<Any?, Any?>() {
            override fun invoke(p1: Any?): Any? = p1
        }
        @Suppress("UNCHECKED_CAST")
        @Given
        fun <A : B, B> instance(): IsSubType<A, B> = Singleton as IsSubType<A, B>
    }
}

/**
 * Type class witnessing that every [A] is not a sub type of [B]
 */
sealed class IsNotSubType<A, B> {
    companion object {
        private object Singleton : IsNotSubType<Any?, Any?>()

        @Suppress("UNCHECKED_CAST")
        @Given
        fun <A, B> instance(): IsNotSubType<A, B> = Singleton as IsNotSubType<A, B>

        @Suppress("UNCHECKED_CAST")
        @Given
        fun <A : B, B> amb1(): IsNotSubType<A, B> = Singleton as IsNotSubType<A, B>

        @Suppress("UNCHECKED_CAST")
        @Given
        fun <A : B, B> amb2(): IsNotSubType<A, B> = Singleton as IsNotSubType<A, B>
    }
}

/**
 * Type class witnessing that every [A] is equal to [B]
 */
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

/**
 * Type class witnessing that every [A] is not equal to [B]
 */
sealed class IsNotEqual<A, B> {
    companion object {
        private object Singleton : IsNotEqual<Any?, Any?>()

        @Suppress("UNCHECKED_CAST")
        @Given
        fun <A, B> instance(): IsNotEqual<A, B> = Singleton as IsNotEqual<A, B>

        @Suppress("UNCHECKED_CAST")
        @Given
        fun <A> amb1(): IsNotEqual<A, A> = Singleton as IsNotEqual<A, A>

        @Suppress("UNCHECKED_CAST")
        @Given
        fun <A> amb2(): IsNotEqual<A, A> = Singleton as IsNotEqual<A, A>
    }
}
