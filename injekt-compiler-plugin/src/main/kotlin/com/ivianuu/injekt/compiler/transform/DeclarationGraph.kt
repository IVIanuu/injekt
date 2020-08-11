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

class DeclarationGraph(
    private val indexer: Indexer,
    val module: IrModuleFragment,
    private val implicitContextParamTransformer: ImplicitContextParamTransformer
) {

    val runReaderContexts: List<IrClass> by lazy {
        indexer.classIndices(RUN_READER_CONTEXT_TAG, "indices")
    }

    val genericContexts: List<IrClass> by lazy {
        indexer.classIndices(GENERIC_CONTEXT_TAG, "indices")
    }

    private val bindingsByKey = mutableMapOf<String, List<IrFunction>>()
    fun bindings(key: String) = bindingsByKey.getOrPut(key) {
        (indexer.functionIndices(GIVEN_TAG, key) +
                indexer.classIndices(GIVEN_TAG, key)
                    .flatMapFix { it.constructors.toList() } +
                indexer.propertyIndices(GIVEN_TAG, key)
                    .mapNotNull { it.getter }
                )
            .map { implicitContextParamTransformer.getTransformedFunction(it) }
            .filter { it.getContext() != null }
            .distinct()
    }

    private val mapEntriesByKey = mutableMapOf<String, List<IrFunction>>()
    fun mapEntries(key: String) = mapEntriesByKey.getOrPut(key) {
        indexer.functionIndices(MAP_ENTRIES_TAG, key)
            .map { implicitContextParamTransformer.getTransformedFunction(it) }
            .filter { it.getContext() != null }
    }

    private val setElementsByKey = mutableMapOf<String, List<IrFunction>>()
    fun setElements(key: String) = setElementsByKey.getOrPut(key) {
        indexer.functionIndices(SET_ELEMENTS_TAG, key)
            .map { implicitContextParamTransformer.getTransformedFunction(it) }
            .filter { it.getContext() != null }
    }

    lateinit var runReaderContextImplTransformer: RunReaderContextImplTransformer

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
        return indexer.classIndices(GIVEN_CONTEXTS, context.descriptor.fqNameSafe.asString())
            .flatMapFix {
                val key =
                    it.getConstantFromAnnotationOrNull<String>(InjektFqNames.GivenContext, 0)!!
                bindings(key) + mapEntries(key) + setElements(key)
            }
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
            *allContexts
                .flatMapFix {
                    indexer.classIndices(
                        READER_INVOCATION_CALLEE_TO_CALLER_TAG,
                        it.descriptor.fqNameSafe.asString()
                    )
                }
                .map {
                    it.getClassFromAnnotation(
                        InjektFqNames.ReaderInvocation,
                        0
                    )!!
                }
                .filter { it != context }
                .toTypedArray(),
            invokerIfRunChildReader
        )
    }

    private val superContextsByContext = mutableMapOf<IrClass, Set<IrClass>>()
    private fun getAllSuperContexts(
        context: IrClass
    ): Set<IrClass> = superContextsByContext.getOrPut(context) {
        indexer.classIndices(READER_IMPL_SUB_TO_SUPER_TAG, context.descriptor.fqNameSafe.asString())
            .map {
                it.getClassFromAnnotation(
                    InjektFqNames.ReaderImpl,
                    0
                )!!
            }
            .toSet()
    }

    private val implementationsByContext = mutableMapOf<IrClass, Set<IrClass>>()
    fun getAllContextImplementations(
        context: IrClass
    ): Set<IrClass> = implementationsByContext.getOrPut(context) {
        val contexts = mutableSetOf<IrClass>()

        contexts += context

        val processedClasses = mutableSetOf<IrClass>()

        fun collectImplementations(context: IrClass) {
            if (context in processedClasses) return
            processedClasses += context

            indexer.classIndices(
                READER_IMPL_SUPER_TO_SUB_TAG,
                context.descriptor.fqNameSafe.asString()
            )
                .map {
                    it.getClassFromAnnotation(
                        InjektFqNames.ReaderImpl,
                        0
                    )!!
                }
                .forEach {
                    contexts += it
                    collectImplementations(it)
                }

            indexer.classIndices(
                READER_INVOCATION_CALLER_TO_CALLEE_TAG,
                context.descriptor.fqNameSafe.asString()
            )
                .filter {
                    it.getConstantFromAnnotationOrNull<Boolean>(
                        InjektFqNames.ReaderInvocation,
                        1
                    )!!
                }
                .map { it.getClassFromAnnotation(InjektFqNames.ReaderInvocation, 0)!! }
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

        contexts
    }

    companion object {
        const val READER_INVOCATION_CALLEE_TO_CALLER_TAG = "readerinvocationcalleetocaller"
        const val READER_INVOCATION_CALLER_TO_CALLEE_TAG = "readerinvocationcallertocallee"
        const val READER_IMPL_SUPER_TO_SUB_TAG = "readerimplsupertosub"
        const val READER_IMPL_SUB_TO_SUPER_TAG = "readerimplsubtosuper"
        const val RUN_READER_CONTEXT_TAG = "runreadercontext"
        const val GIVEN_TAG = "given"
        const val GIVEN_CONTEXTS = "givencontexts"
        const val GENERIC_CONTEXT_TAG = "genericcontext"
        const val MAP_ENTRIES_TAG = "mapentries"
        const val SET_ELEMENTS_TAG = "setelements"
        const val SIGNATURE_TAG = "signature"
    }

}
