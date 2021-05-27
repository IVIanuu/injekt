package com.ivianuu.injekt.ide.quickfixes

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*

fun KtFile.addImport(fqName: FqName, context: InjektContext) {
  ImportInsertHelper.getInstance(project)
    .importDescriptor(
      this,
      context.classifierDescriptorForFqName(
        fqName,
        NoLookupLocation.FROM_BACKEND
      )!!
    )
}