package com.ivianuu.ast.tree.generator.printer

import com.ivianuu.ast.tree.generator.AstTreeBuilder
import com.ivianuu.ast.tree.generator.compositeTransformResultType
import com.ivianuu.ast.tree.generator.model.Element
import com.ivianuu.ast.tree.generator.model.FieldList
import java.io.File

fun printDeepCopyTransformer(elements: List<Element>, generationPath: File) {
    val dir = File(generationPath, DEEP_COPY_PACKAGE.replace(".", "/"))
    dir.mkdirs()
    File(dir, "DeepCopyTransformerImpl.kt").useSmartPrinter {
        println("package $DEEP_COPY_PACKAGE")
        println()
        elements.forEach { println("import ${it.fullQualifiedName}") }
        elements
            .flatMap { it.allImplementations }
            .mapNotNull { it.builder }
            .forEach { println("import ${it.fullQualifiedName}") }
        println("import $VISITOR_PACKAGE.compose")
        println("import ${compositeTransformResultType.fullQualifiedName}")
        println("import com.ivianuu.ast.expressions.buildConst")
        println()
        printGeneratedMessage()

        println("class DeepCopyTransformerImpl(symbolRemapper: SymbolRemapper) : DeepCopyTransformer(symbolRemapper) {")
        println()
        withIndent {
            for (element in elements) {
                for (implementation in element.allImplementations) {
                    val builder = implementation.builder
                    if (builder == null && implementation.element != AstTreeBuilder.constExpression) continue
                    val varName = element.safeDecapitalizedName
                    print("override fun ")
                    element.typeParameters.takeIf { it.isNotBlank() }?.let { print(it) }
                    println(
                        "transform${element.name}($varName: ${element.typeWithArguments}): CompositeTransformResult<${
                            element.transformerType
                                .typeWithArguments
                        }>${element.multipleUpperBoundsList()}{",
                    )
                    withIndent {
                        if (builder == null) {
                            println("return const.context.buildConst(const.value, const.kind, " +
                                    "const.annotations.mapTo(mutableListOf()) { it.transform() }).compose()")
                            return@withIndent
                        }
                        val builderType = builder.typeWithArguments
                        println("val copyBuilder = $builderType($varName.context)")
                        for (field in builder.allFields) {
                            if (field.name == "context") continue
                            when (field.origin) {
                                is FieldList -> {
                                    when {
                                        field.origin.baseType.packageName?.startsWith("com.ivianuu.ast.symbols") == true -> {
                                            println("copyBuilder.${field.name}.addAll($varName.${field.name}.map { " +
                                                    "it${if (field.origin.nullableBaseType) "?" else ""}.let { symbolRemapper.getSymbol(it) } })")
                                        }
                                        field.origin.baseType is Element -> {
                                            println("copyBuilder.${field.name}.addAll($varName.${field.name}.map { " +
                                                    "it${if (field.origin.nullableBaseType) "?" else ""}.transform() })")
                                        }
                                        else -> {
                                            println("copyBuilder.${field.name}.addAll($varName.${field.name})")
                                        }
                                    }
                                }
                                else -> {
                                    when {
                                        field.origin.fullQualifiedName?.startsWith("com.ivianuu.ast.symbols") == true -> {
                                            println("copyBuilder.${field.name} = $varName.${field.name}${if (field.origin.nullable) "?" else ""}.let { symbolRemapper.getSymbol(it) }")
                                        }
                                        field.origin.isAstType -> {
                                            println("copyBuilder.${field.name} = $varName.${field.name}" +
                                                    "${if (field.nullable) "?" else ""}.transform()")
                                        }
                                        else -> {
                                            println("copyBuilder.${field.name} = $varName.${field.name}")
                                        }
                                    }
                                }
                            }
                        }
                        println("return copyBuilder.build().compose()")
                    }
                    println("}")
                    println()
                }
            }
        }
        println("}")
    }
}
