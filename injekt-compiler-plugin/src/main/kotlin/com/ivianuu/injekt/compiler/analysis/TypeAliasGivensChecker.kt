package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.checkers.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*

class TypeAliasGivensChecker(
  private val context: InjektContext
) : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext
  ) {
    if (descriptor !is ClassDescriptor) return

    if (!descriptor.name.asString().endsWith("Givens"))
      return

    val lookupLocation = declaration.lookupLocation
    val correspondingTypeAlias = this.context.memberScopeForFqName(
      descriptor.findPackage().fqName,
      lookupLocation
    )?.getContributedClassifier(
      descriptor.name.asString().removeSuffix("Givens").asNameId(),
      lookupLocation
    ) ?: return

    if (descriptor.kind != ClassKind.OBJECT) {
      context.trace.report(
        InjektErrors.TYPE_ALIAS_GIVENS_NOT_OBJECT
          .on(declaration)
      )
    }

    val givensModule = descriptor.module
    val typeAliasModule = correspondingTypeAlias.module

    if (givensModule != typeAliasModule) {
      context.trace.report(
        InjektErrors.TYPE_ALIAS_GIVENS_NOT_DECLARED_IN_SAME_MODULE
          .on(declaration)
      )
    }
  }
}
