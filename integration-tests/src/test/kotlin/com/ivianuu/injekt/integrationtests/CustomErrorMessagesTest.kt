package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.*
import org.junit.*

class CustomErrorMessagesTest {
    @Test
    fun testShowsCustomGivenNotFoundMessageOnClass() = singleAndMultiCodegen(
        """
            @GivenNotFound("custom message [T]")
            class Dep<T>
        """,
        """
           fun invoke() = given<Dep<String>>() 
        """
    ) {
        compilationShouldHaveFailed("custom message kotlin.String")
    }

    @Test
    fun testShowsCustomGivenNotFoundMessageOnTypeAlias() = singleAndMultiCodegen(
        """
            @GivenNotFound("custom message [T]")
            typealias Dep<T> = String
        """,
        """
           fun invoke() = given<Dep<String>>() 
        """
    ) {
        compilationShouldHaveFailed("custom message kotlin.String")
    }

    @Test
    fun testShowsCustomGivenNotFoundMessageOnQualifier() = singleAndMultiCodegen(
        """
            @GivenNotFound("custom message [T] [${"\\$"}QT]")
            @Qualifier
            annotation class MyQualifier<T>
        """,
        """
           fun invoke() = given<@MyQualifier<String> Foo>() 
        """
    ) {
        compilationShouldHaveFailed("custom message kotlin.String com.ivianuu.injekt.test.Foo")
    }

    @Test
    fun testShowsCustomGivenNotFoundMessageOnParameter() = singleAndMultiCodegen(
        """
            fun <T> func(@Given @GivenNotFound("custom message [T]") value: T) {
            }
        """,
        """
           fun invoke() = func<String>()
        """
    ) {
        compilationShouldHaveFailed("custom message kotlin.String")
    }

    @Test
    fun testShowsCustomGivenAmbiguousMessage() = singleAndMultiCodegen(
        """
            @GivenAmbiguous("custom message [T]")
            @Given fun <T> amb1(): T = TODO()
            @Given fun <T> amb2(): T = TODO()
        """,
        """
           fun invoke() = given<String>() 
        """
    ) {
        compilationShouldHaveFailed("custom message kotlin.String")
    }
}
