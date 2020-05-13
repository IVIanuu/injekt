package com.ivianuu.injekt.frontend

import com.ivianuu.injekt.assertCompileError
import com.ivianuu.injekt.codegen
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