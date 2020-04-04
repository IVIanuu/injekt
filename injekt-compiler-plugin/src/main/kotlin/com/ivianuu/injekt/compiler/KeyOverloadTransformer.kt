package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.resolve.scopes.MemberScope

class KeyOverloadTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val key = getClass(InjektClassNames.Key)

    override fun visitCall(expression: IrCall): IrExpression {
        super.visitCall(expression)

        val descriptor = expression.symbol.descriptor

        if (!descriptor.annotations.hasAnnotation(InjektClassNames.KeyOverloadStub)) return expression

        fun MemberScope.findKeyOverloadFunction() = try {
            findFirstFunction(descriptor.name.asString()) { function ->
                function.annotations.hasAnnotation(InjektClassNames.KeyOverload)
                        && (function.extensionReceiverParameter
                    ?: function.dispatchReceiverParameter)?.type == descriptor.extensionReceiverParameter?.type &&
                        function.typeParameters.size == 1 &&
                        function.valueParameters.firstOrNull()?.type?.constructor?.declarationDescriptor == key &&
                        function.valueParameters.first().type.arguments.first().type == function.typeParameters.first().defaultType &&
                        function.valueParameters.drop(1).all {
                    it.name == descriptor.valueParameters.getOrNull(it.index)?.name
                }
            }
        } catch (e: Exception) {
            null
        }

        val keyOverloadFunction = (descriptor.containingDeclaration as PackageFragmentDescriptor)
            .let { pluginContext.moduleDescriptor.getPackage(it.fqName).memberScope }
            .findKeyOverloadFunction()
            ?: (descriptor.extensionReceiverParameter?.value?.type?.constructor?.declarationDescriptor as? ClassDescriptor)
                ?.unsubstitutedMemberScope?.findKeyOverloadFunction()
            ?: error("Couldn't find @KeyOverload function for $descriptor")

        val keyOf = injektPackage.memberScope
            .findFirstFunction("keyOf") { it.valueParameters.size == 1 }

        val builder = DeclarationIrBuilder(pluginContext, expression.symbol)

        return builder.irCall(
            callee = symbolTable.referenceSimpleFunction(keyOverloadFunction),
            type = expression.type
        ).apply {
            if (keyOverloadFunction.dispatchReceiverParameter != null) {
                dispatchReceiver = expression.extensionReceiver
            } else {
                extensionReceiver = expression.extensionReceiver
            }

            copyTypeArgumentsFrom(expression)

            putValueArgument(
                0,
                builder.irCall(
                    callee = symbolTable.referenceSimpleFunction(keyOf),
                    type = symbolTable.referenceClass(key)
                        .also {
                            if (!it.isBound) pluginContext.irProvider.getDeclaration(it)
                        }
                        .typeWith(expression.getTypeArgument(0)!!)
                ).apply {
                    putTypeArgument(0, expression.getTypeArgument(0))
                    putValueArgument(0, expression.getValueArgument(0))
                }
            )

            (1 until expression.valueArgumentsCount)
                .map { it to expression.getValueArgument(it) }
                .forEach { (i, param) ->
                    putValueArgument(i, param)
                }
        }
    }

}
