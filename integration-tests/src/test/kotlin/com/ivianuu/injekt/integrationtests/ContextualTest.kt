/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalCompilerApi::class)

package com.ivianuu.injekt.integrationtests

import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test

class ContextualTest {
  @Test fun testSimple() = codegen(
    """
      @Contextual fun doRequest() {
        run { resolve<String>() }
      }

      @Contextual fun b() {
        doRequest()
      }

      fun invoke() {
        b()
      }
    """
  ) {

  }

  @Test fun testRecursive() = codegen(
    """
      @Contextual fun a() {
        a()
      }

      @Contextual fun b() {
        b()
      }
    """
  ) {

  }
}
