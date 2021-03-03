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

package com.ivianuu.injekt.compiler.index

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.isTopLevelKtOrJavaMember
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import kotlin.math.absoluteValue

fun KtElement.collectIndices(): List<Index> {
    val indices = mutableListOf<Index>()
    accept(object : KtTreeVisitorVoid() {
        override fun visitDeclaration(declaration: KtDeclaration) {
            super.visitDeclaration(declaration)
            if (!declaration.shouldBeIndexed()) return

            val owner = when (declaration) {
                is KtConstructor<*> -> declaration.getContainingClassOrObject()
                is KtPropertyAccessor -> declaration.property
                else -> declaration
            } as KtNamedDeclaration

            val index = Index(
                owner.fqName!!,
                when (owner) {
                    is KtClassOrObject -> "class"
                    is KtConstructor<*> -> "constructor"
                    is KtFunction -> "function"
                    is KtProperty -> "property"
                    else -> error("Unexpected declaration ${declaration.text}")
                }
            )
            indices += index
        }
    })
    return indices
}

private fun KtDeclaration.shouldBeIndexed(): Boolean {
    if (this !is KtNamedFunction &&
        this !is KtClassOrObject &&
        this !is KtProperty &&
        this !is KtConstructor<*>
    ) return false

    if (this is KtClassOrObject && isLocal) return false
    if (this is KtProperty && !isTopLevel) return false
    if (this !is KtConstructor<*> &&
        this is KtFunction && !isTopLevelKtOrJavaMember()) return false

    return hasAnnotation(InjektFqNames.Given) ||
            hasAnnotation(InjektFqNames.GivenSetElement) ||
            hasAnnotation(InjektFqNames.Module)
}
