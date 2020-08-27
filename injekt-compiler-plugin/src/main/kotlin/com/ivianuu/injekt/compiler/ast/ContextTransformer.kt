package com.ivianuu.injekt.compiler.ast

import com.ivianuu.injekt.compiler.asNameId

class ContextTransformer : AstTransformer {

    override fun transform(element: AstElement): AstTransformResult<AstElement> {
        if (element is AstFunction && element.annotations.any {
                it.callee.callableId.callableName.asString() == "Reader"
            }) {
            return transformReaderFunction(element)
        }
        return super.transform(element)
    }

    private fun transformReaderFunction(function: AstFunction): AstTransformResult<AstDeclaration> {
        val contextDeclarations = mutableListOf<AstDeclaration>()

        val context = AstClass(
            classId = AstClassId(
                function.callableId.packageName,
                (function.callableId.callableName.asString() + "Context").asNameId()
            )
        )

        val transformedFunction = function

        return AstTransformResult.Multiple(listOf(function, context))
    }

}
