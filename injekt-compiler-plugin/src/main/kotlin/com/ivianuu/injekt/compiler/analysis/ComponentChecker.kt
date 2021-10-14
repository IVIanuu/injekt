package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence

class ComponentChecker : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext
  ) {
    if (descriptor !is ClassDescriptor) return
    if (!descriptor.hasAnnotation(InjektFqNames.Component)) return

    if (descriptor.kind != ClassKind.INTERFACE)
      context.trace.report(InjektErrors.COMPONENT_WITHOUT_INTERFACE.on(declaration))

    if (!descriptor.hasAnnotation(InjektFqNames.Provide))
      context.trace.report(InjektErrors.COMPONENT_WITHOUT_PROVIDE.on(declaration))

    descriptor.unsubstitutedMemberScope
      .getContributedDescriptors()
      .filterIsInstance<CallableMemberDescriptor>()
      .filter { it.modality == Modality.ABSTRACT }
      .filter { it.dispatchReceiverParameter?.type != descriptor.module.builtIns.anyType }
      .forEach {
        if (it is PropertyDescriptor && it.isVar) {
          context.trace.report(
            InjektErrors.COMPONENT_MEMBER_VAR
              .on(
                if (it.overriddenTreeUniqueAsSequence(false).count() > 1) declaration
                else it.findPsi() ?: declaration
              )
          )
        }
      }
  }
}
