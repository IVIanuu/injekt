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

package com.ivianuu.injekt.compiler.generator.readercontextimpl

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.generator.DeclarationStore
import com.ivianuu.injekt.compiler.generator.ReaderContextDescriptor
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.render
import com.ivianuu.injekt.compiler.generator.uniqueTypeName
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.name.FqName

@Given
class GivensGraph(
    private val owner: ContextImpl
) {

    private val parent = owner.graph

    private val statements = owner.statements
    private val inputs = owner.inputTypes

    private val declarationStore = given<DeclarationStore>()

    private val contextId = owner.contextId

    private val instanceNodes = inputs
        .mapIndexed { index, inputType -> InstanceGivenNode(inputType, "p$index", owner) }
        .groupBy { it.type }

    //private val inputFunctionNodes = mutableMapOf<Key, MutableSet<Given>>()
    //private val inputGivenMapEntries = mutableMapOf<Key, MutableSet<IrFunction>>()
    //private val inputGivenSetElements = mutableMapOf<Key, MutableSet<IrFunction>>()

    val resolvedGivens = mutableMapOf<TypeRef, GivenNode>()

    sealed class ChainElement {
        class Given(val type: TypeRef) : ChainElement() {
            override fun toString() = type.toString()
        }

        class Call(val fqName: FqName?) : ChainElement() {
            override fun toString() = fqName.orUnknown()
        }
    }

    private val chain = mutableListOf<ChainElement>()
    private val checkedTypes = mutableSetOf<TypeRef>()

    init {
        /**val givenSets = inputs
        .filter { it.type.classOrNull!!.owner.hasAnnotation(InjektFqNames.GivenSet) }
        .associateWith { it.type.classOrNull!!.owner }

        fun IrClass.collectGivens(
        parentAccessStatement: ContextStatement?,
        parentAccessFunction: IrFunction?
        ) {
        val functions = (functions
        .toList() + properties
        .map { it.getter!! })
        .map { given<ReaderContextParamTransformer>().getTransformedFunction(it) }

        val thisAccessStatement: ContextStatement = { c ->
        if (parentAccessStatement == null) {
        irGetField(
        c[contextImpl],
        contextImpl.fields
        .single { it.type.classOrNull!!.owner == this@collectGivens }
        )
        } else {
        inputFunctionNodes
        irCall(parentAccessFunction!!).apply {
        dispatchReceiver = parentAccessStatement(c)
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
        type = function.returnType.asKey(),
        owner = contextImpl,
                            contexts = listOf(function.getContext()!!.defaultType),
                            external = function.isExternalDeclaration(),
                            explicitParameters = explicitParameters,
                            origin = function.descriptor.fqNameSafe,
                            function = function,
        targetContext = targetContext,
        givenSetAccessExpression = thisAccessStatement
        )
                    }
                    functionOrPropertyHasAnnotation(InjektFqNames.GivenSet) -> {
        function.returnType.classOrNull!!.owner.collectGivens(
        thisAccessStatement,
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
        }*/
    }

    fun checkEntryPoints(entryPoints: List<ReaderContextDescriptor>) {
        entryPoints.forEach { check(it) }
    }

    private fun check(type: TypeRef) {
        if (type in checkedTypes) return
        chain.push(ChainElement.Given(type))
        check(getGiven(type))
        chain.pop()
    }

    private fun check(given: GivenNode) {
        if (given.type in checkedTypes) return
        checkedTypes += given.type
        given
            .contexts
            .forEach { check(it) }
    }

    private fun check(context: ReaderContextDescriptor) {
        if (context in checkedTypes) return
        checkedTypes += context.type

        val origin: FqName?/* = context.classOrNull!!.owner.getConstantFromAnnotationOrNull<String>(
            InjektFqNames.Origin,
            0
        )?.let { FqName(it) }*/ // todo
        //chain.push(ChainElement.Call(origin))
        context.givenTypes.forEach { givenType ->
            val existingFunction = owner.members.singleOrNull {
                it is ContextFunction && it.name == givenType.uniqueTypeName()
            }
            if (existingFunction != null) return@forEach
            check(givenType)
        }
        //chain.pop()
    }

    fun getGiven(type: TypeRef): GivenNode {
        var given = getGivenOrNull(type)
        if (given != null) return given

        if (type.isMarkedNullable) {
            given = NullGivenNode(type, owner)
            resolvedGivens[type] = given
            return given
        }

        error(
            buildString {
                var indendation = ""
                fun indent() {
                    indendation = "$indendation    "
                }
                appendLine("No given found for '${type.render()}' in '${contextId.render()}':")

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

    private fun getGivenOrNull(type: TypeRef): GivenNode? {
        var given = resolvedGivens[type]
        if (given != null) return given

        fun GivenNode.check(): GivenNode? {
            if (targetContext != null && targetContext != contextId) {
                if (parent == null) {
                    error(
                        "Context mismatch, given '${type}' " +
                                "is scoped to '${targetContext!!.render()}' but actual context is " +
                                contextId.render()
                    )
                } else {
                    return null
                }
            }

            return this
        }

        val allGivens = givensForKey(type)

        val instanceAndGivenSetGivens = allGivens
            .filter { it.type == type }
            .filter { it is InstanceGivenNode || it.givenSetAccessStatement != null }

        if (instanceAndGivenSetGivens.size > 1) {
            error(
                "Multiple givens found in inputs for '${type}' at:\n${
                    instanceAndGivenSetGivens
                        .joinToString("\n") { "    '${it.origin.orUnknown()}'" }
                }"
            )
        }

        given = instanceAndGivenSetGivens.singleOrNull()
        given?.check()?.let {
            resolvedGivens[type] = it
            check(it)
            return it
        }

        val (internalGlobalGivens, externalGlobalGivens) = allGivens
            .filterNot { it is InstanceGivenNode }
            .filter { it.givenSetAccessStatement == null }
            .filter { it.type == type }
            .partition { !it.external }

        if (internalGlobalGivens.size > 1) {
            error(
                "Multiple internal givens found for '${type}' at:\n${
                    internalGlobalGivens
                        .joinToString("\n") { "    '${it to it.origin.orUnknown()}'" }
                }"
            )
        }

        given = internalGlobalGivens.singleOrNull()
        given?.check()?.let {
            resolvedGivens[type] = it
            check(it)
            // todo (it as? ChildContextGivenNode)?.childContextFactoryImpl
            // todo (it as? GivenCalleeContext)?.contextImpl
            return it
        }

        if (externalGlobalGivens.size > 1) {
            error(
                "Multiple external givens found for '${type}' at:\n${
                    externalGlobalGivens
                        .joinToString("\n") { "    '${it.origin.orUnknown()}'" }
                }.\nPlease specify a given for the requested type in this project."
            )
        }

        given = externalGlobalGivens.singleOrNull()
        given?.check()?.let {
            resolvedGivens[type] = it
            check(it)
            return it
        }

        parent?.getGivenOrNull(type)?.let {
            resolvedGivens[type] = it
            return it
        }

        return null
    }

    private fun givensForKey(type: TypeRef): List<GivenNode> = buildList<GivenNode> {
        instanceNodes[type]?.let { this += it }

        /*inputFunctionNodes[type]?.let { this += it }

        if (type.type.classOrNull!!.owner == contextId) {
            this += GivenSelfContext(
                type = type,
                context = contextImpl
            )
        }

        if (type.type.classOrNull!!.owner.hasAnnotation(InjektFqNames.ContextMarker) ||
            type.type.classOrNull!!.owner.name.asString().endsWith("__Context")
        ) {
            val contexts = mutableListOf<IrType>()
            this += GivenCalleeContext(
                type = type,
                owner = contextImpl,
                origin = null,
                lazyContexts = { contexts },
                lazyContextImpl = {
                    if (type.type.classOrNull!!.owner.typeParameters.isNotEmpty()) {
                        val childContextImpl = buildClass {
                            this.name = (KotlinTypeRef(type.type.toKotlinType()).uniqueTypeName()
                                .asString() + "Impl").asNameId()
                            visibility = Visibilities.PRIVATE
                        }.apply clazz@{
                            parent = contextImpl
                            contextImpl.addChild(this)
                            createImplicitParameterDeclarationWithWrappedDescriptor()
                        }

                        val parentField = childContextImpl.addField(
                            "parent",
                            contextImpl.defaultType
                        )

                        childContextImpl.addConstructor {
                            returnType = childContextImpl.defaultType
                            isPrimary = true
                            visibility = Visibilities.PUBLIC
                        }.apply {
                            val parentValueParameter = addValueParameter(
                                "parent",
                                contextImpl.defaultType
                            )
                            body = irBuilder().irBlockBody {
                                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.constructors.single().owner)
                                +IrInstanceInitializerCallImpl(
                                    UNDEFINED_OFFSET,
                                    UNDEFINED_OFFSET,
                                    childContextImpl.symbol,
                                    context.irBuiltIns.unitType
                                )
                                +irSetField(
                                    irGet(childContextImpl.thisReceiver!!),
                                    parentField,
                                    irGet(parentValueParameter)
                                )
                            }
                        }

                        childContextImpl.superTypes += type.type
                        type.type.visitAllFunctionsWithSubstitutionMap { function, substitutionMap ->
                            val functionKey = function.returnType
                                .substitute(substitutionMap)
                                .asKey()

                            val existingDeclaration = childContextImpl.functions.singleOrNull {
                                it.name == function.name
                            }
                            if (existingDeclaration != null) {
                                existingDeclaration.overriddenSymbols +=
                                    function.symbol as IrSimpleFunctionSymbol
                                return@visitAllFunctionsWithSubstitutionMap
                            }

                            val expression =
                                expressions.getGivenExpression(getGiven(functionKey), function)
                            childContextImpl.addFunction {
                                this.name = function.name
                                returnType = functionKey.type
                            }.apply {
                                dispatchReceiverParameter =
                                    childContextImpl.thisReceiver!!.copyTo(this)
                                overriddenSymbols += function.symbol as IrSimpleFunctionSymbol
                                body = irBuilder().run {
                                    irExprBody(
                                        expression(
                                            this,
                                            ContextExpressionContext(contextImpl) {
                                                irGetField(
                                                    irGet(dispatchReceiverParameter!!),
                                                    parentField
                                                )
                                            }
                                        )
                                    )
                                }
                            }
                        }
                        childContextImpl
                    } else {
                        contexts += type.type
                        type.type.visitAllFunctionsWithSubstitutionMap { function, substitutionMap ->
                            val functionKey = function.returnType
                                .substitute(substitutionMap)
                                .asKey()
                            expressions.getGivenExpression(getGiven(functionKey), function)
                        }
                        null
                    }
                }
            )
        }

        if (type.type.classOrNull!!.owner.hasAnnotation(InjektFqNames.ChildContextFactory)) {
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
            if (type.type.classOrNull!!.owner !in existingFactories) {
                val factory = type.type.classOrNull!!.owner
                val generator = ReaderContextFactoryImplGenerator(
                    name = expressions.uniqueChildNameProvider("F").asNameId(),
                    factoryInterface = factory,
                    factoryType = type.type,
                    irParent = contextImpl,
                    parentContext = contextImpl,
                    parentGraph = this@GivensGraph,
                    parentExpressions = expressions
                )

                this += GivenChildContext(
                    type = type,
                    owner = contextImpl,
                    origin = null,
                    generator = generator
                )
            }
        }

        this += declarationStore.givens(type)
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

                val explicitParameters = listOfNotNull(function.extensionReceiverParameter) +
                        function.valueParameters
                            .filter { it != function.getContextValueParameter() }

                GivenFunction(
                    type = type,
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
            .filter { it.targetContext == null || it.targetContext == contextId }

        (declarationStore.givenMapEntries(type) +
                inputGivenMapEntries.getOrElse(type) { emptySet() })
            .takeIf { it.isNotEmpty() }
            ?.let { entries ->
                GivenMap(
                    type = type,
                    owner = contextImpl,
                    contexts = entries.map { it.getContext()!!.defaultType },
                    givenSetAccessExpression = null,
                    functions = entries
                )
            }
            ?.let { this += it }

        (declarationStore.givenSetElements(type) +
                inputGivenSetElements.getOrElse(type) { emptySet() })
            .takeIf { it.isNotEmpty() }
            ?.let { elements ->
                GivenSet(
                    type = type,
                    owner = contextImpl,
                    contexts = elements.map { it.getContext()!!.defaultType },
                    givenSetAccessExpression = null,
                    functions = elements
                )
            }
            ?.let { this += it }*/
    }

}

private fun FqName?.orUnknown(): String = this?.asString() ?: "unknown origin"
