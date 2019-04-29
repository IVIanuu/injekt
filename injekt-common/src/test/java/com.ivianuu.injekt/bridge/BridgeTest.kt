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

package com.ivianuu.injekt.bridge

import com.ivianuu.injekt.*
import org.junit.Assert.assertEquals
import org.junit.Test

class BridgeTest {

    @Test
    fun testDelegatesToOriginal() {
        val component = component {
            modules(
                module {
                    factory(named("original_name")) { "original_value" }
                    bridge<String>(named("original_name")) bindType CharSequence::class
                }
            )
        }

        assertEquals("original_value", component.get<CharSequence>())
    }

}