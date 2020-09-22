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
import com.ivianuu.injekt.compiler.checkers.getFunctionType
import com.ivianuu.injekt.compiler.checkers.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import com.ivianuu.injekt.compiler.irtransform.asNameId
import com.ivianuu.injekt.compiler.unsafeLazy
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Given(GenerationContext::class)
class DeclarationStore {

    private val indexer = given<Indexer>()

    private val mapDescriptor = moduleDescriptor.findClassAcrossModuleDependencies(
        ClassId.topLevel(FqName("kotlin.collections.Map"))
    )!!
    private val setDescriptor = moduleDescriptor.findClassAcrossModuleDependencies(
        ClassId.topLevel(FqName("kotlin.collections.Set"))
    )!!

    private val allGivens by unsafeLazy {
        (indexer.functionIndices +
                indexer.classIndices
                    .flatMap { it.constructors.toList() } +
                indexer.propertyIndices
                    .mapNotNull { it.getter }
                )
            .filter {
                (it.hasAnnotation(InjektFqNames.Given) || it.hasAnnotatedAnnotations(InjektFqNames.Effect)) ||
                        (it is PropertyAccessorDescriptor &&
                                (it.correspondingProperty.hasAnnotation(InjektFqNames.Given) ||
                                        it.correspondingProperty.hasAnnotatedAnnotations(
                                            InjektFqNames.Effect
                                        ))) ||
                        (it is ConstructorDescriptor && (it.constructedClass.hasAnnotation(
                            InjektFqNames.Given
                        ) ||
                                it.constructedClass.hasAnnotatedAnnotations(InjektFqNames.Effect)))
            }
            .filter {
                isInjektCompiler ||
                        !it.fqNameSafe.asString().startsWith("com.ivianuu.injekt.compiler")
            }
            .distinct()
    }

    private val givensByType = mutableMapOf<TypeRef, List<FunctionDescriptor>>()
    fun givens(type: TypeRef) = givensByType.getOrPut(type) {
        allGivens
            .filter { function ->
                if (function.extensionReceiverParameter != null || function.valueParameters.isNotEmpty()) {
                    KotlinTypeRef(function.getFunctionType()) == type
                } else {
                    KotlinTypeRef(function.returnType!!) == type
                }
            }
    }

    private val allGivenMapEntries by unsafeLazy {
        (indexer.functionIndices +
                indexer.propertyIndices.mapNotNull { it.getter })
            .filter { it.hasAnnotation(InjektFqNames.GivenMapEntries) }
            .filter {
                isInjektCompiler ||
                        !it.fqNameSafe.asString()
                            .startsWith("com.ivianuu.injekt.compiler")
            }
    }

    private val givenMapEntriesByKey = mutableMapOf<TypeRef, List<FunctionDescriptor>>()
    fun givenMapEntries(type: TypeRef) = givenMapEntriesByKey.getOrPut(type) {
        // TODO if (key.type.classOrNull != mapSymbol) return@getOrPut emptyList()
        allGivenMapEntries
            .filter { KotlinTypeRef(it.returnType!!) == type }
    }

    private val allGivenSetElements by unsafeLazy {
        (indexer.functionIndices +
                indexer.propertyIndices.mapNotNull { it.getter })
            .filter { it.hasAnnotation(InjektFqNames.GivenSetElements) }
            .filter {
                isInjektCompiler ||
                        !it.fqNameSafe.asString()
                            .startsWith("com.ivianuu.injekt.compiler")
            }
    }

    private val givenSetElementsByKey = mutableMapOf<TypeRef, List<FunctionDescriptor>>()
    fun givenSetElements(type: TypeRef) = givenSetElementsByKey.getOrPut(type) {
        // todo if (key.type.classOrNull != setSymbol) return@getOrPut emptyList()
        allGivenSetElements
            .filter { KotlinTypeRef(it.returnType!!) == type }
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

}
