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

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.incremental.components.LocationInfo
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.record
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension

class LookupManager {
    lateinit var lookupTracker: LookupTracker
    fun recordLookup(
        sourceFilePath: String,
        lookedUp: DeclarationDescriptor
    ) {
        val location = object : LookupLocation {
            override val location: LocationInfo?
                get() = object : LocationInfo {
                    override val filePath: String
                        get() = sourceFilePath
                    override val position: Position
                        get() = Position.NO_POSITION
                }
        }

        lookupTracker.record(
            location,
            lookedUp.findPackage(),
            lookedUp.name
        )
    }

    fun recordLookup(
        source: IrElement,
        lookedUp: IrDeclarationWithName
    ) {
        val location = object : LookupLocation {
            override val location: LocationInfo?
                get() = object : LocationInfo {
                    override val filePath: String
                        get() = (source as? IrFile)?.path
                            ?: (source as IrDeclarationWithName).file.path
                    override val position: Position
                        get() = Position.NO_POSITION
                }
        }

        // println("record lookup ${location.location?.filePath} -> ${lookedUp.descriptor.fqNameSafe}")

        lookupTracker.record(
            location,
            lookedUp.getPackageFragment()!!.packageFragmentDescriptor,
            lookedUp.name
        )
    }
}

class LookupTrackerInitializer(
    private val lookupManager: LookupManager
) : AnalysisHandlerExtension {

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        lookupManager.lookupTracker = componentProvider.get()
        return null
    }

}
