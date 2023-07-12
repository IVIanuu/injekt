/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.TypeCheckerContext
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy
import org.jetbrains.kotlin.util.slicedMap.WritableSlice
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction

@Suppress("NewApi")
class Context(val module: ModuleDescriptor, val trace: BindingTrace?) : TypeCheckerContext {
  fun withTrace(trace: BindingTrace?) = Context(module, trace)

  override val ctx: Context get() = this

  override fun isDenotable(type: TypeRef): Boolean = true

  val listClassifier by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.list.toClassifierRef(ctx) }
  val collectionClassifier by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.collection.toClassifierRef(ctx) }
  val nullableNothingType by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.nullableNothingType.toTypeRef(ctx = ctx) }
  val anyType by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.anyType.toTypeRef(ctx = ctx) }
  val nullableAnyType by lazy(LazyThreadSafetyMode.NONE) {
    anyType.copy(isMarkedNullable = true)
  }
  val typeKeyClassifier by lazy(LazyThreadSafetyMode.NONE) {
    module.findClassAcrossModuleDependencies(ClassId.topLevel(InjektFqNames.TypeKey))
      ?.toClassifierRef(ctx)
  }
}

private val slices = mutableMapOf<String, BasicWritableSlice<*, *>>()
fun <K, V> sliceOf(kind: String): WritableSlice<K, V> =
  slices.getOrPut(kind) { BasicWritableSlice<K, V>(RewritePolicy.DO_NOTHING) } as BasicWritableSlice<K, V>

fun <K, V> Context.cachedOrNull(kind: String, key: K): V? = trace?.get(sliceOf<K, V>(kind), key)

inline fun <K, V> Context.cached(
  kind: String,
  key: K,
  computation: () -> V
): V {
  return if (trace == null) computation()
  else {
    val slice = sliceOf<K, V>(kind)
    trace.get(slice, key) ?: computation().also { trace.record(slice, key, it) }
  }
}

data class SourcePosition(val filePath: String, val startOffset: Int, val endOffset: Int)

const val INJECTIONS_OCCURRED_IN_FILE_KEY = "injections_occurred_in_file"
const val INJECTION_RESULT_KEY = "injection_result"
