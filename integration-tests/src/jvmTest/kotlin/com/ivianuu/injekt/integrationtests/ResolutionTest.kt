/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.Test

class ResolutionTest {
  @Test fun testPrefersFileInjectableOverInternalPackageInjectable() = codegen(
    """
      @Provide val internalFoo = Foo()
    """,
    """
      @Provide lateinit var fileFoo: Foo
      fun invoke(foo: Foo): Foo {
        fileFoo = foo
        return context()
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
        fun resolve() = context<Foo>()
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
        fun resolve() = context<Foo>()
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
        fun resolve() = context<Foo>()
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
        fun resolve() = context<Foo>()
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

  @Test fun testPrefersConstructorParameterInjectableOverClassBodyInjectable() = codegen(
    """
      class MyClass(@Provide constructorFoo: Foo) {
        val finalFoo = context<Foo>()
        @Provide val classFoo = Foo()
      }

      fun invoke(constructorFoo: Foo) = MyClass(constructorFoo).finalFoo
    """
  ) {
    val expected = Foo()
    invokeSingleFile(expected) shouldBeSameInstanceAs expected
  }

  @Test fun testPrefersSubClassInjectableOverSuperClassInjectable() = singleAndMultiCodegen(
    """
      abstract class MySuperClass(@property:Provide val superClassFoo: Foo = Foo())
      class MySubClass(@property:Provide val subClassFoo: Foo) : MySuperClass() {
        fun finalFoo(): Foo = context<Foo>()
      }
    """,
    """
      fun invoke(subClassFoo: Foo) = MySubClass(subClassFoo).finalFoo()
    """
  ) {
    val expected = Foo()
    invokeSingleFile(expected) shouldBeSameInstanceAs expected
  }

  @Test fun testPrefersFunctionParameterInjectableOverInternalInjectable() = codegen(
    """
      @Provide val internalFoo = Foo()
      fun invoke(@Provide functionFoo: Foo) = context<Foo>()
    """
  ) {
    val expected = Foo()
    invokeSingleFile(expected) shouldBeSameInstanceAs expected
  }

  @Test fun testPrefersFunctionParameterInjectableOverClassInjectable() = codegen(
    """
      class MyClass(@Provide val classFoo: Foo = Foo()) {
        fun resolve(@Provide functionFoo: Foo) = context<Foo>()
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
      fun @receiver:Provide Foo.invoke() = context<Foo>()
    """
  ) {
    val expected = Foo()
    invokeSingleFile(expected) shouldBeSameInstanceAs expected
  }

  @Test fun testPrefersFunctionExtensionReceiverInjectableOverClassInjectable() = codegen(
    """
      class MyClass(@Provide val classFoo: Foo = Foo()) {
        fun @receiver:Provide Foo.resolve() = context<Foo>()
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

  @Test fun testPrefersProviderArgument() = codegen(
    """
      @Provide val foo = Foo()
      fun invoke(foo: Foo) = context<(Foo) -> Foo>()(foo)
    """
  ) {
    val expected = Foo()
    invokeSingleFile(expected) shouldBeSameInstanceAs expected
  }

  @Test fun testPrefersInnerProviderArgumentOverOuterProviderArgument() = codegen(
    """
      @Provide val foo = Foo()
      fun invoke(foo: Foo) = context<(Foo) -> (Foo) -> Foo>()(Foo())(foo)
    """
  ) {
    val expected = Foo()
    invokeSingleFile(expected) shouldBeSameInstanceAs expected
  }

  @Test fun testPrefsInnerBlockInjectable() = codegen(
    """
      fun invoke(): Pair<String, String> {
        @Provide val injectableA = "a"
        return context<String>() to run {
          @Provide val injectableB = "b"
          context<String>()
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
      fun invoke() = context<String>() 
    """
  ) {
    invokeSingleFile() shouldBe "a"
  }

  @Test fun testPrefersNearerInjectableOverBetterType() = codegen(
    """
      fun invoke(): CharSequence {
        @Provide val a: String = "a"
        run {
          @Provide val b: CharSequence = "b"
          return context<CharSequence>()
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
      fun invoke() = context<String>() 
    """
  ) {
    compilationShouldHaveFailed(
      "ambiguous injectables:\n\n" +
          "com.ivianuu.injekt.integrationtests.a\n" +
          "com.ivianuu.injekt.integrationtests.b\n\n" +
          "do all match type kotlin.String for parameter \$contextReceiver_0 of function com.ivianuu.injekt.context"
    )
  }

  @Test fun testCannotDeclareMultipleInjectablesOfTheSameTypeInTheSameCodeBlock() = codegen(
    """
      fun invoke() {
        @Provide val injectableA = "a"
        @Provide val injectableB = "b"
        context<String>()
      }
    """
  ) {
    compilationShouldHaveFailed(
      "ambiguous injectables:\n\n" +
          "com.ivianuu.injekt.integrationtests.invoke.injectableA\n" +
          "com.ivianuu.injekt.integrationtests.invoke.injectableB\n\n" +
          "do all match type kotlin.String for parameter \$contextReceiver_0 of function com.ivianuu.injekt.context"
    )
  }

  @Test fun testPrefersMoreSpecificType() = singleAndMultiCodegen(
    """
      @Provide fun stringSet(): Set<String> = setOf("a", "b", "c")
      @Provide fun <T> anySet(): Set<T> = emptySet()
    """,
    """
      fun invoke() = context<Set<String>>() 
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
      fun invoke() = context<Set<Set<String>>>() 
    """
  ) {
    invokeSingleFile() shouldBe setOf(setOf("a", "b", "c"))
  }

  @Test fun testPrefersMoreSpecificType3() = singleAndMultiCodegen(
    """
      interface Ord<in T>
      @Provide object IntOrd : Ord<Int>
      @Provide object NumberOrd : Ord<Number>
      context(Ord<T>) fun <T> useOrd() = Unit
    """,
    """
      fun invoke() = useOrd<Int>()
    """
  ) {
    irShouldContain(1, "useOrd<Int>(contextReceiverParameter0 = IntOrd)")
  }

  @Test fun testPrefersMoreSpecificType4() = singleAndMultiCodegen(
    """
      fun interface Ord<in T> {
        fun result(): String
      }
      @Provide fun <T : Any> anyOrd(): Ord<T> = TODO()
      @Provide fun <T : Number> numberOrd(): Ord<T> = TODO()
      @Provide fun <T : Int> intOrd(): Ord<T> = TODO()
      context(Ord<T>) fun <T> useOrd() = Unit
    """,
    """
      fun invoke() = useOrd<Int>()
    """
  ) {
    irShouldContain(1, "useOrd<Int>(contextReceiverParameter0 = intOrd<Int>())")
  }

  @Test fun testPrefersMoreSpecificType5() = singleAndMultiCodegen(
    """
      fun interface Ord<in T> {
        fun result(): String
      }
      @Provide fun <T : Any> anyOrd(): Ord<T> = TODO()
      @Provide fun <T : Number> numberOrd(): Ord<T> = TODO()
      @Provide fun <T : Int> intOrd(long: Long): Ord<T> = TODO()
      context(Ord<T>) fun <T> useOrd() = Unit
    """,
    """
      fun invoke() = useOrd<Int>()
    """
  ) {
    irShouldContain(1, "useOrd<Int>(contextReceiverParameter0 = numberOrd<Int>())")
  }

  @Test fun testPrefersNonNullType() = singleAndMultiCodegen(
    """
      @Provide val nonNull = "nonnull"
      @Provide val nullable: String? = "nullable"
    """,
    """
      fun invoke() = context<String?>() 
    """
  ) {
    invokeSingleFile() shouldBe "nonnull"
  }

  @Test fun testPrefersUserInjectableErrorOverFrameworkInjectable() = singleAndMultiCodegen(
    """
      @Provide fun <T> diyProvider(unit: Unit): () -> T = { TODO() } 
    """,
    """
      fun invoke() = context<() -> Foo>() 
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type kotlin.Unit for parameter unit of function com.ivianuu.injekt.integrationtests.diyProvider")
  }
}
