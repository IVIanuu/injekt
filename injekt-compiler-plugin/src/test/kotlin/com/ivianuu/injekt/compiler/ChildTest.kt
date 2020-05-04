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
            val component = parent.component.childFactory(this).also { it.child }
            init {
                Baby(this)
            }
        }
        
        class Baby(val child: Child) {  
            val component = child.component.babyFactory(this).also { it.baby }
        }
        
        interface ParentComponent {
            val parent: Parent
            val childFactory: @ChildFactory (Child) -> ChildComponent
        }

        interface ChildComponent {
            val child: Child
            val parent: Parent 
            val babyFactory: @ChildFactory (Baby) -> BabyComponent
        }
        
        interface BabyComponent { 
            val child: Child
            val parent: Parent
            val baby: Baby
        }
        
        @Factory
        fun createParent(parent: Parent): ParentComponent = createImplementation {
            instance(parent)
            childFactory(::createChild)
        }
        
        @ChildFactory
        fun createChild(child: Child): ChildComponent = createImplementation {
            instance(child)
            childFactory(::createBaby)
        }
        
        @ChildFactory
        fun createBaby(baby: Baby): BabyComponent = createImplementation {
            instance(baby)
        }
        
        fun invoke() = Parent()
    """
    ) {
        invokeSingleFile()
    }

}