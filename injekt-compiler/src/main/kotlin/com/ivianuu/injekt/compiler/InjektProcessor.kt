package com.ivianuu.injekt.compiler

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor

@AutoService(SymbolProcessor::class)
class InjektProcessor : SymbolProcessor {

    private lateinit var codeGenerator: CodeGenerator

    override fun init(
        options: Map<String, String>,
        kotlinVersion: KotlinVersion,
        codeGenerator: CodeGenerator,
        logger: KSPLogger
    ) {
        this.codeGenerator = codeGenerator
    }

    override fun process(resolver: Resolver) {
        val component: GeneratorComponent = GeneratorComponentImpl(codeGenerator, resolver)
        val allFiles = resolver.getAllFiles()
        val isInMerge = allFiles.any { it.fileName.contains("SkipMergeMarker") }
        val filesToProcess = allFiles
            .filter {
                !isInMerge ||
                        it.packageName.asString() != "com.ivianuu.injekt.merge" ||
                        !it.fileName.contains("Api")
            }
        component.functionAliasGenerator.generate(filesToProcess)
        component.bindingModuleGenerator.generate(filesToProcess)
        component.indexGenerator.generate(filesToProcess)
        component.componentGenerator.generate(filesToProcess)
    }

    override fun finish() {
    }

}