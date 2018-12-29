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

const val COMMANDS = "commands"
const val SERVICES = "services"
const val MIXED = "mixed"

interface Service {
    fun start()
}

class ServiceOne : Service {
    override fun start() {

    }
}

class ServiceTwo : Service {
    override fun start() {

    }
}

class ServiceThree : Service {
    override fun start() {

    }
}

interface Command {
    fun execute()
}

class CommandOne : Command {
    override fun execute() {
    }
}

class CommandTwo : Command {
    override fun execute() {

    }
}

class CommandThree : Command {
    override fun execute() {

    }
}