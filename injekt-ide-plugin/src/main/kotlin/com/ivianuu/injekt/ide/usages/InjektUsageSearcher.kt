/*
 * Copyright 2021 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
