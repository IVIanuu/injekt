/*
 * Copyright 2019 Manuel Wrage
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

import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import java.io.File

class BindingFinder(
    private val outputDir: File
) : StorageComponentContainerContributor, DeclarationChecker {

    private val descriptors = mutableListOf<BindingDescriptor>()

    private var generated = false

    init {
        generateNotifier = { generate() }
    }

    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        msg { "register components" }
        container.useInstance(this)
    }

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        msg { "check $descriptor" }
        try {
            createBindingDescriptor(declaration, descriptor, context.trace)?.let {
                descriptors += it
            }
        } catch (e: Exception) {
            msg { e.stackTrace.joinToString("\n") }
        }
    }

    private fun generate() {
        if (generated) return
        generated = true
        msg { "generate $descriptors" }
        descriptors
            .map { BindingGenerator(it) }
            .map { it.generate() }
            .forEach { it.writeTo(outputDir) }
    }
}