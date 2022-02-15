/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.test

import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.*

class Foo

class Bar(val foo: Foo)

class Baz(val bar: Bar, val foo: Foo)

interface Command

class CommandA : Command

class CommandB : Command

@JvmInline value class Tag1<T>(override val _value: Any?) : Tag<T>

@JvmInline value class Tag2<T>(override val _value: Any?) : Tag<T>

@JvmInline value class TypedTag<A, T>(override val _value: Any?) : Tag<T>

object TestScope1

object TestScope2
