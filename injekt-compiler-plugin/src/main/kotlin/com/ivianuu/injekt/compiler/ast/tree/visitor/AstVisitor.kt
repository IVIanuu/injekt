package com.ivianuu.injekt.compiler.ast.tree.visitor

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstAnonymousInitializer
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclaration
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstModuleFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstPackageFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstProperty
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeAlias
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeParameter
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstValueParameter
import com.ivianuu.injekt.compiler.ast.tree.expression.AstBlock
import com.ivianuu.injekt.compiler.ast.tree.expression.AstConst
import com.ivianuu.injekt.compiler.ast.tree.expression.AstExpression
import com.ivianuu.injekt.compiler.ast.tree.expression.AstQualifiedAccess
import com.ivianuu.injekt.compiler.ast.tree.expression.AstReturn
import com.ivianuu.injekt.compiler.ast.tree.expression.AstStringConcatenation
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeArgument
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeProjection

interface AstVisitor<R, D> {

    fun visitElement(element: AstElement, data: D): R
    fun visitModuleFragment(moduleFragment: AstModuleFragment, data: D) =
        visitElement(moduleFragment, data)

    fun visitPackageFragment(packageFragment: AstPackageFragment, data: D) =
        visitElement(packageFragment, data)

    fun visitFile(file: AstFile, data: D) = visitPackageFragment(file, data)

    fun visitDeclaration(declaration: AstDeclaration, data: D) = visitElement(declaration, data)
    fun visitClass(klass: AstClass, data: D) = visitElement(klass, data)
    fun visitFunction(function: AstFunction, data: D) = visitDeclaration(function, data)
    fun visitProperty(property: AstProperty, data: D) = visitDeclaration(property, data)
    fun visitAnonymousInitializer(anonymousInitializer: AstAnonymousInitializer, data: D) =
        visitDeclaration(anonymousInitializer, data)

    fun visitTypeParameter(typeParameter: AstTypeParameter, data: D) =
        visitDeclaration(typeParameter, data)
    fun visitValueParameter(valueParameter: AstValueParameter, data: D) =
        visitDeclaration(valueParameter, data)

    fun visitTypeAlias(typeAlias: AstTypeAlias, data: D) =
        visitDeclaration(typeAlias, data)

    fun visitExpression(expression: AstExpression, data: D) = visitElement(expression, data)
    fun <T> visitConst(const: AstConst<T>, data: D) = visitExpression(const, data)

    fun visitBlock(block: AstBlock, data: D) = visitExpression(block, data)

    fun visitStringConcatenation(stringConcatenation: AstStringConcatenation, data: D) =
        visitExpression(stringConcatenation, data)

    fun visitQualifiedAccess(qualifiedAccess: AstQualifiedAccess, data: D) =
        visitExpression(qualifiedAccess, data)

    fun visitReturn(astReturn: AstReturn, data: D) = visitExpression(astReturn, data)

    fun visitTypeArgument(typeArgument: AstTypeArgument, data: D) = visitElement(typeArgument, data)
    fun visitType(type: AstType, data: D) = visitTypeArgument(type, data)
    fun visitTypeProjection(typeProjection: AstTypeProjection, data: D) =
        visitTypeArgument(typeProjection, data)

}
