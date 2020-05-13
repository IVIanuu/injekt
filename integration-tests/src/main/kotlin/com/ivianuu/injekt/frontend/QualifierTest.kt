package com.ivianuu.injekt.frontend

import com.ivianuu.injekt.assertCompileError
import com.ivianuu.injekt.assertOk
import com.ivianuu.injekt.codegen
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
    fun testSuppressesExpressionSourceRetentionWarning() =
        codegen(
            """
        @Target(AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
        @Qualifier
        annotation class MyQualifier
        """
        ) {
            assertOk()
        }

}
