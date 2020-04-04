package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.psi2ir.findFirstFunction

class KeyOverloadTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val key = getClass(InjektClassNames.Key)

    override fun visitCall(expression: IrCall): IrExpression {
        super.visitCall(expression)

        val descriptor = expression.symbol.descriptor

        if (!descriptor.annotations.hasAnnotation(InjektClassNames.KeyOverload)) return expression

        val parentMemberScope = when (val parent = descriptor.containingDeclaration) {
            is ClassDescriptor -> parent.unsubstitutedMemberScope
            is PackageFragmentDescriptor -> parent.getMemberScope()
            else -> error("Unexpected parent $parent")
        }

        val keyOverload = try {
            parentMemberScope.findFirstFunction(
                name = descriptor.name.asString()
            ) { function ->
                function.valueParameters.first().type.constructor == key.defaultType.constructor &&
                        function.valueParameters.size == descriptor.valueParameters.size /* &&
                        function.valueParameters.drop(1)
                            .all {
                                val other = descriptor.valueParameters[it.index]
                                it.name == other.name
                                        && it.type == other.type
                            }*/
            }
        } catch (e: Exception) {
            error(
                "Failed for $descriptor parent ${descriptor.containingDeclaration} member scope ${parentMemberScope.getContributedFunctions(
                    descriptor.name,
                    NoLookupLocation.FROM_BACKEND
                )}"
            )
        }

        val keyOf = injektPackage.memberScope
            .findFirstFunction("keyOf") { it.valueParameters.size == 1 }

        val builder = DeclarationIrBuilder(pluginContext, expression.symbol)

        return builder.irCall(
            callee = symbolTable.referenceSimpleFunction(keyOverload),
            type = expression.type
        ).apply {
            dispatchReceiver = expression.dispatchReceiver
            extensionReceiver = expression.extensionReceiver

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