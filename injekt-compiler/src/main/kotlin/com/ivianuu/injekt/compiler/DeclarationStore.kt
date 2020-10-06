package com.ivianuu.injekt.compiler

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.impl.toKSDeclaration
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.impl.binary.KSDeclarationDescriptorImpl
import com.ivianuu.injekt.Binding
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope

@Binding(GeneratorComponent::class)
class DeclarationStore(
    private val injektTypes: InjektTypes,
    private val resolver: Resolver
) {

    private val internalIndices = mutableListOf<Index>()

    fun addInternalIndex(index: Index) {
        internalIndices += index
    }

    fun constructorForComponent(type: TypeRef): Callable? {
        return classForFqName(type.classifier.fqName)
            .primaryConstructor
            ?.let { callableForDeclaration(it) }
    }

    private val allIndices by unsafeLazy {
        internalIndices + (memberScopeForFqName(InjektTypes.IndexPackage)
            ?.getContributedDescriptors(DescriptorKindFilter.VALUES)
            ?.filterIsInstance<PropertyDescriptor>()
            ?.map { it.name }
            ?.map {
                val (fqNameWithUnderscores, type) = it.asString().split("__")
                Index(
                    fqNameWithUnderscores.replace("_", "."),
                    type
                )
            } ?: emptyList())
    }

    private val classIndices by unsafeLazy {
        allIndices
            .filter { it.type == "class" }
            .map { classForFqName(it.fqName) }
    }

    private val functionIndices by unsafeLazy {
        allIndices
            .filter { it.type == "function" }
            .flatMap { functionForFqName(it.fqName) }
    }

    private val propertyIndices by unsafeLazy {
        allIndices
            .filter { it.type == "property" }
            .flatMap { propertyForFqName(it.fqName) }
    }

    private val allBindings by unsafeLazy {
        classIndices
            .mapNotNull { it.getInjectConstructor(injektTypes) }
            .map { callableForDeclaration(it) } +
                functionIndices
                    .filter { it.hasAnnotation(injektTypes.binding) }
                    .map { callableForDeclaration(it) } +
                propertyIndices
                    .filter { it.hasAnnotation(injektTypes.binding) }
                    .map { callableForDeclaration(it.getter!!) }
    }

    private val bindingsByType = mutableMapOf<TypeRef, List<Callable>>()
    fun bindingsForType(type: TypeRef): List<Callable> = bindingsByType.getOrPut(type) {
        allBindings
            .filter { type.isAssignable(it.type) }
    }

    private val allMapEntries by unsafeLazy {
        functionIndices
                  //  .filter { it.hasAnnotation(InjektFqNames.MapEntries) }
                    .map { callableForDeclaration(it) } +
                propertyIndices
                    //.filter { it.hasAnnotation(InjektFqNames.SetElements) }
                    .map { callableForDeclaration(it.getter!!) }
    }
    private val mapEntriesForType = mutableMapOf<TypeRef, List<Callable>>()
    fun mapEntriesByType(type: TypeRef): List<Callable> = mapEntriesForType.getOrPut(type) {
        return allMapEntries
            .filter { type.isAssignable(it.type) }
    }

    private val allSetElements by unsafeLazy {
        functionIndices
          //  .filter { it.hasAnnotation(InjektFqNames.SetElements) }
            .map { callableForDeclaration(it) } +
                propertyIndices
            //        .filter { it.hasAnnotation(InjektFqNames.MapEntries) }
                    .map { callableForDeclaration(it.getter!!) }
    }
    private val setElementsForType = mutableMapOf<TypeRef, List<Callable>>()
    fun setElementsByType(type: TypeRef): List<Callable> = setElementsForType.getOrPut(type) {
        return allSetElements
            .filter { type.isAssignable(it.type) }
    }

    val mergeComponents: List<TypeRef> by unsafeLazy {
        classIndices
            .filter { it.hasAnnotation(injektTypes.mergeComponent) }
            .map { it.asType().toTypeRef(injektTypes) }
    }

    private val allMergeDeclarationsByFqName: MutableMap<String, MutableList<TypeRef>> by unsafeLazy {
        /*buildMap<String, MutableList<TypeRef>> {
            generatedMergeDeclarationsByComponent
                .forEach { (mergeComponent, declarations) ->
                    getOrPut(mergeComponent) { mutableListOf() } += declarations.map { it.type }
                }
            classIndices
                .filter { it.hasAnnotation(InjektFqNames.MergeInto) }
                .groupBy { declaration ->
                    declaration.annotations.findAnnotation(InjektFqNames.MergeInto)!!
                        .allValueArguments["component".asNameId()]!!
                        .let { it as KClassValue }
                        .getArgumentType(TODO())
                        .constructor
                        .declarationDescriptor!!
                        .fqNameSafe
                }
                .forEach { (mergeComponent, declarations) ->
                    getOrPut(mergeComponent) { mutableListOf() } += declarations.map { it.defaultType.toTypeRef() }
                }
        }*/
        mutableMapOf()
    }

    fun mergeDeclarationsForMergeComponent(component: String): List<TypeRef> =
        allMergeDeclarationsByFqName[component] ?: emptyList()

    private val generatedMergeDeclarationsByComponent = mutableMapOf<String, MutableList<com.ivianuu.injekt.compiler.ModuleDescriptor>>()
    fun addGeneratedMergeModule(
        mergeComponent: TypeRef,
        moduleDescriptor: com.ivianuu.injekt.compiler.ModuleDescriptor
    ) {
        generatedMergeDeclarationsByComponent.getOrPut(
            mergeComponent.classifier.fqName) { mutableListOf() } += moduleDescriptor
        moduleByType[moduleDescriptor.type] = moduleDescriptor
        callablesByType[moduleDescriptor.type] = moduleDescriptor.callables
    }

    private val callablesByType = mutableMapOf<TypeRef, List<Callable>>()
    fun allCallablesForType(type: TypeRef): List<Callable> {
        return callablesByType.getOrPut(type) {
            val callables = mutableListOf<Callable>()

            fun TypeRef.collect(typeArguments: List<TypeRef>) {
                val substitutionMap = classifier.typeParameters
                    .zip(typeArguments)
                    .toMap()

                callables += classForFqName(classifier.fqName)
                    .let { it.getAllFunctions() + it.getAllProperties() }
                    // todo .filter { it.dispatchReceiverParameter?.type?.isAnyOrNullableAny() != true }
                    .mapNotNull {
                        when (it) {
                            is KSFunctionDeclaration -> callableForDeclaration(it)
                            is KSPropertyDeclaration -> callableForDeclaration(it.getter!!)
                            else -> null
                        }
                    }
                    .map { callable ->
                        callable.copy(
                            type = callable.type.substitute(substitutionMap),
                            valueParameters = callable.valueParameters.map {
                                it.copy(type = it.type.substitute(substitutionMap))
                            }
                        )
                    }

                superTypes
                    .map { it.substitute(substitutionMap) }
                    .forEach { it.collect(it.typeArguments) }
            }

            type.collect(type.typeArguments)

            callables
        }
    }

    private val classesByFqName = mutableMapOf<String, KSClassDeclaration>()
    fun classForFqName(fqName: String): KSClassDeclaration {
        return classesByFqName.getOrPut(fqName) {
            memberScopeForFqName(fqName.parent())!!.getContributedClassifier(
                fqName.shortName().asNameId(), NoLookupLocation.FROM_BACKEND
            ).let { it as MemberDescriptor }!!.toKSDeclaration() as KSClassDeclaration
        }
    }

    private val functionByFqName = mutableMapOf<String, List<KSFunctionDeclaration>>()
    fun functionForFqName(fqName: String): List<KSFunctionDeclaration> {
        return functionByFqName.getOrPut(fqName) {
            memberScopeForFqName(fqName.parent())!!.getContributedFunctions(
                fqName.shortName().asNameId(), NoLookupLocation.FROM_BACKEND
            ).map { it.toKSDeclaration() as KSFunctionDeclaration }
        }
    }

    private val propertyDescriptorsByFqName = mutableMapOf<String, List<KSPropertyDeclaration>>()
    fun propertyForFqName(fqName: String): List<KSPropertyDeclaration> {
        return propertyDescriptorsByFqName.getOrPut(fqName) {
            memberScopeForFqName(fqName.parent())!!.getContributedVariables(
                fqName.shortName().asNameId(), NoLookupLocation.FROM_BACKEND
            ).map { it.toKSDeclaration() as KSPropertyDeclaration }
        }
    }

    private val memberScopeByFqName = mutableMapOf<String, MemberScope?>()
    fun memberScopeForFqName(fqName: String): MemberScope? {
        return memberScopeByFqName.getOrPut(fqName) {
            val pkg = ((resolver as com.google.devtools.ksp.processing.impl.ResolverImpl).module as ModuleDescriptor).getPackage(
                FqName(fqName))

            if (fqName.isEmpty() || pkg.fragments.isNotEmpty()) return pkg.memberScope

            val parentMemberScope = memberScopeForFqName(fqName.parent()) ?: return null

            val classDescriptor =
                parentMemberScope.getContributedClassifier(
                    fqName.shortName().asNameId(),
                    NoLookupLocation.FROM_BACKEND
                ) as? ClassDescriptor ?: return null

            classDescriptor.unsubstitutedMemberScope
        }
    }

    private val callablesByDeclaration = mutableMapOf<KSNode, Callable>()
    fun callableForDeclaration(declaration: KSNode): Callable = callablesByDeclaration.getOrPut(declaration) {
        val owner = when  {
            declaration is KSFunctionDeclaration && declaration.isConstructor ->
                declaration.parentDeclaration as KSClassDeclaration
            declaration is KSPropertyAccessor -> declaration.receiver
            else -> declaration as KSDeclaration
        }
        val extensionReceiver = when (owner) {
            is KSPropertyDeclaration -> owner.extensionReceiver?.resolve()?.toTypeRef(injektTypes)
            is KSFunctionDeclaration -> owner.extensionReceiver?.resolve()?.toTypeRef(injektTypes)
            else -> null
        }
        val parameters = (declaration as? KSFunctionDeclaration)?.parameters ?: emptyList()
        val returnType = when (declaration) {
            is KSFunctionDeclaration -> {
                if (declaration.isConstructor) (owner as KSClassDeclaration).asType().toTypeRef(injektTypes) else
                    declaration.returnType!!.resolve().toTypeRef(injektTypes)
            }
            is KSPropertyDeclaration -> declaration.type.resolve().toTypeRef(injektTypes)
            else -> error("Unexpected declaration $declaration")
        }
        val typeParameters = when (declaration) {
            is KSFunctionDeclaration -> declaration.typeParameters
            is KSPropertyDeclaration -> declaration.typeParameters
            else -> emptyList()
        }
        Callable(
            name = owner.simpleName.asString(),
            packageFqName = owner.packageName.asString(),
            fqName = owner.qualifiedName!!.asString(),
            type = returnType,
            targetComponent = /*owner.findAnnotationWithType(injektTypes.binding)
                ?.arguments
                ?.singleOrNull { it.name!!.asString() == "scopeComponent" }
                ?.value
                ?.allValueArguments
                ?.get("scopeComponent".asNameId())
                ?.let { it as KClassValue }
                ?.getArgumentType(TODO())
                ?.toTypeRef()*/ null,
            contributionKind = when {
                owner.hasAnnotationWithPropertyAndClass(injektTypes.binding) -> Callable.ContributionKind.BINDING
                owner.hasAnnotationWithPropertyAndClass(injektTypes.mapEntries) -> Callable.ContributionKind.MAP_ENTRIES
                owner.hasAnnotationWithPropertyAndClass(injektTypes.setElements) -> Callable.ContributionKind.SET_ELEMENTS
                owner.hasAnnotationWithPropertyAndClass(injektTypes.module) -> Callable.ContributionKind.MODULE
                else -> null
            },
            typeParameters = typeParameters.map { it.toClassifierRef(injektTypes) },
            valueParameters = listOfNotNull(
                extensionReceiver?.let {
                    ValueParameterRef(
                        type = it,
                        isExtensionReceiver = true,
                        isAssisted = it.isAssisted,
                        name = "receiver"
                    )
                }
            ) + parameters.map {
                val typeRef = it.type!!.resolve().toTypeRef(injektTypes)
                ValueParameterRef(
                    type = typeRef,
                    isExtensionReceiver = false,
                    isAssisted = typeRef.isAssisted,
                    name = it.name!!.asString()
                )
            },
            isCall = owner !is KSPropertyDeclaration &&
                    (owner !is KSClassDeclaration || owner.classKind != ClassKind.OBJECT),
            isSuspend = (owner is KSFunctionDeclaration && owner.modifiers.contains(Modifier.SUSPEND)),
            isExternal = owner is KSDeclarationDescriptorImpl
        )
    }

    private val moduleByType = mutableMapOf<TypeRef, com.ivianuu.injekt.compiler.ModuleDescriptor>()
    fun moduleForType(type: TypeRef): com.ivianuu.injekt.compiler.ModuleDescriptor {
        return moduleByType.getOrPut(type) {
            val declaration = classForFqName(type.classifier.fqName)
            val substitutionMap = type.classifier.typeParameters
                .zip(type.typeArguments)
                .toMap()
            declaration.getAllFunctions()

            ModuleDescriptor(
                type,
                (declaration.getAllFunctions() + declaration.getAllProperties()).filter {
                    it.hasAnnotationWithPropertyAndClass(injektTypes.binding) ||
                            it.hasAnnotationWithPropertyAndClass(injektTypes.setElements) ||
                            it.hasAnnotationWithPropertyAndClass(injektTypes.mapEntries) ||
                            it.hasAnnotationWithPropertyAndClass(injektTypes.module)
                }
                    .mapNotNull {
                        when (it) {
                            is KSPropertyAccessor -> it.receiver
                            is FunctionDescriptor -> it
                            else -> null
                        }
                    }
                    .map { callableDeclaration ->
                        val callable = callableForDeclaration(callableDeclaration)
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

    private val callableByFunctionAlias = mutableMapOf<TypeRef, Callable>()
    fun functionForAlias(aliasType: TypeRef): Callable {
        return callableByFunctionAlias.getOrPut(aliasType) {
            callableForDeclaration(
                memberScopeForFqName(aliasType.classifier.fqName.parent())!!
                    .getContributedFunctions(
                        aliasType.classifier.fqName.shortName().asNameId(),
                        NoLookupLocation.FROM_BACKEND
                    )
                    .single()
                    .toKSDeclaration() as KSFunctionDeclaration
            )
        }
    }

}
