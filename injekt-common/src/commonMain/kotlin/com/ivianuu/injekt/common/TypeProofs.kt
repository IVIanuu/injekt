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

import com.ivianuu.injekt.AmbiguousInjectable
import com.ivianuu.injekt.InjectableNotFound
import com.ivianuu.injekt.Provide

/**
 * Proofs that every [A] is a sub type of [B]
 */
@InjectableNotFound("Cannot proof that [A] is sub type of [B]")
sealed interface IsSubType<A, B> : (A) -> B {
  companion object {
    private object Impl : IsSubType<Any?, Any?> {
      override fun invoke(p1: Any?): Any? = p1
    }

    @Suppress("UNCHECKED_CAST")
    @Provide
    fun <A : B, B> instance(): IsSubType<A, B> = Impl as IsSubType<A, B>
  }
}

/**
 * Proofs that every [A] is not a sub type of [B]
 */
sealed interface IsNotSubType<A, B> {
  @Suppress("UNCHECKED_CAST")
  companion object {
    private object Impl : IsNotSubType<Any?, Any?>

    @Provide fun <A, B> instance(): IsNotSubType<A, B> = Impl as IsNotSubType<A, B>

    @Provide
    @AmbiguousInjectable("Cannot proof that [A] is NOT a sub type of [B]")
    fun <A : B, B> amb1(): IsNotSubType<A, B> = throw AssertionError()

    @Provide fun <A : B, B> amb2(): IsNotSubType<A, B> = throw AssertionError()
  }
}

/**
 * Proofs that every [A] is equal to [B]
 */
@InjectableNotFound("Cannot proof that [A] is equal to [B]")
sealed interface IsEqual<A, B> : (A) -> B {
  companion object {
    private object Impl : IsEqual<Any?, Any?> {
      override fun invoke(p1: Any?): Any? = p1
    }

    @Suppress("UNCHECKED_CAST")
    @Provide
    fun <A> instance(): IsEqual<A, A> = Impl as IsEqual<A, A>
  }
}

/**
 * Proofs that every [A] is not equal to [B]
 */
sealed interface IsNotEqual<A, B> {
  companion object {
    private object Impl : IsNotEqual<Any?, Any?>

    @Suppress("UNCHECKED_CAST")
    @Provide
    fun <A, B> instance(): IsNotEqual<A, B> = Impl as IsNotEqual<A, B>

    @Provide
    @AmbiguousInjectable("Cannot proof that [A] is NOT equal of [B]")
    fun <A> amb1(): IsNotEqual<A, A> = throw AssertionError()

    @Provide fun <A> amb2(): IsNotEqual<A, A> = throw AssertionError()
  }
}
