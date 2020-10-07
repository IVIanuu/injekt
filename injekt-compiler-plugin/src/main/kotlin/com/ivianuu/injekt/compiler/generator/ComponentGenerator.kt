package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.generator.componentimpl.CallableBindingNode
import com.ivianuu.injekt.compiler.generator.componentimpl.ComponentImpl
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.namedDeclarationRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext

@Binding
class ComponentGenerator(
    private val bindingContext: BindingContext,
    private val declarationStore: DeclarationStore,
    private val fileManager: FileManager,
    private val componentImplFactory: (
        TypeRef,
        Name,
        ComponentImpl?,
    ) -> ComponentImpl
) : Generator {
    override fun generate(files: List<KtFile>) {
        var generateMergeComponents = false
        files.forEach { file ->
            file.accept(
                namedDeclarationRecursiveVisitor { declaration ->
                    val descriptor = declaration.descriptor<DeclarationDescriptor>(bindingContext)
                        ?: return@namedDeclarationRecursiveVisitor
                    generateMergeComponents = (generateMergeComponents ||
                            descriptor.hasAnnotation(InjektFqNames.GenerateMergeComponents))
                    if (descriptor is ClassDescriptor &&
                        descriptor.hasAnnotation(InjektFqNames.Component)
                    ) {
                        generateComponent(descriptor.defaultType.toTypeRef())
                    }
                }
            )
        }
        if (generateMergeComponents) {
            declarationStore.mergeComponents
                .forEach { generateComponent(it) }
        }
    }

    private fun generateComponent(componentType: TypeRef) {
        val componentImplFqName = componentType.classifier.fqName.toComponentImplFqName()
        val componentImpl = componentImplFactory(
            componentType,
            componentImplFqName.shortName(),
            null
        )
        componentImpl.initialize()

        // extensions functions cannot be called by their fully qualified name
        // because of that we collect all extension function calls and import them
        val imports = mutableSetOf<FqName>()

        fun ComponentImpl.collectImports() {
            imports += graph.resolvedBindings.values
                .filterIsInstance<CallableBindingNode>()
                .filter {
                    it.callable.valueParameters.firstOrNull()
                        ?.isExtensionReceiver == true
                }
                .map { it.callable.fqName }
            componentImpl.children.forEach { it.collectImports() }
        }

        componentImpl.collectImports()

        val code = buildCodeString {
            emitLine("package ${componentImplFqName.parent()}")
            imports.forEach { emitLine("import $it") }
            with(componentImpl) { emit() }

            /*emit("fun ${componentType.classifier.fqName.shortName()}(")
            componentImpl.constructorParameters.forEachIndexed { index, param ->
                emit("${param.name}: ${param.type.render()}")
                if (index != componentImpl.constructorParameters.lastIndex) emit(", ")
            }
            emit("): ${componentType.render()} ")
            braced {
                emitLine("return ${componentImplFqName.shortName()}(")
                componentImpl.constructorParameters.forEachIndexed { index, param ->
                    emit("${param.name}")
                    if (index != componentImpl.constructorParameters.lastIndex) emit(", ")
                }
                emitLine(")")
            }*/
        }

        fileManager.generateFile(
            packageFqName = componentType.classifier.fqName.parent(),
            fileName = "${componentImplFqName.shortName()}.kt",
            code = code
        )
    }
}
