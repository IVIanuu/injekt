package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.ir.propertyIfAccessor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class KeyCachingTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val declarationContainers = mutableListOf<IrDeclarationContainer>()
    private val keyFields =
        mutableMapOf<IrDeclarationContainer, MutableMap<KeyId, IrField>>()
    private val ignoredCalls = mutableSetOf<IrCall>()

    private data class KeyId(
        val type: IrType,
        val qualifier: FqName?
    ) {

        val keyName = Name.identifier(
            "key${
            type.toKotlinType().toString()
                .removeIllegalChars()
                .plus(qualifier?.asString()?.removeIllegalChars().orEmpty())
                .decapitalize()
                .hashCode()
                .toString()
                .removeIllegalChars()
            }"
        )
    }

    private fun KeyId(call: IrCall): KeyId {
        return KeyId(
            type = call.getTypeArgument(0)!!,
            qualifier = call.getValueArgument(0)?.let {
                (it as? IrCall)?.symbol?.ensureBound()?.owner?.propertyIfAccessor
                    ?.safeAs<IrProperty>()?.fqNameWhenAvailable
            }
        )
    }

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val keyOfCalls =
            mutableMapOf<IrDeclarationContainer, MutableSet<IrCall>>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFile(declaration: IrFile): IrFile {
                return try {
                    declarationContainers.push(declaration)
                    super.visitFile(declaration)
                } finally {
                    declarationContainers.pop()
                }
            }

            override fun visitClass(declaration: IrClass): IrStatement {
                return try {
                    declarationContainers.push(declaration)
                    super.visitClass(declaration)
                } finally {
                    declarationContainers.pop()
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
                        parent.declarations -= this
                        parent.declarations.add(0, this)
                    }
                }
            }
        }

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFile(declaration: IrFile): IrFile {
                return try {
                    declarationContainers.push(declaration)
                    super.visitFile(declaration)
                } finally {
                    declarationContainers.pop()
                }
            }

            override fun visitClass(declaration: IrClass): IrStatement {
                return try {
                    declarationContainers.push(declaration)
                    super.visitClass(declaration)
                } finally {
                    declarationContainers.pop()
                }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)

                if (!expression.isValidKeyOfCall()) return expression

                val parent = declarationContainers.last()

                return DeclarationIrBuilder(pluginContext, expression.symbol).irGetField(
                    null,
                    keyFields.getValue(parent).getValue(KeyId(expression))
                )
            }
        })

        return super.visitModuleFragment(declaration)
    }

    private fun IrCall.isValidKeyOfCall(): Boolean {
        if (this in ignoredCalls) return false

        check(declarationContainers.isNotEmpty()) {
            "Is empty for ${this.dump()}"
        }

        val callee = symbol.ensureBound().owner

        if ((callee.fqNameForIrSerialization.asString() != "com.ivianuu.injekt.keyOf" &&
                    callee.fqNameForIrSerialization.asString() != "com.ivianuu.injekt.KeyKt.keyOf") ||
            callee.valueParameters.size != 1 ||
            !callee.isInline ||
            !getTypeArgument(0)!!.isFullyResolved()
        ) return false

        val qualifierExpression = getValueArgument(0)

        if (qualifierExpression != null && qualifierExpression !is IrGetObjectValue) return false

        return true
    }
}
