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
        fun createParent(parent: Parent): ParentComponent {
            transient { parent }
            childFactory(::createChild)
            return create()
        }
        
        @ChildFactory
        fun createChild(child: Child): ChildComponent {
            transient { child }
            return create()
        }
        
        fun invoke() = Parent()
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testMultiNesting() = codegen(
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
            transient { parent }
            childFactory(::createChild)
            return create()
        }
        
        @ChildFactory
        fun createChild(child: Child): ChildComponent {
            transient { child }
            childFactory(::createBaby)
            return create()
        }
        
        @ChildFactory
        fun createBaby(baby: Baby): BabyComponent {
            transient { baby }
            return create()
        }
        
        fun invoke() = Parent()
    """
    ) {
        invokeSingleFile()
    }

}