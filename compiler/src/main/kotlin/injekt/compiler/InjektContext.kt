/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.compiler

import injekt.compiler.resolution.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.plugin.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.*

@Suppress("NewApi")
class InjektContext : TypeCheckerContext {
  lateinit var session: FirSession

  @PublishedApi internal val maps = mutableMapOf<String, MutableMap<Any?, Any?>>()

  fun <K, V> cachedOrNull(kind: String, key: K): V? =
    maps[kind]?.get(key)?.takeIf { it !== Null } as? V

  inline fun <K, V> cached(
    kind: String,
    key: K,
    computation: () -> V
  ): V = maps.getOrPut(kind) { mutableMapOf() }
    .getOrPut(key) { computation() ?: Null }
    .takeIf { it !== Null } as V

  override val ctx: InjektContext get() = this

  override fun isDenotable(type: InjektType): Boolean = true

  val listClassifier by lazy(LazyThreadSafetyMode.NONE) {
    session.symbolProvider.getClassLikeSymbolByClassId(StandardClassIds.List)!!.toInjektClassifier(this)
  }
  val collectionClassifier by lazy(LazyThreadSafetyMode.NONE) {
    session.symbolProvider.getClassLikeSymbolByClassId(StandardClassIds.Collection)!!.toInjektClassifier(this)
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
    StandardClassIds.Function.createConeType(session, arrayOf(ConeStarProjection))
      .toInjektType(ctx)
  }
  val typeKeyClassifier by lazy(LazyThreadSafetyMode.NONE) {
    session.symbolProvider.getClassLikeSymbolByClassId(InjektFqNames.TypeKey)
      ?.toInjektClassifier(ctx)
  }

  val scopeSession by lazy(LazyThreadSafetyMode.NONE) { ScopeSession() }

  companion object {
    val Null = Any()
  }
}

data class SourcePosition(val filePath: String, val endOffset: Int)

const val INJECTIONS_OCCURRED_IN_FILE_KEY = "injections_occurred_in_file"
const val INJECTION_RESULT_KEY = "injection_result"
