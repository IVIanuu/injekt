/*
 * Copyright 2021 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokableSource
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.singleAndMultiCodegen
import com.ivianuu.injekt.test.source
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.jetbrains.kotlin.name.FqName
import org.junit.Test

class InjectableResolutionTest {
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
      fun invoke(foo: Foo) = inject<(@Provide Foo) -> Foo>()(foo)
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testPrefersInnerProviderArgumentOverOuterProviderArgument() = codegen(
    """
      @Provide fun foo() = Foo()
      fun invoke(foo: Foo) = inject<(@Provide Foo) -> (@Provide Foo) -> Foo>()(Foo())(foo)
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
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
      "ambiguous injectables:\n" +
          "com.ivianuu.injekt.integrationtests.a\n" +
          "com.ivianuu.injekt.integrationtests.b\n" +
          "do all match type kotlin.String for parameter value of function com.ivianuu.injekt.inject"
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
      "ambiguous injectables:\n" +
          "com.ivianuu.injekt.integrationtests.invoke.injectableA\n" +
          "com.ivianuu.injekt.integrationtests.invoke.injectableB\n" +
          "do all match type kotlin.String for parameter value of function com.ivianuu.injekt.inject"
    )
  }

  @Test fun testPrefersMoreSpecificType() = singleAndMultiCodegen(
    """
      @Provide fun stringList(): List<String> = listOf("a", "b", "c")
      @Provide fun <T> anyList(): List<T> = emptyList()
    """,
    """
      fun invoke() = inject<List<String>>() 
    """
  ) {
    listOf("a", "b", "c") shouldBe invokeSingleFile()
  }

  @Test fun testPrefersMoreSpecificType2() = singleAndMultiCodegen(
    """
      @Provide fun <T> list(): List<T> = emptyList()
      @Provide fun <T> listList(): List<List<T>> = listOf(listOf("a", "b", "c")) as List<List<T>>
    """,
    """
      fun invoke() = inject<List<List<String>>>() 
    """
  ) {
    invokeSingleFile() shouldBe listOf(listOf("a", "b", "c"))
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
  )

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

  @Test fun testUsesDefaultValueIfNoCandidateExists() = codegen(
    """
      fun invoke(_foo: Foo): Foo {
        fun inner(foo: Foo = _foo) = foo
        return inner()
      }
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testDoesNotUseDefaultValueIfCandidateHasFailures() = codegen(
    """
      @Provide fun bar(foo: Foo) = Bar(foo)
      fun invoke() {
        fun inner(@Inject bar: Bar = Bar(Foo())) = bar
        return inner()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.bar")
  }

  @Test fun testSpreadingInjectableWithTheSameOrigin() = singleAndMultiCodegen(
    """
      @Provide @MyTag class FooModule {
        @Provide val foo = Foo()
      }

      @Tag annotation class MyTag

      @Provide fun <@Spread T : @MyTag S, S> myTag(instance: T): S = instance
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  )

  @Test fun testSpreadingInjectableWithTheSameOrigin2() = singleAndMultiCodegen(
    """
      abstract class FooModule {
        @Provide val foo = Foo()
        companion object {
          @Provide fun create(): @MyTag FooModule = object : FooModule() {
          }
        }
      }

      @Tag annotation class MyTag

      @Provide fun <@Spread T : @MyTag S, S> myTag(instance: T): S = instance
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  )

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
        source(
          """
            @Providers("explicit.value", "star.*")
            fun invoke() = inject<String>()
          """,
          name = "File.kt"
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
            @Provide @Scoped<AppScope> class AndroidLogger : Logger {
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
        source(
          """
            @Providers("injectables.*", "injectables.AndroidLogger.Companion.logger")
            fun invoke() = inject<injectables.Logger>()
          """,
          name = "File.kt"
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
      "ambiguous injectables:\n" +
          "com.ivianuu.injekt.integrationtests.foo\n" +
          "com.ivianuu.injekt.integrationtests.otherFoo\n" +
          "do all match type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.inject"
    )
  }

  @Test fun testPrefersInternalTypeScopeInjectableOverExternal() = multiCodegen(
    listOf(
      listOf(
        source(
          """
            typealias MyType = String
          """,
          packageFqName = FqName("typescope")
        )
      ),
      listOf(
        source(
          """
            @Provide val externalMyType: MyType = "external"
          """,
          packageFqName = FqName("typescope")
        )
      ),
      listOf(
        source(
          """
            @Provide val internalMyType: MyType = "internal"
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

  @Test fun testPrefersSameCompilationAsTypeTypeScopeInjectableOverExternal() = multiCodegen(
    listOf(
      listOf(
        source(
          """
            typealias MyType = String
            @Provide val typeMyType: MyType = "type"
          """,
          packageFqName = FqName("typescope")
        )
      ),
      listOf(
        source(
          """
            @Provide val externalMyType: MyType = "external"
          """,
          packageFqName = FqName("typescope")
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke() = inject<typescope.MyType>()  
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
            typealias MyType = String
            @Provide val typeMyType: MyType = "type"
          """,
          packageFqName = FqName("typescope")
        )
      ),
      listOf(
        source(
          """
            @Provide val internalMyType: MyType = "internal"
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
}
