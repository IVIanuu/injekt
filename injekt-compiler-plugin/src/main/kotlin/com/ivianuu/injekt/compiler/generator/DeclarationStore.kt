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

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import com.ivianuu.injekt.compiler.contextNameOf
import com.ivianuu.injekt.compiler.getContextName
import com.ivianuu.injekt.compiler.irtransform.asNameId
import com.ivianuu.injekt.compiler.unsafeLazy
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Given(GenerationContext::class)
class DeclarationStore {

    private val indexer = given<Indexer>()

    private val internalContextFactories = mutableMapOf<TypeRef, ContextFactoryDescriptor>()

    fun addInternalContextFactory(factory: ContextFactoryDescriptor) {
        internalContextFactories[factory.factoryType] = factory
    }

    private val externalContextFactories = mutableMapOf<TypeRef, ContextFactoryDescriptor>()

    fun getContextFactoryForType(type: TypeRef): ContextFactoryDescriptor {
        return internalContextFactories[type] ?: externalContextFactories[type] ?: kotlin.run {
            val descriptor = moduleDescriptor.findClassAcrossModuleDependencies(
                ClassId.topLevel(type.fqName)
            )!!
            val createFunction = descriptor.unsubstitutedMemberScope
                .getContributedFunctions("create".asNameId(), NoLookupLocation.FROM_BACKEND)
                .single()
            ContextFactoryDescriptor(
                factoryType = type,
                contextType = KotlinTypeRef(createFunction.returnType!!),
                inputTypes = createFunction.valueParameters
                    .map { KotlinTypeRef(it.type) }
            )
        }
    }

    private val internalRootFactories = mutableSetOf<ContextFactoryImplDescriptor>()
    fun addInternalRootFactory(rootFactory: ContextFactoryImplDescriptor) {
        internalRootFactories += rootFactory
    }

    val allRootFactories by unsafeLazy {
        internalRootFactories + moduleDescriptor.getPackage(InjektFqNames.IndexPackage)
            .memberScope
            .let { memberScope ->
                (memberScope.getClassifierNames() ?: emptySet())
                    .map {
                        memberScope.getContributedClassifier(
                            it,
                            NoLookupLocation.FROM_BACKEND
                        )
                    }
            }
            .filterIsInstance<ClassDescriptor>()
            .mapNotNull { index ->
                index.annotations.findAnnotation(InjektFqNames.Index)
                    ?.takeIf { annotation ->
                        annotation.allValueArguments["type".asNameId()]
                            .let { it as StringValue }
                            .value == "class"
                    }
                    ?.let { annotation ->
                        val fqName =
                            annotation.allValueArguments.getValue("fqName".asNameId())
                                .let { it as StringValue }
                                .value
                                .let { FqName(it) }
                        if (!isInjektCompiler &&
                            fqName.asString().startsWith("com.ivianuu.injekt.compiler")
                        ) return@mapNotNull null
                        moduleDescriptor
                            .findClassAcrossModuleDependencies(ClassId.topLevel(fqName))
                    }
            }
            .mapNotNull { index ->
                val factoryImplFqName =
                    index.annotations.findAnnotation(InjektFqNames.RootContextFactory)
                        ?.allValueArguments
                        ?.values
                        ?.single()
                        ?.let { it as StringValue }
                        ?.value
                        ?.let { FqName(it) } ?: return@mapNotNull null
                ContextFactoryImplDescriptor(
                    factoryImplFqName = factoryImplFqName,
                    factory = getContextFactoryForType(KotlinTypeRef(index.defaultType))
                )
            }
    }

    private val internalCallableRefs = mutableListOf<CallableRef>()
    fun addInternalGiven(callableRef: CallableRef) {
        internalCallableRefs += callableRef
    }

    private val allGivens by unsafeLazy {
        internalCallableRefs
            .filter { it.givenKind == CallableRef.GivenKind.GIVEN } + (indexer.functionIndices +
                indexer.classIndices
                    .flatMap { it.constructors.toList() } +
                indexer.propertyIndices
                    .mapNotNull { it.getter }
                )
            .filter {
                it.hasAnnotationWithPropertyAndClass(InjektFqNames.Given) ||
                        it.hasAnnotatedAnnotationsWithPropertyAndClass(InjektFqNames.Effect)
            }
            .filter {
                isInjektCompiler ||
                        !it.fqNameSafe.asString().startsWith("com.ivianuu.injekt.compiler")
            }
            .distinct()
            .map { it.toCallableRef() }
    }

