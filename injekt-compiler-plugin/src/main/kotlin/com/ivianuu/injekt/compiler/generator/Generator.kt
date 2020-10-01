package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.ChildFactory
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace

interface Generator {
    fun generate(files: List<KtFile>)
}

interface GenerationComponent {
    val functionAliasGeneratorFactory: ((FqName, String, String) -> Unit) -> FunctionAliasGenerator
    val fileManager: FileManager
    val componentGenerator: ComponentGenerator
}

@ChildFactory
typealias GenerationComponentFactory = (
    ModuleDescriptor,
    BindingTrace,
    BindingContext,
) -> GenerationComponent
