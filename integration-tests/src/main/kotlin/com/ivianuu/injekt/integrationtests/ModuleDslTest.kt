package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.codegen
import org.junit.Test

class ModuleDslTest {

    @Test
    fun testModuleWithWrongTargetFails() = codegen(
        """
        @Module(Any::class)
        class MyModule
    """
    ) {
        assertCompileError("component")
    }

    @Test
    fun testModuleWithTargetAndNotObjectOrEmptyConstructorFails() = codegen(
        """
        @Module(TestComponent::class)
        class MyModule(val value: String)
    """
    ) {
        assertCompileError("constructor")
    }

}