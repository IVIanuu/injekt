package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.CacheDir
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import com.ivianuu.injekt.compiler.generator.readercontextimpl.CallableGivenNode
import com.ivianuu.injekt.compiler.generator.readercontextimpl.ContextFactoryImpl
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.namedDeclarationRecursiveVisitor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Given(GenerationContext::class)
class RootContextFactoryImplGenerator : Generator {

    private val declarationStore = given<DeclarationStore>()

    override fun generate(files: List<KtFile>) {
        val initTriggerFile = given<CacheDir>().resolve("init_triggers")
        val initTriggers =
            (if (initTriggerFile.exists()) initTriggerFile.readLines() else emptyList())
                .map {
                    val tmp = it.split("=:=")
                    tmp[0] to FqName(tmp[1])
                }
                .flatMap { (type, fqName) ->
                    val memberScope = given<Indexer>().getMemberScope(fqName.parent())
                    val declarations: List<DeclarationDescriptor>? = when (type) {
                        "class" -> listOfNotNull(
                            memberScope?.getContributedClassifier(
                                fqName.shortName(), NoLookupLocation.FROM_BACKEND
                            )
                        )
                        "function" -> memberScope?.getContributedFunctions(
                            fqName.shortName(),
                            NoLookupLocation.FROM_BACKEND
                        )
                            ?.toList()
                        "property" -> memberScope?.getContributedVariables(
                            fqName.shortName(),
                            NoLookupLocation.FROM_BACKEND
                        )
                            ?.toList()
                        else -> error("Unexpected type $type")
                    }
                    declarations?.filter { it.hasAnnotation(InjektFqNames.InitializeInjekt) }
                        ?: emptyList()
                }
                .toMutableList()
        initTriggerFile.delete()
        files.forEach { file ->
            file.accept(
                namedDeclarationRecursiveVisitor {
                    val descriptor = it.descriptor<DeclarationDescriptor>()
                    if (descriptor.hasAnnotation(InjektFqNames.InitializeInjekt)) {
                        initTriggers += descriptor
                    }
                }
            )
        }

        println("init triggers $initTriggers")

        if (initTriggers.isEmpty()) return

        initTriggerFile.createNewFile()
        initTriggerFile.writeText(
            initTriggers.joinToString("\n") { declaration ->
                val type = when (declaration) {
                    is ClassDescriptor -> "class"
                    is FunctionDescriptor -> "function"
                    is PropertyDescriptor -> "property"
                    else -> error("Unexpected declaration $declaration ${declaration.javaClass}")
                }
                "$type=:=${declaration.fqNameSafe}"
            }
        )

        declarationStore.allRootFactories
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

        generateFile(
            packageFqName = descriptor.factoryImplFqName.parent(),
            fileName = "${descriptor.factoryImplFqName.shortName()}.kt",
            code = code
        )
    }

}
