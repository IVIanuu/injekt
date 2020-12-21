package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.SourcePosition
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irUnit
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrBindableSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName

class GivenCallTransformer(private val pluginContext: IrPluginContext) : IrElementTransformerVoid() {

    private data class ResolutionContext(val graph: GivenGraph.Success) {
        val expressionsByRequest = mutableMapOf<GivenRequest, (() -> IrExpression)?>()
    }

    private fun ResolutionContext.fillGivens(
        callable: CallableRef,
        forFunExpression: Boolean,
        call: IrFunctionAccessExpression,
    ) {
        val requests = callable.getGivenRequests(forFunExpression)
        if (callable.callable.dispatchReceiverParameter != null && call.dispatchReceiver == null) {
            call.dispatchReceiver = expressionFor(
                requests.single { it.parameterName.asString() == "_dispatchReceiver" },
                call.symbol
            )
        }

        if (callable.callable.extensionReceiverParameter != null && call.extensionReceiver == null) {
            call.extensionReceiver = expressionFor(
                requests.single { it.parameterName.asString() == "_extensionReceiver" },
                call.symbol
            )
        }
        callable.callable
            .valueParameters
            .filter { call.getValueArgument(it.index) == null }
            .filter {
                it.givenKind() == GivenKind.VALUE ||
                        callable.parameterTypes[it]!!.givenKind == GivenKind.VALUE
            }
            .map { parameter ->
                parameter to expressionFor(
                    requests.single { it.parameterName == parameter.name },
                    call.symbol
                )
            }
            .forEach { call.putValueArgument(it.first.index, it.second) }
    }

    private fun ResolutionContext.expressionFor(
        request: GivenRequest,
        symbol: IrSymbol,
    ): IrExpression? {
        return expressionsByRequest.getOrPut(request) {
            val given = graph.givens[request]
                ?: error("Wtf $request\n${this.graph.givens.toList().joinToString("\n")}")
            when (given) {
                is CallableGivenNode -> callableExpression(given, symbol)
                is DefaultGivenNode -> null
                is FunGivenNode -> funExpression(given, symbol)
                is ObjectGivenNode -> objectExpression(given, symbol)
                is ProviderGivenNode -> providerExpression(given, symbol)
                is ProviderParameterGivenNode -> providerParameterExpression(given, symbol)
                is SetGivenNode -> setExpression(given, symbol)
            }
        }?.invoke()
    }

    private val lambdasByProviderGiven = mutableMapOf<ProviderGivenNode, IrFunction>()

    private fun ResolutionContext.funExpression(
        given: FunGivenNode,
        symbol: IrSymbol
    ): () -> IrExpression = {
        DeclarationIrBuilder(pluginContext, symbol)
            .irLambda(given.type.toIrType(pluginContext)) { function ->
                val givenFun = (given.callable.callable as FunctionDescriptor).irFunction()
                val typeArguments = getSubstitutionMap(
                    listOf(given.type to given.originalType),
                    given.type.classifier.typeParameters
                ).values
                DeclarationIrBuilder(pluginContext, symbol)
                    .irCall(givenFun.symbol)
                    .apply {
                        typeArguments
                            .forEachIndexed { index, typeArgument ->
                                putTypeArgument(index, typeArgument.toIrType(pluginContext))
                            }

                        givenFun
                            .valueParameters
                            .filterNot { it.descriptor.givenKind() == GivenKind.VALUE }
                            .forEachIndexed { index, valueParameter ->
                                putValueArgument(valueParameter.index,
                                    DeclarationIrBuilder(pluginContext, symbol)
                                        .irGet(function.valueParameters[index]))
                            }

                        fillGivens(given.callable, true, this)
                    }
            }
    }

    private fun ResolutionContext.objectExpression(
        given: ObjectGivenNode,
        symbol: IrSymbol,
    ): () -> IrExpression = {
        DeclarationIrBuilder(pluginContext, symbol)
            .irGetObject(pluginContext.referenceClass(given.type.classifier.fqName)!!)
    }

    private fun ResolutionContext.providerExpression(
        given: ProviderGivenNode,
        symbol: IrSymbol,
    ): () -> IrExpression = {
        DeclarationIrBuilder(pluginContext, symbol)
            .irLambda(given.type.toIrType(pluginContext)) { function ->
                lambdasByProviderGiven[given] = function
                expressionFor(given.dependencies.single(), symbol)
                    ?: DeclarationIrBuilder(pluginContext, symbol).irUnit()
            }
    }

    private fun ResolutionContext.providerParameterExpression(
        given: ProviderParameterGivenNode,
        symbol: IrSymbol,
    ): () -> IrExpression = {
        DeclarationIrBuilder(pluginContext, symbol)
            .irGet(lambdasByProviderGiven[given.provider]!!.valueParameters[given.index])
    }

