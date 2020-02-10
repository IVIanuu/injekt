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

package com.ivianuu.injekt.playground

import com.ivianuu.injekt.Factory
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Name
import com.ivianuu.injekt.Param
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.keyOf
import kotlin.reflect.KClass

// kinds
interface Command

@Name
annotation class Commands {
    companion object
}

@Factory
class CommandOne : Command

@Factory
class CommandTwo : Command

@Factory
class CommandThree : Command

val commandsModule = Module {
    map<KClass<out Command>, Command> {
        CommandOne::class to keyOf<CommandOne>()
        CommandOne::class to keyOf<CommandOne>()
        CommandTwo::class to keyOf<CommandTwo>()
    }
}

@Factory
internal class InternalDep

@Factory
object ObjectDep

@Factory
class EmptyConstructorDep

@Factory
class OnlyParamsConstructorDep(@Param param: String, val lol: String)

@Factory
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
    @Commands private val commandsSetProvider: Set<Provider<Command>>,

    private val nullable: String?,

    @Param private val param: String,

    private val adapter: Lazy<JsonAdapter<List<MyInterface>>>
)

interface MyInterface
