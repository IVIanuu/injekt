package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclaration
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeParameter
import com.ivianuu.injekt.compiler.ast.tree.expression.AstQualifiedAccess
import org.jetbrains.kotlin.descriptors.ClassDescriptor

class AstLazyClass(
    private val descriptor: ClassDescriptor,
    private val stubGenerator: Psi2AstStubGenerator,
    private val translator: Psi2AstTranslator
) : AstClass(
    descriptor.name,
    descriptor.kind.toAstClassKind(),
    descriptor.visibility.toAstVisibility(),
    expectActualOf(descriptor.isActual, descriptor.isExpect),
    descriptor.modality.toAstModality(),
    descriptor.isCompanionObject,
    descriptor.isFun,
    descriptor.isData,
    descriptor.isInner,
    descriptor.isExternal
) {

    override val annotations: MutableList<AstQualifiedAccess> by lazy {
        with(translator) {
            descriptor.annotations.toAstAnnotations()
                .toMutableList()
        }
    }

    override val typeParameters: MutableList<AstTypeParameter> by lazy {
        with(translator) {
            descriptor.declaredTypeParameters.toAstTypeParameters()
                .onEach { it.parent = this@AstLazyClass }
                .toMutableList()
        }
    }

    override val declarations: MutableList<AstDeclaration> by lazy {
        with(translator) {
            mutableListOf<AstDeclaration>().apply {
                this += descriptor.constructors.map { it.toAstConstructor() }
                this += descriptor.defaultType.memberScope.getContributedDescriptors()
                    .map { stubGenerator.get(it) as AstDeclaration }
                this += descriptor.staticScope.getContributedDescriptors()
                    .map { stubGenerator.get(it) as AstDeclaration }
            }
        }
    }

}
