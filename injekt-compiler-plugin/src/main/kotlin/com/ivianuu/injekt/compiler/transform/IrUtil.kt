/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import com.ivianuu.shaded_injekt.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun ClassDescriptor.irClass(
  @Inject ctx: Context,
  irCtx: IrPluginContext,
  localDeclarations: LocalDeclarations
): IrClass {
  if (visibility == DescriptorVisibilities.LOCAL)
    return localDeclarations.localClasses
      .single { it.descriptor.uniqueKey() == uniqueKey() }

  return irCtx.referenceClass(fqNameSafe)!!.owner
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun ClassConstructorDescriptor.irConstructor(
  @Inject ctx: Context,
  irCtx: IrPluginContext,
  localDeclarations: LocalDeclarations
): IrConstructor {
  if (constructedClass.visibility == DescriptorVisibilities.LOCAL)
    return localDeclarations.localClasses
      .single { it.descriptor.uniqueKey() == constructedClass.uniqueKey() }
      .constructors
      .single { it.descriptor.uniqueKey() == uniqueKey() }

  return irCtx.referenceConstructors(constructedClass.fqNameSafe)
    .single { it.descriptor.uniqueKey() == uniqueKey() }
    .owner
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun FunctionDescriptor.irFunction(
  @Inject ctx: Context,
  irCtx: IrPluginContext,
  localDeclarations: LocalDeclarations
): IrFunction {
  if (visibility == DescriptorVisibilities.LOCAL)
    return localDeclarations.localFunctions.single {
      it.descriptor.uniqueKey() == uniqueKey()
    }

  if (containingDeclaration.safeAs<DeclarationDescriptorWithVisibility>()
      ?.visibility == DescriptorVisibilities.LOCAL)
        return localDeclarations.localClasses.flatMap { it.declarations }
          .single { it.descriptor.uniqueKey() == uniqueKey() }
          .cast()

  return irCtx.referenceFunctions(fqNameSafe)
    .single { it.descriptor.uniqueKey() == uniqueKey() }
    .owner
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun PropertyDescriptor.irProperty(
  @Inject ctx: Context,
  irCtx: IrPluginContext,
  localDeclarations: LocalDeclarations
): IrProperty {
  if (containingDeclaration.safeAs<DeclarationDescriptorWithVisibility>()
      ?.visibility == DescriptorVisibilities.LOCAL)
        return localDeclarations.localClasses.flatMap { it.declarations }
          .single { it.descriptor.uniqueKey() == uniqueKey() }
          .cast()

  return irCtx.referenceProperties(fqNameSafe)
    .single { it.descriptor.uniqueKey() == uniqueKey() }
    .owner
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun TypeRef.toIrType(
  @Inject irCtx: IrPluginContext,
  localDeclarations: LocalDeclarations,
  ctx: Context
): IrTypeArgument {
  if (isStarProjection) return IrStarProjectionImpl
  return when {
    classifier.isTag -> arguments.last().toIrType()
      .typeOrNull!!
      .cast<IrSimpleType>()
      .let { type ->
        val tagConstructor = irCtx.referenceClass(classifier.fqName)!!
          .constructors.single()
        IrSimpleTypeImpl(
          type.originalKotlinType,
          type.classifier,
          type.hasQuestionMark,
          type.arguments,
          listOf(
            DeclarationIrBuilder(irCtx, tagConstructor)
              .irCall(
                tagConstructor,
                tagConstructor.owner.returnType
                  .classifierOrFail
                  .typeWith(
                    arguments.dropLast(1)
                      .map {
                        it.toIrType().typeOrNull ?: irCtx.irBuiltIns.anyNType
                      }
                  )
              ).apply {
                tagConstructor.owner.typeParameters.indices
                  .forEach { index ->
                    putTypeArgument(
                      index,
                      arguments[index].toIrType().typeOrNull!!
                    )
                  }
              }
          ) + type.annotations,
          type.abbreviation
        )
      }
    else -> {
      val key = classifier.descriptor!!.uniqueKey()
      val fqName = FqName(key.split(":")[1])
      val irClassifier = localDeclarations.localClasses.singleOrNull {
        it.descriptor.uniqueKey() == key
      }
        ?.symbol
        ?: irCtx.referenceClass(fqName)
        ?: irCtx.referenceFunctions(fqName.parent())
          .flatMap { it.owner.typeParameters }
          .singleOrNull { it.descriptor.uniqueKey() == key }
          ?.symbol
        ?: irCtx.referenceProperties(fqName.parent())
          .flatMap { it.owner.getter!!.typeParameters }
          .singleOrNull { it.descriptor.uniqueKey() == key }
          ?.symbol
        ?: (irCtx.referenceClass(fqName.parent()) ?: irCtx.referenceTypeAlias(fqName.parent()))
          ?.owner
          ?.typeParameters
          ?.singleOrNull { it.descriptor.uniqueKey() == key }
          ?.symbol
        ?: error("Could not get for $fqName $key")
      IrSimpleTypeImpl(
        irClassifier,
        isMarkedNullable,
        arguments.map { it.toIrType() },
        emptyList()
      )
    }
  }
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrBuilderWithScope.irLambda(
  type: IrType,
  startOffset: Int = UNDEFINED_OFFSET,
  endOffset: Int = UNDEFINED_OFFSET,
  parameterNameProvider: (Int) -> String = { "p$it" },
  body: IrBuilderWithScope.(IrFunction) -> IrExpression,
): IrExpression {
  type as IrSimpleType
  val returnType = type.arguments.last().typeOrNull!!

  val lambda = IrFactoryImpl.buildFun {
    origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
    name = Name.special("<anonymous>")
    this.returnType = returnType
    visibility = DescriptorVisibilities.LOCAL
    isSuspend = type.classifier.descriptor.fqNameSafe.asString()
      .startsWith("kotlin.coroutines.SuspendFunction")
  }.apply {
    parent = scope.getLocalDeclarationParent()
    type.arguments.forEachIndexed { index, typeArgument ->
      if (index < type.arguments.lastIndex) {
        addValueParameter(
          parameterNameProvider(index),
          typeArgument.typeOrNull!!
        )
      }
    }
    annotations = annotations + type.annotations.map {
      it.deepCopyWithSymbols()
    }
    this.body = DeclarationIrBuilder(context, symbol).run {
      irBlockBody {
        +irReturn(body(this, this@apply))
      }
    }
  }

  return IrFunctionExpressionImpl(
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    function = lambda,
    origin = IrStatementOrigin.LAMBDA
  )
}
