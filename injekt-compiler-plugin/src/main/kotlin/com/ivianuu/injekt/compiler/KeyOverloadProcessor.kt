package com.ivianuu.injekt.compiler

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeVariableName
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.namedFunctionRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.types.Variance
import java.io.File

class KeyOverloadProcessor(private val outputDir: File) {

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
                    functions += descriptor
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