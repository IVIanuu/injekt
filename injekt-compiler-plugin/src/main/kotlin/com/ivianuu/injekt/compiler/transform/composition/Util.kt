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

package com.ivianuu.injekt.compiler.transform.composition

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.analysis.TypeAnnotationChecker
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace

fun IrFunction.isReadable(bindingContext: BindingContext): Boolean {
    if (hasAnnotation(InjektFqNames.Readable)) return true
    val typeAnnotationChecker = TypeAnnotationChecker()
    val bindingTrace = DelegatingBindingTrace(bindingContext, "Injekt IR")
    return try {
        typeAnnotationChecker.hasTypeAnnotation(
            bindingTrace, descriptor,
            InjektFqNames.Readable
        )
    } catch (e: Exception) {
        false
    }
}
