package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.toAnnotationDescriptor
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeAbbreviation
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeAbbreviationImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.DescriptorsRemapper
import org.jetbrains.kotlin.ir.util.SymbolRemapper
import org.jetbrains.kotlin.ir.util.TypeRemapper
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter

class ModuleFunctionTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()
    private val decoys = mutableSetOf<IrFunction>()

    fun getTransformedModule(function: IrFunction): IrFunction {
        return transformedFunctions[function] ?: function
    }

    fun isDecoy(function: IrFunction): Boolean = function in decoys

    override fun visitFile(declaration: IrFile): IrFile {
        val originalFunctions = declaration.declarations.filterIsInstance<IrFunction>()
        val result = super.visitFile(declaration)
        result.patchWithDecoys(originalFunctions)
        return result
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        val originalFunctions = declaration.declarations.filterIsInstance<IrFunction>()
        val result = super.visitClass(declaration) as IrClass
        result.patchWithDecoys(originalFunctions)
        return result
    }

    override fun visitFunction(declaration: IrFunction): IrStatement =
        transformFunctionIfNeeded(super.visitFunction(declaration) as IrFunction)

    private fun IrDeclarationContainer.patchWithDecoys(originalFunctions: List<IrFunction>) {
        for (original in originalFunctions) {
            val transformed = transformedFunctions[original]
            if (transformed != null && transformed != original) {
                declarations.add(
                    original.deepCopyWithSymbolsWithPreservingQualifiers()
                        .also { decoy ->
                            decoys += decoy
                            InjektDeclarationIrBuilder(pluginContext, decoy.symbol).run {
                                if (transformed.valueParameters
                                        .any { it.name.asString().startsWith("class\$") }
                                ) {
                                    decoy.annotations += noArgSingleConstructorCall(symbols.astTyped)
                                }

                                decoy.body = builder.irExprBody(irInjektIntrinsicUnit())
                            }
                        }
                )
            }
        }
    }

    private fun transformFunctionIfNeeded(function: IrFunction): IrFunction {
        if (function.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB ||
            function.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
        ) return function
        if (!function.isModule(pluginContext.bindingContext)) return function
        transformedFunctions[function]?.let { return it }
        if (function in transformedFunctions.values) return function
        if (function in decoys) return function

        val originalCaptures = mutableListOf<IrGetValue>()
        val originalClassOfCalls = mutableListOf<IrCall>()
        val originalTypedModuleCalls = mutableListOf<IrCall>()
        var hasUnresolvedClassOfCalls = false

        function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            private val moduleStack = mutableListOf(function)
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.isModule(pluginContext.bindingContext))
                    moduleStack.push(declaration)
                return super.visitFunction(declaration)
                    .also {
                        if (declaration.isModule(pluginContext.bindingContext))
                            moduleStack.pop()
                    }
            }
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                if (function.isLocal &&
                    moduleStack.last() == function &&
                    expression.symbol.owner !in function.valueParameters &&
                    expression.type.classOrNull != symbols.providerDsl
                ) {
                    originalCaptures += expression
                }
                return super.visitGetValue(expression)
            }
            override fun visitCall(expression: IrCall): IrExpression {
                val callee = transformFunctionIfNeeded(
                    expression.symbol.owner
                )
                if (callee.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.classOf") {
                    originalClassOfCalls += expression
                    if (expression.getTypeArgument(0)!!.toKotlinType().isTypeParameter()) {
                        hasUnresolvedClassOfCalls = true
                    }
                } else if (callee.hasAnnotation(InjektFqNames.AstTyped)) {
                    originalTypedModuleCalls += expression
                    if ((0 until expression.typeArgumentsCount)
                            .map { expression.getTypeArgument(it)!! }
                            .any { it.toKotlinType().isTypeParameter() }
                    ) {
                        hasUnresolvedClassOfCalls = true
                    }
                }
                return super.visitCall(expression)
            }
        })

        if (!hasUnresolvedClassOfCalls && originalCaptures.isEmpty()) {
            transformedFunctions[function] = function
            rewriteTypedFunctionCalls(
                function,
                originalClassOfCalls,
                originalTypedModuleCalls,
                emptyMap()
            )
            return function
        }

        val transformedFunction = function.deepCopyWithSymbolsWithPreservingQualifiers()
        transformedFunctions[function] = transformedFunction

        val classOfCalls = mutableListOf<IrCall>()
        val typedModuleCalls = mutableListOf<IrCall>()
        val captures = mutableListOf<IrGetValue>()

        transformedFunction.body?.transformChildrenVoid(object :
            IrElementTransformerVoid() {
            private val moduleStack = mutableListOf(transformedFunction)
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.isModule(pluginContext.bindingContext))
                    moduleStack.push(declaration)
                return super.visitFunction(declaration)
                    .also {
                        if (declaration.isModule(pluginContext.bindingContext))
                            moduleStack.pop()
                    }
            }
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                if (transformedFunction.isLocal &&
                    moduleStack.last() == transformedFunction &&
                    expression.symbol.owner !in transformedFunction.valueParameters &&
                    expression.type.classOrNull != symbols.providerDsl
                ) {
                    captures += expression
                }
                return super.visitGetValue(expression)
            }

            override fun visitCall(expression: IrCall): IrExpression {
                val callee = transformFunctionIfNeeded(expression.symbol.owner)
                try {
                    if (callee.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.classOf") {
                        classOfCalls += expression
                    } else if (callee.hasAnnotation(InjektFqNames.AstTyped)) {
                        typedModuleCalls += expression
                    }
                } catch (e: Exception) {
                }
                return super.visitCall(expression)
            }
        })

        if (classOfCalls.isNotEmpty() || typedModuleCalls.isNotEmpty()) {
            transformedFunction.annotations +=
                InjektDeclarationIrBuilder(pluginContext, transformedFunction.symbol)
                    .noArgSingleConstructorCall(symbols.astTyped)

            val valueParametersByUnresolvedType =
                mutableMapOf<IrTypeParameterSymbol, IrValueParameter>()

            (classOfCalls
                .map { it.getTypeArgument(0)!! } +
                    typedModuleCalls
                        .flatMap { call ->
                            (0 until call.typeArgumentsCount)
                                .map { call.getTypeArgument(it)!! }
                        })
                .filter { it.toKotlinType().isTypeParameter() }
                .map { it.classifierOrFail as IrTypeParameterSymbol }
                .distinct()
                .forEach { typeParameter ->
                    valueParametersByUnresolvedType[typeParameter] =
                        transformedFunction.addValueParameter(
                            InjektNameConventions.classParameterNameForTypeParameter(
                                    typeParameter.owner
                                )
                                .asString(),
                            irBuiltIns.kClassClass.typeWith(typeParameter.defaultType)
                        )
                }

            rewriteTypedFunctionCalls(
                transformedFunction,
                classOfCalls,
                typedModuleCalls,
                valueParametersByUnresolvedType
            )
        } else {
            rewriteTypedFunctionCalls(
                transformedFunction,
                classOfCalls,
                typedModuleCalls,
                emptyMap()
            )
        }

        if (captures.isNotEmpty()) {
            val valueParameterByCapture = captures.associateWith { capture ->
                transformedFunction.addValueParameter(
                    "capture_${captures.indexOf(capture)}",
                    capture.type
                )
            }

            transformedFunction.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    return valueParameterByCapture[expression]?.let {
                        DeclarationIrBuilder(pluginContext, expression.symbol)
                            .irGet(it)
                    } ?: super.visitGetValue(expression)
                }
            })

            function.file.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
                override fun visitCall(expression: IrCall): IrExpression {
                    if (expression.symbol != function.symbol) {
                        return super.visitCall(expression)
                    }
                    return DeclarationIrBuilder(pluginContext, expression.symbol).run {
                        irCall(transformedFunction).apply {
                            copyTypeAndValueArgumentsFrom(expression)
                            captures.forEach { capture ->
                                val valueParameter = valueParameterByCapture.getValue(capture)
                                putValueArgument(valueParameter.index, capture)
                            }
                        }
                    }
                }
            })
        }

        return transformedFunction
    }

    private fun rewriteTypedFunctionCalls(
        function: IrFunction,
        classOfCalls: List<IrCall>,
        typedModuleCalls: List<IrCall>,
        valueParametersByUnresolvedType: Map<IrTypeParameterSymbol, IrValueParameter>
    ) {
        function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                return when (expression) {
                    in classOfCalls -> {
                        val typeArgument = expression.getTypeArgument(0)!!
                        if (typeArgument.toKotlinType().isTypeParameter()) {
                            val symbol = typeArgument.classifierOrFail as IrTypeParameterSymbol
                            DeclarationIrBuilder(pluginContext, expression.symbol)
                                .irGet(valueParametersByUnresolvedType.getValue(symbol))
                        } else {
                            IrClassReferenceImpl(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                irBuiltIns.kClassClass.typeWith(typeArgument),
                                typeArgument.classifierOrFail,
                                typeArgument
                            )
                        }
                    }
                    in typedModuleCalls -> {
                        val originalFunction = expression.symbol.owner
                        val transformedFunction = transformFunctionIfNeeded(expression.symbol.owner)
                        DeclarationIrBuilder(pluginContext, expression.symbol)
                            .irCall(transformedFunction).apply {
                                dispatchReceiver = expression.dispatchReceiver
                                extensionReceiver = expression.extensionReceiver
                                (0 until expression.typeArgumentsCount).forEach {
                                    putTypeArgument(it, expression.getTypeArgument(it))
                                }
                                (0 until expression.valueArgumentsCount).forEach {
                                    putValueArgument(it, expression.getValueArgument(it))
                                }
                                (originalFunction.valueParameters.size until transformedFunction.valueParameters.size)
                                    .forEach { valueParameterIndex ->
                                        val valueParameter =
                                            transformedFunction.valueParameters[valueParameterIndex]
                                        val typeParameterName = InjektNameConventions
                                            .typeParameterNameForClassParameterName(valueParameter.name)
                                        val typeParameter = transformedFunction.typeParameters
                                            .single { it.name == typeParameterName }
                                        val typeArgument = getTypeArgument(typeParameter.index)!!
                                        putValueArgument(
                                            valueParameterIndex,
                                            if (typeArgument.toKotlinType().isTypeParameter()) {
                                                val symbol =
                                                    typeArgument.classifierOrFail as IrTypeParameterSymbol
                                                DeclarationIrBuilder(
                                                    pluginContext,
                                                    expression.symbol
                                                )
                                                    .irGet(
                                                        valueParametersByUnresolvedType.getValue(
                                                            symbol
                                                        )
                                                    )
                                            } else {
                                                IrClassReferenceImpl(
                                                    UNDEFINED_OFFSET,
                                                    UNDEFINED_OFFSET,
                                                    irBuiltIns.kClassClass.typeWith(typeArgument),
                                                    typeArgument.classifierOrFail,
                                                    typeArgument
                                                )
                                            }
                                        )
                                    }
                            }
                    }
                    else -> super.visitCall(expression)
                }
            }
        })
    }

    private fun IrFunction.deepCopyWithSymbolsWithPreservingQualifiers(): IrFunction {
        val symbolRemapper = DeepCopySymbolRemapper(
            object : DescriptorsRemapper {
                override fun remapDeclaredSimpleFunction(descriptor: FunctionDescriptor): FunctionDescriptor =
                    WrappedSimpleFunctionDescriptor(sourceElement = descriptor.source)
            }
        )
        acceptVoid(symbolRemapper)
        val typeRemapper = DeepCopyTypeRemapper(symbolRemapper)
        return (transform(
            DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper)
                .also { typeRemapper.deepCopy = it }, null
        )
            .patchDeclarationParents(parent) as IrSimpleFunction)
            .also { (it.descriptor as WrappedSimpleFunctionDescriptor).bind(it) }
    }

    private class DeepCopyTypeRemapper(
        private val symbolRemapper: SymbolRemapper
    ) : TypeRemapper {

        lateinit var deepCopy: DeepCopyIrTreeWithSymbols

        override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {
            // TODO
        }

        override fun leaveScope() {
            // TODO
        }

        override fun remapType(type: IrType): IrType =
            if (type !is IrSimpleType)
                type
            else {
                val kotlinType = type.toKotlinType()
                IrSimpleTypeImpl(
                    if (kotlinType is SimpleType) {
                        KotlinTypeFactory.simpleType(
                            try {
                                type.toKotlinType() as SimpleType
                            } catch (e: Exception) {
                                error("${type.render()} is not a SimpleType")
                            },
                            arguments = type.arguments.mapIndexed { index, it ->
                                when (it) {
                                    is IrTypeProjection -> TypeProjectionImpl(
                                        it.variance,
                                        it.type.toKotlinType()
                                    )
                                    is IrStarProjection -> StarProjectionImpl((type.classifier.descriptor as ClassDescriptor).typeConstructor.parameters[index])
                                    else -> error(it)
                                }
                            },
                            annotations = Annotations.create(
                                type.annotations.map { it.toAnnotationDescriptor() }
                            )
                        )
                    } else {
                        kotlinType
                    },
                    symbolRemapper.getReferencedClassifier(type.classifier),
                    type.hasQuestionMark,
                    type.arguments.map { remapTypeArgument(it) },
                    type.annotations.map { it.transform(deepCopy, null) as IrConstructorCall },
                    type.abbreviation?.remapTypeAbbreviation()
                )
            }

        private fun remapTypeArgument(typeArgument: IrTypeArgument): IrTypeArgument =
            if (typeArgument is IrTypeProjection)
                makeTypeProjection(this.remapType(typeArgument.type), typeArgument.variance)
            else
                typeArgument

        private fun IrTypeAbbreviation.remapTypeAbbreviation() =
            IrTypeAbbreviationImpl(
                symbolRemapper.getReferencedTypeAlias(typeAlias),
                hasQuestionMark,
                arguments.map { remapTypeArgument(it) },
                annotations
            )
    }

}
