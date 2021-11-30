/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.shaded_injekt.internal

internal annotation class Index(val fqName: String)

internal annotation class CallableInfo(val values: Array<String>)

internal annotation class ClassifierInfo(val values: Array<String>)

internal annotation class TypeParameterInfo(val values: Array<String>)
