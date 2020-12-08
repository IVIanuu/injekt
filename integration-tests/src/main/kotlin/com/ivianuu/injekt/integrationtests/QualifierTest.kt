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
            @Target(AnnotationTarget.TYPE)
            @Qualifier
            annotation class MyQualifier

            @Binding fun foo1(): Foo = Foo()
            @Binding fun foo2(): @MyQualifier Foo = Foo()
            
            @Component interface FooComponent {
                val foo1: Foo
                val foo2: @MyQualifier Foo
            }
       
            fun invoke(): Pair<Foo, Foo> {
                val component = create<FooComponent>()
                return component.foo1 to component.foo2
            }
            """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testDistinctQualifierMulti() = multiCodegen(
        listOf(
            source(
                """
                    @Target(AnnotationTarget.TYPE)
                    @Qualifier
                    annotation class MyQualifier
                    class Foo1Module {
                        @Binding fun foo1(): @MyQualifier Foo = Foo()
                    }
            """
            )
        ),
        listOf(
            source(
                """
                    class Foo2Module {
                        @Binding fun foo2(): Foo = Foo()
                    }
            """
            )
        ),
        listOf(
            source(
                """
                    @Component interface MyComponent {
                        val foo1: @MyQualifier Foo
                        val foo2: Foo
                    }
                    fun invoke(): Pair<Foo, Foo> {
                        val component = create<MyComponent>(Foo1Module(), Foo2Module())
                        return component.foo1 to component.foo2
                    }
            """,
                name = "File.kt"
            )
        )
    ) {
        val (foo1, foo2) = it.last().invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testDistinctQualifierAnnotationWithArguments() = codegen(
        """
            @Target(AnnotationTarget.TYPE)
            @Qualifier
            annotation class MyQualifier(val value: String)

            @Binding fun _foo1(): @MyQualifier("1") Foo = Foo()
            @Binding fun _foo2(): @MyQualifier("2") Foo = Foo()

            @Component interface FooComponent {
                val foo1: @MyQualifier("1") Foo
                val foo2: @MyQualifier("2") Foo
            }
       
            fun invoke(): Pair<Foo, Foo> {
                val component = create<FooComponent>()
                return component.foo1 to component.foo2
            }
            """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testDistinctQualifierAnnotationWithTypeArguments() = codegen(
        """
            @Target(AnnotationTarget.TYPE)
            @Qualifier
            annotation class MyQualifier<T>

            @Binding fun _foo1(): @MyQualifier<String> Foo = Foo()
            @Binding fun _foo2(): @MyQualifier<Int> Foo = Foo()
            
            @Component interface FooComponent {
                val foo1: @MyQualifier<String> Foo
                val foo2: @MyQualifier<Int> Foo
            }
       
            fun invoke(): Pair<Foo, Foo> {
                val component = create<FooComponent>()
                return component.foo1 to component.foo2
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
            
            @Binding class Dep<T>(val value: @MyQualifier T)
            
            @Binding fun qualified(): @MyQualifier String = ""
            
            @Component interface FooComponent {
                val dep: Dep<String>
            }
            """
    )

    @Test
    fun testQualifierWithFunctionTypeParameter() = codegen(
        """
            @Target(AnnotationTarget.TYPE)
            @Qualifier
            annotation class MyQualifier<T>
            
            @Binding fun <T> qualifiedFoo(): @MyQualifier<T> Foo = Foo()
             
            @Component interface FooComponent {
                val foo: @MyQualifier<String> Foo
            }
            """
    )

    // todo @Test
    fun testQualifierWithFunctionTypeParameterMulti() = multiCodegen(
        listOf(
            source(
                """
                    @Target(AnnotationTarget.TYPE)
                    @Qualifier
                    annotation class MyQualifier<T>
                    
                    @Binding fun <T> qualifiedFoo(): @MyQualifier<T> Foo = Foo()
                """
            )
        ),
        listOf(
            source(
                """
                    @Component interface FooComponent {
                        val foo: @MyQualifier<String> Foo
                    }
                """
            )
        )
    )

}