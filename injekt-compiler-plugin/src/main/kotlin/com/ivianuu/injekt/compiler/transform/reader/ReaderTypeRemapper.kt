/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler.transform.reader

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.makeKotlinType
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.tmpSuspendFunction
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrMetadataSourceOwner
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionBase
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeAbbreviation
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeAbbreviationImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.SymbolRemapper
import org.jetbrains.kotlin.ir.util.SymbolRenamer
import org.jetbrains.kotlin.ir.util.TypeRemapper
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isSuspendFunction
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.types.Variance

@OptIn(ObsoleteDescriptorBasedAPI::class)
class DeepCopyIrTreeWithSymbolsPreservingMetadata(
    private val context: IrPluginContext,
    private val symbolRemapper: DeepCopySymbolRemapper,
    typeRemapper: TypeRemapper,
    symbolRenamer: SymbolRenamer = SymbolRenamer.DEFAULT
) : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper, symbolRenamer) {

    override fun visitClass(declaration: IrClass): IrClass {
        return super.visitClass(declaration).also { it.copyMetadataFrom(declaration) }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        return super.visitFunction(declaration).also { it.copyMetadataFrom(declaration) }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction {
        return super.visitSimpleFunction(declaration).also {
            it.correspondingPropertySymbol = declaration.correspondingPropertySymbol
            it.copyMetadataFrom(declaration)
        }
    }

    override fun visitField(declaration: IrField): IrField {
        return super.visitField(declaration).also {
            (it as IrFieldImpl).metadata = declaration.metadata
        }
    }

    override fun visitProperty(declaration: IrProperty): IrProperty {
        return super.visitProperty(declaration).also { it.copyMetadataFrom(declaration) }
    }

    override fun visitFile(declaration: IrFile): IrFile {
        return super.visitFile(declaration).also {
            if (it is IrFileImpl) {
                it.metadata = declaration.metadata
            }
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrConstructorCall {
        val ownerFn = expression.symbol.owner as? IrConstructor
        if (
            ownerFn != null &&
            ownerFn.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
        ) {
            symbolRemapper.visitConstructor(ownerFn)
            val newFn = super.visitConstructor(ownerFn).also {
                it.parent = ownerFn.parent
                it.patchDeclarationParents(it.parent)
            }
            val newCallee = symbolRemapper.getReferencedConstructor(newFn.symbol)

            return IrConstructorCallImpl(
                expression.startOffset, expression.endOffset,
                expression.type.remapType(),
                newCallee,
                expression.typeArgumentsCount,
                expression.constructorTypeArgumentsCount,
                expression.valueArgumentsCount,
                mapStatementOrigin(expression.origin)
            ).apply {
                copyRemappedTypeArgumentsFrom(expression)
                transformValueArguments(expression)
            }.copyAttributes(expression)
        }
        return super.visitConstructorCall(expression)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrDelegatingConstructorCall {
        val ownerFn = expression.symbol.owner as? IrConstructor
        if (
            ownerFn != null &&
            ownerFn.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
        ) {
            symbolRemapper.visitConstructor(ownerFn)
            val newFn = super.visitConstructor(ownerFn).also {
                it.parent = ownerFn.parent
                it.patchDeclarationParents(it.parent)
            }
            val newCallee = symbolRemapper.getReferencedConstructor(newFn.symbol)

            return IrDelegatingConstructorCallImpl(
                expression.startOffset, expression.endOffset,
                expression.type.remapType(),
                newCallee,
                expression.typeArgumentsCount,
                expression.valueArgumentsCount
            ).apply {
                copyRemappedTypeArgumentsFrom(expression)
                transformValueArguments(expression)
            }.copyAttributes(expression)
        }
        return super.visitDelegatingConstructorCall(expression)
    }

    override fun visitCall(expression: IrCall): IrCall {
        val ownerFn = expression.symbol.owner as? IrSimpleFunction
        val containingClass = expression.symbol.descriptor.containingDeclaration as? ClassDescriptor

        if (
            ownerFn != null &&
            ownerFn.origin == IrDeclarationOrigin.FAKE_OVERRIDE &&
            containingClass != null &&
            containingClass.defaultType.isFunctionType &&
            expression.dispatchReceiver?.type?.isReader() == true
        ) {
            val typeArguments = containingClass.defaultType.arguments
            val newFnClass = context.tmpFunction(typeArguments.size)
            val newInvokeSymbol = newFnClass
                .functions
                .first { it.owner.name == ownerFn.name }

            var newFn: IrSimpleFunction = IrFunctionImpl(
                ownerFn.startOffset,
                ownerFn.endOffset,
                ownerFn.origin,
                IrSimpleFunctionSymbolImpl(newInvokeSymbol.descriptor),
                expression.type,
                newInvokeSymbol.descriptor
            )
            symbolRemapper.visitSimpleFunction(newFn)
            newFn = super.visitSimpleFunction(newFn).also { fn ->
                fn.parent = newFnClass.owner
                fn.overriddenSymbols = ownerFn.overriddenSymbols.map { it }
                fn.dispatchReceiverParameter = ownerFn.dispatchReceiverParameter
                fn.extensionReceiverParameter = ownerFn.extensionReceiverParameter
                newInvokeSymbol.owner.valueParameters.forEach { p ->
                    fn.addValueParameter(p.name.identifier, p.type)
                }
                fn.patchDeclarationParents(fn.parent)
                assert(fn.body == null) { "expected body to be null" }
            }

            val newCallee = symbolRemapper.getReferencedSimpleFunction(newFn.symbol)
            return shallowCopyCall(expression, newCallee).apply {
                copyRemappedTypeArgumentsFrom(expression)
                transformValueArguments(expression)
            }
        }

        if (
            ownerFn != null &&
            ownerFn.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
        ) {
            if (ownerFn.correspondingPropertySymbol != null) {
                val property = ownerFn.correspondingPropertySymbol!!.owner
                symbolRemapper.visitProperty(property)
                super.visitProperty(property).also {
                    it.getter?.correspondingPropertySymbol = it.symbol
                    it.setter?.correspondingPropertySymbol = it.symbol
                    it.parent = ownerFn.parent
                    it.patchDeclarationParents(it.parent)
                }
            } else {
                symbolRemapper.visitSimpleFunction(ownerFn)
                super.visitSimpleFunction(ownerFn).also {
                    it.parent = ownerFn.parent
                    it.correspondingPropertySymbol = ownerFn.correspondingPropertySymbol
                    it.patchDeclarationParents(it.parent)
                }
            }
            val newCallee = symbolRemapper.getReferencedSimpleFunction(ownerFn.symbol)
            return shallowCopyCall(expression, newCallee).apply {
                copyRemappedTypeArgumentsFrom(expression)
                transformValueArguments(expression)
            }
        }

        return super.visitCall(expression)
    }

    /* copied verbatim from DeepCopyIrTreeWithSymbols, except with newCallee as a parameter */
    private fun shallowCopyCall(expression: IrCall, newCallee: IrSimpleFunctionSymbol): IrCall {
        return IrCallImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            newCallee,
            expression.typeArgumentsCount,
            expression.valueArgumentsCount,
            mapStatementOrigin(expression.origin),
            symbolRemapper.getReferencedClassOrNull(expression.superQualifierSymbol)
        ).apply {
            copyRemappedTypeArgumentsFrom(expression)
        }.copyAttributes(expression)
    }

    /* copied verbatim from DeepCopyIrTreeWithSymbols */
    private fun IrMemberAccessExpression.copyRemappedTypeArgumentsFrom(
        other: IrMemberAccessExpression
    ) {
        for (i in 0 until typeArgumentsCount) {
            putTypeArgument(i, other.getTypeArgument(i)?.remapType())
        }
    }

    /* copied verbatim from DeepCopyIrTreeWithSymbols */
    private fun <T : IrMemberAccessExpression> T.transformValueArguments(original: T) {
        transformReceiverArguments(original)
        for (i in 0 until original.valueArgumentsCount) {
            putValueArgument(i, original.getValueArgument(i)?.transform())
        }
    }

    /* copied verbatim from DeepCopyIrTreeWithSymbols */
    private fun <T : IrMemberAccessExpression> T.transformReceiverArguments(original: T): T =
        apply {
            dispatchReceiver = original.dispatchReceiver?.transform()
            extensionReceiver = original.extensionReceiver?.transform()
        }

    private fun IrElement.copyMetadataFrom(owner: IrMetadataSourceOwner) {
        when (this) {
            is IrPropertyImpl -> metadata = owner.metadata
            is IrFunctionBase<*> -> metadata = owner.metadata
            is IrClassImpl -> metadata = owner.metadata
        }
    }

    private fun IrType.isReader(): Boolean {
        return annotations.hasAnnotation(InjektFqNames.Reader)
    }

}

@OptIn(ObsoleteDescriptorBasedAPI::class)
class ReaderTypeRemapper(
    private val context: IrPluginContext,
    private val symbolRemapper: SymbolRemapper
) : TypeRemapper {

    override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {
    }

    override fun leaveScope() {
    }

    override fun remapType(type: IrType): IrType {
        if (type !is IrSimpleType) return type
        if (!type.isFunction() && !type.isSuspendFunction()) return underlyingRemapType(type)
        if (!type.hasAnnotation(InjektFqNames.Reader)) return underlyingRemapType(type)
        val oldIrArguments = type.arguments
        val extraArgs = listOf(
            makeTypeProjection(
                context.irBuiltIns.anyType,
                Variance.INVARIANT
            )
        )
        val newIrArguments =
            oldIrArguments.subList(0, oldIrArguments.size - 1) +
                    extraArgs +
                    oldIrArguments.last()

        val classifier = symbolRemapper.getReferencedClassifier(
            if (type.isSuspendFunction()) {
                context
                    .tmpSuspendFunction(oldIrArguments.size - 1 + extraArgs.size)
            } else {
                context
                    .tmpFunction(oldIrArguments.size - 1 + extraArgs.size)
            }
        )
        val newArguments = newIrArguments.map { remapTypeArgument(it) }

        return IrSimpleTypeImpl(
            makeKotlinType(classifier, newArguments, type.hasQuestionMark, type.annotations),
            classifier,
            type.hasQuestionMark,
            newArguments,
            type.annotations,
            null
        )
    }

    private fun underlyingRemapType(type: IrSimpleType): IrType {
        val classifier = symbolRemapper.getReferencedClassifier(type.classifier)
        val arguments = type.arguments.map { remapTypeArgument(it) }
        return IrSimpleTypeImpl(
            makeKotlinType(classifier, arguments, type.hasQuestionMark, type.annotations),
            classifier,
            type.hasQuestionMark,
            arguments,
            type.annotations,
            type.abbreviation?.remapTypeAbbreviation()
        )
    }

    private fun remapTypeArgument(typeArgument: IrTypeArgument): IrTypeArgument =
        if (typeArgument is IrTypeProjection)
            makeTypeProjection(this.remapType(typeArgument.type), typeArgument.variance)
        else
            typeArgument

    private fun IrTypeAbbreviation.remapTypeAbbreviation() =
        IrTypeAbbreviationImpl(
            symbolRemapper.getReferencedTypeAlias(typeAlias),
            hasQuestionMark,
            arguments.map { remapTypeArgument(it) },
            annotations
        )
}
