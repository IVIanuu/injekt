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
            
            @Component
            abstract class FooComponent {
                abstract val foo1: Foo
                abstract val foo2: @MyQualifier Foo
                @Binding protected fun _foo1(): Foo = Foo()
                @Binding protected fun _foo2(): @MyQualifier Foo = Foo()
            }
       
            fun invoke(): Pair<Foo, Foo> {
                val component = component<FooComponent>()
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
                    @Module
                    object Foo1Module {
                        @Binding fun foo1(): @MyQualifier Foo = Foo()
                    }
            """
            )
        ),
        listOf(
            source(
                """
                    @Module
                    object Foo2Module {
                        @Binding fun foo2(): Foo = Foo()
                    }
            """
            )
        ),
        listOf(
            source(
                """
                    @Component
                    abstract class MyComponent {
                        abstract val foo1: @MyQualifier Foo
                        abstract val foo2: Foo
                        
                        @Module protected val foo1Module = Foo1Module
                        @Module protected val foo2Module = Foo2Module
                    }
                    fun invoke(): Pair<Foo, Foo> {
                        val component = component<MyComponent>()
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
            
            @Component
            abstract class FooComponent {
                abstract val foo1: @MyQualifier("1") Foo
                abstract val foo2: @MyQualifier("2") Foo
                @Binding protected fun _foo1(): @MyQualifier("1") Foo = Foo()
                @Binding protected fun _foo2(): @MyQualifier("2") Foo = Foo()
            }
       
            fun invoke(): Pair<Foo, Foo> {
                val component = component<FooComponent>()
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
            
            @Component
            abstract class FooComponent {
                abstract val foo1: @MyQualifier<String> Foo
                abstract val foo2: @MyQualifier<Int> Foo
                @Binding protected fun _foo1(): @MyQualifier<String> Foo = Foo()
                @Binding protected fun _foo2(): @MyQualifier<Int> Foo = Foo()
            }
       
            fun invoke(): Pair<Foo, Foo> {
                val component = component<FooComponent>()
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
            
            @Binding
            class Dep<T>(val value: @MyQualifier T)
            
            @Binding
            fun qualified(): @MyQualifier String = ""
            
            @Component
            abstract class FooComponent {
                abstract val dep: Dep<String>
            }
            """
    )

    @Test
    fun testQualifierWithFunctionTypeParameter() = codegen(
        """
            @Target(AnnotationTarget.TYPE)
            @Qualifier
            annotation class MyQualifier<T>
            
            @Binding
            fun <T> qualifiedFoo(): @MyQualifier<T> Foo = Foo()
             
            @Component
            abstract class FooComponent {
                abstract val foo: @MyQualifier<String> Foo
            }
            """
    )

    @Test
    fun testQualifierWithFunctionTypeParameterMulti() = multiCodegen(
        listOf(
            source(
                """
                    @Target(AnnotationTarget.TYPE)
                    @Qualifier
                    annotation class MyQualifier<T>
                    
                    @Binding
                    fun <T> qualifiedFoo(): @MyQualifier<T> Foo = Foo()
                """
            )
        ),
        listOf(
            source(
                """
                    @Component
                    abstract class FooComponent {
                        abstract val foo: @MyQualifier<String> Foo
                    }
                """
            )
        )
    )

}