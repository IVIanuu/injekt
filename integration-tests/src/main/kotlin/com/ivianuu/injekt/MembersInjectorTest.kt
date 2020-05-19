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

class MembersInjectorTest {

    @Test
    fun testMembersInjector() = codegen(
        """
        abstract class SuperClass { 
            val foo: Foo by inject()
        }
        class MyClass : SuperClass() { 
            val bar: Bar by inject()
        }
        
        interface TestComponent { 
            val injectMyClass: @MembersInjector (MyClass) -> Unit
            val bar: Bar
        }
        
        @Factory
        fun createComponent(): TestComponent {
            transient { Foo() }
            scoped { Bar(get()) }
            return create()
        }
        
        fun invoke() { 
            val testComponent = createComponent()
            val myClass = MyClass()
            testComponent.injectMyClass(myClass)
            check(myClass.bar === testComponent.bar)
        }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testCanInjectMembersInjectorForAnyType() = codegen(
        """
        @InstanceFactory
        fun createComponent(): @MembersInjector (Any) -> Unit {
            return create()
        }
    """
    )

    @Test
    fun testInjectsMembersOnConstructorInjection() = codegen(
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

}
