/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.Test

class ResolveTest {
  @Test fun testResolvesExternalInjectableInSamePackage() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = context<Foo>()
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesInjectableInSamePackageAndSameFile() = codegen(
    """
      @Provide val foo = Foo()
      fun invoke() = context<Foo>()
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesClassCompanionInjectableFromWithinTheClass() = singleAndMultiCodegen(
    """
      class MyClass {
        fun resolve() = context<Foo>()
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
        @Provide companion object {
          @Provide val foo = Foo()
        }
      }
    """,
    """
      fun invoke() = context<Foo>() 
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
        fun invoke() = context<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesClassCompanionInjectableFromClassConstructor() = singleAndMultiCodegen(
    """
      class MyClass(val foo: Foo = context()) {
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
        fun resolve() = context<Foo>()
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
        fun resolve() = context<Foo>()
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
      @Provide val bar: Bar = Bar(context())
    """,
    """
      fun invoke() = context<Bar>() 
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
      fun invoke() = context<Repo>() 
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testUnresolvedInjectable() = codegen(
    """
      fun invoke() {
        context<String>()
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
      fun invoke() = context<Bar>() 
    """
  ) {
    compilationShouldHaveFailed("\nno injectable found of type com.ivianuu.injekt.integrationtests.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.bar")
  }

  @Test fun testGenericInjectable() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide fun <T> injectableList(value: T): List<T> = listOf(value)
    """,
    """
      fun invoke() = context<List<Foo>>() 
    """
  ) {
    val (foo) = invokeSingleFile<List<Any>>()
    foo.shouldBeTypeOf<Foo>()
  }

  @Test fun testCanResolveStarProjectedType() = singleAndMultiCodegen(
    """
      @Provide fun foos() = Foo() to Foo()
      
      @Tag annotation class First
      @Provide fun <A : @First B, B> first(pair: Pair<B, *>): A = pair.first as A
    """,
    """
      fun invoke() = context<@First Foo>() 
    """
  )

  @Test fun testCannotResolveObjectWithoutInjectable() = singleAndMultiCodegen(
    """
      object MyObject
    """,
    """
      fun invoke() = context<MyObject>() 
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
      fun invoke() = context<Json>()
    """
  ) {
    compilationShouldHaveFailed("no injectable")
  }

  @Test fun testCanResolveObjectWithInjectable() = singleAndMultiCodegen(
    """
      @Provide object MyObject
    """,
    """
      fun invoke() = context<MyObject>() 
    """
  )

  @Test fun testCannotResolveExternalInternalInjectable() = multiCodegen(
    """
      @Provide internal val foo = Foo()
    """,
    """
     fun invoke() = context<Foo>()
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo")
  }

  @Test fun testCannotResolvePrivateClassInjectableFromOuterScope() = singleAndMultiCodegen(
    """
      @Provide class FooHolder {
        @Provide private val foo = Foo()
      }
    """,
    """
      fun invoke() = context<Foo>() 
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo")
  }

  @Test fun testCanResolvePrivateClassInjectableFromInnerScope() = codegen(
    """
      @Provide class FooHolder {
        @Provide private val foo = Foo()
        fun invoke() = context<Foo>()
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
      fun invoke() = context<Foo>() 
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo")
  }

  @Test fun testCanResolveProtectedInjectableFromSameClass() = codegen(
    """
      @Provide open class FooHolder {
        @Provide protected val foo = Foo()
        fun invoke() = context<Foo>()
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
        fun invoke() = context<Foo>()
      }
    """
  )

  @Test fun testCanResolvePrivateTopLevelInjectableInSameFile() = codegen(
    """
      @Provide private val foo = Foo()
      fun invoke() = context<Foo>()
    """
  )

  @Test fun testCannotResolvePrivateTopLevelInjectableInDifferentFile() = codegen(
    """
      @Provide private val foo = Foo()
    """,
    """
      fun invoke() = context<Foo>()
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo")
  }

  @Test fun testAnonymousObjectCanResolveInjectablesOfOuterClass() = codegen(
    """
      class MyClass {
        @Provide private val foo = Foo()

        fun function() {
          object : Any() {
            override fun equals(other: Any?): Boolean {
              context<Foo>()
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
        fun invoke(@Provide myClass: MyClass) = context<Foo>() 
      """
    )

  @Test fun testCannotResolveImplicitInjectableConstructorParameterFromOutsideTheClass() =
    singleAndMultiCodegen(
      """
        @Provide class MyClass(val foo: Foo)
      """,
      """
        fun invoke() = context<Foo>() 
      """
    ) {
      compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo for parameter \$contextReceiver_0 of function com.ivianuu.injekt.context")
    }

  @Test fun testCanResolveImplicitInjectableConstructorParameterFromInsideTheClass() = codegen(
    """
      @Provide class MyClass(@property:Provide private val foo: Foo) {
        fun invoke() = context<Foo>()
      }
    """
  )

  fun main() {
    fun <T> lol() {

    }
  }

  @Test fun testResolvesInjectableWithTypeParameterInScope() = singleAndMultiCodegen(
    """
      @Provide fun <T> list(): List<T> = emptyList()
    """,
    """
      fun <T> invoke() {
        context<List<T>>()
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
        context<Set<T>>()
      }
    """
  ) {
    compilationShouldHaveFailed(
      "type parameter T of injectable com.ivianuu.injekt.integrationtests.set() of type kotlin.collections.Set<com.ivianuu.injekt.integrationtests.invoke.T> for parameter \$contextReceiver_0 of function com.ivianuu.injekt.context is reified but type argument com.ivianuu.injekt.integrationtests.invoke.T is not reified"
    )
  }

  @Test fun testCanResolveProvideParameterInDefaultValueOfFollowingParameter() = codegen(
    """
      fun invoke(@Provide foo: Foo, bar: Bar = Bar(context())) {
      }
    """
  )

  @Test fun testCannotResolveProvideParameterInDefaultValueOfPreviousParameter() = codegen(
    """
      fun invoke(bar: Bar = Bar(context()), @Provide foo: Foo) {
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo for parameter \$contextReceiver_0 of function com.ivianuu.injekt.context")
  }

  @Test fun testCanResolveDispatchReceiverInDefaultValueOfParameter() = codegen(
    """
      class Dep {
        fun invoke(dep: Dep = context()) {
        }
      }
    """
  )

  @Test fun testCanResolveExtensionReceiverInDefaultValueOfParameter() = codegen(
    """
      fun @receiver:Provide Foo.invoke(bar: Bar = Bar(context())) {
      }
    """
  )

  @Test fun testCanResolveContextReceiverInDefaultValueOfParameter() = codegen(
    """
      context((@Provide Foo)) fun invoke(bar: Bar = Bar(context())) {
      }
    """
  )

  @Test fun testCannotResolveClassProvideDeclarationInClassConstructorParameterDefaultValue() = codegen(
    """
      class MyClass {
        @Provide val foo = Foo()
        constructor(foo: Foo = context())
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo for parameter \$contextReceiver_0 of function com.ivianuu.injekt.context")
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
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.FooHolder")
  }

  @Test fun testCannotResolveClassProvideDeclarationInSuperTypeExpression() = codegen(
    """
      abstract class MyAbstractClass(@Inject foo: Foo)
      class MyClass : MyAbstractClass() {
        @Provide val foo = Foo()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.MyAbstractClass")
  }

  @Test fun testCannotResolveThisInSuperTypeDelegation() = codegen(
    """
      interface Scope

      class MyImpl : Scope by context()
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Scope for parameter \$contextReceiver_0 of function com.ivianuu.injekt.context")
  }

  @Test fun testCannotResolveThisInSuperTypeDelegation2() = codegen(
    """
      interface Scope

      fun invoke() {
        val myImpl = object : Scope by context() {
        }
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Scope for parameter \$contextReceiver_0 of function com.ivianuu.injekt.context")
  }

  @Test fun testCanResolveClassProvideDeclarationInSecondaryConstructorAfterSuperInit() = codegen(
    """
      class MyClass(unit: Unit) {
        constructor() : this(Unit) {
          context<Foo>()
        }
        @Provide val foo = Foo()
      }
    """
  )

  @Test fun testCannotResolveClassProvideDeclarationInSecondaryConstructorBeforeSuperInit() = codegen(
    """
      class MyClass(foo: Foo) {
        constructor() : this(context())
        @Provide val foo = Foo()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo for parameter \$contextReceiver_0 of function com.ivianuu.injekt.context")
  }

  @Test fun testCanResolvePrimaryConstructorInjectableInInit() = codegen(
    """
      class MyClass(@Provide foo: Foo) {
        init {
          context<Foo>()
        }
      }
    """
  )

  @Test fun testCanResolvePrimaryConstructorInjectableInPropertyInitializer() = codegen(
    """
      class MyClass(@Provide _foo: Foo) {
        val foo: Foo = context()
      }
    """
  )

  @Test fun testCanResolvePrimaryConstructorInjectableInPropertyInitializerLambda() = codegen(
    """
      class MyClass(@Provide _foo: Foo) {
        val foo: Foo = run { context() }
      }
    """
  )

  @Test fun testCanResolvePrimaryConstructorInjectableInPropertyDelegateInitializer() = codegen(
    """
      class MyClass(@Provide _foo: Foo) {
        val foo: Foo by lazy(context<Foo>()) { Foo() }
      }
    """
  )

  @Test fun testCanResolvePrimaryConstructorInjectableInPropertyDelegateInitializerLambda() = codegen(
    """
      class MyClass(@Provide _foo: Foo) {
        val foo: Foo by lazy { context<Foo>() }
      }
    """
  )

  @Test fun testCannotResolvePrimaryConstructorParameterInFunctionWithMultipleNestedBlocks() = codegen(
    """
      class MyClass(@property:Provide val _foo: Foo) {
        fun foo(): Foo = runBlocking {
          run {
            context()
          }
        }
      }
    """
  )

  @Test fun testCannotResolvePrimaryConstructorInjectableInPropertyGetter() = codegen(
    """
      class MyClass(@Provide _foo: Foo) {
        val foo: Foo get() = context()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo for parameter \$contextReceiver_0 of function com.ivianuu.injekt.context")
  }

  @Test fun testCannotResolveLocalVariableFromWithinInitializer() = codegen(
    """
      fun invoke() {
        @Provide val foo: Foo = context()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo for parameter \$contextReceiver_0 of function com.ivianuu.injekt.context")
  }

  @Test fun testCannotResolveLocalVariableFromWithinDelegateInitializer() = codegen(
    """
      fun invoke() {
        @Provide val foo: Foo by lazy(context<Foo>()) { Foo() }
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo for parameter \$contextReceiver_0 of function com.ivianuu.injekt.context")
  }

  @Test fun testCannotResolveClassPropertyFromWithinInitializer() = codegen(
    """
      class MyClass {
        @Provide private val foo: Foo = context<Foo>()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo for parameter \$contextReceiver_0 of function com.ivianuu.injekt.context")
  }

  @Test fun testCannotResolveClassPropertyFromWithinDelegateInitializer() = codegen(
    """
      class MyClass {
        @Provide private val foo: Foo by lazy(context<Foo>()) { Foo() }
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo for parameter \$contextReceiver_0 of function com.ivianuu.injekt.context")
  }

  @Test fun testCanResolveClassPropertyFromOtherPropertyInitializerIfItsDeclaredBeforeIt() = codegen(
    """
      class MyClass {
        @Provide val foo: Foo = Foo()
        @Provide val bar: Bar = Bar(context())
      }
    """
  )

  @Test fun testCanResolveClassPropertyFromOtherPropertyDelegateInitializerIfItsDeclaredBeforeIt() = codegen(
    """
      class MyClass {
        @Provide val foo: Foo = Foo()
        @Provide val bar: Bar by lazy(context<Foo>()) { Bar(foo) }
      }
    """
  )

  @Test fun testCannotResolveClassPropertyFromOtherPropertyInitializerIfItsDeclaredAfterIt() = codegen(
    """
      class MyClass {
        @Provide val bar: Bar = Bar(context())
        @Provide val foo: Foo = Foo()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo for parameter \$contextReceiver_0 of function com.ivianuu.injekt.context")
  }

  @Test fun testCanResolveClassPropertyFromOtherPropertyInitializerLambdaIfItsDeclaredAfterIt() = codegen(
    """
      class MyClass {
        @Provide val bar: Bar = run { Bar(context()) }
        @Provide val foo: Foo = Foo()
      }
    """
  )

  @Test fun testCannotResolveClassPropertyFromOtherPropertyDelegateInitializerIfItsDeclaredAfterIt() = codegen(
    """
      class MyClass {
        @Provide val bar: Bar by lazy(context<Foo>()) { Bar(foo) }
        @Provide val foo: Foo = Foo()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo for parameter \$contextReceiver_0 of function com.ivianuu.injekt.context")
  }

  @Test fun testCanResolveClassFunctionFromClassPropertyInitializer() = codegen(
    """
      class MyClass {
        @Provide val bar: Bar = Bar(context())
        @Provide fun foo() = Foo()
      }
    """
  )

  @Test fun testCanResolveClassFunctionFromClassPropertyDelegateInitializer() = codegen(
    """
      class MyClass {
        @Provide val bar: Bar by lazy(context<Foo>()) { Bar(foo()) }
        @Provide fun foo() = Foo()
      }
    """
  )

  @Test fun testCanResolveClassComputedPropertyFromClassPropertyInitializer() = codegen(
    """
      class MyClass {
        @Provide val bar: Bar = Bar(context())
        @Provide val foo get() = Foo()
      }
    """
  )

  @Test fun testCanResolveClassComputedPropertyFromClassPropertyDelegateInitializer() = codegen(
    """
      class MyClass {
        @Provide val bar: Bar by lazy(context<Foo>()) { Bar(foo) }
        @Provide val foo get() = Foo()
      }
    """
  )

  @Test fun testCanResolveClassPropertyFromClassInitializerIfItsDeclaredBeforeIt() = codegen(
    """
      class MyClass {
        @Provide val foo: Foo = Foo()
        init {
          context<Foo>()
        }
      }
    """
  )

  @Test fun testCannotResolveClassPropertyFromClassInitializerIfItsDeclaredAfterIt() = codegen(
    """
      class MyClass {
        init {
          context<Foo>()
        }
        @Provide val foo: Foo = Foo()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo for parameter \$contextReceiver_0 of function com.ivianuu.injekt.context")
  }

  @Test fun testCanResolveClassFunctionFromClassInitializer() = codegen(
    """
      class MyClass {
        init {
          context<Foo>()
        }
        @Provide fun foo() = Foo()
      }
    """
  )

  @Test fun testCanResolveClassComputedPropertyFromClassInitializer() = codegen(
    """
      class MyClass {
        init {
          context<Foo>()
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
          context<Foo>()
        }
      }
    """
  )

  @Test fun testCannotResolveTopLevelPropertyFromWithinInitializer() = codegen(
    """
      @Provide private val foo: Foo = context<Foo>()
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo for parameter \$contextReceiver_0 of function com.ivianuu.injekt.context")
  }

  @Test fun testCanResolveTopLevelPropertyFromOtherPropertyInitializerIfItsDeclaredBeforeIt() = codegen(
    """
      @Provide val foo: Foo = Foo()
      @Provide val bar: Bar = Bar(context())
    """
  )

  @Test fun testCanResolveTopLevelPropertyFromOtherPropertyDelegateInitializerIfItsDeclaredBeforeIt() = codegen(
    """
      @Provide val foo: Foo = Foo()
      @Provide val bar: Bar by lazy(context<Foo>()) { Bar(foo) }
    """
  )

  @Test fun testCannotResolveTopLevelPropertyFromOtherPropertyInitializerIfItsDeclaredAfterIt() = codegen(
    """
      @Provide val bar: Bar = Bar(context())
      @Provide val foo: Foo = Foo()
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo for parameter \$contextReceiver_0 of function com.ivianuu.injekt.context")
  }

  @Test fun testCanResolveTopLevelPropertyFromOtherPropertyInitializerLambdaIfItsDeclaredAfterIt() = codegen(
    """
      @Provide val bar: Bar = run { Bar(context()) }
      @Provide val foo: Foo = Foo()
    """
  )

  @Test fun testCanResolveTopLevelPropertyFromOtherPropertyInitializerInDifferentFile() = codegen(
    """
      @Provide val bar: Bar = Bar(context())
    """,
    """
      @Provide val foo: Foo = Foo()
    """
  )

  @Test fun testCannotResolveTopLevelPropertyFromOtherPropertyDelegateInitializerIfItsDeclaredAfterIt() = codegen(
    """
      @Provide val bar: Bar by lazy(context<Foo>()) { Bar(foo) }
      @Provide val foo: Foo = Foo()
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo for parameter \$contextReceiver_0 of function com.ivianuu.injekt.context")
  }

  @Test fun testCanResolveTopLevelFunctionFromTopLevelPropertyInitializer() = codegen(
    """
      @Provide val bar: Bar = Bar(context())
      @Provide fun foo() = Foo()
    """
  )

  @Test fun testCanResolveTopLevelFunctionFromTopLevelPropertyDelegateInitializer() = codegen(
    """
      @Provide val bar: Bar by lazy(context<Foo>()) { Bar(context()) }
      @Provide fun foo() = Foo()
    """
  )

  @Test fun testNestedClassCannotResolveOuterClassInjectables() = codegen(
    """
      class Outer(@Provide val foo: Foo = Foo()) {
        class Inner {
          fun foo() = context<Foo>()
        }
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo for parameter \$contextReceiver_0 of function com.ivianuu.injekt.context")
  }

  @Test fun testInnerClassCanResolveOuterClassInjectables() = codegen(
    """
      class Outer(@property:Provide val foo: Foo = Foo()) {
        inner class Inner {
          fun foo() = context<Foo>()
        }
      }
    """
  )

  @Test fun testNestedClassCanResolveOuterObjectInjectables() = codegen(
    """
      object Outer {
        @Provide private val foo: Foo = Foo()

        class Inner {
          fun foo() = context<Foo>()
        }
      }
    """
  )

  @Test fun testNestedObjectCannotResolveOuterClassInjectables() = codegen(
    """
      class Outer {
        @Provide private val foo: Foo = Foo()

        object Inner {
          fun foo() = context<Foo>()
        }
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.integrationtests.Foo for parameter \$contextReceiver_0 of function com.ivianuu.injekt.context")
  }

  @Test fun testTaggedObjectInjectableIsNotApplicableToUntaggedType() = singleAndMultiCodegen(
    """
      interface Logger

      @Provide @Tag1 object NoopLogger : Logger
      
      context(Logger) fun log() {
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
      @Provide class HaloState(@property:Provide val foo: Foo) {
        val pointerInput = object : Any() {
          override fun equals(other: Any?): Boolean {
            context<Foo>()
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
        @Inject scope: String,
        @Inject Nkey: SourceKey
      ): State<T> = TODO()

      fun invoke(@Inject scope: String) {
        val scope by produceState<Int>()
      }
    """
  )

  @Test fun testCanResolveExtensionReceiverOfFunction() = codegen(
    """
      fun @receiver:Provide Foo.foo() = context<Foo>()
    """
  )

  @Test fun testCanResolveExtensionReceiverOfProperty() = codegen(
    """
      val @receiver:Provide Foo.foo get() = context<Foo>()
    """
  )

  @Test fun testCanResolveContextReceiverOfFunction() = codegen(
    """
      context((@Provide Foo)) fun foo() = context<Foo>()
    """
  )

  @Test fun testCanResolveContextReceiverOfProperty() = codegen(
    """
      context((@Provide Foo)) val foo get() = context<Foo>()
    """
  )

  @Test fun testCannotResolveAInjectableInBlockWhichIsDeclaredAfterIt() = codegen(
    """
      fun invoke(foo: Foo): Pair<Foo?, Foo?> {
        val a = injectOrNull<Foo>()
        @Provide val provided = foo
        val b = injectOrNull<Foo>()
        return a to b
      }
    """
  ) {
    val foo = Foo()
    val result = invokeSingleFile<Pair<Foo?, Foo?>>(foo)
    result.first.shouldBeNull()
    result.second shouldBeSameInstanceAs foo
  }
}
