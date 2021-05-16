package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.checkers.*

class TypeAliasGivensChecker : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext
  ) {
    if (descriptor !is ClassDescriptor) return

    if (!descriptor.name.asString().endsWith("Givens"))
      return

    val correspondingTypeAlias = descriptor.findPackage()
      .getMemberScope()
      .getContributedClassifier(
        descriptor.name.asString().removeSuffix("Givens").asNameId(),
        KotlinLookupLocation(declaration)
      ) ?: return

    if (descriptor.kind != ClassKind.OBJECT) {
      context.trace.report(
        InjektErrors.TYPE_ALIAS_GIVENS_NOT_OBJECT
          .on(declaration)
      )
    }
  }
}
