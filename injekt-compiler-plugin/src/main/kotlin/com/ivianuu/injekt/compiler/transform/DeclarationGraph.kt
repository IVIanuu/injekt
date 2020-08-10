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

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.flatMapFix
import com.ivianuu.injekt.compiler.getClassFromAnnotation
import com.ivianuu.injekt.compiler.getConstantFromAnnotationOrNull
import com.ivianuu.injekt.compiler.getContext
import com.ivianuu.injekt.compiler.transform.implicit.ImplicitContextParamTransformer
import com.ivianuu.injekt.compiler.transform.runreader.RunReaderContextImplTransformer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import kotlin.system.measureTimeMillis

class DeclarationGraph(
    private val indexer: Indexer,
    val module: IrModuleFragment,
    private val implicitContextParamTransformer: ImplicitContextParamTransformer
) {

    private val _runReaderContexts = mutableListOf<IrClass>()
    val runReaderContexts: List<IrClass> get() = _runReaderContexts

    private val _bindings = mutableListOf<IrFunction>()
    val bindings: List<IrFunction> get() = _bindings

    private val _mapEntries = mutableListOf<IrFunction>()
    val mapEntries: List<IrFunction> get() = _mapEntries

    private val _setElements = mutableListOf<IrFunction>()
    val setElements: List<IrFunction> get() = _setElements

    private val _genericContexts = mutableListOf<IrClass>()
    val genericContexts: List<IrClass> get() = _genericContexts

    lateinit var runReaderContextImplTransformer: RunReaderContextImplTransformer

    init {
        measureTimeMillis {
            collectRunReaderContexts()
        }.also { println("collecting run reader contexts took $it ms") }
        measureTimeMillis {
            collectBindings()
        }.also { println("collecting bindings took $it ms") }
        measureTimeMillis {
            collectMapEntries()
        }.also { println("collecting map entries took $it ms") }
        measureTimeMillis {
            collectSetElements()
        }.also { println("collecting set elements took $it ms") }
        measureTimeMillis {
            collectGenericContexts()
        }.also { println("collecting generic contexts took $it ms") }
    }

    sealed class ParentRunReaderContext {
        data class Known(val clazz: IrClass) : ParentRunReaderContext() {
            override fun toString(): String {
                return "Known(context=${clazz.descriptor.fqNameSafe})"
            }
        }

        data class Unknown(
            val origin: IrFunction
        ) : ParentRunReaderContext() {
            override fun toString(): String {
                return "Unknown(origin=${origin.descriptor.fqNameSafe})"
            }
        }
    }

    fun getParentRunReaderContexts(context: IrClass): List<ParentRunReaderContext> {
        val parents = mutableListOf<ParentRunReaderContext>()

        val processedClasses = mutableSetOf<IrClass>()

        val invokingContexts = getInvokingContexts(context)

        fun collectParents(invokingContext: IrClass) {
            if (invokingContext in processedClasses) return
            processedClasses += invokingContext

            if (isRunReaderContext(invokingContext)) {
                parents += ParentRunReaderContext.Known(invokingContext)
                return
            }

            parents += getGivenDeclarationsForContext(invokingContext)
                .flatMapFix { declaration ->
                    val generatedContextsWithInvokerSuperType = runReaderContextImplTransformer
                        .generatedContexts
                        .values
                        .flatten()
                        .filter { invokingContext.defaultType in it.superTypes }
                        .map { it.superTypes.first().classOrNull!!.owner }

                    if (generatedContextsWithInvokerSuperType.isNotEmpty()) {
                        generatedContextsWithInvokerSuperType
                            .map { ParentRunReaderContext.Known(it) }
                    } else {
                        listOf(ParentRunReaderContext.Unknown(declaration))
                    }
                }

            getInvokingContexts(invokingContext)
                .forEach { collectParents(it) }
        }

        invokingContexts.forEach { collectParents(it) }

        return parents
    }

    fun getNonGenericParentContext(context: IrClass): List<IrClass> {
        val parents = mutableListOf<IrClass>()

        val processedClasses = mutableSetOf<IrClass>()

        val invokingContexts = getInvokingContexts(context)

        fun collectParents(invokingContext: IrClass) {
            if (invokingContext in processedClasses) return
            processedClasses += invokingContext

            if (invokingContext.typeParameters.isEmpty()) {
                parents += invokingContext
                return
            }

            getInvokingContexts(invokingContext)
                .forEach { collectParents(it) }
        }

        invokingContexts.forEach { collectParents(it) }

        return parents
    }

    private fun isRunReaderContext(context: IrClass): Boolean {
        return runReaderContexts
            .map { it.superTypes.first() }
            .any { it == context.defaultType }
    }

    private fun getGivenDeclarationsForContext(context: IrClass): List<IrFunction> {
        return (_bindings + _mapEntries + _setElements)
            .filter { it.getContext()?.defaultType == context.defaultType }
    }

    private fun getInvokingContexts(context: IrClass): Set<IrClass> {
        val allContexts = listOf(context) + getAllSuperContexts(context)

        val invokerIfRunChildReader = runReaderContexts
            .singleOrNull { it.superTypes[0] == context.defaultType }
            ?.superTypes
            ?.getOrNull(1)
            ?.classOrNull
            ?.owner

        return setOfNotNull(
            *indexer.classIndices("reader_invocation")
                .filter { clazz ->
                    clazz.getClassFromAnnotation(
                        InjektFqNames.ReaderInvocation,
                        0
                    ) in allContexts
                }
                .map {
                    it.getClassFromAnnotation(
                        InjektFqNames.ReaderInvocation,
                        1
                    )!!
                }
                .filter { it != context }
                .toTypedArray(),
            invokerIfRunChildReader
        )
    }

    private fun getAllSuperContexts(
        context: IrClass
    ): Set<IrClass> {
        return indexer.classIndices(READER_IMPL_TAG)
            .filter { clazz ->
                clazz.getClassFromAnnotation(
                    InjektFqNames.ReaderImpl,
                    1
                ) == context
            }
            .map {
                it.getClassFromAnnotation(
                    InjektFqNames.ReaderImpl,
                    0
                )!!
            }
            .toSet()
    }

    fun getAllContextImplementations(
        context: IrClass
    ): Set<IrClass> {
        val contexts = mutableSetOf<IrClass>()

        contexts += context

        val processedClasses = mutableSetOf<IrClass>()

        fun collectImplementations(context: IrClass) {
            if (context in processedClasses) return
            processedClasses += context

            indexer.classIndices(READER_IMPL_TAG)
                .filter { clazz ->
                    clazz.getClassFromAnnotation(
                        InjektFqNames.ReaderImpl,
                        0
                    ) == context
                }
                .map {
                    it.getClassFromAnnotation(
                        InjektFqNames.ReaderImpl,
                        1
                    )!!
                }
                .forEach {
                    contexts += it
                    collectImplementations(it)
                }

            indexer.classIndices(READER_INVOCATION_TAG)
                .filter { clazz ->
                    clazz.getClassFromAnnotation(
                        InjektFqNames.ReaderInvocation,
                        1
                    ) == context && clazz.getConstantFromAnnotationOrNull<Boolean>(
                        InjektFqNames.ReaderInvocation, 2
                    )!!
                }
                .map {
                    it.getClassFromAnnotation(
                        InjektFqNames.ReaderInvocation,
                        0
                    )!!
                }
                .forEach {
                    contexts += it
                    collectImplementations(it)
                }

            context.superTypes
                .map { it.classOrNull!!.owner }
                .forEach {
                    contexts += it
                    collectImplementations(it)
                }
        }

        collectImplementations(context)

        return contexts
    }

    private fun collectRunReaderContexts() {
        indexer.classIndices(RUN_READER_CONTEXT_TAG)
            .distinct()
            .forEach { _runReaderContexts += it }
    }

    private fun collectBindings() {
        (indexer.functionIndices(BINDING_TAG) + indexer.classIndices(BINDING_TAG)
            .flatMapFix { it.constructors.toList() } +
                indexer.propertyIndices(BINDING_TAG).mapNotNull { it.getter })
            .map { implicitContextParamTransformer.getTransformedFunction(it) }
            .filter { it.getContext() != null }
            .distinct()
            .forEach { _bindings += it }
    }

    private fun collectMapEntries() {
        indexer.functionIndices(MAP_ENTRIES_TAG)
            .map { implicitContextParamTransformer.getTransformedFunction(it) }
            .filter { it.getContext() != null }
            .distinct()
            .forEach { _mapEntries += it }
    }

    private fun collectSetElements() {
        indexer.functionIndices(SET_ELEMENTS_TAG)
            .map { implicitContextParamTransformer.getTransformedFunction(it) }
            .filter { it.getContext() != null }
            .distinct()
            .forEach { _setElements += it }
    }

    private fun collectGenericContexts() {
        indexer.classIndices(GENERIC_CONTEXT_TAG)
            .distinct()
            .forEach { _genericContexts += it }
    }

    companion object {
        const val READER_INVOCATION_TAG = "reader_invocation"
        const val READER_IMPL_TAG = "reader_impl"
        const val RUN_READER_CONTEXT_TAG = "run_reader_context"
        const val BINDING_TAG = "binding"
        const val GENERIC_CONTEXT_TAG = "generic_context"
        const val MAP_ENTRIES_TAG = "map_entries"
        const val SET_ELEMENTS_TAG = "set_elements"
        const val SIGNATURE_TAG = "signature"
    }

}
