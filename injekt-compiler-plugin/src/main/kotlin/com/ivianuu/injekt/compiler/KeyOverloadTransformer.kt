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
    private val qualifier = getClass(InjektClassNames.Qualifier)

    override fun visitCall(expression: IrCall): IrExpression {
        super.visitCall(expression)

        val descriptor = expression.symbol.descriptor

        if (!descriptor.annotations.hasAnnotation(InjektClassNames.KeyOverloadStub)) return expression

        fun MemberScope.findKeyOverloadFunction() = try {
            findFirstFunction(descriptor.name.asString()) { function ->
                function.annotations.hasAnnotation(InjektClassNames.KeyOverload) &&
                        (function.dispatchReceiverParameter
                            ?: function.extensionReceiverParameter)?.type == descriptor.extensionReceiverParameter?.type &&
                        function.typeParameters.size == descriptor.typeParameters.size &&
                        function.typeParameters.all { typeParameter ->
                            val other = descriptor.typeParameters[typeParameter.index]
                            other.name == typeParameter.name
                        } &&
                        function.valueParameters.size == descriptor.valueParameters.size &&
                        function.valueParameters.all { valueParameter ->
                            val other = descriptor.valueParameters[valueParameter.index]
                            if (valueParameter.type.constructor.declarationDescriptor == key) {
                                other.type.constructor.declarationDescriptor == qualifier
                            } else {
                                other.type.constructor == valueParameter.type.constructor
                            }
                        }
            }
        } catch (e: Exception) {
            null
        }

        val function = (descriptor.containingDeclaration as PackageFragmentDescriptor)
            .let { pluginContext.moduleDescriptor.getPackage(it.fqName).memberScope }
            .findKeyOverloadFunction()
            ?: (descriptor.extensionReceiverParameter?.value?.type?.constructor?.declarationDescriptor as? ClassDescriptor)
                ?.unsubstitutedMemberScope?.findKeyOverloadFunction()
            ?: error("Couldn't find @KeyOverload function for $descriptor")

        val keyOf = injektPackage.memberScope
            .findFirstFunction("keyOf") { it.valueParameters.size == 1 }

        val builder = DeclarationIrBuilder(pluginContext, expression.symbol)

        return builder.irCall(
            callee = symbolTable.referenceSimpleFunction(function),
            type = expression.type
        ).apply {
            if (function.dispatchReceiverParameter != null) {
                dispatchReceiver = expression.extensionReceiver
            } else {
                extensionReceiver = expression.extensionReceiver
            }

            copyTypeArgumentsFrom(expression)

            function.valueParameters.forEach { valueParameter ->
                if (valueParameter.type.constructor.declarationDescriptor == key) {
                    val typeArgument = (0 until expression.typeArgumentsCount)
                        .map { expression.getTypeArgument(it)!! }
                        .mapIndexedNotNull { index, type ->
                            if (function.typeParameters[index]?.name ==
                                valueParameter.type.arguments.single().type.constructor.declarationDescriptor?.name
                            )
                                type else null
                        }
                        .single()

                    putValueArgument(
                        valueParameter.index,
                        builder.irCall(
                            callee = symbolTable.referenceSimpleFunction(keyOf),
                            type = symbolTable.referenceClass(key)
                                .also {
                                    if (!it.isBound) pluginContext.irProvider.getDeclaration(it)
                                }
                                .typeWith(typeArgument)
                        ).apply {
                            putTypeArgument(0, typeArgument)
                            putValueArgument(0, expression.getValueArgument(valueParameter.index))
                        }
                    )
                } else {
                    putValueArgument(
                        valueParameter.index,
                        expression.getValueArgument(valueParameter.index)
                    )
                }
            }
        }
    }

}
