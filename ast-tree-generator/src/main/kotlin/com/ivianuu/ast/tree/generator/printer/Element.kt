package com.ivianuu.ast.tree.generator.printer

import com.ivianuu.ast.tree.generator.context.AbstractAstTreeBuilder
import com.ivianuu.ast.tree.generator.model.Element
import com.ivianuu.ast.tree.generator.model.Field
import com.ivianuu.ast.tree.generator.model.Implementation
import com.ivianuu.ast.tree.generator.model.Importable
import com.ivianuu.ast.tree.generator.pureAbstractElementType
import java.io.File

fun Element.generateCode(generationPath: File) {
    val dir = generationPath.resolve(packageName.replace(".", "/"))
    dir.mkdirs()
    val file = File(dir, "$type.kt")
    file.useSmartPrinter {
        println("package $packageName")
        println()
        val imports = collectImports()
        imports.forEach { println("import $it") }
        if (imports.isNotEmpty()) {
            println()
        }
        printGeneratedMessage()
        printElement(this@generateCode)
    }
}

fun SmartPrinter.printElement(element: Element) {
    with(element) {
        val isInterface = kind == Implementation.Kind.Interface

        fun abstract() {
            if (!isInterface) {
                print("abstract ")
            }
        }

        fun override() {
            if (this != AbstractAstTreeBuilder.baseAstElement) {
                print("override ")
            }
        }

        print("${kind!!.title} $type")
        if (typeArguments.isNotEmpty()) {
            print(typeArguments.joinToString(", ", "<", ">") { it.toString() })
        }
        val needPureAbstractElement =
            !isInterface && !allParents.any { it.kind == Implementation.Kind.AbstractClass }

        if (parents.isNotEmpty() || needPureAbstractElement) {
            print(" : ")
            if (needPureAbstractElement) {
                print("${pureAbstractElementType.type}()")
                if (parents.isNotEmpty()) {
                    print(", ")
                }
            }
            print(
                parents.joinToString(", ") {
                    var result = it.type
                    parentsArguments[it]?.let { arguments ->
                        result += arguments.values.joinToString(
                            ", ",
                            "<",
                            ">"
                        ) { it.typeWithArguments }
                    }
                    result + it.kind.braces()
                },
            )
        }
        print(multipleUpperBoundsList())
        println("{")
        withIndent {
            allFields.forEach {
                abstract()
                printField(it, isImplementation = false, override = it.fromParent, end = "")
            }
            if (allFields.isNotEmpty()) {
                println()
            }

            override()
            println("fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visit$name(this, data)")

            fun Field.replaceDeclaration(override: Boolean, overriddenType: Importable? = null, forceNullable: Boolean = false) {
                println()
                abstract()
                if (override) print("override ")
                println(replaceFunctionDeclaration(overriddenType, forceNullable))
            }

            allFields.filter { it.withReplace }.forEach {
                it.replaceDeclaration(overriddenFields[it]!![it]!!, forceNullable = it.useNullableForReplace)
                for (overriddenType in it.overriddenTypes) {
                    it.replaceDeclaration(true, overriddenType)
                }
            }

            for (field in allFields) {
                if (!field.needsSeparateTransform) continue
                println()
                abstract()
                if (field.fromParent) {
                    print("override ")
                }
                println(field.transformFunctionDeclaration(typeWithArguments))
            }
            if (needTransformOtherChildren) {
                println()
                abstract()
                if (element.parents.any { it.needTransformOtherChildren }) {
                    print("override ")
                }
                println(transformFunctionDeclaration("OtherChildren", typeWithArguments))
            }

            if (element == AbstractAstTreeBuilder.baseAstElement) {
                require(isInterface)
                println()
                println("fun accept(visitor: AstVisitorVoid) = accept(visitor, null)")
                println()
                println("fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D)")
                println()
                println("fun acceptChildren(visitor: AstVisitorVoid) = acceptChildren(visitor, null)")
                println()
                println("@Suppress(\"UNCHECKED_CAST\")")
                println("fun <E : AstElement, D> transform(visitor: AstTransformer<D>, data: D): CompositeTransformResult<E> =")
                withIndent {
                    println("accept(visitor, data) as CompositeTransformResult<E>")
                }
                println()
                println("fun <E : AstElement> transform(visitor: AstTransformerVoid): CompositeTransformResult<E> =")
                withIndent {
                    println("transform(visitor, null)")
                }
                println()
                println("fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstElement")
                println()
                println("fun <E : AstElement> transformChildren(visitor: AstTransformerVoid): AstElement =")
                withIndent {
                    println("transformChildren(visitor, null)")
                }
            }
        }
        println("}")
    }
}
