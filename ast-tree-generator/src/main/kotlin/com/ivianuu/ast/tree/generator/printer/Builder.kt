package com.ivianuu.ast.tree.generator.printer

import com.ivianuu.ast.tree.generator.model.Builder
import com.ivianuu.ast.tree.generator.model.Field
import com.ivianuu.ast.tree.generator.model.FieldList
import com.ivianuu.ast.tree.generator.model.FieldWithDefault
import com.ivianuu.ast.tree.generator.model.IntermediateBuilder
import com.ivianuu.ast.tree.generator.model.LeafBuilder
import java.io.File
import kotlin.contracts.ExperimentalContracts

fun Builder.generateCode(generationPath: File) {
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
        printBuilder(this@generateCode)
    }
}

private fun SmartPrinter.printBuilder(builder: Builder) {
    if (builder is LeafBuilder && builder.allFields.isEmpty()) {
        printDslBuildFunction(builder, false)
        return
    }

    println("@AstBuilderDsl")
    when (builder) {
        is IntermediateBuilder -> print("interface ")
        is LeafBuilder -> {
            if (builder.isOpen) {
                print("open ")
            }
            print("class ")
        }
    }
    print(builder.typeWithArguments)
    if (builder is LeafBuilder) print("(override val context: AstContext)")
    if (builder.parents.isNotEmpty()) {
        print(builder.parents.joinToString(separator = ", ", prefix = " : ") { it.type })
    } else {
        print(" : AstBuilder")
    }
    var hasRequiredFields = false
    println(" {")
    withIndent {
        var needNewLine = false
        for (field in builder.allFields) {
            if (field.name == "context") continue
            val (newLine, requiredFields) = printFieldInBuilder(
                field,
                builder,
                fieldIsUseless = false
            )
            needNewLine = newLine
            hasRequiredFields = hasRequiredFields || requiredFields
        }
        val hasBackingFields = builder.allFields.any { it.nullable }
        if (needNewLine) {
            println()
        }
        val buildType = when (builder) {
            is LeafBuilder -> builder.implementation.element.typeWithArguments
            is IntermediateBuilder -> builder.materializedElement!!.typeWithArguments.replace(
                Regex(
                    "<.>"
                ), "<*>"
            )
        }
        if (builder is LeafBuilder && builder.implementation.isPublic) {
            println("@OptIn(AstImplementationDetail::class)")
        }
        if (builder.parents.isNotEmpty()) {
            print("override ")
        }
        print("fun build(): $buildType")
        if (builder is LeafBuilder) {
            println(" {")
            withIndent {
                println("return ${builder.implementation.type}(")
                withIndent {
                    for (field in builder.allFields) {
                        val name = field.name
                        println(name, ",")
                    }
                }
                println(")")
            }
            println("}")
            if (hasBackingFields) {
                println()
            }
        } else {
            println()
        }

        if (builder is LeafBuilder) {
//            for (field in builder.allFields) {
//                printBackingFieldIfNeeded(field)
//            }

            val hasUselessFields = builder.uselessFields.isNotEmpty()
            if (hasUselessFields) {
                println()
                builder.uselessFields.forEachIndexed { index, field ->
                    if (index > 0) {
                        println()
                    }
                    printFieldInBuilder(field, builder, fieldIsUseless = true)
                }
            }
        }
    }
    println("}")
    if (builder is LeafBuilder) {
        println()
        printDslBuildFunction(builder, hasRequiredFields)

        if (builder.wantsCopy) {
            println()
            printDslBuildCopyFunction(builder)
        }
    }
}


private val String.nullable: String get() = if (endsWith("?")) this else "$this?"
private fun FieldWithDefault.needBackingField(fieldIsUseless: Boolean) =
    !nullable && origin !is FieldList && if (fieldIsUseless) {
        defaultValueInImplementation == null
    } else {
        defaultValueInBuilder == null
    }

private fun FieldWithDefault.needNotNullDelegate(fieldIsUseless: Boolean) =
    needBackingField(fieldIsUseless) && (type == "Boolean" || type == "Int")


