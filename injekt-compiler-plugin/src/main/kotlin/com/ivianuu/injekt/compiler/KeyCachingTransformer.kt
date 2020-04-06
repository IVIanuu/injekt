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
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
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
        mutableMapOf<IrDeclarationContainer, MutableMap<KeyId, IrField>>()
    private val ignoredCalls = mutableSetOf<IrCall>()

    private data class KeyId(
        val type: IrType,
        val qualifier: IrType?
    ) {

        constructor(call: IrCall) : this(
            type = call.getTypeArgument(0)!!,
            qualifier = (call.getValueArgument(0) as? IrGetObjectValue)?.type
        )

        val keyName = Name.identifier(
            type.toKotlinType().toString()
                .removeIllegalChars()
                .plus(qualifier?.toKotlinType()?.toString()?.removeIllegalChars().orEmpty())
                .decapitalize() + "Key"
        )

        private fun String.removeIllegalChars(): String {
            return replace("<", "")
                .replace(">", "")
                .replace(" ", "")
                .replace(",", "")
                .replace("*", "")
        }
    }

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
                val keyId = KeyId(call)
                fields.getOrPut(keyId) {
                    parent.addField {
                        name = keyId.keyName
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

        return super.visitModuleFragment(declaration)
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

        val keyId = KeyId(
            type = expression.getTypeArgument(0)!!,
            qualifier = (expression.getValueArgument(0) as? IrGetObjectValue)?.type
        )

        return builder.irGetField(
            null,
            keyFields.getValue(parent).getValue(KeyId(expression))
        )
    }

    private fun IrCall.isValidKeyOfCall(): Boolean {
        if (this in ignoredCalls) return false
        if (declarationContainers.isEmpty()) return false

        val descriptor = symbol.descriptor

        if (descriptor.fqNameSafe.asString() != "com.ivianuu.injekt.keyOf" ||
            descriptor.valueParameters.size != 1 ||
            !descriptor.isInline ||
            !getTypeArgument(0)!!.toKotlinType().isFullyResolved()
        ) return false

        val qualifierExpression = getValueArgument(0)

        if (qualifierExpression != null &&
            qualifierExpression !is IrGetObjectValue
        ) return false

        return true
    }
}
