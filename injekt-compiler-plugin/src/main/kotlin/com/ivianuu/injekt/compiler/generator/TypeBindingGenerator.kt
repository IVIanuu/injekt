package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.generator.componentimpl.emitCallableInvocation
import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.namedDeclarationRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module

@Binding
class TypeBindingGenerator(
    private val bindingContext: BindingContext,
    private val declarationStore: DeclarationStore,
    private val fileManager: FileManager,
    private val typeTranslator: TypeTranslator
) : Generator {

    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            val typeBindings = mutableListOf<CallableDescriptor>()
            file.accept(
                namedDeclarationRecursiveVisitor { declaration ->
                    val descriptor = declaration.descriptor<DeclarationDescriptor>(bindingContext)
                    if (descriptor is CallableDescriptor &&
                        descriptor.hasAnnotation(InjektFqNames.TypeBinding)) {
                        typeBindings += descriptor
                    }
                }
            )
            typeBindings.forEach { descriptor ->
                declarationStore.addGeneratedClassifier(
                    ClassifierRef(
                        fqName = descriptor.fqNameSafe
                            .parent().child(descriptor.name.asString().capitalize().asNameId()),
                        typeParameters = descriptor.typeParameters.map {
                            typeTranslator.toClassifierRef(it)
                        }
                    )
                )
            }
            typeBindings.forEach { descriptor ->
                generateTypeBinding(descriptor)
            }
        }
    }

    private fun generateTypeBinding(descriptor: CallableDescriptor) {
        val packageFqName = descriptor.findPackage().fqName
        val fileName = joinedNameOf(
            packageFqName,
            descriptor.fqNameSafe
        ).asString() + "TypeBinding.kt"
        val bindingFunctionName = "${descriptor.name.asString()}TypeBinding".asNameId()
        val code = buildCodeString {
            emitLine("package $packageFqName")
            emitLine("import ${InjektFqNames.Binding}")
            emitLine()

            val returnType = typeTranslator.toTypeRef(descriptor.returnType!!, descriptor)

            emit("typealias ${descriptor.name.asString().capitalize().asNameId()}")

            if (descriptor.typeParameters.isNotEmpty()) {
                emit("<")
                descriptor.typeParameters
                    .forEachIndexed { index, typeParameter ->
                        emit(typeParameter.name)
                        if (index != descriptor.typeParameters.lastIndex) emit(", ")
                    }
                emit(">")
            }
            emit(" = ${returnType.render()}")

            emitLine()

            emit("@Binding")
            val targetComponent = descriptor.annotations
                .findAnnotation(InjektFqNames.TypeBinding)
                ?.allValueArguments
                ?.get("scopeComponent".asNameId())
                ?.let { it as KClassValue }
                ?.getArgumentType(descriptor.module)
                ?.let { typeTranslator.toTypeRef(it, descriptor) }
            if (targetComponent != null) {
                emitLine("(${targetComponent.render()}::class)")
            } else {
                emitLine()
            }

            if (descriptor.hasAnnotation(InjektFqNames.Composable))
                emitLine("@${InjektFqNames.Composable}")
            if (descriptor.isSuspend) emit("suspend ")
            emit("fun ")
            if (descriptor.typeParameters.isNotEmpty()) {
                emit("<")
                descriptor.typeParameters
                    .forEachIndexed { index, typeParameter ->
                        emit("${typeParameter.name} : ${typeTranslator.toTypeRef(typeParameter.upperBounds.single(), descriptor).render()}")
                        if (index != descriptor.typeParameters.lastIndex) emit(", ")
                    }
                emit("> ")
            }
            descriptor.extensionReceiverParameter
                ?.let { typeTranslator.toTypeRef(it.type, descriptor) }
                ?.let { emit("(${it.render()}).") }
            emitLine("$bindingFunctionName(")

            val nonAssistedValueParameters = descriptor.valueParameters
            nonAssistedValueParameters
                .forEachIndexed { index, valueParameter ->
                    emit("${valueParameter.name}: ${valueParameter.type
                        .let { typeTranslator.toTypeRef(it, descriptor) }.render()}")
                    if (index != nonAssistedValueParameters.lastIndex) emit(", ")
                }
            emit("): ${descriptor.name.asString().capitalize().asNameId()}")
            if (descriptor.typeParameters.isNotEmpty()) {
                emit("<")
                descriptor.typeParameters
                    .forEachIndexed { index, typeParameter ->
                        emit(typeParameter.name)
                        if (index != descriptor.typeParameters.lastIndex) emit(", ")
                    }
                emit(">")
            }
            emitSpace()
            braced {
                val callable = declarationStore.callableForDescriptor(
                    when (descriptor){
                        is PropertyDescriptor -> descriptor.getter!!
                        else -> descriptor as FunctionDescriptor
                    }
                )
                emit("return ")
                emitCallableInvocation(
                    callable,
                    null,
                    callable.valueParameters.map { parameter ->
                        {
                            if (parameter.isExtensionReceiver) {
                                emit("this@$bindingFunctionName")
                            } else {
                                emit(parameter.name)
                            }
                        }
                    }
                )
                emitLine()
            }
        }

        fileManager.generateFile(packageFqName, fileName, code)

        val callableTypeParameters = descriptor.typeParameters
            .map {
                ClassifierRef(
                    packageFqName.child(bindingFunctionName)
                        .child(it.name),
                    superTypes = /*it.upperBounds
                        .map {
                            typeTranslator.toTypeRef(it, descriptor)
                        }*/ emptyList(),
                    isTypeParameter = true
                )
            }
        val bindingCallableSubstitutionMap = descriptor.typeParameters
            .map { typeTranslator.toClassifierRef(it) }
            .zip(callableTypeParameters.map { it.defaultType })
            .toMap()
        val bindingCallable = Callable(
            packageFqName = packageFqName,
            fqName = packageFqName.child(bindingFunctionName),
            name = bindingFunctionName,
            type = declarationStore.generatedClassifierFor(
                descriptor.fqNameSafe
                    .parent().child(descriptor.name.asString().capitalize().asNameId())
            )!!
                .defaultType
                .typeWith(callableTypeParameters.map { it.defaultType }),
            typeParameters = callableTypeParameters,
            valueParameters = (listOfNotNull(descriptor.extensionReceiverParameter) + descriptor.valueParameters)
                .map {
                    ValueParameterRef(
                        type = it.type
                            .let { typeTranslator.toTypeRef(it, descriptor) }
                            .substitute(bindingCallableSubstitutionMap),
                        isExtensionReceiver = it.name.isSpecial,
                        isAssisted = false,
                        name = it.name
                    )
                },
            targetComponent = null,
            contributionKind = Callable.ContributionKind.BINDING,
            isCall = true,
            callableKind = Callable.CallableKind.DEFAULT,
            isExternal = false
        )
        declarationStore.addGeneratedBinding(bindingCallable)
        declarationStore.addGeneratedInternalIndex(
            descriptor.findPsi()!!.containingFile as KtFile,
            Index(bindingCallable.fqName, "function")
        )
    }
}
