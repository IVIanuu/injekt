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
 * A Module is a collection of [Binding]s to drive [Component]s
 *
 * A typical Module might look like this:
 *
 * ´´´
 * val myRepositoryModule = Module {
 *     single { MyRepository(api = get(), database = get()) }
 *     single { MyApi(serializer = get()) }
 *     single { MyDatabase(databaseFile = get()) }
 * }
 * ´´´
 *
 * @see ComponentBuilder.modules
 */
class Module internal constructor(
    internal val bindings: Map<Key, Binding<*>>,
    internal val multiBindingMaps: Map<Key, MultiBindingMap<Any?, Any?>>,
    internal val multiBindingSets: Map<Key, MultiBindingSet<Any?>>
)
