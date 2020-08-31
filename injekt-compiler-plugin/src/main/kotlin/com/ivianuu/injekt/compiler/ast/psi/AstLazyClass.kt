package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclaration
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeParameter
import com.ivianuu.injekt.compiler.ast.tree.expression.AstQualifiedAccess
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor

class AstLazyClass(
    private val descriptor: ClassDescriptor,
    private val stubGenerator: Psi2AstStubGenerator
) : AstClass(
    descriptor.name,
    descriptor.toAstClassKind(),
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
        /* descriptor.annotations
             .map { stubGenerator.get(it) as AstQualifiedAccess }
             .toMutableList()*/
        mutableListOf() // todo
    }

    override val typeParameters: MutableList<AstTypeParameter> by lazy {
        descriptor.declaredTypeParameters
            .map { stubGenerator.get(it) as AstTypeParameter }
            .onEach { it.parent = this@AstLazyClass }
            .toMutableList()
    }

    override val declarations: MutableList<AstDeclaration> by lazy {
        mutableListOf<AstDeclaration>().apply {
            this += descriptor.constructors.map { stubGenerator.get(it) as AstFunction }
            this += descriptor.defaultType.memberScope.getContributedDescriptors()
                .filterNot { it is PropertyAccessorDescriptor }
                .map { stubGenerator.get(it) as AstDeclaration }
            this += descriptor.staticScope.getContributedDescriptors()
                .map { stubGenerator.get(it) as AstDeclaration }
        }
    }

}
