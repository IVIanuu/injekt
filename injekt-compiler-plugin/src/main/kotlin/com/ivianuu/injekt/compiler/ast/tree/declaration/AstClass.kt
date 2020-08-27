package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.AstClassId
import com.ivianuu.injekt.compiler.ast.tree.AstExpectActual
import com.ivianuu.injekt.compiler.ast.tree.AstModality
import com.ivianuu.injekt.compiler.ast.tree.AstVisibility
import com.ivianuu.injekt.compiler.ast.tree.type.AstClassifier
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformInplace

class AstClass(
    var classId: AstClassId,
    var kind: Kind = Kind.CLASS,
    override var visibility: AstVisibility = AstVisibility.PUBLIC,
    override var expectActual: AstExpectActual? = null,
    override var modality: AstModality = AstModality.FINAL,
    var isCompanion: Boolean = false,
    var isFun: Boolean = false,
    var isData: Boolean = false,
    var isInner: Boolean = false,
    var isExternal: Boolean = false
) : AstDeclarationBase(),
    AstClassifier,
    AstDeclarationWithVisibility,
    AstDeclarationWithExpectActual,
    AstDeclarationWithModality,
    //AstTypeParameterContainer,
    AstDeclarationContainer {

    //override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()
    override val declarations: MutableList<AstDeclaration> = mutableListOf()

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitClass(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        annotations.transformInplace(transformer, data)
        declarations.transformInplace(transformer, data)
    }

    enum class Kind {
        CLASS,
        INTERFACE,
        ENUM_CLASS,
        ENUM_ENTRY,
        ANNOTATION,
        OBJECT
    }
}
