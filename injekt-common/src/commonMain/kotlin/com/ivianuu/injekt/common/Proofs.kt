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

class NotProvided<out T> private constructor() {
  sealed class LowPriorityModule {
    private val instance: NotProvided<Nothing> = NotProvided()

    @Provide fun <T> instance(): NotProvided<T> = instance
  }

  @Suppress("UNUSED_PARAMETER")
  companion object : LowPriorityModule() {
    @Provide fun <T> amb1(value: T): NotProvided<T> = throw AssertionError()

    @Provide fun <T> amb2(value: T): NotProvided<T> = throw AssertionError()
  }
}

/**
 * Proofs that every [A] is a sub type of [B]
 */
@InjectableNotFound("Cannot proof that [A] is sub type of [B]")
sealed interface IsSubType<A, B> : (A) -> B {
  companion object {
    private object Instance : IsSubType<Any?, Any?> {
      override fun invoke(p1: Any?): Any? = p1
    }

    @Suppress("UNCHECKED_CAST")
    @Provide
    fun <A : B, B> instance(): IsSubType<A, B> = Instance as IsSubType<A, B>
  }
}

/**
 * Proofs that every [A] is not a sub type of [B]
 */
sealed interface IsNotSubType<A, B> {
  @Suppress("UNCHECKED_CAST")
  companion object {
    private object Instance : IsNotSubType<Any?, Any?>

    @Provide fun <A, B> instance(): IsNotSubType<A, B> = Instance as IsNotSubType<A, B>

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
    private object Instance : IsEqual<Any?, Any?> {
      override fun invoke(p1: Any?): Any? = p1
    }

    @Suppress("UNCHECKED_CAST")
    @Provide
    fun <A> instance(): IsEqual<A, A> = Instance as IsEqual<A, A>
  }
}

/**
 * Proofs that every [A] is not equal to [B]
 */
sealed interface IsNotEqual<A, B> {
  companion object {
    private object Instance : IsNotEqual<Any?, Any?>

    @Suppress("UNCHECKED_CAST")
    @Provide
    fun <A, B> instance(): IsNotEqual<A, B> = Instance as IsNotEqual<A, B>

    @Provide
    @AmbiguousInjectable("Cannot proof that [A] is NOT equal of [B]")
    fun <A> amb1(): IsNotEqual<A, A> = throw AssertionError()

    @Provide fun <A> amb2(): IsNotEqual<A, A> = throw AssertionError()
  }
}

class Suspend private constructor() {
  sealed class LowPriorityModule {
    @AmbiguousInjectable("Not in suspend context")
    @Provide fun amb1(): Suspend = throw AssertionError()

    @Provide fun amb2(): Suspend = throw AssertionError()
  }

  companion object : LowPriorityModule() {
    private val Instance = Suspend()
    @Provide suspend fun instance() = Instance
  }
}

class NotSuspend private constructor() {
  sealed class LowPriorityModule {
    @Provide val instance = NotSuspend()
  }

  companion object : LowPriorityModule() {
    @AmbiguousInjectable("Cannot proof that outside of suspend")
    @Provide suspend fun amb1(): NotSuspend = throw AssertionError()

    @Provide suspend fun amb2(): NotSuspend = throw AssertionError()
  }
}

sealed interface Reified<out T> {
  sealed interface LowPriorityModule {
    @AmbiguousInjectable("Cannot proof that [T] is reified")
    @Provide fun <T> amb1(): Reified<T> = throw AssertionError()

    @Provide fun <T> amb2(): Reified<T> = throw AssertionError()
  }

  companion object : LowPriorityModule {
    @PublishedApi internal object Instance : Reified<Nothing>

    @Provide inline fun <reified T> instance(): Reified<T> = Instance
  }
}

sealed interface NotReified<out T> {
  sealed class LowPriorityModule {
    private object Instance : NotReified<Nothing>
    @Provide fun <T> instance(): NotReified<T> = Instance
  }

  companion object : LowPriorityModule() {
    @Provide fun <T> amb1(): Reified<T> = throw AssertionError()

    @Provide fun <T> amb2(): Reified<T> = throw AssertionError() }
}

sealed interface InComponent<out C : @Component Any> {
  sealed interface LowPriorityModule {
    @AmbiguousInjectable("No enclosing component [C] found")
    @Provide fun <C : @Component Any> amb1(): InComponent<C> = throw AssertionError()

    @Provide fun <C : @Component Any> amb2(): InComponent<C> = throw AssertionError()
  }

  companion object : LowPriorityModule {
    private object Instance : InComponent<Nothing>

    @Provide fun <C : @Component Any> instance():
        @Scoped<C>(eager = true) InComponent<C> = Instance
  }
}

sealed interface NotInComponent<out C : @Component Any> {
  sealed class LowPriorityModule {
    private object Instance : InComponent<Nothing>
    @Provide fun <C : @Component Any> instance(): InComponent<C> = Instance
  }

  companion object : LowPriorityModule() {
    @AmbiguousInjectable("[C] is a enclosing component")
    @Provide fun <C : @Component Any> amb1():
        @Scoped<C>(eager = true) InComponent<C> = throw AssertionError()

    @Provide fun <C : @Component Any> amb2():
        @Scoped<C>(eager = true) InComponent<C> = throw AssertionError()
  }
}
