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

package com.ivianuu.injekt.common.multibinding

import com.ivianuu.injekt.common.NameOne
import com.ivianuu.injekt.common.NameThree
import com.ivianuu.injekt.common.NameTwo
import com.ivianuu.injekt.common.Values
import com.ivianuu.injekt.component
import com.ivianuu.injekt.factoryBuilder
import com.ivianuu.injekt.module
import com.ivianuu.injekt.modules
import com.ivianuu.injekt.multibinding.bindIntoSet
import com.ivianuu.injekt.multibinding.getSet
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test

class SetTest {

    @Test
    fun testSetMultiBinding() {
        val component = component {
            modules(
                module {
                    factoryBuilder<String>(NameOne) {
                        definition { "value_one" }
                        bindIntoSet(Values)
                    }
                    factoryBuilder<String>(NameTwo) {
                        definition { "value_two" }
                        bindIntoSet(Values)
                    }
                    factoryBuilder<String>(NameThree) {
                        definition { "value_three" }
                        bindIntoSet(Values)
                    }
                }
            )
        }

        val set = component.getSet<String>(Values)

        assertEquals(3, set.size)
        assertTrue(set.contains("value_one"))
        assertTrue(set.contains("value_two"))
        assertTrue(set.contains("value_three"))
    }

}