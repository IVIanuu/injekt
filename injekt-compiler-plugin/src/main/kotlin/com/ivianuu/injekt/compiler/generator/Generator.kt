package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.ChildComponent
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

interface Generator {
    fun generate(files: List<KtFile>)
}

typealias SupportsMerge = Boolean

@ChildComponent
abstract class GenerationComponent(
    @Binding protected val moduleDescriptor: ModuleDescriptor,
    @Binding protected val bindingContext: BindingContext
) {
    abstract val functionAliasGeneratorFactory: ((FqName, String, String) -> Unit) -> FunctionAliasGenerator
    abstract val fileManager: FileManager
    abstract val bindingComponentGenerator: BindingComponentGenerator
    abstract val componentGenerator: ComponentGenerator
    abstract val mergeIndexGenerator: MergeIndexGenerator

    @Binding protected fun supportsMerge(moduleDescriptor: ModuleDescriptor): SupportsMerge =
        moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(InjektFqNames.MergeComponent)) != null
}
