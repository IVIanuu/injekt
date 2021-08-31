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

package com.ivianuu.injekt.ide

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.analysis.InjectFunctionDescriptor
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.asJava.elements.KtLightDeclaration
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeserializedDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.unwrapModuleSourceInfo
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun PsiElement.ktElementOrNull() = safeAs<KtDeclaration>()
  ?: safeAs<KtLightDeclaration<*, *>>()?.kotlinOrigin

fun KtAnnotated.isProvideOrInjectDeclaration(): Boolean = hasAnnotation(InjektFqNames.Provide) ||
    (this is KtParameter && hasAnnotation(InjektFqNames.Inject)) ||
    safeAs<KtParameter>()?.getParentOfType<KtFunction>(false)
      ?.isProvideOrInjectDeclaration() == true ||
  safeAs<KtConstructor<*>>()?.getContainingClassOrObject()
    ?.isProvideOrInjectDeclaration() == true

fun ModuleDescriptor.isInjektEnabled(): Boolean = getCapability(ModuleInfo.Capability)
  ?.isInjektEnabled() ?: false

fun PsiElement.isInjektEnabled(): Boolean = getModuleInfo().isInjektEnabled()

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
