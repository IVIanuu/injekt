package com.ivianuu.injekt.frontend

import com.ivianuu.injekt.assertCompileError
import com.ivianuu.injekt.codegen
import org.junit.Test

class AnnotatedBindingTest {

    @Test
    fun testAnnotatedClassOk() = codegen(
        """ 
        @Transient class Dep
    """
    )

    @Test
    fun testAnnotatedObjectOk() = codegen(
        """ 
        @Transient object Dep
    """
    )

    @Test
    fun testAnnotatedAbstractClassFails() = codegen(
        """ 
        @Transient abstract class Dep
    """
    ) {
        assertCompileError("abstract")
    }

    @Test
    fun testAnnotatedInterfaceFails() = codegen(
        """ 
        @Transient interface Dep
    """
    ) {
        assertCompileError("abstract")
    }

    @Test
    fun testAnnotatedAnnotationClassFails() = codegen(
        """ 
        @Transient interface Dep
    """
    ) {
        assertCompileError("abstract")
    }

}