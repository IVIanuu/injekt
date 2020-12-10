package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.isExternalDeclaration
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.interpreter.hasAnnotation
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
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
                        is ConstructorDescriptor -> {
                            if (callable.constructedClass.kind == ClassKind.OBJECT) {
                                val clazz =
                                    pluginContext.referenceClass(callable.constructedClass.fqNameSafe)!!
                                DeclarationIrBuilder(pluginContext, symbol)
                                    .irGetObject(clazz)
                            } else {
                                val constructor =
                                    pluginContext.referenceConstructors(callable.constructedClass.fqNameSafe)
                                        .single()
                                DeclarationIrBuilder(pluginContext, symbol)
                                    .irCall(constructor.owner.symbol)
                                    .apply { fillGivens(this) }
                            }
                        }
                        is PropertyDescriptor -> {
                            val property = pluginContext.referenceProperties(callable.fqNameSafe)
                                .single()
                            DeclarationIrBuilder(pluginContext, symbol)
                                .irCall(property.owner.getter!!.symbol)
                                .apply { fillGivens(this) }
                        }
                        is FunctionDescriptor -> {
                            val function = pluginContext.referenceFunctions(callable.fqNameSafe)
                                .singleOrNull() ?: error("Nothing found for $callable")
                            DeclarationIrBuilder(pluginContext, symbol)
                                .irCall(function)
                                .apply { fillGivens(this) }
                        }
                        else -> error("Unsupported callable $callable")
                    }
                }
                expression
            }()
        }

        private fun givensFor(type: IrType): List<CallableDescriptor> {
            val givens = givensForInThisScope(type.toKotlinType())
            return when {
                givens.isNotEmpty() -> return givens
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

    private var scope: Scope = InternalScope(ExternalScope())

    override fun visitCall(expression: IrCall): IrExpression =
        super.visitCall(expression.apply { scope.fillGivens(this) })

}
