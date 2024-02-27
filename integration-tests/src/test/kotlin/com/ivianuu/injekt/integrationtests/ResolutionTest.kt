/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalCompilerApi::class)

package com.ivianuu.injekt.integrationtests

import io.kotest.matchers.*
import io.kotest.matchers.string.*
import io.kotest.matchers.types.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.junit.*

class ResolutionTest {
  @Test fun testPrefersFileInjectableOverInternalPackageInjectable() = codegen(
    """
      @Provide val internalFoo = Foo()
    """,
    """
      @Provide lateinit var fileFoo: Foo
      fun invoke(foo: Foo): Foo {
        fileFoo = foo
        return inject()
      }
    """
  ) {
    val expected = Foo()
    invokeSingleFile(expected) shouldBeSameInstanceAs expected
  }

  @Test fun testPrefersObjectInjectableOverInternalInjectable() = codegen(
    """
      @Provide val internalFoo = Foo()
      object MyObject {
        @Provide lateinit var objectFoo: Foo
        fun resolve() = inject<Foo>()
      }

      fun invoke(objectFoo: Foo): Foo {
        MyObject.objectFoo = objectFoo
        return MyObject.resolve()
      }
    """
  ) {
    val expected = Foo()
    invokeSingleFile(expected) shouldBeSameInstanceAs expected
  }

  @Test fun testPrefersClassCompanionInjectableOverInternalInjectable() = codegen(
    """
      @Provide val internalFoo = Foo()
      class MyClass {
        fun resolve() = inject<Foo>()
        companion object {
          @Provide lateinit var companionFoo: Foo
        }
      }

      fun invoke(companionFoo: Foo): Foo {
        MyClass.companionFoo = companionFoo
        return MyClass().resolve()
      }
    """
  ) {
    val expected = Foo()
    invokeSingleFile(expected) shouldBeSameInstanceAs expected
  }

  @Test fun testPrefersClassInjectableOverInternalInjectable() = codegen(
    """
      @Provide val internalFoo = Foo()
      class MyClass(@property:Provide val classFoo: Foo) {
        fun resolve() = inject<Foo>()
      }
  
      fun invoke(classFoo: Foo) = MyClass(classFoo).resolve()
    """
  ) {
    val expected = Foo()
    invokeSingleFile(expected) shouldBeSameInstanceAs expected
  }

  @Test fun testPrefersClassInjectableOverClassCompanionInjectable() = codegen(
    """
      class MyClass(@property:Provide val classFoo: Foo) {
        fun resolve() = inject<Foo>()
        companion object {
          @Provide val companionFoo = Foo()
        }
      }

      fun invoke(classFoo: Foo) = MyClass(classFoo).resolve()
    """
  ) {
    val expected = Foo()
    invokeSingleFile(expected) shouldBeSameInstanceAs expected
  }

  @Test fun testPrefersFunctionParameterInjectableOverInternalInjectable() = codegen(
    """
      @Provide val internalFoo = Foo()
      fun invoke(@Provide functionFoo: Foo) = inject<Foo>()
    """
  ) {
    val expected = Foo()
    invokeSingleFile(expected) shouldBeSameInstanceAs expected
  }

  @Test fun testPrefersFunctionParameterInjectableOverClassInjectable() = codegen(
    """
      class MyClass(@Provide val classFoo: Foo = Foo()) {
        fun resolve(@Provide functionFoo: Foo) = inject<Foo>()
      }

      fun invoke(functionFoo: Foo) = MyClass().resolve(functionFoo)
    """
  ) {
    val expected = Foo()
    invokeSingleFile(expected) shouldBeSameInstanceAs expected
  }

  @Test fun testPrefersFunctionExtensionReceiverInjectableOverInternalInjectable() = codegen(
    """
      @Provide val internalFoo = Foo()
      fun @receiver:Provide Foo.invoke() = inject<Foo>()
    """
  ) {
    val expected = Foo()
    invokeSingleFile(expected) shouldBeSameInstanceAs expected
  }