    private fun ResolutionContext.setExpression(
        given: SetGivenNode,
        symbol: IrSymbol,
    ): () -> IrExpression = expr@{
        val elementType =
            given.type.fullyExpandedType.typeArguments.single()

        if (given.elements.isEmpty()) {
            val emptySet = pluginContext.referenceFunctions(
                FqName("kotlin.collections.emptySet")
            ).single()
            return@expr DeclarationIrBuilder(pluginContext, symbol)
                .irCall(emptySet)
                .apply { putTypeArgument(0, elementType.toIrType(pluginContext)) }
        }

        DeclarationIrBuilder(pluginContext, symbol).irBlock {
            val mutableSetOf = pluginContext.referenceFunctions(
                FqName("kotlin.collections.mutableSetOf")
            ).single { it.owner.valueParameters.isEmpty() }

            val setAddAll = mutableSetOf.owner.returnType
                .classOrNull!!
                .owner
                .functions
                .single { it.name.asString() == "add" }

            val tmpSet = irTemporary(
                irCall(mutableSetOf)
                    .apply { putTypeArgument(0, elementType.toIrType(pluginContext)) }
            )

            given.elements
                .forEach {
                    +irCall(setAddAll).apply {
                        dispatchReceiver = irGet(tmpSet)
                        putValueArgument(
                            0,
                            callableExpression(
                                it.toGivenNode(elementType, 0),
                                symbol
                            )()
                        )
                    }
                }

            +irGet(tmpSet)
        }
    }


    private fun ResolutionContext.callableExpression(
        given: CallableGivenNode,
        symbol: IrSymbol,
    ): () -> IrExpression = {
        when (given.callable.callable) {
            is ClassConstructorDescriptor -> classExpression(given.type,
                given.callable,
                given.callable.callable,
                symbol)
            is PropertyDescriptor -> propertyExpression(given.type,
                given.callable,
                given.callable.callable,
                symbol)
            is FunctionDescriptor -> functionExpression(given.type,
                given.callable,
                given.callable.callable,
                symbol)
            is ReceiverParameterDescriptor -> parameterExpression(given.callable.callable, symbol)
            is ValueParameterDescriptor -> parameterExpression(given.callable.callable, symbol)
            is VariableDescriptor -> variableExpression(given.callable.callable, symbol)
            else -> error("Unsupported callable $given")
        }
    }

    private fun ResolutionContext.classExpression(
        type: TypeRef,
        callable: CallableRef,
        descriptor: ClassConstructorDescriptor,
        symbol: IrSymbol,
    ): IrExpression {
        return if (descriptor.constructedClass.kind == ClassKind.OBJECT) {
            val clazz =
                pluginContext.symbolTable.referenceClass(descriptor.constructedClass.original)
                    .bind()
            DeclarationIrBuilder(pluginContext, symbol)
                .irGetObject(clazz)
        } else {
            val constructor =
                pluginContext.symbolTable.referenceConstructor(descriptor.original).bind()
                    .owner
            DeclarationIrBuilder(pluginContext, symbol)
                .irCall(constructor.symbol)
                .apply {
                    val substitutionMap = getSubstitutionMap(listOf(type to callable.originalType))
                    constructor.constructedClass.typeParameters
                        .map {
                            substitutionMap[it.descriptor.toClassifierRef()]
                                ?: error("No substitution found for ${it.dump()}")
                        }
                        .forEachIndexed { index, typeArgument ->
                            putTypeArgument(index, typeArgument.toIrType(pluginContext))
                        }

                    fillGivens(callable, false, this)
                }
        }
    }

    private fun ResolutionContext.propertyExpression(
        type: TypeRef,
        callable: CallableRef,
        descriptor: PropertyDescriptor,
        symbol: IrSymbol,
    ): IrExpression {
        val property = pluginContext.symbolTable.referenceProperty(descriptor.original).bind()
        val getter = property.owner.getter!!
        return DeclarationIrBuilder(pluginContext, symbol)
            .irCall(getter.symbol)
            .apply {
                val substitutionMap = getSubstitutionMap(
                    listOf(type to getter.descriptor.returnType!!.toTypeRef()),
                    getter.typeParameters.map { it.descriptor.toClassifierRef() }
                )

                getter.typeParameters
                    .map {
                        substitutionMap[it.descriptor.toClassifierRef()]
                            ?: error("No substitution found for ${it.dump()}")
                    }
                    .forEachIndexed { index, typeArgument ->
                        putTypeArgument(index, typeArgument.toIrType(pluginContext))
                    }

                fillGivens(callable, false, this)
            }
    }

    private fun ResolutionContext.functionExpression(
        type: TypeRef,
        callable: CallableRef,
        descriptor: FunctionDescriptor,
        symbol: IrSymbol,
    ): IrExpression {
        val function = descriptor.irFunction()
        return DeclarationIrBuilder(pluginContext, symbol)
            .irCall(function.symbol)
            .apply {
                val substitutionMap = getSubstitutionMap(
                    listOf(type to function.descriptor.returnType!!.toTypeRef()),
                    function.typeParameters.map { it.descriptor.toClassifierRef() }
                )

                function.typeParameters
                    .map {
                        substitutionMap[it.descriptor.toClassifierRef()]
                            ?: error("No substitution found for ${it.dump()}")
                    }
                    .forEachIndexed { index, typeArgument ->
                        putTypeArgument(index, typeArgument.toIrType(pluginContext))
                    }

                fillGivens(callable, false, this)
            }
    }

