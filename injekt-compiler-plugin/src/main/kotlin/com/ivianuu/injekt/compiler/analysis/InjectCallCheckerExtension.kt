/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.com.intellij.openapi.project.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.extensions.*

class InjectCallCheckerExtension(private val withDeclarationGenerator: Boolean) : AnalysisHandlerExtension {

}
