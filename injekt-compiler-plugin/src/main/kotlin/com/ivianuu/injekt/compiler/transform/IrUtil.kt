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

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.renderToString
import com.ivianuu.injekt.compiler.uniqueKey
import com.ivianuu.shaded_injekt.Inject
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
import org.jetbrains.kotlin.ir.types.IrTypeAbbreviation
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeAbbreviationImpl
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun ClassDescriptor.irClass(
  @Inject ctx: Context,
  irCtx: IrPluginContext,
  localDeclarationCollector: LocalDeclarationCollector,
  symbolRemapper: InjectSymbolRemapper
): IrClass {
  if (visibility == DescriptorVisibilities.LOCAL)
    return localDeclarationCollector.localClasses
      .single { it.descriptor.uniqueKey() == uniqueKey() }

  return irCtx.referenceClass(fqNameSafe)!!
    .let { symbolRemapper.getReferencedClass(it) }
    .owner
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun ClassConstructorDescriptor.irConstructor(
  @Inject ctx: Context,
  irCtx: IrPluginContext,
  localDeclarationCollector: LocalDeclarationCollector,
  symbolRemapper: InjectSymbolRemapper,
): IrConstructor {
  if (constructedClass.visibility == DescriptorVisibilities.LOCAL)
    return localDeclarationCollector.localClasses
      .single { it.descriptor.uniqueKey() == constructedClass.uniqueKey() }
      .constructors
      .single { it.descriptor.uniqueKey() == uniqueKey() }

  return irCtx.referenceConstructors(constructedClass.fqNameSafe)
    .single { it.descriptor.uniqueKey() == uniqueKey() }
    .let { symbolRemapper.getReferencedConstructor(it) }
    .owner
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun FunctionDescriptor.irFunction(
  @Inject ctx: Context,
  irCtx: IrPluginContext,
  localDeclarationCollector: LocalDeclarationCollector,
  symbolRemapper: InjectSymbolRemapper
): IrFunction {
  if (visibility == DescriptorVisibilities.LOCAL)
    return localDeclarationCollector.localFunctions.single {
      it.descriptor.uniqueKey() == uniqueKey()
    }

  if (containingDeclaration.safeAs<DeclarationDescriptorWithVisibility>()
      ?.visibility == DescriptorVisibilities.LOCAL)
        return localDeclarationCollector.localClasses.flatMap { it.declarations }
          .single { it.descriptor.uniqueKey() == uniqueKey() }
          .cast()

  return irCtx.referenceFunctions(fqNameSafe)
    .single { it.descriptor.uniqueKey() == uniqueKey() }
    .let { symbolRemapper.getReferencedSimpleFunction(it) }
    .owner
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun PropertyDescriptor.irProperty(
  @Inject ctx: Context,
  irCtx: IrPluginContext,
  localDeclarationCollector: LocalDeclarationCollector,
  symbolRemapper: InjectSymbolRemapper
): IrProperty {
  if (containingDeclaration.safeAs<DeclarationDescriptorWithVisibility>()
      ?.visibility == DescriptorVisibilities.LOCAL)
        return localDeclarationCollector.localClasses.flatMap { it.declarations }
          .single { it.descriptor.uniqueKey() == uniqueKey() }
          .cast()

  return irCtx.referenceProperties(fqNameSafe)
    .single { it.descriptor.uniqueKey() == uniqueKey() }
    .let { symbolRemapper.getReferencedProperty(it) }
    .owner
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun TypeRef.toIrType(
  @Inject irCtx: IrPluginContext,
  localDeclarationCollector: LocalDeclarationCollector,
  ctx: Context,
  symbolRemapper: InjectSymbolRemapper
): IrTypeArgument {
  if (isStarProjection) return IrStarProjectionImpl
  return when {
    classifier.isTag -> arguments.last().toIrType()
      .typeOrNull!!
      .cast<IrSimpleType>()
      .let { type ->
        val tagConstructor = irCtx.referenceClass(classifier.fqName)!!
          .let { symbolRemapper.getReferencedClass(it) }
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
      val irClassifier = localDeclarationCollector.localClasses.singleOrNull {
        it.descriptor.uniqueKey() == key
      }
        ?.symbol
        ?: irCtx.referenceClass(fqName)
          ?.let { symbolRemapper.getReferencedClass(it) }
        ?: irCtx.referenceFunctions(fqName.parent())
          .flatMap {
            symbolRemapper.getReferencedFunction(it).owner.typeParameters
              .zip(it.owner.typeParameters)
          }
          .singleOrNull { it.second.descriptor.uniqueKey() == key }
          ?.first
          ?.symbol
        ?: irCtx.referenceProperties(fqName.parent())
          .flatMap {
            symbolRemapper.getReferencedProperty(it).owner.getter!!.typeParameters
              .zip(it.owner.getter!!.typeParameters)
          }
          .singleOrNull { it.second.descriptor.uniqueKey() == key }
          ?.first
          ?.symbol
        ?: (irCtx.referenceClass(fqName.parent())
          ?.let { symbolRemapper.getReferencedClass(it) }
          ?: irCtx.referenceTypeAlias(fqName.parent())
            ?.let { symbolRemapper.getReferencedTypeAlias(it) })
          ?.owner
          ?.typeParameters
          ?.singleOrNull { it.descriptor.uniqueKey() == key }
          ?.symbol
        ?: error("Could not get for $fqName $key")
      IrSimpleTypeImpl(
        irClassifier,
        isMarkedNullable,
        arguments.map { it.toIrType() },
        if (isMarkedComposable) {
          val composableConstructor = irCtx.referenceConstructors(injektFqNames().composable)
            .single()
          listOf(
            DeclarationIrBuilder(irCtx, composableConstructor)
              .irCall(composableConstructor)
          )
        } else emptyList()
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
