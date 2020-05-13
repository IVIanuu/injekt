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
        fun createParent(parent: Parent): ParentComponent {
            instance(parent)
            childFactory(::createChild)
            return createImpl()
        }
        
        @ChildFactory
        fun createChild(child: Child): ChildComponent {
            instance(child)
            childFactory(::createBaby)
            return createImpl()
        }
        
        @ChildFactory
        fun createBaby(baby: Baby): BabyComponent {
            instance(baby)
            return createImpl()
        }
        
        fun invoke() = Parent()
    """
    ) {
        invokeSingleFile()
    }

}