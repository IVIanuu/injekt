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

package com.ivianuu.injekt

@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.CONSTRUCTOR,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.PROPERTY_GETTER,
  AnnotationTarget.PROPERTY_SETTER,
  AnnotationTarget.LOCAL_VARIABLE,
  AnnotationTarget.TYPE
)
annotation class Inject1<A>

@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.CONSTRUCTOR,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.PROPERTY_GETTER,
  AnnotationTarget.PROPERTY_SETTER,
  AnnotationTarget.LOCAL_VARIABLE,
  AnnotationTarget.TYPE
)
annotation class Inject2<A, B>

@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.CONSTRUCTOR,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.PROPERTY_GETTER,
  AnnotationTarget.PROPERTY_SETTER,
  AnnotationTarget.LOCAL_VARIABLE,
  AnnotationTarget.TYPE
)
annotation class Inject3<A, B, C>

@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.CONSTRUCTOR,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.PROPERTY_GETTER,
  AnnotationTarget.PROPERTY_SETTER,
  AnnotationTarget.LOCAL_VARIABLE,
  AnnotationTarget.TYPE
)
annotation class Inject4<A, B, C, D>

@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.CONSTRUCTOR,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.PROPERTY_GETTER,
  AnnotationTarget.PROPERTY_SETTER,
  AnnotationTarget.LOCAL_VARIABLE,
  AnnotationTarget.TYPE
)
annotation class Inject5<A, B, C, D, E>
