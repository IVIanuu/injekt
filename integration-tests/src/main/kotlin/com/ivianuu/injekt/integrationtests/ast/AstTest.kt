package com.ivianuu.injekt.integrationtests.ast

import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.ast.extension.AstGenerationExtension
import com.ivianuu.injekt.compiler.ast.extension.AstPluginContext
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstModuleFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.addChild
import com.ivianuu.injekt.compiler.ast.tree.expression.AstStatement
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformResult
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformerVoid
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
                        class NotTransformed(val param: String) {
                            init {
                                param
                            }
                        }
                        
                        class Other<T> {
                            init {
                                println("hello")
                                prop
                                AObject
                                NotTransformed("lol")
                            }
                        }
                        
                        object AObject
                        
                        abstract class SuperClass {
                            abstract fun abstractFun(): String
                        }
                        
                        class ImplementingClass : SuperClass() {
                            override fun abstractFun(): String = ""
                        }
                        
                        fun aFunction() {
                        }
                        
                        fun bFunction(param: String) {
                            param
                        }
                        
                        fun <T, S> aFunction(a: T, b: S) {
                        }
                        
                        fun higherOrder(block: (Int, Long) -> String) {
                            returningString(0f, 0)
                        }
                        
                        fun returningString(a: Float, b: Int, c: Long = 0L) = 0
                        
                        typealias MyTypeAlias = () -> String
                        
                        typealias MyTypeAlias2<T> = () -> String
                        
                        val prop = "hello world"
                        //val prop2 by lazy { "lol" }
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
                                    moduleFragment.files.toList().forEach { file ->
                                        file.transformChildren(
                                            object : AstTransformerVoid {
                                                override fun visitClass(klass: AstClass): AstTransformResult<AstStatement> {
                                                    moduleFragment.files += AstFile(
                                                        packageFqName = file.packageFqName,
                                                        name = (klass.name.asString() + "Context.kt").asNameId()
                                                    ).apply {
                                                        addChild(
                                                            AstClass(
                                                                name = (klass.name.asString() + "Context").asNameId()
                                                            )
                                                        )
                                                    }
                                                    return super.visitClass(klass)
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
