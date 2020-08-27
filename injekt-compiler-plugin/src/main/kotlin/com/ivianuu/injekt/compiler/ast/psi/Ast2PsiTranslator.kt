package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.ast.string.Ast2StringTranslator
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile

object Ast2PsiTranslator {

    private val proj by lazy {
        KotlinCoreEnvironment.createForProduction(
            Disposer.newDisposable(),
            CompilerConfiguration(),
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        ).project
    }

    fun generateFile(element: AstFile): KtFile =
        PsiManager.getInstance(proj).findFile(
            LightVirtualFile(
                "tmp.kt", KotlinFileType.INSTANCE,
                Ast2StringTranslator.generate(element)
            )
        ) as KtFile

}
