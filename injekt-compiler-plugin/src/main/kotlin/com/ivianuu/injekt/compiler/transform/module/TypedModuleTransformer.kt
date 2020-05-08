package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.toAnnotationDescriptor
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
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
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeAbbreviation
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
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
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter

class TypedModuleTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()

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
        transformFunctionIfNeeded(declaration)

    private fun IrDeclarationContainer.patchWithDecoys(originalFunctions: List<IrFunction>) {
        for (function in originalFunctions) {
            val transformed = transformedFunctions[function]
            if (transformed != null && transformed.valueParameters.size != function.valueParameters.size) {
                declarations.add(
                    function.deepCopyWithSymbols()
                        .apply {
                            InjektDeclarationIrBuilder(pluginContext, symbol).run {
                                annotations += noArgSingleConstructorCall(symbols.astTyped)
                                body = builder.irExprBody(irInjektIntrinsicUnit())
                            }
                        }
                )
            }
        }
    }

    private fun transformFunctionIfNeeded(function: IrFunction): IrFunction {
        if (function.isExternal) return function
        if (!function.hasAnnotation(InjektFqNames.Module)) return function
        transformedFunctions[function]?.let { return it }
        if (function in transformedFunctions.values) return function

        val originalClassOfCalls = mutableListOf<IrCall>()
        val originalTypedModuleCalls = mutableListOf<IrCall>()

        var hasUnresolvedClassOfCalls = false

        function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val callee = transformFunctionIfNeeded(
                    expression.symbol
                        .ensureBound(irProviders)
                        .owner
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

        if (!hasUnresolvedClassOfCalls) {
            transformedFunctions[function] = function
            rewriteTypedFunctionCalls(
                function,
                originalClassOfCalls,
                originalTypedModuleCalls,
                emptyMap()
            )
            return function
        }

        val typedFunction = function.deepCopyWithSymbols()
        transformedFunctions[function] = typedFunction

        val classOfCalls = mutableListOf<IrCall>()
        val typedModuleCalls = mutableListOf<IrCall>()

        typedFunction.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val callee = transformFunctionIfNeeded(
                    expression.symbol
                        .ensureBound(irProviders)
                        .owner
                )
                if (callee.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.classOf") {
                    classOfCalls += expression
                } else if (callee.hasAnnotation(InjektFqNames.AstTyped)) {
                    typedModuleCalls += expression
                }
                return super.visitCall(expression)
            }
        })

        typedFunction.annotations +=
            InjektDeclarationIrBuilder(pluginContext, typedFunction.symbol)
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
                valueParametersByUnresolvedType[typeParameter] = typedFunction.addValueParameter(
                    name = InjektNameConventions.classParameterNameForTypeParameter(typeParameter.owner)
                        .asString(),
                    type = irBuiltIns.kClassClass.typeWith(typeParameter.defaultType)
                )
            }

        rewriteTypedFunctionCalls(
            typedFunction,
            classOfCalls,
            typedModuleCalls,
            valueParametersByUnresolvedType
        )

        return typedFunction
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
                    else -> {
                        super.visitCall(expression)
                    }
                }
            }
        })
    }

    private fun IrFunction.deepCopyWithSymbols(): IrFunction {
        val symbolRemapper = DeepCopySymbolRemapper(
            object : DescriptorsRemapper {
                override fun remapDeclaredSimpleFunction(descriptor: FunctionDescriptor): FunctionDescriptor =
                    WrappedSimpleFunctionDescriptor(sourceElement = descriptor.source)
            }
        )
        acceptVoid(symbolRemapper)
        val typeRemapper = DeepCopyTypeRemapper(symbolRemapper)
        return (transform(DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper)
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
            else
                IrSimpleTypeImpl(
                    KotlinTypeFactory.simpleType(
                        type.toKotlinType() as SimpleType,
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
                    ),
                    symbolRemapper.getReferencedClassifier(type.classifier),
                    type.hasQuestionMark,
                    type.arguments.map { remapTypeArgument(it) },
                    type.annotations.map { it.transform(deepCopy, null) as IrConstructorCall },
                    type.abbreviation?.remapTypeAbbreviation()
                )

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
