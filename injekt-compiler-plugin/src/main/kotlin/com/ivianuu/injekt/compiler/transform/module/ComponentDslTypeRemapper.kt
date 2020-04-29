package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.isInlineClassFieldGetter
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
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
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeAbbreviation
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeAbbreviationImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.IrProvider
import org.jetbrains.kotlin.ir.util.SymbolRemapper
import org.jetbrains.kotlin.ir.util.SymbolRenamer
import org.jetbrains.kotlin.ir.util.TypeRemapper
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

class DeepCopyIrTreeWithSymbolsPreservingMetadata(
    val context: IrPluginContext,
    val symbolRemapper: DeepCopySymbolRemapper,
    val typeRemapper: TypeRemapper,
    val typeTranslator: TypeTranslator,
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
        val srcManager = context.psiSourceManager as PsiSourceManager
        val fileEntry = srcManager.getFileEntry(declaration) as? PsiSourceManager.PsiFileEntry
        return super.visitFile(declaration).also {
            if (fileEntry != null) {
                srcManager.putFileEntry(it, fileEntry)
            }
            if (it is IrFileImpl) {
                it.metadata = declaration.metadata
            }
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrConstructorCall {
        if (!expression.symbol.isBound) context.irProviders.getDeclaration(expression.symbol)
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

    override fun visitCall(expression: IrCall): IrCall {
        if (!expression.symbol.isBound) context.irProviders.getDeclaration(expression.symbol)
        val ownerFn = expression.symbol.owner as? IrSimpleFunction
        val containingClass = expression.symbol.descriptor.containingDeclaration as? ClassDescriptor

        if (
            ownerFn != null &&
            ownerFn.origin == IrDeclarationOrigin.FAKE_OVERRIDE &&
            containingClass != null &&
            containingClass.defaultType.isFunctionType &&
            expression.dispatchReceiver?.type?.isModule() == true
        ) {
            val typeArguments = containingClass.defaultType.arguments
            val newFnClass = context.symbolTable.referenceClass(
                context.builtIns
                    .getFunction(typeArguments.size)
            )
            val newDescriptor = newFnClass
                .descriptor
                .unsubstitutedMemberScope
                .findFirstFunction(ownerFn.name.identifier) { true }

            var newFn: IrSimpleFunction = IrFunctionImpl(
                ownerFn.startOffset,
                ownerFn.endOffset,
                ownerFn.origin,
                newDescriptor,
                expression.type
            )
            symbolRemapper.visitSimpleFunction(newFn)
            newFn = super.visitSimpleFunction(newFn).also { fn ->
                context.irProviders.getDeclaration(newFnClass)
                fn.parent = newFnClass.owner
                fn.overriddenSymbols = ownerFn.overriddenSymbols.map { it }
                fn.dispatchReceiverParameter = ownerFn.dispatchReceiverParameter
                fn.extensionReceiverParameter = ownerFn.extensionReceiverParameter
                newDescriptor.valueParameters.forEach { p ->
                    fn.addValueParameter(p.name.identifier, p.type.toIrType())
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
            ownerFn.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB &&
            !ownerFn.isInlineClassFieldGetter
        ) {
            symbolRemapper.visitSimpleFunction(ownerFn)
            val newFn = super.visitSimpleFunction(ownerFn).also {
                it.parent = ownerFn.parent
                it.correspondingPropertySymbol = ownerFn.correspondingPropertySymbol
                it.patchDeclarationParents(it.parent)
            }
            val newCallee = symbolRemapper.getReferencedSimpleFunction(newFn.symbol)
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
        assert(typeArgumentsCount == other.typeArgumentsCount) {
            "Mismatching type arguments: $typeArgumentsCount vs ${other.typeArgumentsCount} "
        }
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

    /* copied verbatim from DeepCopyIrTreeWithSymbols */
    private inline fun <reified T : IrElement> T.transform() =
        transform(this@DeepCopyIrTreeWithSymbolsPreservingMetadata, null) as T

    /* copied verbatim from DeepCopyIrTreeWithSymbols */
    private fun IrType.remapType() = typeRemapper.remapType(this)

    /* copied verbatim from DeepCopyIrTreeWithSymbols */
    private fun mapStatementOrigin(origin: IrStatementOrigin?) = origin

    private fun IrElement.copyMetadataFrom(owner: IrMetadataSourceOwner) {
        when (this) {
            is IrPropertyImpl -> metadata = owner.metadata
            is IrFunctionBase<*> -> metadata = owner.metadata
            is IrClassImpl -> metadata = owner.metadata
        }
    }

    private fun IrType.isModule(): Boolean {
        return annotations.hasAnnotation(InjektFqNames.Module)
    }

    private fun KotlinType.toIrType(): IrType = typeTranslator.translateType(this)
}

class ModuleTypeRemapper(
    private val context: IrPluginContext,
    private val symbolRemapper: SymbolRemapper,
    private val typeTranslator: TypeTranslator,
    private val componentDslDescriptor: ClassDescriptor
) : TypeRemapper {

    lateinit var deepCopy: IrElementTransformerVoid

    private val scopeStack = mutableListOf<IrTypeParametersContainer>()

    private val shouldTransform: Boolean
        get() {
            return scopeStack.isEmpty() || scopeStack.last().origin != MODULE_DECOY_IMPL
        }

    override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {
        scopeStack.add(irTypeParametersContainer)
    }

    override fun leaveScope() {
        scopeStack.pop()
    }

    private fun IrType.isModule(): Boolean {
        return annotations.hasAnnotation(InjektFqNames.Module)
    }

    private val IrConstructorCall.annotationClass
        get() = this.symbol.descriptor.returnType.constructor.declarationDescriptor

    fun List<IrConstructorCall>.hasAnnotation(fqName: FqName): Boolean =
        any { it.annotationClass?.fqNameOrNull() == fqName }

    private fun KotlinType.toIrType(): IrType = typeTranslator.translateType(this)

    fun IrType.isFunction(): Boolean {
        val classifier = classifierOrNull ?: return false
        val name = classifier.descriptor.name.asString()
        if (!name.startsWith("Function")) return false
        classifier.descriptor.name
        return true
    }

    override fun remapType(type: IrType): IrType {
        if (type !is IrSimpleType) return type
        if (!type.isFunction()) return underlyingRemapType(type)
        if (!type.isModule()) return underlyingRemapType(type)
        if (!shouldTransform) return underlyingRemapType(type)
        val oldIrArguments = type.arguments
        val extraArgs = listOf(
            makeTypeProjection(
                componentDslDescriptor.defaultType
                    .replaceArgumentsWithStarProjections().toIrType(),
                Variance.INVARIANT
            )
        )
        val newIrArguments =
            oldIrArguments.subList(0, oldIrArguments.size - 1) +
                    extraArgs +
                    oldIrArguments.last()

        return IrSimpleTypeImpl(
            null,
            symbolRemapper.getReferencedClassifier(
                context.symbolTable.referenceClass(
                    context
                        .irBuiltIns
                        .builtIns
                        .getFunction(oldIrArguments.size - 1 + extraArgs.size)
                )
            ),
            type.hasQuestionMark,
            newIrArguments.map { remapTypeArgument(it) },
            emptyList(),
            null
        )
    }

    private fun underlyingRemapType(type: IrSimpleType): IrType {
        return IrSimpleTypeImpl(
            null,
            symbolRemapper.getReferencedClassifier(type.classifier),
            type.hasQuestionMark,
            type.arguments.map { remapTypeArgument(it) },
            type.annotations.map { it.transform(deepCopy, null) as IrConstructorCall },
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

fun IrPluginContext.bindIfNeeded(type: IrType) {
    if (type is IrSimpleType) {
        if (!type.classifier.isBound) this.irProviders.getDeclaration(type.classifier)
        type.arguments.forEach { if (it is IrTypeProjection) bindIfNeeded(it.type) }
    }
}

fun List<IrProvider>.getDeclaration(symbol: IrSymbol) {
    if (symbol.isBound) return
    firstNotNullResult { provider ->
        provider.getDeclaration(symbol)
    } ?: error("Could not find declaration for unbound symbol $symbol")
}
