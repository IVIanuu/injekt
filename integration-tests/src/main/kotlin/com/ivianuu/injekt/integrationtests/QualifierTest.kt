package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert.assertNotSame
import org.junit.Test

class QualifierTest {

    @Test
    fun testDistinctQualifier() = codegen(
        """
            @Given val foo = Foo()
            @Given val qualifiedFoo: @Qualifier1 Foo = Foo()
       
            fun invoke(): Pair<Foo, Foo> {
                return given<Foo>() to given<@Qualifier1 Foo>()
            }
            """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testDistinctQualifierAnnotationWithArguments() = codegen(
        """
            @Given val foo1: @Qualifier2("a") Foo = Foo()
            @Given val foo2: @Qualifier2("b") Foo = Foo()
       
            fun invoke(): Pair<Foo, Foo> {
                return given<@Qualifier2("a") Foo>() to given<@Qualifier2("b") Foo>()
            }
            """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testTypeParameterWithQualifierUpperBound() = codegen(
        """
            @Given class Dep<T>(@Given val value: @Qualifier1 T)
            
            @Given fun qualified(): @Qualifier1 String = ""
            
            fun invoke() = given<Dep<String>>()
            """
    )

    @Test
    fun testQualifiedClass() = codegen(
        """ 
            @Given @Qualifier1 class Dep
            fun invoke() = given<@Qualifier1 Dep>()
            """
    )

    @Test
    fun testQualifiedClassMulti() = multiCodegen(
        listOf(
            source(
                """ 
                    @Given @Qualifier1 class Dep
            """
            )
        ),
        listOf(
            source(
                """ 
                    fun invoke() = given<@Qualifier1 Dep>()
            """
            )
        )
    )

    @Test
    fun testQualifiedFunction() = codegen(
        """ 
            @Given @Qualifier1 fun foo() = Foo()
            fun invoke() = given<@Qualifier1 Foo>()
            """
    )

}
