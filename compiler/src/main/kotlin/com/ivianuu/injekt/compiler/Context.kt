/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.di.old.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.*

@Suppress("NewApi")
class Context(
  val module: ModuleDescriptor,
  val cache: InjektCache
) : TypeCheckerContext {
  override val ctx: Context get() = this

  override fun isDenotable(type: InjektType): Boolean = true

  val listClassifier by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.list.toInjektClassifier(ctx) }
  val collectionClassifier by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.collection.toInjektClassifier(ctx) }
  val nullableNothingType by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.nullableNothingType.toTypeRef(ctx = ctx) }
  val anyType by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.anyType.toTypeRef(ctx = ctx) }
  val nullableAnyType by lazy(LazyThreadSafetyMode.NONE) {
    anyType.copy(isMarkedNullable = true)
  }
  val functionType by lazy(LazyThreadSafetyMode.NONE) {
    module.findClassAcrossModuleDependencies(ClassId.topLevel(InjektFqNames.Function))!!
      .toInjektClassifier(ctx).defaultType.copy(arguments = listOf(STAR_PROJECTION_TYPE))
  }
  val typeKeyClassifier by lazy(LazyThreadSafetyMode.NONE) {
    module.findClassAcrossModuleDependencies(ClassId.topLevel(InjektFqNames.TypeKey.asSingleFqName()))
      ?.toInjektClassifier(ctx)
  }
}

data class SourcePosition(val filePath: String, val startOffset: Int, val endOffset: Int)

const val INJECTIONS_OCCURRED_IN_FILE_KEY = "injections_occurred_in_file"
const val INJECTION_RESULT_KEY = "injection_result"

class InjektCache {
  @PublishedApi internal val map = mutableMapOf<Any, Any>()

  fun <K, V : Any> cachedOrNull(kind: String, key: K): V? = map[kind to key] as? V

  inline fun <K, V : Any> cached(
    kind: String,
    key: K,
    computation: () -> V
  ): V = map.getOrPut(kind to key) { computation() } as V
}
