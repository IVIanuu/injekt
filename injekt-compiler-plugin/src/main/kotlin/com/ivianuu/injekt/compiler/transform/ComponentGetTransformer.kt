package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.resolve.Key
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ComponentGetTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val component = getTopLevelClass(InjektFqNames.Component)
    private val componentOwner = getTopLevelClass(InjektFqNames.ComponentOwner)

    override fun visitCall(expression: IrCall): IrExpression {
        return if (expression.symbol.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.get" &&
            expression.symbol.descriptor.typeParameters.single().isReified
        ) {
            DeclarationIrBuilder(pluginContext, expression.symbol).run {
                irCall(
                    callee = symbolTable.referenceSimpleFunction(
                        (if (expression.symbol.descriptor.extensionReceiverParameter?.type == component.defaultType)
                            component.unsubstitutedMemberScope
                        else componentOwner.unsubstitutedMemberScope)
                            .findSingleFunction(Name.identifier("get")),
                    ),
                    type = expression.getTypeArgument(0)!!
                ).apply {
                    dispatchReceiver = expression.extensionReceiver

                    putValueArgument(
                        0,
                        irInt(
                            Key(
                                expression.getTypeArgument(0)!!,
                                expression.getValueArgument(0)
                                    ?.safeAs<IrVarargImpl>()
                                    ?.elements
                                    ?.filterIsInstance<IrGetObjectValue>()
                                    ?.map {
                                        it.type.classOrNull!!.descriptor.containingDeclaration
                                            .fqNameSafe
                                    } ?: emptyList()
                            ).hashCode()
                        )
                    )
                }
            }
        } else {
            super.visitCall(expression)
        }
    }

}
