package com.ivianuu.injekt.compiler.codegen

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File

class InjektCodegenExtension(
    private val srcDir: File,
    private val resourcesDir: File,
    private val cacheDir: File
) : AnalysisHandlerExtension {

    private val serviceLoaderCache = KeyValueFileCache(
        cacheFile = cacheDir.resolve("sl-cache"),
        fromString = { it },
        toString = { it },
        onDelete = { moduleRegistrarManager.removeImpl(FqName(it)) }
    )
    private val moduleRegistrarManager = ModuleRegistrarManager(
        resourcesDir.resolve("META-INF/services/com.ivianuu.injekt.Module\$Registrar")
    )
    private val fileCache = KeyValueFileCache(
        cacheFile = cacheDir.resolve("file-cache"),
        fromString = { File(it) },
        toString = { it.absolutePath },
        onDelete = ::fileCacheOnDelete
    )

    private fun fileCacheOnDelete(file: File) {
        serviceLoaderCache.deleteDependents(file.absolutePath)
        fileCache.deleteDependents(file)
        file.delete()
    }

    private var generatedCode = false

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        if (!generatedCode) {
            files as ArrayList<KtFile>
            files.removeAll { it.text.contains("// injekt-generated") }
            files.forEach { fileCache.deleteDependents(File(it.virtualFilePath)) }
        }
        return null
    }

    override fun analysisCompleted(
        project: Project,
        module: ModuleDescriptor,
        bindingTrace: BindingTrace,
        files: Collection<KtFile>
    ): AnalysisResult? {
        if (generatedCode) {
            fileCache.flush()
            moduleRegistrarManager.flush()
            serviceLoaderCache.flush()
            return null
        }
        generatedCode = true

        val fileManager = FileManager(srcDir)
        val codeGenerators = listOf(
            EffectCodeGenerator(
                bindingTrace.bindingContext,
                fileManager,
                module
            )
        )

        codeGenerators.forEach {
            it.generate(files as List<KtFile>)
        }

        return AnalysisResult.RetryWithAdditionalRoots(
            bindingTrace.bindingContext, module, emptyList(), listOf(srcDir), true
        )
    }

}
