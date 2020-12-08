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

                    class FooBarModule {
                        @Binding fun foo(): InternalFoo = Foo()
                        @Binding fun bar(foo: () -> InternalFoo) = Bar(foo())
                    }
                """
            )
        ),
        listOf(
            source(
                """
                    fun invoke() {
                        create<Bar>(FooBarModule())
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

                    class FooBarModule {
                        @Binding fun foo(): @Internal Foo = Foo()
                        
                        @Binding fun bar(foo: () -> @Internal Foo) = Bar(foo())
                    }
                """
            )
        ),
        listOf(
            source(
                """
                    fun invoke() {
                        create<Bar>(FooBarModule())
                    }
                """
            )
        )
    )

}
