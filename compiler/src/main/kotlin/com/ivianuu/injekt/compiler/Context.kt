/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(TypeRefinement::class, TypeRefinement::class)

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.components.*
import org.jetbrains.kotlin.resolve.calls.inference.components.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.util.slicedMap.*

@Suppress("NewApi")
class Context(val module: ModuleDescriptor, val trace: BindingTrace?) {
  fun withTrace(trace: BindingTrace?) = Context(module, trace)

  val collectionClassifier get() = module.builtIns.collection
  val anyType get() = module.builtIns.anyType
  val nullableAnyType get() = module.builtIns.nullableAnyType
  val functionType by lazy(LazyThreadSafetyMode.NONE) {
    module.findClassAcrossModuleDependencies(ClassId.topLevel(InjektFqNames.Function))!!
      .defaultType
      .replaceArgumentsWithStarProjections()
  }

  val constraintInjector by lazy(LazyThreadSafetyMode.NONE) {
    ConstraintInjector(
      ConstraintIncorporator(typeApproximator, oracle, utilContext),
      typeApproximator,
      LanguageVersionSettingsImpl.DEFAULT
    )
  }

  private val typeApproximator by lazy(LazyThreadSafetyMode.NONE) {
    TypeApproximator(module.builtIns, LanguageVersionSettingsImpl.DEFAULT)
  }
  private val oracle by lazy(LazyThreadSafetyMode.NONE) {
    TrivialConstraintTypeInferenceOracle(
      ClassicTypeSystemContextForCS(
        module.builtIns, module.getKotlinTypeRefiner()
      )
    )
  }
  private val utilContext by lazy(LazyThreadSafetyMode.NONE) {
    ClassicConstraintSystemUtilContext(module.getKotlinTypeRefiner(), module.builtIns)
  }
  val resultTypeResolver by lazy(LazyThreadSafetyMode.NONE) {
    ResultTypeResolver(
      typeApproximator,
      oracle,
      LanguageVersionSettingsImpl.DEFAULT
    )
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
