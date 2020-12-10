package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.extractGivensOfCallable
import com.ivianuu.injekt.compiler.extractGivensOfDeclaration
import com.ivianuu.injekt.compiler.isExternalDeclaration
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.interpreter.hasAnnotation
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType

@Binding class GivenCallTransformer(
    private val declarationStore: DeclarationStore,
    private val pluginContext: IrPluginContext,
) : IrElementTransformerVoid() {

    private abstract inner class Scope(private val parent: Scope?) {

        private val expressionsByType = mutableMapOf<IrType, () -> IrExpression>()

        fun fillGivens(call: IrFunctionAccessExpression) {
            val callee = call.symbol.owner
            val calleeDescriptor = callee.descriptor
            val givenInfo = declarationStore.givenInfoForCallable(calleeDescriptor)

            if (givenInfo.requiredGivens.isNotEmpty() || givenInfo.givensWithDefault.isNotEmpty()) {
                val substitutionMap = callee.typeParameters
                    .map { it.symbol }
                    .zip((0 until call.typeArgumentsCount).map { call.getTypeArgument(it)!! })
                    .toMap()
                callee
                    .valueParameters
                    .filter { it.type.hasAnnotation(InjektFqNames.Given) }
                    .filter { call.getValueArgument(it.index) == null }
                    .map {
                        it to getExpressionForType(
                            it.type.substitute(substitutionMap),
                            call.symbol
                        )
                    }
                    .forEach { call.putValueArgument(it.first.index, it.second) }
            }
        }

        private fun getExpressionForType(type: IrType, symbol: IrSymbol): IrExpression {
            return expressionsByType.getOrPut(type) {
                val callable = givensFor(type).singleOrNull() ?: error("Wtf $type")
                val expression: () -> IrExpression = {
                    when (callable) {
                        is ConstructorDescriptor -> classExpression(callable, symbol)
                        is PropertyDescriptor -> propertyExpression(callable, symbol)
                        is FunctionDescriptor -> functionExpression(callable, symbol)
                        is ReceiverParameterDescriptor -> parameterExpression(callable, symbol)
                        is ValueParameterDescriptor -> parameterExpression(callable, symbol)
                        else -> error("Unsupported callable $callable")
                    }
                }
                expression
            }()
        }

        private fun classExpression(
            descriptor: ConstructorDescriptor,
            symbol: IrSymbol,
        ): IrExpression {
            return if (descriptor.constructedClass.kind == ClassKind.OBJECT) {
                val clazz =
                    pluginContext.referenceClass(descriptor.constructedClass.fqNameSafe)!!
                DeclarationIrBuilder(pluginContext, symbol)
                    .irGetObject(clazz)
            } else {
                val constructor =
                    pluginContext.referenceConstructors(descriptor.constructedClass.fqNameSafe)
                        .single()
                DeclarationIrBuilder(pluginContext, symbol)
                    .irCall(constructor.owner.symbol)
                    .apply { fillGivens(this) }
            }
        }

        private fun propertyExpression(
            descriptor: PropertyDescriptor,
            symbol: IrSymbol,
        ): IrExpression {
            val property = pluginContext.referenceProperties(descriptor.fqNameSafe)
                .single()
            val getter = property.owner.getter!!
            return DeclarationIrBuilder(pluginContext, symbol)
                .irCall(getter.symbol)
                .apply {
                    val dispatchReceiverParameter = getter.dispatchReceiverParameter
                    if (dispatchReceiverParameter != null) {
                        dispatchReceiver =
                            if (dispatchReceiverParameter.type.classOrNull?.owner?.kind == ClassKind.OBJECT) {
                                DeclarationIrBuilder(pluginContext, symbol)
                                    .irGetObject(dispatchReceiverParameter.type.classOrNull!!)
                            } else {
                                dispatchReceiverAccessors.reversed()
                                    .first { it.first == dispatchReceiverParameter.type.classOrNull?.owner }
                                    .second()
                            }
                    }
                    fillGivens(this)
                }
        }

        private fun functionExpression(
            descriptor: FunctionDescriptor,
            symbol: IrSymbol,
        ): IrExpression {
            val function = descriptor.irFunction()
            return DeclarationIrBuilder(pluginContext, symbol)
                .irCall(function.symbol)
                .apply {
                    val dispatchReceiverParameter = function.dispatchReceiverParameter
                    if (dispatchReceiverParameter != null) {
                        dispatchReceiver =
                            if (dispatchReceiverParameter.type.classOrNull?.owner?.kind == ClassKind.OBJECT) {
                                DeclarationIrBuilder(pluginContext, symbol)
                                    .irGetObject(dispatchReceiverParameter.type.classOrNull!!)
                            } else {
                                dispatchReceiverAccessors.reversed()
                                    .first { it.first == dispatchReceiverParameter.type.classOrNull?.owner }
                                    .second()
                            }
                    }
                    fillGivens(this)
                }
        }

        private fun parameterExpression(
            descriptor: ParameterDescriptor,
            symbol: IrSymbol,
        ): IrExpression {
            val valueParameter =
                when (val containingDeclaration = descriptor.containingDeclaration) {
                    is ConstructorDescriptor -> containingDeclaration.irConstructor()
                        .allParameters
                        .single { it.name == descriptor.name }
                    is FunctionDescriptor -> containingDeclaration.irFunction()
                        .allParameters
                        .single { it.name == descriptor.name }
                    else -> error("Unexpected parent $descriptor $containingDeclaration")
                }

            return DeclarationIrBuilder(pluginContext, symbol)
                .irGet(valueParameter)
        }

        private fun ConstructorDescriptor.irConstructor() =
            pluginContext.referenceConstructors(constructedClass.fqNameSafe).first {
                it.descriptor.original == this.original
            }.owner

        private fun FunctionDescriptor.irFunction() = pluginContext.referenceFunctions(fqNameSafe)
            .singleOrNull()?.owner ?: error("Nothing found for $this")

        private fun givensFor(type: IrType): List<CallableDescriptor> {
            val givens = givensForInThisScope(type.toKotlinType())
            return when {
                givens.isNotEmpty() -> givens
                parent != null -> parent.givensFor(type)
                else -> emptyList()
            }
        }

        protected abstract fun givensForInThisScope(type: KotlinType): List<CallableDescriptor>
    }

    private inner class ExternalScope : Scope(null) {
        override fun givensForInThisScope(type: KotlinType): List<CallableDescriptor> =
            declarationStore.givensForType(type)
                .filter { it.isExternalDeclaration() }
                .filter { it.visibility == DescriptorVisibilities.PUBLIC }
    }

    private inner class InternalScope(parent: Scope?) : Scope(parent) {
        override fun givensForInThisScope(type: KotlinType): List<CallableDescriptor> =
            declarationStore.givensForType(type)
                .filterNot { it.isExternalDeclaration() }
    }

    private inner class ClassScope(
        private val declaration: IrClass,
        parent: Scope?,
    ) : Scope(parent) {
        private val allGivens =
            declaration.descriptor.extractGivensOfDeclaration(pluginContext.bindingContext)

        override fun givensForInThisScope(type: KotlinType): List<CallableDescriptor> =
            allGivens.filter { it.returnType == type }
    }

    private inner class FunctionScope(
        private val declaration: IrFunction,
        parent: Scope?,
    ) : Scope(parent) {
        private val allGivens =
            declaration.descriptor.extractGivensOfCallable(pluginContext.bindingContext)

        override fun givensForInThisScope(type: KotlinType): List<CallableDescriptor> =
            allGivens.filter { it.returnType == type }
    }

    private var scope: Scope = InternalScope(ExternalScope())

    private val dispatchReceiverAccessors = mutableListOf<Pair<IrClass, () -> IrExpression>>()

    private inline fun <R> inScope(scope: Scope, block: () -> R): R {
        val prevScope = this.scope
        this.scope = scope
        val result = block()
        this.scope = prevScope
        return result
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        dispatchReceiverAccessors.push(
            declaration to {
                DeclarationIrBuilder(pluginContext, declaration.symbol)
                    .irGet(declaration.thisReceiver!!)
            }
        )
        val result = if (declaration.kind == ClassKind.OBJECT) {
            inScope(ClassScope(declaration, scope)) { super.visitClass(declaration) }
        } else {
            val parentScope = declaration.companionObject()
                ?.let { it as? IrClass }
                ?.let { ClassScope(it, scope) } ?: scope
            inScope(ClassScope(declaration, parentScope)) { super.visitClass(declaration) }
        }
        dispatchReceiverAccessors.pop()
        return result
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        val dispatchReceiver = declaration.dispatchReceiverParameter?.type?.classOrNull?.owner
        if (dispatchReceiver != null) {
            dispatchReceiverAccessors.push(
                dispatchReceiver to {
                    DeclarationIrBuilder(pluginContext, declaration.symbol)
                        .irGet(declaration.dispatchReceiverParameter!!)
                }
            )
        }
        val result = inScope(FunctionScope(declaration, scope)) {
            super.visitFunction(declaration)
        }
        if (dispatchReceiver != null) {
            dispatchReceiverAccessors.pop()
        }
        return result
    }

    override fun visitCall(expression: IrCall): IrExpression =
        super.visitCall(expression.apply { scope.fillGivens(this) })

}
