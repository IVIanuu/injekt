package com.ivianuu.injekt.integrationtests.ast

import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.ast.AstClass
import com.ivianuu.injekt.compiler.ast.AstClassId
import com.ivianuu.injekt.compiler.ast.AstElement
import com.ivianuu.injekt.compiler.ast.AstFile
import com.ivianuu.injekt.compiler.ast.AstModuleFragment
import com.ivianuu.injekt.compiler.ast.AstTransformResult
import com.ivianuu.injekt.compiler.ast.AstTransformer
import com.ivianuu.injekt.compiler.ast.addChild
import com.ivianuu.injekt.compiler.ast.extension.AstGenerationExtension
import com.ivianuu.injekt.compiler.ast.extension.AstPluginContext
import com.ivianuu.injekt.compiler.ast.transformSingle
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.source
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.ExtensionPoint
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.Test

class AstTest {

    @Test
    fun testSimple() {
        codegen(
            sources = arrayOf(
                source(
                    """
                        class NotTransformed
                        """,
                    injektImports = false,
                    initializeInjekt = false
                )
            ),
            config = {
                compilerPlugins += object : ComponentRegistrar {
                    override fun registerProjectComponents(
                        project: MockProject,
                        configuration: CompilerConfiguration
                    ) {
                        project.extensionArea.registerExtensionPoint(
                            AstGenerationExtension.extensionPointName.name,
                            AstGenerationExtension::class.java.name,
                            ExtensionPoint.Kind.INTERFACE
                        )
                        AstGenerationExtension.registerExtension(
                            project,
                            object : AstGenerationExtension {
                                override fun generate(
                                    moduleFragment: AstModuleFragment,
                                    pluginContext: AstPluginContext
                                ) {
                                    moduleFragment.files.toList().forEach { currentFile ->
                                        currentFile.transformSingle(
                                            object : AstTransformer {
                                                override fun transform(element: AstElement): AstTransformResult<AstElement> {
                                                    if (element !is AstClass) return super.transform(
                                                        element
                                                    )
                                                    moduleFragment.files += AstFile(
                                                        packageFqName = currentFile.packageFqName,
                                                        name = (element.classId.className.asString() + "Context.kt").asNameId()
                                                    ).apply {
                                                        addChild(
                                                            AstClass(
                                                                classId = AstClassId(
                                                                    packageName = element.classId.packageName,
                                                                    className = (element.classId.className.asString() + "Context").asNameId()
                                                                )
                                                            )
                                                        )
                                                    }
                                                    return super.transform(element)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        )
    }

}
