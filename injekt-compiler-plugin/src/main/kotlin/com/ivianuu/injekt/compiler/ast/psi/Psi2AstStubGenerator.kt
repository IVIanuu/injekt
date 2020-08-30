package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclarationContainer
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstExternalPackageFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstProperty
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeAlias
import com.ivianuu.injekt.compiler.ast.tree.declaration.addChild
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor

class Psi2AstStubGenerator(
    private val storage: Psi2AstStorage
) {

    lateinit var translator: Psi2AstTranslator

    fun get(descriptor: DeclarationDescriptor): AstElement {
        return when (descriptor) {
            is PackageFragmentDescriptor -> descriptor.toAstPackageFragment()
            is ClassDescriptor -> descriptor.toAstClassStub()
            is FunctionDescriptor -> descriptor.toAstFunctionStub()
            is PropertyDescriptor -> descriptor.toAstPropertyStub()
            is TypeAliasDescriptor -> descriptor.toAstTypeAliasStub()
            else -> error("Unexpected descriptor $descriptor ${descriptor.javaClass}")
        }
    }

    private fun PackageFragmentDescriptor.toAstPackageFragment() =
        storage.externalPackageFragments.getOrPut(this) {
            AstExternalPackageFragment(fqName)
        }

    private fun ClassDescriptor.toAstClassStub(): AstClass {
        storage.classes[original]?.let { return it }
        return AstLazyClass(
            descriptor = this,
            translator = translator,
            stubGenerator = this@Psi2AstStubGenerator
        ).apply {
            storage.classes[original] = this
            (get(containingDeclaration) as AstDeclarationContainer).addChild(this)
        }
    }

    private fun FunctionDescriptor.toAstFunctionStub(): AstFunction {
        storage.functions[original]?.let { return it }
        return AstFunction(
            name = name,
            kind = toAstFunctionKind(),
            visibility = visibility.toAstVisibility(),
            expectActual = expectActualOf(isActual, isExpect),
            modality = modality.toAstModality(),
            returnType = with(translator) {
                returnType!!.toAstType()
            },
            isInline = isInline,
            isExternal = isExternal,
            isInfix = isInfix,
            isOperator = isOperator,
            isTailrec = isTailrec,
            isSuspend = isSuspend
        ).apply {
            storage.functions[original] = this
            (get(containingDeclaration) as AstDeclarationContainer).addChild(this)
            with(translator) {
                annotations += this@toAstFunctionStub.annotations.toAstAnnotations()
                typeParameters += this@toAstFunctionStub.typeParameters.toAstTypeParameters()
                    .onEach { it.parent = this@apply }
                dispatchReceiverType = dispatchReceiverParameter?.type?.toAstType()
                extensionReceiverType = extensionReceiverParameter?.type?.toAstType()
                valueParameters += this@toAstFunctionStub.valueParameters.toAstValueParameters()
                    .onEach { it.parent = this@apply }
                overriddenDeclarations += overriddenDescriptors
                    .map { it.toAstFunctionStub() }
            }
        }
    }

    private fun PropertyDescriptor.toAstPropertyStub(): AstProperty {
        storage.properties[original]?.let { return it }
        return AstProperty(
            name = name,
            type = with(translator) { type.toAstType() },
            kind = when {
                isConst -> AstProperty.Kind.CONST_VAL
                isLateInit -> AstProperty.Kind.LATEINIT_VAR
                isVar -> AstProperty.Kind.VAR
                else -> AstProperty.Kind.VAl
            },
            modality = modality.toAstModality(),
            visibility = visibility.toAstVisibility(),
            expectActual = expectActualOf(isActual, isExpect),
            isExternal = isExternal
        ).apply {
            storage.properties[original] = this
            (get(containingDeclaration) as AstDeclarationContainer).addChild(this)
            with(translator) {
                annotations += this@toAstPropertyStub.annotations.toAstAnnotations()
                typeParameters += this@toAstPropertyStub.typeParameters.toAstTypeParameters()
                    .onEach { it.parent = this@apply }
                dispatchReceiverType = dispatchReceiverParameter?.type?.toAstType()
                extensionReceiverType = extensionReceiverParameter?.type?.toAstType()
            }
        }
    }

    private fun TypeAliasDescriptor.toAstTypeAliasStub(): AstTypeAlias {
        storage.typeAliases.get(original)?.let { return it }
        return AstTypeAlias(
            name = name,
            type = with(translator) { expandedType.toAstType() },
            visibility = visibility.toAstVisibility(),
            expectActual = expectActualOf(isActual, isExpect)
        ).apply {
            storage.typeAliases[original] = this
            (get(containingDeclaration) as AstDeclarationContainer).addChild(this)
            with(translator) {
                annotations += this@toAstTypeAliasStub.annotations.toAstAnnotations()
                typeParameters += this@toAstTypeAliasStub.declaredTypeParameters.toAstTypeParameters()
                    .onEach { it.parent = this@apply }
            }
        }
    }

}
