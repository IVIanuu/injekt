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

package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Global declaration registry
 * This used for generated modules
 * [Component]s will search here as a last resort if no internal declarations or dependencies
 * Fulfill a request
 */
object GlobalDeclarationRegistry {

    private val dummyComponent = component {}

    val declarationRegistry = DeclarationRegistry(dummyComponent)

    fun loadModules(vararg modules: Module) {
        declarationRegistry.loadModules(*modules)
    }

    fun saveDeclaration(declaration: Declaration<*>) {
        declarationRegistry.saveDeclaration(declaration)
    }

    fun findDeclaration(type: KClass<*>, name: String? = null) =
        findDeclaration(Key.of(type, name))

    fun findDeclaration(key: Key) = declarationRegistry.findDeclaration(key)

}