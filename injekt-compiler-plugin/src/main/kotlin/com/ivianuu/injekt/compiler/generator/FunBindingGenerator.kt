package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.generator.componentimpl.emitCallableInvocation
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.namedFunctionRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Binding
class FunBindingGenerator(
    private val bindingContext: BindingContext,
    private val declarationStore: DeclarationStore,
    private val fileManager: FileManager,
    private val typeTranslator: TypeTranslator
) : Generator {

    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            file.accept(
                namedFunctionRecursiveVisitor { declaration ->
                    val descriptor = declaration.descriptor<FunctionDescriptor>(bindingContext)
                        ?: return@namedFunctionRecursiveVisitor
                    if (!descriptor.hasAnnotation(InjektFqNames.FunBinding)) return@namedFunctionRecursiveVisitor
                    val aliasDeclaration = ((declaration.parent as KtFile)
                        .declarations
                        .singleOrNull { it is KtTypeAlias && it.name == declaration.name }
                        ?: error("No typealias found for fun binding ${descriptor.name}")) as KtTypeAlias
                    val aliasDescriptor = aliasDeclaration.descriptor<TypeAliasDescriptor>(bindingContext)!!
                    generateFunBinding(descriptor, aliasDescriptor)
                }
            )
        }
    }

    private fun generateFunBinding(
        function: FunctionDescriptor,
        alias: TypeAliasDescriptor
    ) {
        val packageFqName = function.findPackage().fqName
        val fileName = joinedNameOf(
            packageFqName,
            function.fqNameSafe
        ).asString() + "FunBinding.kt"
        val bindingFunctionName = "${function.name.asString()}FunBinding".asNameId()

        val isExtensionAlias = alias.expandedType.hasAnnotation(KotlinBuiltIns.FQ_NAMES.extensionFunctionType)
        val functionValueParameters = alias.expandedType.arguments
            .drop(if (isExtensionAlias) 1 else 0)
            .dropLast(1)

        val dependencies = listOfNotNull(
            if (function.extensionReceiverParameter != null && !isExtensionAlias)
                function.extensionReceiverParameter else null
        ) + function.valueParameters
            .dropLast(functionValueParameters.size)

        val effects = function
            .annotations
            .filter { it.hasAnnotation(InjektFqNames.BindingModule) }

        val code = buildCodeString {
            emitLine("package $packageFqName")
            emitLine("import ${InjektFqNames.Binding}")
            emitLine()

            effects.forEach { emitLine("@${it.fqName!!}") }

            emitLine("@Binding")
            emit("fun ")
            if (alias.declaredTypeParameters.isNotEmpty()) {
                emit("<")
                function.typeParameters
                    .forEachIndexed { index, typeParameter ->
                        emit("${typeParameter.name} : ${typeTranslator.toTypeRef(typeParameter.upperBounds.single()).render()}")
                        if (index != function.typeParameters.lastIndex) emit(", ")
                    }
                emit("> ")
            }

            dependencies.firstOrNull()
                ?.takeIf { it == function.extensionReceiverParameter }
                ?.let { typeTranslator.toTypeRef(it.type) }
                ?.let { emit("${it.render()}.") }

            emit("$bindingFunctionName(")

            dependencies
                .drop(if (dependencies.firstOrNull() == function.extensionReceiverParameter) 1 else 0)
                .forEachIndexed { index, dependency ->
                    emit("${dependency.name}: ${typeTranslator.toTypeRef(dependency.type).render()}")
                    if (index != dependencies.lastIndex) emit(", ")
                }

            emit("): ${alias.name}")
            if (alias.declaredTypeParameters.isNotEmpty()) {
                emit("<")
                alias.declaredTypeParameters
                    .forEachIndexed { index, typeParameter ->
                        emit(typeParameter.name)
                        if (index != alias.declaredTypeParameters.lastIndex) emit(", ")
                    }
                emit(">")
            }
            emitSpace()
            braced {
                emit("return { ")
                functionValueParameters
                    .forEachIndexed { index, parameter ->
                        emit("p$index: ${typeTranslator.toTypeRef(parameter.type).renderExpanded()}")
                        if (index != functionValueParameters.lastIndex) emit(", ")
                    }
                emitLine(" ->")
                var functionParamIndex = 0
                val callable = declarationStore.callableForDescriptor(function)
                emitCallableInvocation(
                    callable,
                    null,
                    function.allParameters.map { parameter ->
                        when {
                            parameter !in dependencies -> {
                                {
                                    if (parameter == function.extensionReceiverParameter) {
                                        emit("this")
                                    } else {
                                        emit("p${functionParamIndex++}")
                                    }
                                }
                            }
                            else -> {
                                {
                                    if (parameter == function.extensionReceiverParameter) {
                                        emit("this@$bindingFunctionName")
                                    } else {
                                        emit(parameter.name)
                                    }
                                }
                            }
                        }
                    }
                )
                emitLine()
                emitLine("}")
            }
        }

        fileManager.generateFile(packageFqName, fileName, code)

        val callableTypeParameters = function.typeParameters
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
        val bindingCallableSubstitutionMap = function.typeParameters
            .map { typeTranslator.toClassifierRef(it) }
            .zip(callableTypeParameters.map { it.defaultType })
            .toMap()
        val bindingCallable = Callable(
            packageFqName = packageFqName,
            fqName = packageFqName.child(bindingFunctionName),
            name = bindingFunctionName,
            type = typeTranslator.toClassifierRef(alias)
                .defaultType
                .typeWith(callableTypeParameters.map { it.defaultType }),
            typeParameters = callableTypeParameters,
            valueParameters = dependencies
                .map {
                    ValueParameterRef(
                        type = it.type
                            .let { typeTranslator.toTypeRef(it) }
                            .substitute(bindingCallableSubstitutionMap),
                        isExtensionReceiver = it == function.extensionReceiverParameter,
                        name = it.name
                    )
                },
            targetComponent = null,
            contributionKind = Callable.ContributionKind.BINDING,
            bindingModules = effects
                .map { it.fqName!! },
            isCall = true,
            callableKind = Callable.CallableKind.DEFAULT,
            isExternal = false
        )
        declarationStore.addGeneratedBinding(bindingCallable, function.findPsi()!!.containingFile as KtFile)
        declarationStore.addGeneratedInternalIndex(
            function.findPsi()!!.containingFile as KtFile,
            Index(bindingCallable.fqName, "function")
        )
    }

}
