package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.backend.asNameId
import com.ivianuu.injekt.compiler.frontend.hasAnnotation
import com.ivianuu.injekt.compiler.removeIllegalChars
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.getAbbreviation
import kotlin.math.absoluteValue

@Reader
fun <D : DeclarationDescriptor> KtDeclaration.descriptor() =
    given<BindingContext>()[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as D

@Reader
val moduleDescriptor: ModuleDescriptor
    get() = given()

fun DeclarationDescriptor.uniqueKey() = when (this) {
    is ClassDescriptor -> "${fqNameSafe}${
        if (visibility == Visibilities.LOCAL &&
            name.isSpecial
        ) findPsi()?.startOffset else ""
    }__class"
    is FunctionDescriptor -> uniqueFunctionKeyOf(
        fqNameSafe,
        visibility,
        findPsi()?.startOffset,
        allParameters.map {
            it.type.constructor.declarationDescriptor!!.fqNameSafe
        })
    is PropertyDescriptor -> "${fqNameSafe}${
        if (visibility == Visibilities.LOCAL &&
            name.isSpecial
        ) findPsi()?.startOffset else ""
    }__property${
        listOfNotNull(
            dispatchReceiverParameter?.type,
            extensionReceiverParameter?.type
        ).map { it.constructor.declarationDescriptor!!.fqNameSafe }.hashCode().absoluteValue
    }"
    else -> error("Unsupported declaration $this")
}

fun uniqueFunctionKeyOf(
    fqName: FqName,
    visibility: Visibility,
    startOffset: Int? = null,
    parameterTypes: List<FqName>
) = "$fqName${
    if (visibility == Visibilities.LOCAL && fqName.shortName().isSpecial) startOffset ?: "" else ""
}__function${parameterTypes.hashCode().absoluteValue}"

fun KotlinType.render() = buildString {
    fun KotlinType.renderInner() {
        if (hasAnnotation(InjektFqNames.Composable)) {
            append("@${InjektFqNames.Composable} ")
        }
        if (hasAnnotation(InjektFqNames.Reader)) {
            append("@${InjektFqNames.Reader} ")
        }
        val abbreviation = getAbbreviation()
        if (abbreviation != null) {
            append(abbreviation.constructor.declarationDescriptor!!.fqNameSafe)
        } else {
            append(constructor.declarationDescriptor!!.fqNameSafe)
        }
        val arguments = abbreviation?.arguments ?: arguments
        if (arguments.isNotEmpty()) {
            append("<")
            arguments.forEachIndexed { index, argument ->
                if (argument.isStarProjection) append("*")
                else argument.type.renderInner()
                if (index != arguments.lastIndex) append(", ")
            }
            append(">")
        }

        if (isMarkedNullable) append("?")
    }
    renderInner()
}

fun KotlinType.uniqueTypeName(): Name {
    fun KotlinType.renderName(includeArguments: Boolean = true): String {
        return buildString {
            val qualifier = annotations.findAnnotation(InjektFqNames.Qualifier)
                ?.allValueArguments?.values?.singleOrNull()
                ?.value as? String
            if (qualifier != null) append("${qualifier}_")

            val fqName = getAbbreviation()?.constructor?.declarationDescriptor?.fqNameSafe
                ?: constructor.declarationDescriptor!!.fqNameSafe
            append(fqName.pathSegments().joinToString("_") { it.asString() })

            if (includeArguments) {
                arguments.forEachIndexed { index, typeArgument ->
                    if (index == 0) append("_")
                    arguments.forEachIndexed { index, argument ->
                        if (argument.isStarProjection) append("start")
                        else argument.type.renderName()
                        if (index != arguments.lastIndex) append(", ")
                    }
                }
            }
        }
    }

    val fullTypeName = renderName()

    // Conservatively shorten the name if the length exceeds 128
    return (if (fullTypeName.length <= 128) fullTypeName
    else ("${renderName(includeArguments = false)}_${fullTypeName.hashCode()}"))
        .removeIllegalChars()
        .asNameId()
}
