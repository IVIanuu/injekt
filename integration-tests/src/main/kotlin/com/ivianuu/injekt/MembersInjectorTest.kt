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

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import org.junit.Test

class MembersInjectorTest {

    @Test
    fun testMembersInjector() = codegen(
        """
        abstract class SuperClass { 
            val foo: Foo by inject()
            lateinit var foo2: Foo
            @Inject
            fun injectFoo2(foo: Foo) {
                foo2 = foo
            }
        }
        class MyClass : SuperClass() { 
            val bar: Bar by inject()
            lateinit var bar2: Bar
            @Inject
            fun injectBar2(bar: Bar) {
                bar2 = bar
            }
        }
        
        interface TestComponent { 
            val injectMyClass: @MembersInjector (MyClass) -> Unit
            val foo: Foo
            val bar: Bar
        }
        
        @Factory
        fun createComponent(): TestComponent {
            scoped { Foo() }
            scoped { foo: Foo -> Bar(foo) }
            return create()
        }
        
        fun invoke() { 
            val testComponent = createComponent()
            val myClass = MyClass()
            testComponent.injectMyClass(myClass)
            check(myClass.foo === testComponent.foo) {
                "my class foo " + myClass.foo.toString() +
                "test component foo " + testComponent.foo.toString()
            }
            check(myClass.foo2 === testComponent.foo)
            check(myClass.bar === testComponent.bar)
            check(myClass.bar2 === testComponent.bar)
        }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testCanInjectMembersInjectorForAnyType() =
        codegen(
            """
        @InstanceFactory
        fun createComponent(): @MembersInjector (Any) -> Unit {
            return create()
        }
    """
        )

    @Test
    fun testInjectsMembersOnConstructorInjection() =
        codegen(
            """
        @Transient
        class ConstructorAndMembersDep(
            private val foo: Foo
        ) { 
            val bar: Bar by inject()
        }

        @InstanceFactory
        fun createDep(): ConstructorAndMembersDep {
            transient<Foo>()
            transient<Bar>()
            return create()
        }
        
        fun invoke() {
            val dep = createDep()
            dep.bar
        }
    """
        ) {
            invokeSingleFile()
        }

    @Test
    fun testMultiCompilationMembersInjector() = multiCodegen(
        listOf(
            source(
                """
                class MyClass { 
                    val foo: Foo by inject() 
                    lateinit var foo2: Foo
                    
                    @Inject 
                    fun injectFoo(foo: Foo) { 
                        foo2 = foo
                    }
                }
            """
            )
        ),
        listOf(
            source(
                """
                @InstanceFactory
                fun myClassFactory(): MyClass {
                    transient<Foo>()
                    transient<MyClass>()
                    return create()
                }
                
                fun invoke() {
                    val instance = myClassFactory()
                    instance.foo
                    instance.foo2
                }
            """,
                name = "File.kt"
            )
        )
    ) {
        it.last().invokeSingleFile()
    }

    @Test
    fun testMultiCompilationMembersInjectorWithSubclass() = multiCodegen(
        listOf(
            source("""
               abstract class RootClass {
                    val rootBar: Bar by inject()
                    private val foo: Foo by inject()
                    val rootFoo: Foo get() = foo
                } 
            """)
        ),
        listOf(
            source("""
               abstract class SuperClass : RootClass() {
                    val superBar: Bar by inject()
                    private val foo: Foo by inject()
                    val superFoo: Foo get() = foo
                } 
            """)
        ),
        listOf(
            source("""
                class MyClass : SuperClass() { 
                    val foo: Foo by inject() 
                    lateinit var foo2: Foo
                    
                    @Inject 
                    fun injectFoo(foo: Foo) { 
                        foo2 = foo
                    }
                }
            """
            )
        ),
        listOf(
            source(
                """
                @InstanceFactory
                fun myClassFactory(): MyClass {
                    transient<Foo>()
                    transient<Bar>()
                    transient<MyClass>()
                    return create()
                }
                
                fun invoke() {
                    val instance = myClassFactory()
                    instance.rootBar
                    instance.rootFoo
                    instance.superBar
                    instance.superFoo
                    instance.foo
                    instance.foo2
                }
            """,
                name = "File.kt"
            )
        )
    ) {
        it.last().invokeSingleFile()
    }

}
