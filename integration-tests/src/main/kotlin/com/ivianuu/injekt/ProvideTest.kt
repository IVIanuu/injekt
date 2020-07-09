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

import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.codegen
import org.junit.Test

class ProvideTest {

    @Test
    fun testAnnotatedClassOk() = codegen(
        """ 
        @Unscoped class Dep
    """
    )

    @Test
    fun testAnnotatedObjectOk() = codegen(
        """ 
        @Unscoped object Dep
    """
    )

    @Test
    fun testAnnotatedAbstractClassFails() = codegen(
        """ 
        @Unscoped abstract class Dep
    """
    ) {
        assertCompileError("abstract")
    }

    @Test
    fun testAnnotatedInterfaceFails() = codegen(
        """ 
        @Unscoped interface Dep
    """
    ) {
        assertCompileError("abstract")
    }

    @Test
    fun testAnnotatedAnnotationClassFails() = codegen(
        """ 
        @Unscoped interface Dep
    """
    ) {
        assertCompileError("abstract")
    }

    @Test
    fun testClassAndConstructorAnnotationFails() = codegen(
        """
         @Unscoped class Dep @Unscoped constructor()  
        """
    ) {
        assertCompileError("either")
    }

    @Test
    fun testMultipleConstructorsWithAnnotationsFails() = codegen(
        """
            class Dep {
                @Unscoped constructor(foo: Foo)
                @Unscoped constructor(bar: Bar)
            }
        """
    ) {
        assertCompileError("1")
    }

    @Test
    fun testMultipleConstructorsWithoutAnnotationsFails() = codegen(
        """
            @Unscoped
            class Dep { 
                constructor(foo: Foo)
                constructor(bar: Bar)
            }
        """
    ) {
        assertCompileError("choose")
    }

}
