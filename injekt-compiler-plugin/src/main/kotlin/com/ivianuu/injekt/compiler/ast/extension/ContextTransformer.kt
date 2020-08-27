package com.ivianuu.injekt.compiler.ast.extension

import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.ast.AstAnnotationContainer
import com.ivianuu.injekt.compiler.ast.AstCall
import com.ivianuu.injekt.compiler.ast.AstClass
import com.ivianuu.injekt.compiler.ast.AstClassId
import com.ivianuu.injekt.compiler.ast.AstElement
import com.ivianuu.injekt.compiler.ast.AstFunction
import com.ivianuu.injekt.compiler.ast.AstGetValueParameter
import com.ivianuu.injekt.compiler.ast.AstTransformResult
import com.ivianuu.injekt.compiler.ast.AstTransformer
import com.ivianuu.injekt.compiler.ast.AstValueParameter
import com.ivianuu.injekt.compiler.ast.classIdOrFail
import com.ivianuu.injekt.compiler.ast.compose
import com.ivianuu.injekt.compiler.ast.deepCopy
import com.ivianuu.injekt.compiler.ast.defaultType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

fun AstAnnotationContainer.hasAnnotation(classId: AstClassId) =
    annotations.any { it.callee.returnType.classIdOrFail == classId }

class ContextTransformer : AstTransformer {

    private val transformedFunctions = mutableMapOf<AstFunction, AstFunction>()

    override fun transform(element: AstElement): AstTransformResult<AstElement> {
        return super.transform(
            if (element is AstFunction) transformFunctionIfNeeded(element)
            else element
        )
    }

    private fun transformFunctionIfNeeded(function: AstFunction): AstFunction {
        transformedFunctions[function]?.let { return it }
        if (function in transformedFunctions.values) return function
        if (!function.hasAnnotation(Reader)) return function

        val context = AstClass(
            classId = AstClassId(
                function.callableId.packageName,
                (function.callableId.callableName.asString() + "Context").asNameId()
            )
        )

        val transformedFunction = function.deepCopy()
        transformedFunctions[function] = transformedFunction

        val contextValueParameter = AstValueParameter(
            "_context".asNameId(),
            context.defaultType
        ).also {
            transformedFunction.valueParameters += it
            it.parent = transformedFunction
        }

        transformedFunction.body?.let {
            object : AstTransformer {
                override fun transform(element: AstElement): AstTransformResult<AstElement> {
                    if (element !is AstCall) return super.transform(element)
                    if (!element.callee.hasAnnotation(Reader)) return super.transform(element)
                    val transformedCallee = transformFunctionIfNeeded(element.callee)
                    return AstCall(
                        transformedCallee,
                        element.arguments + AstGetValueParameter(contextValueParameter)
                    ).compose()
                }
            }.transform(it)
        }

        return transformedFunction
    }

    val Reader = AstClassId(FqName("com.ivianuu.injekt"), Name.identifier("Reader"))
}
