package com.ivianuu.injekt.ide.refs

import com.intellij.lang.findUsages.*
import com.intellij.openapi.application.*
import com.intellij.openapi.project.*
import com.intellij.patterns.*
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.*
import com.intellij.psi.search.*
import com.intellij.psi.search.searches.*
import com.intellij.util.*
import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.analyzer.*
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
import org.jetbrains.kotlin.resolve.source.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class InjektReferencesSearcher :
  QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>() {
  override fun processQuery(
    params: ReferencesSearch.SearchParameters,
    processor: Processor<in PsiReference>
  ) {
    params.project.runReadActionInSmartMode {
      /*val elementToSearch = params.elementToSearch
      println("check search for element $elementToSearch")
      if (elementToSearch !is KtDeclaration) return@runReadActionInSmartMode
      if (!elementToSearch.hasAnnotation(InjektFqNames.Provide) &&
        !elementToSearch.hasAnnotation(InjektFqNames.Inject) &&
        elementToSearch.parent.safeAs<KtFunction>()?.hasAnnotation(InjektFqNames.Provide) != true &&
        (elementToSearch !is KtPrimaryConstructor &&
            elementToSearch.getParentOfType<KtClassOrObject>(false)
              ?.hasAnnotation(InjektFqNames.Provide) != true))
        return@runReadActionInSmartMode
      println("perform for element $elementToSearch ${elementToSearch.name}")*/

      val psiManager = PsiManager.getInstance(params.project)

      fun search(scope: SearchScope) {
        if (scope is LocalSearchScope) {
          for (element in scope.scope) {
            element.accept(
              callExpressionRecursiveVisitor { call ->
                if (call.isValid) {
                  call.references
                    .filterIsInstance<InjectReference>()
                    .filter { it.isReferenceTo(params.elementToSearch) }
                    .forEach { processor.process(it) }
                }
              }
            )
          }
        } else if (scope is GlobalSearchScope) {
          FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope)
            .forEach { file ->
              val psiFile = psiManager.findFile(file) as? KtFile
              if (psiFile != null)
                search(LocalSearchScope(psiFile))
            }
        }
      }

      search(params.effectiveSearchScope)
    }
  }
}

class InjektKotlinReferenceProviderContributor : KotlinReferenceProviderContributor {
  override fun registerReferenceProviders(registrar: KotlinPsiReferenceRegistrar) {
    KotlinReferenceContributor()
      .registerReferenceProviders(registrar)

    registrar.registerMultiProvider<KtCallExpression> { call ->
      val context = call.getResolutionFacade().analyze(call)
      val graph = context[InjektWritableSlices.INJECTION_GRAPH_FOR_CALL, call]
        ?: return@registerMultiProvider emptyArray()
      if (graph !is InjectionGraph.Success)
        return@registerMultiProvider emptyArray()
      val references = mutableListOf<PsiReference>()
      graph.forEachResultRecursive { _, value ->
        val candidate = value.candidate
        if (candidate is CallableInjectable &&
          (candidate.callable.callable.findPackage() !is BuiltInsPackageFragment)) {
          references += InjectReference(
            call,
            candidate.callable.callable.findPsiDeclarations(call.project, call.resolveScope)
              .firstOrNull()
              ?.safeAs<KtDeclaration>()
              ?: candidate.callable.callable.safeAs<ReceiverParameterDescriptor>()
                ?.containingDeclaration
                ?.findPsiDeclarations(call.project, call.resolveScope)
                ?.firstOrNull()
                ?.safeAs<KtDeclaration>()
              ?: error("Wtf ${candidate.callable.callable}"),
            candidate.callable.callable.name
          )
        }
      }
      references.toTypedArray()
    }
  }
}

class InjectReference(
  expression: KtCallExpression,
  private val target: KtDeclaration,
  private val name: Name
) : PsiReferenceBase<KtCallExpression>(expression, expression.textRange), KtReference, KtDescriptorsBasedReference {
  override fun multiResolve(p0: Boolean): Array<ResolveResult> =
    arrayOf(PsiElementResolveResult(target, true))

  override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> =
    listOfNotNull(target.descriptor(context))

  override val resolver
    get() = KotlinDescriptorsBasedReferenceResolver

  override val resolvesByNames: Collection<Name>
    get() = listOf(name)

  override fun resolve(): PsiElement = target

  override fun isReferenceTo(element: PsiElement): Boolean =
    super<PsiReferenceBase>.isReferenceTo(element)
}

private fun DeclarationDescriptor.findPsiDeclarations(project: Project, resolveScope: GlobalSearchScope): Collection<PsiElement> {
  val fqName = fqNameSafe

  fun Collection<KtNamedDeclaration>.fqNameFilter() = filter { it.fqName == fqName }
  return when (this) {
    is DeserializedClassDescriptor ->
      KotlinFullClassNameIndex.getInstance()[fqName.asString(), project, resolveScope]
    is DeserializedClassConstructorDescriptor ->
      KotlinFullClassNameIndex.getInstance()[fqName.parent().asString(), project, resolveScope]
        .singleOrNull()
        ?.let { declaration ->
          if (isPrimary) listOf(declaration.primaryConstructor!!)
          else listOf(declaration.secondaryConstructors[
              constructedClass.constructors
                .filterNot { it.isPrimary }
                .indexOf(this)
          ])
        } ?: error("nope")
    is DeserializedTypeAliasDescriptor ->
      KotlinTypeAliasShortNameIndex.getInstance()[fqName.shortName()
        .asString(), project, resolveScope].fqNameFilter()
    is DeserializedSimpleFunctionDescriptor, is FunctionImportedFromObject ->
      KotlinFunctionShortNameIndex.getInstance()[fqName.shortName()
        .asString(), project, resolveScope]
        .let {
          it.fqNameFilter()
            .takeIf { it.isNotEmpty() }
            ?: error("nope ")
        }
    is DeserializedPropertyDescriptor, is PropertyImportedFromObject ->
      KotlinPropertyShortNameIndex.getInstance()[fqName.shortName()
        .asString(), project, resolveScope].fqNameFilter()
    is DeclarationDescriptorWithSource -> listOfNotNull(source.getPsi())
    else -> emptyList()
  }
}
