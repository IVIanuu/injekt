/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.integrationtests

import injekt.*
import kotlin.annotation.AnnotationTarget.*

class Foo

class Bar(val foo: Foo)

interface Command

class CommandA : Command

class CommandB : Command

@Tag @Target(CLASS, CONSTRUCTOR, TYPE) annotation class Tag1

@Tag @Target(CLASS, CONSTRUCTOR, TYPE) annotation class Tag2

@Tag @Target(CLASS, CONSTRUCTOR, TYPE) annotation class TypedTag<T>
