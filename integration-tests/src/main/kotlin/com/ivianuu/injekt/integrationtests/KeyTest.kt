package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.common.Key
import com.ivianuu.injekt.test.*
import junit.framework.Assert.assertEquals
import org.junit.Test

class KeyTest {

    @Test
    fun testKeyOf() = codegen(
        """
           fun invoke() = keyOf<String>() 
        """
    ) {
        assertEquals("kotlin.String",
            invokeSingleFile<Key<String>>().value)
    }

    @Test
    fun testForKeyTypeParameter() = codegen(
        """
            inline fun <@ForKey T> listKeyOf() = keyOf<List<T>>()
            fun invoke() = listKeyOf<String>() 
        """
    ) {
        assertEquals("kotlin.collections.List<kotlin.String>",
            invokeSingleFile<Key<List<String>>>().value)
    }

    @Test
    fun testForKeyTypeParameterInInterface() = codegen(
        """
            interface KeyFactory {
                fun <@ForKey T> listKeyOf(): Key<List<T>>
                companion object : KeyFactory {
                    override fun <@ForKey T> listKeyOf() = keyOf<List<T>>()
                }
            }
            fun invoke() = KeyFactory.listKeyOf<String>() 
        """
    ) {
        assertEquals("kotlin.collections.List<kotlin.String>",
            invokeSingleFile<Key<List<String>>>().value)
    }

    @Test
    fun testForKeyTypeParameterMulti() = multiCodegen(
        listOf(
            source(
                """
                    inline fun <@ForKey T> listKeyOf() = keyOf<List<T>>()
                """
            )
        ),
        listOf(
            source(
                """
                    fun invoke() = listKeyOf<String>()
                """,
                name = "File.kt"
            )
        )
    ) {
        assertEquals("kotlin.collections.List<kotlin.String>",
            it.last().invokeSingleFile<Key<List<String>>>().value
        )
    }

    @Test
    fun testNonForKeyTypeParameterCannotBeUsedForForKey() = codegen(
        """
           fun <T> invoke() = keyOf<T>() 
        """
    ) {
        assertCompileError()
    }

}