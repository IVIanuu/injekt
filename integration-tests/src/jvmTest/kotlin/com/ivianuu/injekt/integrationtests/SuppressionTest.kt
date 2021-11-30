/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.shouldNotContainMessage
import com.ivianuu.injekt.test.singleAndMultiCodegen
import org.junit.Test

class SuppressionTest {
  @Test fun testCanUseInfixWithInject() = singleAndMultiCodegen(
    """
      interface Combine<T> {
        fun plus(a: T, b: T): T
      }

      infix fun <T> T.combine(other: T, @Inject combine: Combine<T>): T = combine.plus(this, other)
      
      @Provide object StringCombine : Combine<String> {
        override fun plus(a: String, b: String) = a + b
      }
    """,
    """
      fun invoke() {
        "a" combine "b"
      } 
    """
  )

  @Test fun testCanUseOperatorWithInject() = singleAndMultiCodegen(
    """
      interface Combine<T> {
        fun plus(a: T, b: T): T
      }
  
      operator fun <T> T.plus(other: T, @Inject combine: Combine<T>): T = combine.plus(this, other)
  
      @JvmInline value class Key(val value: String)
  
      @Provide object KeyCombine : Combine<Key> {
          override fun plus(a: Key, b: Key) = Key(a.value + b.value)
      }
    """,
    """
      fun invoke() {
        Key("a") + Key("b")
      } 
    """
  )

  @Test fun testCanUseUnaryPlusWithInject() = singleAndMultiCodegen(
    """
      operator fun <T> T.unaryPlus(@Inject builder: StringBuilder) {
        builder.append(toString())
      }
    """,
    """
      fun invoke() = buildString {
        +42
      }
    """
  )

  @Test fun testDoesNotShowUnusedTypeParameterIfUsedInAnnotation() = codegen(
    """
      typealias ComponentScope<C> = @ComponentScopeTag<C> String

      @Tag annotation class ComponentScopeTag<C : Component>
    """
  ) {
    shouldNotContainMessage("Type alias parameter C is not used in the expanded type String and does not affect type checking")
  }

  @Test fun testDoesNotWarnInlineOnProvideDeclaration() = codegen(
    """
      @Provide inline fun func() {
      }
    """
  ) {
    shouldNotContainMessage("Expected performance impact from inlining is insignificant. Inlining works best for functions with parameters of functional types")
  }

  @Test fun testDoesNotWarnInlineWithInjectParams() = codegen(
    """
      inline fun func(@Inject x: Unit) {
      }
    """
  ) {
    shouldNotContainMessage("Expected performance impact from inlining is insignificant. Inlining works best for functions with parameters of functional types")
  }
}
