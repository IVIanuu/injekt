/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalCompilerApi::class)

package injekt.integrationtests

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
        return create()
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
        fun resolve() = create<Foo>()
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
        fun resolve() = create<Foo>()
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
        fun resolve() = create<Foo>()
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
        fun resolve() = create<Foo>()
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
      fun invoke(@Provide functionFoo: Foo) = create<Foo>()
    """
  ) {
    val expected = Foo()
    invokeSingleFile(expected) shouldBeSameInstanceAs expected
  }

  @Test fun testPrefersFunctionParameterInjectableOverClassInjectable() = codegen(
    """
      class MyClass(@property:Provide val classFoo: Foo = Foo()) {
        fun resolve(@Provide functionFoo: Foo) = create<Foo>()
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
      fun @receiver:Provide Foo.invoke() = create<Foo>()
    """
  ) {
    val expected = Foo()
    invokeSingleFile(expected) shouldBeSameInstanceAs expected
  }

  @Test fun testPrefersFunctionExtensionReceiverInjectableOverClassInjectable() = codegen(
    """
      class MyClass(@property:Provide val classFoo: Foo = Foo()) {
        fun @receiver:Provide Foo.resolve() = create<Foo>()
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
      fun invoke(foo: Foo) = create<(Foo) -> Foo>()(foo)
    """
  ) {
    val expected = Foo()
    invokeSingleFile(expected) shouldBeSameInstanceAs expected
  }

  @Test fun testPrefersInnerLambdaParameterOverOuterLambdaParameter() = codegen(
    """
      @Provide val foo = Foo()
      fun invoke(foo: Foo) = create<(Foo) -> (Foo) -> Foo>()(Foo())(foo)
    """
  ) {
    val expected = Foo()
    invokeSingleFile(expected) shouldBeSameInstanceAs expected
  }

  @Test fun testPrefsInnerBlockInjectable() = codegen(
    """
      fun invoke(): Pair<String, String> {
        @Provide val injectableA = "a"
        return create<String>() to run {
          @Provide val injectableB = "b"
          create<String>()
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
      fun invoke() = create<String>() 
    """
  ) {
    invokeSingleFile() shouldBe "a"
  }

  @Test fun testPrefersFurtherSuccessOverCloserFailure() = codegen(
    """
      @Provide fun a() = "a"
      
      fun invoke() {
        @Provide fun b(long: Long) = "b"
        create<String>()
      }
    """
  ) {
    invokeSingleFile() shouldBe "a"
  }

  @Test fun testPrefersCloserInjectableOverBetterType() = codegen(
    """
      fun invoke(): CharSequence {
        @Provide val a: String = "a"
        run {
          @Provide val b: CharSequence = "b"
          return create<CharSequence>()
        }
      }
    """
  ) {
    invokeSingleFile() shouldBe "b"
  }

  @Test fun testAmbiguousInjectables() = singleAndMultiCodegen(
    """
      @Provide val a = "a"
      @Provide val b = "b"
    """,
    """
      fun invoke() = create<String>() 
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
      fun invoke() = create<Set<String>>() 
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
      fun invoke() = create<Set<Set<String>>>() 
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
      fun invoke() = create<String?>() 
    """
  ) {
    invokeSingleFile() shouldBe "nonnull"
  }

  @Test fun testDoesNotPreferValueArgumentOverAnother() = codegen(
    """
      @Provide class FooModule {
        @Provide fun foo() = Foo()
      }

      fun createFoo(@Provide foo1: Foo, @Provide foo2: Foo) = create<Foo>()
    """
  ) {
    compilationShouldHaveFailed("ambiguous")
  }

  @Test fun testPrefersUserInjectableErrorOverBuiltInInjectable() = singleAndMultiCodegen(
    """
      @Provide fun <T> diyLambda(unit: Unit): () -> T = { TODO() } 
    """,
    """
      fun invoke() = create<() -> Foo>() 
    """
  ) {
    compilationShouldHaveFailed("no injectable")
  }

  @Test fun testPrefersSubClassOverSuperClassInjectable() = singleAndMultiCodegen(
    """
      abstract class SuperClass {
        @Provide val superValue = "super"
        @Provide class SubClass : SuperClass() {
          @Provide val subValue = "sub"
        }
      }
    """,
    """
      fun invoke() = create<String>()
    """
  ) {
    invokeSingleFile() shouldBe "sub"
  }

  @Test fun testPrefersSuperClassSuccessOverSubClassFailure() = singleAndMultiCodegen(
    """
      abstract class SuperClass {
        @Provide val superValue = "super"
        @Provide class SubClass : SuperClass() {
          @Provide fun subValue(value: Int) = "sub"
        }
      }
    """,
    """
      fun invoke() = create<String>()
    """
  ) {
    invokeSingleFile() shouldBe "super"
  }

  @Test fun testCircularDependencyFails() = singleAndMultiCodegen(
    """
      @Provide class A(b: B)
      @Provide class B(a: A)
    """,
    """
      fun invoke() = create<A>() 
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
      fun invoke() = create<B>()
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testReifiedTypeArgumentForReifiedTypeParameterWorks() = singleAndMultiCodegen(
    """
      @Provide inline fun <reified T : Any> klass() = T::class
    """,
    """
      fun invoke() = create<KClass<Foo>>()
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testNonReifiedTypeArgumentForReifiedTypeParameterFails() = singleAndMultiCodegen(
    """
      @Provide inline fun <reified T : Any> klass() = T::class
    """,
    """
      fun <T : Any> invoke() = create<KClass<T>>()
    """
  ) {
    compilationShouldHaveFailed("reified")
  }

  @Test fun testCorrectSuspendCallContextWorks() = singleAndMultiCodegen(
    """
      @Provide suspend fun foo() = Foo()
    """,
    """
      fun invoke() = runBlocking { create<Foo>() }
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testWrongSuspendCallContextFails() = singleAndMultiCodegen(
    """
      @Provide suspend fun foo() = Foo()
    """,
    """
      fun invoke() = create<Foo>()
    """
  ) {
    compilationShouldHaveFailed("suspend")
  }

  @Test fun testCorrectComposableCallContextWorks() = singleAndMultiCodegen(
    """
      @Provide @Composable fun foo() = Foo()
    """,
    """
      fun invoke() = runComposing { create<Foo>() }
    """,
    config = { withCompose() }
  ) {
    invokeSingleFile()
  }

  @Test fun testWrongComposableCallContextFails() = singleAndMultiCodegen(
    """
      @Provide @Composable fun foo() = Foo()
    """,
    """
      fun invoke() = create<Foo>()
    """,
    config = { withCompose() }
  ) {
    compilationShouldHaveFailed("composable")
  }

  @Test fun testKeepsCallContextInVariableInitializer() = singleAndMultiCodegen(
    """
      @Provide @Composable fun composableFoo(): Foo = Foo()
    """,
    """
      fun invoke() = runComposing {
        val result = create<Foo>()
        result
      }
    """,
    config = { withCompose() }
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }
}
