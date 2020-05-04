package com.ivianuu.injekt.compiler

import org.junit.Test

class ChildTest {

    @Test
    fun testParentChild() = codegen(
        """
        class Parent {
            val component = createParent(this).also { it.parent }
            init {
                Child(this)
            }
        }
        class Child(val parent: Parent) {
            init {
                parent.component.childFactory(this).also { it.child }
            }
        }
        
        interface ParentComponent {
            val parent: Parent
            val childFactory: @ChildFactory (Child) -> ChildComponent
        }

        interface ChildComponent {
            val child: Child
            val parent: Parent
        }
        
        @Factory
        fun createParent(parent: Parent): ParentComponent = createImplementation {
            instance(parent)
            childFactory(::createChild)
        }
        
        @ChildFactory
        fun createChild(child: Child): ChildComponent = createImplementation {
            instance(child)
        }
        
        fun invoke() = Parent()
    """
    ) {
        invokeSingleFile()
    }

}