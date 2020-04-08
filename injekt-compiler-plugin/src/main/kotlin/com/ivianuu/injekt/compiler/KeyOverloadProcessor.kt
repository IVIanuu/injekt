package com.ivianuu.injekt.compiler

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeVariableName
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.namedFunctionRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import java.io.File

class KeyOverloadProcessor(
    private val module: ModuleDescriptor,
    private val outputDir: File
) {

    private val qualifier by lazy {
        module.findClassAcrossModuleDependencies(
            ClassId.topLevel(InjektClassNames.Qualifier)
        )!!
    }

    // todo generate file per overload and only generate if it does not exist
    fun processKeyOverloads(
        file: KtFile,
        bindingTrace: BindingTrace,
        onFileGenerated: () -> Unit
    ) {
        val functions = mutableListOf<FunctionDescriptor>()
        file.accept(
            namedFunctionRecursiveVisitor { function ->
                val descriptor = bindingTrace[BindingContext.FUNCTION, function]
                if (descriptor?.annotations?.hasAnnotation(InjektClassNames.KeyOverload) == true) {
                    val packageDescriptor = descriptor.findPackage()

                    val exists = packageDescriptor.getMemberScope()
                        .getContributedFunctions(descriptor.name, NoLookupLocation.FROM_BACKEND)
                        .filter { otherFunction ->
                            otherFunction.typeParametersCount == descriptor.typeParametersCount &&
                                    otherFunction.valueParameters.size == descriptor.valueParameters.size &&
                                    otherFunction.valueParameters.all { otherValueParameter ->
                                        val descriptorValueParameter =
                                            descriptor.valueParameters[otherValueParameter.index]
                                        if (descriptorValueParameter.index == 0) {
                                            otherValueParameter.type.isSubtypeOf(qualifier.defaultType)
                                        } else {
                                            otherValueParameter.name == descriptorValueParameter.name
                                        }
                                    }
                        }
                        .any()

                    if (!exists) functions += descriptor
                }
            }
        )

        if (functions.isNotEmpty()) {
            FileSpec.builder(
                    file.packageFqName.asString(),
                    "${file.name.removeSuffix(".kt")}Stubs.kt"
                )
                .apply {
                    functions.forEach { function ->
                        addFunction(keyOverloadStubFunction(function))
                    }
                }
                .build()
                .writeTo(outputDir)
            onFileGenerated()
        }
    }

    private fun keyOverloadStubFunction(function: FunctionDescriptor): FunSpec {
        return FunSpec.builder(function.name.asString())
            .addAnnotations(
                function.annotations
                    .map {
                        if (it.fqName == InjektClassNames.KeyOverload) {
                            AnnotationSpec.builder(InjektClassNames.KeyOverloadStub.asClassName())
                                .build()
                        } else {
                            AnnotationSpec.builder(it.fqName!!.asClassName())
                                .build()
                        }
                    }
            )
            .apply {
                if (function.isInline) {
                    addModifiers(KModifier.INLINE)
                }
                if (function.visibility == Visibilities.INTERNAL) {
                    addModifiers(KModifier.INTERNAL)
                }
                if (function.isOperator) {
                    addModifiers(KModifier.OPERATOR)
                }
                if (function.isSuspend) {
                    addModifiers(KModifier.SUSPEND)
                }
            }
            .addTypeVariables(
                function.typeParameters
                    .map { typeParameter ->
                        TypeVariableName(
                            typeParameter.name.asString(),
                            *typeParameter.upperBounds
                                .map { it.asTypeName()!! }
                                .toTypedArray(),
                            variance = when (typeParameter.variance) {
                                Variance.INVARIANT -> null
                                Variance.IN_VARIANCE -> KModifier.IN
                                Variance.OUT_VARIANCE -> KModifier.OUT
                            }
                        )
                    }
            )
            .apply {
                function.extensionReceiverParameter?.let { extensionReceiver ->
                    receiver(extensionReceiver.type.asTypeName()!!)
                } ?: function.dispatchReceiverParameter?.let { dispatchReceiver ->
                    receiver(dispatchReceiver.type.asTypeName()!!)
                }
            }
            .addParameter(
                ParameterSpec.builder(
                        "qualifier",
                        InjektClassNames.Qualifier.asClassName()
                    )
                    .defaultValue("error(\"stub\")\n")
                    .build()
            )
            .addParameters(
                function.valueParameters
                    .drop(1)
                    .map { valueParameter ->
                        ParameterSpec.builder(
                                valueParameter.name.asString(),
                                valueParameter.type.asTypeName()!!
                            )
                            .apply {
                                if (valueParameter.declaresDefaultValue()) {
                                    if (valueParameter.type.isFunctionType &&
                                        function.isInline && !valueParameter.isNoinline
                                    ) {
                                        defaultValue("{ ${
                                        valueParameter.type.getValueParameterTypesFromFunctionType()
                                            .indices.joinToString(", ") { "_" }
                                        } error(\"stub\") }")
                                    } else {
                                        defaultValue("error(\"stub\")")
                                    }
                                }

                                if (valueParameter.isCrossinline) {
                                    addModifiers(KModifier.CROSSINLINE)
                                } else if (valueParameter.isNoinline) {
                                    addModifiers(KModifier.NOINLINE)
                                }
                            }
                            .build()
                    }
            )
            .apply { function.returnType?.let { returns(it.asTypeName()!!) } }
            .addCode("error(\"stub\")\n")
            .build()
    }

}