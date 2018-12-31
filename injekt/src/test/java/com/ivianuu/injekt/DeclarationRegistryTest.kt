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

import com.ivianuu.injekt.util.TestDep1
import com.ivianuu.injekt.util.TestDep2
import junit.framework.Assert.*
import org.junit.Test

class DeclarationRegistryTest {

    @Test
    fun testLoadModules() {
        val registry = component { }.declarationRegistry
        val module = module {
            factory { TestDep1() }
            factory { TestDep2(get()) }
        }

        registry.loadModules(module)

        assertEquals(registry.getAllDeclarations(), module.declarations.values.toSet())
    }

    @Test
    fun testLoadComponents() {
        val registry = component { }.declarationRegistry

        val component = component {
            modules(
                module {
                    factory { TestDep1() }
                    factory { TestDep2(get()) }
                }
            )
        }

        registry.loadComponents(component)

        assertEquals(
            registry.getAllDeclarations(),
            component.declarationRegistry.getAllDeclarations()
        )
    }

    @Test
    fun testSaveDeclaration() {
        val registry = component { }.declarationRegistry
        val declaration =
            Declaration.create(TestDep1::class, null, Declaration.Kind.FACTORY) { TestDep1() }
        assertEquals(null, registry.findDeclaration(TestDep1::class))
        registry.saveDeclaration(declaration.key, declaration)
        assertEquals(declaration, registry.findDeclaration(TestDep1::class))
    }

    @Test
    fun testAllowsValidOverride() {
        val registry = component { }.declarationRegistry
        val declaration1 =
            Declaration.create(TestDep1::class, null, Declaration.Kind.FACTORY) { TestDep1() }
        val declaration2 =
            Declaration.create(TestDep1::class, null, Declaration.Kind.FACTORY) { TestDep1() }
                .apply { override = true }

        val throwed = try {
            registry.saveDeclaration(declaration1.key, declaration1)
            registry.saveDeclaration(declaration2.key, declaration2)
            false
        } catch (e: OverrideException) {
            true
        }

        assertFalse(throwed)
    }

    @Test
    fun testThrowsOnInvalidOverride() {
        val registry = component { }.declarationRegistry
        val declaration1 =
            Declaration.create(TestDep1::class, null, Declaration.Kind.FACTORY) { TestDep1() }
        val declaration2 =
            Declaration.create(TestDep1::class, null, Declaration.Kind.FACTORY) { TestDep1() }

        val throwed = try {
            registry.saveDeclaration(declaration1.key, declaration1)
            registry.saveDeclaration(declaration2.key, declaration2)
            false
        } catch (e: OverrideException) {
            true
        }

        assertTrue(throwed)
    }

    @Test
    fun testGetEagerInstance() {
        val module = module {
            factory { TestDep1() }
            single(createOnStart = true) { TestDep2(TestDep1()) }
        }

        val eagerInstances = module.declarations.values.filter { it.createOnStart }.toSet()

        val registry = component {
            modules(module)
        }.declarationRegistry

        assertEquals(eagerInstances, registry.getEagerInstances())
    }

}