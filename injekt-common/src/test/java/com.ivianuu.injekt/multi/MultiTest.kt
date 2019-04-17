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

package com.ivianuu.injekt.multi

import com.ivianuu.injekt.component
import com.ivianuu.injekt.get
import com.ivianuu.injekt.module
import com.ivianuu.injekt.parametersOf
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

data class MultiValue(val value: Int)

class MultiTest {

    @Test
    fun testMultiValues() {
        val component = component {
            module {
                multi { (value: Int) -> MultiValue(value) }
            }
        }

        val firstValueOne = component.get<MultiValue> { parametersOf(1) }
        val secondValueOne = component.get<MultiValue> { parametersOf(1) }

        assertTrue(firstValueOne === secondValueOne)

        val valueTwo = component.get<MultiValue> { parametersOf(2) }

        assertFalse(firstValueOne === valueTwo)
    }

    @Test
    fun testThrowsOnNullParams() {
        val component = component {
            module {
                multi { (value: Int) -> MultiValue(value) }
            }
        }

        val throwed = try {
            component.get<MultiValue>()
            false
        } catch (e: Exception) {
            true
        }

        assertTrue(throwed)
    }

}