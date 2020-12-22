package com.ivianuu.injekt.integrationtests

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
        assertEquals("kotlin.String", invokeSingleFile())
    }

    @Test
    fun testForKeyTypeParameter() = codegen(
        """
            inline fun <@ForKey T> listKeyOf() = keyOf<List<T>>()
            fun invoke() = listKeyOf<String>() 
        """
    ) {
        assertEquals("kotlin.collections.List<kotlin.String>", invokeSingleFile())
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
        assertEquals("kotlin.collections.List<kotlin.String>", it.last().invokeSingleFile())
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