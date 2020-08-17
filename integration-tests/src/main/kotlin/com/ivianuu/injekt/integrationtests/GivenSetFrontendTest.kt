package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.codegen
import org.junit.Test

class GivenSetFrontendTest {

    @Test
    fun testGivenSetPropertyWithNonGivenSetReturnTypeFails() = codegen(
        """
        @GivenSet
        class Outer { 
            @GivenSet val inner = Inner()
            
            class Inner
        }
    """
    ) {
        assertCompileError("not a @GivenSet")
    }

    @Test
    fun testGivenSetFunctonWithNonGivenSetReturnTypeFails() = codegen(
        """
        @GivenSet
        class Outer { 
            @GivenSet fun inner() = Inner()
            
            class Inner
        }
    """
    ) {
        assertCompileError("not a @GivenSet")
    }

}