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

package com.ivianuu.injekt.ide

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.util.concurrency.AppExecutorUtil
import com.ivianuu.injekt.compiler.SrcDir
import com.ivianuu.injekt.compiler.generator.Generator
import com.ivianuu.injekt.compiler.generator.GivenFunGenerator
import com.ivianuu.injekt.compiler.generator.IndexGenerator
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.LazyEntity
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.Printer

class GeneratorManager(private val project: Project, private val srcDir: SrcDir) {

    val cacheExecutor =
        AppExecutorUtil.createBoundedApplicationPoolExecutor("Injekt worker", 1)

    private val psiManager = PsiManager.getInstance(project)

    private val generators: List<Generator> = listOf(IndexGenerator(), GivenFunGenerator())

    private val fileStates = mutableMapOf<String, FileState>()

    private val psiElementFactory = KtPsiFactory(project, false)

    private class FileState(
        val file: KtFile,
        val fileHash: Int,
        val generatedFiles: List<KtFile>,
        val descriptors: List<DeclarationDescriptor>,
    )

    fun descriptors(): List<DeclarationDescriptor> =
        fileStates.values.flatMap { it.descriptors }

    fun refresh(files: List<KtFile>) {
        return
        ReadAction.nonBlocking {
            val changedFiles = files
                .filter {
                    val state = fileStates[it.virtualFilePath]
                    state == null || state.fileHash != it.text.hashCode()
                }
            changedFiles.forEach { fileStates.remove(it.virtualFilePath) }

            println("refresh files $files: changed $changedFiles")

            val newFiles = mutableListOf<Pair<KtFile, KtFile>>()
            for (generator in generators) {
                println("Process $changedFiles with $generator")
                generator.generate(
                    object : Generator.Context {
                        override fun generateFile(
                            packageFqName: FqName,
                            fileName: String,
                            originatingFile: KtFile,
                            code: String,
                        ) {
                            try {
                                val ktFile =
                                    psiElementFactory.createAnalyzableFile("_injekt_${fileName}",
                                        code,
                                        originatingFile)
                                println("Generate file for $originatingFile -> ${ktFile}\n$code")
                                newFiles += originatingFile to ktFile
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }
                    },
                    changedFiles
                )
            }

            println("Generation finished $newFiles")

            try {
                val newDescriptors: List<Pair<KtFile, List<DeclarationDescriptor>>> =
                    if (newFiles.isEmpty()) {
                        emptyList()
                    } else {
                        val resolutionFacade = KotlinCacheService.getInstance(project)
                            .getResolutionFacade(files + newFiles.map { it.second })
                        println("resolution facade $resolutionFacade")
                        newFiles
                            .map { (file, newFile) ->
                                file to newFile.declarations
                                    .map {
                                        resolutionFacade.resolveToDescriptor(it,
                                            BodyResolveMode.FULL)
                                    }
                            }
                    }

                println("new descriptors ${
                    newDescriptors.map {
                        it.second
                    }
                }")

                newDescriptors.forEach { (file, descriptors) ->
                    fileStates[file.virtualFilePath] = FileState(
                        file,
                        file.text.hashCode(),
                        newFiles.filter { it.first == file }.map { it.second },
                        descriptors
                    )
                }

                newDescriptors
                    .flatMap { it.second }
                    .filterIsInstance<LazyEntity>()
                    .forEach { it.forceResolveAllContents() }

                println("new state $fileStates")

                for ((origin, _) in newFiles) {
                    DaemonCodeAnalyzer.getInstance(project).restart(origin)
                }
            } catch (e: Throwable) {
                if (e is ProcessCanceledException) {
                    cacheExecutor.submit {
                        refresh(files)
                    }
                } else {
                    e.printStackTrace()
                }
            }
        }
            .expireWith(project)
            .inSmartMode(project)
            .submit(cacheExecutor)
    }

    /*fun clear(file: KtFile) {
        println("Clear file $file")
        generatedFiles.remove(file)
            ?.forEach { removedFile ->
                File(removedFile.virtualFilePath).delete()
                println("Remove file $removedFile by $file")
                descriptorsByGeneratedFile.remove(removedFile)
            }
    }*/

}

class GeneratorPackageFragmentProviderExtension(
    private val manager: GeneratorManager,
) : PackageFragmentProviderExtension {
    override fun getPackageFragmentProvider(
        project: Project,
        module: ModuleDescriptor,
        storageManager: StorageManager,
        trace: BindingTrace,
        moduleInfo: ModuleInfo?,
        lookupTracker: LookupTracker,
    ): PackageFragmentProvider? {
        return packageFragmentProvider(module, manager)
    }
}


private fun packageFragmentProvider(
    module: ModuleDescriptor,
    manager: GeneratorManager,
): PackageFragmentProvider = object : PackageFragmentProvider {
    override fun getPackageFragments(fqName: FqName) = listOf(
        object : PackageFragmentDescriptorImpl(module, fqName) {
            override fun getMemberScope() = object : MemberScope {
                override fun getClassifierNames(): Set<Name> =
                    manager.descriptors().filterIsInstance<ClassifierDescriptor>().map { it.name }
                        .toSet()

                override fun getContributedClassifier(
                    name: Name,
                    location: LookupLocation,
                ): ClassifierDescriptor? =
                    manager.descriptors().filterIsInstance<ClassifierDescriptor>()
                        .firstOrNull { it.name == name }

                override fun getContributedDescriptors(
                    kindFilter: DescriptorKindFilter,
                    nameFilter: (Name) -> Boolean,
                ): Collection<DeclarationDescriptor> =
                    manager.descriptors().filter { it.name == name }

                override fun getContributedFunctions(
                    name: Name,
                    location: LookupLocation,
                ): Collection<SimpleFunctionDescriptor> =
                    manager.descriptors().filterIsInstance<SimpleFunctionDescriptor>()

                override fun getContributedVariables(
                    name: Name,
                    location: LookupLocation,
                ): Collection<PropertyDescriptor> =
                    manager.descriptors().filterIsInstance<PropertyDescriptor>()

                override fun getFunctionNames(): Set<Name> =
                    manager.descriptors().filterIsInstance<SimpleFunctionDescriptor>()
                        .map { it.name }.toSet()

                override fun getVariableNames(): Set<Name> =
                    manager.descriptors().filterIsInstance<PropertyDescriptor>().map { it.name }
                        .toSet()

                override fun printScopeStructure(p: Printer) {
                }
            }
        }
    )

    override fun getSubPackagesOf(
        fqName: FqName,
        nameFilter: (Name) -> Boolean,
    ): Collection<FqName> = manager.descriptors().map { it.findPackage().fqName }
        .filter { it.parent() == fqName }
}
