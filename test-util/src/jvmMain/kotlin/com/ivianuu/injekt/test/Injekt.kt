/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.test

class Foo

class Bar(val foo: Foo)

interface Command

class CommandA : Command

class CommandB : Command
