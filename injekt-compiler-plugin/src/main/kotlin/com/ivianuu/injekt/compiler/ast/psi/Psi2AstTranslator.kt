package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.ast.tree.AstVariance
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstConstructor
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclaration
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstModuleFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstSimpleFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeAlias
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
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.getAbbreviation
import org.jetbrains.kotlin.types.model.TypeArgumentMarker
import org.jetbrains.kotlin.types.upperIfFlexible

class Psi2AstTranslator(
    private val bindingTrace: BindingTrace,
    private val moduleDescriptor: ModuleDescriptor,
    private val stubGenerator: Psi2AstStubGenerator
) {

    private val classes = mutableMapOf<ClassDescriptor, AstClass>()
    private val simpleFunctions = mutableMapOf<SimpleFunctionDescriptor, AstSimpleFunction>()
    private val constructors = mutableMapOf<ConstructorDescriptor, AstConstructor>()

    fun generateModule(files: List<KtFile>): AstModuleFragment {
        return AstModuleFragment(moduleDescriptor.name).apply {
            this.files += files.map { generateFile(it) }
        }
    }

    fun generateFile(file: KtFile): AstFile {
        return AstFile(
            packageFqName = file.packageFqName,
            name = file.name.asNameId()
        ).apply {
            // todo annotations += generateAnnotations()
            generateDeclarations(file.declarations)
                .forEach { addChild(it) }
        }
    }

    private fun generateDeclarations(declarations: List<KtDeclaration>): MutableList<AstDeclaration> =
        mutableListOf<AstDeclaration>().apply {
            this += declarations
                .filter {
                    // todo
                    it is KtClassOrObject || it is KtFunction
                }
                .map { generateDeclaration(it) }
        }

    private fun generateDeclaration(declaration: KtDeclaration): AstDeclaration =
        when (declaration) {
            is KtClassOrObject -> getOrCreateClass(
                bindingTrace.get(
                    BindingContext.DECLARATION_TO_DESCRIPTOR,
                    declaration
                ) as ClassDescriptor
            )
            is KtFunction -> getOrCreateFunction(
                bindingTrace.get(
                    BindingContext.DECLARATION_TO_DESCRIPTOR,
                    declaration
                ) as SimpleFunctionDescriptor
            )
            is KtConstructor<*> -> getOrCreateConstructor(
                bindingTrace.get(
                    BindingContext.DECLARATION_TO_DESCRIPTOR,
                    declaration
                ) as ConstructorDescriptor
            )
            else -> error("Unexpected declaration $declaration")
        }

    fun getOrCreateClass(descriptor: ClassDescriptor): AstClass {
        val declaration = descriptor.findPsi() as? KtClassOrObject
            ?: return stubGenerator.getOrCreateClass(descriptor)
        return classes.getOrPut(descriptor) {
            AstClass(
                name = descriptor.name,
                kind = descriptor.kind.toAstClassKind(),
                visibility = descriptor.visibility.toAstVisibility(),
                expectActual = multiPlatformModalityOf(
                    descriptor.isActual,
                    descriptor.isExpect
                ),
                modality = descriptor.modality.toAstModality(),
                isCompanion = descriptor.isCompanionObject,
                isFun = descriptor.isFun,
                isData = descriptor.isData,
                isInner = descriptor.isInner,
                isExternal = descriptor.isExternal
            ).apply {
                classes[descriptor] = this// todo fixes recursion issues
                annotations += descriptor.annotations.toAstAnnotations()
                // todo typeParameters += generateTypeParameters(descriptor.declaredTypeParameters)
                generateDeclarations(declaration.declarations)
                    .forEach { addChild(it) }
            }
        }
    }

    fun getOrCreateFunction(descriptor: SimpleFunctionDescriptor): AstSimpleFunction {
        return simpleFunctions.getOrPut(descriptor) {
            AstSimpleFunction(
                name = descriptor.name,
                visibility = descriptor.visibility.toAstVisibility(),
                expectActual = multiPlatformModalityOf(
                    descriptor.isActual,
                    descriptor.isExpect
                ),
                modality = descriptor.modality.toAstModality(),
                returnType = descriptor.returnType!!.toAstType(),
                isInfix = descriptor.isInfix,
                isOperator = descriptor.isOperator,
                isTailrec = descriptor.isTailrec,
                isSuspend = descriptor.isSuspend
            ).apply {
                simpleFunctions[descriptor] = this// todo fixes recursion issues
                annotations += descriptor.annotations.toAstAnnotations()
                //typeParameters += generateTypeParameters(descriptor.typeParameters)
                valueParameters += descriptor.valueParameters.toAstValueParameters()
                overriddenFunctions += descriptor.overriddenDescriptors
                    .map { getOrCreateFunction(it as SimpleFunctionDescriptor) }
            }
        }
    }

    fun getOrCreateConstructor(descriptor: ConstructorDescriptor): AstConstructor {
        return constructors.getOrPut(descriptor) {
            AstConstructor(
                visibility = descriptor.visibility.toAstVisibility(),
                expectActual = multiPlatformModalityOf(
                    descriptor.isActual,
                    descriptor.isExpect
                ),
                returnType = descriptor.returnType.toAstType(),
                isPrimary = descriptor.isPrimary
            ).apply {
                constructors[descriptor] = this// todo fixes recursion issues
                annotations += descriptor.annotations.toAstAnnotations()
                valueParameters += descriptor.valueParameters.toAstValueParameters()
            }
        }
    }

    fun getOrCreateTypeAlias(typeAlias: TypeAliasDescriptor): AstTypeAlias {
        return TODO()
    }

    private fun KotlinType.toAstType(): AstType {
        val approximatedType = upperIfFlexible()
        return AstType().apply {
            classifier =
                when (val classifier = approximatedType.constructor.declarationDescriptor) {
                    is ClassDescriptor -> getOrCreateClass(classifier)
                    else -> error("Unexpected classifier $classifier")
                }
            hasQuestionMark = approximatedType.isMarkedNullable
            arguments += approximatedType.arguments.map { it.toAstTypeArgument() }
            abbreviation = approximatedType.getAbbreviation()?.toAstTypeAbbreviation()
        }
    }

    private fun SimpleType.toAstTypeAbbreviation(): AstTypeAbbreviation {
        val typeAliasDescriptor = constructor.declarationDescriptor.let {
            it as? TypeAliasDescriptor
                ?: throw AssertionError("TypeAliasDescriptor expected: $it")
        }
        return AstTypeAbbreviation(getOrCreateTypeAlias(typeAliasDescriptor)).apply {
            hasQuestionMark = isMarkedNullable
            arguments += this@toAstTypeAbbreviation.arguments.map { it.toAstTypeArgument() }
            annotations += this@toAstTypeAbbreviation.annotations.toList().toAstAnnotations()
        }
    }

    private fun TypeArgumentMarker.toAstTypeArgument(): AstTypeArgument {
        return when (this) {
            is StarProjectionImpl -> AstStarProjection
            is TypeProjection -> toAstTypeProjection()
            else -> error("Unexpected type $this")
        }
    }

    private fun TypeProjection.toAstTypeProjection(): AstTypeProjection {
        return AstTypeProjectionImpl(
            variance = when (projectionKind) {
                Variance.INVARIANT -> null
                Variance.IN_VARIANCE -> AstVariance.IN
                Variance.OUT_VARIANCE -> AstVariance.OUT
            },
            type = type.toAstType()
        )
    }

    // todo
    private fun AnnotationDescriptor.toAstAnnotation() = AstCall()
    private fun List<AnnotationDescriptor>.toAstAnnotations() = map { it.toAstAnnotation() }
    private fun Annotations.toAstAnnotations() = map { it.toAstAnnotation() }

    private fun ValueParameterDescriptor.toAstValueParameter() = AstValueParameter(
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
        annotations += this@toAstValueParameter.annotations.toList().map { it.toAstAnnotation() }
    }

    private fun List<ValueParameterDescriptor>.toAstValueParameters() =
        map { it.toAstValueParameter() }

    // todo
    /*private fun generateTypeParameters(typeParameters: List<TypeParameterDescriptor>) =
        mutableListOf<AstTypeParameter>()*/

}