    private fun ResolutionContext.parameterExpression(
        descriptor: ParameterDescriptor,
        symbol: IrSymbol,
    ): IrExpression {
        return when (val containingDeclaration = descriptor.containingDeclaration) {
            is ClassDescriptor -> receiverAccessors.last {
                descriptor.type.constructor.declarationDescriptor == it.first.descriptor
            }.second()
            is ClassConstructorDescriptor -> DeclarationIrBuilder(pluginContext, symbol)
                .irGet(
                    containingDeclaration.irConstructor()
                        .allParameters
                        .single { it.name == descriptor.name }
                )
            is FunctionDescriptor -> DeclarationIrBuilder(pluginContext, symbol)
                .irGet(
                    containingDeclaration.irFunction()
                        .let { function ->
                            function.allParameters
                                .filter { it != function.dispatchReceiverParameter }
                        }
                        .single { it.name == descriptor.name }
                )
            else -> error("Unexpected parent $descriptor $containingDeclaration")
        }
    }

    private fun ResolutionContext.variableExpression(
        descriptor: VariableDescriptor,
        symbol: IrSymbol,
    ): IrExpression {
        return DeclarationIrBuilder(pluginContext, symbol)
            .irGet(variables.single { it.descriptor == descriptor })
    }

    private fun ClassConstructorDescriptor.irConstructor() =
        pluginContext.symbolTable.referenceConstructor(original).bind().owner

    private fun FunctionDescriptor.irFunction() =
        pluginContext.symbolTable.referenceSimpleFunction(original)
            .bind()
            .owner

    private fun <T : IrBindableSymbol<*, *>> T.bind(): T {
        (pluginContext as IrPluginContextImpl).linker.run {
            getDeclaration(this@bind)
            postProcess()
        }
        return this
    }

    private val receiverAccessors = mutableListOf<Pair<IrClass, () -> IrExpression>>()

    private val variables = mutableListOf<IrVariable>()

    private val fileStack = mutableListOf<IrFile>()
    override fun visitFile(declaration: IrFile): IrFile {
        fileStack.push(declaration)
        return super.visitFile(declaration)
            .also { fileStack.pop() }
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        receiverAccessors.push(
            declaration to {
                DeclarationIrBuilder(pluginContext, declaration.symbol)
                    .irGet(declaration.thisReceiver!!)
            }
        )
        val result = super.visitClass(declaration)
        receiverAccessors.pop()
        return result
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        val dispatchReceiver = declaration.dispatchReceiverParameter?.type?.classOrNull?.owner
        if (dispatchReceiver != null) {
            receiverAccessors.push(
                dispatchReceiver to {
                    DeclarationIrBuilder(pluginContext, declaration.symbol)
                        .irGet(declaration.dispatchReceiverParameter!!)
                }
            )
        }
        val extensionReceiver = declaration.extensionReceiverParameter?.type?.classOrNull?.owner
        if (extensionReceiver != null) {
            receiverAccessors.push(
                extensionReceiver to {
                    DeclarationIrBuilder(pluginContext, declaration.symbol)
                        .irGet(declaration.extensionReceiverParameter!!)
                }
            )
        }
        val result = super.visitFunction(declaration)
        if (dispatchReceiver != null) receiverAccessors.pop()
        if (extensionReceiver != null) receiverAccessors.pop()
        return result
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        return super.visitVariable(declaration)
            .also { variables += declaration }
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression) =
        super.visitFunctionAccess(expression.apply {
            val givenNodes = pluginContext.bindingContext[
                    InjektWritableSlices.GIVEN_GRAPH,
                    SourcePosition(
                        fileStack.last().fileEntry.name,
                        expression.startOffset,
                        expression.endOffset
                    )
            ]
            if (givenNodes != null) {
                try {
                    val substitutionMap = getSubstitutionMap(
                        (0 until expression.typeArgumentsCount)
                            .map { getTypeArgument(it)!!.toKotlinType().toTypeRef() }
                            .zip(
                                expression.symbol.descriptor.typeParameters
                                    .map { it.defaultType.toTypeRef() }
                            )
                    ) + getSubstitutionMap(
                        ((dispatchReceiver?.type as? IrSimpleType)?.arguments
                            ?.map { it.typeOrNull!!.toKotlinType().toTypeRef() }
                            ?: emptyList())
                            .zip(
                                dispatchReceiver?.type?.classOrNull?.owner?.let {
                                    it.typeParameters
                                        .map { it.defaultType.toKotlinType().toTypeRef() }
                                } ?: emptyList()
                            )
                    )
                    ResolutionContext(givenNodes)
                        .fillGivens(expression.symbol.descriptor.toCallableRef()
                            .substitute(substitutionMap), false, expression)
                } catch (e: Throwable) {
                    throw RuntimeException("Wtf ${expression.dump()}", e)
                }
            }
        })


}
