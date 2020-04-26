package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.Key
import junit.framework.Assert.assertEquals
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

    @Test
    fun testKeyOverload() = codegen(
        """ 
            inline fun <reified T> keyOverload(qualifier: kotlin.reflect.KClass<*>? = null) = false
            fun <T> keyOverload(key: Key<T>) = true
            
            fun invoke() = keyOverload<String>()
        """
    ) {
        assertEquals(true, invokeSingleFile())
    }

    @Test
    fun testKeyOverloadInClass() = codegen(
        """ 
            class MyClass {
                fun <T> keyOverload(key: Key<T>) = true
                inline fun <reified T> keyOverload(qualifier: kotlin.reflect.KClass<*>? = null) = false
            }

            fun invoke() = MyClass().keyOverload<String>()
        """
    ) {
        assertEquals(true, invokeSingleFile())
    }

    @Test
    fun testKeyOverloadInClassWithExtension() = codegen(
        """ 
            class MyClass {
                fun <T> keyOverload(key: Key<T>) = true
            }
            inline fun <reified T> MyClass.keyOverload(qualifier: kotlin.reflect.KClass<*>? = null) = false

            fun invoke() = MyClass().keyOverload<String>()
        """
    ) {
        assertEquals(true, invokeSingleFile())
    }

    @Test
    fun testExtensionKeyOverload() = codegen(
        """ 
            class MyClass {
                fun <T> keyOverload(key: Key<T>) = true
            }
            inline fun <reified T> MyClass.keyOverload(qualifier: kotlin.reflect.KClass<*>? = null) = false

            fun invoke() = MyClass().keyOverload<String>()
        """
    ) {
        assertEquals(true, invokeSingleFile())
    }

    @Test
    fun testKeyOverloadFromDifferentCompilation() {
        codegen(
            """
                fun invoke() = KeyOverloadTestClass().keyOverload<String>()
            """
        ) {
            assertEquals(true, invokeSingleFile())
        }
    }

}

class KeyOverloadTestClass {
    fun <T> keyOverload(key: Key<T>) = true
    inline fun <reified T> keyOverload(qualifier: kotlin.reflect.KClass<*>? = null) = false
}