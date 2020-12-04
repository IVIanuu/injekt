/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.generator.componentimpl.ComponentExpression
import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeserializedDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.findClassifierAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.BooleanValue
import org.jetbrains.kotlin.resolve.constants.ByteValue
import org.jetbrains.kotlin.resolve.constants.CharValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.DoubleValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.FloatValue
import org.jetbrains.kotlin.resolve.constants.IntValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.constants.LongValue
import org.jetbrains.kotlin.resolve.constants.ShortValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.constants.UByteValue
import org.jetbrains.kotlin.resolve.constants.UIntValue
import org.jetbrains.kotlin.resolve.constants.ULongValue
import org.jetbrains.kotlin.resolve.constants.UShortValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny

@Binding(GenerationComponent::class)
class DeclarationStore(val module: ModuleDescriptor) {

    lateinit var typeTranslator: TypeTranslator

    private val internalIndices = mutableListOf<Index>()

    fun addInternalIndex(index: Index) {
        internalIndices += index
    }

    fun constructorForComponent(type: TypeRef): Callable? {
        return classDescriptorForFqName(type.classifier.fqName)
            .unsubstitutedPrimaryConstructor
            ?.let { callableForDescriptor(it) }
    }

    private val allIndices by unsafeLazy {
        internalIndices + externalIndices
    }

    private val externalIndices by unsafeLazy {
        memberScopeForFqName(InjektFqNames.IndexPackage)
            ?.getContributedDescriptors(DescriptorKindFilter.VALUES)
            ?.filterIsInstance<PropertyDescriptor>()
            ?.map { indexProperty ->
                val annotation = indexProperty.annotations.findAnnotation(InjektFqNames.Index)!!
                val fqName = annotation.allValueArguments["fqName".asNameId()]!!.value as String
                val type = annotation.allValueArguments["type".asNameId()]!!.value as String
                Index(FqName(fqName), type)
            } ?: emptyList()
    }

    private val classIndices by unsafeLazy {
        allIndices
            .filter { it.type == "class" }
            .map { classDescriptorForFqName(it.fqName) }
    }

    private val functionIndices by unsafeLazy {
        allIndices
            .filter { it.type == "function" }
            .flatMap { functionDescriptorForFqName(it.fqName) }
    }

    private val propertyIndices by unsafeLazy {
        allIndices
            .filter { it.type == "property" }
            .flatMap { propertyDescriptorsForFqName(it.fqName) }
    }

    private val allBindings by unsafeLazy {
        (classIndices
            .mapNotNull { it.getInjectConstructor() }
            .map { callableForDescriptor(it) } +
                functionIndices
                    .map { callableForDescriptor(it) } +
                propertyIndices
                    .map { callableForDescriptor(it.getter!!) })
            .filter {
                it.contributionKind == Callable.ContributionKind.BINDING ||
                        it.contributionKind == Callable.ContributionKind.MODULE
            }
    }

    private val bindingsByType = mutableMapOf<TypeRef, List<Callable>>()
    fun bindingsForType(type: TypeRef): List<Callable> = bindingsByType.getOrPut(type) {
        allBindings
            .filter { type.isAssignable(it.type) }
            .distinct()
    }

    private val generatedClassifiers = mutableMapOf<FqName, ClassifierRef>()
    fun addGeneratedClassifier(classifier: ClassifierRef) {
        generatedClassifiers[classifier.fqName] = classifier
    }
    fun generatedClassifierFor(fqName: FqName): ClassifierRef? = generatedClassifiers[fqName]

    val allModules: List<Callable> by unsafeLazy {
        classIndices
            .filter { it.hasAnnotation(InjektFqNames.Module) }
            .map { it.getInjectConstructor()!! }
            .map { callableForDescriptor(it) } +
                functionIndices
                    .filter { it.hasAnnotation(InjektFqNames.Module) }
                    .map { callableForDescriptor(it) } +
                propertyIndices
                    .filter { it.hasAnnotation(InjektFqNames.Module) }
                    .map { callableForDescriptor(it.getter!!) }
    }

