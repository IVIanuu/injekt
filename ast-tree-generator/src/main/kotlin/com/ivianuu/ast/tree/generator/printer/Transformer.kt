package com.ivianuu.ast.tree.generator.printer

import com.ivianuu.ast.tree.generator.compositeTransformResultType
import com.ivianuu.ast.tree.generator.context.AbstractAstTreeBuilder
import com.ivianuu.ast.tree.generator.model.Element

import java.io.File

fun printTransformer(elements: List<Element>, generationPath: File) {
    val dir = File(generationPath, VISITOR_PACKAGE.replace(".", "/"))
    dir.mkdirs()
    File(dir, "AstTransformer.kt").useSmartPrinter {
        println("package $VISITOR_PACKAGE")
        println()
        elements.forEach { println("import ${it.fullQualifiedName}") }
        println("import ${compositeTransformResultType.fullQualifiedName}")
        println()
        printGeneratedMessage()

        println("abstract class AstTransformer<in D> : AstVisitor<CompositeTransformResult<AstElement>, D>() {")
        println()
        withIndent {
            println("abstract fun <E : AstElement> transformElement(element: E, data: D): CompositeTransformResult<E>")
            println()
            for (element in elements) {
                if (element == AbstractAstTreeBuilder.baseAstElement) continue
                val varName = element.safeDecapitalizedName
                print("open fun ")
                element.typeParameters.takeIf { it.isNotBlank() }?.let { print(it) }
                println(
                    "transform${element.name}($varName: ${element.typeWithArguments}, data: D): CompositeTransformResult<${
                        element.transformerType
                            .typeWithArguments
                    }>${element.multipleUpperBoundsList()}{",
                )
                withIndent {
                    println("return transform${element.visitorSuperType!!.name}($varName, data)")
                }
                println("}")
                println()
            }

            for (element in elements) {
                val varName = element.safeDecapitalizedName
                print("final override fun ")
                element.typeParameters.takeIf { it.isNotBlank() }?.let { print(it) }

                println(
                    "visit${element.name}($varName: ${element.typeWithArguments}, data: D): CompositeTransformResult<${
                        element.transformerType
                            .typeWithArguments
                    }>${element.multipleUpperBoundsList()}{",
                )
                withIndent {
                    println("return transform${element.name}($varName, data)")
                }
                println("}")
                println()
            }
        }
        println("}")
    }
}

fun printTransformerVoid(elements: List<Element>, generationPath: File) {
    val dir = File(generationPath, VISITOR_PACKAGE.replace(".", "/"))
    dir.mkdirs()
    File(dir, "AstTransformerVoid.kt").useSmartPrinter {
        println("package $VISITOR_PACKAGE")
        println()
        elements.forEach { println("import ${it.fullQualifiedName}") }
        println()
        printGeneratedMessage()

        println("abstract class AstTransformerVoid : AstTransformer<Nothing?>() {")

        withIndent {
            println("open fun <E : AstElement> transformElement(element: E): CompositeTransformResult<E> {")
            withIndent {
                println("element.transformChildren(this, null)")
                println("return element.compose()")
                println("}")
            }
            println()
            for (element in elements) {
                if (element == AbstractAstTreeBuilder.baseAstElement) continue
                val varName = element.safeDecapitalizedName
                print("open fun ")
                element.typeParameters.takeIf { it.isNotBlank() }?.let { print(it) }
                println(
                    "transform${element.name}($varName: ${element.typeWithArguments}): CompositeTransformResult<${
                        element.transformerType
                            .typeWithArguments
                    }>${element.multipleUpperBoundsList()}{",
                )
                withIndent {
                    println("return transformElement($varName)")
                }
                println("}")
                println()
            }

            for (element in elements) {
                val varName = element.safeDecapitalizedName
                print("final override fun ")
                if (element == AbstractAstTreeBuilder.baseAstElement) {
                    print("<E : AstElement> ")
                } else {
                    element.typeParameters.takeIf { it.isNotBlank() }?.let { print(it) }
                }
                println(
                    "transform${element.name}($varName: ${
                    if (element == AbstractAstTreeBuilder.baseAstElement) "E"
                    else element.typeWithArguments
                    }, data: Nothing?): CompositeTransformResult<${
                        if (element == AbstractAstTreeBuilder.baseAstElement) "E"
                        else element.transformerType
                            .typeWithArguments
                    }>${element.multipleUpperBoundsList()}{",
                )
                withIndent {
                    println("return transform${element.name}($varName)")
                }
                println("}")
                println()
            }
        }
        println("}")
    }
}
