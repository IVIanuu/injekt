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

/**
class ModuleTest {

    @Test
    fun testAllowsValidOverride() {
        val throwed = try {
            component {
                modules(
                    module {
                        single { TestDep1() }
                        single(override = true) { TestDep1() }
                    }
                )
            }
            false
        } catch (e: OverrideException) {
            true
        }

        assertFalse(throwed)
    }

    @Test
    fun testThrowsOnInvalidOverride() {
        val throwed = try {
            component {
                modules(
                    module {
                        single { TestDep1() }
                        single { TestDep1() }
                    }
                )
            }
            false
        } catch (e: OverrideException) {
            true
        }

        assertTrue(throwed)
    }

}*/