    private val givensByType = mutableMapOf<TypeRef, List<CallableRef>>()
    fun givens(type: TypeRef) = givensByType.getOrPut(type) {
        allGivens.filter { it.type == type }
    }

    private val allGivenMapEntries by unsafeLazy {
        internalCallableRefs
            .filter { it.givenKind == CallableRef.GivenKind.GIVEN_MAP_ENTRIES } +
                (indexer.functionIndices +
                        indexer.propertyIndices.mapNotNull { it.getter })
                    .filter { it.hasAnnotation(InjektFqNames.GivenMapEntries) }
                    .filter {
                        isInjektCompiler ||
                                !it.fqNameSafe.asString()
                                    .startsWith("com.ivianuu.injekt.compiler")
                    }.map { it.toCallableRef() }
    }

    private val givenMapEntriesByKey = mutableMapOf<TypeRef, List<CallableRef>>()
    fun givenMapEntries(type: TypeRef) = givenMapEntriesByKey.getOrPut(type) {
        // TODO if (key.type.classOrNull != mapSymbol) return@getOrPut emptyList()
        allGivenMapEntries
            .filter { it.type == type }
    }

    private val allGivenSetElements by unsafeLazy {
        internalCallableRefs
            .filter { it.givenKind == CallableRef.GivenKind.GIVEN_SET_ELEMENTS } + (indexer.functionIndices +
                indexer.propertyIndices.mapNotNull { it.getter })
            .filter { it.hasAnnotation(InjektFqNames.GivenSetElements) }
            .filter {
                isInjektCompiler ||
                        !it.fqNameSafe.asString()
                            .startsWith("com.ivianuu.injekt.compiler")
            }
            .map { it.toCallableRef() }
    }

    private val givenSetElementsByKey = mutableMapOf<TypeRef, List<CallableRef>>()
    fun givenSetElements(type: TypeRef) = givenSetElementsByKey.getOrPut(type) {
        // todo if (key.type.classOrNull != setSymbol) return@getOrPut emptyList()
        allGivenSetElements
            .filter { it.type == type }
    }

    private val internalRunReaderContexts = mutableMapOf<TypeRef, MutableSet<TypeRef>>()
    fun addInternalRunReaderContext(
        contextId: TypeRef,
        blockContext: TypeRef
    ) {
        internalRunReaderContexts.getOrPut(contextId) { mutableSetOf() } += blockContext
    }

    private val runReaderContexts = mutableMapOf<TypeRef, Set<TypeRef>>()
    fun getRunReaderContexts(contextId: TypeRef): Set<TypeRef> {
        return runReaderContexts.getOrPut(contextId) {
            internalRunReaderContexts.getOrElse(contextId) { emptySet() } + indexer.classIndices
                .mapNotNull {
                    val runReaderCallAnnotation =
                        it.annotations.findAnnotation(InjektFqNames.RunReaderCall)
                            ?: return@mapNotNull null
                    KotlinTypeRef(
                        runReaderCallAnnotation.allValueArguments["calleeContext".asNameId()]
                            .let { it as KClassValue }
                            .getArgumentType(moduleDescriptor)
                    ) to KotlinTypeRef(runReaderCallAnnotation.allValueArguments["blockContext".asNameId()]
                        .let { it as KClassValue }
                        .getArgumentType(moduleDescriptor))
                }
                .filter { it.first == contextId }
                .map { it.second }
        }
    }

    private val readerContextsByDeclaration =
        mutableMapOf<DeclarationDescriptor, ReaderContextDescriptor>()
    val readerContextsByType = mutableMapOf<TypeRef, ReaderContextDescriptor>()
    private val externalReaderContexts =
        mutableMapOf<DeclarationDescriptor, ReaderContextDescriptor>()

