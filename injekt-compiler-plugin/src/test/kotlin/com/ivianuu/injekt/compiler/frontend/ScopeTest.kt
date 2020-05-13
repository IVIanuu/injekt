package com.ivianuu.injekt.compiler.frontend

import com.ivianuu.injekt.compiler.assertCompileError
import com.ivianuu.injekt.compiler.codegen
import org.junit.Test

class ScopeTest {

    @Test
    fun testScopeNeedsRuntimeRetention() = codegen(
        """
        @Retention(AnnotationRetention.SOURCE)
        @Scope
        annotation class MyScope
    """
    ) {
        assertCompileError("runtime")
    }

}