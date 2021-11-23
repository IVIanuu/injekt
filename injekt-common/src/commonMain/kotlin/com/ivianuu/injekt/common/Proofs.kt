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

class Provided<out T> private constructor() {
  @Suppress("UNUSED_PARAMETER")
  companion object {
    @PublishedApi internal val instance: Provided<Nothing> = Provided()

    @Provide inline fun <T> instance(x: () -> T): Provided<T> = instance
  }
}

class NotProvided<out T> private constructor() {
  sealed class LowPriorityModule {
    private val instance: NotProvided<Nothing> = NotProvided()

    @Provide fun <T> instance(): NotProvided<T> = instance
  }

  @Suppress("UNUSED_PARAMETER")
  companion object : LowPriorityModule() {
    @AmbiguousInjectable("Cannot proof that [T] is not provided")
    @Provide fun <T> amb1(value: T): NotProvided<T> = throw AssertionError()

    @Provide fun <T> amb2(value: T): NotProvided<T> = throw AssertionError()
  }
}

@InjectableNotFound("Cannot proof that [A] is sub type of [B]")
sealed interface IsSubType<A, B> {
  companion object {
    private object Instance : IsSubType<Any?, Any?>

    @Suppress("UNCHECKED_CAST")
    @Provide
    fun <A : B, B> instance(): IsSubType<A, B> = Instance as IsSubType<A, B>
  }
}

sealed interface IsNotSubType<A, B> {
  sealed class LowPriorityModule {
    private object Instance : IsNotSubType<Any?, Any?>

    @Suppress("UNCHECKED_CAST")
    @Provide fun <A, B> instance(): IsNotSubType<A, B> = Instance as IsNotSubType<A, B>
  }

  @Suppress("UNCHECKED_CAST")
  companion object : LowPriorityModule() {
    @Provide
    @AmbiguousInjectable("Cannot proof that [A] is NOT a sub type of [B]")
    fun <A : B, B> amb1(): IsNotSubType<A, B> = throw AssertionError()

    @Provide fun <A : B, B> amb2(): IsNotSubType<A, B> = throw AssertionError()
  }
}

@InjectableNotFound("Cannot proof that [A] is equal to [B]")
sealed interface IsEqual<A, B> {
  companion object {
    private object Instance : IsEqual<Any?, Any?>

    @Suppress("UNCHECKED_CAST")
    @Provide
    fun <A> instance(): IsEqual<A, A> = Instance as IsEqual<A, A>
  }
}

sealed interface IsNotEqual<A, B> {
  sealed class LowPriorityModule {
    private object Instance : IsNotEqual<Any?, Any?>

    @Suppress("UNCHECKED_CAST")
    @Provide
    fun <A, B> instance(): IsNotEqual<A, B> = Instance as IsNotEqual<A, B>
  }

  companion object : LowPriorityModule() {
    @Provide
    @AmbiguousInjectable("Cannot proof that [A] is NOT equal of [B]")
    fun <A> amb1(): IsNotEqual<A, A> = throw AssertionError()

    @Provide fun <A> amb2(): IsNotEqual<A, A> = throw AssertionError()
  }
}

class InSuspend private constructor() {
  sealed interface LowPriorityModule {
    @AmbiguousInjectable("Cannot proof that in suspend context")
    @Provide fun amb1(): InSuspend = throw AssertionError()

    @Provide fun amb2(): InSuspend = throw AssertionError()
  }

  companion object : LowPriorityModule {
    private val Instance = InSuspend()
    @Provide suspend fun instance() = Instance
  }
}

class NotInSuspend private constructor() {
  sealed class LowPriorityModule {
    @Provide val instance = NotInSuspend()
  }

  companion object : LowPriorityModule() {
    @AmbiguousInjectable("Cannot proof that outside of suspend")
    @Provide suspend fun amb1(): NotInSuspend = throw AssertionError()

    @Provide suspend fun amb2(): NotInSuspend = throw AssertionError()
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
    @Provide inline fun <reified T> amb1(): NotReified<T> = throw AssertionError()

    @Provide inline fun <reified T> amb2(): NotReified<T> = throw AssertionError() }
}

sealed interface InComponent<out C : Component> {
  sealed interface LowPriorityModule {
    @AmbiguousInjectable("Cannot proof enclosing [C]")
    @Provide fun <C : Component> amb1(): InComponent<C> = throw AssertionError()

    @Provide fun <C : Component> amb2(): InComponent<C> = throw AssertionError()
  }

  companion object : LowPriorityModule {
    private object Instance : InComponent<Nothing>

    @Provide @Scoped<C>(eager = true)
    fun <C : Component> instance(): InComponent<C> = Instance
  }
}

sealed interface NotInComponent<out C : Component> {
  sealed class LowPriorityModule {
    private object Instance : NotInComponent<Nothing>
    @Provide fun <C : Component> instance(): NotInComponent<C> = Instance
  }

  companion object : LowPriorityModule() {
    @AmbiguousInjectable("Cannot proof [C] is not a enclosing component")
    @Provide @Scoped<C>()
    fun <C : Component> amb1(): NotInComponent<C> = throw AssertionError()

    @Provide @Scoped<C>()
    fun <C : Component> amb2(): NotInComponent<C> = throw AssertionError()
  }
}
