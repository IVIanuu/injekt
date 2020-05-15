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

    @Test
    fun testParentChildComposition() = codegen(
        """
                interface AppComponent
                
                @CompositionFactory 
                fun appComponentFactory(): AppComponent {
                    return createImpl()
                }
                
                @Module
                fun appModule() {
                    installIn<AppComponent>()
                    scoped { Foo() }
                }
                
                class App { 
                    val component = compositionFactoryOf<AppComponent, () -> AppComponent>()()
                    private val foo: Foo by inject()
                    init {
                        component.inject(this)
                        Activity(this)
                    }
                }

                interface ActivityComponent

                @CompositionFactory
                fun activityComponentFactory(): ActivityComponent {
                    parent<AppComponent>()
                    scoped { Bar(get()) }
                    return createImpl()
                }
                
                class Activity(private val app: App) {
                    val component = app.component.get<@ChildFactory () -> ActivityComponent>()() 
                    private val foo: Foo by inject()
                    private val bar: Bar by inject()
                    init {
                        component.inject(this)
                        Fragment(this)
                    }
                }
                
                interface FragmentComponent

                @CompositionFactory
                fun fragmentComponentFactory(): FragmentComponent {
                    parent<ActivityComponent>()
                    scoped { Baz(get(), get()) }
                    return createImpl()
                }
                
                class Fragment(private val activity: Activity) {
                    private val component = activity.component.get<@ChildFactory () -> FragmentComponent>()()
                    private val foo: Foo by inject()
                    private val bar: Bar by inject() 
                    private val baz: Baz by inject()
                    init {
                        component.inject(this)
                    }
                }
                
                fun invoke() {
                    generateCompositions()
                    App()
                }
                """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testParentMultiChildComposition() = multiCodegen(
        listOf(
            source(
                """
                interface AppComponent
                
                @CompositionFactory 
                fun appComponentFactory(): AppComponent {
                    return createImpl()
                }
                
                @Module
                fun appModule() {
                    installIn<AppComponent>()
                    scoped { Foo() }
                }

                interface ActivityComponent

                @CompositionFactory
                fun activityComponentFactory(): ActivityComponent {
                    parent<AppComponent>()
                    scoped { Bar(get()) }
                    return createImpl()
                }
                
                class App { 
                    val component = compositionFactoryOf<AppComponent, () -> AppComponent>()
                    private val foo: Foo by inject()
                    init {
                        component.inject(this)
                    }
                }
                """
            )
        ),
        listOf(
            source(
                """
                class Activity(private val app: App) {
                    val component = app.component.get<@ChildFactory () -> ActivityComponent>() 
                    private val foo: Foo by inject()
                    private val bar: Bar by inject()
                    init {
                        component.inject(this)
                    }
                }
                
                interface FragmentComponent

                @CompositionFactory
                fun fragmentComponentFactory(): FragmentComponent {
                    parent<ActivityComponent>()
                    scoped { Baz(get(), get()) }
                    return createImpl()
                }
                
                class Fragment(private val activity: Activity) {
                    private val component = activity.component.get<@ChildFactory () -> FragmentComponent>()
                    private val foo: Foo by inject()
                    private val bar: Bar by inject() 
                    private val baz: Baz by inject()
                    init {
                        component.inject(this)
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
