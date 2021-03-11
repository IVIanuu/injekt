/*
 * Copyright 2020 Manuel Wrage
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

import com.ivianuu.injekt.compiler.CacheDir
import com.ivianuu.injekt.compiler.DeclarationStore
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension

class InjektKtGenerationExtension(cacheDir: CacheDir) : AnalysisHandlerExtension {

    private val givenCallFileManager = GivenCallFileManager(cacheDir)

    private lateinit var lazyTopDownAnalyzer: LazyTopDownAnalyzer

    private var completed = false

    override fun analysisCompleted(
        project: Project,
        module: ModuleDescriptor,
        bindingTrace: BindingTrace,
        files: Collection<KtFile>,
    ): AnalysisResult? {
        if (completed) return null
        completed = true
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
        val checker = GivenCallChecker(
            bindingTrace,
            DeclarationStore(module),
            givenCallFileManager
        )
        files.forEach { it.accept(checker) }
        givenCallFileManager.flush()
        return null
    }
}
