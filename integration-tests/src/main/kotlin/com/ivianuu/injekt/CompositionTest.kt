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

import org.junit.Test

class CompositionTest {

    @Test
    fun testSimpleComposition() = multiCodegen(
        listOf(
            source(
                """
                @CompositionFactory 
                fun factory(): TestComponent {
                    return createImpl()
                }
                
                class FooEntryPointConsumer(testComponent: TestComponent) {
                    init {
                        testComponent.get<Foo>()
                    }
                }
                """
            )
        ),
        listOf(
            source(
                """
                interface BarEntryPoint {
                    val bar: Bar
                }
                
                @Module
                fun fooBarModule() {
                    installIn<TestComponent>()
                    transient<Foo>()
                    transient<Bar>()
                }
                
                class BarEntryPointConsumer(private val testComponent: TestComponent) { 
                    private val bar: Bar by inject()
                    fun doInject() {
                        testComponent.inject(this)
                        println(bar)
                    }
                }
                
                fun main() {
                    generateCompositions()
                }
                """
            )
        )
    )

}
