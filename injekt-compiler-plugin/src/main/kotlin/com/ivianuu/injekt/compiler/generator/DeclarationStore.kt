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
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
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
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

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
                ClassId.topLevel(type.classifier.fqName)
            )!!
            val createFunction = descriptor.unsubstitutedMemberScope
                .getContributedFunctions("create".asNameId(), NoLookupLocation.FROM_BACKEND)
                .single()
            ContextFactoryDescriptor(
                factoryType = type,
                contextType = createFunction.returnType!!.toTypeRef(),
                inputTypes = createFunction.valueParameters
                    .map { it.type.toTypeRef() }
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
                    factory = getContextFactoryForType(index.defaultType.toTypeRef())
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
                    runReaderCallAnnotation.allValueArguments["calleeContext".asNameId()]
                        .let { it as KClassValue }
                        .getArgumentType(moduleDescriptor)
                        .toTypeRef() to runReaderCallAnnotation.allValueArguments["blockContext".asNameId()]
                        .let { it as KClassValue }
                        .getArgumentType(moduleDescriptor)
                        .toTypeRef()
                }
                .filter { it.first == contextId }
                .map { it.second }
        }
    }

    val internalReaderContextsByType = mutableMapOf<ClassifierRef, ReaderContextDescriptor>()
    private val readerContextsByType = mutableMapOf<ClassifierRef, ReaderContextDescriptor>()

    fun getReaderContextForCallable(callableRef: CallableRef): ReaderContextDescriptor? {
        return getReaderContextByType(
            SimpleTypeRef(
                classifier = ClassifierRef(
                    callableRef.packageFqName.child(
                        contextNameOf(
                            packageFqName = callableRef.packageFqName,
                            fqName = callableRef.fqName,
                            uniqueKey = callableRef.uniqueKey
                        )
                    )
                ),
                isContext = true
            )
        )
    }

    fun addInternalReaderContext(context: ReaderContextDescriptor) {
        readerContextsByType[context.type.classifier] = context
        internalReaderContextsByType[context.type.classifier] = context
    }

    fun getReaderContextByType(type: TypeRef): ReaderContextDescriptor? {
        readerContextsByType[type.classifier]?.let { return it }

        return moduleDescriptor.findClassAcrossModuleDependencies(
            ClassId.topLevel(type.classifier.fqName)
        )?.let { classDescriptor ->
            ReaderContextDescriptor(
                type = type,
                typeParameters = classDescriptor.declaredTypeParameters
                    .map {
                        ReaderContextTypeParameter(
                            it.name,
                            it.upperBounds.map { it.toTypeRef() }
                        )
                    },
                originatingFiles = emptyList(),
                origin = FqName(
                    classDescriptor.annotations.findAnnotation(InjektFqNames.Origin)!!
                        .allValueArguments["fqName".asNameId()]!!.value as String
                )
            ).apply {
                givenTypes += classDescriptor.unsubstitutedMemberScope
                    .getContributedDescriptors(DescriptorKindFilter.FUNCTIONS)
                    .filterIsInstance<FunctionDescriptor>()
                    .filter { it.dispatchReceiverParameter?.type == classDescriptor.defaultType }
                    .map { it.returnType!!.toTypeRef() }
            }
        }?.also { readerContextsByType[type.classifier] = it }
    }

    fun getReaderContextForDeclaration(declaration: DeclarationDescriptor): ReaderContextDescriptor? {
        val contextFqName = declaration.findPackage().fqName.child(
            declaration.getContextName()
        )
        return getReaderContextByType(
            SimpleTypeRef(
                classifier = ClassifierRef(
                    fqName = contextFqName,
                    typeParameters = when (declaration) {
                        is ClassifierDescriptorWithTypeParameters -> declaration.declaredTypeParameters
                            .map {
                                ClassifierRef(
                                    contextFqName.child(it.name),
                                    isTypeParameter = true
                                )
                            }
                        is CallableDescriptor -> declaration.typeParameters
                            .map {
                                ClassifierRef(
                                    contextFqName.child(it.name),
                                    isTypeParameter = true
                                )
                            }
                        else -> emptyList()
                    }
                ),
                isContext = true
            )
        )
    }

    private val givenSetsByType = mutableMapOf<TypeRef, GivenSetDescriptor>()
    fun getGivenSetForType(type: TypeRef): GivenSetDescriptor {
        return givenSetsByType.getOrPut(type) {
            val descriptor = indexer.getMemberScope(type.classifier.fqName.parent())!!
                .getContributedClassifier(
                    type.classifier.fqName.shortName(),
                    NoLookupLocation.FROM_BACKEND
                )!! as ClassDescriptor
            val members = descriptor.unsubstitutedMemberScope.getContributedDescriptors(
                DescriptorKindFilter.CALLABLES
            )
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