    private val allFunBindings by unsafeLazy {
        functionIndices
            .filter { it.hasAnnotation(InjektFqNames.FunBinding) }
            .map {
                val callable = callableForDescriptor(it)
                val type = (generatedClassifiers[callable.fqName] ?:typeTranslator.toClassifierRef(
                    module.findClassifierAcrossModuleDependencies(
                        ClassId.topLevel(callable.fqName)
                    )!!
                )).defaultType
                FunBindingDescriptor(
                    callable,
                    type,
                    type
                )
            }
    }
    private val funBindingsByType = mutableMapOf<TypeRef, List<FunBindingDescriptor>>()
    fun funBindingsForType(type: TypeRef): List<FunBindingDescriptor> = funBindingsByType.getOrPut(type) {
        allFunBindings
            .filter { type.isAssignable(it.type) }
    }

    private val allInterceptors by unsafeLazy {
        functionIndices
            .filter { it.hasAnnotation(InjektFqNames.Interceptor) }
            .map { callableForDescriptor(it) } +
                propertyIndices
                    .filter { it.hasAnnotation(InjektFqNames.Interceptor) }
                    .map { callableForDescriptor(it.getter!!) }
    }
    private val interceptorsForType = mutableMapOf<TypeRef, List<Callable>>()
    fun interceptorsByType(providerType: TypeRef): List<Callable> = interceptorsForType.getOrPut(providerType) {
        allInterceptors
            .filter { providerType.isAssignable(it.type) }
    }

    private val allMapEntries by unsafeLazy {
        functionIndices
            .filter { it.hasAnnotation(InjektFqNames.MapEntries) }
            .map { callableForDescriptor(it) } +
                propertyIndices
                    .filter { it.hasAnnotation(InjektFqNames.MapEntries) }
                    .map { callableForDescriptor(it.getter!!) }
    }
    private val mapEntriesForType = mutableMapOf<TypeRef, List<Callable>>()
    fun mapEntriesByType(type: TypeRef): List<Callable> = mapEntriesForType.getOrPut(type) {
        allMapEntries
            .filter { type.isAssignable(it.type) }
    }

    private val allSetElements by unsafeLazy {
        functionIndices
            .filter { it.hasAnnotation(InjektFqNames.SetElements) }
            .map { callableForDescriptor(it) } +
                propertyIndices
                    .filter { it.hasAnnotation(InjektFqNames.SetElements) }
                    .map { callableForDescriptor(it.getter!!) }
    }
    private val setElementsForType = mutableMapOf<TypeRef, List<Callable>>()
    fun setElementsByType(type: TypeRef): List<Callable> = setElementsForType.getOrPut(type) {
        allSetElements
            .filter { type.isAssignable(it.type) }
    }

    val mergeComponents: List<TypeRef> by unsafeLazy {
        classIndices
            .filter { it.hasAnnotation(InjektFqNames.MergeComponent) }
            .map { it.defaultType.let { typeTranslator.toTypeRef2(it) } }
    }

    private val allMergeDeclarationsByFqName by unsafeLazy {
        buildMap<FqName, MutableList<TypeRef>> {
            classIndices
                .filter { it.hasAnnotation(InjektFqNames.MergeInto) }
                .groupBy { declaration ->
                    declaration.annotations.findAnnotation(InjektFqNames.MergeInto)!!
                        .allValueArguments["component".asNameId()]!!
                        .let { it as KClassValue }
                        .getArgumentType(module)
                        .constructor
                        .declarationDescriptor!!
                        .fqNameSafe
                }
                .forEach { (mergeComponent, declarations) ->
                    getOrPut(mergeComponent) { mutableListOf() } += declarations.map {
                        it.defaultType.let {
                            typeTranslator.toTypeRef2(it)
                        }
                    }
                }
        }
    }

    fun mergeDeclarationsForMergeComponent(component: FqName): List<TypeRef> =
        allMergeDeclarationsByFqName[component] ?: emptyList()

