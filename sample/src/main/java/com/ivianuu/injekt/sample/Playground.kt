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

package com.ivianuu.injekt.sample

import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.Name
import com.ivianuu.injekt.Provider

// kinds
interface Command

@Name(Commands.Companion::class)
annotation class Commands {
    companion object
}

@Inject
class MyDep(
    // default
    private val command: Command,

    // lazy
    private val commandLazy: Lazy<Command>,

    // provider
    private val commandProvider: Provider<Command>,

    // map
    @Commands private val commandsMap: Map<String, Command>,
    @Commands private val commandsMapLazy: Map<String, Lazy<Command>>,
    @Commands private val commandsMapProvider: Map<String, Provider<Command>>,

    // set
    @Commands private val commandsSet: Set<Command>,
    @Commands private val commandsSetLazy: Set<Lazy<Command>>,
    @Commands private val commandsSetProvider: Set<Provider<Command>>
)