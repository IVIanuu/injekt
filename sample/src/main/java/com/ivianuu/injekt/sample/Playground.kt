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

import com.ivianuu.injekt.Bind
import com.ivianuu.injekt.Factory
import com.ivianuu.injekt.Single
import com.ivianuu.injekt.multibinding.BindingMap
import com.ivianuu.injekt.multibinding.BindingSet
import com.ivianuu.injekt.multibinding.MapName
import com.ivianuu.injekt.multibinding.SetName
import com.ivianuu.injekt.provider.Provider
import com.ivianuu.injekt.weak.WeakKind

object NoScope

@Bind(kind = WeakKind::class, scope = NoScope::class)
class WeakDep

@Single(scope = NoScope::class)
class SingleDep

@Factory
class FactoryDep

// kinds
interface Command

object Commands : MapName<String, Command>, SetName<Command>

@Factory
class MyDep(
    // default
    private val command: Command,

    // lazy
    private val commandLazy: Lazy<Command>,

    // provider
    private val commandProvider: Provider<Command>,

    // map
    @BindingMap(Commands::class) private val commandsMap: Map<String, Command>,
    @BindingMap(Commands::class) private val commandsMapLazy: Map<String, Lazy<Command>>,
    @BindingMap(Commands::class) private val commandsMapProvider: Map<String, Provider<Command>>,

    // set
    @BindingSet(Commands::class) private val commandsSet: Set<Command>,
    @BindingSet(Commands::class) private val commandsSetLazy: Set<Lazy<Command>>,
    @BindingSet(Commands::class) private val commandsSetProvider: Set<Provider<Command>>
)