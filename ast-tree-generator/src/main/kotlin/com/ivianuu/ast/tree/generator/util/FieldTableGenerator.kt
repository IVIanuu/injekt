package com.ivianuu.ast.tree.generator.util

import com.ivianuu.ast.tree.generator.context.AbstractAstTreeBuilder
import com.ivianuu.ast.tree.generator.model.Element
import com.ivianuu.ast.tree.generator.model.Field
import java.io.File

fun printFieldUsageTable(builder: AbstractAstTreeBuilder) {
    val elements = builder.elements.filter { it.allImplementations.isNotEmpty() }
    val fields = elements.flatMapTo(mutableSetOf()) { it.allFields }

    val mapping = mutableMapOf<Element, Set<Field>>()
    val fieldsCount = mutableMapOf<Field, Int>()
    for (element in elements) {
        val containingFields = mutableSetOf<Field>()
        for (field in fields) {
            if (field in element.allFields) {
                containingFields += field
                fieldsCount[field] = fieldsCount.getOrDefault(field, 0) + 1
            }
        }
        mapping[element] = containingFields
    }

    val sortedFields = fields.sortedByDescending { fieldsCount[it] }
    File("compiler/ast/tree/table.csv").printWriter().use { printer ->
        with(printer) {
            val delim = ","
            print(delim)
            println(sortedFields.joinToString(delim) { "${it.name}:${fieldsCount.getValue(it)}" })
            for (element in elements) {
                print(element.name + delim)
                val containingFields = mapping.getValue(element)
                println(sortedFields.joinToString(delim) { if (it in containingFields) "+" else "-" })
            }
        }
    }
}