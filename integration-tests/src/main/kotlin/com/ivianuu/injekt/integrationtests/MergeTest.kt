package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import org.junit.Test

class MergeTest {

    @Test
    fun testSimpleMerge() = codegen(
        """
            @Module(TestMergeComponent::class)
            object MyModule {
                @Given
                fun foo() = Foo()
            }
            
            @EntryPoint(TestMergeComponent::class)
            interface MyEntryPoint {
                val foo: Foo
            }
            
            @MergeFactory
            typealias MyMergeFactory = () -> TestMergeComponent
            
            fun invoke(): Foo {
                val component = rootMergeFactory<MyMergeFactory>()()
                val entryPoint = component.entryPoint<MyEntryPoint>()
                return entryPoint.foo
            }
        """
    ) {
        invokeSingleFile()
    }

}