    private val callablesByType = mutableMapOf<TypeRef, List<Callable>>()
    fun allCallablesForType(type: TypeRef): List<Callable> {
        return callablesByType.getOrPut(type) {
            val callables = mutableListOf<Callable>()

            fun TypeRef.collect(typeArguments: List<TypeRef>) {
                val substitutionMap = classifier.typeParameters
                    .zip(typeArguments)
                    .toMap()

                callables += classDescriptorForFqName(classifier.fqName)
                    .unsubstitutedMemberScope
                    .getContributedDescriptors(DescriptorKindFilter.CALLABLES)
                    .filterIsInstance<CallableDescriptor>()
                    .filter { it.dispatchReceiverParameter?.type?.isAnyOrNullableAny() != true }
                    .mapNotNull {
                        when (it) {
                            is FunctionDescriptor -> callableForDescriptor(it)
                            is PropertyDescriptor -> callableForDescriptor(it.getter!!)
                            else -> null
                        }
                    }
                    .map { it.substitute(substitutionMap) }

                classifier.superTypes
                    .map { it.substitute(substitutionMap) }
                    .forEach { it.collect(it.typeArguments) }
            }

            type.collect(type.typeArguments)

            callables
        }
    }

    private val classDescriptorByFqName = mutableMapOf<FqName, ClassDescriptor>()
    fun classDescriptorForFqName(fqName: FqName): ClassDescriptor {
        return classDescriptorByFqName.getOrPut(fqName) {
            memberScopeForFqName(fqName.parent())!!.getContributedClassifier(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            ) as? ClassDescriptor ?: error("Could not get for $fqName")
        }
    }

    private val functionDescriptorsByFqName = mutableMapOf<FqName, List<FunctionDescriptor>>()
    fun functionDescriptorForFqName(fqName: FqName): List<FunctionDescriptor> {
        return functionDescriptorsByFqName.getOrPut(fqName) {
            memberScopeForFqName(fqName.parent())!!.getContributedFunctions(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            ).toList()
        }
    }

    private val propertyDescriptorsByFqName = mutableMapOf<FqName, List<PropertyDescriptor>>()
    fun propertyDescriptorsForFqName(fqName: FqName): List<PropertyDescriptor> {
        return propertyDescriptorsByFqName.getOrPut(fqName) {
            memberScopeForFqName(fqName.parent())!!.getContributedVariables(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            ).toList()
        }
    }

    private val memberScopeByFqName = mutableMapOf<FqName, MemberScope?>()
    fun memberScopeForFqName(fqName: FqName): MemberScope? {
        return memberScopeByFqName.getOrPut(fqName) {
            val pkg = module.getPackage(fqName)

            if (fqName.isRoot || pkg.fragments.isNotEmpty()) return@getOrPut pkg.memberScope

            val parentMemberScope = memberScopeForFqName(fqName.parent()) ?: return@getOrPut null

            val classDescriptor =
                parentMemberScope.getContributedClassifier(
                    fqName.shortName(),
                    NoLookupLocation.FROM_BACKEND
                ) as? ClassDescriptor ?: return@getOrPut null

            classDescriptor.unsubstitutedMemberScope
        }
    }

    private fun valueArgsForAnnotation(annotation: AnnotationDescriptor): Map<Name, ComponentExpression> {
        return annotation.allValueArguments.mapValues { (_, arg) ->
            {
                fun ConstantValue<*>.emit() {
                    when (this) {
                        is ArrayValue -> {
                            // todo avoid boxing
                            emit("arrayOf(")
                            value.forEachIndexed { index, itemValue ->
                                itemValue.emit()
                                if (index != value.lastIndex) emit(", ")
                            }
                            emit(")")
                        }
                        is BooleanValue -> emit(value)
                        is ByteValue -> emit("$value")
                        is CharValue -> emit("'${value}'")
                        is DoubleValue -> emit("$value")
                        is EnumValue -> emit("${enumClassId.asSingleFqName()}.${enumEntryName}")
                        is FloatValue -> emit("${value}f")
                        is IntValue -> emit("$value")
                        is KClassValue -> emit("${(value as KClassValue.Value.NormalClass).classId.asSingleFqName()}::class")
                        is LongValue -> emit("${value}L")
                        is ShortValue -> emit("$value")
                        is StringValue -> emit("\"${value}\"")
                        is UByteValue -> emit("${value}u")
                        is UIntValue -> emit("${value}u")
                        is ULongValue -> emit("(${value}UL)")
                        is UShortValue -> emit("${value}u")
                        else -> error("Unsupported bindingArg type $value")
                    }.let {}
                }

                arg.emit()
            }
        }
    }

