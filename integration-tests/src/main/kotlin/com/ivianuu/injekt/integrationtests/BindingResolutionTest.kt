package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.assertInternalError
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertNull
import junit.framework.Assert.assertSame
import org.junit.Test

class BindingResolutionTest {

    @Test
    fun testPrefersExplicitOverImplicitBinding() = codegen(
        """
            @Binding class Dep
            
            @Component abstract class MyComponent(@Binding protected val _dep: Dep) { 
                abstract val dep: Dep
            }
            
            fun invoke(): Pair<Any, Any> {
                val dep = Dep()
                return dep to component<MyComponent>(dep).dep
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Any, Any>>()
        assertSame(a, b)
    }

    @Test
    fun testMultipleResolvableExplicitBindingFails() = codegen(
        """
            @Component abstract class MyComponent(
                @Binding protected val foo1: Foo,
                @Binding protected val foo2: Foo
            ) {
                abstract val foo: Foo
            }
        """
    ) {
        assertInternalError("multiple explicit bindings")
    }

    @Test
    fun testPrefersInternalImplicitOverExternalImplicitBinding() = multiCodegen(
        listOf(
            source(
                """
                    var externalFooField: Foo? = null
                    @Binding val externalFoo: Foo get() = externalFooField!!
                """
            )
        ),
        listOf(
            source(
                """
                    var internalFooField: Foo? = null
                    @Binding val internalFoo: Foo get() = internalFooField!!

                    @Component abstract class MyComponent {
                        abstract val foo: Foo
                    }
                    
                    fun invoke(
                        internalFoo: Foo,
                        externalFoo: Foo
                    ): Foo {
                        externalFooField = externalFoo
                        internalFooField = internalFoo
                        return component<MyComponent>().foo
                    }
                """,
                name = "File.kt"
            )
        )
    ) {
        val externalFoo = Foo()
        val internalFoo = Foo()
        assertSame(internalFoo, it.last().invokeSingleFile(internalFoo, externalFoo))
    }

    @Test
    fun testMultipleResolvableInternalImplicitBindingFails() = codegen(
        """
        @Binding fun foo1() = Foo()
        @Binding fun foo2() = Foo()
        
        @Component abstract class MyComponent {
            abstract val foo: Foo
        }
        
        fun invoke(): Foo { 
            return component<MyComponent>().foo
        }
        """
    ) {
        assertInternalError("multiple internal implicit bindings")
    }

    @Test
    fun testMultipleResolvableExternalImplicitBindingsFails() = multiCodegen(
        listOf(
            source(
                """
                    @Binding fun foo1() = Foo()
            """
            )
        ),
        listOf(
            source(
                """
                    @Binding fun foo2() = Foo()
            """
            )
        ),
        listOf(
            source(
                """
                    @Component abstract class MyComponent {
                        abstract val foo: Foo
                    }
                    fun invoke(): Foo { 
                        return component<MyComponent>().foo
                    }
                """
            )
        )
    ) {
        it.last().assertInternalError("multiple external implicit bindings")
    }

    @Test
    fun testPrefersUserOverDefaultBinding() = codegen(
        """
            class Dep

            @Default
            @Binding fun dep() = Dep()

            @Component abstract class MyComponent(@Binding protected val _dep: Dep) { 
                abstract val dep: Dep
            }
            
            fun invoke(): Pair<Any, Any> {
                val dep = Dep()
                return dep to component<MyComponent>(dep).dep
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Any, Any>>()
        assertSame(a, b)
    }

    @Test
    fun testMultipleDefaultBindingsFails() = codegen(
        """
            @Default
            @Binding fun foo1() = Foo()

            @Default
            @Binding fun foo2() = Foo()
            
            @Component abstract class MyComponent { 
                abstract val foo: Foo
            }
        """
    ) {
        assertInternalError("Multiple internal implicit default bindings")
    }

    @Test
    fun testPrefsUserBindingOverFrameworkBinding() = codegen(
        """
            @Component abstract class MyComponent(
                @Binding protected val _lazyFoo: () -> Foo
            ) {
                abstract val lazyFoo: () -> Foo
            }
            
            fun invoke(): Pair<() -> Foo, () -> Foo> {
                val lazyFoo = { Foo() }
                return lazyFoo to component<MyComponent>(lazyFoo).lazyFoo
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Any, Any>>()
        assertSame(a, b)
    }

    @Test
    fun testPrefersResolvableBinding() = codegen(
        """
            val defaultFoo = Foo()
            
            @Binding fun bar() = Bar(defaultFoo)
            
            @Binding fun bar(foo: Foo) = Bar(foo)
            
            @Component abstract class MyComponent { 
                abstract val bar: Bar
            }
            
            fun invoke(): Pair<Any, Any> {
                return defaultFoo to component<MyComponent>().bar.foo
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Any, Any>>()
        assertSame(a, b)
    }

    @Test
    fun testPrefersExactType() = codegen(
        """
            class Dep<T>(val value: T)
            
            @Component abstract class FooComponent {
                abstract val fooDep: Dep<Foo>
                
                @Binding protected fun <T> genericDep(t: T): Dep<T> = error("")
                
                @Binding protected fun fooDep(foo: Foo): Dep<Foo> = Dep(foo)
                
                @Binding protected fun foo() = Foo()
            }
            
            fun invoke() {
                component<FooComponent>().fooDep
            }
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testMissingBindingFails() = codegen(
        """
            class Dep
            
            @Component abstract class DepComponent {
                abstract val dep: Dep
            }
        """
    ) {
        assertInternalError("no binding")
    }

    @Test
    fun testDeeplyMissingBindingFails() = codegen(
        """
            @Component abstract class BazComponent {
                abstract val baz: Baz
            
                @Binding protected fun bar(foo: Foo) = Bar(foo)
        
                @Binding protected fun baz(bar: Bar, foo: Foo) = Baz(bar, foo)
            }
        """
    ) {
        assertInternalError("no binding")
    }

    @Test
    fun testDistinctTypeParameter() = codegen(
        """
            @Component abstract class MyComponent {
                abstract val setOfStrings: Set<String>
                abstract val setOfInts: Set<Int>
            
                @SetElements protected fun _setA() = setOf("a")
                @SetElements protected fun _setB() = setOf(0)
            }

            fun invoke(): Pair<Set<String>, Set<Int>> {
                val component = component<MyComponent>()
                return component.setOfStrings to component.setOfInts
            }
            """
    ) {
        val (setA, setB) = invokeSingleFile<Pair<Set<String>, Set<Int>>>()
        assertNotSame(setA, setB)
    }

    @Test
    fun testDistinctTypeAlias() = codegen(
        """
            typealias Foo1 = Foo
            typealias Foo2 = Foo
            
            @Component abstract class FooComponent {
                abstract val foo1: Foo1
                abstract val foo2: Foo2
                @Binding protected fun _foo1(): Foo1 = Foo()
                @Binding protected fun _foo2(): Foo2 = Foo()
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
    fun testDistinctTypeAliasMulti() = multiCodegen(
        listOf(
            source(
                """
                    typealias Foo1 = Foo
                    object Foo1Module {
                        @Binding fun foo1(): Foo1 = Foo()
                    }
            """
            )
        ),
        listOf(
            source(
                """
                    typealias Foo2 = Foo
                    object Foo2Module {
                        @Binding fun foo2(): Foo2 = Foo()
                    }
            """
            )
        ),
        listOf(
            source(
                """
                    @Component abstract class MyComponent {
                        abstract val foo1: Foo1
                        abstract val foo2: Foo2
                        
                        @Module protected val foo1Module = Foo1Module
                        @Module protected val foo2Module = Foo2Module
                    }
                    fun invoke(): Pair<Foo1, Foo2> {
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
    fun testCanUseNonNullBindingForNullableRequest() = codegen(
        """
            @Component abstract class FooComponent { 
                abstract val foo: Foo?
                @Binding protected fun foo(): Foo = Foo()
            }

            fun invoke() = component<FooComponent>().foo
            """
    ) {
        assertNotNull(invokeSingleFile())
    }

    @Test
    fun testCannotUseNullableBindingForNonNullRequest() = codegen(
        """
            @Component abstract class FooComponent { 
                abstract val foo: Foo
                @Binding protected fun foo(): Foo? = Foo()
            }
            """
    ) {
        assertInternalError("No binding")
    }

    @Test
    fun testBindingForNullableRequestCanGetUsedForNonNullRequest() = codegen(
        """
            @Component abstract class MyComponent {
                abstract val nullableFoo: Foo?
                abstract val nonNullFoo: Foo
                
                @Binding protected fun foo() = Foo()
            }
        """
    )

    @Test
    fun testReturnsNullOnMissingNullableRequest() = codegen(
        """
            @Component abstract class FooComponent {
                abstract val foo: Foo?
            }
            fun invoke(): Foo? { 
                return component<FooComponent>().foo
            }
        """
    ) {
        assertNull(invokeSingleFile())
    }

    @Test
    fun testReturnsDefaultOnMissingOpenRequest() = codegen(
        """
            val DEFAULT_FOO = Foo()
            @Component abstract class FooComponent {
                open val foo: Foo = DEFAULT_FOO
            }
            fun invoke(): Pair<Foo, Foo> { 
                return DEFAULT_FOO to component<FooComponent>().foo
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo, Foo>>()
        assertSame(a, b)
    }

    @Test
    fun testReturnsDefaultOnMissingOpenNullableRequest() = codegen(
        """
            val DEFAULT_FOO = Foo()
            @Component abstract class FooComponent {
                open val foo: Foo? = DEFAULT_FOO
            }
            fun invoke(): Pair<Foo?, Foo?> { 
                return DEFAULT_FOO to component<FooComponent>().foo
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo?, Foo?>>()
        assertSame(a, b)
    }

    @Test
    fun testUsesNullOnMissingNullableDependency() = codegen(
        """
            @Binding class Dep(val foo: Foo?)
            
            @Component abstract class FooComponent {
                abstract val dep: Dep
            }
            
            fun invoke(): Foo? { 
                return component<FooComponent>().dep.foo
            }
        """
    ) {
        assertNull(invokeSingleFile())
    }

    @Test
    fun testUsesNullOnMissingGenericNullableDependency() = codegen(
        """
            @Binding class Dep<T>(val value: T?)
            
            @Component abstract class FooComponent {
                abstract val dep: Dep<Foo>
            }
            
            fun invoke(): Foo? { 
                return component<FooComponent>().dep.value
            }
        """
    ) {
        assertNull(invokeSingleFile())
    }

    @Test
    fun testUsesDefaultOnMissingNullableDependency() = codegen(
        """
            @Binding class Dep(val foo: Foo? = DEFAULT_FOO)
            val DEFAULT_FOO = Foo()
            
            @Component abstract class FooComponent {
                abstract val dep: Dep
            }
            
            fun invoke(): Pair<Foo?, Foo?> { 
                return DEFAULT_FOO to component<FooComponent>().dep.foo
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo?, Foo?>>()
        assertSame(a, b)
    }

    @Test
    fun testUsesDefaultOnMissingDependency() = codegen(
        """
            @Binding            
            class Dep(val foo: Foo = DEFAULT_FOO)
            val DEFAULT_FOO = Foo()
            
            @Component abstract class FooComponent {
                abstract val dep: Dep
            }
            fun invoke(): Pair<Foo, Foo> { 
                return DEFAULT_FOO to component<FooComponent>().dep.foo
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo, Foo>>()
        assertSame(a, b)
    }

    @Test
    fun testPrefersExplicitOverExplicitParentBinding() = codegen(
        """
            @Component abstract class MyComponent(@Binding protected val stringBinding: String) {
                abstract val string: String
                abstract val childFactory: (String) -> MyChildComponent
            }

            @ChildComponent
            abstract class MyChildComponent(@Binding protected val stringBinding: String) {
                abstract val string: String
            }

            fun invoke(): Pair<String, String> {
                val parent = component<MyComponent>("parent")
                return parent.string to parent.childFactory("child").string
            }
        """
    ) {
        val (parent, child) = invokeSingleFile<Pair<String, String>>()
        assertEquals("parent", parent)
        assertEquals("child", child)
    }

    @Test
    fun testPrefersExplicitParentBindingOverImplicitBinding() = codegen(
        """
            @Component abstract class MyComponent(@Binding protected val stringBinding: String) {
                abstract val childFactory: () -> MyChildComponent
            }

            @ChildComponent
            abstract class MyChildComponent {
                abstract val string: String
            }

            @Binding fun implicit() = "implicit"

            fun invoke(): String {
                return component<MyComponent>("parent").childFactory().string
            }
        """
    ) {
        val value = invokeSingleFile<String>()
        assertEquals("parent", value)
    }

}
