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

package com.ivianuu.injekt.ide

import com.intellij.openapi.application.Application
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.testFramework.registerServiceInstance
import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.analysis.GivenCallChecker
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

fun Application.registerGivenCallCheckerRunner(project: Project) {
    val existing = project.getService(KotlinCacheService::class.java)
    project.registerServiceInstance(
        KotlinCacheService::class.java,
        existing.intercepted()
    )
}

private fun KotlinCacheService.intercepted() = object : KotlinCacheService by this {
    override fun getResolutionFacade(elements: List<KtElement>) =
        this@intercepted.getResolutionFacade(elements)
            .intercepted()

    override fun getResolutionFacade(
        elements: List<KtElement>,
        platform: TargetPlatform,
    ) = this@intercepted.getResolutionFacade(elements, platform)
        .intercepted()

    override fun getResolutionFacadeByFile(
        file: PsiFile,
        platform: TargetPlatform,
    ) = this@intercepted.getResolutionFacadeByFile(file, platform)
        ?.intercepted()

    override fun getResolutionFacadeByModuleInfo(
        moduleInfo: ModuleInfo,
        platform: TargetPlatform,
    ) = this@intercepted.getResolutionFacadeByModuleInfo(moduleInfo, platform)
        ?.intercepted()
}

private fun ResolutionFacade.intercepted() = object : ResolutionFacade by this {

    private val declarationStore = DeclarationStore(moduleDescriptor)

    override fun analyze(
        elements: Collection<KtElement>,
        bodyResolveMode: BodyResolveMode,
    ): BindingContext {
        val result = this@intercepted.analyze(elements, bodyResolveMode)
        return runChecker(result, elements)
    }

    override fun analyze(
        element: KtElement,
        bodyResolveMode: BodyResolveMode,
    ): BindingContext {
        val result = this@intercepted.analyze(element, bodyResolveMode)
        return runChecker(result, listOf(element))
    }

    override fun analyzeWithAllCompilerChecks(elements: Collection<KtElement>): AnalysisResult {
        val result = this@intercepted.analyzeWithAllCompilerChecks(elements)
        val bindingContext = runChecker(result.bindingContext, elements)
        return AnalysisResult.success(bindingContext, moduleDescriptor)
    }

    private fun runChecker(
        bindingContext: BindingContext,
        elements: Collection<KtElement>,
    ): BindingContext {
        val bindingTrace = DelegatingBindingTrace(
            bindingContext,
            "Given call checker trace"
        )

        val checker = GivenCallChecker(bindingTrace, declarationStore, null)

        elements
            .forEach { element ->
                println("run checker on $element")
                try {
                    element.accept(checker ,null)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }

        return bindingTrace.bindingContext
    }
}