package com.ivianuu.injekt.ide.usages

import com.intellij.psi.*
import com.intellij.usages.*
import com.intellij.usages.impl.rules.*
import com.ivianuu.injekt.ide.refs.*
import org.jetbrains.kotlin.psi.*

class InjektUsageTypeProvider : UsageTypeProviderEx {
  override fun getUsageType(element: PsiElement?, targets: Array<out UsageTarget>): UsageType? {
    if (element !is KtCallExpression) return null
    val target = (targets.firstOrNull() as? PsiElementUsageTarget)?.element ?: return null
    val ref = element.references
      .firstOrNull { it.isReferenceTo(target) }
    return if (ref is InjectReference) InjectionUsageType else null
  }

  override fun getUsageType(p0: PsiElement?): UsageType? = null
}

private val InjectionUsageType = UsageType { "Injection" }
