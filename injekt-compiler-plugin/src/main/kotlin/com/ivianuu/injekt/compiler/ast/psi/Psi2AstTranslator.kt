package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.ast.AstCall
import com.ivianuu.injekt.compiler.ast.AstCallableId
import com.ivianuu.injekt.compiler.ast.AstClass
import com.ivianuu.injekt.compiler.ast.AstConstructor
import com.ivianuu.injekt.compiler.ast.AstDeclaration
import com.ivianuu.injekt.compiler.ast.AstFile
import com.ivianuu.injekt.compiler.ast.AstModuleFragment
import com.ivianuu.injekt.compiler.ast.AstSimpleFunction
import com.ivianuu.injekt.compiler.ast.AstType
import com.ivianuu.injekt.compiler.ast.AstTypeParameter
import com.ivianuu.injekt.compiler.ast.AstValueParameter
import com.ivianuu.injekt.compiler.ast.addChild
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType

class Psi2AstTranslator(
    private val bindingTrace: BindingTrace,
    private val moduleDescriptor: ModuleDescriptor
) {

    private val classes =
        mutableMapOf<ClassDescriptor, AstClass>()
    private val simpleFunctions =
        mutableMapOf<SimpleFunctionDescriptor, AstSimpleFunction>()
    private val constructors =
        mutableMapOf<ConstructorDescriptor, AstConstructor>()

    fun generateModule(files: List<KtFile>): AstModuleFragment {
        return AstModuleFragment(moduleDescriptor.name).apply {
            this.files = files.map { generateFile(it) }
        }
    }

    fun generateFile(file: KtFile): AstFile {
        return AstFile(
            packageFqName = file.packageFqName,
            name = file.name.asNameId()
        ).apply {
            annotations = generateAnnotations()
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
        val declaration = descriptor.findPsi() as KtClassOrObject
        return classes.getOrPut(descriptor) {
            AstClass(
                classId = descriptor.classId!!.toAstClassId(),
                kind = descriptor.kind.toAstClassKind(),
                visibility = descriptor.visibility.toAstVisibility(),
                multiPlatformModality = multiPlatformModalityOf(
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
                annotations += generateAnnotations()
                typeParameters += generateTypeParameters(descriptor.declaredTypeParameters)
                generateDeclarations(declaration.declarations)
                    .forEach { addChild(it) }
            }
        }
    }

    fun getOrCreateFunction(descriptor: SimpleFunctionDescriptor): AstSimpleFunction {
        return simpleFunctions.getOrPut(descriptor) {
            AstSimpleFunction(
                callableId = AstCallableId(
                    descriptor.findPackage().fqName,
                    descriptor.dispatchReceiverParameter?.value?.type?.constructor?.declarationDescriptor
                        ?.let { it as ClassDescriptor }?.fqNameSafe,
                    descriptor.name
                ),
                visibility = descriptor.visibility.toAstVisibility(),
                multiPlatformModality = multiPlatformModalityOf(
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
                annotations += generateAnnotations()
                typeParameters += generateTypeParameters(descriptor.typeParameters)
                valueParameters += generateValueParameters(descriptor.valueParameters)
                overriddenFunctions += descriptor.overriddenDescriptors
                    .map { getOrCreateFunction(it as SimpleFunctionDescriptor) }
            }
        }
    }

    fun getOrCreateConstructor(descriptor: ConstructorDescriptor): AstConstructor {
        return constructors.getOrPut(descriptor) {
            AstConstructor(
                callableId = AstCallableId(
                    descriptor.findPackage().fqName,
                    null,
                    descriptor.name
                ),
                visibility = descriptor.visibility.toAstVisibility(),
                multiPlatformModality = multiPlatformModalityOf(
                    descriptor.isActual,
                    descriptor.isExpect
                ),
                returnType = descriptor.returnType.toAstType(),
                isPrimary = descriptor.isPrimary
            ).apply {
                constructors[descriptor] = this// todo fixes recursion issues
                annotations += generateAnnotations()
                valueParameters += generateValueParameters(descriptor.valueParameters)
            }
        }
    }

    private fun KotlinType.toAstType(): AstType = TODO()

    // todo
    private fun generateAnnotations() = mutableListOf<AstCall>()

    /*private fun generateModifiers(modifiers: KtModifierList?) =
        modifiers?.node?.children().orEmpty().mapNotNull { node ->
            when (node) {
                is KtAnnotationEntry -> null
                is KtAnnotation -> null
                is PsiWhiteSpace -> null
                else -> AstModifier.values().singleOrNull { it.name.toLowerCase() == node.text }
            }
        }.toMutableList()*/

    private fun generateValueParameters(valueParameters: List<ValueParameterDescriptor>) =
        mutableListOf<AstValueParameter>()

    // todo
    private fun generateTypeParameters(typeParameters: List<TypeParameterDescriptor>) =
        mutableListOf<AstTypeParameter>()

}
