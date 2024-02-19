/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import io.kotest.matchers.nulls.*
import io.kotest.matchers.types.*
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
        @Provide companion object {
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
    invokeSingleFile().shouldBeTypeOf<Bar>()
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
    compilationShouldHaveFailed("no injectable")
  }

  @Test fun testNestedUnresolvedInjectable() = singleAndMultiCodegen(
    """
      @Provide fun bar(foo: Foo) = Bar(foo)
    """,
    """
      fun invoke() = inject<Bar>() 
    """
  ) {
    compilationShouldHaveFailed("no injectable")
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
    compilationShouldHaveFailed("no injectable")
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
    compilationShouldHaveFailed("no injectable")
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
    compilationShouldHaveFailed("no injectable")
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

  @Test fun testCannotResolvePrivateTopLevelInjectableInDifferentFile() = codegen(
    """
      @Provide private val foo = Foo()
    """,
    """
      fun invoke() = inject<Foo>()
    """
  ) {
    compilationShouldHaveFailed("no injectable")
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
      compilationShouldHaveFailed("no injectable")
    }

  @Test fun testCanResolveImplicitInjectableConstructorParameterFromInsideTheClass() = codegen(
    """
      @Provide class MyClass(@property:Provide private val foo: Foo) {
        fun invoke() = inject<Foo>()
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

  // todo @Test
  fun testSmartcastWithInject() = codegen(
    """
      class MyType {
        fun <T> doSomething(@Inject key: TypeKey<T>) {
        }
      }
      fun invoke(myType: MyType?) {
        if (myType != null) {
          myType.doSomething<String>()
        }
      }
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
    compilationShouldHaveFailed("no injectable")
  }

  @Test fun testCanResolveDispatchReceiverInDefaultValueOfParameter() = codegen(
    """
      class Dep {
        fun invoke(dep: Dep = inject()) {
        }
      }
    """
  )

  @Test fun testCanResolveExtensionReceiverInDefaultValueOfParameter() = codegen(
    """
      fun @receiver:Provide Foo.invoke(bar: Bar = Bar(inject())) {
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
    compilationShouldHaveFailed("no injectable")
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
    compilationShouldHaveFailed("no injectable")
  }

  @Test fun testCannotResolveClassProvideDeclarationInSuperTypeExpression() = codegen(
    """
      abstract class MyAbstractClass(@Inject foo: Foo)
      class MyClass : MyAbstractClass() {
        @Provide val foo = Foo()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable")
  }

  @Test fun testCannotResolveThisInSuperTypeDelegation() = codegen(
    """
      interface Scope

      class MyImpl : Scope by inject()
    """
  ) {
    compilationShouldHaveFailed("no injectable")
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
    compilationShouldHaveFailed("no injectable")
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
    compilationShouldHaveFailed("no injectable")
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
    compilationShouldHaveFailed("no injectable")
  }

  @Test fun testCannotResolveLocalVariableFromWithinInitializer() = codegen(
    """
      fun invoke() {
        @Provide val foo: Foo = inject()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable")
  }

  @Test fun testCannotResolveLocalVariableFromWithinDelegateInitializer() = codegen(
    """
      fun invoke() {
        @Provide val foo: Foo by lazy(inject<Foo>()) { Foo() }
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable")
  }

  @Test fun testCannotResolveClassPropertyFromWithinInitializer() = codegen(
    """
      class MyClass {
        @Provide private val foo: Foo = inject<Foo>()
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable")
  }

  @Test fun testCannotResolveClassPropertyFromWithinDelegateInitializer() = codegen(
    """
      class MyClass {
        @Provide private val foo: Foo by lazy(inject<Foo>()) { Foo() }
      }
    """
  ) {
    compilationShouldHaveFailed("no injectable")
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
    compilationShouldHaveFailed("no injectable")
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
    compilationShouldHaveFailed("no injectable")
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
    compilationShouldHaveFailed("no injectable")
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
    compilationShouldHaveFailed("no injectable")
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
    compilationShouldHaveFailed("no injectable")
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
    compilationShouldHaveFailed("no injectable")
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
    compilationShouldHaveFailed("no injectable")
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
    compilationShouldHaveFailed("no injectable")
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
    compilationShouldHaveFailed("no injectable")
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

  @Test fun testCanResolveExtensionReceiverOfFunction() = codegen(
    """
      fun @receiver:Provide Foo.foo() = inject<Foo>()
    """
  )

  @Test fun testCanResolveExtensionReceiverOfProperty() = codegen(
    """
      val @receiver:Provide Foo.foo get() = inject<Foo>()
    """
  )

  @Test fun testCannotResolveAInjectableInBlockWhichIsDeclaredAfterIt() = codegen(
    """
      fun <T> injectOrNull(@Inject x: T? = null): T? = x
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

  @Test fun testCannotResolveUnconstrainedType() = codegen(
    """
      @Provide fun <T> everything(): T = error("")
    """,
    """
      fun invoke() = inject<Foo>()
    """
  ) {
    compilationShouldHaveFailed()
  }
}
