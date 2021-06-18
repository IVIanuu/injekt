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

import com.ivianuu.injekt.*
import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.analysis.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.*

fun TypeRef.toIrType(
  @Inject pluginContext: IrPluginContext,
  @Inject localClasses: List<IrClass>,
  @Inject context: InjektContext
): IrTypeArgument {
  if (isStarProjection) return IrStarProjectionImpl
  return when {
    classifier.isTypeAlias -> superTypes.single()
      .toIrType()
      .let {
        it as IrSimpleType
        IrSimpleTypeImpl(
          it.classifier,
          it.hasQuestionMark,
          it.arguments,
          it.annotations,
          toIrAbbreviation()
        )
      }
    classifier.isTag -> arguments.last().toIrType()
      .typeOrNull!!
      .cast<IrSimpleType>()
      .let { type ->
        val tagConstructor = pluginContext.referenceClass(classifier.fqName)!!
          .constructors.single()
        IrSimpleTypeImpl(
          type.originalKotlinType,
          type.classifier,
          type.hasQuestionMark,
          type.arguments,
          listOf(
            DeclarationIrBuilder(pluginContext, tagConstructor)
              .irCall(
                tagConstructor,
                tagConstructor.owner.returnType
                  .classifierOrFail
                  .typeWith(
                    arguments.dropLast(1)
                      .map { it.toIrType().typeOrNull!! }
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
      val irClassifier = localClasses.singleOrNull { it.descriptor.fqNameSafe == fqName }
        ?.symbol
        ?: pluginContext.referenceClass(fqName)
        ?: pluginContext.referenceFunctions(fqName.parent())
          .flatMap { it.owner.typeParameters }
          .singleOrNull { it.descriptor.uniqueKey() == key }
          ?.symbol
        ?: pluginContext.referenceProperties(fqName.parent())
          .flatMap { it.owner.getter!!.typeParameters }
          .singleOrNull { it.descriptor.uniqueKey() == key }
          ?.symbol
        ?: (pluginContext.referenceClass(fqName.parent())
          ?: pluginContext.referenceTypeAlias(fqName.parent()))
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
          val composableConstructor = pluginContext.referenceConstructors(InjektFqNames.Composable)
            .single()
          listOf(
            DeclarationIrBuilder(pluginContext, composableConstructor)
              .irCall(composableConstructor)
          )
        } else emptyList()
      )
    }
  }
}

private fun TypeRef.toIrAbbreviation(
  @Inject pluginContext: IrPluginContext,
  @Inject localClasses: List<IrClass>,
  @Inject context: InjektContext
): IrTypeAbbreviation {
  val typeAlias = pluginContext.referenceTypeAlias(classifier.fqName)!!
  return IrTypeAbbreviationImpl(
    typeAlias,
    isMarkedNullable,
    arguments.map { it.toIrType() },
    emptyList()
  )
}

fun TypeRef.toKotlinType(@Inject context: AnalysisContext): SimpleType {
  if (isStarProjection) return context.injektContext.module.builtIns.anyType
  return when {
    classifier.isTypeAlias -> superTypes.single().toKotlinType()
      .withAbbreviation(toAbbreviation())
    // todo add this tag to type
    classifier.isTag -> arguments.last().toKotlinType()
    else -> classifier.descriptor!!.original.defaultType
      .replace(
        newArguments = arguments.map {
          TypeProjectionImpl(
            Variance.INVARIANT,
            it.toKotlinType()
          )
        },
        newAnnotations = if (isMarkedComposable) {
          Annotations.create(
            listOf(
              AnnotationDescriptorImpl(
                context.injektContext.classifierDescriptorForFqName(
                  InjektFqNames.Composable,
                  NoLookupLocation.FROM_BACKEND
                )!!.defaultType,
                emptyMap(),
                SourceElement.NO_SOURCE
              )
            )
          )
        } else {
          Annotations.EMPTY
        }
      )
      .makeNullableAsSpecified(isMarkedNullable)
  }
}

fun TypeRef.toAbbreviation(@Inject context: AnalysisContext): SimpleType =
  classifier.descriptor!!.defaultType
    .replace(newArguments = arguments.map {
      TypeProjectionImpl(
        Variance.INVARIANT,
        it.toKotlinType()
      )
    })
    .makeNullableAsSpecified(isMarkedNullable)

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
