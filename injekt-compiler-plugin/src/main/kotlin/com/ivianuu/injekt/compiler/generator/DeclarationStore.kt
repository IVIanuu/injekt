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
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.findClassifierAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.ClassId
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

    fun addInternalIndex(index: Index) {
        internalIndices += index
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

    private val typeAliasIndices by unsafeLazy {
        allIndices
            .filter { it.type == "typealias" }
            .map { typeAliasDescriptorForFqName(it.fqName) }
    }

    private val effectCallables: List<Callable> by unsafeLazy {
        val generatedBindings = mutableListOf<Callable>()

        var newBindings = (classIndices
            .mapNotNull { it.getInjectConstructor() }
            .map { callableForDescriptor(it) } +
                functionIndices
                    .map { callableForDescriptor(it) } +
                propertyIndices
                    .map { callableForDescriptor(it.getter!!) } +
                implBindings
                    .map { it.callable } + allFunBindings
            .map { it.callable })
            .distinct()
            .filter { it.effects.isNotEmpty() } + typeAliasIndices
            .filter { it.hasAnnotatedAnnotations(InjektFqNames.Effect) }
            .flatMap { typeAlias ->
                val type = typeTranslator.toClassifierRef(typeAlias)
                typeAlias.getAnnotatedAnnotations(InjektFqNames.Effect)
                    .flatMap {
                        effectCallablesForAnnotation(
                            it,
                            typeAlias,
                            type.defaultType
                        )
                    }
            }

        while (newBindings.isNotEmpty()) {
            val currentBindings = newBindings.toList()
            generatedBindings += currentBindings
            newBindings = currentBindings
                .flatMap { binding ->
                    val finalBinding = binding.newEffect(this, ++effect)
                    if (finalBinding.isFunBinding) {
                        effectFunBindings[finalBinding.effectType] =
                            allFunBindings
                                .single { it.callable == binding }
                    } else {
                        effectBindings[finalBinding.effectType] = finalBinding
                    }
                    finalBinding.effects
                }
        }

        generatedBindings.toList()
    }

    private val effectBindings = mutableMapOf<TypeRef, Callable>()
    fun effectBindingsFor(type: TypeRef) = listOfNotNull(effectBindings[type])

    private val effectFunBindings = mutableMapOf<TypeRef, FunBindingDescriptor>()
    fun effectFunBindingsFor(type: TypeRef) = listOfNotNull(effectFunBindings[type])

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
            } + implBindings.flatMap { implBinding ->
            listOf(implBinding.callable, implBinding.callable.copy(type = implBinding.superType))
        } + effectCallables
            .filter { it.contributionKind == Callable.ContributionKind.BINDING }
    }

    private val bindingsByType = mutableMapOf<TypeRef, List<Callable>>()
    fun bindingsForType(type: TypeRef): List<Callable> = bindingsByType.getOrPut(type) {
        allBindings
            .filter { type.isAssignable(it.type) }
            .distinct()
    }

    private val implBindings by unsafeLazy {
        classIndices
            .filter { it.hasAnnotation(InjektFqNames.ImplBinding) }
            .map {
                val callable = callableForDescriptor(it.getInjectConstructor()!!)
                ImplBindingDescriptor(callable, callable.type, callable.type.superTypes().first())
            }
    }

    private val generatedClassifiers = mutableMapOf<FqName, ClassifierRef>()
    fun addGeneratedClassifier(classifier: ClassifierRef) {
        generatedClassifiers[classifier.fqName] = classifier
    }
    fun generatedClassifierFor(fqName: FqName): ClassifierRef? = generatedClassifiers[fqName]

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

    private val allDecorators by unsafeLazy {
        functionIndices
            .filter { it.hasAnnotation(InjektFqNames.Decorator) }
            .map { callableForDescriptor(it) } +
                propertyIndices
                    .filter { it.hasAnnotation(InjektFqNames.Decorator) }
                    .map { callableForDescriptor(it.getter!!) } + effectCallables
            .filter { it.contributionKind == Callable.ContributionKind.DECORATOR }
    }
    private val decoratorsForType = mutableMapOf<TypeRef, List<Callable>>()
    fun decoratorsByType(providerType: TypeRef): List<Callable> = decoratorsForType.getOrPut(providerType) {
        allDecorators
            .filter { providerType.isAssignable(it.type) }
    }

    private val allMapEntries by unsafeLazy {
        functionIndices
            .filter { it.hasAnnotation(InjektFqNames.MapEntries) }
            .map { callableForDescriptor(it) } +
                propertyIndices
                    .filter { it.hasAnnotation(InjektFqNames.MapEntries) }
                    .map { callableForDescriptor(it.getter!!) } + effectCallables
            .filter { it.contributionKind == Callable.ContributionKind.MAP_ENTRIES }
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
                    .map { callableForDescriptor(it.getter!!) } + effectCallables
            .filter { it.contributionKind == Callable.ContributionKind.SET_ELEMENTS }
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
                    .map { it.defaultType }
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

    private val typeAliasDescriptorByFqName = mutableMapOf<FqName, TypeAliasDescriptor>()
    fun typeAliasDescriptorForFqName(fqName: FqName): TypeAliasDescriptor {
        return typeAliasDescriptorByFqName.getOrPut(fqName) {
            memberScopeForFqName(fqName.parent())!!.getContributedClassifier(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            ) as? TypeAliasDescriptor ?: error("Could not get for $fqName")
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

    fun typeArgsForAnnotation(annotation: AnnotationDescriptor, source: DeclarationDescriptor?): Map<Name, TypeRef> {
        return ((annotation.type.constructor.declarationDescriptor as ClassDescriptor).declaredTypeParameters)
            .zip(annotation.type.arguments).map { (param, arg) ->
                param.name to typeTranslator.toTypeRef(arg.type, source)
            }.toMap()
    }

    fun valueArgsForAnnotation(annotation: AnnotationDescriptor): Map<Name, ComponentExpression> {
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

    fun decoratorCallablesForAnnotation(
        annotation: AnnotationDescriptor,
        source: DeclarationDescriptor
    ): List<Callable> {
        val typeArgs = typeArgsForAnnotation(annotation, source)
        val valueArgs = valueArgsForAnnotation(annotation)
        val origin = (source.findPsi()?.containingFile as? KtFile)?.virtualFilePath
            ?: source.fqNameSafe
        return classDescriptorForFqName(annotation.fqName!!)
            .companionObjectDescriptor!!
            .unsubstitutedMemberScope
            .getContributedDescriptors()
            .filterIsInstance<CallableDescriptor>()
            .filter {
                it.visibility == Visibilities.PUBLIC &&
                        it.dispatchReceiverParameter?.type?.isAnyOrNullableAny() != true
            }
            .map { callableForDescriptor(it as FunctionDescriptor) }
            .map { callable ->
                val substitutionMap = callable.typeParameters
                    .filter { it.argName != null }
                    .map {
                        it.defaultType to (typeArgs[it.argName]
                            ?: error("Couldn't get type argument for ${it.argName} in $annotation"))
                    }
                    .toMap(mutableMapOf())
                substitutionMap += getSubstitutionMap(
                    substitutionMap.map {
                        it.value to it.key
                    }
                )

                val callableValueArgs = callable.valueParameters
                    .filter { it.argName != null }
                    .map { parameter ->
                        val arg = valueArgs[parameter.argName]
                        parameter.name to when {
                            arg != null -> arg
                            parameter.type.isMarkedNullable -> { { emit("null") } }
                            else -> error("No argument provided for non null binding arg ${parameter.name} in $origin")
                        }
                    }
                    .toMap()

                callable.substitute(substitutionMap).copy(valueArgs = callableValueArgs)
            }
    }

    private var effect = 0
    fun effectCallablesForAnnotation(
        annotation: AnnotationDescriptor,
        source: DeclarationDescriptor,
        bindingType: TypeRef
    ): List<Callable> {
        val origin = (source.findPsi()?.containingFile as? KtFile)?.virtualFilePath
            ?: source.fqNameSafe
        val typeArgs = typeArgsForAnnotation(annotation, source)
        val valueArgs = valueArgsForAnnotation(annotation)
        return classDescriptorForFqName(annotation.fqName!!)
            .companionObjectDescriptor!!
            .unsubstitutedMemberScope
            .getContributedDescriptors()
            .filterIsInstance<CallableDescriptor>()
            .map {
                callableForDescriptor(
                    when (it) {
                        is ClassDescriptor -> it.getInjectConstructor()!!
                        is PropertyDescriptor -> it.getter!!
                        else -> it as FunctionDescriptor
                    }
                )
            }
            .filter {
                it.contributionKind != null ||
                        it.effects.isNotEmpty() ||
                        it.decorators.isNotEmpty()
            }
            .map { effectCallable ->
                val substitutionMap = effectCallable.typeParameters
                    .filter { it.argName != null }
                    .map {
                        it.defaultType to (typeArgs[it.argName]
                            ?: error("Couldn't get type argument for ${it.argName} in $origin"))
                    }
                    .toMap(mutableMapOf())

                // todo there might be a better way to resolve everything
                val subjectTypeParameter = effectCallable.typeParameters
                    .first { it.argName == null }
                    .defaultType
                substitutionMap += getSubstitutionMap(
                    listOf(bindingType to subjectTypeParameter) +
                            substitutionMap
                                .map { it.value to it.key }
                )

                check(effectCallable.typeParameters.all { it.defaultType in substitutionMap }) {
                    "Couldn't resolve all type arguments ${substitutionMap.map {
                        it.key.classifier.fqName to it.value
                    }} missing ${effectCallable.typeParameters.filter {
                        it.defaultType !in substitutionMap
                    }.map { it.fqName }} in $origin"
                }

                val callableValueArgs = effectCallable.valueParameters
                    .filter { it.argName != null }
                    .map { parameter ->
                        val arg = valueArgs[parameter.argName]
                        parameter.name to when {
                            arg != null -> arg
                            parameter.type.isMarkedNullable -> { { emit("null") } }
                            else -> error("No argument provided for non null binding arg ${parameter.name} in $origin")
                        }
                    }
                    .toMap()

                effectCallable.substitute(substitutionMap)
                    .copy(
                        valueArgs = callableValueArgs,
                        typeArgs = substitutionMap
                            .mapKeys { it.key.classifier }
                    )
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

        // use the fun binding type if possible
        val effectType = (if (descriptor.hasAnnotation(InjektFqNames.FunBinding)) {
            (generatedClassifiers[owner.fqNameSafe] ?:
            typeTranslator.toClassifierRef(
                module.findClassifierAcrossModuleDependencies(
                    ClassId.topLevel(owner.fqNameSafe)
                )!!
            )).defaultType
        } else type)

        val callableTargetComponent = (owner.annotations.findAnnotation(InjektFqNames.Binding) ?:
        owner.annotations.findAnnotation(InjektFqNames.ImplBinding) ?:
        owner.annotations.findAnnotation(InjektFqNames.FunBinding) ?:
        owner.annotations.findAnnotation(InjektFqNames.MapEntries) ?:
        owner.annotations.findAnnotation(InjektFqNames.SetElements) ?:
        owner.annotations.findAnnotation(InjektFqNames.Decorator) ?:
        descriptor.containingDeclaration.containingDeclaration?.annotations
            ?.findAnnotation(InjektFqNames.Decorator))
            ?.allValueArguments
            ?.let {
                it["scopeComponent".asNameId()]
                    ?: it["targetComponent".asNameId()]
            }
            ?.let { it as KClassValue }
            ?.getArgumentType(module)
            ?.let { typeTranslator.toTypeRef(it, descriptor, Variance.INVARIANT) }

        val decorators = (descriptor
            .getAnnotatedAnnotations(InjektFqNames.Decorator) + owner
            .getAnnotatedAnnotations(InjektFqNames.Decorator))
            .distinct()
            .flatMap { decoratorCallablesForAnnotation(it, descriptor) }
            .distinct()

        val decoratorsByTargetComponent = decorators
            .filter { it.targetComponent != null }
            .map { it.targetComponent to it }
            .groupBy { it.first }
        if (decoratorsByTargetComponent.size > 1) {
            error("Decorators target component mismatch. Decorators of '${descriptor.fqNameSafe}' have different target components\n" +
                    decorators.joinToString("\n") { decorator ->
                        "'${decorator.fqName}' = '${decorator.targetComponent?.render()}'"
                    }
            )
        }
        val decoratorTargetComponent = decorators
            .mapNotNull { it.targetComponent }
            .firstOrNull()

        if (callableTargetComponent != null &&
            decoratorTargetComponent != null &&
            callableTargetComponent != decoratorTargetComponent) {
            error("Target component mismatch. '${descriptor.fqNameSafe}' target component is '${callableTargetComponent.render()}' but " +
                    "decorator target component is '${decoratorTargetComponent.render()}'.")
        }

        val targetComponent = callableTargetComponent ?: decoratorTargetComponent

        Callable(
            name = owner.name,
            packageFqName = descriptor.findPackage().fqName,
            fqName = owner.fqNameSafe,
            type = type,
            targetComponent = targetComponent,
            scoped = callableTargetComponent != null,
            contributionKind = when {
                owner.hasAnnotationWithPropertyAndClass(InjektFqNames.Binding) -> Callable.ContributionKind.BINDING
                owner.hasAnnotationWithPropertyAndClass(InjektFqNames.Decorator) -> Callable.ContributionKind.DECORATOR
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
                    val parameterType = it.type.let { typeTranslator.toTypeRef(it, descriptor, Variance.INVARIANT) }
                    ValueParameterRef(
                        type = parameterType,
                        originalType = parameterType,
                        isExtensionReceiver = true,
                        name = "_receiver".asNameId(),
                        inlineKind = ValueParameterRef.InlineKind.NONE,
                        argName = it.getArgName(),
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
                    isExtensionReceiver = false,
                    name = it.name,
                    inlineKind = when {
                        it.isNoinline -> ValueParameterRef.InlineKind.NOINLINE
                        it.isCrossinline -> ValueParameterRef.InlineKind.CROSSINLINE
                        else -> ValueParameterRef.InlineKind.NONE
                    },
                    argName = it.getArgName(),
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
            decorators = decorators,
            effects = (descriptor
                .getAnnotatedAnnotations(InjektFqNames.Effect) + owner
                .getAnnotatedAnnotations(InjektFqNames.Effect))
                .distinct()
                .flatMap { effectCallablesForAnnotation(it, descriptor, effectType) }
                .distinct(),
            effectType = effectType,
            isExternal = owner is DeserializedDescriptor,
            isInline = descriptor.isInline,
            visibility = descriptor.visibility,
            modality = descriptor.modality,
            receiver = descriptor.dispatchReceiverParameter?.type?.constructor?.declarationDescriptor
                ?.let { typeTranslator.toClassifierRef(it) },
            isFunBinding = descriptor.hasAnnotation(InjektFqNames.FunBinding),
            valueArgs = emptyMap(),
            typeArgs = emptyMap()
        )
    }

    private val moduleByType = mutableMapOf<TypeRef, com.ivianuu.injekt.compiler.generator.ModuleDescriptor>()
    fun moduleForType(type: TypeRef): com.ivianuu.injekt.compiler.generator.ModuleDescriptor {
        return moduleByType.getOrPut(type) {
            val descriptor = classDescriptorForFqName(type.classifier.fqName)
            val moduleSubstitutionMap = type.classifier.typeParameters
                .map { it.defaultType }
                .zip(type.typeArguments)
                .toMap()
            ModuleDescriptor(
                type = type,
                callables = descriptor.unsubstitutedMemberScope.getContributedDescriptors().filter {
                    it.hasAnnotationWithPropertyAndClass(InjektFqNames.Binding) ||
                            it.hasAnnotationWithPropertyAndClass(InjektFqNames.Decorator) ||
                            it.hasAnnotatedAnnotationsWithPropertyAndClass(InjektFqNames.Decorator) ||
                            it.hasAnnotationWithPropertyAndClass(InjektFqNames.Effect) ||
                            it.hasAnnotatedAnnotationsWithPropertyAndClass(InjektFqNames.Effect) ||
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

                        // todo tmp workaround for composables
                        if ((descriptor.containingDeclaration as? ClassDescriptor)
                                ?.hasAnnotation(InjektFqNames.Effect) == true) {
                            substitutionMap += callable.typeParameters
                                .map { it.defaultType }
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
