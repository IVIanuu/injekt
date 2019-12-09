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
 * A key of a binding
 *
 * This is used to identify bindings in [Component]s and [Module]s
 *
 * @see Component.get
 * @see ModuleBuilder.bind
 */
data class Key internal constructor(val type: Type<*>, val name: Any? = null)

inline fun <reified T> keyOf(name: Any? = null): Key = keyOf(typeOf<T>(), name)

/**
 * Create a key
 *
 * @param type the type of the key
 * @param name the name of the key
 * @return the constructed key
 */
fun keyOf(type: Type<*>, name: Any? = null): Key = Key(type, name)
