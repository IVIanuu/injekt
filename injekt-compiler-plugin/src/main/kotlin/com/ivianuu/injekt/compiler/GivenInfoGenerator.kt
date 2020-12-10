package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class GivenInfoGenerator(
    private val bindingContext: BindingContext,
    private val declarationStore: DeclarationStore,
    private val fileManager: FileManager,
) {
    fun generate(files: List<KtFile>) {
        files.forEach { file ->
            val givenInfos = mutableListOf<Pair<FqName, GivenInfo>>()

            file.accept(object : KtTreeVisitorVoid() {
                override fun visitDeclaration(declaration: KtDeclaration) {
                    super.visitDeclaration(declaration)
                    if (declaration !is KtNamedFunction &&
                        declaration !is KtClassOrObject &&
                        declaration !is KtProperty &&
                        declaration !is KtConstructor<*>
                    ) return

                    if (declaration is KtProperty && declaration.isLocal) return
                    if (declaration.visibilityModifierType() == KtTokens.PRIVATE_KEYWORD) return

                    val descriptor = declaration.descriptor<DeclarationDescriptor>(bindingContext)!!

                    val givenInfo = declarationStore.givenInfoFor(descriptor)

                    if (givenInfo !== GivenInfo.Empty) {
                        givenInfos += descriptor.fqNameSafe to givenInfo
                    }
                }
            })

            if (givenInfos.isEmpty()) return@forEach

            val fileName = file.packageFqName.pathSegments().joinToString("_") +
                    "_${file.name.removeSuffix(".kt")}GivenInfos.kt"
            val nameProvider = UniqueNameProvider()
            fileManager.generateFile(
                originatingFile = file,
                packageFqName = InjektFqNames.IndexPackage,
                fileName = fileName,
                code = buildString {
                    appendLine("package ${InjektFqNames.IndexPackage}")
                    appendLine("import ${InjektFqNames.GivenInfo}")

                    givenInfos
                        .forEach { (fqName, info) ->
                            val infoName = nameProvider(
                                fqName.pathSegments().joinToString("_") + "_given_info"
                            ).asNameId()
                            appendLine("@GivenInfo(key = \"${info.key}\",\n" +
                                    "requiredGivens = [${info.requiredGivens.joinToString(", ") { "\"$it\"" }}],\n" +
                                    "givensWithDefault = [${
                                        info.givensWithDefault.joinToString(", ") { "\"$it\"" }
                                    }]\n" +
                                    ")")
                            appendLine("internal val $infoName = Unit")
                        }
                }
            )
        }
    }
}