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

package com.ivianuu.injekt.ide.quickfixes

import com.ivianuu.injekt.compiler.analysis.*
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*

fun KtFile.addImport(fqName: FqName, context: AnalysisContext) {
  ImportInsertHelper.getInstance(project)
    .importDescriptor(
      this,
      context.injektContext.classifierDescriptorForFqName(
        fqName,
        NoLookupLocation.FROM_BACKEND
      )!!
    )
}