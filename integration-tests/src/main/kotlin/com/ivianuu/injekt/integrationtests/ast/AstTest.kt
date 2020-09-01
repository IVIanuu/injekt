package com.ivianuu.injekt.integrationtests.ast

import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.ast.extension.AstGenerationExtension
import com.ivianuu.injekt.compiler.ast.extension.AstPluginContext
import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstModuleFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.addChild
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformResult
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformerVoid
import com.ivianuu.injekt.compiler.removeIllegalChars
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.source
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.ExtensionPoint
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.Test

class AstTest {

    val DOLLAR_SIGN = "$"

    @Test
    fun testSimple() {
        codegen(
            sources = arrayOf(
                source(
                    """ 
                        class NotTransformed(val param: String) {
                            var lol: String = "hello world"
                            
                            /*var lol2: String 
                                get() = "hello world"
                                set(value) {  }*/
                        
                            init {
                                //param
                                //member()
                                //this.member()
                                //param.plus("lol")
                                //com.ivianuu.injekt.given<String>()
                            }
                            
                            fun member() {
                                var lol = ""
                                val (a, _, c) = Triple("a", "b", "c")
                            }
                            
                            fun anonymous() {
                                java.lang.System.out.println(object {
                                })
                            }
                            
                            fun localFun() {
                                fun local() {
                                    
                                }
                            }
                            
                            fun localClass() {
                                class LocalClass {
                                    
                                }
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
                        
                        interface AInterface
                        
                        //enum class AEnum(val value: String) {
                        //    A("a"), B("b")
                        //}
                        
                        sealed class ASealed {
                            class AInner : ASealed()
                        }
                        
                        abstract class SuperClass {
                            //abstract fun abstractFun(): String
                        }
                        
                        class ImplementingClass : SuperClass() {
                            //override fun abstractFun(): String = ""
                        }
                        
                        fun aFunction() {
                        }

                        @com.ivianuu.injekt.Reader
                        fun bFunction(p: String) {
                            p
                        }
                        
                        fun <T, S> aFunction(a: T, b: S) {
                        }
                        
                        fun higherOrder(block: (Int, Long) -> String) {
                            returningString(0f, 0)
                        }
                        
                        fun throwing() {
                            throw RuntimeException("")
                        }
                        
                        fun tryCatch() {
                            try {
                                println("do work")
                            } catch (e: RuntimeException) {
                                println("runtime ${DOLLAR_SIGN}e")
                            } catch(e: Exception) { 
                                println("exception ${DOLLAR_SIGN}e")
                            } finally {
                                println("finally")
                            }
                        }
                        
                        /*fun withVararg(vararg params: String) {
                            withVararg(*params)
                        }*/
                        
                        fun whileF() {
                            while (true) {
                                println("while")
                            }
                        }
                        
                        fun doWhile() {
                            do {
                                println("do")
                            } while (true)
                        }
                        
                        fun returningString(a: Float = 0f, b: Int = 0, c: Long = 0L) = 0
                        
                        typealias MyTypeAlias = () -> String
                        
                        typealias MyTypeAlias2<T> = () -> String
                        
                        val prop = "hello world ${DOLLAR_SIGN}{aFunction()}lol${DOLLAR_SIGN}{returningString(0f)}"
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
                                                override fun visitClass(
                                                    klass: AstClass,
                                                    data: Nothing?
                                                ): AstTransformResult<AstElement> {
                                                    moduleFragment.files += AstFile(
                                                        packageFqName = file.packageFqName,
                                                        name = (klass.name.asString()
                                                            .removeIllegalChars() + "Context.kt").asNameId()
                                                    ).apply {
                                                        addChild(
                                                            AstClass(
                                                                name = (klass.name.asString()
                                                                    .removeIllegalChars() + "Context").asNameId()
                                                            )
                                                        )
                                                    }
                                                    return super.visitClass(klass, null)
                                                }
                                            },
                                            null
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
