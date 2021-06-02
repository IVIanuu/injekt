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

package com.ivianuu.injekt.ide.refs

import com.intellij.lang.findUsages.*
import com.intellij.openapi.application.*
import com.intellij.openapi.project.*
import com.intellij.openapi.util.*
import com.intellij.patterns.*
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.*
import com.intellij.psi.search.*
import com.intellij.psi.search.searches.*
import com.intellij.util.*
import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.analysis.*
import com.ivianuu.injekt.compiler.resolution.*
import com.ivianuu.injekt.ide.*
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.asJava.elements.*
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.idea.*
import org.jetbrains.kotlin.idea.caches.project.*
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.codeInsight.*
import org.jetbrains.kotlin.idea.decompiler.navigation.*
import org.jetbrains.kotlin.idea.findUsages.*
import org.jetbrains.kotlin.idea.imports.*
import org.jetbrains.kotlin.idea.references.*
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.jvm.*
import org.jetbrains.kotlin.resolve.source.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.*
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class InjektKotlinReferenceProviderContributor : KotlinReferenceProviderContributor {
  override fun registerReferenceProviders(registrar: KotlinPsiReferenceRegistrar) {
    KotlinReferenceContributor()
      .registerReferenceProviders(registrar)
    registrar.registerMultiProvider<KtExpression> { expression ->
      val context = expression.getResolutionFacade().analyze(expression)
      val graph = context[InjektWritableSlices.INJECTION_GRAPH_FOR_CALL, expression]
        ?: return@registerMultiProvider emptyArray()
      if (graph !is InjectionGraph.Success)
        return@registerMultiProvider emptyArray()
      val references = mutableListOf<PsiReference>()
      graph.forEachResultRecursive { _, value ->
        if (value !is ResolutionResult.Success.WithCandidate.Value)
          return@forEachResultRecursive
        val candidate = value.candidate
        if (candidate is CallableInjectable &&
          (candidate.callable.callable.findPackage() !is BuiltInsPackageFragment)) {
          references += InjectReference(expression, {
              candidate.callable.callable.findPsiDeclarations(expression.project, expression.resolveScope)
                .firstOrNull()
                ?.safeAs<KtDeclaration>()
                ?: candidate.callable.callable.safeAs<ReceiverParameterDescriptor>()
                  ?.containingDeclaration
                  ?.findPsiDeclarations(expression.project, expression.resolveScope)
                  ?.firstOrNull()
                  ?.safeAs<KtDeclaration>()
            },
            candidate.callable.callable.name
          )
        }
      }
      references.toTypedArray()
    }
  }
}

class InjectReference(
  expression: KtExpression,
  computeTarget: () -> KtDeclaration?,
  private val name: Name
) : PsiReferenceBase<KtExpression>(
  expression,
  TextRange.from(0, expression.text.length)
), KtReference, KtDescriptorsBasedReference {
  private val target by lazy(computeTarget)

  override fun multiResolve(p0: Boolean): Array<ResolveResult> =
    target?.let { arrayOf(PsiElementResolveResult(it, true)) } ?: emptyArray()

  override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> =
    listOfNotNull(target?.descriptor(context))

  override val resolver
    get() = KotlinDescriptorsBasedReferenceResolver

  override val resolvesByNames: Collection<Name>
    get() = listOf(name)

  override fun resolve(): PsiElement? = target

  override fun isReferenceTo(element: PsiElement): Boolean =
    super<PsiReferenceBase>.isReferenceTo(element)
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
