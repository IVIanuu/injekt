/*
 * Copyright 2018 Manuel Wrage
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

package com.ivianuu.injekt.annotations

import com.ivianuu.injekt.BindingCreator
import com.ivianuu.injekt.Kind
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class CreatorInterceptor(val interceptor: KClass<out BindingCreator.Interceptor<*>>)

annotation class Kinds(val kinds: Array<KClass<out Kind>>)

annotation class Scoped(val scope: KClass<out Scope>)
annotation class Qualified(val qualifier: KClass<out Qualified>)

annotation class BindTypes(val types: Array<KClass<*>>)

annotation class BindQualifiers(val qualifiers: Array<KClass<out Qualifier>>)
annotation class UseInterceptors(val interceptors: Array<KClass<out BindingCreator.Interceptor<*>>>)