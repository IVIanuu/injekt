package com.ivianuu.ast.tree.generator.util

import com.ivianuu.ast.tree.generator.context.AbstractAstTreeBuilder
import com.ivianuu.ast.tree.generator.model.AbstractElement
import com.ivianuu.ast.tree.generator.model.AstField
import com.ivianuu.ast.tree.generator.model.Element
import com.ivianuu.ast.tree.generator.model.FieldList

fun detectBaseTransformerTypes(builder: AbstractAstTreeBuilder) {
    val usedAsFieldType = mutableMapOf<AbstractElement, Boolean>().withDefault { false }
    for (element in builder.elements) {
        for (field in element.allAstFields) {
            val fieldElement = when (field) {
                is AstField -> field.element
                is FieldList -> field.baseType as Element
                else -> throw IllegalArgumentException()
            }
            usedAsFieldType[fieldElement] = true
        }
    }

    for (element in builder.elements) {
        element.traverseParents {
            if (usedAsFieldType.getValue(it)) {
                element.baseTransformerType = it
                return@traverseParents
            }
        }
    }
}
