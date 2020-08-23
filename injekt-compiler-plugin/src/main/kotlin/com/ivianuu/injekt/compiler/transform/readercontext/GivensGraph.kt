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

package com.ivianuu.injekt.compiler.transform.readercontext

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.getAllFunctionsWithSubstitutionMap
import com.ivianuu.injekt.compiler.getClassFromAnnotation
import com.ivianuu.injekt.compiler.getConstantFromAnnotationOrNull
import com.ivianuu.injekt.compiler.getContext
import com.ivianuu.injekt.compiler.getContextValueParameter
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.substitute
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import com.ivianuu.injekt.compiler.transform.InjektContext
import com.ivianuu.injekt.compiler.transform.ReaderContextParamTransformer
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.uniqueTypeName
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class GivensGraph(
    private val parent: GivensGraph?,
    private val injektContext: InjektContext,
    private val declarationGraph: DeclarationGraph,
    private val contextImpl: IrClass,
    private val expressions: GivenExpressions,
    val inputs: List<IrField>,
    private val readerContextParamTransformer: ReaderContextParamTransformer
) {

    private val instanceNodes = inputs
        .map { GivenInstance(it, contextImpl) }
        .groupBy { it.key }

    private val inputFunctionNodes = mutableMapOf<Key, MutableSet<Given>>()
    private val inputGivenMapEntries = mutableMapOf<Key, MutableSet<IrFunction>>()
    private val inputGivenSetElements = mutableMapOf<Key, MutableSet<IrFunction>>()

    val resolvedGivens = mutableMapOf<Key, Given>()

    sealed class ChainElement {
        class Given(val key: Key) : ChainElement() {
            override fun toString() = key.toString()
        }

        class Call(val fqName: FqName?) : ChainElement() {
            override fun toString() = fqName.orUnknown()
        }
    }

    private val chain = mutableListOf<ChainElement>()
    private val checkedKeys = mutableSetOf<Key>()
    private val checkedTypes = mutableSetOf<IrType>()

    private val substitutionMap = contextImpl.superTypes.first()
        .let {
            it.classOrNull!!.owner
                .typeParameters
                .map { it.symbol }
                .zip(it.typeArguments)
                .toMap()
        }

    init {
        val givenSets = inputs
            .filter { it.type.classOrNull!!.owner.hasAnnotation(InjektFqNames.GivenSet) }
            .associateWith { it.type.classOrNull!!.owner }

        fun IrClass.collectGivens(
            parentAccessExpression: ContextExpression?,
            parentAccessFunction: IrFunction?
        ) {
            val functions = (functions
                .toList() + properties
                .map { it.getter!! })
                .map { readerContextParamTransformer.getTransformedFunction(it) }

            val thisAccessExpression: ContextExpression = { c ->
                if (parentAccessExpression == null) {
                    irGetField(
                        c[contextImpl],
                        contextImpl.fields
                            .single { it.type.classOrNull!!.owner == this@collectGivens }
                    )
                } else {
                    inputFunctionNodes
                    irCall(parentAccessFunction!!).apply {
                        dispatchReceiver = parentAccessExpression(c)
                    }
                }
            }

            for (function in functions) {
                fun functionOrPropertyHasAnnotation(fqName: FqName) =
                    function.hasAnnotation(fqName) ||
                            (function is IrSimpleFunction && function.correspondingPropertySymbol?.owner?.hasAnnotation(
                                fqName
                            ) == true)

                when {
                    functionOrPropertyHasAnnotation(InjektFqNames.Given) -> {
                        val targetContext = (function.getClassFromAnnotation(
                            InjektFqNames.Given, 0
                        )
                            ?: (if (function is IrConstructor) function.constructedClass.getClassFromAnnotation(
                                InjektFqNames.Given, 0
                            ) else null)
                            ?: if (function is IrSimpleFunction) function.correspondingPropertySymbol
                                ?.owner?.getClassFromAnnotation(InjektFqNames.Given, 0) else null)
                            ?.takeUnless { it.defaultType.isNothing() }

                        val explicitParameters = function.valueParameters
                            .filter { it != function.getContextValueParameter() }

                        inputFunctionNodes.getOrPut(function.returnType.asKey()) { mutableSetOf() } += GivenFunction(
                            key = function.returnType.asKey(),
                            owner = contextImpl,
                            contexts = listOf(function.getContext()!!.defaultType),
                            external = function.isExternalDeclaration(),
                            explicitParameters = explicitParameters,
                            origin = function.descriptor.fqNameSafe,
                            function = function,
                            targetContext = targetContext,
                            givenSetAccessExpression = thisAccessExpression
                        )
                    }
                    functionOrPropertyHasAnnotation(InjektFqNames.GivenSet) -> {
                        function.returnType.classOrNull!!.owner.collectGivens(
                            thisAccessExpression,
                            function
                        )
                    }
                    functionOrPropertyHasAnnotation(InjektFqNames.GivenMapEntries) -> {
                        inputGivenMapEntries.getOrPut(function.returnType.asKey()) { mutableSetOf() } += function
                    }
                    functionOrPropertyHasAnnotation(InjektFqNames.GivenSetElements) -> {
                        inputGivenSetElements.getOrPut(function.returnType.asKey()) { mutableSetOf() } += function
                    }
                }
            }
        }

        givenSets.values.forEach {
            it.collectGivens(null, null)
        }
    }

    fun checkEntryPoints(entryPoints: List<IrType>) {
        entryPoints.forEach { check(it) }
    }

    private fun check(key: Key) {
        if (key in checkedKeys) return
        chain.push(ChainElement.Given(key))
        check(getGiven(key))
        chain.pop()
    }

    private fun check(given: Given) {
        if (given.key in checkedKeys) return
        checkedKeys += given.key
        given
            .contexts
            .flatMap {
                listOf(it) + declarationGraph.getAllContextImplementations(it.classOrNull!!.owner)
                    .drop(1)
                    .map { it.defaultType }
            }
            .forEach { check(it) }
    }

    private fun check(context: IrType) {
        if (context in checkedTypes) return
        checkedTypes += context

        println("check ${context.render()}")

        val origin = context.classOrNull!!.owner.getConstantFromAnnotationOrNull<String>(
            InjektFqNames.Origin,
            0
        )?.let { FqName(it) }
        chain.push(ChainElement.Call(origin))

        (listOf(context) + declarationGraph.getAllContextImplementations(context.classOrNull!!.owner)
            .drop(1).map { it.defaultType })
            .flatMap { it.getAllFunctionsWithSubstitutionMap(injektContext).toList() }
            .forEach { (function, substitutionMap) ->
                val existingFunction = contextImpl.functions.singleOrNull {
                    it.name == function.name
                }
                if (existingFunction != null) {
                    existingFunction.overriddenSymbols += function.symbol as IrSimpleFunctionSymbol
                    return@forEach
                }

                check(function.returnType.substitute(substitutionMap).asKey())
            }

        chain.pop()
    }

    fun getGiven(key: Key): Given {
        var given = getGivenOrNull(key)
        if (given != null) return given

        if (key.type.isMarkedNullable()) {
            given = GivenNull(key, contextImpl)
            resolvedGivens[key] = given
            return given
        }

        error(
            buildString {
                var indendation = ""
                fun indent() {
                    indendation = "$indendation    "
                }
                appendLine(
                    "No given found for '${key}' in '${
                        contextImpl.superTypes.first().render()
                    }':"
                )

                chain.forEachIndexed { index, element ->
                    if (index == 0) {
                        appendLine("${indendation}runReader call '${element}'")
                    } else {
                        when (element) {
                            is ChainElement.Call -> {
                                val lastElement = chain.getOrNull(index - 1)
                                if (lastElement is ChainElement.Given) {
                                    appendLine("${indendation}given by '${element}'")
                                } else {
                                    appendLine("${indendation}calls reader '${element}'")
                                }
                            }
                            is ChainElement.Given -> appendLine("${indendation}requires given '${element}'")
                        }

                    }
                    indent()
                }
            }
        )
    }

    private fun getGivenOrNull(key: Key): Given? {
        var given = resolvedGivens[key]
        if (given != null) return given

        fun Given.check(): Given? {
            if (targetContext != null && targetContext != contextImpl.superTypes.first().classOrNull!!.owner) {
                if (parent == null) {
                    error(
                        "Context mismatch, given '${key}' " +
                                "is scoped to '${targetContext.defaultType.render()}' but actual context is " +
                                contextImpl.superTypes.first().render()
                    )
                } else {
                    return null
                }
            }

            return this
        }

        val allGivens = givensForKey(key)

        val instanceAndGivenSetGivens = allGivens
            .filter { it.key == key }
            .filter { it is GivenInstance || it.givenSetAccessExpression != null }

        if (instanceAndGivenSetGivens.size > 1) {
            error(
                "Multiple givens found in inputs for '${key}' at:\n${
                    instanceAndGivenSetGivens
                        .joinToString("\n") { "    '${it.origin.orUnknown()}'" }
                }"
            )
        }

        given = instanceAndGivenSetGivens.singleOrNull()
        given?.check()?.let {
            resolvedGivens[key] = it
            check(it)
            return it
        }

        val (internalGlobalGivens, externalGlobalGivens) = allGivens
            .filterNot { it is GivenInstance }
            .filter { it.givenSetAccessExpression == null }
            .filter { it.key == key }
            .partition { !it.external }

        if (internalGlobalGivens.size > 1) {
            error(
                "Multiple internal givens found for '${key}' at:\n${
                    internalGlobalGivens
                        .joinToString("\n") { "    '${it.origin.orUnknown()}'" }
                }"
            )
        }

        given = internalGlobalGivens.singleOrNull()
        given?.check()?.let {
            resolvedGivens[key] = it
            check(it)
            (it as? GivenChildContext)?.factory
            return it
        }

        if (externalGlobalGivens.size > 1) {
            error(
                "Multiple external givens found for '${key}' at:\n${
                    externalGlobalGivens
                        .joinToString("\n") { "    '${it.origin.orUnknown()}'" }
                }.\nPlease specify a given for the requested type in this project."
            )
        }

        given = externalGlobalGivens.singleOrNull()
        given?.check()?.let {
            resolvedGivens[key] = it
            check(it)
            return it
        }

        parent?.getGivenOrNull(key)?.let {
            resolvedGivens[key] = it
            return it
        }

        return null
    }

    private fun givensForKey(key: Key): List<Given> = buildList<Given> {
        instanceNodes[key]?.let { this += it }

        inputFunctionNodes[key]?.let { this += it }

        if (key.type.classOrNull!!.owner.hasAnnotation(InjektFqNames.ChildContextFactory)) {
            val existingFactories = mutableSetOf<IrClass>()
            var currentContext: IrClass? = contextImpl
            while (currentContext != null) {
                existingFactories += (currentContext.parent as IrClass)
                    .superTypes.first().classOrNull!!.owner
                currentContext = currentContext.fields
                    .singleOrNull { it.name.asString() == "parent" }
                    ?.type
                    ?.classOrNull
                    ?.owner
            }
            if (key.type.classOrNull!!.owner !in existingFactories) {
                val factory = key.type.classOrNull!!.owner
                val generator = ReaderContextFactoryImplGenerator(
                    injektContext = injektContext,
                    name = expressions.uniqueChildNameProvider("F".asNameId()),
                    factoryInterface = factory,
                    factoryType = key.type,
                    irParent = contextImpl,
                    declarationGraph = declarationGraph,
                    readerContextParamTransformer = readerContextParamTransformer,
                    parentContext = contextImpl,
                    parentGraph = this@GivensGraph,
                    parentExpressions = expressions
                )

                this += GivenChildContext(
                    key = key,
                    owner = contextImpl,
                    contexts = emptyList(),
                    origin = null,
                    generator = generator
                )
            }
        }

        this += declarationGraph.givens(key.type)
            .map { function ->
                val targetContext = (function.getClassFromAnnotation(
                    InjektFqNames.Given, 0
                )
                    ?: (if (function is IrConstructor) function.constructedClass.getClassFromAnnotation(
                        InjektFqNames.Given, 0
                    ) else null)
                    ?: if (function is IrSimpleFunction) function.correspondingPropertySymbol
                        ?.owner?.getClassFromAnnotation(InjektFqNames.Given, 0) else null)
                    ?.takeUnless { it.defaultType.isNothing() }

                val explicitParameters = function.valueParameters
                    .filter { it != function.getContextValueParameter() }

                GivenFunction(
                    key = key,
                    owner = contextImpl,
                    contexts = listOf(function.getContext()!!.defaultType),
                    external = function.isExternalDeclaration(),
                    explicitParameters = explicitParameters,
                    origin = function.descriptor.fqNameSafe,
                    function = function,
                    targetContext = targetContext,
                    givenSetAccessExpression = null
                )
            }

        (declarationGraph.givenMapEntries(key.type.uniqueTypeName().asString()) +
                inputGivenMapEntries.getOrElse(key) { emptySet() })
            .takeIf { it.isNotEmpty() }
            ?.let { entries ->
                GivenMap(
                    key = key,
                    owner = contextImpl,
                    contexts = entries.map { it.getContext()!!.defaultType },
                    givenSetAccessExpression = null,
                    functions = entries
                )
            }
            ?.let { this += it }

        (declarationGraph.givenSetElements(key.type.uniqueTypeName().asString()) +
                inputGivenSetElements.getOrElse(key) { emptySet() })
            .takeIf { it.isNotEmpty() }
            ?.let { elements ->
                GivenSet(
                    key = key,
                    owner = contextImpl,
                    contexts = elements.map { it.getContext()!!.defaultType },
                    givenSetAccessExpression = null,
                    functions = elements
                )
            }
            ?.let { this += it }
    }
}
