/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.ide

import com.intellij.psi.*
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.project.*
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.facet.*

fun ModuleDescriptor.isInjektEnabled(): Boolean = getCapability(ModuleInfo.Capability)
  ?.isInjektEnabled() ?: false

fun PsiElement.isInjektEnabled(): Boolean = try {
  getModuleInfo().isInjektEnabled()
} catch (e: Throwable) {
  false
}

fun ModuleInfo.isInjektEnabled(): Boolean {
  val module = unwrapModuleSourceInfo()?.module ?: return false
  val facet = KotlinFacet.get(module) ?: return false
  val pluginClasspath = facet.configuration.settings.compilerArguments?.pluginClasspaths ?: return false
  return pluginClasspath.any {
    it.contains("injekt-compiler-plugin")
  }
}
