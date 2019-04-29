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

package com.ivianuu.injekt.eager

import com.ivianuu.injekt.module
import com.ivianuu.injekt.modules
import com.ivianuu.injekt.util.TestDep1
import junit.framework.Assert.assertTrue
import org.junit.Test

class EagerTest {

    @Test
    fun testCreatesOnInit() {
        var called = false

        eagerComponent {
            modules(
                module {
                    eager { called = true }
                }
            )
        }

        assertTrue(called)
    }

    @Test
    fun testCreatesOnlyOnce() {
        val component = eagerComponent {
            modules(
                module {
                    eager { TestDep1() }
                }
            )
        }

        val instance = component.instances.first()

        val value1 = instance.get(component.context)
        val value2 = instance.get(component.context)

        assertTrue(value1 === value2)
    }

}