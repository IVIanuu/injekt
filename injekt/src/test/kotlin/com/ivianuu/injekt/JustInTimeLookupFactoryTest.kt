/*
 * Copyright 2019 Manuel Wrage
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

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNull
import org.junit.Test

class JustInTimeLookupFactoryTest {

    @Test
    fun testUnscoped() {
        factories.forEach { factory ->
            val lookup = factory.findBindingForKey<MyUnscopedDep>(keyOf<MyUnscopedDep>())
            assertNotNull(lookup)
            assertNull(lookup!!.scope)
        }
    }

    @Test
    fun testScoped() {
        factories.forEach { factory ->
            val lookup = factory.findBindingForKey<MyScopedDep>(keyOf<MyScopedDep>())
            assertNotNull(lookup)
            assertEquals(TestScopeOne, lookup!!.scope)
        }
    }

    @Test
    fun testCannotResolveNamed() {
        factories.forEach { factory ->
            val lookup = factory.findBindingForKey<MyUnscopedDep>(keyOf<MyUnscopedDep>("name"))
            assertNull(lookup)
        }
    }
}

@Factory
class MyUnscopedDep

object MyUnscopedDep__Binding : LinkedBinding<MyUnscopedDep>() {
    override fun invoke(parameters: ParametersDefinition?) = MyUnscopedDep()
}

@TestScopeOne
@Single
class MyScopedDep

object MyScopedDep__Binding : LinkedBinding<MyScopedDep>(), HasScope {
    override val scope: Any
        get() = TestScopeOne

    override fun invoke(parameters: ParametersDefinition?) = MyScopedDep()
}

private val factories = listOf(
    CodegenJustInTimeLookupFactory,
    ReflectiveJustInTimeLookupFactory,
    DefaultJustInTimeLookupFactory
)
