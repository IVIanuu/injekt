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

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.setGenerateMergeComponents
import org.junit.Test

class MergeTest {

    @Test
    fun testMergeComponent() = codegen(
        """
            @MergeComponent
            abstract class MyComponent
            
            @Binding fun foo() = Foo()
            
            @MergeInto(MyComponent::class)
            interface FooComponent {
                val foo: Foo
            }
            
            fun invoke() {
                val component = component<MyComponent>()
                val fooComponent = component.mergeComponent<FooComponent>()
                fooComponent.foo
            }
        """,
        config = { setGenerateMergeComponents(true) }
    )

    @Test
    fun testMergeChildComponent() = codegen(
        """
            @Component abstract class MyParentComponent {
                abstract val myChildComponentFactory: () -> MyChildComponent
            }
            
            @MergeChildComponent
            abstract class MyChildComponent
            
            @Binding fun foo() = Foo()
            
            @MergeInto(MyChildComponent::class)
            interface FooComponent {
                val foo: Foo
            }
            
            fun invoke() {
                val parentComponent = component<MyParentComponent>()
                val childComponent = parentComponent.myChildComponentFactory()
                val fooComponent = childComponent.mergeComponent<FooComponent>()
                fooComponent.foo
            }
        """,
        config = { setGenerateMergeComponents(true) }
    )

    @Test
    fun testMergeComponentObjectModule() = codegen(
        """
            @MergeComponent
            abstract class MyComponent
            
            @Binding fun foo() = Foo()
            
            @MergeInto(MyComponent::class)
            interface FooComponent {
                val foo: Foo
            }
            
            fun invoke() {
                val component = component<MyComponent>()
                val fooComponent = component.mergeComponent<FooComponent>()
                fooComponent.foo
            }
        """,
        config = { setGenerateMergeComponents(true) }
    )

}
