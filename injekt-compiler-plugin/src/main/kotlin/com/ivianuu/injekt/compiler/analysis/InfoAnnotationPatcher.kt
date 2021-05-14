package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.checkers.*

class InfoAnnotationPatcher(private val context: InjektContext) : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext
  ) {
    // requesting infos triggers saving them
    when (descriptor) {
      is ClassDescriptor -> {
        descriptor.classifierInfo(this.context, context.trace)
        descriptor.declaredTypeParameters
          .forEach { it.classifierInfo(this.context, context.trace) }
        descriptor.constructors
          .forEach { it.callableInfo(this.context, context.trace) }
      }
      is CallableDescriptor -> {
        descriptor.callableInfo(this.context, context.trace)
        descriptor.typeParameters
          .forEach { it.classifierInfo(this.context, context.trace) }
      }
    }
  }
}
