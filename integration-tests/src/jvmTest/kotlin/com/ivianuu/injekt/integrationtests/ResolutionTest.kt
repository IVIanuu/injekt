/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.*
import io.kotest.matchers.*
import io.kotest.matchers.types.*
import org.jetbrains.kotlin.name.*
import org.junit.*

class ResolutionTest {
  @Test fun testPrefersInternalInjectableOverExternal() = multiCodegen(
    """
      @Provide lateinit var externalFoo: Foo
    """,
    """
      @Provide lateinit var internalFoo: Foo
  
      fun invoke(internal: Foo, external: Foo): Foo {
        externalFoo = external
        internalFoo = internal
        return inject<Foo>()
      }
    """
  ) {
    val internal = Foo()
    val external = Foo()
    val result = invokeSingleFile(internal, external)
    result shouldBeSameInstanceAs internal
  }

  @Test fun testPrefersObjectInjectableOverInternalInjectable() = codegen(
    """
      @Provide lateinit var internalFoo: Foo
      object MyObject {
        @Provide lateinit var objectFoo: Foo
        fun resolve() = inject<Foo>()
      }

      fun invoke(internal: Foo, objectFoo: Foo): Foo {
        internalFoo = internal
        MyObject.objectFoo = objectFoo
        return MyObject.resolve()
      }
    """
  ) {
    val internal = Foo()
    val objectFoo = Foo()
    val result = invokeSingleFile(internal, objectFoo)
    objectFoo shouldBeSameInstanceAs result
  }

  @Test fun testPrefersClassCompanionInjectableOverInternalInjectable() = codegen(
    """
      @Provide lateinit var internalFoo: Foo
      class MyClass {
        fun resolve() = inject<Foo>()
        companion object {
          @Provide lateinit var companionFoo: Foo
        }
      }

      fun invoke(internal: Foo, companionFoo: Foo): Foo {
        internalFoo = internal
        MyClass.companionFoo = companionFoo
        return MyClass().resolve()
      }
    """
  ) {
    val internal = Foo()
    val companionFoo = Foo()
    val result = invokeSingleFile(internal, companionFoo)
    companionFoo shouldBeSameInstanceAs result
  }

  @Test fun testPrefersClassInjectableOverInternalInjectable() = codegen(
    """
      @Provide lateinit var internalFoo: Foo
      class MyClass(@Provide val classFoo: Foo) {
        fun resolve() = inject<Foo>()
      }
  
      fun invoke(internal: Foo, classFoo: Foo): Foo {
        internalFoo = internal
        return MyClass(classFoo).resolve()
      }
    """
  ) {
    val internal = Foo()
    val classFoo = Foo()
    val result = invokeSingleFile(internal, classFoo)
    classFoo shouldBeSameInstanceAs result
  }

  @Test fun testPrefersClassInjectableOverClassCompanionInjectable() = codegen(
    """
      class MyClass(@Provide val classFoo: Foo) {
        fun resolve() = inject<Foo>()
        companion object {
          @Provide lateinit var companionFoo: Foo
        }
      }

      fun invoke(classFoo: Foo, companionFoo: Foo): Foo {
        MyClass.companionFoo = companionFoo
        return MyClass(classFoo).resolve()
      }
    """
  ) {
    val classFoo = Foo()
    val companionFoo = Foo()
    val result = invokeSingleFile(classFoo, companionFoo)
    classFoo shouldBeSameInstanceAs result
  }

  @Test fun testPrefersConstructorParameterInjectableOverClassBodyInjectable() = codegen(
    """
      lateinit var classBodyFoo: Foo
      class MyClass(@Provide constructorFoo: Foo) {
        val finalFoo = inject<Foo>()
        @Provide val classFoo get() = classBodyFoo
      }

      fun invoke(constructorFoo: Foo, _classBodyFoo: Foo): Foo {
        classBodyFoo = _classBodyFoo
        return MyClass(constructorFoo).finalFoo
      }
    """
  ) {
    val constructorFoo = Foo()
    val classBodyFoo = Foo()
    val result = invokeSingleFile(constructorFoo, classBodyFoo)
    result shouldBeSameInstanceAs constructorFoo
  }

  @Test fun testPrefersSubClassInjectableOverSuperClassInjectable() = singleAndMultiCodegen(
    """
      abstract class MySuperClass(@Provide val superClassFoo: Foo)
      class MySubClass(@Provide val subClassFoo: Foo, superClassFoo: Foo) : MySuperClass(superClassFoo) {
        fun finalFoo(): Foo = inject()
      }
    """,
    """
      fun invoke(subClassFoo: Foo, superClassFoo: Foo): Foo {
        return MySubClass(subClassFoo, superClassFoo).finalFoo()
      } 
    """
  ) {
    val subClassFoo = Foo()
    val superClassFoo = Foo()
    val result = invokeSingleFile(subClassFoo, superClassFoo)
    result shouldBeSameInstanceAs subClassFoo
  }

