package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import com.ivianuu.injekt.compiler.generator.readercontextimpl.CallableGivenNode
import com.ivianuu.injekt.compiler.generator.readercontextimpl.ContextFactoryImpl
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.namedDeclarationRecursiveVisitor

@Given(GenerationContext::class)
class RootContextFactoryImplGenerator : Generator {

    private val fileManager = given<KtFileManager>()

    override fun generate(files: List<KtFile>) {
        var initTrigger: KtDeclaration? = null
        files.forEach { file ->
            file.accept(
                namedDeclarationRecursiveVisitor {
                    val descriptor = it.descriptor<DeclarationDescriptor>()
                    if (descriptor.hasAnnotation(InjektFqNames.InitializeInjekt)) {
                        initTrigger = initTrigger ?: it
                    }
                }
            )
        }

        if (initTrigger == null) return

        given<DeclarationStore>().allRootFactories
            .forEach { generateRootFactory(it) }
    }

    private fun generateRootFactory(
        descriptor: ContextFactoryImplDescriptor
    ) {
        val factoryImpl = ContextFactoryImpl(
            name = descriptor.factoryImplFqName.shortName(),
            factoryType = descriptor.factory.factoryType,
            inputTypes = descriptor.factory.inputTypes,
            contextType = descriptor.factory.contextType,
            parent = null
        )
        factoryImpl.initialize()

        // extensions functions cannot be called by their fully qualified name
        // because of that we collect all extension function calls and import them
        val imports = mutableSetOf<FqName>()

        fun ContextFactoryImpl.collectImports() {
            imports += context.graph.resolvedGivens.values
                .filterIsInstance<CallableGivenNode>()
                .filter {
                    it.callable.valueParameters.firstOrNull()
                        ?.isExtensionReceiver == true
                }
                .map { it.callable.fqName }
            context.children.forEach { it.collectImports() }
        }

        factoryImpl.collectImports()

        val code = buildCodeString {
            emitLine("// injekt-generated")
            emitLine("package ${descriptor.factoryImplFqName.parent()}")
            imports.forEach { emitLine("import $it") }
            with(factoryImpl) { emit() }
        }

        fileManager.generateFile(
            packageFqName = descriptor.factoryImplFqName.parent(),
            fileName = "${descriptor.factoryImplFqName.shortName()}.kt",
            code = code,
            originatingFiles = emptyList()
        )
    }

}