  @Test fun testPrefersFunctionExtensionReceiverInjectableOverClassInjectable() = codegen(
    """
      class MyClass(@Provide val classFoo: Foo = Foo()) {
        fun @receiver:Provide Foo.resolve() = inject<Foo>()
      }

      fun invoke(functionFoo: Foo): Foo {
        return with(MyClass()) {
          functionFoo.resolve()
        }
      }
    """
  ) {
    val expected = Foo()
    invokeSingleFile(expected) shouldBeSameInstanceAs expected
  }

  @Test fun testPrefersLambdaArgument() = codegen(
    """
      @Provide val foo = Foo()
      fun invoke(foo: Foo) = inject<(Foo) -> Foo>()(foo)
    """
  ) {
    val expected = Foo()
    invokeSingleFile(expected) shouldBeSameInstanceAs expected
  }

  @Test fun testPrefersInnerLambdaParameterOverOuterLambdaParameter() = codegen(
    """
      @Provide val foo = Foo()
      fun invoke(foo: Foo) = inject<(Foo) -> (Foo) -> Foo>()(Foo())(foo)
    """
  ) {
    val expected = Foo()
    invokeSingleFile(expected) shouldBeSameInstanceAs expected
  }

  @Test fun testPrefsInnerBlockInjectable() = codegen(
    """
      fun invoke(): Pair<String, String> {
        @Provide val injectableA = "a"
        return inject<String>() to run {
          @Provide val injectableB = "b"
          inject<String>()
        }
      }
    """
  ) {
    invokeSingleFile() shouldBe ("a" to "b")
  }

  @Test fun testPrefersResolvableInjectable() = singleAndMultiCodegen(
    """
      @Provide fun a() = "a"
      @Provide fun b(long: Long) = "b"
    """,
    """
      fun invoke() = inject<String>() 
    """
  ) {
    invokeSingleFile() shouldBe "a"
  }

  @Test fun testPrefersNearerFailureOverResolvableInjectable() = codegen(
    """
      @Provide fun a() = "a"
      
      fun invoke() {
        @Provide fun b(long: Long) = "b"
        inject<String>()
      } 
    """
  ) {
    compilationShouldHaveFailed()
  }

  @Test fun testPrefersNearerInjectableOverBetterType() = codegen(
    """
      fun invoke(): CharSequence {
        @Provide val a: String = "a"
        run {
          @Provide val b: CharSequence = "b"
          return inject<CharSequence>()
        }
      }
    """
  ) {
    invokeSingleFile() shouldBe "b"
  }

  @Test fun testAmbiguousInjectables() = codegen(
    """
      @Provide val a = "a"
      @Provide val b = "b"
    """,
    """
      fun invoke() = inject<String>() 
    """
  ) {
    compilationShouldHaveFailed("ambiguous")
  }

  @Test fun testPrefersMoreSpecificType() = singleAndMultiCodegen(
    """
      @Provide fun stringSet(): Set<String> = setOf("a", "b", "c")
      @Provide fun <T> anySet(): Set<T> = emptySet()
    """,
    """
      fun invoke() = inject<Set<String>>() 
    """
  ) {
    invokeSingleFile() shouldBe setOf("a", "b", "c")
  }

  @Test fun testPrefersMoreSpecificType2() = singleAndMultiCodegen(
    """
      @Provide fun <T> set(): Set<T> = emptySet()
      @Provide fun <T> setSet(): Set<Set<T>> = setOf(setOf("a", "b", "c")) as Set<Set<T>>
    """,
    """
      fun invoke() = inject<Set<Set<String>>>() 
    """
  ) {
    invokeSingleFile() shouldBe setOf(setOf("a", "b", "c"))
  }