  @Test fun testPrefersFunctionParameterInjectableOverInternalInjectable() = codegen(
    """
      @Provide lateinit var internalFoo: Foo
      fun invoke(internal: Foo, @Provide functionFoo: Foo): Foo {
        internalFoo = internal
        return inject()
      }
    """
  ) {
    val internal = Foo()
    val functionFoo = Foo()
    val result = invokeSingleFile(internal, functionFoo)
    functionFoo shouldBeSameInstanceAs result
  }

  @Test fun testPrefersFunctionParameterInjectableOverClassInjectable() = codegen(
    """
      class MyClass(@Provide val classFoo: Foo) {
        fun resolve(@Provide functionFoo: Foo) = inject<Foo>()
      }

      fun invoke(classFoo: Foo, functionFoo: Foo): Foo {
        return MyClass(classFoo).resolve(functionFoo)
      }
    """
  ) {
    val classFoo = Foo()
    val functionFoo = Foo()
    val result = invokeSingleFile(classFoo, functionFoo)
    functionFoo shouldBeSameInstanceAs result
  }

  @Test fun testPrefersFunctionReceiverInjectableOverInternalInjectable() = codegen(
    """
      @Provide lateinit var internalFoo: Foo
      fun Foo.invoke(internal: Foo): Foo {
        internalFoo = internal
        return inject()
      }
    """
  ) {
    val internal = Foo()
    val functionFoo = Foo()
    val result = invokeSingleFile(functionFoo, internal)
    functionFoo shouldBeSameInstanceAs result
  }

  @Test fun testPrefersFunctionReceiverInjectableOverClassInjectable() = codegen(
    """
      class MyClass(@Provide val classFoo: Foo) {
        fun Foo.resolve() = inject<Foo>()
      }

      fun invoke(classFoo: Foo, functionFoo: Foo): Foo {
        return with(MyClass(classFoo)) {
          functionFoo.resolve()
        }
      }
    """
  ) {
    val classFoo = Foo()
    val functionFoo = Foo()
    val result = invokeSingleFile(classFoo, functionFoo)
    functionFoo shouldBeSameInstanceAs result
  }

