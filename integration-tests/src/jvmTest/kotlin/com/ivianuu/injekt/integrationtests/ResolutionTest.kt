/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokableSource
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.irShouldContain
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.singleAndMultiCodegen
import com.ivianuu.injekt.test.source
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.jetbrains.kotlin.name.FqName
import org.junit.Test

class ResolutionTest {
  @Test fun testPrefersInternalProviderOverExternal() = multiCodegen(
    """
      @Provide lateinit var externalFoo: Foo
    """,
    """
      @Provide lateinit var internalFoo: Foo
  
      fun invoke(internal: Foo, external: Foo): Foo {
        externalFoo = external
        internalFoo = internal
        return context<Foo>()
      }
    """
  ) {
    val internal = Foo()
    val external = Foo()
    val result = invokeSingleFile(internal, external)
    result shouldBeSameInstanceAs internal
  }

  @Test fun testPrefersObjectProviderOverInternalProvider() = codegen(
    """
      @Provide lateinit var internalFoo: Foo
      object MyObject {
        @Provide lateinit var objectFoo: Foo
        fun resolve() = context<Foo>()
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

  @Test fun testPrefersClassCompanionProviderOverInternalProvider() = codegen(
    """
      @Provide lateinit var internalFoo: Foo
      class MyClass {
        fun resolve() = context<Foo>()
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

  @Test fun testPrefersClassProviderOverInternalProvider() = codegen(
    """
      @Provide lateinit var internalFoo: Foo
      class MyClass(@Provide val classFoo: Foo) {
        fun resolve() = context<Foo>()
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

  @Test fun testPrefersClassProviderOverClassCompanionProvider() = codegen(
    """
      class MyClass(@Provide val classFoo: Foo) {
        fun resolve() = context<Foo>()
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

  @Test fun testPrefersConstructorParameterProviderOverClassBodyProvider() = codegen(
    """
      lateinit var classBodyFoo: Foo
      class MyClass(@Provide constructorFoo: Foo) {
        val finalFoo = context<Foo>()
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

  @Test fun testPrefersSubClassProviderOverSuperClassProvider() = singleAndMultiCodegen(
    """
      abstract class MySuperClass(@Provide val superClassFoo: Foo)
      class MySubClass(@Provide val subClassFoo: Foo, superClassFoo: Foo) : MySuperClass(superClassFoo) {
        fun finalFoo(): Foo = context()
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

  @Test fun testPrefersFunctionParameterProviderOverInternalProvider() = codegen(
    """
      @Provide lateinit var internalFoo: Foo
      fun invoke(internal: Foo, @Provide functionFoo: Foo): Foo {
        internalFoo = internal
        return context()
      }
    """
  ) {
    val internal = Foo()
    val functionFoo = Foo()
    val result = invokeSingleFile(internal, functionFoo)
    functionFoo shouldBeSameInstanceAs result
  }

  @Test fun testPrefersFunctionParameterProviderOverClassProvider() = codegen(
    """
      class MyClass(@Provide val classFoo: Foo) {
        fun resolve(@Provide functionFoo: Foo) = context<Foo>()
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

  @Test fun testPrefersFunctionReceiverProviderOverInternalProvider() = codegen(
    """
      @Provide lateinit var internalFoo: Foo
      fun Foo.invoke(internal: Foo): Foo {
        internalFoo = internal
        return context()
      }
    """
  ) {
    val internal = Foo()
    val functionFoo = Foo()
    val result = invokeSingleFile(functionFoo, internal)
    functionFoo shouldBeSameInstanceAs result
  }

  @Test fun testPrefersFunctionReceiverProviderOverClassProvider() = codegen(
    """
      class MyClass(@Provide val classFoo: Foo) {
        fun Foo.resolve() = context<Foo>()
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
      fun invoke(foo: Foo) = context<(Foo) -> Foo>()(foo)
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testPrefersInnerProviderArgumentOverOuterProviderArgument() = codegen(
    """
      @Provide fun foo() = Foo()
      fun invoke(foo: Foo) = context<(Foo) -> (Foo) -> Foo>()(Foo())(foo)
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testPrefsInnerBlockProvider() = codegen(
    """
      fun invoke(): Pair<String, String> {
        @Provide val providerA = "a"
        return context<String>() to run {
          @Provide val providerB = "b"
          context<String>()
        }
      }
    """
  ) {
    invokeSingleFile() shouldBe ("a" to "b")
  }

  @Test fun testPrefersResolvableProvider() = singleAndMultiCodegen(
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

  @Test fun testPrefersNearerProviderOverBetterType() = codegen(
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

  @Test fun testAmbiguousProviders() = codegen(
    """
      @Provide val a = "a"
      @Provide val b = "b"
    """,
    """
      fun invoke() = context<String>() 
    """
  ) {
    compilationShouldHaveFailed(
      "ambiguous providers:\n\n" +
          "com.ivianuu.injekt.integrationtests.a\n" +
          "com.ivianuu.injekt.integrationtests.b\n\n" +
          "do all match type kotlin.String for parameter x of function com.ivianuu.injekt.context"
    )
  }

  @Test fun testCannotDeclareMultipleProvidersOfTheSameTypeInTheSameCodeBlock() = codegen(
    """
      fun invoke() {
        @Provide val providerA = "a"
        @Provide val providerB = "b"
        context<String>()
      }
    """
  ) {
    compilationShouldHaveFailed(
      "ambiguous providers:\n\n" +
          "com.ivianuu.injekt.integrationtests.invoke.providerA\n" +
          "com.ivianuu.injekt.integrationtests.invoke.providerB\n\n" +
          "do all match type kotlin.String for parameter x of function com.ivianuu.injekt.context"
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
      fun <T> useOrd(@Context ord: Ord<T>) = ord
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
      fun <T> useOrd(@Context ord: Ord<T>) = ord
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
      fun <T> useOrd(@Context ord: Ord<T>) = ord
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
      fun invoke() = context<String?>() 
    """
  ) {
    invokeSingleFile() shouldBe "nonnull"
  }

  @Test fun testDoesNotUseFrameworkProvidersIfThereAreUserProviders() = singleAndMultiCodegen(
    """
      @Provide fun <T> diyProvider(unit: Unit): () -> T = { TODO() } 
    """,
    """
      fun invoke() = context<() -> Foo>() 
    """
  ) {
    compilationShouldHaveFailed("no provider found of type kotlin.Unit for parameter unit of function com.ivianuu.injekt.integrationtests.diyProvider")
  }

  @Test fun testPrefersAnyProvidersOverTypeScopeProvider() = singleAndMultiCodegen(
    """
      class Dep(val value: String) {
        companion object {
          @Provide val string = "b"
          @Provide fun dep(string: String) = Dep(string)
        }
      }
    """,
    """
      fun invoke(@Provide string: String) = context<Dep>().value
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
        fun inner(@Context foo: Foo = Foo()) = foo
        return inner()
      }
    """
  ) {
    compilationShouldHaveFailed(
      "ambiguous providers:\n\n" +
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
        fun inner(@Context bar: Bar = Bar(foo)) = bar
        return inner()
      }
    """
  ) {
    compilationShouldHaveFailed(
      " \n" +
          "ambiguous providers of type com.ivianuu.injekt.test.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.bar.\n" +
          "\n" +
          "I found:\n" +
          "\n" +
          "  com.ivianuu.injekt.integrationtests.invoke.inner(\n" +
          "    bar = com.ivianuu.injekt.integrationtests.bar(\n" +
          "      foo = /* ambiguous: com.ivianuu.injekt.integrationtests.foo1, com.ivianuu.injekt.integrationtests.foo2 do match type com.ivianuu.injekt.test.Foo */ context<com.ivianuu.injekt.test.Foo>()\n" +
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
            object MyProviders {
              @Provide val a = "a"
              @Provide val b = "b"
            }
          """,
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke() = @Providers("providers.MyProviders.a") run {
              @Providers("providers.MyProviders.b") run {
                context<String>()
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
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke(): String {
              @Provide val a = "a"
              return run {
                @Providers("providers.value") context<String>()
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
            fun invoke() = context<String>()
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
            fun invoke() = context<String>()
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
          packageFqName = FqName("providers")
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
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("providers.*", "providers.AndroidLogger.Companion.logger")
            fun invoke() = context<providers.Logger>()
          """
        )
      )
    )
  ) {
    invokeSingleFile()!!.javaClass.name shouldBe "providers.AndroidLogger"
  }

  @Test fun testDoesNotPreferProvidersInTheSameFile() = codegen(
    """
      @Provide val otherFoo = Foo()
    """,
    """
      @Provide val foo = Foo()

      fun invoke() {
        context<Foo>()
      }
    """
  ) {
    compilationShouldHaveFailed(
      "ambiguous providers:\n\n" +
          "com.ivianuu.injekt.integrationtests.foo\n" +
          "com.ivianuu.injekt.integrationtests.otherFoo\n\n" +
          "do all match type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.context"
    )
  }

  @Test fun testPrefersInternalTypeScopeProviderOverExternal() = multiCodegen(
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
            fun invoke() = context<typescope.MyType>().value
          """
        )
      )
    )
  ) {
    invokeSingleFile() shouldBe "internal"
  }

  @Test fun testPrefersSameCompilationAsTypeTypeScopeProviderOverExternal() = multiCodegen(
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
            fun invoke() = context<typescope.MyType>().value
          """
        )
      )
    )
  ) {
    invokeSingleFile() shouldBe "type"
  }

  @Test fun testPrefersInternalTypeScopeProviderOverSameCompilationAsType() = multiCodegen(
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
            fun invoke() = context<typescope.MyType>()  
          """
        )
      )
    )
  ) {
    invokeSingleFile() shouldBe "internal"
  }

  @Test fun testPrefersTypeScopeProviderOverNonAmbiguityError() = singleAndMultiCodegen(
    """
      class MyDep {
        companion object {
          @Provide val myDep = MyDep()
        }
      }
  
      @Provide class Context1<A>(@Provide val a: A)
    """,
    """
      fun invoke() = context<MyDep>()
    """
  )

  @Test fun testPrefersAmbiguityErrorOverTypeScopeProvider() = singleAndMultiCodegen(
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
      fun invoke() = context<MyDep>()
    """
  ) {
    compilationShouldHaveFailed("ambiguous providers")
  }

  @Test fun testDoesNotPreferValueArgumentOverAnother() = codegen(
    """
      @Provide class FooModule {
        @Provide fun foo() = Foo()
      }

      fun createFoo(@Context foo1: Foo, @Context foo2: Foo) = context<Foo>()
    """
  ) {
    compilationShouldHaveFailed("ambiguous providers")
  }

  @Test fun testDoesPreferShorterChain() = codegen(
    """
      @Provide class FooModule {
        @Provide fun foo() = Foo()
      }

      fun createFoo(@Context module: FooModule, @Context foo: Foo) = context<Foo>()
    """,
    """
      fun invoke(foo: Foo) = createFoo(FooModule(), foo)
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }
}
