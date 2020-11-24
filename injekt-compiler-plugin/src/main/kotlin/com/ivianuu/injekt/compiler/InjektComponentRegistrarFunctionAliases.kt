package com.ivianuu.injekt.compiler
import com.google.auto.service.AutoService
import com.ivianuu.injekt.Assisted
import com.ivianuu.injekt.FunBinding
import com.ivianuu.injekt.compiler.checkers.InjektStorageContainerContributor
import com.ivianuu.injekt.compiler.generator.DeleteOldFilesExtension
import com.ivianuu.injekt.compiler.generator.InjektKtGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File
@com.ivianuu.injekt.internal.FunctionAlias
typealias registerExtensions = (Boolean) -> Unit