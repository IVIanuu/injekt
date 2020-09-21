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

package com.ivianuu.injekt.compiler.backend

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.backend.readercontextimpl.Key
import com.ivianuu.injekt.compiler.backend.readercontextimpl.asKey
import com.ivianuu.injekt.compiler.unsafeLazy
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Given(IrContext::class)
class DeclarationGraph {

    private val indexer = given<IrIndexedDeclarations>()

    private val mapSymbol = pluginContext.referenceClass(
        FqName("kotlin.collections.Map")
    )!!
    private val setSymbol = pluginContext.referenceClass(
        FqName("kotlin.collections.Set")
    )!!

    val rootContextFactories: List<IrClass> by unsafeLazy {
        indexer.classIndices
            .filter { it.hasAnnotation(InjektFqNames.RootContextFactory) }
            .filter {
                isInjektCompiler ||
                        !it.descriptor.fqNameSafe.asString()
                            .startsWith("com.ivianuu.injekt.compiler")
            }
    }

    private val allGivens by unsafeLazy {
        (indexer.functionIndices +
                indexer.classIndices
                    .flatMap { it.constructors.toList() } +
                indexer.propertyIndices
                    .mapNotNull { it.getter }
                )
            .filter {
                (it.hasAnnotation(InjektFqNames.Given) || it.hasAnnotatedAnnotations(InjektFqNames.Effect)) ||
                        (it is IrSimpleFunction &&
                                (it.correspondingPropertySymbol?.owner?.hasAnnotation(InjektFqNames.Given) == true ||
                                        it.correspondingPropertySymbol?.owner?.hasAnnotatedAnnotations(
                                            InjektFqNames.Effect
                                        ) == true)) ||
                        (it is IrConstructor && (it.constructedClass.hasAnnotation(InjektFqNames.Given) ||
                                it.constructedClass.hasAnnotatedAnnotations(InjektFqNames.Effect)))
            }
            .filter {
                isInjektCompiler ||
                        !it.descriptor.fqNameSafe.asString()
                            .startsWith("com.ivianuu.injekt.compiler")
            }
            .map { given<ReaderContextParamTransformer>().getTransformedFunction(it) }
            .filter { it.getContext() != null }
            .distinct()
    }

    private val givensByKey = mutableMapOf<Key, List<IrFunction>>()
    fun givens(key: Key) = givensByKey.getOrPut(key) {
        allGivens
            .filter { function ->
                if (function.extensionReceiverParameter != null || function.valueParameters
                        .any { it.name.asString() != "_context" }
                ) {
                    function.getFunctionType(skipContext = true).asKey() == key
                } else {
                    function.returnType.asKey() == key
                }
            }
    }

    private val allGivenMapEntries by unsafeLazy {
        (indexer.functionIndices +
                indexer.propertyIndices.mapNotNull { it.getter })
            .filter { it.hasAnnotation(InjektFqNames.GivenMapEntries) }
            .filter {
                isInjektCompiler ||
                        !it.descriptor.fqNameSafe.asString()
                            .startsWith("com.ivianuu.injekt.compiler")
            }
            .map { given<ReaderContextParamTransformer>().getTransformedFunction(it) }
            .filter { it.getContext() != null }
    }

    private val givenMapEntriesByKey = mutableMapOf<Key, List<IrFunction>>()
    fun givenMapEntries(key: Key) = givenMapEntriesByKey.getOrPut(key) {
        if (key.type.classOrNull != mapSymbol) return@getOrPut emptyList()
        allGivenMapEntries
            .filter { it.returnType.asKey() == key }
    }

    private val allGivenSetElements by unsafeLazy {
        (indexer.functionIndices +
                indexer.propertyIndices.mapNotNull { it.getter })
            .filter { it.hasAnnotation(InjektFqNames.GivenSetElements) }
            .filter {
                isInjektCompiler ||
                        !it.descriptor.fqNameSafe.asString()
                            .startsWith("com.ivianuu.injekt.compiler")
            }
            .map { given<ReaderContextParamTransformer>().getTransformedFunction(it) }
            .filter { it.getContext() != null }
    }

    private val givenSetElementsByKey = mutableMapOf<Key, List<IrFunction>>()
    fun givenSetElements(key: Key) = givenSetElementsByKey.getOrPut(key) {
        if (key.type.classOrNull != setSymbol) return@getOrPut emptyList()
        allGivenSetElements
            .filter { it.returnType.asKey() == key }
    }

    private val runReaderContexts = mutableMapOf<IrClass, List<IrClass>>()
    fun getRunReaderContexts(context: IrClass): List<IrClass> {
        return runReaderContexts.getOrPut(context) {
            indexer.classIndices
                .filter {
                    it.getClassFromAnnotation(InjektFqNames.RunReaderCall, 0) == context
                }
                .mapNotNull { it.getClassFromAnnotation(InjektFqNames.RunReaderCall, 1) }
        }
    }

}
