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
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
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
class DeclarationStore(private val module: ModuleDescriptor) {

    lateinit var typeTranslator: TypeTranslator

    private val internalIndices = mutableListOf<Index>()
    val internalGeneratedIndices: Map<KtFile, List<Index>> get() = _internalGeneratedIndices
    private val _internalGeneratedIndices = mutableMapOf<KtFile, MutableList<Index>>()

    fun addInternalIndex(index: Index) {
        internalIndices += index
    }

    fun addGeneratedInternalIndex(file: KtFile, index: Index) {
        _internalGeneratedIndices.getOrPut(file) { mutableListOf() } += index
    }

    fun constructorForComponent(type: TypeRef): Callable? {
        return classDescriptorForFqName(type.classifier.fqName)
            .unsubstitutedPrimaryConstructor
            ?.let { callableForDescriptor(it) }
    }

    private val allIndices by unsafeLazy {
        internalIndices + (memberScopeForFqName(InjektFqNames.IndexPackage)
            ?.getContributedDescriptors(DescriptorKindFilter.VALUES)
            ?.filterIsInstance<PropertyDescriptor>()
            ?.map {
                val annotation = it.annotations.findAnnotation(InjektFqNames.Index)!!
                val fqName = annotation.allValueArguments["fqName".asNameId()]!!.value as String
                val type = annotation.allValueArguments["type".asNameId()]!!.value as String
                Index(FqName(fqName), type)
            } ?: emptyList())
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
                        (it.contributionKind != Callable.ContributionKind.DECORATOR &&
                                it.decorators.isNotEmpty())
            }
    }

    private val bindingsByType = mutableMapOf<TypeRef, List<Callable>>()
    fun bindingsForType(type: TypeRef): List<Callable> = bindingsByType.getOrPut(type) {
        (allBindings + generatedCallables
            .filter {
                it.first.contributionKind == Callable.ContributionKind.BINDING ||
                        (it.first.contributionKind != Callable.ContributionKind.DECORATOR &&
                                it.first.decorators.isNotEmpty())
            }
            .map { it.first })
            .filter { type.isAssignable(it.type) }
            .distinct()
    }

    val generatedCallables = mutableListOf<Pair<Callable, KtFile>>()
    fun addGeneratedCallable(callable: Callable, file: KtFile) {
        generatedCallables += callable to file
    }

    private val generatedClassifiers = mutableMapOf<FqName, ClassifierRef>()
    fun addGeneratedClassifier(classifier: ClassifierRef) {
        generatedClassifiers[classifier.fqName] = classifier
    }
    fun generatedClassifierFor(fqName: FqName): ClassifierRef? = generatedClassifiers[fqName]

    private val allDecorators by unsafeLazy {
        functionIndices
            .filter { it.hasAnnotation(InjektFqNames.Decorator) }
            .map { callableForDescriptor(it) } +
                propertyIndices
                    .filter { it.hasAnnotation(InjektFqNames.Decorator) }
                    .map { callableForDescriptor(it.getter!!) } + (generatedCallables
            .filter { it.first.contributionKind == Callable.ContributionKind.DECORATOR }
            .map { it.first })
    }
    private val decoratorsForType = mutableMapOf<TypeRef, List<Callable>>()
    fun decoratorsByType(type: TypeRef, callableKind: Callable.CallableKind): List<Callable> = decoratorsForType.getOrPut(type) {
        val providerType = when (callableKind) {
            Callable.CallableKind.DEFAULT -> typeTranslator.toClassifierRef(
                module.builtIns.getFunction(0)
            ).defaultType.typeWith(listOf(type))
            Callable.CallableKind.SUSPEND -> typeTranslator.toClassifierRef(
                module.builtIns.getSuspendFunction(0)
            ).defaultType.typeWith(listOf(type))
            Callable.CallableKind.COMPOSABLE -> typeTranslator.toClassifierRef(
                module.builtIns.getFunction(0)
            ).defaultType.typeWith(listOf(type)).copy(isComposable = true)
        }
        return allDecorators
            .filter { providerType.isAssignable(it.type) }
    }

    private val allMapEntries by unsafeLazy {
        functionIndices
            .filter { it.hasAnnotation(InjektFqNames.MapEntries) }
            .map { callableForDescriptor(it) } +
                propertyIndices
                    .filter { it.hasAnnotation(InjektFqNames.MapEntries) }
                    .map { callableForDescriptor(it.getter!!) } + (generatedCallables
            .filter { it.first.contributionKind == Callable.ContributionKind.MAP_ENTRIES }
            .map { it.first })
    }
    private val mapEntriesForType = mutableMapOf<TypeRef, List<Callable>>()
    fun mapEntriesByType(type: TypeRef): List<Callable> = mapEntriesForType.getOrPut(type) {
        return allMapEntries
            .filter { type.isAssignable(it.type) }
    }

    private val allSetElements by unsafeLazy {
        functionIndices
            .filter { it.hasAnnotation(InjektFqNames.SetElements) }
            .map { callableForDescriptor(it) } +
                propertyIndices
                    .filter { it.hasAnnotation(InjektFqNames.SetElements) }
                    .map { callableForDescriptor(it.getter!!) } + (generatedCallables
            .filter { it.first.contributionKind == Callable.ContributionKind.SET_ELEMENTS }
            .map { it.first })
    }
    private val setElementsForType = mutableMapOf<TypeRef, List<Callable>>()
    fun setElementsByType(type: TypeRef): List<Callable> = setElementsForType.getOrPut(type) {
        return allSetElements
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

                superTypes
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

            if (fqName.isRoot || pkg.fragments.isNotEmpty()) return pkg.memberScope

            val parentMemberScope = memberScopeForFqName(fqName.parent()) ?: return null

            val classDescriptor =
                parentMemberScope.getContributedClassifier(
                    fqName.shortName(),
                    NoLookupLocation.FROM_BACKEND
                ) as? ClassDescriptor ?: return null

            classDescriptor.unsubstitutedMemberScope
        }
    }

    fun argsForAnnotation(annotation: AnnotationDescriptor): Map<Name, ComponentExpression> {
        return annotation.allValueArguments.mapValues { (_, bindingArg) ->
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

                bindingArg.emit()
            }
        }
    }

    fun decoratorDescriptorForAnnotation(
        annotation: AnnotationDescriptor,
        source: DeclarationDescriptor?
    ): DecoratorDescriptor {
        return DecoratorDescriptor(
            callables = classDescriptorForFqName(annotation.fqName!!)
                .companionObjectDescriptor!!
                .unsubstitutedMemberScope
                .getContributedDescriptors()
                .filterIsInstance<CallableDescriptor>()
                .filter {
                    it.visibility == Visibilities.PUBLIC &&
                            it.dispatchReceiverParameter?.type?.isAnyOrNullableAny() != true
                }
                .map { callableForDescriptor(it as FunctionDescriptor) },
            annotationType = typeTranslator.toTypeRef(annotation.type, source),
            args = argsForAnnotation(annotation)
        )
    }

    fun effectDescriptorForAnnotation(
        annotation: AnnotationDescriptor,
        source: DeclarationDescriptor
    ): EffectDescriptor {
        return EffectDescriptor(
            type = typeTranslator.toTypeRef(annotation.type, source),
            callables = moduleForType(
                typeTranslator.toClassifierRef(
                    classDescriptorForFqName(annotation.fqName!!)
                        .companionObjectDescriptor!!
                ).defaultType
            ).callables,
            args = argsForAnnotation(annotation)
        )
    }

    fun qualifierDescriptorForAnnotation(
        annotation: AnnotationDescriptor,
        source: DeclarationDescriptor?
    ): QualifierDescriptor {
        return QualifierDescriptor(
            type = typeTranslator.toTypeRef(annotation.type, source),
            args = argsForAnnotation(annotation)
                .mapValues { buildCodeString { it.value(this) } }
        )
    }

    private val callablesByDescriptor = mutableMapOf<CallableDescriptor, Callable>()
    fun callableForDescriptor(descriptor: FunctionDescriptor): Callable = callablesByDescriptor.getOrPut(descriptor) {
        val owner = when (descriptor) {
            is ConstructorDescriptor -> descriptor.constructedClass
            is PropertyAccessorDescriptor -> descriptor.correspondingProperty
            else -> descriptor
        }

        Callable(
            name = owner.name,
            packageFqName = descriptor.findPackage().fqName,
            fqName = owner.fqNameSafe,
            type =  descriptor.returnType!!.let { typeTranslator.toTypeRef(it, descriptor, Variance.INVARIANT) },
            targetComponent = owner.annotations.findAnnotation(InjektFqNames.Binding)
                ?.allValueArguments
                ?.get("scopeComponent".asNameId())
                ?.let { it as KClassValue }
                ?.getArgumentType(module)
                ?.let { typeTranslator.toTypeRef(it, descriptor, Variance.INVARIANT) },
            contributionKind = when {
                owner.hasAnnotationWithPropertyAndClass(InjektFqNames.Binding) -> Callable.ContributionKind.BINDING
                owner.hasAnnotation(InjektFqNames.Decorator) -> Callable.ContributionKind.DECORATOR
                owner.hasAnnotationWithPropertyAndClass(InjektFqNames.MapEntries) -> Callable.ContributionKind.MAP_ENTRIES
                owner.hasAnnotationWithPropertyAndClass(InjektFqNames.SetElements) -> Callable.ContributionKind.SET_ELEMENTS
                owner.hasAnnotationWithPropertyAndClass(InjektFqNames.Module) -> Callable.ContributionKind.MODULE
                else -> null
            },
            typeParameters = (when (owner) {
                is FunctionDescriptor -> owner.typeParameters
                is ClassDescriptor -> owner.declaredTypeParameters
                is PropertyDescriptor -> owner.typeParameters
                else -> error("Unexpected owner $owner")
            }).map {
                typeTranslator.toClassifierRef(it)
            },
            valueParameters = listOfNotNull(
                descriptor.extensionReceiverParameter?.let {
                    ValueParameterRef(
                        type = it.type.let { typeTranslator.toTypeRef(it, descriptor, Variance.INVARIANT) },
                        isExtensionReceiver = true,
                        name = "_receiver".asNameId(),
                        inlineKind = ValueParameterRef.InlineKind.NONE,
                        argName = it.getArgName(),
                        hasDefault = false,
                        defaultExpression = null
                    )
                }
            ) + descriptor.valueParameters.map {
                ValueParameterRef(
                    type = it.type.let { typeTranslator.toTypeRef(it, descriptor, Variance.INVARIANT) },
                    isExtensionReceiver = false,
                    name = it.name,
                    inlineKind = when {
                        it.isNoinline -> ValueParameterRef.InlineKind.NOINLINE
                        it.isCrossinline -> ValueParameterRef.InlineKind.CROSSINLINE
                        else -> ValueParameterRef.InlineKind.NONE
                    },
                    argName = it.getArgName(),
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
            decorators = (descriptor
                .getAnnotatedAnnotations(InjektFqNames.Decorator) + owner
                .getAnnotatedAnnotations(InjektFqNames.Decorator))
                .distinct()
                .map { decoratorDescriptorForAnnotation(it, descriptor) },
            effects = (descriptor
                .getAnnotatedAnnotations(InjektFqNames.Effect) + owner
                .getAnnotatedAnnotations(InjektFqNames.Effect))
                .distinct()
                .map { effectDescriptorForAnnotation(it, descriptor) },
            isExternal = owner is DeserializedDescriptor,
            isInline = descriptor.isInline,
            isFunBinding = descriptor.hasAnnotation(InjektFqNames.FunBinding),
            visibility = descriptor.visibility,
            modality = descriptor.modality,
            receiver = descriptor.dispatchReceiverParameter?.type?.constructor?.declarationDescriptor
                ?.let { typeTranslator.toClassifierRef(it) }
        )
    }

    private val moduleByType = mutableMapOf<TypeRef, com.ivianuu.injekt.compiler.generator.ModuleDescriptor>()
    fun moduleForType(type: TypeRef): com.ivianuu.injekt.compiler.generator.ModuleDescriptor {
        return moduleByType.getOrPut(type) {
            val descriptor = classDescriptorForFqName(type.classifier.fqName)
            val moduleSubstitutionMap = type.classifier.typeParameters
                .zip(type.typeArguments)
                .toMap()
            ModuleDescriptor(
                type = type,
                callables = descriptor.unsubstitutedMemberScope.getContributedDescriptors().filter {
                    it.hasAnnotationWithPropertyAndClass(InjektFqNames.Binding) ||
                            it.hasAnnotationWithPropertyAndClass(InjektFqNames.SetElements) ||
                            it.hasAnnotationWithPropertyAndClass(InjektFqNames.MapEntries) ||
                            it.hasAnnotationWithPropertyAndClass(InjektFqNames.Module) ||
                            it.hasAnnotatedAnnotationsWithPropertyAndClass(InjektFqNames.Effect)
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

                        // todo tmp workaround for composables
                        if ((descriptor.containingDeclaration as? ClassDescriptor)
                                ?.hasAnnotation(InjektFqNames.Effect) == true) {
                            substitutionMap += callable.typeParameters
                                .zip(moduleSubstitutionMap.values)
                        }

                        callable.copy(
                            type = callable.type.substitute(substitutionMap),
                            valueParameters = callable.valueParameters.map {
                                it.copy(
                                    type = it.type.substitute(substitutionMap)
                                )
                            }
                        )
                    }
            )
        }
    }

}