  @Test fun testPrefersMoreSpecificType3() = singleAndMultiCodegen(
    """
      interface Ord<in T>
      @Provide object IntOrd : Ord<Int>
      @Provide object NumberOrd : Ord<Number>
      fun <T> useOrd(ord: Ord<T> = inject) = ord
    """,
    """
      fun invoke() = useOrd<Int>()
    """
  ) {
    invokeSingleFile()!!.javaClass.name shouldContain "IntOrd"
  }

  @Test fun testPrefersMoreSpecificType4() = singleAndMultiCodegen(
    """
      interface Ord<in T>
      @Provide fun <T : Any> anyOrd(): Ord<T> = object : Ord<T> {}
      @Provide fun <T : Number> numberOrd(): Ord<T> = object : Ord<T> {}
      @Provide fun <T : Int> intOrd(): Ord<T> = object : Ord<T> {}
      fun <T> useOrd(ord: Ord<T> = inject) = ord
    """,
    """
      fun invoke() = useOrd<Int>()
    """
  ) {
    invokeSingleFile()!!.javaClass.name shouldContain "intOrd"
  }

  @Test fun testPrefersMoreSpecificType5() = singleAndMultiCodegen(
    """
      interface Ord<in T>
      @Provide fun <T : Any> anyOrd(): Ord<T> = object : Ord<T> {}
      @Provide fun <T : Number> numberOrd(): Ord<T> = object : Ord<T> {}
      @Provide fun <T : Int> intOrd(long: Long): Ord<T> = object : Ord<T> {}
      fun <T> useOrd(ord: Ord<T> = inject) = ord
    """,
    """
      fun invoke() = useOrd<Int>()
    """
  ) {
    invokeSingleFile()!!.javaClass.name shouldContain "numberOrd"
  }

  @Test fun testPrefersNonNullType() = singleAndMultiCodegen(
    """
      @Provide val nonNull = "nonnull"
      @Provide val nullable: String? = "nullable"
    """,
    """
      fun invoke() = inject<String?>() 
    """
  ) {
    invokeSingleFile() shouldBe "nonnull"
  }

  @Test fun testDoesNotPreferValueArgumentOverAnother() = codegen(
    """
      @Provide class FooModule {
        @Provide fun foo() = Foo()
      }

      fun createFoo(@Provide foo1: Foo, @Provide foo2: Foo) = inject<Foo>()
    """
  ) {
    compilationShouldHaveFailed("ambiguous")
  }

  @Test fun testPrefersUserInjectableErrorOverBuiltInInjectable() = singleAndMultiCodegen(
    """
      @Provide fun <T> diyLambda(unit: Unit): () -> T = { TODO() } 
    """,
    """
      fun invoke() = inject<() -> Foo>() 
    """
  ) {
    compilationShouldHaveFailed("no injectable")
  }

  @Test fun testCircularDependencyFails() = singleAndMultiCodegen(
    """
      @Provide class A(b: B)
      @Provide class B(a: A)
    """,
    """
      fun invoke() = inject<A>() 
    """
  ) {
    compilationShouldHaveFailed("diverging")
  }

  @Test fun testLambdaBreaksCircularDependency() = singleAndMultiCodegen(
    """
      @Provide class A(b: B)
      @Provide class B(a: (B) -> A) {
        val a = a(this)
      }
    """,
    """
      fun invoke() = inject<B>()
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testReifiedTypeArgumentForReifiedTypeParameterWorks() = singleAndMultiCodegen(
    """
      @Provide inline fun <reified T : Any> klass() = T::class
    """,
    """
      fun invoke() = inject<KClass<Foo>>()
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testNonReifiedTypeArgumentForReifiedTypeParameterFails() = singleAndMultiCodegen(
    """
      @Provide inline fun <reified T : Any> klass() = T::class
    """,
    """
      fun <T : Any> invoke() = inject<KClass<T>>()
    """
  ) {
    compilationShouldHaveFailed("reified")
  }
}
