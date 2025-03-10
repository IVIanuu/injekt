/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.integrationtests

import injekt.*

class Foo

class Bar(val foo: Foo)

interface Command

class CommandA : Command

class CommandB : Command

@Tag annotation class Tag1

@Tag annotation class Tag2

@Tag annotation class TypedTag<T>
