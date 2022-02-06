/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.com.intellij.openapi.project.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.extensions.*

class InjectCallCheckerExtension(
  private val withDeclarationGenerator: Boolean,
  private val injektFqNames: InjektFqNames
) : AnalysisHandlerExtension {
  private var completionCount = 0

  override fun analysisCompleted(
    project: Project,
    module: ModuleDescriptor,
    bindingTrace: BindingTrace,
    files: Collection<KtFile>
  ): AnalysisResult? {
    if (completionCount < 1 && withDeclarationGenerator)
      return null.also { completionCount++ }
    if (completionCount > 0 && !withDeclarationGenerator)
      return null.also { completionCount++ }

    val checker = InjectCallChecker(Context(module, injektFqNames, bindingTrace))
    files.forEach { it.accept(checker) }

    return null
  }
}
