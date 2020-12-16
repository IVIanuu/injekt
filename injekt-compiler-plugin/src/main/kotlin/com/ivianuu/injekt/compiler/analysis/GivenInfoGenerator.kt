package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.FileManager
import com.ivianuu.injekt.compiler.GivenInfo
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.UniqueNameProvider
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.descriptor
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

class GivenInfoGenerator(
    private val bindingContext: BindingContext,
    private val declarationStore: DeclarationStore,
    private val fileManager: FileManager,
) {
    fun generate(files: List<KtFile>) {
        return
        files.forEach { file ->
            val givenInfos = mutableListOf<Pair<FqName, GivenInfo>>()

            file.accept(object : KtTreeVisitorVoid() {
                override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression) {
                }

                override fun visitDeclaration(declaration: KtDeclaration) {
                    super.visitDeclaration(declaration)
                    if (declaration !is KtNamedFunction &&
                        declaration !is KtClassOrObject &&
                        declaration !is KtProperty &&
                        declaration !is KtConstructor<*>
                    ) return

                    if (declaration is KtClassOrObject && declaration.isLocal) return
                    if (declaration is KtProperty && declaration.isLocal) return
                    if (declaration is KtFunction && declaration.isLocal) return
                    if (declaration is KtConstructor<*> && declaration.containingClassOrObject
                            ?.isLocal == true
                    ) return

                    val descriptor = declaration.descriptor<DeclarationDescriptor>(bindingContext)
                        ?: error("Wtf $declaration ${declaration.text}")

                    val givenInfo = try {
                        declarationStore.internalGivenInfoFor(descriptor)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return
                    } ?: GivenInfo.Empty

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
                originatingFile = file.virtualFilePath,
                packageFqName = InjektFqNames.IndexPackage,
                fileName = fileName,
                code = buildString {
                    appendLine("package ${InjektFqNames.IndexPackage}")
                    appendLine("import ${InjektFqNames.GivenInfo}")

                    givenInfos
                        .forEach { (fqName, info) ->
                            val infoName = nameProvider(
                                fqName.pathSegments()
                                    .joinToString("_") + "_${info.key.hashCode()}_given_info"
                            ).asNameId()
                            appendLine("@GivenInfo(key = \"${info.key}\",\n" +
                                    "givens = [${info.givens.joinToString(", ") { "\"$it\"" }}]\n" +
                                    ")")
                            appendLine("internal val $infoName = Unit")
                        }
                }
            )
        }
    }
}