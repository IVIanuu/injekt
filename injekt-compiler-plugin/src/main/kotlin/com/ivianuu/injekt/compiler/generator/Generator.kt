package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.ChildFactory
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import java.io.File

interface Generator {
    fun generate(files: List<KtFile>)
}

interface GenerationComponent {
    val functionAliasGeneratorFactory: ((FqName, String, String, File) -> Unit) -> FunctionAliasGenerator
    val fileManager: FileManager
    val rootFactoryGenerator: RootFactoryGenerator
}

@ChildFactory
typealias GenerationComponentFactory = (
    ModuleDescriptor,
    BindingTrace,
    BindingContext,
) -> GenerationComponent
