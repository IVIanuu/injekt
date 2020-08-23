package com.ivianuu.injekt.compiler.transform.readercontextimpl

import com.ivianuu.injekt.compiler.SimpleUniqueNameProvider
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.getAllFunctionsWithSubstitutionMap
import com.ivianuu.injekt.compiler.substitute
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import com.ivianuu.injekt.compiler.transform.InjektContext
import com.ivianuu.injekt.compiler.transform.ReaderContextParamTransformer
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import com.ivianuu.injekt.compiler.uniqueTypeName
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.name.Name

class ReaderContextFactoryImplGenerator(
    private val injektContext: InjektContext,
    private val name: Name,
    private val factoryInterface: IrClass,
    private val factoryType: IrType,
    private val irParent: IrDeclarationParent,
    private val declarationGraph: DeclarationGraph,
    private val readerContextParamTransformer: ReaderContextParamTransformer,
    private val parentContext: IrClass?,
    private val parentGraph: GivensGraph?,
    private val parentExpressions: GivenExpressions?
) {

    fun generateFactory(): IrClass {
        val createFunction = factoryInterface.functions
            .first { it.name.asString().startsWith("create") }

        val inputTypes = createFunction
            .valueParameters
            .map { it.type }
            .map {
                it.substitute(
                    factoryInterface.typeParameters
                        .map { it.symbol }
                        .zip(factoryType.typeArguments.map { it.typeOrFail })
                        .toMap()
                )
            }

        val contextIdType = createFunction.returnType
            .substitute(
                factoryInterface.typeParameters
                    .map { it.symbol }
                    .zip(factoryType.typeArguments.map { it.typeOrFail })
                    .toMap()
            )

        val factoryImpl = buildClass {
            this.name = this@ReaderContextFactoryImplGenerator.name
            if (parentContext == null) kind = ClassKind.OBJECT
            visibility = if (parentContext != null) Visibilities.PRIVATE else Visibilities.INTERNAL
        }.apply clazz@{
            parent = irParent
            createImplicitParameterDeclarationWithWrappedDescriptor()
            superTypes += factoryType
        }

        val parentField = if (parentContext != null) {
            factoryImpl.addField("parent", parentContext.defaultType)
        } else null

        factoryImpl.addConstructor {
            returnType = factoryImpl.defaultType
            isPrimary = true
            visibility = Visibilities.PUBLIC
        }.apply {
            val parentValueParameter = if (parentField != null) {
                addValueParameter(parentField.name.asString(), parentField.type)
            } else null

            body = DeclarationIrBuilder(
                injektContext,
                symbol
            ).irBlockBody {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.constructors.single().owner)
                +IrInstanceInitializerCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    factoryImpl.symbol,
                    context.irBuiltIns.unitType
                )
                if (parentValueParameter != null) {
                    +irSetField(
                        irGet(factoryImpl.thisReceiver!!),
                        parentField!!,
                        irGet(parentValueParameter)
                    )
                }
            }
        }

        val contextImpl = generateReaderContext(contextIdType, inputTypes, factoryImpl)
        factoryImpl.addChild(contextImpl)

        factoryImpl.addFunction {
            name = "create".asNameId()
            returnType = contextIdType
        }.apply {
            dispatchReceiverParameter = factoryImpl.thisReceiver!!.copyTo(this)

            overriddenSymbols += createFunction.symbol

            val inputNameProvider = SimpleUniqueNameProvider()
            inputTypes.forEach {
                addValueParameter(
                    inputNameProvider(it.uniqueTypeName()).asString(),
                    it
                )
            }

            body = DeclarationIrBuilder(injektContext, symbol).run {
                irExprBody(
                    if (contextImpl.isObject) {
                        irGetObject(contextImpl.symbol)
                    } else {
                        val contextImplInputs = contextImpl.constructors.single()
                            .valueParameters
                            .drop(if (parentContext != null) 1 else 0)
                            .map { it.type }
                        irCall(contextImpl.constructors.single()).apply {
                            if (parentContext != null) {
                                putValueArgument(
                                    0,
                                    irGetField(
                                        irGet(dispatchReceiverParameter!!),
                                        parentField!!
                                    )
                                )
                            }
                            contextImplInputs.forEachIndexed { index, type ->
                                putValueArgument(
                                    index + if (parentContext != null) 1 else 0,
                                    irGet(valueParameters[index])
                                )
                            }
                        }
                    }
                )
            }
        }

        return factoryImpl
    }

    private fun generateReaderContext(
        contextIdType: IrType,
        inputTypes: List<IrType>,
        irParent: IrDeclarationParent
    ): IrClass {
        val contextImpl = buildClass {
            this.name = "C".asNameId()
            visibility = Visibilities.PRIVATE
            if (parentContext == null && inputTypes.isEmpty()) kind = ClassKind.OBJECT
        }.apply clazz@{
            parent = irParent
            createImplicitParameterDeclarationWithWrappedDescriptor()
            superTypes += contextIdType
        }

        val parentField = if (parentContext != null) {
            contextImpl.addField("parent", parentContext.defaultType)
        } else null

        val inputFieldNameProvider = SimpleUniqueNameProvider()
        val inputFields = inputTypes
            .map {
                contextImpl.addField(
                    inputFieldNameProvider(it.uniqueTypeName()),
                    it
                )
            }

        contextImpl.addConstructor {
            returnType = contextImpl.defaultType
            isPrimary = true
            visibility = Visibilities.PUBLIC
        }.apply {
            val parentValueParameter = if (parentField != null) {
                addValueParameter(parentField.name.asString(), parentField.type)
            } else null

            val inputValueParameters = inputFields.associateWith {
                addValueParameter(
                    it.name.asString(),
                    it.type
                )
            }

            body = DeclarationIrBuilder(
                injektContext,
                symbol
            ).irBlockBody {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.constructors.single().owner)
                +IrInstanceInitializerCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    contextImpl.symbol,
                    context.irBuiltIns.unitType
                )
                if (parentValueParameter != null) {
                    +irSetField(
                        irGet(contextImpl.thisReceiver!!),
                        parentField!!,
                        irGet(parentValueParameter)
                    )
                }
                inputValueParameters.forEach { (field, valueParameter) ->
                    +irSetField(
                        irGet(contextImpl.thisReceiver!!),
                        field,
                        irGet(valueParameter)
                    )
                }
            }
        }

        val expressions = GivenExpressions(
            parent = parentExpressions,
            injektContext = injektContext,
            contextImpl = contextImpl
        )

        val graph = GivensGraph(
            parent = parentGraph,
            injektContext = injektContext,
            declarationGraph = declarationGraph,
            expressions = expressions,
            contextImpl = contextImpl,
            inputs = inputFields,
            readerContextParamTransformer = readerContextParamTransformer
        )

        val entryPoints = listOf(contextIdType)
            .flatMap {
                listOf(it) + declarationGraph.getAllContextImplementations(it.classOrNull!!.owner)
                    .drop(1)
                    .map { it.defaultType }
            } + declarationGraph.getRunReaderContexts(contextIdType.classOrNull!!.owner)
            .flatMap { declarationGraph.getAllContextImplementations(it) }
            .map { it.defaultType }

        graph.checkEntryPoints(entryPoints)

        (entryPoints + graph.resolvedGivens.flatMap { it.value.contexts })
            .flatMap {
                listOf(it) + declarationGraph.getAllContextImplementations(it.classOrNull!!.owner)
                    .drop(1)
                    .map { it.defaultType }
            }
            .onEach {
                if (it !in contextImpl.superTypes) {
                    contextImpl.superTypes += it
                }
            }
            .flatMap { it.getAllFunctionsWithSubstitutionMap(injektContext).toList() }
            .filter { (function, _) ->
                val existingDeclaration = contextImpl.functions.singleOrNull {
                    it.name == function.name
                }
                if (existingDeclaration != null) {
                    existingDeclaration.overriddenSymbols += function.symbol as IrSimpleFunctionSymbol
                    false
                } else true
            }
            .map { (function, substitutionMap) ->
                function to function.returnType
                    .substitute(substitutionMap)
                    .asKey()
            }
            .forEach { (superFunction, key) ->
                expressions.getGivenExpression(graph.getGiven(key), superFunction)
            }

        return contextImpl
    }
}
