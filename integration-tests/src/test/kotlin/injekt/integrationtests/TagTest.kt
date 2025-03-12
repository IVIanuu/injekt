
/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalCompilerApi::class)

package injekt.integrationtests

import io.kotest.matchers.types.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.junit.*

class TagTest {
  @Test fun testDistinctTag() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide val taggedFoo: @Tag1 Foo = Foo()
    """,
    """
      fun invoke(): Pair<Foo, Foo> {
        return create<Foo>() to create<@Tag1 Foo>()
      } 
    """
  ) {
    val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
    foo1 shouldNotBeSameInstanceAs foo2
  }

  @Test fun testTaggedClass() = singleAndMultiCodegen(
    """ 
      @Provide @Tag1 class Baz
    """,
    """
      fun invoke() = create<@Tag1 Baz>()
    """
  )

  @Test fun testTaggedPrimaryConstructor() = singleAndMultiCodegen(
    """ 
      class Baz @Provide @Tag1 constructor()
    """,
    """
      fun invoke() = create<@Tag1 Baz>()
    """
  )

  @Test fun testTaggedSecondaryConstructor() = singleAndMultiCodegen(
    """ 
      class Baz {
        @Provide @Tag1 constructor()
      }
    """,
    """
      fun invoke() = create<@Tag1 Baz>()
    """
  )

  @Test fun testTagWithValueParameters() = codegen(
    """ 
      @Tag annotation class MyTag(val value: String)
    """
  ) {
    compilationShouldHaveFailed("tag cannot have value parameters")
  }

  @Test fun testTagWithTypeParameters() = singleAndMultiCodegen(
    """
      @Tag @Target(AnnotationTarget.TYPE) annotation class MyTag<T>
      @Provide val taggedFoo: @MyTag<String> Foo = Foo()
    """,
    """
      fun invoke() = create<@MyTag<String> Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testTagTypeAliasPattern() = singleAndMultiCodegen(
    """
      @Tag @Target(AnnotationTarget.TYPE) annotation class TaggedFooTag
      typealias TaggedFoo = @TaggedFooTag Foo
      @Provide val taggedFoo: TaggedFoo = Foo()
    """,
    """
      fun invoke() = create<TaggedFoo>()
    """
  )

  @Test fun testGenericTagTypeAliasPattern() = singleAndMultiCodegen(
    """
      typealias ComponentScope<N> = @ComponentScopeTag<N> String

      @Tag @Target(AnnotationTarget.TYPE) annotation class ComponentScopeTag<N> {
        @Provide companion object {
          @Provide fun <N> scope(): ComponentScope<N> = ""
        }
      }
    """,
    """
      fun invoke() = create<ComponentScope<Foo>>()
    """
  )

  @Test fun testTaggedTypeAliasWhichAlsoHasTagsInItsExpandedType() = singleAndMultiCodegen(
    """
      @Tag @Target(AnnotationTarget.TYPE) annotation class TaggedFooTag<T>
      typealias TaggedT<T> = @TaggedFooTag<T> Foo
      @Provide fun <T> taggedFoo(): @Tag1 TaggedT<String> = Foo()
    """,
    """
      fun invoke() = create<@Tag1 @TaggedFooTag<String> Foo>()
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testComplexTagTypeAliasPatternWithAddOns() = singleAndMultiCodegen(
    """
      interface Screen<R>

      @Stable fun interface Ui<S : Screen<*>> {
        @Composable fun Content()
      }
      
      @Tag @Target(AnnotationTarget.TYPE) annotation class UiTag<S : Screen<*>> {
        @Provide companion object {
          @Provide inline fun <@AddOn T : Uii<S>, S : Screen<*>> uiiToUi(
            crossinline uii: @Composable () -> T
          ): Ui<S> = Ui { uii() }
        }
      }
      
      @Provide inline fun <reified T : Any> kclass(): KClass<T> = T::class

      @Provide fun <@AddOn T : Ui<S>, S : Screen<*>> rootNavGraphUiFactory(
        screenClass: KClass<S>,
        uiFactory: (S) -> T
      ): Pair<KClass<Screen<*>>, UiFactory<Screen<*>>> =
        (screenClass to uiFactory) as Pair<KClass<Screen<*>>, UiFactory<Screen<*>>>
      
      typealias Uii<S> = @UiTag<S> Unit

      typealias UiFactory<S> = (S) -> Ui<S>
    """,
    """
      class ActionsScreen : Screen<Unit>
      
      @Provide @Composable fun ActionsUi(): Uii<ActionsScreen> {
      }

      fun invoke() {
        create<List<Pair<KClass<Screen<*>>, UiFactory<Screen<*>>>>>()
      }
    """,
    config = { withCompose() }
  ) {
    invokeSingleFile()
  }

  @Test fun testDoesNotNeedToDeclareTagAnnotationTargets() = singleAndMultiCodegen(
    """
      @Tag @Target(AnnotationTarget.TYPE) annotation class MyCoolTag
    """,
    """
      @Provide fun provideSomething(): @MyCoolTag Foo = Foo()
      fun invoke() = create<@MyCoolTag Foo>()
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testTaggedClassReferencesItself() = singleAndMultiCodegen(
    """
      @Provide @Scoped<MyClass> class MyClass
    """,
    """
      @Provide val scope = Scope<MyClass>()
      fun invoke() = create<MyClass>()
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testTypeAliasTag() = singleAndMultiCodegen(
    """
      @Tag typealias MyTag<T> = Foo
      @Provide val taggedFoo: MyTag<String> = Foo()
      @Provide val untaggedFoo = Foo()
    """,
    """
      fun invoke() = create<MyTag<String>>() 
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testTagNullability() = singleAndMultiCodegen(
    """
      @Provide val foo: @Scoped<Unit> String? = null
    """,
    """
      fun invoke() {
        @Provide val scope = Scope<Unit>()
        create<String?>()
      }
    """
  ) {
    invokeSingleFile()
  }
}