    fun qualifierDescriptorForAnnotation(
        annotation: AnnotationDescriptor,
        source: DeclarationDescriptor?
    ): QualifierDescriptor {
        return QualifierDescriptor(
            type = typeTranslator.toTypeRef(annotation.type, source),
            args = valueArgsForAnnotation(annotation)
                .mapValues { buildCodeString { it.value(this) } }
        )
    }

    private val callablesByDescriptor = mutableMapOf<Any, Callable>()
    fun callableForDescriptor(descriptor: FunctionDescriptor): Callable = callablesByDescriptor.getOrPut(descriptor) {
        val owner = when (descriptor) {
            is ConstructorDescriptor -> descriptor.constructedClass
            is PropertyAccessorDescriptor -> descriptor.correspondingProperty
            else -> descriptor
        }

        val type = descriptor.returnType!!.let { typeTranslator.toTypeRef(it, descriptor, Variance.INVARIANT) }

        val funApiParams = if (descriptor.hasAnnotation(InjektFqNames.FunBinding)) {
            val classifier = (generatedClassifiers[owner.fqNameSafe] ?:
            typeTranslator.toClassifierRef(
                module.findClassifierAcrossModuleDependencies(
                    ClassId.topLevel(owner.fqNameSafe)
                )!!
            ))
            classifier.funApiParams
        } else emptyList()

        Callable(
            name = owner.name,
            packageFqName = descriptor.findPackage().fqName,
            fqName = owner.fqNameSafe,
            type = type,
            targetComponent = descriptor.targetComponent(module, typeTranslator)
                ?: owner.targetComponent(module, typeTranslator),
            scoped = descriptor.hasAnnotationWithPropertyAndClass(InjektFqNames.Scoped),
            eager = descriptor.hasAnnotationWithPropertyAndClass(InjektFqNames.Eager),
            default = descriptor.hasAnnotationWithPropertyAndClass(InjektFqNames.Default),
            contributionKind = descriptor.contributionKind(),
            typeParameters = (when (owner) {
                is FunctionDescriptor -> owner.typeParameters
                is ClassDescriptor -> owner.declaredTypeParameters
                is PropertyDescriptor -> owner.typeParameters
                else -> error("Unexpected owner $owner")
            }).map {
                typeTranslator.toClassifierRef(it)
            },
            valueParameters = listOfNotNull(
                descriptor.dispatchReceiverParameter?.let {
                    val parameterType = it.type.let { typeTranslator.toTypeRef(it, descriptor, Variance.INVARIANT) }
                    ValueParameterRef(
                        type = parameterType,
                        originalType = parameterType,
                        parameterKind = ValueParameterRef.ParameterKind.DISPATCH_RECEIVER,
                        name = "_dispatchReceiver".asNameId(),
                        isFunApi = false,
                        hasDefault = false,
                        defaultExpression = null
                    )
                },
                descriptor.extensionReceiverParameter?.let {
                    val parameterType = it.type.let { typeTranslator.toTypeRef(it, descriptor, Variance.INVARIANT) }
                    ValueParameterRef(
                        type = parameterType,
                        originalType = parameterType,
                        parameterKind = ValueParameterRef.ParameterKind.EXTENSION_RECEIVER,
                        name = "_extensionReceiver".asNameId(),
                        isFunApi = "<this>" in funApiParams.map { it.asString() },
                        hasDefault = false,
                        defaultExpression = null
                    )
                }
            ) + descriptor.valueParameters.map {
                val parameterType = it.type.let { typeTranslator.toTypeRef(it, descriptor, Variance.INVARIANT) }
                ValueParameterRef(
                    type = parameterType,
                    originalType = parameterType,
                    parameterKind = ValueParameterRef.ParameterKind.VALUE_PARAMETER,
                    name = it.name,
                    isFunApi = it.name in funApiParams,
                    hasDefault = it.declaresDefaultValue(),
                    defaultExpression = if (!it.declaresDefaultValue()) null else ({
                        emit((it.findPsi() as KtParameter).defaultValue!!.text)
                    })
                )
            },
            isCall = owner !is PropertyDescriptor &&
                    (owner !is ClassDescriptor || owner.kind != ClassKind.OBJECT),
            callableKind = if (owner is CallableDescriptor) {
                when {
                    owner.isSuspend -> Callable.CallableKind.SUSPEND
                    owner.hasAnnotation(InjektFqNames.Composable) -> Callable.CallableKind.COMPOSABLE
                    else -> Callable.CallableKind.DEFAULT
                }
            } else Callable.CallableKind.DEFAULT,
            isExternal = owner is DeserializedDescriptor,
            isInline = descriptor.isInline,
            visibility = descriptor.visibility,
            modality = descriptor.modality,
            isFunBinding = descriptor.hasAnnotation(InjektFqNames.FunBinding)
        )
    }

