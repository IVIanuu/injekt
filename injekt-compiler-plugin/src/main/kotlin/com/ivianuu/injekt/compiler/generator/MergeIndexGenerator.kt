package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.classOrObjectRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Binding
class MergeIndexGenerator(
    private val bindingContext: BindingContext,
    private val declarationStore: DeclarationStore,
    private val fileManager: FileManager,
    private val supportsMerge: SupportsMerge
) : Generator {
    override fun generate(files: List<KtFile>) {
        if (!supportsMerge) return
        files.forEach { file ->
            val indices = mutableListOf<FqName>()
            file.accept(
                classOrObjectRecursiveVisitor { declaration ->
                    val descriptor = declaration.descriptor<ClassDescriptor>(bindingContext)
                        ?: return@classOrObjectRecursiveVisitor
                    if (descriptor.hasAnnotation(InjektFqNames.MergeComponent) ||
                            descriptor.hasAnnotation(InjektFqNames.MergeChildComponent) ||
                            descriptor.hasAnnotation(InjektFqNames.MergeInto)) {
                        indices += descriptor.fqNameSafe
                        declarationStore.addMergeDeclaration(descriptor.defaultType.toTypeRef())
                    }
                }
            )

            if (indices.isNotEmpty()) {
                val fileName = file.packageFqName.pathSegments().joinToString("_") + "_${file.name}"
                fileManager.generateFile(
                    packageFqName = InjektFqNames.MergeIndexPackage,
                    fileName = fileName,
                    code = buildCodeString {
                        emitLine("package ${InjektFqNames.MergeIndexPackage}")
                        indices.forEach { indexFqName ->
                            val indexName = indexFqName.pathSegments().joinToString("_")
                            emitLine("val $indexName = Unit")
                        }
                    }
                )
            }
        }
    }
}