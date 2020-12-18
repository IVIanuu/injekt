package com.ivianuu.injekt.ide

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.testFramework.registerServiceInstance
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

fun registerGivenCallCheckerRunner(project: Project) {
    val existing = project.getService(KotlinCacheService::class.java)
    project.registerServiceInstance(
        KotlinCacheService::class.java,
        existing.intercepted()
    )
}

private fun KotlinCacheService.intercepted() = object : KotlinCacheService by this {
    override fun getResolutionFacade(elements: List<KtElement>): ResolutionFacade {
        println("get resolution facade $elements")
        return this@intercepted.getResolutionFacade(elements)
            .intercepted()
    }

    override fun getResolutionFacade(
        elements: List<KtElement>,
        platform: TargetPlatform,
    ): ResolutionFacade {
        println("reso facade for elements $elements")
        return this@intercepted.getResolutionFacade(elements, platform)
            .intercepted()
    }

    override fun getResolutionFacadeByFile(
        file: PsiFile,
        platform: TargetPlatform,
    ): ResolutionFacade? {
        println("reso facade by file $file")
        return this@intercepted.getResolutionFacadeByFile(file, platform)
            ?.intercepted()
    }

    override fun getResolutionFacadeByModuleInfo(
        moduleInfo: ModuleInfo,
        platform: TargetPlatform,
    ): ResolutionFacade? {
        println("reso facade by module $moduleInfo")
        return this@intercepted.getResolutionFacadeByModuleInfo(moduleInfo, platform)
            ?.intercepted()
    }
}

private fun ResolutionFacade.intercepted() = object : ResolutionFacade by this {
    override fun analyze(
        elements: Collection<KtElement>,
        bodyResolveMode: BodyResolveMode,
    ): BindingContext {
        println("analyse multiple $elements $bodyResolveMode")
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
        println("analyse with all checls $elements result $result $bindingContext")
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

        elements
            .map { it.containingKtFile }
            .distinct()
            .forEach {
                try {
                    it.accept(
                        GivenCallChecker(bindingTrace, moduleDescriptor),
                        null
                    )
                } catch (e: Throwable) {
                }
            }

        return bindingTrace.bindingContext
    }
}