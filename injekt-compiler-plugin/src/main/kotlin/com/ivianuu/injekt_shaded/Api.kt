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

@file:Suppress("NOTHING_TO_INLINE")

package com.ivianuu.injekt_shaded

@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.CONSTRUCTOR,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.LOCAL_VARIABLE,
  AnnotationTarget.VALUE_PARAMETER,
  AnnotationTarget.TYPE
)
annotation class Provide

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
annotation class Inject

@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.CONSTRUCTOR,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.FILE,
  AnnotationTarget.LOCAL_VARIABLE,
  AnnotationTarget.EXPRESSION
)
@Retention(AnnotationRetention.SOURCE)
annotation class Providers(vararg val importPaths: String)

inline fun <T> inject(@Inject value: T): T = value

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Tag

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Spread
