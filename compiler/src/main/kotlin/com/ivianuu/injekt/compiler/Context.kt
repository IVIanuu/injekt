/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

@Suppress("NewApi")
class Context(val module: ModuleDescriptor, val trace: BindingTrace?) {
  fun withTrace(trace: BindingTrace?) = Context(module, trace)

  val frameworkKeyType by lazy(LazyThreadSafetyMode.NONE) {
    module.findClassAcrossModuleDependencies(ClassId.topLevel(InjektFqNames.FrameworkKey))!!
      .defaultType
  }
  val listClassifier get() = module.builtIns.list
  val collectionClassifier get() = module.builtIns.collection
  val anyType get() = module.builtIns.anyType
  val nullableAnyType get() = module.builtIns.nullableAnyType
  val functionType by lazy(LazyThreadSafetyMode.NONE) {
    module.findClassAcrossModuleDependencies(ClassId.topLevel(InjektFqNames.Function))!!
      .defaultType
  }
  val typeKeyClassifier by lazy(LazyThreadSafetyMode.NONE) {
    module.findClassAcrossModuleDependencies(ClassId.topLevel(InjektFqNames.TypeKey))
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
