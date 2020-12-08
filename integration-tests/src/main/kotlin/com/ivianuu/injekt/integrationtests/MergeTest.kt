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
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.setGenerateMergeComponents
import com.ivianuu.injekt.test.source
import org.junit.Test

class MergeTest {

    @Test
    fun testMergeComponent() = codegen(
        """
            @MergeComponent abstract class MyComponent
            
            @Binding fun foo() = Foo()

            fun invoke() {
                val component = component<MyComponent>()
                component.get<Foo>()
            }
        """,
        config = { setGenerateMergeComponents(true) }
    ) {
        invokeSingleFile()
    }

    @Test
    fun testMergeComponentMulti() = multiCodegen(
        listOf(
            source(
                """
                        @MergeComponent abstract class MyComponent
        """)
        ),

        listOf(
            source(
                """
                    @Binding fun foo() = Foo()
                    fun invoke() {
                        val component = component<MyComponent>()
                        component.get<Foo>()        
                    }
                """,
                name = "File.kt"
            ),
        ),
        listOf(source("")),
        config = { if (it == 2) setGenerateMergeComponents(true) }
    ) {
        it.last().invokeSingleFile()
    }

    @Test
    fun testMergeChildComponent() = codegen(
        """
            @Component abstract class MyParentComponent {
                abstract val myChildComponentFactory: () -> MyChildComponent
            }
            
            @MergeChildComponent
            abstract class MyChildComponent
            
            @Binding fun foo() = Foo()
            
            fun invoke() {
                val parentComponent = component<MyParentComponent>()
                val childComponent = parentComponent.myChildComponentFactory()
                childComponent.get<Foo>()
            }
        """,
        config = { setGenerateMergeComponents(true) }
    ) {
        invokeSingleFile()
    }

}
