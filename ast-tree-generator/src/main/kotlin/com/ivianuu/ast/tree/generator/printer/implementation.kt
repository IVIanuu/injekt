package com.ivianuu.ast.tree.generator.printer

import com.ivianuu.ast.tree.generator.model.AstField
import com.ivianuu.ast.tree.generator.model.Field
import com.ivianuu.ast.tree.generator.model.FieldList
import com.ivianuu.ast.tree.generator.model.FieldWithDefault
import com.ivianuu.ast.tree.generator.model.Implementation
import com.ivianuu.ast.tree.generator.model.Importable
import com.ivianuu.ast.tree.generator.pureAbstractElementType
import java.io.File

fun Implementation.generateCode(generationPath: File) {
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
        printImplementation(this@generateCode)
    }
}

fun SmartPrinter.printImplementation(implementation: Implementation) {
    fun Field.transform() {
        when (this) {
            is FieldWithDefault -> origin.transform()

            is AstField ->
                println("$name = ${name}${call()}transformSingle(transformer, data)")

            is FieldList -> {
                println("${name}.transformInplace(transformer, data)")
            }

            else -> throw IllegalStateException()
        }
    }

    with(implementation) {
        if (requiresOptIn) {
            println("@OptIn(AstImplementationDetail::class)")
        }
        if (!isPublic) {
            print("internal ")
        }
        print("${kind!!.title} $type")
        print(element.typeParameters)

        val isInterface = kind == Implementation.Kind.Interface
        val isAbstract = kind == Implementation.Kind.AbstractClass

        fun abstract() {
            if (isAbstract) {
                print("abstract ")
            }
        }

        if (!isInterface && !isAbstract && fieldsWithoutDefault.isNotEmpty()) {
            if (isPublic) {
                print(" @AstImplementationDetail constructor")
            }
            println("(")
            withIndent {
                fieldsWithoutDefault.forEachIndexed { _, field ->
                    printField(field, isImplementation = true, override = true, end = ",")
                }
            }
            print(")")
        }

        print(" : ")
        if (!isInterface && !allParents.any { it.kind == Implementation.Kind.AbstractClass }) {
            print("${pureAbstractElementType.type}(), ")
        }
        print(allParents.joinToString { "${it.typeWithArguments}${it.kind.braces()}" })
        println(" {")
        withIndent {
            if (isInterface || isAbstract) {
                allFields.forEach {
                    abstract()
                    printField(it, isImplementation = true, override = true, end = "")
                }
            } else {
                fieldsWithDefault.forEach {
                    printFieldWithDefaultInImplementation(it)
                }
                if (fieldsWithDefault.isNotEmpty()) {
                    println()
                }
            }

            element.allFields.filter { it.type.contains("Symbol") && it !is FieldList }
                .takeIf {
                    it.isNotEmpty() && !isInterface && !isAbstract &&
                            !element.type.contains("Reference")
                            && !element.type.contains("ResolvedQualifier")
                            && !element.type.endsWith("Ref")
                }
                ?.let { symbolFields ->
                    println("init {")
                    for (symbolField in symbolFields) {
                        withIndent {
                            println("${symbolField.name}${symbolField.call()}bind(this)")
                        }
                    }
                    println("}")
                    println()
                }

            fun Field.acceptString(): String = "${name}${call()}accept(visitor, data)"
            if (!isInterface && !isAbstract) {
                print("override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {")

                if (element.allAstFields.isNotEmpty()) {
                    println()
                    withIndent {
                        for (field in allFields.filter { it.isAstType }) {
                            if (field.withGetter || !field.needAcceptAndTransform) continue
                            when (field.name) {
                                "explicitReceiver" -> {
                                    val explicitReceiver = implementation["explicitReceiver"]!!
                                    val dispatchReceiver = implementation["dispatchReceiver"]!!
                                    val extensionReceiver = implementation["extensionReceiver"]!!
                                    println(
                                        """
                                    |${explicitReceiver.acceptString()}
                                    |        if (dispatchReceiver !== explicitReceiver) {
                                    |            ${dispatchReceiver.acceptString()}
                                    |        }
                                    |        if (extensionReceiver !== explicitReceiver && extensionReceiver !== dispatchReceiver) {
                                    |            ${extensionReceiver.acceptString()}
                                    |        }
                                        """.trimMargin(),
                                    )
                                }

                                "dispatchReceiver", "extensionReceiver", "subjectVariable", "companionObject" -> {
                                }

                                else -> {
                                    if (type == "AstWhenExpressionImpl" && field.name == "subject") {
                                        println(
                                            """
                                        |val subjectVariable_ = subjectVariable
                                        |        if (subjectVariable_ != null) {
                                        |            subjectVariable_.accept(visitor, data)
                                        |        } else {
                                        |            subject?.accept(visitor, data)
                                        |        }
                                            """.trimMargin(),
                                        )
                                    } else {
                                        when (field.origin) {
                                            is AstField -> {
                                                println(field.acceptString())
                                            }

                                            is FieldList -> {
                                                println("${field.name}.forEach { it.accept(visitor, data) }")
                                            }

                                            else -> throw IllegalStateException()
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
                println("}")
                println()
            }

            abstract()
            print("override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): $typeWithArguments")
            if (!isInterface && !isAbstract) {
                println(" {")
                withIndent {
                    for (field in allFields) {
                        when {
                            !field.isMutable || !field.isAstType || field.withGetter || !field.needAcceptAndTransform -> {
                            }

                            field.name == "explicitReceiver" -> {
                                val explicitReceiver = implementation["explicitReceiver"]!!
                                val dispatchReceiver = implementation["dispatchReceiver"]!!
                                val extensionReceiver = implementation["extensionReceiver"]!!
                                if (explicitReceiver.isMutable) {
                                    println("explicitReceiver = explicitReceiver${explicitReceiver.call()}transformSingle(transformer, data)")
                                }
                                if (dispatchReceiver.isMutable) {
                                    println(
                                        """
                                    |if (dispatchReceiver !== explicitReceiver) {
                                    |            dispatchReceiver = dispatchReceiver.transformSingle(transformer, data)
                                    |        }
                                """.trimMargin(),
                                    )
                                }
                                if (extensionReceiver.isMutable) {
                                    println(
                                        """
                                    |if (extensionReceiver !== explicitReceiver && extensionReceiver !== dispatchReceiver) {
                                    |            extensionReceiver = extensionReceiver.transformSingle(transformer, data)
                                    |        }
                                """.trimMargin(),
                                    )
                                }
                            }

                            field.name in setOf("dispatchReceiver", "extensionReceiver") -> {
                            }

                            field.name == "companionObject" -> {
                                println("companionObject = declarations.asSequence().filterIsInstance<AstRegularClass>().firstOrNull { it.isCompanion }")
                            }

                            field.needsSeparateTransform -> {
                                if (!(element.needTransformOtherChildren && field.needTransformInOtherChildren)) {
                                    println("transform${field.name.capitalize()}(transformer, data)")
                                }
                            }

                            !element.needTransformOtherChildren -> {
                                field.transform()
                            }

                            else -> {
                                field.transform()
                            }
                        }
                    }
                    if (element.needTransformOtherChildren) {
                        println("transformOtherChildren(transformer, data)")
                    }
                    println("return this")
                }
                println("}")
            } else {
                println()
            }

            fun generateReplace(
                field: Field,
                overriddenType: Importable? = null,
                forceNullable: Boolean = false,
                body: () -> Unit,
            ) {
                println()
                abstract()
                print("override ${field.replaceFunctionDeclaration(overriddenType, forceNullable)}")
                if (isInterface || isAbstract) {
                    println()
                    return
                }
                print(" {")
                if (!field.isMutable) {
                    println("}")
                    return
                }
                println()
                withIndent {
                    body()
                }
                println("}")
            }

            for (field in allFields.filter { it.withReplace }) {
                val capitalizedFieldName = field.name.capitalize()
                val newValue = "new$capitalizedFieldName"
                generateReplace(field, forceNullable = field.useNullableForReplace) {
                    when {
                        field.withGetter -> {}

                        field.origin is FieldList -> {
                            println("${field.name}.clear()")
                            println("${field.name}.addAll($newValue)")
                        }

                        else -> {
                            if (field.useNullableForReplace) {
                                println("require($newValue != null)")
                            }
                            println("${field.name} = $newValue")
                        }
                    }
                }

                for (overriddenType in field.overriddenTypes) {
                    generateReplace(field, overriddenType) {
                        println("require($newValue is ${field.typeWithArguments})")
                        println("replace$capitalizedFieldName($newValue)")
                    }
                }
            }
        }
        println("}")
    }
}