    fun getReaderContextForCallable(callableRef: CallableRef): ReaderContextDescriptor? {
        return getReaderContextByType(
            SimpleTypeRef(
                fqName = callableRef.packageFqName.child(
                    contextNameOf(
                        packageFqName = callableRef.packageFqName,
                        fqName = callableRef.fqName,
                        uniqueKey = callableRef.uniqueKey
                    )
                )
            )
        )
    }

    fun addReaderContextForType(type: TypeRef, context: ReaderContextDescriptor) {
        readerContextsByType[type] = context
    }

    fun getReaderContextByType(type: TypeRef): ReaderContextDescriptor? {
        readerContextsByType[type]?.let { return it }

        return moduleDescriptor.findClassAcrossModuleDependencies(
            ClassId.topLevel(type.fqName)
        )?.let { classDescriptor ->
            ReaderContextDescriptor(
                type = type,
                typeParameters = classDescriptor.declaredTypeParameters
                    .map {
                        ReaderContextTypeParameter(
                            it.name,
                            it.upperBounds.map { KotlinTypeRef(it) }
                        )
                    },
                originatingFiles = emptyList(),
                origin = FqName(
                    classDescriptor.annotations.findAnnotation(InjektFqNames.Origin)!!
                        .allValueArguments["fqName".asNameId()]!!.value as String
                )
            ).apply {
                givenTypes += classDescriptor.unsubstitutedMemberScope
                    .getContributedDescriptors()
                    .filterIsInstance<FunctionDescriptor>()
                    .filter { it.dispatchReceiverParameter?.type == classDescriptor.defaultType }
                    .map { KotlinTypeRef(it.returnType!!) }
            }
        }?.also { readerContextsByType[type] = it }
    }

    fun addReaderContextForDeclaration(
        declaration: DeclarationDescriptor,
        context: ReaderContextDescriptor
    ) {
        readerContextsByDeclaration[declaration.original] = context
        readerContextsByType[context.type] = context
    }

    fun getReaderContextForDeclaration(declaration: DeclarationDescriptor): ReaderContextDescriptor? {
        return readerContextsByDeclaration[declaration.original]
            ?: externalReaderContexts[declaration.original]
            ?: declaration.findPackage()
                .getMemberScope()
                .getContributedClassifier(
                    declaration.getContextName(),
                    NoLookupLocation.FROM_BACKEND
                )
                ?.let { it as ClassDescriptor }
                ?.let {
                    ReaderContextDescriptor(
                        SimpleTypeRef(it.fqNameSafe, isContext = true),
                        it.declaredTypeParameters
                            .map { typeParameter ->
                                ReaderContextTypeParameter(
                                    typeParameter.name,
                                    typeParameter.upperBounds
                                        .map { KotlinTypeRef(it) }
                                )
                            },
                        declaration.fqNameSafe,
                        emptyList()
                    )
                }
                ?.also { externalReaderContexts[declaration.original] = it }
                ?.also { readerContextsByType[it.type] = it }
    }

    private val givenSetsByType = mutableMapOf<TypeRef, GivenSetDescriptor>()
    fun getGivenSetForType(type: TypeRef): GivenSetDescriptor {
        return givenSetsByType.getOrPut(type) {
            val descriptor = indexer.getMemberScope(type.fqName.parent())!!
                .getContributedClassifier(
                    type.fqName.shortName(),
                    NoLookupLocation.FROM_BACKEND
                )!! as ClassDescriptor
            val members = descriptor.unsubstitutedMemberScope.getContributedDescriptors()
            GivenSetDescriptor(
                type = type,
                callables = members
                    .filter {
                        it.hasAnnotationWithPropertyAndClass(
                            InjektFqNames.Given
                        ) || it.hasAnnotationWithPropertyAndClass(InjektFqNames.GivenSetElements) ||
                                it.hasAnnotationWithPropertyAndClass(InjektFqNames.GivenMapEntries) ||
                                it.hasAnnotationWithPropertyAndClass(InjektFqNames.GivenSet)
                    }
                    .mapNotNull {
                        when (it) {
                            is PropertyDescriptor -> it.getter!!
                            is FunctionDescriptor -> it
                            else -> null
                        }
                    }
                    .map { it.toCallableRef() }
            )
        }
    }

}
