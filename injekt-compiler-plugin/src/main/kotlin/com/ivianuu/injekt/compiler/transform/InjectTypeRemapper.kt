/*
 * Copyright 2021 Manuel Wrage
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

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.DISPATCH_RECEIVER_INDEX
import com.ivianuu.injekt.compiler.EXTENSION_RECEIVER_INDEX
import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.WithInjektContext
import com.ivianuu.injekt.compiler.callableInfo
import com.ivianuu.injekt.compiler.injectNTypes
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt_shaded.Inject
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.ir.propertyIfAccessor
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrMetadataSourceOwner
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeAbbreviation
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeAbbreviationImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.SymbolRemapper
import org.jetbrains.kotlin.ir.util.SymbolRenamer
import org.jetbrains.kotlin.ir.util.TypeRemapper
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isSuspendFunction
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class DeepCopyIrTreeWithSymbolsPreservingMetadata(
  private val symbolRemapper: DeepCopySymbolRemapper,
  private val typeRemapper: InjectNTypeRemapper,
  @Inject private val context: InjektContext,
  @Inject private val pluginContext: IrPluginContext,
  symbolRenamer: SymbolRenamer = SymbolRenamer.DEFAULT
) : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper, symbolRenamer) {

  override fun visitClass(declaration: IrClass): IrClass {
    return super.visitClass(declaration).also { it.copyMetadataFrom(declaration) }
  }

  override fun visitFunction(declaration: IrFunction): IrStatement {
    return super.visitFunction(declaration).also {
      it.copyMetadataFrom(declaration)
    }
  }

  override fun visitConstructor(declaration: IrConstructor): IrConstructor {
    return super.visitConstructor(declaration).also {
      it.copyMetadataFrom(declaration)
    }
  }

  override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction {
    if (declaration.symbol.isRemappedAndBound()) {
      return symbolRemapper.getReferencedSimpleFunction(declaration.symbol).owner
    }
    if (declaration.symbol.isBoundButNotRemapped()) {
      symbolRemapper.visitSimpleFunction(declaration)
    }
    typeRemapper.scopeStack.push(declaration to null)
    return super.visitSimpleFunction(declaration).also {
      typeRemapper.scopeStack.pop()
      it.correspondingPropertySymbol = declaration.correspondingPropertySymbol
      it.copyMetadataFrom(declaration)
    }
  }

  override fun visitField(declaration: IrField): IrField {
    return super.visitField(declaration).also {
      it.metadata = declaration.metadata
    }
  }

  override fun visitProperty(declaration: IrProperty): IrProperty {
    typeRemapper.scopeStack.push(declaration to null)
    return super.visitProperty(declaration).also {
      typeRemapper.scopeStack.pop()
      it.copyMetadataFrom(declaration)
      it.copyAttributes(declaration)
    }
  }

  override fun visitFile(declaration: IrFile): IrFile {
    return super.visitFile(declaration).also {
      if (it is IrFileImpl) {
        it.metadata = declaration.metadata
      }
    }
  }

  override fun visitConstructorCall(expression: IrConstructorCall): IrConstructorCall {
    if (!expression.symbol.isBound)
      (pluginContext as IrPluginContextImpl).linker.getDeclaration(expression.symbol)
    val ownerFn = expression.symbol.owner as? IrConstructor
    // If we are calling an external constructor, we want to "remap" the types of its signature
    // as well, since if it they are @Composable it will have its unmodified signature. These
    // types won't be traversed by default by the DeepCopyIrTreeWithSymbols so we have to
    // do it ourself here.
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
    if (!expression.symbol.isBound)
      (pluginContext as IrPluginContextImpl).linker.getDeclaration(expression.symbol)
    val ownerFn = expression.symbol.owner as? IrConstructor
    // If we are calling an external constructor, we want to "remap" the types of its signature
    // as well, since if it they are @Composable it will have its unmodified signature. These
    // types won't be traversed by default by the DeepCopyIrTreeWithSymbols so we have to
    // do it ourself here.
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
        expression.valueArgumentsCount,
      ).apply {
        copyRemappedTypeArgumentsFrom(expression)
        transformValueArguments(expression)
      }.copyAttributes(expression)
    }
    return super.visitDelegatingConstructorCall(expression)
  }

  private fun IrFunction.hasInjectNArguments(): Boolean {
    if (
      dispatchReceiverParameter?.type?.isInjectN() == true ||
      extensionReceiverParameter?.type?.isInjectN() == true
    ) return true

    for (param in valueParameters) {
      if (param.type.isInjectN()) return true
    }
    return false
  }

  @OptIn(ObsoleteDescriptorBasedAPI::class)
  override fun visitCall(expression: IrCall): IrCall {
    val ownerFn = expression.symbol.owner as? IrSimpleFunction
    @Suppress("DEPRECATION")
    val containingClass = expression.symbol.descriptor.containingDeclaration as? ClassDescriptor

    // Any virtual calls on composable functions we want to make sure we update the call to
    // the right function base class (of n+1 arity). The most often virtual call to make on
    // a function instance is `invoke`, which we *already* do in the ComposeParamTransformer.
    // There are others that can happen though as well, such as `equals` and `hashCode`. In this
    // case, we want to update those calls as well.
    if (ownerFn != null &&
      containingClass != null &&
      (containingClass.defaultType.isFunctionType ||
          containingClass.defaultType.isSuspendFunctionType) &&
      expression.dispatchReceiver?.type?.isInjectN() == true
    ) {
      val newTypeArgumentsSize = containingClass.defaultType.arguments.size - 1 +
          (expression.dispatchReceiver!!
            .safeAs<IrDeclarationReference>()
            ?.symbol
            ?.descriptor
            ?.safeAs<CallableDescriptor>()
            ?.callableInfo()
            ?.type
            ?.injectNTypes
            ?.size
            ?: throw AssertionError("Cannot find out inject n size for ${expression.dump()}"))
      val newFnClass = if (containingClass.defaultType.isFunctionType)
        pluginContext.irBuiltIns.function(newTypeArgumentsSize).owner
      else
        pluginContext.irBuiltIns.suspendFunction(newTypeArgumentsSize).owner

      var newFn = newFnClass
        .functions
        .first { it.name == ownerFn.name }

      symbolRemapper.visitSimpleFunction(newFn)
      typeRemapper.scopeStack.push(newFn to null)
      newFn = super.visitSimpleFunction(newFn).also { fn ->
        typeRemapper.scopeStack.pop()
        fn.parent = newFnClass
        fn.patchDeclarationParents(fn.parent)
      }

      val newCallee = symbolRemapper.getReferencedSimpleFunction(newFn.symbol)
      return shallowCopyCall(expression, newCallee).apply {
        copyRemappedTypeArgumentsFrom(expression)
        transformValueArguments(expression)
      }
    }

    // If we are calling an external function, we want to "remap" the types of its signature
    // as well, since if it is @Composable it will have its unmodified signature. These
    // functions won't be traversed by default by the DeepCopyIrTreeWithSymbols so we have to
    // do it ourself here.
    //
    // When an external declaration for a property getter/setter is transformed, we need to
    // also transform the corresponding property so that we maintain the relationship
    // `getterFun.correspondingPropertySymbol.owner.getter == getterFun`. If we do not
    // maintain this relationship inline class getters will be incorrectly compiled.
    if (
      ownerFn != null &&
      ownerFn.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
    ) {
      if (ownerFn.correspondingPropertySymbol != null) {
        val property = ownerFn.correspondingPropertySymbol!!.owner
        // avoid java properties since they go through a different lowering and it is
        // also impossible for them to have composable types
        if (property.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) {
          symbolRemapper.visitProperty(property)
          visitProperty(property).also {
            it.getter?.correspondingPropertySymbol = it.symbol
            it.setter?.correspondingPropertySymbol = it.symbol
            it.parent = ownerFn.parent
            it.patchDeclarationParents(it.parent)
            it.copyAttributes(property)
          }
        }
      } else {
        symbolRemapper.visitSimpleFunction(ownerFn)
        visitSimpleFunction(ownerFn).also {
          it.parent = ownerFn.parent
          it.correspondingPropertySymbol = null
          it.patchDeclarationParents(it.parent)
        }
      }
      val newCallee = symbolRemapper.getReferencedSimpleFunction(ownerFn.symbol)
      typeRemapper.scopeStack.push(newCallee.owner to InjectNTypeRemapper.Kind.RETURN_TYPE)
      return shallowCopyCall(expression, newCallee).apply {
        copyRemappedTypeArgumentsFrom(expression)
        transformValueArguments(expression)
        typeRemapper.scopeStack.pop()
      }
    }

    if (ownerFn != null && ownerFn.hasInjectNArguments()) {
      val newFn = visitSimpleFunction(ownerFn).also {
        it.overriddenSymbols = ownerFn.overriddenSymbols.map { override ->
          if (override.isBound) {
            visitSimpleFunction(override.owner).apply {
              parent = override.owner.parent
            }.symbol
          } else {
            override
          }
        }
        it.parent = ownerFn.parent
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

  private fun IrSimpleFunctionSymbol.isBoundButNotRemapped(): Boolean {
    return this.isBound && symbolRemapper.getReferencedFunction(this) == this
  }

  private fun IrSimpleFunctionSymbol.isRemappedAndBound(): Boolean {
    val symbol = symbolRemapper.getReferencedFunction(this)
    return symbol.isBound && symbol != this
  }

  /* copied verbatim from DeepCopyIrTreeWithSymbols, except with newCallee as a parameter */
  private fun shallowCopyCall(expression: IrCall, newCallee: IrSimpleFunctionSymbol): IrCall {
    return IrCallImpl(
      expression.startOffset, expression.endOffset,
      expression.type.remapType(),
      newCallee,
      expression.typeArgumentsCount,
      newCallee.owner.valueParameters.size,
      mapStatementOrigin(expression.origin),
      symbolRemapper.getReferencedClassOrNull(expression.superQualifierSymbol)
    ).apply {
      copyRemappedTypeArgumentsFrom(expression)
    }.copyAttributes(expression)
  }

  /* copied verbatim from DeepCopyIrTreeWithSymbols */
  private fun IrMemberAccessExpression<*>.copyRemappedTypeArgumentsFrom(
    other: IrMemberAccessExpression<*>
  ) {
    assert(typeArgumentsCount == other.typeArgumentsCount) {
      "Mismatching type arguments: $typeArgumentsCount vs ${other.typeArgumentsCount} "
    }
    for (i in 0 until typeArgumentsCount) {
      putTypeArgument(i, other.getTypeArgument(i)?.remapType())
    }
  }

  /* copied verbatim from DeepCopyIrTreeWithSymbols */
  private fun <T : IrMemberAccessExpression<*>> T.transformValueArguments(original: T) {
    transformReceiverArguments(original)
    for (i in 0 until original.valueArgumentsCount) {
      putValueArgument(i, original.getValueArgument(i)?.transform())
    }
  }

  /* copied verbatim from DeepCopyIrTreeWithSymbols */
  private fun <T : IrMemberAccessExpression<*>> T.transformReceiverArguments(original: T): T =
    apply {
      dispatchReceiver = original.dispatchReceiver?.transform()
      extensionReceiver = original.extensionReceiver?.transform()
    }

  private fun IrElement.copyMetadataFrom(owner: IrMetadataSourceOwner) {
    if (this is IrMetadataSourceOwner) {
      metadata = owner.metadata
    } else {
      throw IllegalArgumentException("Cannot copy metadata to $this")
    }
  }

  private fun IrType.isInjectN(): Boolean = hasAnnotation(injektFqNames.inject2)
}

