/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.ide

import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import com.intellij.psi.search.*
import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.analysis.*
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.asJava.elements.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.idea.caches.project.*
import org.jetbrains.kotlin.idea.codeInsight.*
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.facet.*
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.jvm.*
import org.jetbrains.kotlin.utils.addToStdlib.*

fun PsiElement.injektFqNames(ctx: Context) = module
  ?.getOptionValueInFacet(RootPackageOption)
  ?.let { InjektFqNames(FqName(it)) }
  ?: InjektFqNames.Default

fun ModuleDescriptor.injektFqNames(ctx: Context): InjektFqNames = moduleInfo?.unwrapModuleSourceInfo()?.module
  ?.getOptionValueInFacet(RootPackageOption)
  ?.let { InjektFqNames(FqName(it)) }
  ?: InjektFqNames.Default

fun Module.getOptionValueInFacet(option: AbstractCliOption): String? {
  val kotlinFacet = KotlinFacet.get(this) ?: return null
  val commonArgs = kotlinFacet.configuration.settings.compilerArguments ?: return null

  val prefix = "plugin:com.ivianuu.injekt:${option.optionName}="

  val optionValue = commonArgs.pluginOptions
    ?.firstOrNull { it.startsWith(prefix) }
    ?.substring(prefix.length)

  return optionValue
}

fun PsiElement.ktElementOrNull() = safeAs<KtDeclaration>()
  ?: safeAs<KtLightDeclaration<*, *>>()?.kotlinOrigin

fun KtAnnotated.isProvideOrInjectDeclaration(): Boolean = hasAnnotation(ctx.injektFqNames.provide) ||
    (this is KtParameter && hasAnnotation(ctx.injektFqNames.inject)) ||
    safeAs<KtParameter>()?.getParentOfType<KtFunction>(false)
      ?.isProvideOrInjectDeclaration() == true ||
    safeAs<KtConstructor<*>>()?.getContainingClassOrObject()
      ?.isProvideOrInjectDeclaration() == true

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

fun DeclarationDescriptor.findPsiDeclarations(project: Project, resolveScope: GlobalSearchScope): Collection<PsiElement> {
  if (this is PackageViewDescriptor)
    return listOf(
      KotlinJavaPsiFacade.getInstance(project)
        .findPackage(fqName.asString(), resolveScope)
    )

  if (this is InjectFunctionDescriptor)
    return underlyingDescriptor.findPsiDeclarations(project, resolveScope)

  if (this is ConstructorDescriptor &&
    constructedClass.kind == ClassKind.OBJECT)
    return constructedClass.findPsiDeclarations(project, resolveScope)

  if (this is ValueParameterDescriptor &&
    (containingDeclaration is DeserializedDescriptor ||
        containingDeclaration is InjectFunctionDescriptor)) {
    return listOfNotNull(
      containingDeclaration.findPsiDeclarations(project, resolveScope)
        .firstOrNull()
        .safeAs<KtFunction>()
        ?.valueParameters
        ?.get(index)
    )
  }

  if (this is LazyClassReceiverParameterDescriptor)
    return containingDeclaration.findPsiDeclarations(project, resolveScope)

  return DescriptorToSourceUtilsIde.getAllDeclarations(project, this, resolveScope)
}
