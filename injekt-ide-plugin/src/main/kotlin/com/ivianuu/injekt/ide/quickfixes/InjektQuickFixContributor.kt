package com.ivianuu.injekt.ide.quickfixes

import org.jetbrains.kotlin.idea.quickfix.*

class InjektQuickFixContributor : QuickFixContributor {
  override fun registerQuickFixes(quickFixes: QuickFixes) {
    quickFixes.importInjectable()
    quickFixes.addMissingInjectableAsParameter()
  }
}
