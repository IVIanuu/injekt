/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.internal

internal annotation class DeclarationInfo(val values: Array<String>)

internal annotation class TypeParameterInfo(val values: Array<String>)

@Target(AnnotationTarget.TYPE) internal annotation class FrameworkKey(val value: String)
