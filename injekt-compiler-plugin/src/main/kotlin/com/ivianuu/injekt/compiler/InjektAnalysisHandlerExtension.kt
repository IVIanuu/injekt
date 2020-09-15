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

import com.ivianuu.injekt.compiler.analysis.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.analysis.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.analysis.hasAnnotation
import com.ivianuu.injekt.compiler.transform.asNameId
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.namedDeclarationRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.getAbbreviation
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import java.io.File

class InjektAnalysisHandlerExtension(
    private val srcDir: File,
    private val resourcesDir: File,
    private val cacheDir: File
) : AnalysisHandlerExtension {

    private val serviceLoaderCache = KeyValueFileCache(
        cacheFile = cacheDir.resolve("sl-cache"),
        fromString = { it },
        toString = { it },
        onDelete = { moduleRegistrarManager.removeImpl(FqName(it)) }
    )
    private val moduleRegistrarManager = ModuleRegistrarManager(
        resourcesDir.resolve("META-INF/services/com.ivianuu.injekt.Module\$Registrar")
    )
    private val fileCache = KeyValueFileCache(
        cacheFile = cacheDir.resolve("file-cache"),
        fromString = { File(it) },
        toString = { it.absolutePath },
        onDelete = ::fileCacheOnDelete
    )

    private fun fileCacheOnDelete(file: File) {
        serviceLoaderCache.deleteDependents(file.absolutePath)
        fileCache.deleteDependents(file)
        file.delete()
    }

    private var generatedCode = false

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        if (!generatedCode) {
            files as ArrayList<KtFile>
            files.removeAll { it.text.contains("// injekt-generated") }
            files.forEach { fileCache.deleteDependents(File(it.virtualFilePath)) }
        }
        return null
    }

    override fun analysisCompleted(
        project: Project,
        module: ModuleDescriptor,
        bindingTrace: BindingTrace,
        files: Collection<KtFile>
    ): AnalysisResult? {
        if (generatedCode) {
            fileCache.flush()
            moduleRegistrarManager.flush()
            serviceLoaderCache.flush()
            return null
        }
        generatedCode = true

        files.forEach { file ->
            file.accept(
                namedDeclarationRecursiveVisitor { namedDeclaration ->
                    val descriptor =
                        bindingTrace.bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, namedDeclaration]
                            ?: return@namedDeclarationRecursiveVisitor

                    if (descriptor.hasAnnotation(InjektFqNames.Module)) {
                        generateRegistrarForModule(
                            File((descriptor.findPsi()!!.containingFile as KtFile).virtualFilePath),
                            descriptor.findPackage().fqName,
                            descriptor.fqNameSafe,
                            (descriptor as FunctionDescriptor).dispatchReceiverParameter
                                ?.type?.constructor?.declarationDescriptor?.fqNameSafe,
                            descriptor.annotations.findAnnotation(InjektFqNames.Module)!!
                                .getTargetContextOrAny(descriptor.module),
                            descriptor
                        )
                        return@namedDeclarationRecursiveVisitor
                    }

                    if (!descriptor.hasAnnotation(InjektFqNames.Given) &&
                        !descriptor.hasAnnotatedAnnotations(
                            InjektFqNames.Adapter,
                            descriptor.module
                        )
                    ) return@namedDeclarationRecursiveVisitor
                    generateModuleForGiven(descriptor)
                }
            )
        }

        return AnalysisResult.RetryWithAdditionalRoots(
            bindingTrace.bindingContext, module, emptyList(), listOf(srcDir), true
        )
    }

    private fun generateModuleForGiven(descriptor: DeclarationDescriptor) {
        val targetDescriptor = if (descriptor is ConstructorDescriptor)
            descriptor.constructedClass else descriptor
        val packageName = targetDescriptor.findPackage().fqName
        val moduleName = getJoinedName(
            packageName,
            targetDescriptor.fqNameSafe.child("Module".asNameId())
        )
        val targetContext = if (descriptor.hasAnnotation(InjektFqNames.Given)) {
            descriptor.annotations.findAnnotation(InjektFqNames.Given)!!
                .getTargetContextOrAny(descriptor.module)
        } else {
            descriptor.getAnnotatedAnnotations(InjektFqNames.Adapter, descriptor.module)
                .single()
                .getTargetContextOrAny(descriptor.module)
        }

        val moduleCode = buildCodeString {
            emitLine("// injekt-generated")
            emitLine("package $packageName")
            emitLine()
            emitLine("import com.ivianuu.injekt.Module")
            emitLine("import com.ivianuu.injekt.ModuleRegistry")
            emitLine("import com.ivianuu.injekt.ContextBuilder")
            emitLine("import com.ivianuu.injekt.scoped")
            emitLine("import com.ivianuu.injekt.Key")
            emitLine("import com.ivianuu.injekt.keyOf")
            emitLine()
            emitLine("@Module($targetContext::class)")
            emit("fun ContextBuilder.$moduleName() ")

            val functionDescriptor = if (targetDescriptor is ClassDescriptor)
                targetDescriptor.constructors
                    .singleOrNull { it.hasAnnotation(InjektFqNames.Given) }
                    ?: targetDescriptor.unsubstitutedPrimaryConstructor
                    ?: return@buildCodeString
            else targetDescriptor as FunctionDescriptor
            val functionName = if (functionDescriptor is ConstructorDescriptor)
                functionDescriptor.constructedClass.fqNameSafe.asString()
            else functionDescriptor.name.asString()
            val dispatchReceiver =
                functionDescriptor.dispatchReceiverParameter?.returnType

            fun emitProvider() {
                braced {
                    if (targetDescriptor.hasAnnotation(InjektFqNames.Given) ||
                        descriptor !is FunctionDescriptor
                    ) {
                        val assistedParameters = functionDescriptor.valueParameters
                        if (assistedParameters.isEmpty()) {
                            emit(
                                "${
                                    dispatchReceiver?.render()?.let { "$it." }.orEmpty()
                                }$functionName()"
                            )
                        } else {
                            emit("{ ")
                            assistedParameters.forEachIndexed { index, param ->
                                emit("p$index: ${param.returnType!!.render()}")
                                if (index != assistedParameters.lastIndex) {
                                    emit(", ")
                                } else {
                                    emitLine(" ->")
                                }
                            }
                            indented {
                                emit(
                                    "${
                                        dispatchReceiver?.render()?.let { "$it." }
                                            .orEmpty()
                                    }$functionName("
                                )
                                assistedParameters.forEachIndexed { index, param ->
                                    emit("p$index")
                                    if (index != assistedParameters.lastIndex) emit(", ")
                                }
                                emitLine(")")
                            }
                            emit("}")
                        }
                    } else {
                        val assistedParameters = functionDescriptor.valueParameters
                        emit("{ ")
                        assistedParameters.forEachIndexed { index, param ->
                            emit("p$index: ${param.returnType!!.render()}")
                            if (index != assistedParameters.lastIndex) {
                                emit(", ")
                            } else {
                                emitLine(" ->")
                            }
                        }
                        indented {
                            emit(
                                "${
                                    dispatchReceiver?.render()?.let { "$it." }.orEmpty()
                                }$functionName("
                            )
                            assistedParameters.forEachIndexed { index, param ->
                                emit("p$index")
                                if (index != assistedParameters.lastIndex) emit(", ")
                            }
                            emitLine(")")
                        }
                        emit("}")
                    }
                }
            }
            braced {
                if (descriptor.hasAnnotation(InjektFqNames.Given)) {
                    if (targetContext != InjektFqNames.AnyContext)
                        emit("scoped ")
                    else emit("unscoped ")
                    emitProvider()
                } else {
                    val adapterImpl = descriptor.getAnnotatedAnnotations(
                        InjektFqNames.Adapter, descriptor.module
                    )
                        .single()
                        .annotationClass!!
                        .companionObjectDescriptor!!
                    emit("with(${adapterImpl.fqNameSafe}) ")
                    val assistedParameters = functionDescriptor.valueParameters
                    val keyType = if (assistedParameters.isNotEmpty() ||
                        descriptor is FunctionDescriptor &&
                        !descriptor.hasAnnotation(InjektFqNames.Given)
                    ) {
                        if (functionDescriptor.isSuspend) {
                            descriptor.builtIns.getSuspendFunction(assistedParameters.size)
                        } else {
                            descriptor.builtIns.getFunction(assistedParameters.size)
                        }
                            .defaultType
                            .replace(
                                newArguments = assistedParameters
                                    .map { it.returnType!!.asTypeProjection() } +
                                        functionDescriptor.returnType!!.asTypeProjection()
                            )
                    } else functionDescriptor.returnType!!
                    val key = if (descriptor is FunctionDescriptor &&
                        !descriptor.hasAnnotation(InjektFqNames.Given)
                    ) {
                        // todo
                        "Key<${keyType.render()}>(\"f_${descriptor.fqNameSafe}\")"
                    } else {
                        "keyOf<${keyType.render()}>()"
                    }
                    braced {
                        emit("configure($key) ")
                        emitProvider()
                    }
                }
            }
        }

        val moduleFile = srcDir.resolve(packageName.asString().replace(".", "/"))
            .also { it.mkdirs() }
            .resolve("$moduleName.kt")
            .also { it.createNewFile() }
        moduleFile.writeText(moduleCode)
        fileCache.recordDependency(
            moduleFile,
            File(
                (descriptor.findPsi()!!.containingFile as KtFile)
                    .virtualFilePath
            )
        )
        recordLookup(moduleFile.absolutePath, descriptor)

        generateRegistrarForModule(
            moduleFile,
            packageName,
            packageName.child(moduleName),
            null,
            targetContext,
            targetDescriptor
        )
    }

    private fun generateRegistrarForModule(
        moduleFile: File,
        modulePackageName: FqName,
        moduleFqName: FqName,
        moduleDispatchReceiver: FqName?,
        targetContext: FqName,
        originatingDescriptor: DeclarationDescriptor
    ) {
        val moduleRegistrarName = getJoinedName(
            modulePackageName,
            moduleFqName.child("Registrar".asNameId())
        )
        val moduleRegistrarFqName = modulePackageName
            .child(moduleRegistrarName)
        val registrarCode = buildCodeString {
            emitLine("// injekt-generated")
            emitLine("package $modulePackageName")
            emitLine()
            emitLine("import com.ivianuu.injekt.Module")
            emitLine("import com.ivianuu.injekt.ModuleRegistry")
            emitLine("import com.ivianuu.injekt.ContextBuilder")
            emitLine("import com.ivianuu.injekt.scoped")
            emitLine("import com.ivianuu.injekt.Key")
            emitLine("import com.ivianuu.injekt.keyOf")

            emit("class $moduleRegistrarName : Module.Registrar ")
            braced {
                emit("override fun register() ")
                braced {
                    emit("ModuleRegistry.module(keyOf<$targetContext>()")
                    if (moduleDispatchReceiver != null) {
                        emit(") ")
                        braced {
                            emit("with($moduleDispatchReceiver) ")
                            braced {
                                emit("${moduleFqName.shortName()}()")
                            }
                        }
                    } else {
                        emitLine(", ContextBuilder::${moduleFqName.shortName()})")
                    }
                }
            }
        }

        val registrarFile = srcDir.resolve(modulePackageName.asString().replace(".", "/"))
            .also { it.mkdirs() }
            .resolve("$moduleRegistrarName.kt")
            .also { it.createNewFile() }
        registrarFile.writeText(registrarCode)
        fileCache.recordDependency(registrarFile, moduleFile)
        serviceLoaderCache.recordDependency(
            moduleRegistrarFqName.asString(),
            registrarFile.absolutePath
        )
        recordLookup(registrarFile.absolutePath, originatingDescriptor)
        moduleRegistrarManager.addImpl(moduleRegistrarFqName)
    }

    private fun KotlinType.render() = buildString {
        fun KotlinType.renderInner() {
            if (hasAnnotation(InjektFqNames.Composable)) {
                append("@${InjektFqNames.Composable} ")
            }
            if (hasAnnotation(InjektFqNames.Reader)) {
                append("@${InjektFqNames.Reader} ")
            }
            val abbreviation = getAbbreviation()
            if (abbreviation != null) {
                append(abbreviation.constructor.declarationDescriptor!!.fqNameSafe)
            } else {
                append(constructor.declarationDescriptor!!.fqNameSafe)
            }
            val arguments = abbreviation?.arguments ?: arguments
            if (arguments.isNotEmpty()) {
                append("<")
                arguments.forEachIndexed { index, argument ->
                    if (argument.isStarProjection) append("*")
                    else argument.type.renderInner()
                    if (index != arguments.lastIndex) append(", ")
                }
                append(">")
            }

            if (isMarkedNullable) append("?")
        }
        renderInner()
    }

    private fun AnnotationDescriptor.getTargetContextOrAny(module: ModuleDescriptor): FqName {
        return allValueArguments.values.singleOrNull()
            ?.let { it as KClassValue }
            ?.getArgumentType(module)
            ?.constructor
            ?.declarationDescriptor
            ?.fqNameSafe
            ?: InjektFqNames.AnyContext
    }


    private fun getJoinedName(
        packageFqName: FqName,
        fqName: FqName
    ): Name {
        val joinedSegments = fqName.asString()
            .removePrefix(packageFqName.asString() + ".")
            .split(".")
        return joinedSegments.joinToString("_").asNameId()
    }

}