    private val moduleByType = mutableMapOf<TypeRef, com.ivianuu.injekt.compiler.generator.ModuleDescriptor>()
    fun moduleForType(type: TypeRef): com.ivianuu.injekt.compiler.generator.ModuleDescriptor {
        val finalType = type.fullyExpandedType
        return moduleByType.getOrPut(finalType) {
            val descriptor = classDescriptorForFqName(finalType.classifier.fqName)
            val moduleSubstitutionMap = finalType.classifier.typeParameters
                .zip(finalType.typeArguments)
                .toMap()

            val callables = if (finalType.contributionKind != null && (finalType.isFunction || finalType.isSuspendFunction)) {
                val invokeDescriptor = descriptor.unsubstitutedMemberScope.getContributedDescriptors()
                    .first { it.name.asString() == "invoke" } as FunctionDescriptor
                val callable = callableForDescriptor(invokeDescriptor)
                val substitutionMap = moduleSubstitutionMap.toMutableMap()
                val finalCallable = callable.copy(
                    type = callable.type.substitute(substitutionMap),
                    valueParameters = callable.valueParameters.map {
                        val parameterType = if (it.parameterKind ==
                            ValueParameterRef.ParameterKind.DISPATCH_RECEIVER) {
                            finalType
                        } else it.type
                        it.copy(
                            type = parameterType.substitute(substitutionMap)
                        )
                    },
                    targetComponent = finalType.targetComponent,
                    scoped = finalType.scoped,
                    eager = finalType.eager,
                    default = finalType.default,
                    contributionKind = finalType.contributionKind
                )
                listOf(finalCallable)
            } else {
                descriptor.unsubstitutedMemberScope.getContributedDescriptors().filter {
                    it.hasAnnotationWithPropertyAndClass(InjektFqNames.Binding) ||
                            it.hasAnnotationWithPropertyAndClass(InjektFqNames.Interceptor) ||
                            it.hasAnnotationWithPropertyAndClass(InjektFqNames.SetElements) ||
                            it.hasAnnotationWithPropertyAndClass(InjektFqNames.MapEntries) ||
                            it.hasAnnotationWithPropertyAndClass(InjektFqNames.Module)
                }
                    .mapNotNull {
                        when (it) {
                            is PropertyDescriptor -> it.getter!!
                            is FunctionDescriptor -> it
                            else -> null
                        }
                    }
                    .map { callableDescriptor ->
                        val callable = callableForDescriptor(callableDescriptor)
                        val substitutionMap = moduleSubstitutionMap.toMutableMap()
                        callable.copy(
                            type = callable.type.substitute(substitutionMap),
                            valueParameters = callable.valueParameters.map {
                                val parameterType = if (it.parameterKind ==
                                    ValueParameterRef.ParameterKind.DISPATCH_RECEIVER) {
                                    finalType
                                } else it.type
                                it.copy(
                                    type = parameterType.substitute(substitutionMap)
                                )
                            }
                        )
                    }
            }

            ModuleDescriptor(type = finalType, callables = callables)
        }
    }

}