@Suppress("DEPRECATION")
class InjectNTypeRemapper(
  private val symbolRemapper: SymbolRemapper,
  @Inject private val context: InjektContext,
  @Inject private val pluginContext: IrPluginContext
) : TypeRemapper {

  lateinit var deepCopy: IrElementTransformerVoid

  enum class Kind {
    RETURN_TYPE,
    UNKNOWN
  }

  val scopeStack = mutableListOf<Pair<IrDeclaration, Kind?>>()

  override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {
  }

  override fun leaveScope() {
  }

  private fun IrType.isInjectN(): Boolean = hasAnnotation(injektFqNames.inject2)

  @OptIn(ObsoleteDescriptorBasedAPI::class)
  private fun IrType.isFunction(): Boolean {
    val classifier = classifierOrNull ?: return false
    val name = classifier.descriptor.name.asString()
    if (!name.startsWith("Function")) return false
    classifier.descriptor.name
    return true
  }

  @OptIn(ObsoleteDescriptorBasedAPI::class)
  override fun remapType(type: IrType): IrType {
    if (type !is IrSimpleType) return type
    if (!type.isFunction() && !type.isSuspendFunction()) return underlyingRemapType(type)
    if (!type.isInjectN()) return underlyingRemapType(type)

    val owner = scopeStack.lastOrNull()

    val extraArgsCount: Int = if (owner != null &&
      owner.first.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB) {
      when (val descriptor = owner.first.descriptor) {
        is PropertyAccessorDescriptor -> {
          val ownerFunction = owner.first as IrFunction
          val property = ownerFunction.propertyIfAccessor
          val info = property.descriptor.cast<PropertyDescriptor>().callableInfo()
          when {
            type === ownerFunction.returnType || owner.second == Kind.RETURN_TYPE -> info.type.injectNTypes
            type === ownerFunction.dispatchReceiverParameter?.type -> info.parameterTypes[DISPATCH_RECEIVER_INDEX]!!.injectNTypes
            type === ownerFunction.extensionReceiverParameter?.type -> info.parameterTypes[EXTENSION_RECEIVER_INDEX]!!.injectNTypes
            else -> throw AssertionError("Unexpected type $type ${ownerFunction.dump()}")
          }.size
        }
        else -> throw AssertionError("Unexpected owner $descriptor")
      }
    } else {
      type.toKotlinType().injectNTypes().size
    }

    val oldIrArguments = type.arguments

    val extraArgs = (0 until extraArgsCount)
      .map { makeTypeProjection(pluginContext.irBuiltIns.anyNType, Variance.INVARIANT) }

    val newIrArguments =
      oldIrArguments.subList(0, oldIrArguments.size - 1) +
          extraArgs +
          oldIrArguments.last()

    val newArgSize = oldIrArguments.size - 1 + extraArgs.size
    val functionCls = if (type.isFunction())
      pluginContext.irBuiltIns.function(newArgSize)
    else
      pluginContext.irBuiltIns.suspendFunction(newArgSize)

    return IrSimpleTypeImpl(
      null,
      functionCls,
      type.hasQuestionMark,
      newIrArguments.map { remapTypeArgument(it) },
      type.annotations.filter { !it.isInjectNAnnotation() }.map {
        it.transform(deepCopy, null) as IrConstructorCall
      },
      null
    )
  }

  private fun underlyingRemapType(type: IrSimpleType): IrType = IrSimpleTypeImpl(
    null,
    symbolRemapper.getReferencedClassifier(type.classifier),
    type.hasQuestionMark,
    type.arguments.map { remapTypeArgument(it) },
    type.annotations.map { it.transform(deepCopy, null) as IrConstructorCall },
    type.abbreviation?.remapTypeAbbreviation()
  )

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

@WithInjektContext private fun IrConstructorCall.isInjectNAnnotation() =
  symbol.owner.parent.fqNameForIrSerialization == injektFqNames.inject2
