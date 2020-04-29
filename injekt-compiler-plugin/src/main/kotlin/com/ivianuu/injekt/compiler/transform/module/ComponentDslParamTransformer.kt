package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.generateSymbols
import com.ivianuu.injekt.compiler.hasModuleAnnotation
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedFunctionDescriptorWithContainerSource
import org.jetbrains.kotlin.ir.descriptors.WrappedPropertyGetterDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedPropertySetterDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.util.OperatorNameConventions

class ComponentDslParamTransformer(
    context: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    bindingTrace: BindingTrace
) : AbstractInjektTransformer(context, symbolRemapper, bindingTrace) {

    override fun lower(module: IrModuleFragment) {
        module.transformChildrenVoid(this)

        module.acceptVoid(symbolRemapper)

        val typeRemapper = ModuleTypeRemapper(
            context,
            symbolRemapper,
            typeTranslator,
            symbols.componentDsl.descriptor
        )

        val transformer = DeepCopyIrTreeWithSymbolsPreservingMetadata(
            context,
            symbolRemapper,
            typeRemapper,
            typeTranslator
        ).also { typeRemapper.deepCopy = it }
        module.transformChildren(
            transformer,
            null
        )

        module.patchDeclarationParents()
    }

    private val transformedFunctions: MutableMap<IrFunction, IrFunction> = mutableMapOf()

    private val transformedFunctionSet = mutableSetOf<IrFunction>()

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        return transformedFunctions[expression.symbol.ensureBound(context.irProviders).owner]
            ?.let { transformed ->
                IrFunctionReferenceImpl(
                    expression.startOffset,
                    expression.endOffset,
                    expression.type,
                    transformed.symbol,
                    expression.typeArgumentsCount,
                    expression.reflectionTarget,
                    expression.origin
                ).also {
                    it.dispatchReceiver = expression.dispatchReceiver
                    it.extensionReceiver = expression.extensionReceiver
                    it.copyAttributes(expression)
                    it.copyTypeArgumentsFrom(expression)
                }
            } ?: super.visitFunctionReference(expression)
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        return super.visitFunction(declaration.withComponentDslParamIfNeeded())
    }

    override fun visitFile(declaration: IrFile): IrFile {
        val originalFunctions = mutableListOf<IrFunction>()
        val originalProperties = mutableListOf<Pair<IrProperty, IrSimpleFunction>>()
        loop@ for (child in declaration.declarations) {
            when (child) {
                is IrFunction -> originalFunctions.add(child)
                is IrProperty -> {
                    val getter = child.getter ?: continue@loop
                    originalProperties.add(child to getter)
                }
            }
        }
        val result = super.visitFile(declaration)
        result.patchWithSyntheticModuleDecoys(originalFunctions, originalProperties)
        return result
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        val originalFunctions = declaration.functions.toList()
        val originalProperties = declaration
            .properties
            .mapNotNull { p -> p.getter?.let { p to it } }
            .toList()
        val result = super.visitClass(declaration)
        if (result !is IrClass) error("expected IrClass")
        result.patchWithSyntheticModuleDecoys(originalFunctions, originalProperties)
        return result
    }

    fun IrDeclarationContainer.patchWithSyntheticModuleDecoys(
        originalFunctions: List<IrFunction>,
        originalProperties: List<Pair<IrProperty, IrSimpleFunction>>
    ) {
        for (function in originalFunctions) {
            if (transformedFunctions.containsKey(function) && function.isModule()) {
                declarations.add(function.copyAsModuleDecoy())
            }
        }
        for ((property, getter) in originalProperties) {
            if (transformedFunctions.containsKey(getter) && property.hasModuleAnnotation()) {
                val newGetter = property.getter
                assert(getter !== newGetter)
                assert(newGetter != null)
                property.getter = getter.copyAsModuleDecoy().also { it.parent = this }
                declarations.add(newGetter!!)
                newGetter.parent = this
            }
        }
    }

    fun IrCall.withComponentDslParamIfNeeded(componentDslParam: IrValueParameter): IrCall {
        val isModuleLambda = isModuleLambdaInvoke()
        if (!symbol.descriptor.isModule() && !isModuleLambda)
            return this
        val ownerFn = when {
            isModuleLambda -> {
                if (!symbol.isBound) context.irProviders.getDeclaration(symbol)
                (symbol.owner as IrSimpleFunction).lambdaInvokeWithComponentDslParamIfNeeded()
            }
            else -> (symbol.owner as IrSimpleFunction).withComponentDslParamIfNeeded()
        }
        if (!transformedFunctionSet.contains(ownerFn))
            return this
        if (symbol.owner == ownerFn)
            return this
        return IrCallImpl(
            startOffset,
            endOffset,
            type,
            ownerFn.symbol,
            typeArgumentsCount,
            ownerFn.valueParameters.size,
            origin,
            superQualifierSymbol
        ).also {
            it.copyAttributes(this)
            it.copyTypeArgumentsFrom(this)
            it.dispatchReceiver = dispatchReceiver
            it.extensionReceiver = extensionReceiver
            for (i in 0 until valueArgumentsCount) {
                val arg = getValueArgument(i)
                if (arg != null) {
                    it.putValueArgument(i, arg)
                }
            }
            it.putValueArgument(
                valueArgumentsCount,
                IrGetValueImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    componentDslParam.symbol
                )
            )
        }
    }

    fun IrFunction.withComponentDslParamIfNeeded(): IrFunction {
        if (transformedFunctionSet.contains(this)) return this

        if (origin == MODULE_DECOY_IMPL) return this

        if (!descriptor.isModule()) return this

        if (isNonModuleInlinedLambda()) return this

        if (isExpect) return this

        return transformedFunctions[this] ?: copyWithComponentDslParam()
    }

    fun IrFunction.lambdaInvokeWithComponentDslParamIfNeeded(): IrFunction {
        if (transformedFunctionSet.contains(this)) return this
        return transformedFunctions.getOrPut(this) {
            lambdaInvokeWithComponentDslParam().also { transformedFunctionSet.add(it) }
        }
    }

    fun IrFunction.lambdaInvokeWithComponentDslParam(): IrFunction {
        val descriptor = descriptor
        val argCount = descriptor.valueParameters.size
        val extraParams = 1
        val newFnClass = context.symbolTable
            .referenceClass(context.builtIns.getFunction(argCount + extraParams))
        val newDescriptor = newFnClass.descriptor.unsubstitutedMemberScope.findFirstFunction(
            OperatorNameConventions.INVOKE.identifier
        ) { true }

        return IrFunctionImpl(
            startOffset,
            endOffset,
            origin,
            newDescriptor,
            newDescriptor.returnType?.toIrType()!!
        ).also { fn ->
            if (!newFnClass.isBound) context.irProviders.getDeclaration(newFnClass)
            fn.parent = newFnClass.owner

            fn.copyTypeParametersFrom(this)
            dispatchReceiverParameter?.type?.let { context.bindIfNeeded(it) }
            fn.dispatchReceiverParameter = dispatchReceiverParameter?.copyTo(fn)
            extensionReceiverParameter?.type?.let { context.bindIfNeeded(it) }
            fn.extensionReceiverParameter = extensionReceiverParameter?.copyTo(fn)
            newDescriptor.valueParameters.forEach { p ->
                fn.addValueParameter(p.name.identifier, p.type.toIrType())
            }
            assert(fn.body == null) { "expected body to be null" }
        }
    }

    private fun IrFunction.copyAsModuleDecoy(): IrSimpleFunction {
        if (origin == IrDeclarationOrigin.FAKE_OVERRIDE) return this as IrSimpleFunction
        return copy().also { fn ->
            fn.origin = MODULE_DECOY_IMPL
            (fn as IrFunctionImpl).metadata = metadata
            val errorCls = symbols.getTopLevelClass(
                FqName(
                    "kotlin" +
                            ".NotImplementedError"
                )
            )
            val errorCtor = errorCls.constructors.single {
                it.descriptor.valueParameters.size == 1 &&
                        KotlinBuiltIns.isString(it.descriptor.valueParameters.single().type)
            }
            fn.valueParameters = emptyList()
            fn.valueParameters = valueParameters.map { p -> p.copyTo(fn, defaultValue = null) }
            fn.body = DeclarationIrBuilder(context, fn.symbol).irBlockBody {
                +irThrow(
                    IrConstructorCallImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        errorCls.defaultType,
                        errorCtor,
                        0, 0, 1
                    ).also {
                        it.putValueArgument(
                            0, IrConstImpl.string(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                context.irBuiltIns.stringType,
                                "Module functions cannot be called without a " +
                                        "component dsl. If you are getting this error, it " +
                                        "is likely because of a misconfigured compiler"
                            )
                        )
                    }
                )
            }
        }
    }

    private fun wrapDescriptor(descriptor: FunctionDescriptor): WrappedSimpleFunctionDescriptor {
        return when (descriptor) {
            is PropertyGetterDescriptor ->
                WrappedPropertyGetterDescriptor(
                    descriptor.annotations,
                    descriptor.source
                )
            is PropertySetterDescriptor ->
                WrappedPropertySetterDescriptor(
                    descriptor.annotations,
                    descriptor.source
                )
            is DescriptorWithContainerSource ->
                WrappedFunctionDescriptorWithContainerSource(descriptor.containerSource)
            else ->
                WrappedSimpleFunctionDescriptor(sourceElement = descriptor.source)
        }
    }

    private fun IrFunction.copy(
        isInline: Boolean = this.isInline,
        modality: Modality = descriptor.modality
    ): IrSimpleFunction {
        val descriptor = descriptor
        val newDescriptor = wrapDescriptor(descriptor)

        return IrFunctionImpl(
            startOffset,
            endOffset,
            origin,
            IrSimpleFunctionSymbolImpl(newDescriptor),
            name,
            visibility,
            modality,
            returnType,
            isInline,
            isExternal,
            descriptor.isTailrec,
            descriptor.isSuspend,
            descriptor.isOperator,
            isExpect,
            isFakeOverride
        ).also { fn ->
            newDescriptor.bind(fn)
            if (this is IrSimpleFunction) {
                fn.correspondingPropertySymbol = correspondingPropertySymbol
            }
            fn.parent = parent

            this.typeParameters.forEach {
                it.superTypes.forEach {
                    if (it is IrSimpleType && !it.classifier.isBound) context.irProviders
                        .getDeclaration(
                            it
                                .classifier
                        )
                }
            }
            fn.copyTypeParametersFrom(this)
            generateSymbols(context)
            fn.dispatchReceiverParameter = dispatchReceiverParameter?.copyTo(fn)
            fn.extensionReceiverParameter = extensionReceiverParameter?.copyTo(fn)
            fn.valueParameters = valueParameters.map { p ->
                p.type.let {
                    if (it is IrSimpleType && !it.classifier.isBound) context.irProviders
                        .getDeclaration(
                            it
                                .classifier
                        )
                }
                p.type.let {
                    if (it is IrSimpleType) it.arguments.forEach {
                        if (it is IrTypeProjection) {
                            val tp = it.type
                            if (tp is IrSimpleType && !tp.classifier.isBound) context.irProviders
                                .getDeclaration(tp.classifier)
                        }
                    }
                }
                p.copyTo(fn, name = dexSafeName(p.name))
            }
            fn.annotations = annotations.map { a -> a }
            fn.body = body?.deepCopyWithSymbols(this)
        }
    }

    private fun dexSafeName(name: Name): Name {
        return if (name.isSpecial && name.asString().contains(' ')) {
            val sanitized = name
                .asString()
                .replace(' ', '$')
                .replace('<', '$')
                .replace('>', '$')
            Name.identifier(sanitized)
        } else name
    }

    private fun jvmNameAnnotation(name: String): IrConstructorCall {
        val jvmName = symbols.getTopLevelClass(DescriptorUtils.JVM_NAME)
        val cd = symbols.getTopLevelClass(DescriptorUtils.JVM_NAME)
            .descriptor
            .unsubstitutedPrimaryConstructor!!
        val type = jvmName.createType(false, emptyList())
        val ctor = context.symbolTable.referenceConstructor(cd)
        return IrConstructorCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            ctor,
            0, 0, 1
        ).also {
            it.putValueArgument(
                0, IrConstImpl.string(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    context.irBuiltIns.stringType,
                    name
                )
            )
        }
    }

    private fun IrFunction.copyWithComponentDslParam(): IrFunction {
        assert(explicitParameters.lastOrNull()?.name != InjektNameConventions.ComponentDslParameter) {
            "Attempted to add component dsl param to $this, but it has already been added."
        }
        return copy().also { fn ->
            val oldFn = this

            transformedFunctionSet.add(fn)
            transformedFunctions[oldFn] = fn

            if (this is IrOverridableDeclaration<*>) {
                fn.overriddenSymbols = overriddenSymbols.map {
                    it as IrSimpleFunctionSymbol
                    val owner = it.owner
                    val newOwner = owner.withComponentDslParamIfNeeded()
                    newOwner.symbol as IrSimpleFunctionSymbol
                }
            }

            val descriptor = descriptor
            if (descriptor is PropertyGetterDescriptor &&
                fn.annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.getterName(descriptor.correspondingProperty.name.identifier)
                fn.annotations += jvmNameAnnotation(name)
            }

            if (descriptor is PropertySetterDescriptor &&
                fn.annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.setterName(descriptor.correspondingProperty.name.identifier)
                fn.annotations += jvmNameAnnotation(name)
            }

            val valueParametersMapping = explicitParameters
                .zip(fn.explicitParameters)
                .toMap()

            val componentDslParam = fn.addValueParameter(
                InjektNameConventions.ComponentDslParameter.identifier,
                symbols.componentDsl.defaultType
            )

            fn.transformChildrenVoid(object : IrElementTransformerVoid() {
                var isNestedScope = false
                override fun visitGetValue(expression: IrGetValue): IrGetValue {
                    val newParam = valueParametersMapping[expression.symbol.owner]
                    return if (newParam != null) {
                        IrGetValueImpl(
                            expression.startOffset,
                            expression.endOffset,
                            expression.type,
                            newParam.symbol,
                            expression.origin
                        )
                    } else expression
                }

                override fun visitReturn(expression: IrReturn): IrExpression {
                    if (expression.returnTargetSymbol == oldFn.symbol) {
                        return super.visitReturn(
                            IrReturnImpl(
                                expression.startOffset,
                                expression.endOffset,
                                expression.type,
                                fn.symbol,
                                expression.value
                            )
                        )
                    }
                    return super.visitReturn(expression)
                }

                override fun visitFunction(declaration: IrFunction): IrStatement {
                    val wasNested = isNestedScope
                    try {
                        isNestedScope = if (declaration.isInlinedLambda()) wasNested else true
                        return super.visitFunction(declaration)
                    } finally {
                        isNestedScope = wasNested
                    }
                }

                override fun visitCall(expression: IrCall): IrExpression {
                    val expr = if (!isNestedScope) {
                        expression.withComponentDslParamIfNeeded(componentDslParam)
                    } else
                        expression
                    return super.visitCall(expr)
                }
            })
        }
    }

    fun IrCall.isModuleLambdaInvoke(): Boolean {
        return origin == IrStatementOrigin.INVOKE &&
                dispatchReceiver?.type?.hasModuleAnnotation() == true
    }

    fun IrFunction.isNonModuleInlinedLambda(): Boolean {
        descriptor.findPsi()?.let { psi ->
            (psi as? KtFunctionLiteral)?.let {
                val arg = InlineUtil.getInlineArgumentDescriptor(
                    it,
                    context.bindingContext
                ) ?: return false

                return !arg.type.hasModuleAnnotation()
            }
        }
        return false
    }

    fun IrFunction.isInlinedLambda(): Boolean {
        descriptor.findPsi()?.let { psi ->
            (psi as? KtFunctionLiteral)?.let {
                if (InlineUtil.isInlinedArgument(
                        it,
                        context.bindingContext,
                        false
                    )
                )
                    return true
            }
        }
        return false
    }

}

internal val MODULE_DECOY_IMPL =
    object : IrDeclarationOriginImpl("MODULE_DECOY_IMPL", isSynthetic = true) {}
