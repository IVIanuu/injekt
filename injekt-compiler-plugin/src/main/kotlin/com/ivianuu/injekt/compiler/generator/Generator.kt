package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.ChildFactory
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace

interface Generator {
    fun generate(files: List<KtFile>)
}

interface GenerationComponent {
    val functionAliasGenerator: FunctionAliasGenerator
    val fileManager: FileManager
    val rootFactoryGenerator: RootFactoryGenerator
}

@ChildFactory
typealias GenerationComponentFactory = (
    ModuleDescriptor,
    BindingTrace,
    BindingContext,
) -> GenerationComponent
