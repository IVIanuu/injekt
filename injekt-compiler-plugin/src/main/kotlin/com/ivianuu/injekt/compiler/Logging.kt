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

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.FunApi
import com.ivianuu.injekt.FunBinding

var loggingEnabled = true

interface Logger {
    fun log(msg: String)
}

object LoggerImpl : Logger {
    override fun log(msg: String) {
        println(msg)
    }
}

@Binding
fun logger(): Logger? = if (loggingEnabled) LoggerImpl else null

@FunBinding
fun log(
    logger: Logger?,
    @FunApi msg: () -> String,
) {
    logger?.log(msg())
}
