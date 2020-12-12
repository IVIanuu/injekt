package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertNotSame
import org.junit.Test

class QualifierTest {

    @Test
    fun testDistinctQualifier() = codegen(
        """
            @Target(AnnotationTarget.TYPE)
            @Qualifier
            annotation class MyQualifier

            @Given val foo = Foo()
            @Given val qualifiedFoo: @MyQualifier Foo = Foo()
       
            fun invoke(): Pair<Foo, Foo> {
                return given<Foo>() to given<@MyQualifier Foo>()
            }
            """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testDistinctQualifierAnnotationWithArguments() = codegen(
        """
            @Target(AnnotationTarget.TYPE)
            @Qualifier
            annotation class MyQualifier(val value: String)
            
            @Given val foo1: @MyQualifier("a") Foo = Foo()
            @Given val foo2: @MyQualifier("b") Foo = Foo()
       
            fun invoke(): Pair<Foo, Foo> {
                return given<@MyQualifier("a") Foo>() to given<@MyQualifier("b") Foo>()
            }
            """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testTypeParameterWithQualifierUpperBound() = codegen(
        """
            @Target(AnnotationTarget.TYPE)
            @Qualifier
            annotation class MyQualifier
            
            @Given class Dep<T>(val value: @MyQualifier T = given)
            
            @Given fun qualified(): @MyQualifier String = ""
            
            fun invoke() = given<Dep<String>>()
            """
    )

}
