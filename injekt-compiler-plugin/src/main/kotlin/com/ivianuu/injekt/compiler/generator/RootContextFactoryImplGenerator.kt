package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import com.ivianuu.injekt.compiler.generator.readercontextimpl.ContextFactoryImpl
import com.ivianuu.injekt.compiler.irtransform.asNameId
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.namedDeclarationRecursiveVisitor
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Given(GenerationContext::class)
class RootContextFactoryImplGenerator : Generator {

    private val fileManager = given<KtFileManager>()

    private val internalRootFactories = mutableSetOf<ContextFactoryImplDescriptor>()

    fun addRootFactory(factoryDescriptor: ContextFactoryImplDescriptor) {
        internalRootFactories += factoryDescriptor
    }

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

        given<ModuleDescriptor>().getPackage(InjektFqNames.IndexPackage)
            .memberScope
            .let { memberScope ->
                (memberScope.getClassifierNames() ?: emptySet())
                    .map {
                        memberScope.getContributedClassifier(
                            it,
                            NoLookupLocation.FROM_BACKEND
                        )
                    }
            }
            .filterIsInstance<ClassDescriptor>()
            .mapNotNull { index ->
                index.annotations.findAnnotation(InjektFqNames.Index)
                    ?.takeIf { annotation ->
                        annotation.allValueArguments["type".asNameId()]
                            .let { it as StringValue }
                            .value == "class"
                    }
                    ?.let { annotation ->
                        val fqName =
                            annotation.allValueArguments.getValue("fqName".asNameId())
                                .let { it as StringValue }
                                .value
                                .let { FqName(it) }
                        if (!isInjektCompiler &&
                            fqName.asString().startsWith("com.ivianuu.injekt.compiler")
                        ) return@mapNotNull null
                        given<ModuleDescriptor>()
                            .findClassAcrossModuleDependencies(ClassId.topLevel(fqName))
                    }
            }
            .mapNotNull { index ->
                index.annotations.findAnnotation(InjektFqNames.RootContextFactory)
                    ?.allValueArguments
                    ?.values
                    ?.single()
                    ?.let { it as StringValue }
                    ?.value
                    ?.let { FqName(it) }
                    ?.let {
                        index.unsubstitutedMemberScope.getContributedFunctions(
                            "create".asNameId(), NoLookupLocation.FROM_BACKEND
                        )
                            .single().returnType!!
                            .constructor.declarationDescriptor!!.fqNameSafe to it
                    }
            }
            .forEach {

            }

        internalRootFactories.forEach { generateRootFactory(it) }
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
        val code = buildCodeString {
            emitLine("// injekt-generated")
            emitLine("package ${descriptor.factoryImplFqName.parent()}")
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
