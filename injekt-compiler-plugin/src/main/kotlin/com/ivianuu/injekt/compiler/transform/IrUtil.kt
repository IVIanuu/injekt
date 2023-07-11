/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, FirIncompatiblePluginAPI::class, ObsoleteDescriptorBasedAPI::class)

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.uniqueKey
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

context (Context, IrPluginContext, LocalDeclarations)
fun ClassDescriptor.irClass(): IrClass {
  if (visibility == DescriptorVisibilities.LOCAL)
    return localClasses
      .single { it.descriptor.uniqueKey() == uniqueKey() }

  return referenceClass(fqNameSafe)!!.owner
}

context (Context, IrPluginContext, LocalDeclarations)
fun ClassConstructorDescriptor.irConstructor(): IrConstructor {
  if (constructedClass.visibility == DescriptorVisibilities.LOCAL)
    return localClasses
      .single { it.descriptor.uniqueKey() == constructedClass.uniqueKey() }
      .constructors
      .single { it.descriptor.uniqueKey() == uniqueKey() }

  return referenceConstructors(constructedClass.fqNameSafe)
    .single { it.descriptor.uniqueKey() == uniqueKey() }
    .owner
}

context (Context, IrPluginContext, LocalDeclarations)
@OptIn(FirIncompatiblePluginAPI::class, ObsoleteDescriptorBasedAPI::class)
fun FunctionDescriptor.irFunction(): IrFunction {
  if (visibility == DescriptorVisibilities.LOCAL)
    return localFunctions.single {
      it.descriptor.uniqueKey() == uniqueKey()
    }

  if (containingDeclaration.safeAs<DeclarationDescriptorWithVisibility>()
      ?.visibility == DescriptorVisibilities.LOCAL)
        return localClasses.flatMap { it.declarations }
          .single { it.descriptor.uniqueKey() == uniqueKey() }
          .cast()

  return referenceFunctions(fqNameSafe)
    .single { it.descriptor.uniqueKey() == uniqueKey() }
    .owner
}

context (Context, IrPluginContext, LocalDeclarations)
fun PropertyDescriptor.irProperty(): IrProperty {
  if (containingDeclaration.safeAs<DeclarationDescriptorWithVisibility>()
      ?.visibility == DescriptorVisibilities.LOCAL)
        return localClasses.flatMap { it.declarations }
          .single { it.descriptor.uniqueKey() == uniqueKey() }
          .cast()

  return referenceProperties(fqNameSafe)
    .single { it.descriptor.uniqueKey() == uniqueKey() }
    .owner
}

context (Context, IrPluginContext, LocalDeclarations)
fun TypeRef.toIrType(): IrTypeArgument {
  if (isStarProjection) return IrStarProjectionImpl
  return when {
    classifier.isTag -> arguments.last().toIrType()
      .typeOrNull!!
      .cast<IrSimpleType>()
      .let { type ->
        val tagConstructor = referenceClass(classifier.fqName)!!
          .constructors.single()
        IrSimpleTypeImpl(
          type.originalKotlinType,
          type.classifier,
          type.nullability,
          type.arguments,
          listOf(
            DeclarationIrBuilder(this@IrPluginContext, tagConstructor)
              .irCall(
                tagConstructor,
                tagConstructor.owner.returnType
                  .classifierOrFail
                  .typeWith(
                    arguments.dropLast(1)
                      .map { it.toIrType().typeOrNull ?: irBuiltIns.anyNType }
                  )
              ).apply {
                tagConstructor.owner.typeParameters.indices
                  .forEach { index ->
                    putTypeArgument(index, arguments[index].toIrType().typeOrNull!!)
                  }
              }
          ) + type.annotations,
          type.abbreviation
        )
      }
    else -> {
      val key = classifier.descriptor!!.uniqueKey()
      val fqName = FqName(key.split(":")[1])
      val irClassifier = localClasses.singleOrNull { it.descriptor.uniqueKey() == key }
        ?.symbol
        ?: referenceClass(fqName)
        ?: referenceFunctions(fqName.parent())
          .flatMap { it.owner.typeParameters }
          .singleOrNull { it.descriptor.uniqueKey() == key }
          ?.symbol
        ?: referenceProperties(fqName.parent())
          .flatMap { it.owner.getter!!.typeParameters }
          .singleOrNull { it.descriptor.uniqueKey() == key }
          ?.symbol
        ?: (referenceClass(fqName.parent()) ?: referenceTypeAlias(fqName.parent()))
          ?.owner
          ?.typeParameters
          ?.singleOrNull { it.descriptor.uniqueKey() == key }
          ?.symbol
        ?: error("Could not get for $fqName $key")
      IrSimpleTypeImpl(
        classifier = irClassifier,
        hasQuestionMark = isMarkedNullable,
        arguments = arguments.map { it.toIrType() },
        annotations = emptyList()
      )
    }
  }
}

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
