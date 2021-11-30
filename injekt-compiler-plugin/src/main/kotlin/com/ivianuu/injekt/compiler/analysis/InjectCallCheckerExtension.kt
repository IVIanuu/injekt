/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.shaded_injekt.Inject
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension

class InjectCallCheckerExtension(
  private val withDeclarationGenerator: Boolean,
  @Inject private val injektFqNames: InjektFqNames
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
