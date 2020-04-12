package com.ivianuu.injekt.compiler

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeVariableName
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.declarationRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class GenerateDslProcessor(
    private val moduleDescriptor: ModuleDescriptor
) : ElementProcessor {

    private val qualifier by lazy {
        moduleDescriptor.findClassAcrossModuleDependencies(
            ClassId.topLevel(InjektClassNames.Qualifier)
        )!!
    }

    override fun processFile(
        file: KtFile,
        bindingTrace: BindingTrace,
        generateFile: (FileSpec) -> Unit
    ) {
        val behaviors = mutableListOf<DeclarationDescriptor>()

        file.accept(
            declarationRecursiveVisitor { declaration ->
                when (declaration) {
                    is KtFunction -> {
                        val descriptor =
                            bindingTrace[BindingContext.FUNCTION, declaration] as? FunctionDescriptor
                                ?: return@declarationRecursiveVisitor

                        if (descriptor.annotations.hasAnnotation(InjektClassNames.GenerateDsl)) {
                            behaviors += descriptor
                        }
                    }
                    is KtProperty -> {
                        val descriptor =
                            bindingTrace[BindingContext.VARIABLE, declaration] as? PropertyDescriptor
                                ?: return@declarationRecursiveVisitor

                        if (descriptor.annotations.hasAnnotation(InjektClassNames.GenerateDsl)) {
                            behaviors += descriptor
                        }
                    }
                }
            }
        )

        if (behaviors.isNotEmpty()) {
            FileSpec.builder(
                    file.packageFqName.asString(),
                    "${file.name.removeSuffix(".kt")}GeneratedDsl"
                )
                .apply {
                    behaviors.forEach { behavior ->
                        val annotation = behavior.annotations
                            .findAnnotation(InjektClassNames.GenerateDsl)!!

                        val generateBuilder =
                            annotation.argumentValue("generateBuilder")?.value as? Boolean ?: true

                        val builderName =
                            annotation
                                .argumentValue("builderName")
                                ?.value
                                ?.cast<String>()
                                ?.takeIf { it.isNotEmpty() }
                                ?: behavior.name.asString().decapitalize()

                        if (generateBuilder) {
                            addFunction(keyOverloadStubBuilder(behavior, builderName))
                            addFunction(keyOverloadBuilder(behavior, builderName))
                        }

                        val generateDelegate =
                            annotation.argumentValue("generateDelegate")?.value as? Boolean ?: false

                        val delegateName =
                            annotation
                                .argumentValue("delegateName")
                                ?.value
                                ?.cast<String>()
                                ?.takeIf { it.isNotEmpty() }
                                ?: behavior.name.asString().decapitalize()

                        if (generateDelegate) {
                            addFunction(keyOverloadStubDelegate(behavior, delegateName))
                            addFunction(keyOverloadDelegate(behavior, delegateName))
                            addProperty(delegateQualifierProperty(behavior))
                        }
                    }
                }
                .build()
                .let(generateFile)
        }
    }

    private fun keyOverloadStubBuilder(
        declarationDescriptor: DeclarationDescriptor,
        name: String
    ) = baseDslBuilder(
        name = name,
        declaration = declarationDescriptor,
        annotation = InjektClassNames.KeyOverloadStub,
        firstParameter = ParameterSpec.builder(
                "qualifier",
                InjektClassNames.Qualifier.asClassName()
            )
            .defaultValue("error(\"stub\")")
            .build(),
        code = "error(\"stub\")"
    )

    private fun keyOverloadBuilder(
        declarationDescriptor: DeclarationDescriptor,
        name: String
    ) = baseDslBuilder(
        name = name,
        declaration = declarationDescriptor,
        annotation = InjektClassNames.KeyOverload,
        firstParameter = ParameterSpec.builder(
            "key",
            InjektClassNames.Key.asClassName()
                .parameterizedBy(TypeVariableName("BindingType"))
        ).build(),
        code = "bind(key = key,\n" +
                "behavior = ${
                declarationDescriptor.name.asString() + declarationDescriptor.safeAs<FunctionDescriptor>()
                    ?.let { function ->
                        function.typeParameters
                            .takeIf { it.isNotEmpty() }
                            ?.joinToString(",") { it.name.asString() }
                            ?.let { "<$it>" }
                            .orEmpty() + "(" + function.valueParameters.joinToString("") {
                            "${it.name} = ${it.name},\n"
                        } + ")"
                    }.orEmpty()
                } + behavior,\n" +
                "duplicateStrategy = duplicateStrategy,\n" +
                "provider = provider\n)"
    )

    private fun baseDslBuilder(
        name: String,
        declaration: DeclarationDescriptor,
        annotation: FqName,
        firstParameter: ParameterSpec,
        code: String
    ): FunSpec = FunSpec.builder(name)
        .addKdoc("Dsl builder for the [${declaration.name}] behavior")
        .addAnnotation(annotation.asClassName())
        .addTypeVariable(TypeVariableName("BindingType"))
        .apply {
            if (declaration is FunctionDescriptor) {
                addTypeVariables(
                    declaration.typeParameters
                        .map { typeParameter ->
                            TypeVariableName(
                                typeParameter.name.asString(),
                                typeParameter.upperBounds
                                    .map { it.asTypeName()!! }
                            )
                        }
                )
            }
        }
        .receiver(InjektClassNames.ComponentBuilder.asClassName())
        .addParameter(firstParameter)
        .apply {
            if (declaration is FunctionDescriptor) {
                declaration.valueParameters.forEach { valueParameter ->
                    addParameter(
                        ParameterSpec.builder(
                                valueParameter.name.asString(),
                                valueParameter.type.asTypeName()!!
                            )
                            .apply {
                                if (valueParameter.declaresDefaultValue()) {
                                    defaultValue(
                                        valueParameter.findPsi()
                                            .cast<KtParameter>().defaultValue!!.text
                                    )
                                }
                            }
                            .build()
                    )
                }
            }
        }
        .addParameter(
            ParameterSpec.builder(
                    "behavior",
                    InjektClassNames.Behavior.asClassName()
                )
                .defaultValue("Behavior.None")
                .build()
        )
        .addParameter(
            ParameterSpec.builder(
                    "duplicateStrategy",
                    InjektClassNames.DuplicateStrategy.asClassName()
                )
                .defaultValue("DuplicateStrategy.Fail")
                .build()
        )
        .addParameter(
            ParameterSpec.builder(
                    "provider",
                    InjektClassNames.BindingProvider.asClassName()
                        .parameterizedBy(TypeVariableName("BindingType"))
                )
                .build()
        )
        .addCode(code)
        .build()

    private fun keyOverloadStubDelegate(
        declaration: DeclarationDescriptor,
        name: String
    ) = baseDslDelegate(
        name = name,
        declaration = declaration,
        annotation = InjektClassNames.KeyOverloadStub,
        firstParameter = ParameterSpec.builder(
                "qualifier",
                InjektClassNames.Qualifier.asClassName()
            )
            .defaultValue("error(\"stub\")")
            .build(),
        code = "error(\"stub\")"
    )

    private fun keyOverloadDelegate(
        declaration: DeclarationDescriptor,
        name: String
    ) = baseDslDelegate(
        name = name,
        declaration = declaration,
        annotation = InjektClassNames.KeyOverload,
        firstParameter = ParameterSpec.builder(
            "key",
            InjektClassNames.Key.asClassName()
                .parameterizedBy(TypeVariableName("BindingType"))
        ).build(),
        code = "bind(key = key.copy(qualifier = key.qualifier + ${declaration.name}Init),\n" +
                "behavior = ${
                declaration.name.asString() + declaration.safeAs<FunctionDescriptor>()
                    ?.let { function ->
                        function.typeParameters
                            .takeIf { it.isNotEmpty() }
                            ?.joinToString(",") { it.name.asString() }
                            ?.let { "<$it>" }
                            .orEmpty() + "(" + function.valueParameters.joinToString("") {
                            "${it.name} = ${it.name},\n"
                        } + ")"
                    }.orEmpty()
                },\nduplicateStrategy = com.ivianuu.injekt.DuplicateStrategy.Drop,\n" +
                "provider = { get(key) })"
    )

    private fun baseDslDelegate(
        name: String,
        declaration: DeclarationDescriptor,
        annotation: FqName,
        firstParameter: ParameterSpec,
        code: String
    ): FunSpec = FunSpec.builder(name)
        .addKdoc("Delegate for the [${declaration.name}] behavior")
        .addAnnotation(annotation.asClassName())
        .addTypeVariable(TypeVariableName("BindingType"))
        .apply {
            if (declaration is FunctionDescriptor) {
                addTypeVariables(
                    declaration.typeParameters
                        .map { typeParameter ->
                            TypeVariableName(
                                typeParameter.name.asString(),
                                typeParameter.upperBounds
                                    .map { it.asTypeName()!! }
                            )
                        }
                )
            }
        }
        .receiver(InjektClassNames.ComponentBuilder.asClassName())
        .addParameter(firstParameter)
        .apply {
            if (declaration is FunctionDescriptor) {
                declaration.valueParameters.forEach { valueParameter ->
                    addParameter(
                        ParameterSpec.builder(
                                valueParameter.name.asString(),
                                valueParameter.type.asTypeName()!!
                            )
                            .apply {
                                if (valueParameter.declaresDefaultValue()) {
                                    defaultValue(
                                        valueParameter.findPsi()
                                            .cast<KtParameter>().defaultValue!!.text
                                    )
                                }
                            }
                            .build()
                    )
                }
            }
        }
        .addCode(code)
        .build()

    private fun delegateQualifierProperty(
        declaration: DeclarationDescriptor
    ) = PropertySpec.builder(
            "${declaration.name}Init",
            qualifier.asClassName()!!,
            KModifier.PRIVATE
        )
        .initializer("com.ivianuu.injekt.Qualifier()")
        .build()

}
