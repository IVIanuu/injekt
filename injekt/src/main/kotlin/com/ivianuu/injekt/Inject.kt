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
 * Makes the annotated class injectable by generating a binding for it
 *
 * For example making a class injectable looks like this:
 *
 * ´@Inject class MyViewModel(private val myRepository: MyRepository)´
 *
 * In case a class has multiple constructors the injectable constructor must be annotated instead
 *
 * ```
 * class MyApi {
 *
 *     @Inject
 *     constructor() : this("https://defaulturl.com/")
 *
 *     constructor(url: String)
 *
 * }
 * ```
 *
 * @see Name
 * @see Scope
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
annotation class Inject