package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.extensions.*
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.storage.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class TypeClassSyntheticResolveExtension : SyntheticResolveExtension {
    override fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name? =
        if (thisDescriptor.hasAnnotation(InjektFqNames.Extension)) {
            SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
        } else null

    override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> =
        thisDescriptor
            .takeIf { it.isCompanionObject }
            ?.containingDeclaration
            ?.cast<ClassDescriptor>()
            ?.takeIf { it.hasAnnotation(InjektFqNames.Extension) }
            ?.getTypeClassFunctions()
            ?.map { it.name }
            ?: emptyList()

    override fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {
        val typeClassFunctions = thisDescriptor
            .takeIf { it.isCompanionObject }
            ?.containingDeclaration
            ?.cast<ClassDescriptor>()
            ?.takeIf { it.hasAnnotation(InjektFqNames.Extension) }
            ?.getTypeClassFunctions()
            ?.filter { it.name == name }
            ?.map { TypeClassFunctionDescriptor(InjektContext(thisDescriptor.module), it, thisDescriptor) }
            ?: return
        result += typeClassFunctions
    }
}

private fun ClassDescriptor.getTypeClassFunctions(): List<SimpleFunctionDescriptor> =
    unsubstitutedMemberScope
        .getContributedDescriptors()
        .filterIsInstance<SimpleFunctionDescriptor>()
        .filter { it.extensionReceiverParameter != null }

class TypeClassFunctionDescriptor(
    context: InjektContext,
    function: SimpleFunctionDescriptor,
    containing: ClassDescriptor
) : SimpleFunctionDescriptorImpl(
    containing,
    null,
    Annotations.create(
        function.annotations + AnnotationDescriptorImpl(
            function.module.findClassAcrossModuleDependencies(
                ClassId.topLevel(InjektFqNames.SyntheticExtensionCallable)
            )!!.defaultType,
            mapOf("value".asNameId() to StringValue(
                function.uniqueKey(context))
            ),
            SourceElement.NO_SOURCE
        )
    ),
    function.name,
    function.kind,
    function.source
) {
    init {
        val typeClass = function.containingDeclaration.cast<ClassDescriptor>()
        val typeParametersMap = (typeClass.declaredTypeParameters + function.typeParameters)
            .associateWith {
                TypeParameterDescriptorImpl.createForFurtherModification(
                    this,
                    it.annotations,
                    it.isReified,
                    it.variance,
                    it.name,
                    it.index,
                    it.source,
                    LockBasedStorageManager.NO_LOCKS
                )
            }
        val substitutor = TypeSubstitutor.create(
            typeParametersMap
                .map { it.key.typeConstructor to it.value.defaultType.asTypeProjection() }
                .toMap()
        )
        typeParametersMap.forEach { (source, dest) ->
            source.upperBounds.forEach { upperBound ->
                dest.addUpperBound(
                    substitutor.safeSubstitute(upperBound, Variance.INVARIANT)
                )
            }
        }
        typeParametersMap.values.forEach { it.setInitialized() }
        initialize(
            function.extensionReceiverParameter
                ?.let {
                      DescriptorFactory.createExtensionReceiverParameterForCallable(
                          this,
                          substitutor.safeSubstitute(it.type, Variance.INVARIANT),
                          it.annotations
                      )
                },
            ReceiverParameterDescriptorImpl(
                this,
                ImplicitClassReceiver(containing),
                Annotations.EMPTY
            ),
            typeParametersMap.values.toList(),
            function.valueParameters
                .map { valueParameter ->
                    ValueParameterDescriptorImpl(
                        this,
                        null,
                        valueParameter.index,
                        valueParameter.annotations,
                        valueParameter.name,
                        substitutor.safeSubstitute(valueParameter.type, Variance.INVARIANT),
                        valueParameter.declaresDefaultValue(),
                        valueParameter.isCrossinline,
                        valueParameter.isNoinline,
                        valueParameter.varargElementType?.let {
                            substitutor.safeSubstitute(it, Variance.INVARIANT)
                        },
                        valueParameter.source
                    )
                } + ValueParameterDescriptorImpl(
                this,
                null,
                function.valueParameters.size,
                Annotations.create(
                    listOf(
                        AnnotationDescriptorImpl(
                            function.module.findClassAcrossModuleDependencies(
                                ClassId.topLevel(InjektFqNames.Given)
                            )!!.defaultType,
                            emptyMap(),
                            SourceElement.NO_SOURCE
                        )
                    )
                ),
                "evidence".asNameId(),
                substitutor.safeSubstitute(function.dispatchReceiverParameter!!.type, Variance.INVARIANT),
                false,
                false,
                false,
                null,
                function.source
            ),
            function.returnType?.let {
                substitutor.safeSubstitute(it, Variance.INVARIANT)
            },
            Modality.FINAL,
            typeClass.visibility
        )
        isSuspend = function.isSuspend
        isOperator = function.isOperator
        isInfix = function.isInfix
        isTailrec = function.isTailrec
    }
}
