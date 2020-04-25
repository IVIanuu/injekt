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

import junit.framework.Assert.assertSame
import org.junit.Test

class AliasTest {

    @Test
    fun testAlias() {
        val component = Component {
            com.ivianuu.injekt.single { TestDep1() }
            com.ivianuu.injekt.alias<TestDep1, Any>()
        }

        val declared = component.get<TestDep1>()
        val aliased = component.get<Any>()
        assertSame(declared, aliased)
    }

    @Test
    fun testAliasQualifier() {
        val component = Component {
            com.ivianuu.injekt.single { TestDep1() }
            alias<TestDep1>(aliasQualifier = TestQualifier1)
        }

        val declared = component.get<TestDep1>()
        val aliased = component.get<TestDep1>(qualifier = TestQualifier1)
        assertSame(declared, aliased)
    }
}
