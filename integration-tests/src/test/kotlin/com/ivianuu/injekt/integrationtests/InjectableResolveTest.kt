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

import com.ivianuu.injekt.test.Bar
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.singleAndMultiCodegen
import com.ivianuu.injekt.test.source
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldBeTypeOf
import org.jetbrains.kotlin.name.FqName
import org.junit.Test

class InjectableResolveTest {
  @Test fun testResolvesExternalInjectableInSamePackage() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = inject<Foo>()
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesExternalInjectableInDifferentPackage() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        source(
          """
            @Providers("injectables.*")
            fun invoke() = inject<Foo>()
          """,
          name = "File.kt"
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesInternalInjectableFromDifferentPackageWithAllUnderImport() = codegen(
    listOf(
      source(
        """
          @Provide val foo = Foo()
        """,
        packageFqName = FqName("injectables")
      ),
      source(
        """
          @Providers("injectables.*")
          fun invoke() = inject<Foo>()
        """,
        name = "File.kt"
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesInjectableInSamePackageAndSameFile() = codegen(
    """
      @Provide val foo = Foo()
      fun invoke() = inject<Foo>()
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesClassCompanionInjectableFromWithinTheClass() = singleAndMultiCodegen(
    """
      class MyClass {
        fun resolve() = inject<Foo>()
        companion object {
          @Provide val foo = Foo()
        }
      }
    """,
    """
      fun invoke() = MyClass().resolve() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesClassCompanionInjectableFromOuterClass() = singleAndMultiCodegen(
    """
      class MyClass {
        companion object {
          @Provide val foo = Foo()
        }
      }
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesClassCompanionClassInjectableFromOuterClass() = singleAndMultiCodegen(
    """
      class MyClass {
        companion object {
          @Provide class MyModule {
            @Provide val foo = Foo()
          }
        }
      }
    """,
    """
        fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesClassCompanionInjectableFromClassConstructor() = singleAndMultiCodegen(
    """
      class MyClass(val foo: Foo = inject()) {
        companion object {
          @Provide val foo = Foo()
        }
      }
    """,
    """
      fun invoke() = MyClass().foo
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesClassConstructorPropertyInjectable() = singleAndMultiCodegen(
    """
      class MyClass(@Provide val foo: Foo = Foo()) {
        fun resolve() = inject<Foo>()
      }
    """,
    """
      fun invoke() = MyClass().resolve()
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesClassInjectable() = singleAndMultiCodegen(
    """
      class MyClass {
        @Provide val foo = Foo()
        fun resolve() = inject<Foo>()
      }
    """,
    """
      fun invoke() = MyClass().resolve() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testDerivedInjectable() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide val bar: Bar = Bar(inject())
    """,
    """
      fun invoke() = inject<Bar>() 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Bar>()
  }

  @Test fun testCanResolveSubTypeOfInjectable() = singleAndMultiCodegen(
    """
      interface Repo
      @Provide class RepoImpl : Repo
    """,
    """
      fun invoke() = inject<Repo>() 
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testUnresolvedInjectable() = codegen(
    """
      fun invoke() {
        inject<String>()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type kotlin.String")
  }

  @Test fun testNestedUnresolvedInjectable() = singleAndMultiCodegen(
    """
      @Provide fun bar(foo: Foo) = Bar(foo)
    """,
    """
      fun invoke() = inject<Bar>() 
    """
  ) {
    compilationShouldHaveFailed(" no injectable found of type com.ivianuu.injekt.test.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.bar")
  }

  @Test fun testGenericInjectable() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide fun <T> injectableList(value: T): List<T> = listOf(value)
    """,
    """
      fun invoke() = inject<List<Foo>>() 
    """
  ) {
    val (foo) = invokeSingleFile<List<Any>>()
    foo.shouldBeTypeOf<Foo>()
  }

  @Test fun testFunctionInvocationWithInjectables() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      fun usesFoo(@Inject foo: Foo) {
      }
    """,
    """
      fun invoke() {
        usesFoo()
      } 
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testLocalFunctionInvocationWithInjectables() = codegen(
    """
      @Provide val foo = Foo()
      fun invoke() {
        fun usesFoo(@Inject foo: Foo) {
        }                    
        usesFoo()
      }
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testConstructorInvocationWithInjectables() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      class UsesFoo(@Inject foo: Foo)
    """,
    """
      fun invoke() {
        UsesFoo()
      } 
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testPrimaryConstructorInjectableWithReceiver() = singleAndMultiCodegen(
    """
      class UsesFoo(@Provide val foo: Foo)
    """,
    """
      fun invoke(foo: Foo) = with(UsesFoo(foo)) {
        inject<Foo>()
      }
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testLocalConstructorInvocationWithInjectables() = codegen(
    """
      @Provide val foo = Foo()
      fun invoke() {
        class UsesFoo(@Inject foo: Foo)
        UsesFoo()
      }
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testCanResolveInjectableOfInjectableThisFunction() = codegen(
    """
      class Dep(@Provide val foo: Foo)
      fun invoke(foo: Foo) = with(Dep(foo)) { inject<Foo>() }
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testCanResolveInjectableWhichDependsOnAssistedInjectableOfTheSameType() = singleAndMultiCodegen(
    """
      typealias SpecialScope = Unit
      
      @Provide fun <E> asRunnable(factory: (@Provide SpecialScope) -> List<E>): List<E> = factory(Unit)
      
      @Provide fun raw(scope: SpecialScope): List<String> = listOf("")
    """,
    """
      fun invoke() = inject<List<String>>()
    """
  )

  @Test fun testCanResolveStarProjectedType() = singleAndMultiCodegen(
    """
      @Provide fun foos() = Foo() to Foo()
      
      @Tag annotation class First
      @Provide fun <A : @First B, B> first(pair: Pair<B, *>): A = pair.first as A
    """,
    """
      fun invoke() = inject<@First Foo>() 
    """
  )

  @Test fun testCannotResolveObjectWithoutInjectable() = singleAndMultiCodegen(
    """
      object MyObject
    """,
    """
      fun invoke() = inject<MyObject>() 
    """
  ) {
    compilationShouldHaveFailed("no injectable")
  }

  @Test fun testCannotResolveObjectBySubTypeWithoutInjectable() = codegen(
    """
      interface Json {
        companion object : Json
      }
    """,
    """
      fun invoke() = inject<Json>()
    """
  ) {
    compilationShouldHaveFailed("no injectable")
  }

  @Test fun testCanResolveObjectWithInjectable() = singleAndMultiCodegen(
    """
      @Provide object MyObject
    """,
    """
      fun invoke() = inject<MyObject>() 
    """
  )

  @Test fun testCannotResolveExternalInternalInjectable() = multiCodegen(
    """
      @Provide internal val foo = Foo()
    """,
    """
     fun invoke() = inject<Foo>()
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo")
  }

  @Test fun testCannotResolvePrivateClassInjectableFromOuterScope() = singleAndMultiCodegen(
    """
      @Provide class FooHolder {
        @Provide private val foo = Foo()
      }
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo")
  }

  @Test fun testCanResolvePrivateClassInjectableFromInnerScope() = codegen(
    """
      @Provide class FooHolder {
        @Provide private val foo = Foo()
        fun invoke() = inject<Foo>()
      }
    """
  )

  @Test fun testCannotResolveProtectedInjectableFromOuterScope() = singleAndMultiCodegen(
    """
      @Provide open class FooHolder {
        @Provide protected val foo = Foo()
      }
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo")
  }

  @Test fun testCanResolveProtectedInjectableFromSameClass() = codegen(
    """
      @Provide open class FooHolder {
        @Provide protected val foo = Foo()
        fun invoke() = inject<Foo>()
      }
    """
  )

  @Test fun testCanResolveProtectedInjectableFromSubClass() = singleAndMultiCodegen(
    """
      abstract class AbstractFooHolder {
        @Provide protected val foo = Foo()
      }
    """,
    """
      class FooHolderImpl : AbstractFooHolder() {
        fun invoke() = inject<Foo>()
      } 
    """
  )

  @Test fun testCanResolvePrivateTopLevelInjectableInSameFile() = codegen(
    """
      @Provide private val foo = Foo()
      fun invoke() = inject<Foo>()
    """
  )

  @Test fun testCanResolvePrivateTopLevelInjectableInSameFileMultiFile() = codegen(
    """
      // triggers creation of package scope
      fun invoke(@Provide unit: Unit) {
        inject<Unit>()
      }
    """,
    """
      @Provide private val foo = Foo()
      fun invoke() = inject<Foo>()
    """
  )

  @Test fun testCannotResolvePrivateTopLevelInjectableInDifferentFile() = codegen(
    """
      @Provide private val foo = Foo()
    """,
    """
      fun invoke() = inject<Foo>()
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo")
  }

  @Test fun testCannotResolvePropertyWithTheSameNameAsAnInjectablePrimaryConstructorParameter() =
    singleAndMultiCodegen(
      """
        @Provide class MyClass(foo: Foo) {
          val foo = foo
        }
      """,
      """
        fun invoke() = inject<Foo>() 
      """
    ) {
      compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.inject")
    }

  @Test fun testAnonymousObjectCanResolveInjectablesOfOuterClass() = codegen(
    """
      class MyClass {
        @Provide private val foo = Foo()

        fun function() {
          object : Any() {
            override fun equals(other: Any?): Boolean {
              inject<Foo>()
              return super.equals(other)
            }
          }
        }
      }
    """
  )

  @Test fun testCanResolveExplicitMarkedInjectableConstructorParameterFromOutsideTheClass() =
    singleAndMultiCodegen(
      """
        class MyClass(@Provide val foo: Foo)
      """,
      """
        fun invoke(@Provide myClass: MyClass) = inject<Foo>() 
      """
    )

  @Test fun testCannotResolveImplicitInjectableConstructorParameterFromOutsideTheClass() =
    singleAndMultiCodegen(
      """
        @Provide class MyClass(val foo: Foo)
      """,
      """
        fun invoke() = inject<Foo>() 
      """
    ) {
      compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.inject")
    }

  @Test fun testCanResolveImplicitInjectableConstructorParameterFromInsideTheClass() = codegen(
    """
      @Provide class MyClass(private val foo: Foo) {
        fun invoke() = inject<Foo>()
      }
    """
  )

  @Test fun testResolvesInjectableWithTypeParameterInScope() = singleAndMultiCodegen(
    """
      @Provide fun <T> list(): List<T> = emptyList()
    """,
    """
      fun <T> invoke() {
        inject<List<T>>()
      } 
    """
  )

  @Test fun testCannotUseNonReifiedTypeParameterForReifiedInjectable() = singleAndMultiCodegen(
    """
      @Provide inline fun <reified T> list(): List<T> {
        T::class
        return emptyList()
      }
    """,
    """
      fun <T> invoke() {
        inject<List<T>>()
      }
    """
  ) {
    compilationShouldHaveFailed(
      "type parameter T of injectable com.ivianuu.injekt.integrationtests.list() of type kotlin.collections.List<com.ivianuu.injekt.integrationtests.invoke.T> for parameter value of function com.ivianuu.injekt.inject is reified but type argument com.ivianuu.injekt.integrationtests.invoke.T is not reified"
    )
  }

  @Test fun testCannotResolveUnparameterizedSubTypeOfParameterizedInjectable() = singleAndMultiCodegen(
    """
      typealias TypedString<T> = String
  
      @Provide val foo = Foo()
  
      @Provide fun <T : Foo> typedString(value: T): TypedString<T> = ""
    """,
    """
      fun invoke() = inject<String>() 
    """
  )

  @Test fun testCannotResolveUnparameterizedSubTypeOfParameterizedInjectableWithTags() =
    singleAndMultiCodegen(
      """
        typealias TypedString<T> = String

        @Provide val foo = Foo()

        @Provide fun <T : Foo> typedString(value: T): @TypedTag<T> TypedString<T> = ""
      """,
      """
        fun invoke() = inject<@TypedTag<Foo> String>() 
      """
    )

  @Test fun testSafeCallWithInject() = singleAndMultiCodegen(
      """
        @Provide val foo = Foo()

        fun String.myFunc(@Inject foo: Foo) {
        }
      """,
      """
        fun invoke() = (null as? String)?.myFunc()
      """
    )

  @Test fun testInvocationOfFunctionDeclaredInSuperClassWithInjectParameters() = singleAndMultiCodegen(
    """
      open class MySuperClass {
        fun func(@Inject foo: Foo) {
        }
      }

      class MySubClass : MySuperClass()
    """,
    """
      fun invoke(@Inject foo: Foo) = MySubClass().func()
    """
  )

  @Test fun testInvocationOfFunctionDeclaredInSuperClassWithGenericInjectParameters() = singleAndMultiCodegen(
    """
      open class MySuperClass<T> {
        fun <S : T> func(@Inject s: S) {
        }
      }

      class MySubClass<T> : MySuperClass<T>()
    """,
    """
      fun invoke(@Inject foo: Foo) = MySubClass<Any>().func<Foo>()
    """
  )

  @Test fun testCanResolveProvideParameterInDefaultValueOfFollowingParameter() = codegen(
    """
      fun invoke(@Provide foo: Foo, bar: Bar = Bar(inject())) {
      }
    """
  )

  @Test fun testCannotResolveProvideParameterInDefaultValueOfPreviousParameter() = codegen(
    """
      fun invoke(bar: Bar = Bar(inject()), @Provide foo: Foo) {
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testCanResolveReceiverInDefaultValueOfParameter() = codegen(
    """
      fun Foo.invoke(bar: Bar = Bar(inject())) {
      }
    """
  )

  @Test fun testCannotResolveClassProvideDeclarationInClassConstructorParameterDefaultValue() = codegen(
    """
      class MyClass {
        @Provide val foo = Foo()
        constructor(foo: Foo = inject())
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testCanResolvePrimaryConstructorInjectableInSuperTypeExpression() = codegen(
    """
      interface FooHolder {
        val foo: Foo
      }
      fun FooHolder(@Inject foo: Foo) = object : FooHolder {
        override val foo = foo
      }
      class MyClass(@Provide foo: Foo) : FooHolder by FooHolder()
    """
  )

  @Test fun testCannotResolveSecondaryConstructorInjectableInSuperTypeExpression() = codegen(
    """
      interface FooHolder {
        val foo: Foo
      }
      fun FooHolder(@Inject foo: Foo) = object : FooHolder {
        override val foo = foo
      }
      class MyClass() : FooHolder by FooHolder() {
        constructor(@Provide foo: Foo) : this()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.FooHolder")
  }

  @Test fun testCannotResolveClassProvideDeclarationInSuperTypeExpression() = codegen(
    """
      abstract class MyAbstractClass(@Inject foo: Foo)
      class MyClass : MyAbstractClass() {
        @Provide val foo = Foo()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.MyAbstractClass.<init>")
  }

  @Test fun testCanResolveClassProvideDeclarationInSecondaryConstructorAfterSuperInit() = codegen(
    """
      class MyClass(unit: Unit) {
        constructor() : this(Unit) {
          inject<Foo>()
        }
        @Provide val foo = Foo()
      }
    """
  )

  @Test fun testCannotResolveClassProvideDeclarationInSecondaryConstructorBeforeSuperInit() = codegen(
    """
      class MyClass(foo: Foo) {
        constructor() : this(inject())
        @Provide val foo = Foo()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testCanResolvePrimaryConstructorInjectableInInit() = codegen(
    """
      class MyClass(@Provide foo: Foo) {
        init {
          inject<Foo>()
        }
      }
    """
  )

  @Test fun testCanResolvePrimaryConstructorInjectableInPropertyInitializer() = codegen(
    """
      class MyClass(@Provide _foo: Foo) {
        val foo: Foo = inject()
      }
    """
  )

  @Test fun testCanResolvePrimaryConstructorInjectableInPropertyInitializerLambda() = codegen(
    """
      class MyClass(@Provide _foo: Foo) {
        val foo: Foo = run { inject() }
      }
    """
  )

  @Test fun testCanResolvePrimaryConstructorInjectableInPropertyDelegateInitializer() = codegen(
    """
      class MyClass(@Provide _foo: Foo) {
        val foo: Foo by lazy(inject<Foo>()) { Foo() }
      }
    """
  )

  @Test fun testCanResolvePrimaryConstructorInjectableInPropertyDelegateInitializerLambda() = codegen(
    """
      class MyClass(@Provide _foo: Foo) {
        val foo: Foo by lazy { inject<Foo>() }
      }
    """
  )

  @Test fun testCannotResolvePrimaryConstructorParameterInFunctionWithMultipleNestedBlocks() = codegen(
    """
      class MyClass(@Provide val _foo: Foo) {
        fun foo(): Foo = runBlocking {
          run {
            inject()
          }
        }
      }
    """
  )

  @Test fun testCannotResolvePrimaryConstructorInjectableInPropertyGetter() = codegen(
    """
      class MyClass(@Provide _foo: Foo) {
        val foo: Foo get() = inject()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testCannotResolveLocalVariableFromWithinInitializer() = codegen(
    """
      fun invoke() {
        @Provide val foo: Foo = inject()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testCannotResolveLocalVariableFromWithinDelegateInitializer() = codegen(
    """
      fun invoke() {
        @Provide val foo: Foo by lazy(inject<Foo>()) { Foo() }
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testCannotResolveClassPropertyFromWithinInitializer() = codegen(
    """
      class MyClass {
        @Provide private val foo: Foo = inject<Foo>()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testCannotResolveClassPropertyFromWithinDelegateInitializer() = codegen(
    """
      class MyClass {
        @Provide private val foo: Foo by lazy(inject<Foo>()) { Foo() }
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testCanResolveClassPropertyFromOtherPropertyInitializerIfItsDeclaredBeforeIt() = codegen(
    """
      class MyClass {
        @Provide val foo: Foo = Foo()
        @Provide val bar: Bar = Bar(inject())
      }
    """
  )

  @Test fun testCanResolveClassPropertyFromOtherPropertyDelegateInitializerIfItsDeclaredBeforeIt() = codegen(
    """
      class MyClass {
        @Provide val foo: Foo = Foo()
        @Provide val bar: Bar by lazy(inject<Foo>()) { Bar(foo) }
      }
    """
  )

  @Test fun testCannotResolveClassPropertyFromOtherPropertyInitializerIfItsDeclaredAfterIt() = codegen(
    """
      class MyClass {
        @Provide val bar: Bar = Bar(inject())
        @Provide val foo: Foo = Foo()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testCanResolveClassPropertyFromOtherPropertyInitializerLambdaIfItsDeclaredAfterIt() = codegen(
    """
      class MyClass {
        @Provide val bar: Bar = run { Bar(inject()) }
        @Provide val foo: Foo = Foo()
      }
    """
  )

  @Test fun testCannotResolveClassPropertyFromOtherPropertyDelegateInitializerIfItsDeclaredAfterIt() = codegen(
    """
      class MyClass {
        @Provide val bar: Bar by lazy(inject<Foo>()) { Bar(foo) }
        @Provide val foo: Foo = Foo()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testCanResolveClassFunctionFromClassPropertyInitializer() = codegen(
    """
      class MyClass {
        @Provide val bar: Bar = Bar(inject())
        @Provide fun foo() = Foo()
      }
    """
  )

  @Test fun testCanResolveClassFunctionFromClassPropertyDelegateInitializer() = codegen(
    """
      class MyClass {
        @Provide val bar: Bar by lazy(inject<Foo>()) { Bar(foo()) }
        @Provide fun foo() = Foo()
      }
    """
  )

  @Test fun testCanResolveClassComputedPropertyFromClassPropertyInitializer() = codegen(
    """
      class MyClass {
        @Provide val bar: Bar = Bar(inject())
        @Provide val foo get() = Foo()
      }
    """
  )

  @Test fun testCanResolveClassComputedPropertyFromClassPropertyDelegateInitializer() = codegen(
    """
      class MyClass {
        @Provide val bar: Bar by lazy(inject<Foo>()) { Bar(foo) }
        @Provide val foo get() = Foo()
      }
    """
  )

  @Test fun testCanResolveClassPropertyFromClassInitializerIfItsDeclaredBeforeIt() = codegen(
    """
      class MyClass {
        @Provide val foo: Foo = Foo()
        init {
          inject<Foo>()
        }
      }
    """
  )

  @Test fun testCannotResolveClassPropertyFromClassInitializerIfItsDeclaredAfterIt() = codegen(
    """
      class MyClass {
        init {
          inject<Foo>()
        }
        @Provide val foo: Foo = Foo()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testCanResolveClassFunctionFromClassInitializer() = codegen(
    """
      class MyClass {
        init {
          inject<Foo>()
        }
        @Provide fun foo() = Foo()
      }
    """
  )

  @Test fun testCanResolveClassComputedPropertyFromClassInitializer() = codegen(
    """
      class MyClass {
        init {
          inject<Foo>()
        }
        @Provide val foo get() = Foo()
      }
    """
  )

  @Test fun testCannotResolveTopLevelPropertyFromWithinInitializer() = codegen(
    """
      @Provide private val foo: Foo = inject<Foo>()
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testCanResolveTopLevelPropertyFromOtherPropertyInitializerIfItsDeclaredBeforeIt() = codegen(
    """
      @Provide val foo: Foo = Foo()
      @Provide val bar: Bar = Bar(inject())
    """
  )

  @Test fun testCanResolveTopLevelPropertyFromOtherPropertyDelegateInitializerIfItsDeclaredBeforeIt() = codegen(
    """
      @Provide val foo: Foo = Foo()
      @Provide val bar: Bar by lazy(inject<Foo>()) { Bar(foo) }
    """
  )

  @Test fun testCannotResolveTopLevelPropertyFromOtherPropertyInitializerIfItsDeclaredAfterIt() = codegen(
    """
      @Provide val bar: Bar = Bar(inject())
      @Provide val foo: Foo = Foo()
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testCanResolveTopLevelPropertyFromOtherPropertyInitializerLambdaIfItsDeclaredAfterIt() = codegen(
    """
      @Provide val bar: Bar = run { Bar(inject()) }
      @Provide val foo: Foo = Foo()
    """
  )

  @Test fun testCanResolveTopLevelPropertyFromOtherPropertyInitializerInDifferentFile() = codegen(
    """
      @Provide val bar: Bar = Bar(inject())
    """,
    """
      @Provide val foo: Foo = Foo()
    """
  )

  @Test fun testCannotResolveTopLevelPropertyFromOtherPropertyDelegateInitializerIfItsDeclaredAfterIt() = codegen(
    """
      @Provide val bar: Bar by lazy(inject<Foo>()) { Bar(foo) }
      @Provide val foo: Foo = Foo()
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testCanResolveTopLevelFunctionFromTopLevelPropertyInitializer() = codegen(
    """
      @Provide val bar: Bar = Bar(inject())
      @Provide fun foo() = Foo()
    """
  )

  @Test fun testCanResolveTopLevelFunctionFromTopLevelPropertyDelegateInitializer() = codegen(
    """
      @Provide val bar: Bar by lazy(inject<Foo>()) { Bar(inject()) }
      @Provide fun foo() = Foo()
    """
  )

  @Test fun testNestedClassCannotResolveOuterClassInjectables() = codegen(
    """
      class Outer(@Provide val foo: Foo = Foo()) {
        class Inner {
          fun foo() = inject<Foo>()
        }
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testInnerClassCanResolveOuterClassInjectables() = codegen(
    """
      class Outer(@Provide val foo: Foo = Foo()) {
        inner class Inner {
          fun foo() = inject<Foo>()
        }
      }
    """
  )

  @Test fun testNestedClassCanResolveOuterObjectInjectables() = codegen(
    """
      object Outer {
        @Provide private val foo: Foo = Foo()

        class Inner {
          fun foo() = inject<Foo>()
        }
      }
    """
  )

  @Test fun testNestedObjectCannotResolveOuterClassInjectables() = codegen(
    """
      class Outer {
        @Provide private val foo: Foo = Foo()

        object Inner {
          fun foo() = inject<Foo>()
        }
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testTaggedObjectInjectableIsNotApplicableToUntaggedType() = singleAndMultiCodegen(
    """
      interface Logger

      @Provide @Tag1 object NoopLogger : Logger
      
      fun log(@Inject logger: Logger) {
      }
    """,
    """
      fun invoke() = log()
    """
  ) {
    compilationShouldHaveFailed(
      "no injectable found of type com.ivianuu.injekt.integrationtests.Logger for parameter logger of function com.ivianuu.injekt.integrationtests.log"
    )
  }

  @Test fun testCanResolveOuterClassInjectableFromFunctionInsideAnonymousObject() = codegen(
    """
      @Provide class HaloState(val foo: Foo) {
        val pointerInput = object : Any() {
          override fun equals(other: Any?): Boolean {
            inject<Foo>()
            return true
          }
        }
      }
    """
  )

  @Test fun testPropertyDelegateExpressionWithVarParameterFollowedByMultipleInjectParameter() = codegen(
    """
      fun <T> produceState(
        vararg keys: Any?,
        @Inject scope: Scope,
        @Inject key: SourceKey
      ): State<T> = TODO()

      fun invoke(@Inject scope: Scope) {
        val scope by produceState<Int>()
      }
    """
  )
}
