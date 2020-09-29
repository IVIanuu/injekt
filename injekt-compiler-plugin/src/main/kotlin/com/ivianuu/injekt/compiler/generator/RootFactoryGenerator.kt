package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import com.ivianuu.injekt.compiler.generator.componentimpl.ComponentFactoryImpl
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.namedDeclarationRecursiveVisitor

@Given
class RootFactoryGenerator : Generator {
    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            file.accept(
                namedDeclarationRecursiveVisitor { declaration ->
                    val descriptor = declaration.descriptor<DeclarationDescriptor>()
                        ?: return@namedDeclarationRecursiveVisitor
                    if (descriptor is TypeAliasDescriptor &&
                        descriptor.hasAnnotation(InjektFqNames.RootFactory)
                    ) {
                        generateRootFactory(FactoryDescriptor(descriptor.defaultType.toTypeRef()))
                    }
                }
            )
        }
    }

    private fun generateRootFactory(descriptor: FactoryDescriptor) {
        val factoryImplFqName = descriptor.factoryType.classifier.fqName.toFactoryImplFqName()
        val factoryImpl = ComponentFactoryImpl(
            name = factoryImplFqName.shortName(),
            factoryType = descriptor.factoryType,
            inputTypes = descriptor.inputTypes,
            contextType = descriptor.contextType,
            parent = null
        )
        factoryImpl.initialize()

        val code = buildCodeString {
            emitLine("// injekt-generated")
            emitLine("package ${factoryImplFqName.parent()}")
            with(factoryImpl) { emit() }
        }

        given<FileManager>().generateFile(
            packageFqName = descriptor.factoryType.classifier.fqName.parent(),
            fileName = "${factoryImplFqName.shortName()}.kt",
            code = code
        )
    }
}
