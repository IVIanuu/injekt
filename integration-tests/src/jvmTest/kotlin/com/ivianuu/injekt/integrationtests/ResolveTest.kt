/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.*
import io.kotest.matchers.types.*
import org.jetbrains.kotlin.name.*
import org.junit.*

class ResolveTest {
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
        invokableSource(
          """
            @Providers("injectables.*")
            fun invoke() = inject<Foo>()
          """
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
      invokableSource(
        """
          @Providers("injectables.*")
          fun invoke() = inject<Foo>()
        """
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
      class MyClass(@property:Provide val foo: Foo = Foo()) {
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
    compilationShouldHaveFailed("\nno injectable found of type com.ivianuu.injekt.test.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.bar")
  }

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
        class MyClass(@property:Provide val foo: Foo)
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
      compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.inject")
    }

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
      @Provide inline fun <reified T> set(): Set<T> {
        T::class
        return emptySet()
      }
    """,
    """
      fun <T> invoke() {
        inject<Set<T>>()
      }
    """
  ) {
    compilationShouldHaveFailed(
      "type parameter T of injectable com.ivianuu.injekt.integrationtests.set() of type kotlin.collections.Set<com.ivianuu.injekt.integrationtests.invoke.T> for parameter x of function com.ivianuu.injekt.inject is reified but type argument com.ivianuu.injekt.integrationtests.invoke.T is not reified"
    )
  }

  @Test fun testCannotUseNestedNonReifiedTypeParameterForReifiedInjectable() = singleAndMultiCodegen(
    """
      @Provide inline fun <reified T> set(): Set<T> {
        T::class
        return emptySet()
      }
    """,
    """
      fun <T> invoke() {
        inject<Set<List<T>>>()
      }
    """
  ) {
    compilationShouldHaveFailed(
      "type parameter T of injectable com.ivianuu.injekt.integrationtests.set() of type kotlin.collections.Set<kotlin.collections.List<com.ivianuu.injekt.integrationtests.invoke.T>> for parameter x of function com.ivianuu.injekt.inject is reified but type argument com.ivianuu.injekt.integrationtests.invoke.T is not reified"
    )
  }

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
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.inject")
  }

  @Test fun testCannotResolveClassProvideDeclarationInClassConstructorParameterDefaultValue() = codegen(
    """
      class MyClass {
        @Provide val foo = Foo()
        constructor(foo: Foo = inject())
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.inject")
  }

  @Test fun testCanResolvePrimaryConstructorInjectableInSuperTypeExpression() = codegen(
    """
      interface FooHolder {
        val foo: Foo
      }
      fun FooHolder(foo: Foo) = object : FooHolder {
        override val foo = foo
      }
      class MyClass(@Provide foo: Foo) : FooHolder by FooHolder(inject())
    """
  )

  @Test fun testCannotResolveSecondaryConstructorInjectableInSuperTypeExpression() = codegen(
    """
      interface FooHolder {
        val foo: Foo
      }
      fun FooHolder(foo: Foo) = object : FooHolder {
        override val foo = foo
      }
      class MyClass() : FooHolder by FooHolder(inject()) {
        constructor(@Provide foo: Foo) : this()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.inject")
  }

  @Test fun testCannotResolveClassProvideDeclarationInSuperTypeExpression() = codegen(
    """
      abstract class MyAbstractClass(foo: Foo)
      class MyClass : MyAbstractClass(inject()) {
        @Provide val foo = Foo()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.inject.")
  }

  @Test fun testCannotResolveThisInSuperTypeDelegation() = codegen(
    """
      interface Scope

      class MyImpl : Scope by inject()
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Scope for parameter x of function com.ivianuu.injekt.inject")
  }

  @Test fun testCannotResolveThisInSuperTypeDelegation2() = codegen(
    """
      interface Scope

      fun invoke() {
        val myImpl = object : Scope by inject() {
        }
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Scope for parameter x of function com.ivianuu.injekt.inject")
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
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.inject")
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
      class MyClass(@property:Provide val _foo: Foo) {
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
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.inject")
  }

  @Test fun testCannotResolveLocalVariableFromWithinInitializer() = codegen(
    """
      fun invoke() {
        @Provide val foo: Foo = inject()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.inject")
  }

  @Test fun testCannotResolveLocalVariableFromWithinDelegateInitializer() = codegen(
    """
      fun invoke() {
        @Provide val foo: Foo by lazy(inject<Foo>()) { Foo() }
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.inject")
  }

  @Test fun testCannotResolveClassPropertyFromWithinInitializer() = codegen(
    """
      class MyClass {
        @Provide private val foo: Foo = inject<Foo>()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.inject")
  }

  @Test fun testCannotResolveClassPropertyFromWithinDelegateInitializer() = codegen(
    """
      class MyClass {
        @Provide private val foo: Foo by lazy(inject<Foo>()) { Foo() }
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.inject")
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
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.inject")
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
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.inject")
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
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.inject")
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

  @Test fun testCanResolvePropertyOfSuperClassFromClassInit() = codegen(
    """
      abstract class MySuperClass {
        @Provide val foo = Foo()
      }

      class MyClass : MySuperClass() {
        init {
          inject<Foo>()
        }
      }
    """
  )

  @Test fun testCannotResolveTopLevelPropertyFromWithinInitializer() = codegen(
    """
      @Provide private val foo: Foo = inject<Foo>()
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.inject")
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
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.inject")
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
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.inject")
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
      class Outer(@property:Provide val foo: Foo = Foo()) {
        class Inner {
          fun foo() = inject<Foo>()
        }
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.inject")
  }

  @Test fun testInnerClassCanResolveOuterClassInjectables() = codegen(
    """
      class Outer(@property:Provide val foo: Foo = Foo()) {
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
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.inject")
  }

  @Test fun testCanResolveOuterClassInjectableFromFunctionInsideAnonymousObject() = codegen(
    """
      @Provide class HaloState(@property:Provide val foo: Foo) {
        val pointerInput = object : Any() {
          override fun equals(other: Any?): Boolean {
            inject<Foo>()
            return true
          }
        }
      }
    """
  )
}
