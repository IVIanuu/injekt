/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.*

class Foo

class Bar(val foo: Foo)

interface Command

class CommandA : Command

class CommandB : Command

@Tag
@Target(AnnotationTarget.TYPE, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.CLASS)
annotation class Tag1

@Tag
@Target(AnnotationTarget.TYPE, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.CLASS)
annotation class Tag2

@Tag
@Target(AnnotationTarget.TYPE, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.CLASS)
annotation class TypedTag<T>

object TestScope1

object TestScope2
