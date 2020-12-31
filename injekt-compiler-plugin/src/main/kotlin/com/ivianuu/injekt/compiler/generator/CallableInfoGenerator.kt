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

import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.FileManager
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.PersistedCallableInfo
import com.ivianuu.injekt.compiler.UniqueNameProvider
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.descriptor
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import java.util.Base64

class CallableInfoGenerator(
    private val bindingContext: BindingContext,
    private val declarationStore: DeclarationStore
) : Generator {
    @Suppress("NewApi")
    override fun generate(context: Generator.Context, files: List<KtFile>) {
        files.forEach { file ->
            val infos = mutableListOf<PersistedCallableInfo>()

            file.accept(object : KtTreeVisitorVoid() {
                override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression) {
                }

                override fun visitDeclaration(declaration: KtDeclaration) {
                    super.visitDeclaration(declaration)
                    if (!declaration.shouldBeIndexed()) return

                    val descriptor = declaration.descriptor<DeclarationDescriptor>(bindingContext)
                        ?: error("Wtf $declaration ${declaration.text}")

                    if (descriptor !is CallableDescriptor) return

                    val callableInfo = declarationStore.internalCallableInfoFor(
                        descriptor)
                    if (callableInfo !== null) {
                        infos += callableInfo
                    }
                }
            })

            if (infos.isEmpty()) return@forEach

            val fileName = file.packageFqName.pathSegments().joinToString("_") +
                    "_${file.name.removeSuffix(".kt")}CallableInfos.kt"
            val nameProvider = UniqueNameProvider()
            context.generateFile(
                originatingFile = file,
                packageFqName = InjektFqNames.IndexPackage,
                fileName = fileName,
                code = buildString {
                    appendLine("package ${InjektFqNames.IndexPackage}")
                    appendLine("import ${InjektFqNames.CallableInfo}")

                    infos
                        .forEach { info ->
                            val infoName = nameProvider(
                                "_${info.key.hashCode()}_callable_info").asNameId()
                            val value = Base64.getEncoder()
                                .encode(declarationStore.moshi.adapter(PersistedCallableInfo::class.java)
                                    .toJson(info).toByteArray())
                                .decodeToString()
                            appendLine("@CallableInfo(key = \"${info.key}\",\n" +
                                    "value = \"$value\"\n)")
                            appendLine("internal val $infoName = Unit")
                        }
                }
            )
        }
    }
}
