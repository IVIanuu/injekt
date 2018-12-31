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
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

class CreateOnStartTest {

    @Test
    fun testEagerSingleIsCreatedAtStart() {
        val component = component {
            modules(
                module {
                    single(createOnStart = true) { TestDep1() }
                }
            )
        }

        val declaration = component.declarationRegistry.findDeclaration(TestDep1::class)
            ?: error("no declaration found")

        assertTrue(declaration.createOnStart)
        assertTrue(declaration.instance.isCreated)
    }

    @Test
    fun testNonEagerSingleIsNotCreatedAtStart() {
        val component = component {
            modules(
                module {
                    single(createOnStart = false) { TestDep1() }
                }
            )
        }

        val declaration = component.declarationRegistry.findDeclaration(TestDep1::class)
            ?: error("no declaration found")

        assertFalse(declaration.createOnStart)
        assertFalse(declaration.instance.isCreated)
    }

    @Test
    fun testFactoryIsNotCreatedAtStart() {
        val component = component {
            modules(
                module {
                    factory { TestDep1() }
                }
            )
        }

        val declaration = component.declarationRegistry.findDeclaration(TestDep1::class)
            ?: error("no declaration found")

        assertFalse(declaration.createOnStart)
        assertFalse(declaration.instance.isCreated)
    }

    @Test
    fun testDeferEagerInstances() {
        val component = component(createEagerInstances = false) {
            modules(
                module {
                    single(createOnStart = true) { TestDep1() }
                }
            )
        }

        val declaration = component.declarationRegistry.findDeclaration(TestDep1::class)
            ?: error("no declaration found")

        assertFalse(declaration.instance.isCreated)

        component.createEagerInstances()

        assertTrue(declaration.instance.isCreated)
    }

}