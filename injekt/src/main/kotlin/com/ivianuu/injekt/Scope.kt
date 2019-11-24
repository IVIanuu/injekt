/*
 * Copyright 2019 Manuel Wrage
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

/**
 * Marks the annotated class as a scope
 *
 * For example a scope for activities is declared like this
 *
 * @Scope annotation class ActivityScope
 *
 * The following code ensures that the same view model instance is reused in activity scoped [Component]s
 *
 * ´´´
 * @ActivityScope
 * @Inject
 * class MyViewModel
 * ´´´
 *
 * @see ComponentBuilder.scopes
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Scope

/**
 * Used by generated code to provide the of the injectable
 *
 * @see CodegenJustInTimeLookupFactory
 */
interface HasScope {
    val scope: Any
}