package com.ivianuu.injekt.compiler.transform.readercontext

import com.ivianuu.injekt.compiler.SimpleUniqueNameProvider
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.flatMapFix
import com.ivianuu.injekt.compiler.getAllClasses
import com.ivianuu.injekt.compiler.recordLookup
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import com.ivianuu.injekt.compiler.transform.InjektContext
import com.ivianuu.injekt.compiler.transform.implicit.ImplicitContextParamTransformer
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
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ReaderContextFactoryImplGenerator(
    private val injektContext: InjektContext,
    private val name: Name,
    private val factoryInterface: IrClass,
    private val irParent: IrDeclarationParent,
    private val declarationGraph: DeclarationGraph,
    private val implicitContextParamTransformer: ImplicitContextParamTransformer,
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

        val contextId = createFunction.returnType.classOrNull!!.owner

        val factoryImpl = buildClass {
            this.name = this@ReaderContextFactoryImplGenerator.name
            if (parentContext == null) kind = ClassKind.OBJECT
            visibility = if (parentContext != null) Visibilities.PRIVATE else Visibilities.INTERNAL
        }.apply clazz@{
            parent = irParent
            createImplicitParameterDeclarationWithWrappedDescriptor()
            superTypes += factoryInterface.defaultType
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

        val contextImpl = generateReaderContext(contextId, inputTypes, factoryImpl)
        factoryImpl.addChild(contextImpl)

        factoryImpl.addFunction {
            name = "create".asNameId()
            returnType = contextId.defaultType
        }.apply {
            dispatchReceiverParameter = factoryImpl.thisReceiver!!.copyTo(this)

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
        contextId: IrClass,
        inputTypes: List<IrType>,
        irParent: IrDeclarationParent
    ): IrClass {
        val contextImpl = buildClass {
            this.name = "ContextImpl".asNameId()
            visibility = Visibilities.PRIVATE
            if (parentContext == null && inputTypes.isEmpty()) kind = ClassKind.OBJECT
        }.apply clazz@{
            parent = irParent
            createImplicitParameterDeclarationWithWrappedDescriptor()
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

        val graph = GivensGraph(
            parent = parentGraph,
            declarationGraph = declarationGraph,
            contextImpl = contextImpl,
            inputs = inputFields,
            implicitContextParamTransformer = implicitContextParamTransformer
        )

        val expressions = GivenExpressions(
            parent = parentExpressions,
            injektContext = injektContext,
            contextImpl = contextImpl,
            declarationGraph = declarationGraph,
            givensGraph = graph,
            implicitContextParamTransformer = implicitContextParamTransformer
        )

        var firstRound = true

        while (true) {
            val superTypes =
                (if (firstRound) listOf(contextId) + declarationGraph.getRunReaderContexts(contextId)
                else graph.resolvedGivens.values
                    .flatMapFix { it.contexts }
                    .flatMapFix { it.getAllClasses() })
                    .flatMapFix { declarationGraph.getAllContextImplementations(it) }
                    .distinct()
                    .filter { it !in contextImpl.superTypes.map { it.classOrNull!!.owner } }

            if (superTypes.isEmpty()) break

            fun implement(superType: IrClass) {
                if (superType in contextImpl.superTypes.map { it.classOrNull!!.owner }) return
                contextImpl.superTypes += superType.defaultType
                recordLookup(contextImpl, superType)

                for (declaration in superType.declarations.toList()) {
                    if (declaration !is IrFunction) continue
                    if (declaration is IrConstructor) continue
                    if (declaration.dispatchReceiverParameter?.type ==
                        injektContext.irBuiltIns.anyType
                    ) continue
                    val existingDeclaration = contextImpl.functions.singleOrNull {
                        it.name == declaration.name
                    }
                    if (existingDeclaration != null) {
                        existingDeclaration.overriddenSymbols += declaration.symbol as IrSimpleFunctionSymbol
                        continue
                    }
                    val request =
                        GivenRequest(
                            declaration.returnType.asKey(),
                            null,
                            declaration.descriptor.fqNameSafe
                        )
                    expressions.getGivenExpression(graph.getGiven(request))
                }

                superType.superTypes
                    .map { it.classOrNull!!.owner }
                    .forEach { implement(it) }
            }

            superTypes.forEach { implement(it) }

            firstRound = false
        }

        return contextImpl
    }
}
