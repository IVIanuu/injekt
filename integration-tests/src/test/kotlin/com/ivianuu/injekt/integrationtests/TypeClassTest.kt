package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.*
import io.kotest.matchers.*
import org.junit.*

class TypeClassTest {
    @Test
    fun testExtensionWithFunction() = codegen(
        """
            import com.ivianuu.injekt.integrationtests.MyExtension.Companion.answer            

            @Extension interface MyExtension<T> {
                fun T.answer(): Int
            }

            @Given fun <T> myExtension() = object : MyExtension<T> {
                override fun T.answer() = 42
            } 

            fun invoke() = "".answer()
        """
    ) {
        invokeSingleFile() shouldBe 42
    }

    @Test
    fun testExtensionWithAdditionalTypeParameter() = codegen(
        """
            import com.ivianuu.injekt.integrationtests.MyExtension.Companion.answer            

            @Extension interface MyExtension<T> {
                fun <R> T.answer(): Int
            }

            @Given fun <T> myExtension() = object : MyExtension<T> {
                override fun <R> T.answer() = 42
            } 

            fun invoke() = "".answer<String, Long>()
        """
    ) {
        invokeSingleFile() shouldBe 42
    }

    @Test
    fun testExtensionWithInfixFunction() = codegen(
        """
            import com.ivianuu.injekt.integrationtests.MyExtension.Companion.answer            

            @Extension interface MyExtension<T> {
                infix fun T.answer(other: T): Int
            }

            @Given fun <T> myExtension() = object : MyExtension<T> {
                override fun T.answer(other: T) = 42
            } 

            fun invoke() = "" answer ""
        """
    ) {
        invokeSingleFile() shouldBe 42
    }

    @Test
    fun testExtensionWithOperatorFunction() = codegen(
        """
            import com.ivianuu.injekt.integrationtests.MyExtension.Companion.plus            

            @Extension interface MyExtension<T> {
                operator fun T.plus(other: T): Int
            }

            @Given fun <T> myExtension() = object : MyExtension<T> {
                override fun T.plus(other: T) = 42
            } 

            fun invoke() = Unit + Unit
        """
    ) {
        invokeSingleFile() shouldBe 42
    }

    @Test
    fun testExtensionWithSuspendFunction() = codegen(
        """
            import com.ivianuu.injekt.integrationtests.MyExtension.Companion.answer            

            @Extension interface MyExtension<T> {
                suspend fun T.answer(): Int
            }

            @Given fun <T> myExtension() = object : MyExtension<T> {
                override suspend fun T.answer() = 42
            } 

            fun invoke() = runBlocking { "".answer() }
        """
    ) {
        invokeSingleFile() shouldBe 42
    }
}
