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

package com.ivianuu.injekt

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertTrue
import org.junit.Test

class JustInTimeLookupFactoryTest {

    @Test
    fun testUnscoped() {
        val binding =
            CodegenJustInTimeLookupFactory.findBinding<MyUnscopedDep>(typeOf())
        assertNotNull(binding)
        assertEquals(Scoping.Unscoped, binding?.scoping)
    }

    @Test
    fun testScoped() {
        val binding =
            CodegenJustInTimeLookupFactory.findBinding<MyScopedDep>(typeOf())
        assertNotNull(binding)
        assertTrue(binding?.scoping is Scoping.Scoped)
        assertEquals(TestScopeOne, (binding?.scoping as? Scoping.Scoped)?.name)
    }

}

@Factory
class MyUnscopedDep

@TestScopeOne
@Single
class MyScopedDep
