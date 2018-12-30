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

import com.ivianuu.injekt.annotations.Factory
import com.ivianuu.injekt.annotations.Name
import com.ivianuu.injekt.annotations.Param
import com.ivianuu.injekt.annotations.Single

const val COMMANDS = "commands"
const val SERVICES = "services"
const val MIXED = "mixed"

interface Service {
    fun start()
}

@Factory
class ServiceOne : Service {
    override fun start() {

    }
}

@Factory
class ServiceTwo : Service {
    override fun start() {

    }
}

@Factory
class ServiceThree : Service {
    override fun start() {

    }
}

interface Command {
    fun execute()
}

@Single(createOnStart = true)
class CommandOne : Command {
    override fun execute() {
    }
}

@Factory(override = true)
class CommandTwo(
    @Param private val id: String,
    private val appDependency: AppDependency,
    @Param private val password: String
) : Command {
    override fun execute() {

    }
}

@Binds([Command::class])
@IntoClassMap(COMMANDS, CommandThree::class)
@IntoClassMap(SERVICES, CommandThree::class)
@Factory(secondaryTypes = [Command::class, Any::class, String::class])
class CommandThree(
    @Name("app") private val appDependency: AppDependency,
    private val mainActivityDependency: MainActivityDependency
) : Command {
    override fun execute() {

    }
}