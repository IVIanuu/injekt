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

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IrProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

object InjektClassNames {
    val InjektPackage = FqName("com.ivianuu.injekt")
    val InjektInternalPackage = FqName("com.ivianuu.injekt.internal")
    val InjektComponentsPackage = FqName("com.ivianuu.injekt.internal.Components")

    val Component = FqName("com.ivianuu.injekt.Component")
    val ComponentDsl = FqName("com.ivianuu.injekt.ComponentDsl")
    val ComponentMetadata = FqName("com.ivianuu.injekt.internal.ComponentMetadata")
    val Qualifier = FqName("com.ivianuu.injekt.Qualifier")
    val Module = FqName("com.ivianuu.injekt.Module")
    val ModuleMetadata = FqName("com.ivianuu.injekt.internal.ModuleMetadata")
    val Provider = FqName("com.ivianuu.injekt.Provider")
    val ProviderDsl = FqName("com.ivianuu.injekt.ProviderDsl")
}

fun ModuleDescriptor.getTopLevelClass(fqName: FqName) =
    findClassAcrossModuleDependencies(ClassId.topLevel(fqName))!!

internal lateinit var messageCollector: MessageCollector

fun message(
    message: String,
    tag: String = "ddd",
    severity: CompilerMessageSeverity = CompilerMessageSeverity.WARNING
) {
    messageCollector.report(severity, "$tag: $message")
}

fun String.removeIllegalChars(): String {
    return replace("<", "")
        .replace(">", "")
        .replace(" ", "")
        .replace(",", "")
        .replace("*", "")
        .replace(".", "")
        .replace("-", "")
}

fun <T : IrSymbol> T.ensureBound(irProviders: List<IrProvider>): T {
    if (!this.isBound) irProviders.forEach { it.getDeclaration(this) }
    check(this.isBound) { "$this is not bound" }
    return this
}

fun List<IrConstructorCall>.hasAnnotation(fqName: FqName): Boolean =
    any { it.symbol.descriptor.constructedClass.fqNameSafe == fqName }

fun getComponentFqName(
    expression: IrExpression,
    file: IrFile
): FqName {
    return FqName(
        "${file.fqName}.Component${
        (file.name.removeSuffix(".kt") + expression.startOffset).hashCode()
            .toString()
            .removeIllegalChars()
        }"
    )
}
