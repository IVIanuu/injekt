package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstConstructor
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclarationContainer
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstExternalPackageFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstProperty
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstSimpleFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeAlias
import com.ivianuu.injekt.compiler.ast.tree.declaration.addChild
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor

class Psi2AstStubGenerator(
    private val storage: Psi2AstStorage
) {

    lateinit var translator: Psi2AstTranslator

    fun get(descriptor: DeclarationDescriptor): AstElement {
        return when (descriptor) {
            is PackageFragmentDescriptor -> descriptor.toAstPackageFragment()
            is ClassDescriptor -> descriptor.toAstClassStub()
            is SimpleFunctionDescriptor -> descriptor.toAstSimpleFunctionStub()
            is ConstructorDescriptor -> descriptor.toAstConstructorStub()
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
        storage.classes[this]?.let { return it }
        return AstLazyClass(
            descriptor = this,
            translator = translator,
            stubGenerator = this@Psi2AstStubGenerator
        ).apply {
            storage.classes[this@toAstClassStub] = this
            (get(containingDeclaration) as AstDeclarationContainer).addChild(this)
        }
    }

    private fun SimpleFunctionDescriptor.toAstSimpleFunctionStub(): AstSimpleFunction {
        storage.simpleFunctions[this]?.let { return it }
        return AstSimpleFunction(
            name = name,
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
            storage.simpleFunctions[this@toAstSimpleFunctionStub] = this
            with(translator) {
                annotations += this@toAstSimpleFunctionStub.annotations.toAstAnnotations()
                typeParameters += this@toAstSimpleFunctionStub.typeParameters.toAstTypeParameters()
                    .onEach { it.parent = this@apply }
                valueParameters += this@toAstSimpleFunctionStub.valueParameters.toAstValueParameters()
                    .onEach { it.parent = this@apply }
                overriddenFunctions += overriddenDescriptors
                    .map { (it as SimpleFunctionDescriptor).toAstSimpleFunction() }
            }
        }
    }

    private fun ConstructorDescriptor.toAstConstructorStub(): AstConstructor {
        storage.constructors.get(this)?.let { return it }
        return AstConstructor(
            constructedClass = constructedClass.toAstClassStub(),
            visibility = visibility.toAstVisibility(),
            expectActual = expectActualOf(isActual, isExpect),
            returnType = with(translator) { returnType.toAstType() },
            isPrimary = isPrimary
        ).apply {
            storage.constructors[this@toAstConstructorStub] = this
            with(translator) {
                annotations += this@toAstConstructorStub.annotations.toAstAnnotations()
                typeParameters += this@toAstConstructorStub.typeParameters.toAstTypeParameters()
                    .onEach { it.parent = this@apply }
                valueParameters += this@toAstConstructorStub.valueParameters.toAstValueParameters()
                    .onEach { it.parent = this@apply }
            }
        }
    }

    private fun PropertyDescriptor.toAstPropertyStub(): AstProperty {
        storage.properties[this]?.let { return it }
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
            storage.properties[this@toAstPropertyStub] = this
            with(translator) {
                annotations += this@toAstPropertyStub.annotations.toAstAnnotations()
                typeParameters += this@toAstPropertyStub.typeParameters.toAstTypeParameters()
                    .onEach { it.parent = this@apply }
            }
        }
    }

    private fun TypeAliasDescriptor.toAstTypeAliasStub(): AstTypeAlias {
        storage.typeAliases.get(this)?.let { return it }
        return AstTypeAlias(
            name = name,
            type = with(translator) { expandedType.toAstType() },
            visibility = visibility.toAstVisibility(),
            expectActual = expectActualOf(isActual, isExpect)
        ).apply {
            storage.typeAliases[this@toAstTypeAliasStub] = this
            with(translator) {
                annotations += this@toAstTypeAliasStub.annotations.toAstAnnotations()
                typeParameters += this@toAstTypeAliasStub.declaredTypeParameters.toAstTypeParameters()
                    .onEach { it.parent = this@apply }
            }
        }
    }

}