private fun SmartPrinter.printFieldInBuilder(
    field: FieldWithDefault,
    builder: Builder,
    fieldIsUseless: Boolean
): Pair<Boolean, Boolean> {
    if (field.withGetter && !fieldIsUseless) return false to false
    if (field.origin is FieldList) {
        printFieldListInBuilder(field.origin, builder, fieldIsUseless)
        return true to false
    }
    val name = field.name
    val type = field.typeWithArguments
    val defaultValue = if (fieldIsUseless)
        field.defaultValueInImplementation.also { requireNotNull(it) }
    else
        field.defaultValueInBuilder

    printDeprecationOnUselessFieldIfNeeded(field, builder, fieldIsUseless)
    printModifiers(builder, field, fieldIsUseless)
    print("var $name: $type")
    var hasRequiredFields = false
    val needNewLine = when {
        fieldIsUseless -> {
            println()
            withIndent {
                println("get() = throw IllegalStateException()")
                println("set(value) {")
                withIndent {
                    println("throw IllegalStateException()")
                }
                println("}")
            }
            true
        }

        builder is IntermediateBuilder -> {
            println()
            false
        }
        field.needNotNullDelegate(fieldIsUseless) -> {
            println(" by kotlin.properties.Delegates.notNull<${field.type}>()")
            hasRequiredFields = true
            true
        }

        field.needBackingField(fieldIsUseless) -> {
//            println()
//            withIndent {
//                println("get() = _$name ?: throw IllegalArgumentException(\"$name should be initialized\")")
//                println("set(value) {")
//                withIndent {
//                    println("_$name = value")
//                }
//                println("}")
//                println()
//            }
//            false
            println()
            hasRequiredFields = true
            true
        }
        field.lazyDefault -> {
            println(" by lazyVar { $defaultValue }")
            true
        }
        else -> {
            println(" = $defaultValue")
            true
        }
    }
    return needNewLine to hasRequiredFields
}

private fun SmartPrinter.printDeprecationOnUselessFieldIfNeeded(
    field: Field,
    builder: Builder,
    fieldIsUseless: Boolean
) {
    if (fieldIsUseless) {
        println("@Deprecated(\"Modification of '${field.name}' has no impact for ${builder.type}\", level = DeprecationLevel.HIDDEN)")
    }
}

private fun SmartPrinter.printFieldListInBuilder(
    field: FieldList,
    builder: Builder,
    fieldIsUseless: Boolean
) {
    printDeprecationOnUselessFieldIfNeeded(field, builder, fieldIsUseless)
    printModifiers(builder, field, fieldIsUseless)
    print("val ${field.name}: ${field.mutableType}")
    if (builder is LeafBuilder) {
        print(" = mutableListOf()")
    }
    println()
}

private fun SmartPrinter.printModifiers(builder: Builder, field: Field, fieldIsUseless: Boolean) {
    if (builder is IntermediateBuilder) {
        print("abstract ")
    }
    if (builder.isFromParent(field)) {
        print("override ")
    } else if (builder is LeafBuilder && builder.isOpen) {
        print("open ")
    }
    if (builder is LeafBuilder && field is FieldWithDefault && field.needBackingField(fieldIsUseless) && !fieldIsUseless && !field.needNotNullDelegate(
            fieldIsUseless
        )
    ) {
        print("lateinit ")
    }
}

private fun SmartPrinter.printDslBuildFunction(
    builder: LeafBuilder,
    hasRequiredFields: Boolean
) {
    val isEmpty = builder.allFields.isEmpty()
    if (!isEmpty) {
        println("@OptIn(ExperimentalContracts::class)")
        print("inline ")
    } else if (builder.implementation.isPublic) {
        println("@OptIn(AstImplementationDetail::class)")
    }
    print("fun ")
    builder.implementation.element.typeArguments.takeIf { it.isNotEmpty() }?.let {
        print(it.joinToString(separator = ", ", prefix = "<", postfix = "> ") { it.name })
    }
    val builderType = builder.typeWithArguments
    val name =
        builder.implementation.name?.replaceFirst("Ast", "") ?: builder.implementation.element.name
    print("AstBuilder.build${name}(")
    if (!isEmpty) {
        print("init: $builderType.() -> Unit")
        if (!hasRequiredFields) {
            print(" = {}")
        }
    }
    println("): ${builder.implementation.element.typeWithArguments} {")
    withIndent {
        print("return ")
        if (isEmpty) {
            println("${builder.implementation.type}()")
        } else {
            println("$builderType(context).apply(init).build()")
        }
    }
    println("}")
}

private fun SmartPrinter.printDslBuildCopyFunction(builder: LeafBuilder) {
    val name =
        builder.implementation.name?.replaceFirst("Ast", "") ?: builder.implementation.element.name
    val builderType = builder.typeWithArguments

    println("@OptIn(ExperimentalContracts::class)")
    print("inline fun ")
    builder.implementation.element.typeArguments.takeIf { it.isNotEmpty() }?.let {
        print(it.joinToString(separator = ", ", prefix = "<", postfix = "> ") { it.name })
    }
    print("Ast$name.copy(init: $builderType.() -> Unit = {}")
    println("): ${builder.implementation.element.typeWithArguments} {")
    withIndent {
        println("val copyBuilder = $builderType(context)")
        for (field in builder.allFields) {
            if (field.name == "context") continue
            when (field.origin) {
                is FieldList -> println("copyBuilder.${field.name}.addAll(${field.name})")
                else -> println("copyBuilder.${field.name} = ${field.name}")
            }
        }
        println("return copyBuilder.apply(init).build()")
    }
    println("}")
}
