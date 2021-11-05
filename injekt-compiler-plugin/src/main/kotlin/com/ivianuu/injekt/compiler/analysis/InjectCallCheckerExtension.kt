/*
 * Copyright 2021 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.shaded_injekt.Inject
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension

class InjectCallCheckerExtension(
  @Inject private val injektFqNames: InjektFqNames
) : AnalysisHandlerExtension {
  private lateinit var lazyTopDownAnalyzer: LazyTopDownAnalyzer

  private var checked = false

  override fun doAnalysis(
    project: Project,
    module: ModuleDescriptor,
    projectContext: ProjectContext,
    files: Collection<KtFile>,
    bindingTrace: BindingTrace,
    componentProvider: ComponentProvider
  ): AnalysisResult? {
    lazyTopDownAnalyzer = componentProvider.get()
    return null
  }

  override fun analysisCompleted(
    project: Project,
    module: ModuleDescriptor,
    bindingTrace: BindingTrace,
    files: Collection<KtFile>
  ): AnalysisResult? {
    if (checked) return null
    checked = true

    try {
      lazyTopDownAnalyzer.analyzeDeclarations(
        TopDownAnalysisMode.TopLevelDeclarations,
        files
      )
    } catch (e: Throwable) {
    }
    try {
      lazyTopDownAnalyzer.analyzeDeclarations(
        TopDownAnalysisMode.LocalDeclarations,
        files
      )
    } catch (e: Throwable) {
    }

    val checker = InjectCallChecker(Context(module, injektFqNames, bindingTrace))
    files.forEach { it.accept(checker) }

    return null
  }
}
