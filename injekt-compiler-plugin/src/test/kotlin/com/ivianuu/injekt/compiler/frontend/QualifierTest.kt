package com.ivianuu.injekt.compiler.frontend

import com.ivianuu.injekt.compiler.assertCompileError
import com.ivianuu.injekt.compiler.assertOk
import com.ivianuu.injekt.compiler.codegen
import org.junit.Test

class QualifierTest {

    @Test
    fun testScopeNeedsRuntimeRetention() = codegen(
        """
        @Retention(AnnotationRetention.SOURCE)
        @Target(AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
        @Qualifier
        annotation class MyQualifier
    """
    ) {
        assertCompileError("runtime")
    }

    @Test
    fun testSuppressesExpressionSourceRetentionWarning() = codegen(
        """
        @Target(AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
        @Qualifier
        annotation class MyQualifier
        """
    ) {
        assertOk()
    }

}
