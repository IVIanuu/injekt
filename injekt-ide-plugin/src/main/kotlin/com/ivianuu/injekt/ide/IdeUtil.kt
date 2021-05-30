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

package com.ivianuu.injekt.ide

import com.intellij.psi.*
import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.asJava.elements.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*

fun PsiElement.ktElementOrNull() = safeAs<KtDeclaration>()
  ?: safeAs<KtLightDeclaration<*, *>>()?.kotlinOrigin

fun KtAnnotated.isProvideOrInjectDeclaration() = hasAnnotation(InjektFqNames.Provide) ||
  hasAnnotation(InjektFqNames.Inject) ||
    safeAs<KtParameter>()?.getParentOfType<KtNamedFunction>(false)
    ?.hasAnnotation(InjektFqNames.Provide) == true ||
  safeAs<KtConstructor<*>>()?.getContainingClassOrObject()
    ?.hasAnnotation(InjektFqNames.Provide) == true
