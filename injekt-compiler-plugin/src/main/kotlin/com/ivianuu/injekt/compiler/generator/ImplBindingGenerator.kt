package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.classOrObjectRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny

@Binding
class ImplBindingGenerator(
    private val bindingContext: BindingContext,
    private val declarationStore: DeclarationStore,
    private val fileManager: FileManager
) : Generator {

    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            file.accept(
                classOrObjectRecursiveVisitor { declaration ->
                    val descriptor = declaration.descriptor<ClassDescriptor>(bindingContext)
                        ?: return@classOrObjectRecursiveVisitor
                    if (descriptor.hasAnnotation(InjektFqNames.ImplBinding)) {
                        generateImplBinding(descriptor)
                    }
                }
            )
        }
    }

    private fun generateImplBinding(descriptor: ClassDescriptor) {
        val singleSuperType = descriptor.defaultType.constructor
            .supertypes.first { !it.isAnyOrNullableAny() }
            .toTypeRef()
        val packageFqName = descriptor.findPackage().fqName
        val fileName = joinedNameOf(
            packageFqName,
            descriptor.fqNameSafe
        ).asString() + "ImplBinding.kt"
        val implFunctionName = "${descriptor.name.asString()}Binding".asNameId()
        val superTypeFunctionName = singleSuperType.classifier.fqName.shortName()
            .asString().decapitalize().asNameId()
        val targetComponent = descriptor.annotations
            .findAnnotation(InjektFqNames.ImplBinding)
            ?.allValueArguments
            ?.get("scopeComponent".asNameId())
            ?.let { it as KClassValue }
            ?.getArgumentType(descriptor.module)
            ?.toTypeRef()
        val injectConstructor = descriptor.getInjectConstructor()!!
        fileManager.generateFile(
            packageFqName = packageFqName,
            fileName = fileName,
            code = buildCodeString {
                emitLine("package $packageFqName")
                emitLine("import ${InjektFqNames.Binding}")

                emit("@Binding")
                if (targetComponent != null) emitLine("(${targetComponent.classifier.fqName}::class)")
                else emitLine()
                emitLine("fun $implFunctionName(")
                injectConstructor.valueParameters
                    .forEachIndexed { index, valueParameter ->
                        emit("${valueParameter.name}: ${valueParameter.type.toTypeRef().render()}")
                        if (index != injectConstructor.valueParameters.lastIndex) emit(", ")
                    }
                emit("): ${descriptor.defaultType.toTypeRef().render()} ")
                braced {
                    emitLine("return ${descriptor.name}(")
                    injectConstructor.valueParameters
                        .forEachIndexed { index, valueParameter ->
                            emit("${valueParameter.name}")
                            if (index != injectConstructor.valueParameters.lastIndex) emit(", ")
                        }
                    emitLine(")")
                }
                emitLine()
                emitLine("@Binding")
                emitLine("val ${descriptor.name}.$superTypeFunctionName: ${singleSuperType.render()}")
                indented { emitLine("get() = this") }
            }
        )

        val implCallable = Callable(
            packageFqName = packageFqName,
            fqName = packageFqName.child(implFunctionName),
            name = implFunctionName,
            type = descriptor.defaultType.toTypeRef(),
            typeParameters = emptyList(),
            valueParameters = injectConstructor.valueParameters
                .map {
                    ValueParameterRef(
                        type = it.type.toTypeRef(),
                        isExtensionReceiver = false,
                        isAssisted = it.type.hasAnnotation(InjektFqNames.Assisted),
                        name = it.name
                    )
                },
            targetComponent = targetComponent,
            contributionKind = Callable.ContributionKind.BINDING,
            isCall = true,
            isSuspend = false,
            isExternal = false
        )
        declarationStore.addGeneratedBinding(implCallable)
        declarationStore.addGeneratedInternalIndex(
            descriptor.findPsi()!!.containingFile as KtFile,
            Index(implCallable.fqName, "function")
        )

        val superTypeCallable = Callable(
            packageFqName = packageFqName,
            fqName = packageFqName.child(superTypeFunctionName),
            name = superTypeFunctionName,
            type = singleSuperType,
            typeParameters = emptyList(),
            valueParameters = listOf(
                ValueParameterRef(
                    type = descriptor.defaultType.toTypeRef(),
                    isExtensionReceiver = true,
                    isAssisted = false,
                    name = "receiver".asNameId()
                )
            ),
            targetComponent = null,
            contributionKind = Callable.ContributionKind.BINDING,
            isCall = false,
            isSuspend = false,
            isExternal = false
        )

        declarationStore.addGeneratedBinding(superTypeCallable)
        declarationStore.addGeneratedInternalIndex(
            descriptor.findPsi()!!.containingFile as KtFile,
            Index(superTypeCallable.fqName, "function")
        )
    }

}
