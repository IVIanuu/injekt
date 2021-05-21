package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.checkers.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class TypeAliasModuleChecker(private val context: InjektContext) : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext
  ) {
    if (descriptor !is ClassDescriptor) return

    if (!descriptor.name.asString().endsWith("Module"))
      return

    val lookupLocation = declaration.lookupLocation
    val correspondingTypeAlias = this.context.memberScopeForFqName(
      descriptor.findPackage().fqName,
      lookupLocation
    )?.getContributedClassifier(
      descriptor.name.asString().removeSuffix("Module").asNameId(),
      lookupLocation
    )?.safeAs<TypeAliasDescriptor>() ?: return

    if (descriptor.kind != ClassKind.OBJECT) {
      context.trace.report(
        InjektErrors.TYPE_ALIAS_MODULE_NOT_OBJECT
          .on(declaration)
      )
    }

    val injectablesModule = descriptor.module
    val typeAliasModule = correspondingTypeAlias.module

    if (injectablesModule != typeAliasModule) {
      context.trace.report(
        InjektErrors.TYPE_ALIAS_MODULE_NOT_DECLARED_IN_SAME_COMPILATION_UNIT
          .on(declaration)
      )
    }
  }
}
