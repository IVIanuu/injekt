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

  @Test fun testPrefersConstructorParameterInjectableOverClassBodyInjectable() = codegen(
    """
      class MyClass(@Provide constructorFoo: Foo) {
        val finalFoo = inject<Foo>()
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
        fun finalFoo(): Foo = inject()
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

  @Test fun testPrefersProviderArgument() = codegen(
    """
      @Provide val foo = Foo()
      fun invoke(foo: Foo) = inject<(Foo) -> Foo>()(foo)
    """
  ) {
    val expected = Foo()
    invokeSingleFile(expected) shouldBeSameInstanceAs expected
  }

  @Test fun testPrefersInnerProviderArgumentOverOuterProviderArgument() = codegen(
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
    compilationShouldHaveFailed(
      "ambiguous injectables:\n\n" +
          "com.ivianuu.injekt.integrationtests.a\n" +
          "com.ivianuu.injekt.integrationtests.b\n\n" +
          "do all match type kotlin.String for parameter x of function com.ivianuu.injekt.inject"
    )
  }

  @Test fun testCannotDeclareMultipleInjectablesOfTheSameTypeInTheSameCodeBlock() = codegen(
    """
      fun invoke() {
        @Provide val injectableA = "a"
        @Provide val injectableB = "b"
        inject<String>()
      }
    """
  ) {
    compilationShouldHaveFailed(
      "ambiguous injectables:\n\n" +
          "com.ivianuu.injekt.integrationtests.invoke.injectableA\n" +
          "com.ivianuu.injekt.integrationtests.invoke.injectableB\n\n" +
          "do all match type kotlin.String for parameter x of function com.ivianuu.injekt.inject"
    )
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
      fun <T> useOrd(@Inject ord: Ord<T>) = ord
    """,
    """
      fun invoke() = useOrd<Int>()
    """
  ) {
    irShouldContain(1, "useOrd<Int>(ord = IntOrd)")
  }

  @Test fun testPrefersMoreSpecificType4() = singleAndMultiCodegen(
    """
      fun interface Ord<in T> {
        fun result(): String
      }
      @Provide fun <T : Any> anyOrd(): Ord<T> = TODO()
      @Provide fun <T : Number> numberOrd(): Ord<T> = TODO()
      @Provide fun <T : Int> intOrd(): Ord<T> = TODO()
      fun <T> useOrd(@Inject ord: Ord<T>) = ord
    """,
    """
      fun invoke() = useOrd<Int>()
    """
  ) {
    irShouldContain(1, "useOrd<Int>(ord = intOrd<Int>())")
  }

  @Test fun testPrefersMoreSpecificType5() = singleAndMultiCodegen(
    """
      fun interface Ord<in T> {
        fun result(): String
      }
      @Provide fun <T : Any> anyOrd(): Ord<T> = TODO()
      @Provide fun <T : Number> numberOrd(): Ord<T> = TODO()
      @Provide fun <T : Int> intOrd(long: Long): Ord<T> = TODO()
      fun <T> useOrd(@Inject ord: Ord<T>) = ord
    """,
    """
      fun invoke() = useOrd<Int>()
    """
  ) {
    irShouldContain(1, "useOrd<Int>(ord = numberOrd<Int>())")
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

  @Test fun testPrefersUserInjectableErrorOverFrameworkInjectable() = singleAndMultiCodegen(
    """
      @Provide fun <T> diyProvider(unit: Unit): () -> T = { TODO() } 
    """,
    """
      fun invoke() = inject<() -> Foo>() 
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type kotlin.Unit for parameter unit of function com.ivianuu.injekt.integrationtests.diyProvider")
  }

  @Test fun testUsesDefaultValueOnNonAmbiguityError() = codegen(
    """
      fun invoke(_foo: Foo): Foo {
        fun inner(foo: Foo = _foo) = foo
        return inner()
      }
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testDoesNotUseDefaultValueOnAmbiguityError() = codegen(
    """
      @Provide fun foo1() = Foo()
      @Provide fun foo2() = Foo()

      fun invoke(): Foo {
        fun inner(@Inject foo: Foo = Foo()) = foo
        return inner()
      }
    """
  ) {
    compilationShouldHaveFailed(
      "ambiguous injectables:\n\n" +
          "com.ivianuu.injekt.integrationtests.foo1\n" +
          "com.ivianuu.injekt.integrationtests.foo2\n\n" +
          "do all match type com.ivianuu.injekt.integrationtests.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.invoke.inner"
    )
  }

  @Test fun testDoesNotUseDefaultValueOnNestedAmbiguityError() = codegen(
    """
      @Provide fun foo1() = Foo()
      @Provide fun foo2() = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)

      fun invoke(foo: Foo): Bar {
        fun inner(@Inject bar: Bar = Bar(foo)) = bar
        return inner()
      }
    """
  ) {
    compilationShouldHaveFailed(
      " \n" +
          "ambiguous injectables of type com.ivianuu.injekt.integrationtests.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.bar.\n" +
          "\n" +
          "I found:\n" +
          "\n" +
          "  com.ivianuu.injekt.integrationtests.invoke.inner(\n" +
          "    bar = com.ivianuu.injekt.integrationtests.bar(\n" +
          "      foo = /* ambiguous: com.ivianuu.injekt.integrationtests.foo1, com.ivianuu.injekt.integrationtests.foo2 do match type com.ivianuu.injekt.integrationtests.Foo */ inject<com.ivianuu.injekt.integrationtests.Foo>()\n" +
          "    )\n" +
          "  )\n" +
          "\n" +
          "but\n" +
          "\n" +
          "com.ivianuu.injekt.integrationtests.foo1\n" +
          "com.ivianuu.injekt.integrationtests.foo2\n" +
          "\n" +
          "do all match type com.ivianuu.injekt.integrationtests.Foo."
    )
  }

  @Test fun testDoesNotPreferValueArgumentOverAnother() = codegen(
    """
      @Provide class FooModule {
        @Provide fun foo() = Foo()
      }

      fun createFoo(@Inject foo1: Foo, @Inject foo2: Foo) = inject<Foo>()
    """
  ) {
    compilationShouldHaveFailed("ambiguous injectables")
  }
}
