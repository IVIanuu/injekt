package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstConstructor
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclaration
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstModuleFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstSimpleFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeAlias
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeParameter
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstValueParameter
import com.ivianuu.injekt.compiler.ast.tree.declaration.addChild
import com.ivianuu.injekt.compiler.ast.tree.expression.AstCall
import com.ivianuu.injekt.compiler.ast.tree.type.AstStarProjection
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeAbbreviation
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeArgument
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeProjection
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeProjectionImpl
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.getAbbreviation
import org.jetbrains.kotlin.types.model.TypeArgumentMarker
import org.jetbrains.kotlin.types.upperIfFlexible

class Psi2AstTranslator(
    private val bindingTrace: BindingTrace,
    private val moduleDescriptor: ModuleDescriptor,
    private val stubGenerator: Psi2AstStubGenerator
) {

    private val files = mutableMapOf<KtFile, AstFile>()

    private val classes = mutableMapOf<ClassDescriptor, AstClass>()
    private val simpleFunctions = mutableMapOf<SimpleFunctionDescriptor, AstSimpleFunction>()
    private val constructors = mutableMapOf<ConstructorDescriptor, AstConstructor>()
    private val typeParameters = mutableMapOf<TypeParameterDescriptor, AstTypeParameter>()
    private val valueParameters = mutableMapOf<ValueParameterDescriptor, AstValueParameter>()
    private val typeAliases = mutableMapOf<TypeAliasDescriptor, AstTypeAlias>()

    private val types = mutableMapOf<KotlinType, AstType>()
    private val typeAbbreviations = mutableMapOf<SimpleType, AstTypeAbbreviation>()
    private val typeArguments = mutableMapOf<TypeArgumentMarker, AstTypeArgument>()
    private val typeProjections = mutableMapOf<TypeProjection, AstTypeProjection>()

    fun generateModule(files: List<KtFile>): AstModuleFragment {
        return AstModuleFragment(moduleDescriptor.name).apply {
            this.files += files.map { it.toAstFile() }
        }
    }

    private fun KtFile.toAstFile(): AstFile {
        return files.getOrPut(this) {
            AstFile(
                packageFqName = packageFqName,
                name = name.asNameId()
            ).apply {
                // todo annotations += generateAnnotations()
                this@toAstFile.declarations.toAstDeclarations()
                    .forEach { addChild(it) }
            }
        }
    }

    private fun KtDeclaration.toAstDeclaration(): AstDeclaration =
        when (this) {
            is KtClassOrObject -> (bindingTrace.get(
                BindingContext.DECLARATION_TO_DESCRIPTOR,
                this
            ) as ClassDescriptor).toAstClass()
            is KtFunction -> (
                    bindingTrace.get(
                        BindingContext.DECLARATION_TO_DESCRIPTOR,
                        this
                    ) as SimpleFunctionDescriptor
                    ).toAstSimpleFunction()
            is KtConstructor<*> -> (
                    bindingTrace.get(
                        BindingContext.DECLARATION_TO_DESCRIPTOR,
                        this
                    ) as ConstructorDescriptor
                    ).toAstConstructor()
            is KtTypeAlias -> (
                    bindingTrace.get(
                        BindingContext.DECLARATION_TO_DESCRIPTOR,
                        this
                    ) as TypeAliasDescriptor
                    ).toAstTypeAlias()
            else -> error("Unexpected declaration $this")
        }

    private fun List<KtDeclaration>.toAstDeclarations() =
        map { it.toAstDeclaration() }

    private fun ClassDescriptor.toAstClass(): AstClass {
        val declaration = findPsi() as? KtClassOrObject
            ?: return stubGenerator.getOrCreateClass(this)
        return classes.getOrPut(this) {
            AstClass(
                name = name,
                kind = kind.toAstClassKind(),
                visibility = visibility.toAstVisibility(),
                expectActual = expectActualOf(isActual, isExpect),
                modality = modality.toAstModality(),
                isCompanion = isCompanionObject,
                isFun = isFun,
                isData = isData,
                isInner = isInner,
                isExternal = isExternal
            ).apply {
                classes[this@toAstClass] = this
                annotations += this@toAstClass.annotations.toAstAnnotations()
                typeParameters += declaredTypeParameters.toAstTypeParameters()
                declaration.declarations.toAstDeclarations()
                    .forEach { addChild(it) }
            }
        }
    }

    private fun SimpleFunctionDescriptor.toAstSimpleFunction(): AstSimpleFunction {
        return simpleFunctions.getOrPut(this) {
            AstSimpleFunction(
                name = name,
                visibility = visibility.toAstVisibility(),
                expectActual = expectActualOf(isActual, isExpect),
                modality = modality.toAstModality(),
                returnType = returnType!!.toAstType(),
                isInfix = isInfix,
                isOperator = isOperator,
                isTailrec = isTailrec,
                isSuspend = isSuspend
            ).apply {
                simpleFunctions[this@toAstSimpleFunction] = this
                annotations += this@toAstSimpleFunction.annotations.toAstAnnotations()
                typeParameters += this@toAstSimpleFunction.typeParameters.toAstTypeParameters()
                valueParameters += this@toAstSimpleFunction.valueParameters.toAstValueParameters()
                overriddenFunctions += overriddenDescriptors
                    .map { (it as SimpleFunctionDescriptor).toAstSimpleFunction() }
            }
        }
    }

    private fun ConstructorDescriptor.toAstConstructor(): AstConstructor {
        return constructors.getOrPut(this) {
            AstConstructor(
                visibility = visibility.toAstVisibility(),
                expectActual = expectActualOf(isActual, isExpect),
                returnType = returnType.toAstType(),
                isPrimary = isPrimary
            ).apply {
                constructors[this@toAstConstructor] = this// todo fixes recursion issues
                annotations += this@toAstConstructor.annotations.toAstAnnotations()
                typeParameters += this@toAstConstructor.typeParameters.toAstTypeParameters()
                valueParameters += this@toAstConstructor.valueParameters.toAstValueParameters()
            }
        }
    }

    private fun TypeAliasDescriptor.toAstTypeAlias(): AstTypeAlias {
        return typeAliases.getOrPut(this) {
            AstTypeAlias(
                name = name,
                type = expandedType.toAstType(),
                visibility = visibility.toAstVisibility(),
                expectActual = expectActualOf(isActual, isExpect)
            ).apply {
                typeAliases[this@toAstTypeAlias] = this
                annotations += this@toAstTypeAlias.annotations.toAstAnnotations()
                typeParameters += this@toAstTypeAlias.declaredTypeParameters.toAstTypeParameters()
            }
        }
    }

    // todo
    private fun AnnotationDescriptor.toAstAnnotation() = AstCall()
    private fun List<AnnotationDescriptor>.toAstAnnotations() = map { it.toAstAnnotation() }
    private fun Annotations.toAstAnnotations() = map { it.toAstAnnotation() }

    private fun TypeParameterDescriptor.toAstTypeParameter(): AstTypeParameter {
        return typeParameters.getOrPut(this) {
            AstTypeParameter(
                name = name,
                isReified = isReified,
                variance = variance.toAstVariance()
            ).apply {
                annotations += this@toAstTypeParameter.annotations.toAstAnnotations()
                superTypes += this@toAstTypeParameter.upperBounds.map { it.toAstType() }
            }
        }
    }

    private fun List<TypeParameterDescriptor>.toAstTypeParameters() =
        map { it.toAstTypeParameter() }

    private fun ValueParameterDescriptor.toAstValueParameter(): AstValueParameter {
        return this@Psi2AstTranslator.valueParameters.getOrPut(this) {
            AstValueParameter(
                name = name,
                type = type.toAstType(),
                isVarArg = isVararg,
                inlineHint = when {
                    isCrossinline -> AstValueParameter.InlineHint.CROSSINLINE
                    isNoinline -> AstValueParameter.InlineHint.NOINLINE
                    else -> null
                },
                defaultValue = if (hasDefaultValue()) AstCall() else null // todo
            ).apply {
                annotations += this@toAstValueParameter.annotations.toAstAnnotations()
            }
        }
    }

    private fun List<ValueParameterDescriptor>.toAstValueParameters() =
        map { it.toAstValueParameter() }

    private fun KotlinType.toAstType(): AstType {
        return types.getOrPut(this) {
            val approximatedType = upperIfFlexible()
            AstType().apply {
                classifier =
                    when (val classifier = approximatedType.constructor.declarationDescriptor) {
                        is ClassDescriptor -> classifier.toAstClass()
                        is TypeParameterDescriptor -> classifier.toAstTypeParameter()
                        else -> error("Unexpected classifier $classifier")
                    }
                hasQuestionMark = approximatedType.isMarkedNullable
                arguments += approximatedType.arguments.map { it.toAstTypeArgument() }
                abbreviation = approximatedType.getAbbreviation()?.toAstTypeAbbreviation()
            }
        }
    }

    private fun SimpleType.toAstTypeAbbreviation(): AstTypeAbbreviation {
        return typeAbbreviations.getOrPut(this) {
            val typeAliasDescriptor = constructor.declarationDescriptor.let {
                it as? TypeAliasDescriptor
                    ?: throw AssertionError("TypeAliasDescriptor expected: $it")
            }
            AstTypeAbbreviation(typeAliasDescriptor.toAstTypeAlias()).apply {
                hasQuestionMark = isMarkedNullable
                arguments += this@toAstTypeAbbreviation.arguments.map { it.toAstTypeArgument() }
                annotations += this@toAstTypeAbbreviation.annotations.toList().toAstAnnotations()
            }
        }
    }

    private fun TypeArgumentMarker.toAstTypeArgument(): AstTypeArgument {
        return typeArguments.getOrPut(this) {
            when (this) {
                is StarProjectionImpl -> AstStarProjection
                is TypeProjection -> toAstTypeProjection()
                else -> error("Unexpected type $this")
            }
        }
    }

    private fun TypeProjection.toAstTypeProjection(): AstTypeProjection {
        return typeProjections.getOrPut(this) {
            AstTypeProjectionImpl(
                variance = projectionKind.toAstVariance(),
                type = type.toAstType()
            )
        }
    }

}
