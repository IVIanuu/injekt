/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.singleAndMultiCodegen
import io.kotest.matchers.shouldBe
import org.junit.Test

class PersistenceTest {
  @Test fun testModuleDispatchReceiverTypeInference() = singleAndMultiCodegen(
    """
      class MyModule<T : S, S> {
        @Provide fun provide(value: S): T = value as T
      }
  
      @Provide val module = MyModule<String, CharSequence>()
  
      @Provide val value: CharSequence = "42"
    """,
    """
      fun invoke() = context<String>() 
    """
  ) {
    invokeSingleFile() shouldBe "42"
  }

  @Test fun testNonProvidersecondaryConstructorWithProviderParameters() = singleAndMultiCodegen(
    """
      class MyClass {
        constructor(@Context unit: Unit)
      }
    """,
    """
      fun invoke(@Provide unit: Unit) = MyClass()
    """
  )

  @Test fun testNonProviderClassWithProviderMembers() = singleAndMultiCodegen(
    """ 
      abstract class MyModule<T : S, S> {
        @Provide fun func(t: T): S = t
      }
      class MyModuleImpl<T> : MyModule<@Tag1 T, T>()
    """,
    """
      @Provide val myFooModule = MyModuleImpl<Foo>()
      @Provide val foo: @Tag1 Foo = Foo()
      fun invoke() = context<Foo>()
        """
  )

  @Test fun testSupportsLargeFunction() = singleAndMultiCodegen(
    """
      @Tag annotation class MyAlias<T>
      fun <T> largeFunc(${
        (1..150).map { "@Context p$it: @MyAlias<T> Any?" }.joinToString("\n,")
      }): String = ""
    """,
    """
      fun invoke() {
        with("" as @MyAlias<String> String) { largeFunc<String>() }
      }
    """
  ) {
    invokeSingleFile()
  }
}
