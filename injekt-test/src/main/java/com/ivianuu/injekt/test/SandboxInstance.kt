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

package com.ivianuu.injekt.test

import com.ivianuu.injekt.*
import com.ivianuu.injekt.InjektPlugins.logger
import org.mockito.Mockito.mock

/**
 * Sandbox Instance Holder - let execute the definition but return a mock of it
 */
@Suppress("UNCHECKED_CAST")
class SandboxInstance<T : Any>(declaration: Declaration<T>) : Instance<T>(declaration) {

    private var _value: T? = null

    override val isCreated: Boolean
        get() = _value != null

    override fun get(params: ParamsDefinition?): T {
        if (_value == null) {
            _value = create(params)
        }
        return _value ?: error("SandboxInstance should return a value for $declaration")
    }

    override fun create(params: ParamsDefinition?): T {
        try {
            declaration.definition.invoke(component.context, params?.invoke() ?: emptyParameters())
        } catch (e: Exception) {
            when (e) {
                is NoDeclarationFoundException, is InstanceCreationException, is OverrideException -> {
                    throw BrokenDeclarationException("Declaration $declaration is broken due to error : $e")
                }
                else -> logger?.debug("sandbox resolution continue on caught error: $e")
            }
        }
        return mock(declaration.type.java) as T
    }

}

class BrokenDeclarationException(msg: String) : Exception(msg)
