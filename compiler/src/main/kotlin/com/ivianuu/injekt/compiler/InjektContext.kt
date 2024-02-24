/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.utils.addToStdlib.*

@Suppress("NewApi")
class InjektContext : TypeCheckerContext {
  lateinit var session: FirSession

  @PublishedApi internal val map = mutableMapOf<Any, Any>()

  fun <K, V : Any> cachedOrNull(kind: String, key: K): V? = map[kind to key] as? V

  inline fun <K, V : Any> cached(
    kind: String,
    key: K,
    computation: () -> V
  ): V = map.getOrPut(kind to key) { computation() } as V

  override val ctx: InjektContext get() = this

  override fun isDenotable(type: InjektType): Boolean = true

  val listClassifier by lazy(LazyThreadSafetyMode.NONE) {
    session.symbolProvider.getClassLikeSymbolByClassId(
      ClassId.topLevel(StandardNames.FqNames.list)
    )!!.toInjektClassifier(this)
  }
  val collectionClassifier by lazy(LazyThreadSafetyMode.NONE) {
    session.symbolProvider.getClassLikeSymbolByClassId(
      ClassId.topLevel(StandardNames.FqNames.list)
    )!!.toInjektClassifier(this)
  }
  val nullableNothingType by lazy(LazyThreadSafetyMode.NONE) {
    session.builtinTypes.nullableNothingType.coneType.toInjektType(ctx = ctx)
  }
  val anyType by lazy(LazyThreadSafetyMode.NONE) {
    session.builtinTypes.anyType.coneType.toInjektType(ctx = ctx)
  }
  val nullableAnyType by lazy(LazyThreadSafetyMode.NONE) {
    anyType.copy(isMarkedNullable = true)
  }
  val functionType by lazy(LazyThreadSafetyMode.NONE) {
    session.symbolProvider.getClassLikeSymbolByClassId(
      ClassId.topLevel(StandardNames.FqNames.functionSupertype.toSafe())
    )!!.cast<FirClassSymbol<*>>().defaultType().toInjektType(this)
      .withArguments(listOf(STAR_PROJECTION_TYPE))
  }
}

data class SourcePosition(val filePath: String, val startOffset: Int, val endOffset: Int)

const val INJECTIONS_OCCURRED_IN_FILE_KEY = "injections_occurred_in_file"
const val INJECTION_RESULT_KEY = "injection_result"
