package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.ast.AstCall
import com.ivianuu.injekt.compiler.ast.AstClassImpl
import com.ivianuu.injekt.compiler.ast.AstDeclaration
import com.ivianuu.injekt.compiler.ast.AstFile
import com.ivianuu.injekt.compiler.ast.AstFileImpl
import com.ivianuu.injekt.compiler.ast.AstModifier
import com.ivianuu.injekt.compiler.ast.AstTypeParameter
import com.ivianuu.injekt.compiler.ast.string.Ast2StringTranslator
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtAnnotation
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtTypeParameterList
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.resolve.BindingTrace

class Psi2AstGenerator(
    private val bindingTrace: BindingTrace
) {

    fun generateFile(file: KtFile): AstFile {
        return AstFileImpl(
            packageFqName = file.packageFqName.asString(),
            name = file.name,
            annotations = generateAnnotations(),
            declarations = generateDeclarations(file.declarations)
        )
    }

    private fun generateDeclarations(declarations: List<KtDeclaration>): MutableList<AstDeclaration> =
        mutableListOf<AstDeclaration>().apply {
            this += declarations
                .filterIsInstance<KtClassOrObject>() // todo
                .map { generateDeclaration(it) }
        }

    private fun generateDeclaration(declaration: KtDeclaration) = when (declaration) {
        is KtClassOrObject -> generateClass(declaration)
        else -> error("Unexpected declaration $declaration")
    }

    private fun generateClass(declaration: KtClassOrObject) = AstClassImpl(
        name = declaration.nameAsSafeName.asString(),
        annotations = generateAnnotations(),
        modifiers = generateModifiers(declaration.modifierList),
        typeParameters = generateTypeParameters(declaration.typeParameterList),
        declarations = generateDeclarations(declaration.declarations)
    )

    // todo
    private fun generateAnnotations() = mutableListOf<AstCall>()

    private fun generateModifiers(modifiers: KtModifierList?) =
        modifiers?.node?.children().orEmpty().mapNotNull { node ->
            when (node) {
                is KtAnnotationEntry -> null
                is KtAnnotation -> null
                is PsiWhiteSpace -> null
                else -> AstModifier.values().singleOrNull { it.name.toLowerCase() == node.text }
            }
        }.toMutableList()

    // todo
    private fun generateTypeParameters(typeParameters: KtTypeParameterList?) =
        mutableListOf<AstTypeParameter>()
}

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
