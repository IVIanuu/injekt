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
import junit.framework.Assert.assertTrue
import org.junit.Test

class BehaviorTest {

    @Test
    fun testApply() {
        var applied = false
        val behavior = object : Behavior.Element {
            override fun <T> apply(provider: BindingProvider<T>): BindingProvider<T> {
                applied = true
                return provider
            }
        }

        Binding(
            keyOf(),
            behavior
        ) { "" }

        assertTrue(applied)
    }

    @Test
    fun testMultipleApply() {
        val appliedBehaviors = mutableListOf<Int>()
        val behavior = (1..3)
            .map { behavior ->
                object : Behavior.Element {
                    override fun <T> apply(provider: BindingProvider<T>): BindingProvider<T> {
                        appliedBehaviors += behavior
                        return provider
                    }
                } as Behavior
            }
            .reduceRight { p, acc -> p + acc }

        Binding(
            keyOf(),
            behavior
        ) { "" }

        assertEquals(listOf(1, 2, 3), appliedBehaviors)
    }
}
