/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, UnsafeDuringIrConstructionAPI::class)

package com.ivianuu.injekt.compiler.ir

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.ir.InjectCallTransformer.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.lazy.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.utils.addToStdlib.*

abstract class AbstractInjektTransformer(
  val compilationDeclarations: CompilationDeclarations,
  val irCtx: IrPluginContext,
  val ctx: InjektContext
) : IrElementTransformerVoidWithContext() {
  protected inline fun <reified T : FirBasedSymbol<*>> IrSymbol.toFirSymbol() =
    (owner.safeAs<IrMetadataSourceOwner>()?.metadata?.safeAs<FirMetadataSource>()?.fir?.symbol
      ?: owner.safeAs<AbstractFir2IrLazyDeclaration<*>>()?.fir?.safeAs<FirMemberDeclaration>()?.symbol)
      ?.safeAs<T>()

  protected fun FirClassifierSymbol<*>.toIrClassifierSymbol(): IrSymbol = when (this) {
    is FirClassSymbol<*> -> compilationDeclarations.declarations
      .singleOrNull { it.toFirSymbol<FirClassSymbol<*>>() == this }
      ?: irCtx.referenceClass(classId)
        ?.takeIf { it.toFirSymbol<FirClassSymbol<*>>() == this }
      ?: error("wtf $this")

    is FirTypeAliasSymbol -> irCtx.referenceTypeAlias(classId) ?: error("wtf $this")
    is FirTypeParameterSymbol -> (containingDeclarationSymbol
      .safeAs<FirCallableSymbol<*>>()
      ?.toIrCallableSymbol()
      ?.owner
      ?.typeParameters
      ?: containingDeclarationSymbol
        .safeAs<FirClassifierSymbol<*>>()
        ?.toIrClassifierSymbol()
        ?.owner
        ?.cast<IrTypeParametersContainer>()
        ?.typeParameters)
      ?.singleOrNull { it.name == name }
      ?.symbol
      ?: error("wtf $this")

    else -> throw AssertionError("Unexpected classifier $this")
  }

  protected fun FirCallableSymbol<*>.toIrCallableSymbol(): IrFunctionSymbol = when (this) {
    is FirConstructorSymbol -> compilationDeclarations.declarations
      .singleOrNull { it.toFirSymbol<FirConstructorSymbol>() == this }
      ?.cast<IrConstructorSymbol>()
      ?: irCtx.referenceConstructors(resolvedReturnType.classId!!)
        .singleOrNull { it.toFirSymbol<FirConstructorSymbol>() == this }
      ?: error("wtf $this")

    is FirFunctionSymbol<*> -> compilationDeclarations.declarations.singleOrNull {
      it.toFirSymbol<FirFunctionSymbol<*>>() == this
    }
      ?.cast<IrFunctionSymbol>()
      ?: irCtx.referenceFunctions(callableId)
        .singleOrNull { it.toFirSymbol<FirFunctionSymbol<*>>() == this }
      ?: error("wtf $this")

    is FirPropertySymbol -> (compilationDeclarations.declarations
      .singleOrNull { it.toFirSymbol<FirPropertySymbol>() == this }
      ?.cast<IrPropertySymbol>()
      ?: irCtx.referenceProperties(callableId)
        .singleOrNull { it.toFirSymbol<FirPropertySymbol>() == this })
      ?.owner
      ?.getter
      ?.symbol
      ?: error("wtf $this")

    else -> throw AssertionError("Unexpected callable $this")
  }

  protected fun InjektType.toIrType(irBuilder: DeclarationIrBuilder): IrTypeArgument = when {
    isStarProjection -> IrStarProjectionImpl
    classifier.isTag -> arguments.last().toIrType(irBuilder)
      .typeOrFail
      .addAnnotations(
        listOf(
          irBuilder.irCallConstructor(
            irCtx.referenceClass(classifier.classId!!)!!.constructors.single(),
            arguments.dropLast(1).map { it.toIrType(irBuilder).typeOrFail }
          )
        )
      ).cast()

    else -> IrSimpleTypeImpl(
      classifier.symbol!!.toIrClassifierSymbol().cast(),
      isMarkedNullable,
      arguments.map { it.toIrType(irBuilder) },
      emptyList()
    )
  }
}