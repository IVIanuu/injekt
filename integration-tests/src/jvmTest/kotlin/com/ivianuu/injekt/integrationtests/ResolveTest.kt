/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Bar
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokableSource
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.singleAndMultiCodegen
import com.ivianuu.injekt.test.source
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldBeTypeOf
import org.jetbrains.kotlin.name.FqName
import org.junit.Test

class ResolveTest {
  @Test fun testResolvesExternalProviderInSamePackage() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = context<Foo>()
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesExternalProviderInDifferentPackage() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
          """,
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("providers.*")
            fun invoke() = context<Foo>()
          """
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesInternalProviderFromDifferentPackageWithAllUnderImport() = codegen(
    listOf(
      source(
        """
          @Provide val foo = Foo()
        """,
        packageFqName = FqName("providers")
      ),
      invokableSource(
        """
          @Providers("providers.*")
          fun invoke() = context<Foo>()
        """
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesProviderInSamePackageAndSameFile() = codegen(
    """
      @Provide val foo = Foo()
      fun invoke() = context<Foo>()
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesClassCompanionProviderFromWithinTheClass() = singleAndMultiCodegen(
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

  @Test fun testResolvesClassCompanionProviderFromOuterClass() = singleAndMultiCodegen(
    """
      class MyClass {
        companion object {
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

  @Test fun testResolvesClassCompanionClassProviderFromOuterClass() = singleAndMultiCodegen(
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

  @Test fun testResolvesClassCompanionProviderFromClassConstructor() = singleAndMultiCodegen(
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

  @Test fun testResolvesClassConstructorPropertyProvider() = singleAndMultiCodegen(
    """
      class MyClass(@Provide val foo: Foo = Foo()) {
        fun resolve() = context<Foo>()
      }
    """,
    """
      fun invoke() = MyClass().resolve()
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesClassProvider() = singleAndMultiCodegen(
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

  @Test fun testDerivedProvider() = singleAndMultiCodegen(
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

  @Test fun testCanResolveSubTypeOfProvider() = singleAndMultiCodegen(
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

  @Test fun testUnresolvedProvider() = codegen(
    """
      fun invoke() {
        context<String>()
      }
    """
  ) {
    compilationShouldHaveFailed("no provider found of type kotlin.String")
  }

  @Test fun testNestedUnresolvedProvider() = singleAndMultiCodegen(
    """
      @Provide fun bar(foo: Foo) = Bar(foo)
    """,
    """
      fun invoke() = context<Bar>() 
    """
  ) {
    compilationShouldHaveFailed("\nno provider found of type com.ivianuu.injekt.test.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.bar")
  }

  @Test fun testGenericProvider() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide fun <T> providerList(value: T): List<T> = listOf(value)
    """,
    """
      fun invoke() = context<List<Foo>>() 
    """
  ) {
    val (foo) = invokeSingleFile<List<Any>>()
    foo.shouldBeTypeOf<Foo>()
  }

  @Test fun testFunctionInvocationWithProviders() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      fun usesFoo(@Context foo: Foo) {
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

  @Test fun testLocalFunctionInvocationWithProviders() = codegen(
    """
      @Provide val foo = Foo()
      fun invoke() {
        fun usesFoo(@Context foo: Foo) {
        }                    
        usesFoo()
      }
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testConstructorInvocationWithProviders() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      class UsesFoo(@Context foo: Foo)
    """,
    """
      fun invoke() {
        UsesFoo()
      } 
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testPrimaryConstructorProviderWithReceiver() = singleAndMultiCodegen(
    """
      class UsesFoo(@Provide val foo: Foo)
    """,
    """
      fun invoke(foo: Foo) = with(UsesFoo(foo)) {
        context<Foo>()
      }
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testLocalConstructorInvocationWithProviders() = codegen(
    """
      @Provide val foo = Foo()
      fun invoke() {
        class UsesFoo(@Context foo: Foo)
        UsesFoo()
      }
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testCanResolveProviderOfProviderThisFunction() = codegen(
    """
      class Dep(@Provide val foo: Foo)
      fun invoke(foo: Foo) = with(Dep(foo)) { context<Foo>() }
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
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

  @Test fun testCannotResolveObjectWithoutProvider() = singleAndMultiCodegen(
    """
      object MyObject
    """,
    """
      fun invoke() = context<MyObject>() 
    """
  ) {
    compilationShouldHaveFailed("no provider")
  }

  @Test fun testCannotResolveObjectBySubTypeWithoutProvider() = codegen(
    """
      interface Json {
        companion object : Json
      }
    """,
    """
      fun invoke() = context<Json>()
    """
  ) {
    compilationShouldHaveFailed("no provider")
  }

  @Test fun testCanResolveObjectWithProvider() = singleAndMultiCodegen(
    """
      @Provide object MyObject
    """,
    """
      fun invoke() = context<MyObject>() 
    """
  )

  @Test fun testCannotResolveExternalInternalProvider() = multiCodegen(
    """
      @Provide internal val foo = Foo()
    """,
    """
     fun invoke() = context<Foo>()
    """
  ) {
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo")
  }

  @Test fun testCannotResolvePrivateClassProviderFromOuterScope() = singleAndMultiCodegen(
    """
      @Provide class FooHolder {
        @Provide private val foo = Foo()
      }
    """,
    """
      fun invoke() = context<Foo>() 
    """
  ) {
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo")
  }

  @Test fun testCanResolvePrivateClassProviderFromInnerScope() = codegen(
    """
      @Provide class FooHolder {
        @Provide private val foo = Foo()
        fun invoke() = context<Foo>()
      }
    """
  )

  @Test fun testCannotResolveProtectedProviderFromOuterScope() = singleAndMultiCodegen(
    """
      @Provide open class FooHolder {
        @Provide protected val foo = Foo()
      }
    """,
    """
      fun invoke() = context<Foo>() 
    """
  ) {
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo")
  }

  @Test fun testCanResolveProtectedProviderFromSameClass() = codegen(
    """
      @Provide open class FooHolder {
        @Provide protected val foo = Foo()
        fun invoke() = context<Foo>()
      }
    """
  )

  @Test fun testCanResolveProtectedProviderFromSubClass() = singleAndMultiCodegen(
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

  @Test fun testCanResolvePrivateTopLevelProviderInSameFile() = codegen(
    """
      @Provide private val foo = Foo()
      fun invoke() = context<Foo>()
    """
  )

  @Test fun testCanResolvePrivateTopLevelProviderInSameFileMultiFile() = codegen(
    """
      // triggers creation of package scope
      fun invoke(@Provide unit: Unit) {
        context<Unit>()
      }
    """,
    """
      @Provide private val foo = Foo()
      fun invoke() = context<Foo>()
    """
  )

  @Test fun testCannotResolvePrivateTopLevelProviderInDifferentFile() = codegen(
    """
      @Provide private val foo = Foo()
    """,
    """
      fun invoke() = context<Foo>()
    """
  ) {
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo")
  }

  @Test fun testCannotResolvePropertyWithTheSameNameAsAnProviderPrimaryConstructorParameter() =
    singleAndMultiCodegen(
      """
        @Provide class MyClass(foo: Foo) {
          val foo = foo
        }
      """,
      """
        fun invoke() = context<Foo>() 
      """
    ) {
      compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.context")
    }

  @Test fun testAnonymousObjectCanResolveProvidersOfOuterClass() = codegen(
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

  @Test fun testCanResolveExplicitMarkedProviderConstructorParameterFromOutsideTheClass() =
    singleAndMultiCodegen(
      """
        class MyClass(@Provide val foo: Foo)
      """,
      """
        fun invoke(@Provide myClass: MyClass) = context<Foo>() 
      """
    )

  @Test fun testCannotResolveImplicitProviderConstructorParameterFromOutsideTheClass() =
    singleAndMultiCodegen(
      """
        @Provide class MyClass(val foo: Foo)
      """,
      """
        fun invoke() = context<Foo>() 
      """
    ) {
      compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.context")
    }

  @Test fun testCanResolveImplicitProviderConstructorParameterFromInsideTheClass() = codegen(
    """
      @Provide class MyClass(private val foo: Foo) {
        fun invoke() = context<Foo>()
      }
    """
  )

  @Test fun testResolvesProviderWithTypeParameterInScope() = singleAndMultiCodegen(
    """
      @Provide fun <T> list(): List<T> = emptyList()
    """,
    """
      fun <T> invoke() {
        context<List<T>>()
      } 
    """
  )

  @Test fun testCannotUseNonReifiedTypeParameterForReifiedProvider() = singleAndMultiCodegen(
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
      "type parameter T of provider com.ivianuu.injekt.integrationtests.set() of type kotlin.collections.Set<com.ivianuu.injekt.integrationtests.invoke.T> for parameter x of function com.ivianuu.injekt.context is reified but type argument com.ivianuu.injekt.integrationtests.invoke.T is not reified"
    )
  }

  @Test fun testSafeCallWithcontext() = singleAndMultiCodegen(
      """
        @Provide val foo = Foo()

        fun String.myFunc(@Context foo: Foo) {
        }
      """,
      """
        fun invoke() = (null as? String)?.myFunc()
      """
    )

  // todo @Test
  fun testSmartcastWithcontext() = codegen(
    """
      class MyType {
        fun <T> doSomething(@Context key: TypeKey<T>) {
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
        fun func(@Context foo: Foo) {
        }
      }

      class MySubClass : MySuperClass()
    """,
    """
      fun invoke(@Context foo: Foo) = MySubClass().func()
    """
  )

  @Test fun testInvocationOfFunctionDeclaredInSuperClassWithGenericInjectParameters() = singleAndMultiCodegen(
    """
      open class MySuperClass<T> {
        fun <S : T> func(@Context s: S) {
        }
      }

      class MySubClass<T> : MySuperClass<T>()
    """,
    """
      fun invoke(@Context foo: Foo) = MySubClass<Any>().func<Foo>()
    """
  )

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
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.context")
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
      fun Foo.invoke(bar: Bar = Bar(context())) {
      }
    """
  )

  @Test fun testCanResolveContextReceiverInDefaultValueOfParameter() = codegen(
    """
      context(Foo) fun invoke(bar: Bar = Bar(context())) {
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
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.context")
  }

  @Test fun testCanResolvePrimaryConstructorProviderInSuperTypeExpression() = codegen(
    """
      interface FooHolder {
        val foo: Foo
      }
      fun FooHolder(@Context foo: Foo) = object : FooHolder {
        override val foo = foo
      }
      class MyClass(@Provide foo: Foo) : FooHolder by FooHolder()
    """
  )

  @Test fun testCannotResolveSecondaryConstructorProviderInSuperTypeExpression() = codegen(
    """
      interface FooHolder {
        val foo: Foo
      }
      fun FooHolder(@Context foo: Foo) = object : FooHolder {
        override val foo = foo
      }
      class MyClass() : FooHolder by FooHolder() {
        constructor(@Provide foo: Foo) : this()
      }
    """
  ) {
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.FooHolder")
  }

  @Test fun testCannotResolveClassProvideDeclarationInSuperTypeExpression() = codegen(
    """
      abstract class MyAbstractClass(@Context foo: Foo)
      class MyClass : MyAbstractClass() {
        @Provide val foo = Foo()
      }
    """
  ) {
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.MyAbstractClass")
  }

  @Test fun testCannotResolveThisInSuperTypeDelegation() = codegen(
    """
      interface Scope

      class MyImpl : Scope by context()
    """
  ) {
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.integrationtests.Scope for parameter x of function com.ivianuu.injekt.context")
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
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.integrationtests.Scope for parameter x of function com.ivianuu.injekt.context")
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
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.context")
  }

  @Test fun testCanResolvePrimaryConstructorProviderInInit() = codegen(
    """
      class MyClass(@Provide foo: Foo) {
        init {
          context<Foo>()
        }
      }
    """
  )

  @Test fun testCanResolvePrimaryConstructorProviderInPropertyInitializer() = codegen(
    """
      class MyClass(@Provide _foo: Foo) {
        val foo: Foo = context()
      }
    """
  )

  @Test fun testCanResolvePrimaryConstructorProviderInPropertyInitializerLambda() = codegen(
    """
      class MyClass(@Provide _foo: Foo) {
        val foo: Foo = run { context() }
      }
    """
  )

  @Test fun testCanResolvePrimaryConstructorProviderInPropertyDelegateInitializer() = codegen(
    """
      class MyClass(@Provide _foo: Foo) {
        val foo: Foo by lazy(context<Foo>()) { Foo() }
      }
    """
  )

  @Test fun testCanResolvePrimaryConstructorProviderInPropertyDelegateInitializerLambda() = codegen(
    """
      class MyClass(@Provide _foo: Foo) {
        val foo: Foo by lazy { context<Foo>() }
      }
    """
  )

  @Test fun testCannotResolvePrimaryConstructorParameterInFunctionWithMultipleNestedBlocks() = codegen(
    """
      class MyClass(@Provide val _foo: Foo) {
        fun foo(): Foo = runBlocking {
          run {
            context()
          }
        }
      }
    """
  )

  @Test fun testCannotResolvePrimaryConstructorProviderInPropertyGetter() = codegen(
    """
      class MyClass(@Provide _foo: Foo) {
        val foo: Foo get() = context()
      }
    """
  ) {
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.context")
  }

  @Test fun testCannotResolveLocalVariableFromWithinInitializer() = codegen(
    """
      fun invoke() {
        @Provide val foo: Foo = context()
      }
    """
  ) {
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.context")
  }

  @Test fun testCannotResolveLocalVariableFromWithinDelegateInitializer() = codegen(
    """
      fun invoke() {
        @Provide val foo: Foo by lazy(context<Foo>()) { Foo() }
      }
    """
  ) {
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.context")
  }

  @Test fun testCannotResolveClassPropertyFromWithinInitializer() = codegen(
    """
      class MyClass {
        @Provide private val foo: Foo = context<Foo>()
      }
    """
  ) {
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.context")
  }

  @Test fun testCannotResolveClassPropertyFromWithinDelegateInitializer() = codegen(
    """
      class MyClass {
        @Provide private val foo: Foo by lazy(context<Foo>()) { Foo() }
      }
    """
  ) {
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.context")
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
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.context")
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
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.context")
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
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.context")
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
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.context")
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
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.context")
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
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.context")
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

  @Test fun testNestedClassCannotResolveOuterClassProviders() = codegen(
    """
      class Outer(@Provide val foo: Foo = Foo()) {
        class Inner {
          fun foo() = context<Foo>()
        }
      }
    """
  ) {
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.context")
  }

  @Test fun testInnerClassCanResolveOuterClassProviders() = codegen(
    """
      class Outer(@Provide val foo: Foo = Foo()) {
        inner class Inner {
          fun foo() = context<Foo>()
        }
      }
    """
  )

  @Test fun testNestedClassCanResolveOuterObjectProviders() = codegen(
    """
      object Outer {
        @Provide private val foo: Foo = Foo()

        class Inner {
          fun foo() = context<Foo>()
        }
      }
    """
  )

  @Test fun testNestedObjectCannotResolveOuterClassProviders() = codegen(
    """
      class Outer {
        @Provide private val foo: Foo = Foo()

        object Inner {
          fun foo() = context<Foo>()
        }
      }
    """
  ) {
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.context")
  }

  @Test fun testTaggedObjectProviderIsNotApplicableToUntaggedType() = singleAndMultiCodegen(
    """
      interface Logger

      @Provide @Tag1 object NoopLogger : Logger
      
      fun log(@Context logger: Logger) {
      }
    """,
    """
      fun invoke() = log()
    """
  ) {
    compilationShouldHaveFailed(
      "no provider found of type com.ivianuu.injekt.integrationtests.Logger for parameter logger of function com.ivianuu.injekt.integrationtests.log"
    )
  }

  @Test fun testCanResolveOuterClassProviderFromFunctionInsideAnonymousObject() = codegen(
    """
      @Provide class HaloState(val foo: Foo) {
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
        @Context scope: String,
        @Context Nkey: SourceKey
      ): State<T> = TODO()

      fun invoke(@Context scope: String) {
        val scope by produceState<Int>()
      }
    """
  )

  @Test fun testCanResolveExtensionReceiverOfFunction() = codegen(
    """
      fun Foo.foo() = context<Foo>()
    """
  )

  @Test fun testCanResolveContextReceiverOfFunction() = codegen(
    """
      context(Foo) fun foo() = context<Foo>()
    """
  )

  @Test fun testCanResolveExtensionReceiverOfProperty() = codegen(
    """
      val Foo.foo get() = context<Foo>()
    """
  )

  @Test fun testMeh() = singleAndMultiCodegen(
    """
      class ResourceProvider {
        context(AppContext, String) fun loadResource(id: Int, prefix: String): String = ""
        fun load(id: Int): String = ""
      }

      typealias AppContext = Foo
    """,
    """
      @Provide val foo: AppContext = Foo()

      context(ResourceProvider, String)
      fun loadMessage(messageRes: Int) {
        loadResource(0, "")
      }

      abstract class AbstractRepo(string: String)

      context(ResourceProvider) class RepoImpl : AbstractRepo(load(0)) {
        fun func() {
          load(0)
        }
      }
    """
  )

  @Test fun testCanResolveContextReceiverOfProperty() = codegen(
    """
      context(Foo) val foo get() = context<Foo>()
    """
  )

  @Test fun testCanResolveContextReceiverOfClass() = codegen(
    """
      context(Foo) class Dep {
        val foo get() = context<Foo>()
      }
    """
  )
}
