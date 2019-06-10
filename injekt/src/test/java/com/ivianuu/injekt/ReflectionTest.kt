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

import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

@Name(PackageName.Companion::class)
annotation class PackageName {
    companion object
}

class ReflectionDep

class ReflectionDepWithParam(@Param val value: Int)

class ReflectionDepWithNamedParam(@PackageName val packageName: String)

class ReflectionDepWithAtInjectConstructor {

    val arg: Any?

    constructor(testDep1: TestDep1) {
        arg = testDep1
    }

    @Inject
    constructor(testDep2: TestDep2) {
        arg = testDep2
    }

}

interface Memoized<T>

class ReflectionDepWithParameterizedDep(
    private val mapOfStrings: Map<String, Memoized<String>>
)

@TestScope
class ReflectionDepWithScope

@Scope
annotation class OtherScope

class ReflectionTest {

    @Test
    fun testCreatesViaReflection() {
        val component = component()
        component.get<ReflectionDep>()
    }

    @Test
    fun testUsesParams() {
        val component = component()
        component.get<ReflectionDepWithParam> { parametersOf(1) }
    }

    @Test
    fun testUsesNamedParams() {
        val component = component {
            modules(
                module {
                    factory(PackageName) { "com.ivianuu.injekt" }
                }
            )
        }

        component.get<ReflectionDepWithNamedParam>()
    }

    @Test
    fun testUsesScope() {
        val testScopeComponent = component {
            scopes(TestScope::class)
        }

        val component = component {
            scopes(OtherScope::class)
            dependencies(testScopeComponent)
        }

        component.get<ReflectionDepWithScope>()

        assertTrue(testScopeComponent.bindings.containsKey(keyOf<ReflectionDepWithScope>()))
        assertFalse(component.bindings.containsKey(keyOf<ReflectionDepWithScope>()))
    }

    @Test
    fun testUsesAtInjectConstructor() {
        val component = component()
        assertTrue(component.get<ReflectionDepWithAtInjectConstructor>().arg is TestDep2)
    }

    @Test
    fun testResolversParameterizedDeps() {
        val component = component {
            modules(
                module {
                    map<String, Memoized<String>>()
                }
            )
        }

        component.get<ReflectionDepWithParameterizedDep>()
    }
}