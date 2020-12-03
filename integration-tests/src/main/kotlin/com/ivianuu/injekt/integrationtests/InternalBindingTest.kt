package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import org.junit.Test

class InternalBindingTest {

    @Test
    fun testBindingsCanBeInternalizedViaInternalTypeAliases() = multiCodegen(
        listOf(
            source(
                """
                    internal typealias InternalFoo = Foo

                    object FooBarModule {
                        @Binding
                        fun foo(): InternalFoo = Foo()
                        
                        @Binding
                        fun bar(foo: () -> InternalFoo) = Bar(foo())
                    }
                """
            )
        ),
        listOf(
            source(
                """
                    @Component 
                    abstract class MyComponent {
                        abstract val bar: Bar
                        
                        @Module
                        protected val fooBarModule = FooBarModule
                    }
                """
            )
        )
    )

    @Test
    fun testBindingsCanBeInternalizedViaInternalQualifiers() = multiCodegen(
        listOf(
            source(
                """
                    @Qualifier
                    @Target(AnnotationTarget.TYPE)
                    internal annotation class Internal

                    object FooBarModule {
                        @Binding
                        fun foo(): @Internal Foo = Foo()
                        
                        @Binding
                        fun bar(foo: () -> @Internal Foo) = Bar(foo())
                    }
                """
            )
        ),
        listOf(
            source(
                """
                    @Component 
                    abstract class MyComponent {
                        abstract val bar: Bar
                        
                        @Module
                        protected val fooBarModule = FooBarModule
                    }
                """
            )
        )
    )

}
