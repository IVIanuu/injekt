package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.generator.componentimpl.ComponentFactoryImpl
import com.ivianuu.injekt.compiler.generator.componentimpl.ComponentImpl
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.namedDeclarationRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext

@Given
class RootFactoryGenerator(
    private val bindingContext: BindingContext,
    private val fileManager: FileManager,
    private val componentFactoryImplFactory: (
        Name,
        TypeRef,
        List<TypeRef>,
        TypeRef,
        ComponentImpl?,
    ) -> ComponentFactoryImpl
) : Generator {
    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            file.accept(
                namedDeclarationRecursiveVisitor { declaration ->
                    val descriptor = declaration.descriptor<DeclarationDescriptor>(bindingContext)
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
        val factoryImpl = componentFactoryImplFactory(
            factoryImplFqName.shortName(),
            descriptor.factoryType,
            descriptor.inputTypes,
            descriptor.contextType,
            null
        )
        factoryImpl.initialize()

        val code = buildCodeString {
            emitLine("// injekt-generated")
            emitLine("package ${factoryImplFqName.parent()}")
            with(factoryImpl) { emit() }
        }

        fileManager.generateFile(
            packageFqName = descriptor.factoryType.classifier.fqName.parent(),
            fileName = "${factoryImplFqName.shortName()}.kt",
            code = code
        )
    }
}
