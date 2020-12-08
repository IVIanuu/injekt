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
            
            fun invoke(): Pair<Any, Any> {
                val dep = Dep()
                return dep to create<Dep>(dep)
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Any, Any>>()
        assertSame(a, b)
    }

    @Test
    fun testMultipleExplicitBindingFails() = codegen(
        """
            class Module1 { 
                @Binding val foo = Foo() 
            }
            class Module2 {
                @Binding val foo = Foo()
            }

            fun invoke() = create<Foo>(Module1(), Module2())
        """
    ) {
        assertInternalError("multiple explicit bindings")
    }

    @Test
    fun testMultipleImplicitBindingFails() = codegen(
        """
        @Binding fun foo1() = Foo()
        @Binding fun foo2() = Foo()
        
        @Component interface MyComponent {
            val foo: Foo
        }
        
        fun invoke(): Foo { 
            return create<MyComponent>().foo
        }
        """
    ) {
        assertInternalError("multiple implicit bindings")
    }

    @Test
    fun testPrefersNonDefaultOverDefaultBinding() = codegen(
        """
            class Dep

            @Default @Binding fun dep() = Dep()
            
            fun invoke(): Pair<Any, Any> {
                val dep = Dep()
                return dep to create<Dep>(dep)
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
            
            fun invoke() = create<Foo>()
        """
    ) {
        assertInternalError("Multiple implicit default bindings")
    }

    @Test
    fun testPrefsUserBindingOverFrameworkBinding() = codegen(
        """
            fun invoke(): Pair<() -> Foo, () -> Foo> {
                val lazyFoo = { Foo() }
                return lazyFoo to create<() -> Foo>(lazyFoo)
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

            @Binding fun <T> genericDep(t: T): Dep<T> = error("")
                
            @Binding fun fooDep(foo: Foo): Dep<Foo> = Dep(foo)
                
            @Binding fun foo() = Foo()

            fun invoke() = create<Dep<Foo>>()
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testMissingBindingFails() = codegen(
        """
            class Dep
            fun invoke() = create<Dep>()
        """
    ) {
        assertInternalError("no binding")
    }

    @Test
    fun testDeeplyMissingBindingFails() = codegen(
        """
            @Binding fun bar(foo: Foo) = Bar(foo)
            @Binding fun baz(bar: Bar, foo: Foo) = Baz(bar, foo)
            fun invoke() = create<Baz>()
        """
    ) {
        assertInternalError("no binding")
    }

    @Test
    fun testDistinctTypeParameter() = codegen(
        """
            @SetElements fun setA() = setOf("a")
            @SetElements fun setB() = setOf(0)
            
            @Component interface MyComponent {
                val setOfStrings: Set<String>
                val setOfInts: Set<Int>
            }

            fun invoke(): Pair<Set<String>, Set<Int>> {
                val component = create<MyComponent>()
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

            @Binding fun _foo1(): Foo1 = Foo()
            @Binding fun _foo2(): Foo2 = Foo()

            @Component interface FooComponent {
                val foo1: Foo1
                val foo2: Foo2
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
    fun testDistinctTypeAliasMulti() = multiCodegen(
        listOf(
            source(
                """
                    typealias Foo1 = Foo
                    @Binding fun foo1(): Foo1 = Foo()
            """
            )
        ),
        listOf(
            source(
                """
                    typealias Foo2 = Foo
                    @Binding fun foo2(): Foo2 = Foo()
            """
            )
        ),
        listOf(
            source(
                """
                    @Component interface MyComponent {
                        val foo1: Foo1
                        val foo2: Foo2
                    }
                    fun invoke(): Pair<Foo1, Foo2> {
                        val component = create<MyComponent>()
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
            @Binding fun foo(): Foo = Foo()

            fun invoke() = create<Foo?>()
            """
    ) {
        assertNotNull(invokeSingleFile())
    }

    @Test
    fun testCannotUseNullableBindingForNonNullRequest() = codegen(
        """
            @Binding fun foo(): Foo? = Foo()
            fun invoke() = create<Foo>()
            """
    ) {
        assertInternalError("No binding")
    }

    @Test
    fun testBindingForNullableRequestCanGetUsedForNonNullRequest() = codegen(
        """
            @Component interface MyComponent {
                val nullableFoo: Foo?
                val nonNullFoo: Foo
                
                @Binding fun foo() = Foo()
            }
        """
    )

    @Test
    fun testReturnsNullOnMissingNullableRequest() = codegen(
        """
            @Component interface FooComponent {
                val foo: Foo?
            }
            fun invoke(): Foo? { 
                return create<FooComponent>().foo
            }
        """
    ) {
        assertNull(invokeSingleFile())
    }

    @Test
    fun testReturnsDefaultOnMissingOpenRequest() = codegen(
        """
            val DEFAULT_FOO = Foo()
            @Component interface FooComponent {
                val foo: Foo get() = DEFAULT_FOO
            }
            fun invoke(): Pair<Foo, Foo> { 
                return DEFAULT_FOO to create<FooComponent>().foo
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo, Foo>>()
        assertSame(a, b)
    }

    @Test
    fun testReturnsDefaultOnMissingNullableRequestWithDefault() = codegen(
        """
            val DEFAULT_FOO = Foo()
            @Component interface FooComponent {
                val foo: Foo? get() = DEFAULT_FOO
            }
            fun invoke(): Pair<Foo?, Foo?> { 
                return DEFAULT_FOO to create<FooComponent>().foo
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
            
            @Component interface FooComponent {
                val dep: Dep
            }
            
            fun invoke(): Foo? { 
                return create<FooComponent>().dep.foo
            }
        """
    ) {
        assertNull(invokeSingleFile())
    }

    @Test
    fun testUsesNullOnMissingGenericNullableDependency() = codegen(
        """
            @Binding class Dep<T>(val value: T?)
            
            @Component interface FooComponent {
                val dep: Dep<Foo>
            }
            
            fun invoke(): Foo? { 
                return create<FooComponent>().dep.value
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
            
            @Component interface FooComponent {
                val dep: Dep
            }
            
            fun invoke(): Pair<Foo?, Foo?> { 
                return DEFAULT_FOO to create<FooComponent>().dep.foo
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
            
            @Component interface FooComponent {
                val dep: Dep
            }
            fun invoke(): Pair<Foo, Foo> { 
                return DEFAULT_FOO to create<FooComponent>().dep.foo
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo, Foo>>()
        assertSame(a, b)
    }

    @Test
    fun testPrefersExplicitOverExplicitParentBinding() = codegen(
        """
            @Component interface MyComponent {
                val string: String
                val childFactory: (String) -> MyChildComponent
            }

            @Component interface MyChildComponent {
                val string: String
            }

            fun invoke(): Pair<String, String> {
                val parent = create<MyComponent>("parent")
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
            @Component interface MyComponent {
                val childFactory: () -> MyChildComponent
            }

            @Component interface MyChildComponent {
                val string: String
            }

            @Binding fun implicit() = "implicit"

            fun invoke(): String {
                return create<MyComponent>("parent").childFactory().string
            }
        """
    ) {
        val value = invokeSingleFile<String>()
        assertEquals("parent", value)
    }

}
