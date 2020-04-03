package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class KeyCachingTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val declarationContainers = mutableListOf<IrDeclarationContainer>()
    private val keyFields =
        mutableMapOf<IrDeclarationContainer, MutableMap<IrType, IrField>>()
    private val ignoredCalls = mutableSetOf<IrCall>()

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val keyOfCalls =
            mutableMapOf<IrDeclarationContainer, MutableSet<IrCall>>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                return try {
                    if (declaration is IrDeclarationContainer) declarationContainers.push(
                        declaration
                    )
                    super.visitDeclaration(declaration)
                } finally {
                    if (declaration is IrDeclarationContainer) declarationContainers.pop()
                }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)

                if (expression.isValidKeyOfCall()) {
                    keyOfCalls.getOrPut(declarationContainers.last()) { mutableSetOf() } += expression
                }

                return expression
            }
        })

        keyOfCalls.forEach { (parent, calls) ->
            val fields = keyFields.getOrPut(parent) { mutableMapOf() }
            calls.forEach { call ->
                fields.getOrPut(call.type) {
                    parent.addField {
                        name = Name.identifier(
                            call.type.toKotlinType().toString()
                                .replaceFirst("Key", "")
                                .replace("<", "")
                                .replace(">", "")
                                .replace(" ", "")
                                .replace(",", "")
                                .decapitalize() + "Key"
                        )
                        type = call.type
                        isStatic = true
                        //visibility = Visibilities.PRIVATE
                    }.apply {
                        initializer = DeclarationIrBuilder(pluginContext, call.symbol)
                            .irExprBody(call.deepCopyWithVariables().also {
                                ignoredCalls += it
                            })
                    }
                }
            }
        }

        super.visitModuleFragment(declaration)

        return declaration
    }

    override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
        return try {
            if (declaration is IrDeclarationContainer) declarationContainers.push(declaration)
            super.visitDeclaration(declaration)
        } finally {
            if (declaration is IrDeclarationContainer) declarationContainers.pop()
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        super.visitCall(expression)

        if (!expression.isValidKeyOfCall()) return expression

        val parent = declarationContainers.last()

        val builder = DeclarationIrBuilder(pluginContext, expression.symbol)

        return builder.irGetField(
            null,/*(parent as? IrClass)?.thisReceiver?.let {
            builder.irGet(it)
        }, */keyFields.getValue(parent).getValue(expression.type)
        )
    }

    private fun IrCall.isValidKeyOfCall(): Boolean {
        if (this in ignoredCalls) return false
        if (declarationContainers.isEmpty()) return false

        val descriptor = symbol.descriptor

        if (descriptor.fqNameSafe.asString() != "com.ivianuu.injekt.keyOf" ||
            !getTypeArgument(0)!!.toKotlinType().isFullyResolved()
        ) {
            return false
        }

        if (getValueArgument(0) !is IrClassReference) return false

        val qualifierExpression = if (descriptor.valueParameters.size == 1)
            getValueArgument(0)
        else getValueArgument(3)

        // todo check if qualifier is static
        if (qualifierExpression != null) return false

        return true
    }
}
