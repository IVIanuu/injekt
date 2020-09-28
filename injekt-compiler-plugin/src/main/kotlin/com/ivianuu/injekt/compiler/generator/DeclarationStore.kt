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
import com.ivianuu.injekt.compiler.irtransform.asNameId
import com.ivianuu.injekt.compiler.unsafeLazy
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
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

    private val internalContextFactories = mutableMapOf<FqName, ContextFactoryDescriptor>()
    fun addInternalContextFactory(factory: ContextFactoryDescriptor) {
        internalContextFactories[factory.factoryType.classifier.fqName] = factory
    }

    private val externalContextFactories = mutableMapOf<FqName, ContextFactoryDescriptor>()
    fun getContextFactoryForFqName(fqName: FqName): ContextFactoryDescriptor {
        return internalContextFactories[fqName] ?: externalContextFactories[fqName] ?: kotlin.run {
            val descriptor = moduleDescriptor.findClassAcrossModuleDependencies(
                ClassId.topLevel(fqName)
            )!!
            val createFunction = descriptor.unsubstitutedMemberScope
                .getContributedFunctions("create".asNameId(), NoLookupLocation.FROM_BACKEND)
                .single()
            ContextFactoryDescriptor(
                factoryType = SimpleTypeRef(
                    classifier = descriptor.toClassifierRef(),
                    isChildContextFactory = descriptor.hasAnnotation(InjektFqNames.ChildContextFactory),
                    typeArguments = descriptor.defaultType
                        .arguments.map { it.type.toTypeRef(it.projectionKind) }
                ),
                contextType = createFunction.returnType!!.toTypeRef(),
                inputTypes = createFunction.valueParameters
                    .map { it.type.toTypeRef() }
            ).also { externalContextFactories[fqName] = it }
        }
    }

    private val internalRootFactories = mutableSetOf<ContextFactoryImplDescriptor>()
    fun addInternalRootFactory(rootFactory: ContextFactoryImplDescriptor) {
        internalRootFactories += rootFactory
    }

    val allRootFactories by unsafeLazy {
        internalRootFactories + moduleDescriptor.getPackage(rootFactoriesPath.fqName)
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
                    factory = getContextFactoryForFqName(index.defaultType.toTypeRef().classifier.fqName)
                )
            }
    }

    private val internalCallables = mutableListOf<CallableRef>()
    fun addInternalGiven(callableRef: CallableRef) {
        internalCallables += callableRef
    }

    private val givensByType = mutableMapOf<TypeRef, List<CallableRef>>()
    fun givens(type: TypeRef) = givensByType.getOrPut(type) {
        val path = givensPathOf(type)
        (internalCallables
            .filter { it.givenKind == CallableRef.GivenKind.GIVEN } + (indexer.functionIndices(path) +
                indexer.classIndices(path)
                    .flatMap { it.constructors.toList() } +
                indexer.propertyIndices(path)
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
            .map { it.toCallableRef() })
            .filter { type.isAssignable(it.type) }
    }

    private val givenMapEntriesByType = mutableMapOf<TypeRef, List<CallableRef>>()
    fun givenMapEntries(type: TypeRef) = givenMapEntriesByType.getOrPut(type) {
        val path = givenMapEntriesPathOf(type)
        (internalCallables
            .filter { it.givenKind == CallableRef.GivenKind.GIVEN_MAP_ENTRIES } +
                (indexer.functionIndices(path) +
                        indexer.propertyIndices(path).mapNotNull { it.getter })
                    .filter { it.hasAnnotation(InjektFqNames.GivenMapEntries) }
                    .filter {
                        isInjektCompiler ||
                                !it.fqNameSafe.asString()
                                    .startsWith("com.ivianuu.injekt.compiler")
                    }.map { it.toCallableRef() })
            .filter { type.isAssignable(it.type) }
    }

    private val givenSetElementsByType = mutableMapOf<TypeRef, List<CallableRef>>()
    fun givenSetElements(type: TypeRef) = givenSetElementsByType.getOrPut(type) {
        val path = givenSetElementsPathOf(type)
        (internalCallables
            .filter { it.givenKind == CallableRef.GivenKind.GIVEN_SET_ELEMENTS } + (indexer.functionIndices(
            path
        ) +
                indexer.propertyIndices(path).mapNotNull { it.getter })
            .filter { it.hasAnnotation(InjektFqNames.GivenSetElements) }
            .filter {
                isInjektCompiler ||
                        !it.fqNameSafe.asString()
                            .startsWith("com.ivianuu.injekt.compiler")
            }
            .map { it.toCallableRef() })
            .filter { type.isAssignable(it.type) }
    }

    private val internalEntryPointsByContext = mutableMapOf<FqName, MutableSet<FqName>>()
    fun addInternalEntryPoint(
        contextId: FqName,
        entryPoint: FqName
    ) {
        internalEntryPointsByContext.getOrPut(contextId) { mutableSetOf() } += entryPoint
    }

    private val entryPointsByContext = mutableMapOf<FqName, Set<FqName>>()
    fun getEntryPoints(contextId: FqName): Set<FqName> {
        return entryPointsByContext.getOrPut(contextId) {
            internalEntryPointsByContext.getOrElse(contextId) { emptySet() } +
                    indexer.classIndices(entryPointPathOf(contextId))
                        .map {
                            val entryPointAnnotation =
                                it.annotations.findAnnotation(InjektFqNames.EntryPoint)!!
                            entryPointAnnotation.allValueArguments["entryPoint".asNameId()]
                                .let { it as KClassValue }
                                .getArgumentType(moduleDescriptor)
                                .toTypeRef()
                                .classifier
                                .fqName
                        }
        }
    }

    val internalInfosByFqName = mutableMapOf<FqName, ReaderInfo>()
    private val readerContextsByFqName = mutableMapOf<FqName, ReaderInfo>()
    fun getReaderContextForCallable(callableRef: CallableRef): ReaderInfo? {
        return getReaderInfoByFqName(
            callableRef.packageFqName,
            callableRef.fqName
        )
    }

    fun addInternalReaderInfo(info: ReaderInfo) {
        readerContextsByFqName[info.callable.fqName] = info
        internalInfosByFqName[info.callable.fqName] = info
    }

    fun getReaderInfoByFqName(
        packageFqName: FqName,
        fqName: FqName
    ): ReaderInfo? {
        readerContextsByFqName[fqName]?.let { return it }

        val memberScope = indexer.getMemberScope(fqName.parent())!!

        return null

        /*return memberScope.getContributedDescriptors(DescriptorKindFilter.FUNCTIONS)
            .filter { it.hasAnnotation(InjektFqNames.ReaderOverload) }
            .filter {

            }

        return moduleDescriptor.findClassAcrossModuleDependencies(
            ClassId.topLevel(fqName)
        )?.let { classDescriptor ->
            ReaderContextDescriptor(
                type = SimpleTypeRef(
                    classifier = classDescriptor.toClassifierRef(),
                    isContext = true,
                    typeArguments = classDescriptor.defaultType
                        .arguments.map { it.type.toTypeRef(it.projectionKind) }
                ),
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
        }?.also { readerContextsByFqName[fqName] = it }*/
    }

    fun getReaderInfoForDeclaration(declaration: DeclarationDescriptor): ReaderInfo? {
        return getReaderInfoByFqName(
            declaration.findPackage()!!.fqName,
            declaration.fqNameSafe
        )
    }

}
