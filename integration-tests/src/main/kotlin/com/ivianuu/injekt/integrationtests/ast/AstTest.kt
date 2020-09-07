package com.ivianuu.injekt.integrationtests.ast

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstElement
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.declarations.AstClass
import com.ivianuu.ast.declarations.AstModuleFragment
import com.ivianuu.ast.declarations.AstRegularClass
import com.ivianuu.ast.declarations.addFile
import com.ivianuu.ast.declarations.builder.buildFile
import com.ivianuu.ast.declarations.builder.buildRegularClass
import com.ivianuu.ast.declarations.classId
import com.ivianuu.ast.extension.AstGenerationExtension
import com.ivianuu.ast.psi2ast.astEnabled
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitorVoid
import com.ivianuu.ast.visitors.CompositeTransformResult
import com.ivianuu.ast.visitors.compose
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.removeIllegalChars
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.source
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.ExtensionPoint
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.name.ClassId
import org.junit.Test

class AstTest {

    val DOLLAR_SIGN = "$"

    @Test
    fun lol() {
        astEnabled = true
        codegen(
            source(
                """
                //val lol = ("lol" as String).toInt()
                
                class MyClass {
                
                    init {
                        ("" as String).test()
                    }
                
                    fun String.test() {
                        
                    }
                }
            """,
                injektImports = false,
                initializeInjekt = false
            )
        )
    }

    @Test
    fun testSimple() {
        astEnabled = true
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
                                println(a.toString().plus(c))
                            }
                            
                            fun anonymous() {
                                java.lang.System.out.println(object {
                                })
                            }
                            
                            fun lambda(block: (String, String) -> Unit) {
                            }
                            
                            fun suspendLambda(block: suspend () -> Unit) {
                            }
                            
                            suspend fun suspendFun() {
                            }
                            
                            fun lambdaCaller() {
                                lambda { a, b ->
                                }
                                lambda(fun (a: String, b: String) {
                                
                                })
                                suspendLambda {  }
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
                        }
                        
                        fun callWithVararg() {
                            withVararg("a", "b", "c")
                            withVararg(*arrayOf("a", "b"), "c")
                        }*/
                        
                        fun safeCall(name: String?) {
                            //name?.toList()?.size?.minus(1)
                        }
                        
                        fun comparisonOperations() {
                            0 > 1
                            0f >= 1f
                            0L < 1L
                            0L >= 1L
                        }
                        
                        fun equalityOperations() {
                            "a" == "b"
                            "b" != "a"
                            "a" === "b"
                            "b" === "a"
                        }
                        
                        fun logicOperations() {
                            val result =  "a" == "b" && "b" == "a"
                            val result2 = "a" != "b" || "b" != "a"
                        }
                        
                        fun whileLoop() {
                            while (true) {
                                println("while")
                            }
                        }
                        
                        fun doWhileLoop() {
                            do {
                                println("do")
                            } while (true)
                        }
                        
                        fun forLoop() {
                            for (i in 1.until(100)) {
                                println("item ${DOLLAR_SIGN}i")
                            }
                        }
                        
                        fun ifElse() {
                            if ("a" == "b") {
                                println("first")
                            } else if ("a" == "c" || "a" == "d") {
                                println("second")
                            } else {
                                println("third")
                            }
                        }
                        
                        fun augmentedAssignment() {
                            var i = 0
                            i = 1
                            i += 1
                            i /= 1
                            i -= 1
                            i *= 1
                        }
                        
                        fun lol() {
                            val a = listOf("a")
                            val b = listOf("b")
                            val ab = a + b
                            val a2 = ab - b
                            val b2 = ab - a
                        }
                        
                        /*fun whenBasic() {
                            when {
                                "a" == "b" -> {
                                    println("first")
                                }
                                "a" == "c" -> {
                                    println("second")
                                }
                                else -> {
                                    println("second")
                                }
                            }
                        }*/
                        
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
                                    context: AstContext
                                ) {
                                    moduleFragment.files.toList().forEach { file ->
                                        file.accept(
                                            object : AstVisitorVoid(), AstBuilder by AstBuilder(context) {
                                                override fun visitElement(element: AstElement) {
                                                    element.acceptChildren(this)
                                                }

                                                override fun visitRegularClass(regularClass: AstRegularClass) {
                                                    super.visitRegularClass(regularClass)
                                                    moduleFragment.addFile(
                                                        buildFile {
                                                            packageFqName = file.packageFqName
                                                            name = (regularClass.name.asString()
                                                                .removeIllegalChars() + "Context.kt")
                                                            declarations += buildRegularClass {
                                                                symbol = AstRegularClassSymbol(
                                                                    regularClass
                                                                        .classId
                                                                        .fqName
                                                                        .parent()
                                                                        .child("${regularClass.name}Context".asNameId())
                                                                )
                                                            }
                                                        }
                                                    )
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
