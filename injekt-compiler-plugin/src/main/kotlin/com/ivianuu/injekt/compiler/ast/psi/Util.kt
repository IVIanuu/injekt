package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.ast.extension.AstBuiltIns
import com.ivianuu.injekt.compiler.ast.tree.AstExpectActual
import com.ivianuu.injekt.compiler.ast.tree.AstModality
import com.ivianuu.injekt.compiler.ast.tree.AstVariance
import com.ivianuu.injekt.compiler.ast.tree.AstVisibility
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstProperty
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice

interface Generator {

    val context: GeneratorContext

    fun <T : DeclarationDescriptor> KtElement.descriptor(): T =
        getOrFail(BindingContext.DECLARATION_TO_DESCRIPTOR, this).original as T

    fun <K, V : Any> get(slice: ReadOnlySlice<K, V>, key: K): V? =
        context.bindingContext[slice, key]

    fun <K, V : Any> getOrFail(slice: ReadOnlySlice<K, V>, key: K): V =
        context.bindingContext[slice, key] ?: throw RuntimeException("No $slice for $key")

    fun <K, V : Any> getOrFail(slice: ReadOnlySlice<K, V>, key: K, message: (K) -> String): V =
        context.bindingContext[slice, key] ?: throw RuntimeException(message(key))

    fun KtExpression.getTypeInferredByFrontend(): KotlinType? =
        this@Generator.context.bindingContext.getType(this)

    fun KtExpression.getTypeInferredByFrontendOrFail(): KotlinType =
        getTypeInferredByFrontend() ?: throw RuntimeException("No type for expression: $text")

    fun KtExpression.getExpressionTypeWithCoercionToUnit(): KotlinType? =
        if (isUsedAsExpression(this@Generator.context.bindingContext))
            getTypeInferredByFrontend()
        else this@Generator.context.kotlinBuiltIns.unitType

    fun KtExpression.getExpressionTypeWithCoercionToUnitOrFail(): KotlinType =
        getExpressionTypeWithCoercionToUnit()
            ?: throw RuntimeException("No type for expression: $text")

    fun KtElement.getResolvedCall(): ResolvedCall<out CallableDescriptor>? =
        getResolvedCall(this@Generator.context.bindingContext)

    fun KotlinType.toAstType() = context.typeMapper.translate(this)

}

class GeneratorContext(
    val module: ModuleDescriptor,
    val bindingContext: BindingContext,
    val builtIns: AstBuiltIns,
    val kotlinBuiltIns: KotlinBuiltIns,
    val storage: Psi2AstStorage,
    val typeMapper: TypeMapper,
    val astProvider: AstProvider,
    val stubGenerator: Psi2AstStubGenerator
) {

    private val classes = mutableMapOf<FqName, AstClass?>()
    private val functions = mutableMapOf<FqName, List<AstFunction>>()
    private val constructors = mutableMapOf<FqName, List<AstFunction>>()
    private val properties = mutableMapOf<FqName, List<AstProperty>>()

    fun referenceClass(fqName: FqName): AstClass? = classes.getOrPut(fqName) {
        val scope = resolveMemberScope(fqName.parent())
        scope
            ?.getContributedClassifier(fqName.shortName(), NoLookupLocation.FROM_BACKEND)
            ?.let { it as ClassDescriptor }
            ?.let { storage.classes[it] ?: stubGenerator.get(it) as? AstClass }
    }

    fun referenceFunctions(fqName: FqName): List<AstFunction> =
        functions.getOrPut(fqName) {
            val scope = resolveMemberScope(fqName.parent())
            scope?.getContributedFunctions(fqName.shortName(), NoLookupLocation.FROM_BACKEND)
                ?.mapNotNull {
                    storage.functions[it] ?: stubGenerator.get(it) as? AstFunction
                }
                ?: emptyList()
        }

    fun referenceConstructors(classFqName: FqName): List<AstFunction> =
        constructors.getOrPut(classFqName) {
            val kclass = referenceClass(classFqName)
            kclass
                ?.declarations
                ?.filterIsInstance<AstFunction>()
                ?: emptyList()
        }

    fun referenceProperties(fqName: FqName): List<AstProperty>? = properties.getOrPut(fqName) {
        val scope = resolveMemberScope(fqName.parent())
        scope?.getContributedVariables(fqName.shortName(), NoLookupLocation.FROM_BACKEND)
            ?.mapNotNull { storage.properties[it] ?: stubGenerator.get(it) as? AstProperty }
            ?: emptyList()
    }

    private fun resolveMemberScope(fqName: FqName): MemberScope? {
        val pkg = module.getPackage(fqName)

        if (fqName.isRoot || pkg.fragments.isNotEmpty()) return pkg.memberScope

        val parentMemberScope = resolveMemberScope(fqName.parent()) ?: return null

        val classDescriptor =
            parentMemberScope.getContributedClassifier(
                fqName.shortName(),
                NoLookupLocation.FROM_BACKEND
            ) as? ClassDescriptor
                ?: return null

        return classDescriptor.unsubstitutedMemberScope
    }

}

fun FunctionDescriptor.toAstFunctionKind() = when (this) {
    is ConstructorDescriptor -> AstFunction.Kind.CONSTRUCTOR
    is PropertyGetterDescriptor -> AstFunction.Kind.PROPERTY_GETTER
    is PropertySetterDescriptor -> AstFunction.Kind.PROPERTY_SETTER
    is SimpleFunctionDescriptor -> AstFunction.Kind.SIMPLE_FUNCTION
    else -> error("Unexpected function $this")
}

fun Modality.toAstModality() = when (this) {
    Modality.FINAL -> AstModality.FINAL
    Modality.SEALED -> AstModality.SEALED
    Modality.OPEN -> AstModality.OPEN
    Modality.ABSTRACT -> AstModality.ABSTRACT
}

fun Visibility.toAstVisibility() = when (this) {
    Visibilities.PUBLIC -> AstVisibility.PUBLIC
    Visibilities.INTERNAL -> AstVisibility.INTERNAL
    Visibilities.PROTECTED -> AstVisibility.PROTECTED
    Visibilities.PRIVATE -> AstVisibility.PRIVATE
    Visibilities.LOCAL -> AstVisibility.LOCAL
    else -> AstVisibility.PUBLIC
}

fun ClassDescriptor.toAstClassKind() = when {
    // todo find a better way to detect this
    visibility == Visibilities.LOCAL && name.isSpecial -> AstClass.Kind.ANONYMOUS_OBJECT
    else -> when (kind) {
        ClassKind.CLASS -> AstClass.Kind.CLASS
        ClassKind.INTERFACE -> AstClass.Kind.INTERFACE
        ClassKind.ENUM_CLASS -> AstClass.Kind.ENUM_CLASS
        ClassKind.ENUM_ENTRY -> AstClass.Kind.ENUM_ENTRY
        ClassKind.ANNOTATION_CLASS -> AstClass.Kind.ANNOTATION
        ClassKind.OBJECT -> AstClass.Kind.OBJECT
    }
}

fun Variance.toAstVariance() = when (this) {
    Variance.INVARIANT -> null
    Variance.IN_VARIANCE -> AstVariance.IN
    Variance.OUT_VARIANCE -> AstVariance.OUT
}

fun expectActualOf(
    isActual: Boolean,
    isExpect: Boolean
): AstExpectActual? = when {
    isActual -> AstExpectActual.ACTUAL
    isExpect -> AstExpectActual.EXPECT
    else -> null
}

val UninitializedType = AstType()
