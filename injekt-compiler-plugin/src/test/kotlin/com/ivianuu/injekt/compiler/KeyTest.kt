package com.facebook.buck.jvm.java.javax.com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.codegen
import com.ivianuu.injekt.compiler.invokeSingleFile
import junit.framework.Assert.assertSame
import org.junit.Test

class KeyTest {

    @Test
    fun testKeyCaching() = codegen(
        """
        fun invoke() = keyOf<String>()
        """
    ) {
        assertSame(
            invokeSingleFile(),
            invokeSingleFile()
        )
    }

    @Test
    fun testKeyOfTransform() = codegen(
        """
        fun invoke() = keyOf<String>()
        """
    )

}
