package com.ivianuu.injekt.compiler.ast.tree.visitor

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstAnonymousInitializer
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstConstructor
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclaration
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstModuleFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstPackageFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstProperty
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstPropertyAccessor
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstSimpleFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeAlias
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeParameter
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstValueParameter
import com.ivianuu.injekt.compiler.ast.tree.expression.AstBlock
import com.ivianuu.injekt.compiler.ast.tree.expression.AstCall
import com.ivianuu.injekt.compiler.ast.tree.expression.AstConst
import com.ivianuu.injekt.compiler.ast.tree.expression.AstExpression
import com.ivianuu.injekt.compiler.ast.tree.expression.AstGetValueParameter
import com.ivianuu.injekt.compiler.ast.tree.expression.AstReturn
import com.ivianuu.injekt.compiler.ast.tree.expression.AstStatement
import com.ivianuu.injekt.compiler.ast.tree.expression.AstStringConcatenation
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeArgument
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeProjection

interface AstTransformerVoid : AstTransformer<Nothing?> {

    fun <T : AstElement> T.transformChildrenAndCompose(): AstTransformResult<T> {
        transformChildren(this@AstTransformerVoid)
        return compose()
    }

    fun visitElement(element: AstElement) = element.transformChildrenAndCompose()
    override fun visitElement(element: AstElement, data: Nothing?) = visitElement(element)

    fun visitModuleFragment(moduleFragment: AstModuleFragment) =
        moduleFragment.transformChildrenAndCompose()

    override fun visitModuleFragment(moduleFragment: AstModuleFragment, data: Nothing?) =
        visitModuleFragment(moduleFragment)

    fun visitPackageFragment(packageFragment: AstPackageFragment) =
        packageFragment.transformChildrenAndCompose()

    override fun visitPackageFragment(packageFragment: AstPackageFragment, data: Nothing?) =
        visitPackageFragment(packageFragment)

    fun visitFile(file: AstFile) = file.transformChildrenAndCompose()
    override fun visitFile(file: AstFile, data: Nothing?) = visitFile(file)

    fun visitDeclaration(declaration: AstDeclaration): AstTransformResult<AstStatement> =
        declaration.transformChildrenAndCompose() as AstTransformResult<AstStatement>

    override fun visitDeclaration(declaration: AstDeclaration, data: Nothing?) =
        visitDeclaration(declaration)

    fun visitClass(klass: AstClass) = visitDeclaration(klass)
    override fun visitClass(klass: AstClass, data: Nothing?) = visitClass(klass)

    fun visitFunction(function: AstFunction) = visitDeclaration(function)
    override fun visitFunction(function: AstFunction, data: Nothing?) =
        visitFunction(function)

    fun visitSimpleFunction(simpleFunction: AstSimpleFunction) = visitFunction(simpleFunction)
    override fun visitSimpleFunction(simpleFunction: AstSimpleFunction, data: Nothing?) =
        visitSimpleFunction(simpleFunction)

    fun visitConstructor(constructor: AstConstructor) = visitFunction(constructor)
    override fun visitConstructor(constructor: AstConstructor, data: Nothing?) =
        visitConstructor(constructor)

    fun visitPropertyAccessor(propertyAccessor: AstPropertyAccessor) =
        visitFunction(propertyAccessor)

    override fun visitPropertyAccessor(propertyAccessor: AstPropertyAccessor, data: Nothing?) =
        visitPropertyAccessor(propertyAccessor)

    fun visitProperty(property: AstProperty) = visitDeclaration(property)
    override fun visitProperty(property: AstProperty, data: Nothing?) =
        visitProperty(property)

    fun visitAnonymousInitializer(anonymousInitializer: AstAnonymousInitializer) =
        visitDeclaration(anonymousInitializer)

    override fun visitAnonymousInitializer(
        anonymousInitializer: AstAnonymousInitializer,
        data: Nothing?
    ) = visitAnonymousInitializer(anonymousInitializer)

    fun visitTypeParameter(typeParameter: AstTypeParameter) = visitDeclaration(typeParameter)
    override fun visitTypeParameter(typeParameter: AstTypeParameter, data: Nothing?) =
        visitTypeParameter(typeParameter)

    fun visitValueParameter(valueParameter: AstValueParameter) = visitDeclaration(valueParameter)
    override fun visitValueParameter(valueParameter: AstValueParameter, data: Nothing?) =
        visitValueParameter(valueParameter)

    fun visitTypeAlias(typeAlias: AstTypeAlias) = visitDeclaration(typeAlias)
    override fun visitTypeAlias(typeAlias: AstTypeAlias, data: Nothing?) =
        visitTypeAlias(typeAlias)

    fun visitExpression(expression: AstExpression): AstTransformResult<AstStatement> =
        expression.transformChildrenAndCompose() as AstTransformResult<AstStatement>

    override fun visitExpression(expression: AstExpression, data: Nothing?) =
        visitExpression(expression)

    fun <T> visitConst(const: AstConst<T>) = visitExpression(const)
    override fun <T> visitConst(const: AstConst<T>, data: Nothing?) = visitConst(const)

    fun visitBlock(block: AstBlock) = visitExpression(block)
    override fun visitBlock(block: AstBlock, data: Nothing?) = visitBlock(block)

    fun visitStringConcatenation(stringConcatenation: AstStringConcatenation) =
        visitExpression(stringConcatenation)

    override fun visitStringConcatenation(
        stringConcatenation: AstStringConcatenation,
        data: Nothing?
    ) =
        visitStringConcatenation(stringConcatenation)

    fun visitGetValueParameter(getValueParameter: AstGetValueParameter) =
        visitExpression(getValueParameter)

    override fun visitGetValueParameter(getValueParameter: AstGetValueParameter, data: Nothing?) =
        visitGetValueParameter(getValueParameter)

    fun visitCall(call: AstCall) = visitExpression(call)
    override fun visitCall(call: AstCall, data: Nothing?) = visitCall(call)

    fun visitReturn(astReturn: AstReturn) = visitExpression(astReturn)
    override fun visitReturn(astReturn: AstReturn, data: Nothing?) = visitReturn(astReturn)

    fun visitTypeArgument(typeArgument: AstTypeArgument) =
        typeArgument.transformChildrenAndCompose()

    override fun visitTypeArgument(typeArgument: AstTypeArgument, data: Nothing?) =
        visitTypeArgument(typeArgument)

    fun visitType(type: AstType) = type.transformChildrenAndCompose()
    override fun visitType(type: AstType, data: Nothing?) = visitType(type)

    fun visitTypeProjection(typeProjection: AstTypeProjection) =
        typeProjection.transformChildrenAndCompose()

    override fun visitTypeProjection(typeProjection: AstTypeProjection, data: Nothing?) =
        visitTypeProjection(typeProjection)

}
