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

import kotlin.reflect.KClass

/**
 * Marks the annotated class as a name which can be used to differentiate between instances of the same type
 *
 * It's a good convention to declare names which can be used from both the dsl AND annotated classes
 * This can be achieved by declaring it like this:
 *
 * ´´´
 * @Name(UserId.Companion::class)
 * annotation class UserId {
 *     companion object
 * }
 * ´´´
 *
 * We can then use the name in the dsl as follows:
 *
 * ´´´
 * factory {
 *     MyViewModel(userId = get(name = UserId))
 * }
 * ´´´
 *
 * And also in @Inject annotated classes like this:
 *
 * ´´´
 * @Inject
 * class MyViewModel(@UserId private val userId: String)
 * ´´´
 *
 * @see Component.get
 * @see Inject
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CLASS)
annotation class Name(val name: KClass<*>)