  @Test fun testPrefersProviderArgument() = codegen(
    """
      @Provide fun foo() = Foo()
      fun invoke(foo: Foo) = inject<(Foo) -> Foo>()(foo)
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testPrefersInnerProviderArgumentOverOuterProviderArgument() = codegen(
    """
      @Provide fun foo() = Foo()
      fun invoke(foo: Foo) = inject<(Foo) -> (Foo) -> Foo>()(Foo())(foo)
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
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

  @Test fun testDoesNotUseFrameworkInjectablesIfThereAreUserInjectables() = singleAndMultiCodegen(
    """
      @Provide fun <T> diyProvider(unit: Unit): () -> T = { TODO() } 
    """,
    """
      fun invoke() = inject<() -> Foo>() 
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type kotlin.Unit for parameter unit of function com.ivianuu.injekt.integrationtests.diyProvider")
  }

  @Test fun testPrefersAnyInjectablesOverTypeScopeInjectable() = singleAndMultiCodegen(
    """
      class Dep(val value: String) {
        companion object {
          @Provide val string = "b"
          @Provide fun dep(string: String) = Dep(string)
        }
      }
    """,
    """
      fun invoke(@Provide string: String) = inject<Dep>().value
    """
  ) {
    invokeSingleFile("a") shouldBe "a"
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
          "do all match type com.ivianuu.injekt.test.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.invoke.inner"
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
          "ambiguous injectables of type com.ivianuu.injekt.test.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.bar.\n" +
          "\n" +
          "I found:\n" +
          "\n" +
          "  com.ivianuu.injekt.integrationtests.invoke.inner(\n" +
          "    bar = com.ivianuu.injekt.integrationtests.bar(\n" +
          "      foo = /* ambiguous: com.ivianuu.injekt.integrationtests.foo1, com.ivianuu.injekt.integrationtests.foo2 do match type com.ivianuu.injekt.test.Foo */ inject<com.ivianuu.injekt.test.Foo>()\n" +
          "    )\n" +
          "  )\n" +
          "\n" +
          "but\n" +
          "\n" +
          "com.ivianuu.injekt.integrationtests.foo1\n" +
          "com.ivianuu.injekt.integrationtests.foo2\n" +
          "\n" +
          "do all match type com.ivianuu.injekt.test.Foo."
    )
  }

  @Test fun testPrefersNearerImport() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            object MyInjectables {
              @Provide val a = "a"
              @Provide val b = "b"
            }
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke() = @Providers("injectables.MyInjectables.a") run {
              @Providers("injectables.MyInjectables.b") run {
                inject<String>()
              }
            }
          """
        )
      )
    )
  ) {
    invokeSingleFile() shouldBe "b"
  }

  @Test fun testPrefersNearerImportOverLocalDeclaration() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val value = "b"
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke(): String {
              @Provide val a = "a"
              return run {
                @Providers("injectables.value") inject<String>()
              }
            }
          """
        )
      )
    )
  ) {
    invokeSingleFile() shouldBe "b"
  }

  @Test fun testPrefersExplicitImportOverStarImport() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val value = "explicit"
          """,
          packageFqName = FqName("explicit")
        ),
        source(
          """
            @Provide val value = "star"
          """,
          packageFqName = FqName("star")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("explicit.value", "star.*")
            fun invoke() = inject<String>()
          """
        )
      )
    )
  ) {
    invokeSingleFile() shouldBe "explicit"
  }

  @Test fun testPrefersExplicitExternalImportOverInternalStarImport() = multiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val value = "explicit"
          """,
          packageFqName = FqName("explicit")
        )
      ),
      listOf(
        source(
          """
            @Provide val value = "star"
          """,
          packageFqName = FqName("internal")
        ),
        invokableSource(
          """
            @Providers("explicit.value", "internal.*")
            fun invoke() = inject<String>()
          """
        )
      )
    )
  ) {
    invokeSingleFile() shouldBe "explicit"
  }

  @Test fun testPrefersExplicitImportOverStarImport2() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            interface Logger

            @Provide object NoopLogger : Logger

            @Provide class PrintingLogger : Logger
          """,
          packageFqName = FqName("injectables")
        ),
        source(
          """
            @Provide class AndroidLogger : Logger {
              companion object {
                @Provide inline fun logger(
                  android: () -> AndroidLogger,
                  noop: () -> NoopLogger
                ): Logger = android()
              }
            }
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("injectables.*", "injectables.AndroidLogger.Companion.logger")
            fun invoke() = inject<injectables.Logger>()
          """
        )
      )
    )
  ) {
    invokeSingleFile()!!.javaClass.name shouldBe "injectables.AndroidLogger"
  }

  @Test fun testDoesNotPreferInjectablesInTheSameFile() = codegen(
    """
      @Provide val otherFoo = Foo()
    """,
    """
      @Provide val foo = Foo()

      fun invoke() {
        inject<Foo>()
      }
    """
  ) {
    compilationShouldHaveFailed(
      "ambiguous injectables:\n\n" +
          "com.ivianuu.injekt.integrationtests.foo\n" +
          "com.ivianuu.injekt.integrationtests.otherFoo\n\n" +
          "do all match type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.inject"
    )
  }

  @Test fun testPrefersInternalTypeScopeInjectableOverExternal() = multiCodegen(
    listOf(
      listOf(
        source(
          """
            @JvmInline value class MyType(val value: String)
          """,
          packageFqName = FqName("typescope")
        )
      ),
      listOf(
        source(
          """
            @Provide val externalMyType = MyType("external")
          """,
          packageFqName = FqName("typescope")
        )
      ),
      listOf(
        source(
          """
            @Provide val internalMyType = MyType("internal")
          """,
          packageFqName = FqName("typescope")
        ),
        invokableSource(
          """
            fun invoke() = inject<typescope.MyType>().value
          """
        )
      )
    )
  ) {
    invokeSingleFile() shouldBe "internal"
  }

  @Test fun testPrefersSameCompilationAsTypeTypeScopeInjectableOverExternal() = multiCodegen(
    listOf(
      listOf(
        source(
          """
            @JvmInline value class MyType(val value: String)
            @Provide val typeMyType = MyType("type")
          """,
          packageFqName = FqName("typescope")
        )
      ),
      listOf(
        source(
          """
            @Provide val externalMyType = MyType("external")
          """,
          packageFqName = FqName("typescope")
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke() = inject<typescope.MyType>().value
          """
        )
      )
    )
  ) {
    invokeSingleFile() shouldBe "type"
  }

  @Test fun testPrefersInternalTypeScopeInjectableOverSameCompilationAsType() = multiCodegen(
    listOf(
      listOf(
        source(
          """
            @JvmInline value class MyType(val value: String)
            @Provide val typeMyType = MyType("type")
          """,
          packageFqName = FqName("typescope")
        )
      ),
      listOf(
        source(
          """
            @Provide val internalMyType = MyType("internal")
          """,
          packageFqName = FqName("typescope")
        ),
        invokableSource(
          """
            fun invoke() = inject<typescope.MyType>()  
          """
        )
      )
    )
  ) {
    invokeSingleFile() shouldBe "internal"
  }

  @Test fun testPrefersTypeScopeInjectableOverNonAmbiguityError() = singleAndMultiCodegen(
    """
      class MyDep {
        companion object {
          @Provide val myDep = MyDep()
        }
      }
  
      @Provide class Context1<A>(@Provide val a: A)
    """,
    """
      fun invoke() = inject<MyDep>()
    """
  )

  @Test fun testPrefersAmbiguityErrorOverTypeScopeInjectable() = singleAndMultiCodegen(
    """
      class MyDep {
        object TypeScope {
          @Provide val myDep = MyDep()
        }
      }

      @Provide val a = MyDep()
      @Provide val b = MyDep()
    """,
    """
      fun invoke() = inject<MyDep>()
    """
  ) {
    compilationShouldHaveFailed("ambiguous injectables")
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

  @Test fun testDoesPreferShorterChain() = codegen(
    """
      @Provide class FooModule {
        @Provide fun foo() = Foo()
      }

      fun createFoo(@Inject module: FooModule, @Inject foo: Foo) = inject<Foo>()
    """,
    """
      fun invoke(foo: Foo) = createFoo(FooModule(), foo)
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }
}
