package com.ivianuu.injekt.compiler.transform.key

import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.isFullyResolved
import com.ivianuu.injekt.compiler.removeIllegalChars
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irEqualsNull
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irSetVar
import org.jetbrains.kotlin.ir.builders.irTemporaryVar
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class KeyCachingTransformer(
    context: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    bindingTrace: BindingTrace
) : AbstractInjektTransformer(context, symbolRemapper, bindingTrace) {

    private val declarationContainers = mutableListOf<IrDeclarationContainer>()
    private val keyAccessors =
        mutableMapOf<IrDeclarationContainer, MutableMap<KeyId, IrExpression>>()

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
                (it as? IrClassReference)?.classType?.classOrNull?.descriptor?.fqNameSafe
            }
        )
    }

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val keyOfCalls =
            mutableMapOf<IrDeclarationContainer, MutableSet<IrCall>>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFile(declaration: IrFile): IrFile {
                declarationContainers.push(declaration)
                return super.visitFile(declaration)
                    .also { declarationContainers.pop() }
            }

            override fun visitClass(declaration: IrClass): IrStatement {
                declarationContainers.push(declaration)
                return super.visitClass(declaration)
                    .also { declarationContainers.pop() }
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
            val fields = keyAccessors.getOrPut(parent) { mutableMapOf() }
            calls.forEach { call ->
                val keyId = KeyId(call)
                fields.getOrPut(keyId) {
                    val field = parent.addField {
                        name = keyId.keyName
                        type = call.type.makeNullable()
                        isStatic = true
                    }

                    DeclarationIrBuilder(context, call.symbol).run {
                        irBlock {
                            val tmpKey = irTemporaryVar(irGetField(null, field))
                            +irIfThen(
                                irEqualsNull(irGet(tmpKey)),
                                irBlock {
                                    +irSetVar(tmpKey.symbol, call.deepCopyWithVariables())
                                    +irSetField(null, field, irGet(tmpKey))
                                }
                            )
                            +irGet(tmpKey)
                        }
                    }
                }
            }
        }

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFile(declaration: IrFile): IrFile {
                declarationContainers.push(declaration)
                return super.visitFile(declaration)
                    .also { declarationContainers.pop() }
            }

            override fun visitClass(declaration: IrClass): IrStatement {
                declarationContainers.push(declaration)
                return super.visitClass(declaration)
                    .also { declarationContainers.pop() }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)

                if (!expression.isValidKeyOfCall()) return expression

                val parent = declarationContainers.last()

                return keyAccessors.getValue(parent).getValue(KeyId(expression))
                    .deepCopyWithVariables()
            }
        })

        return super.visitModuleFragment(declaration)
    }

    private fun IrCall.isValidKeyOfCall(): Boolean {
        check(declarationContainers.isNotEmpty()) {
            "Is empty for ${this.dump()}"
        }

        val callee = symbol.ensureBound(context.irProviders).owner

        if ((callee.fqNameForIrSerialization.asString() != "com.ivianuu.injekt.keyOf" &&
                    callee.fqNameForIrSerialization.asString() != "com.ivianuu.injekt.KeyKt.keyOf") ||
            callee.valueParameters.size != 1 ||
            !callee.isInline ||
            !getTypeArgument(0)!!.isFullyResolved()
        ) return false

        val qualifierExpression = getValueArgument(0)

        if (qualifierExpression != null && qualifierExpression !is IrClassReference) return false

        return true
    }
}
