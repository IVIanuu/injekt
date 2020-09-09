package com.ivianuu.ast.tree.generator.printer

import com.ivianuu.ast.tree.generator.context.AbstractAstTreeBuilder
import java.io.File

const val DEEP_COPY_PACKAGE = "com.ivianuu.ast.deepcopy"
const val VISITOR_PACKAGE = "com.ivianuu.ast.visitors"
const val BASE_PACKAGE = "com.ivianuu.ast"
val GENERATED_MESSAGE = """
    /*
     * This file was generated automatically
     * DO NOT MODIFY IT MANUALLY
     */
     """.trimIndent()

fun printElements(builder: AbstractAstTreeBuilder, generationPath: File) {
    builder.elements.forEach { it.generateCode(generationPath) }
    builder.elements.flatMap { it.allImplementations }.forEach { it.generateCode(generationPath) }
    builder.elements.flatMap { it.allImplementations }.mapNotNull { it.builder }
        .forEach { it.generateCode(generationPath) }
    builder.intermediateBuilders.forEach { it.generateCode(generationPath) }

    printVisitor(builder.elements, generationPath)
    printVisitorVoid(builder.elements, generationPath)
    printTransformer(builder.elements, generationPath)
    printTransformerVoid(builder.elements, generationPath)
    printDeepCopyTransformer(builder.elements, generationPath)
}

fun SmartPrinter.printGeneratedMessage() {
    println(GENERATED_MESSAGE)
    println()
}
