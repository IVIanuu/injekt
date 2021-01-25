/*
 * Copyright 2020 Manuel Wrage
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

package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun KtDeclaration.shouldBeIndexed(): Boolean {
    if (this !is KtNamedFunction &&
        this !is KtClassOrObject &&
        this !is KtProperty &&
        this !is KtConstructor<*>
    ) return false

    if (this is KtClassOrObject && isLocal) return false
    if (this is KtProperty && isLocal) return false
    if (this is KtFunction && isLocal) return false

    val owner = when (this) {
        is KtConstructor<*> -> getContainingClassOrObject()
        is KtPropertyAccessor -> property
        else -> this
    } as KtNamedDeclaration

    if ((owner is KtNamedFunction ||
                owner is KtProperty) &&
        owner.parent.safeAs<KtClassBody>()?.parent is KtClass
    ) return false

    if ((owner is KtProperty || owner is KtNamedFunction) &&
        getParentOfType<KtClassOrObject>(false)?.let {
            it is KtClass || it.hasAnnotation(InjektFqNames.Module)
        } == true)
        return false

    return hasAnnotation(InjektFqNames.Given) ||
            hasAnnotation(InjektFqNames.GivenSetElement) ||
            hasAnnotation(InjektFqNames.Module) ||
            hasAnnotation(InjektFqNames.Interceptor) ||
            hasAnnotation(InjektFqNames.GivenFun)
}