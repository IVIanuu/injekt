package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.ChildComponent
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace

interface Generator {
    fun generate(files: List<KtFile>)
}

@ChildComponent
abstract class GenerationComponent(
    @Binding protected val moduleDescriptor: ModuleDescriptor,
    @Binding protected val bindingTrace: BindingTrace,
    @Binding protected val bindingContext: BindingContext
) {
    abstract val functionAliasGeneratorFactory: ((FqName, String, String) -> Unit) -> FunctionAliasGenerator
    abstract val fileManager: FileManager
    abstract val componentGenerator: ComponentGenerator
}
