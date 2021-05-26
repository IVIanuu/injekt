package com.ivianuu.injekt.ide.quickfixes

import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.nj2k.NewJavaToKotlinConverter.Companion.addImports
import org.jetbrains.kotlin.psi.*

fun KtFile.addImportIfNeeded(importFqName: FqName) {
  if (importList
      ?.imports
      ?.any {
        (it.importPath?.isAllUnder == false && it.importPath?.fqName == importFqName) ||
            (it.importPath?.isAllUnder == true &&
                it.importPath?.fqName == importFqName.parent())
      } == true) return
  addImports(listOf(importFqName))
}
