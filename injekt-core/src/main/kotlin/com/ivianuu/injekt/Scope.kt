/*
 * Copyright 2020 Manuel Wrage
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

import com.ivianuu.injekt.internal.asScoped
import com.ivianuu.injekt.internal.injektIntrinsic
import kotlin.reflect.KClass

/**
 * Scopes are used to name [Component]s
 * This allows annotation api [Binding]s to be associated with a specific [Component]
 *
 * A scope can be declared like this
 *
 * ´´´
 * @Scope
 * annotation class ActivityScope
 * ´´´
 *
 * The following code ensures that the view model will be only instantiated in the activity scoped [Component]
 *
 * ´´´
 * @ActivityScope
 * @Factory
 * class MyViewModel
 * ´´´
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Scope

@Module
inline fun <reified T> scoped(
    qualifier: KClass<*>? = null,
    bindingDefinition: BindingDefinition<T>
): Unit = injektIntrinsic()

@Module
inline fun <reified T> scoped(
    qualifier: KClass<*>? = null,
    binding: Binding<T>
): Unit = injektIntrinsic()

@Module
fun <T> scoped(
    key: Key<T>,
    bindingDefinition: BindingDefinition<T>
): Unit = injektIntrinsic()

@Module
fun <T> scoped(
    key: Key<T>,
    binding: Binding<T>
) {
    addBinding(key, binding.asScoped())
}
