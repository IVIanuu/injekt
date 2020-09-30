package com.ivianuu.injekt.compiler.generator.merge

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import com.ivianuu.injekt.compiler.generator.FileManager
import com.ivianuu.injekt.compiler.generator.Generator
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.asNameId
import com.ivianuu.injekt.compiler.generator.buildCodeString
import com.ivianuu.injekt.compiler.generator.descriptor
import com.ivianuu.injekt.compiler.generator.toTypeRef
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.namedDeclarationRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Given
class MergeFactoryGenerator(
    private val bindingContext: BindingContext,
    private val fileManager: FileManager,
    private val module: ModuleDescriptor
) : Generator {
    override fun generate(files: List<KtFile>) {
        val internalMergeFactories = mutableListOf<TypeRef>()
        val entryPoints = mutableListOf<Pair<FqName, FqName>>()
        val modules = mutableListOf<Pair<FqName, FqName>>()
        var generateMergeFactories = false
        files.forEach { file ->
            file.accept(
                namedDeclarationRecursiveVisitor { declaration ->
                    val descriptor = declaration.descriptor<DeclarationDescriptor>(bindingContext)
                    if (descriptor?.hasAnnotation(InjektFqNames.GenerateMergeFactories) == true) {
                        generateMergeFactories = true
                    }
                    if (descriptor is TypeAliasDescriptor &&
                            descriptor.hasAnnotation(InjektFqNames.MergeFactory)) {
                        internalMergeFactories += descriptor.defaultType.toTypeRef()
                    }
                    if (descriptor?.hasAnnotation(InjektFqNames.Module) == true) {
                        val component = descriptor.annotations.findAnnotation(InjektFqNames.Module)
                            ?.allValueArguments
                            ?.get("installIn".asNameId())
                            ?.let { it as KClassValue }
                            ?.getArgumentType(module)
                            ?.toTypeRef()
                            ?.classifier
                            ?.fqName
                        if (component != null) {
                            modules += component to descriptor.fqNameSafe
                        }
                    }
                    if (descriptor?.hasAnnotation(InjektFqNames.EntryPoint) == true) {
                        val component = descriptor.annotations.findAnnotation(InjektFqNames.EntryPoint)
                            ?.allValueArguments
                            ?.get("installIn".asNameId())
                            ?.let { it as KClassValue }
                            ?.getArgumentType(module)
                            ?.toTypeRef()
                            ?.classifier
                            ?.fqName
                        if (component != null) {
                            entryPoints += component to descriptor.fqNameSafe
                        }
                    }
                }
            )
        }

        (entryPoints + modules).forEach { (mergeFactory, element) ->
            val indexName = mergeFactory.pathSegments().joinToString("_") + "__" +
                    element.pathSegments().joinToString("_")
            fileManager.generateFile(
                InjektFqNames.MergeIndexPackage,
                "$indexName.kt",
                buildCodeString {
                    emitLine("package ${InjektFqNames.MergeIndexPackage}")
                    emitLine("val $indexName = Unit")
                }
            )
        }

        if (!generateMergeFactories) return

    }
}
