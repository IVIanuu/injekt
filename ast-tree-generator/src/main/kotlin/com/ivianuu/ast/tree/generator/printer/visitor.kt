package com.ivianuu.ast.tree.generator.printer

import com.ivianuu.ast.tree.generator.context.AbstractAstTreeBuilder
import com.ivianuu.ast.tree.generator.model.Element

import java.io.File

fun printVisitor(elements: List<Element>, generationPath: File) {
    val dir = File(generationPath, VISITOR_PACKAGE.replace(".", "/"))
    dir.mkdirs()
    File(dir, "AstVisitor.kt").useSmartPrinter {
        println("package $VISITOR_PACKAGE")
        println()
        elements.forEach { println("import ${it.fullQualifiedName}") }
        println()
        printGeneratedMessage()

        println("abstract class AstVisitor<out R, in D> {")

        pushIndent()
        println("abstract fun visitElement(element: AstElement, data: D): R\n")
        for (element in elements) {
            if (element == AbstractAstTreeBuilder.baseAstElement) continue
            with(element) {
                val varName = safeDecapitalizedName
                println("open fun ${typeParameters}visit$name($varName: $typeWithArguments, data: D): R${multipleUpperBoundsList()} = visit${element.visitorSuperType!!.name}($varName, data)")
                println()
            }
        }
        popIndent()
        println("}")
    }
}


fun printVisitorVoid(elements: List<Element>, generationPath: File) {
    val dir = File(generationPath, VISITOR_PACKAGE.replace(".", "/"))
    dir.mkdirs()
    File(dir, "AstVisitorVoid.kt").useSmartPrinter {
        println("package $VISITOR_PACKAGE")
        println()
        elements.forEach { println("import ${it.fullQualifiedName}") }
        println()
        printGeneratedMessage()

        println("abstract class AstVisitorVoid : AstVisitor<Unit, Nothing?>() {")

        withIndent {
            println("abstract fun visitElement(element: AstElement)")
            println()
            for (element in elements) {
                if (element == AbstractAstTreeBuilder.baseAstElement) continue
                with(element) {
                    val varName = safeDecapitalizedName
                    println("open fun ${typeParameters}visit$name($varName: $typeWithArguments)${multipleUpperBoundsList()}{")
                    withIndent {
                        println("visitElement($varName)")
                    }
                    println("}")
                    println()
                }
            }

            for (element in elements) {
                with(element) {
                    val varName = safeDecapitalizedName
                    println("final override fun ${typeParameters}visit$name($varName: $typeWithArguments, data: Nothing?)${multipleUpperBoundsList()}{")
                    withIndent {
                        println("visit$name($varName)")
                    }
                    println("}")
                    println()
                }
            }
        }
        println("}")
    }
}
