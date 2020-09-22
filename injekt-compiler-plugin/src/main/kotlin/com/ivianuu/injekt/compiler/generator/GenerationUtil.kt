package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.backend.asNameId
import com.ivianuu.injekt.compiler.frontend.hasAnnotation
import com.ivianuu.injekt.compiler.removeIllegalChars
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.incremental.components.LocationInfo
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.record
import org.jetbrains.kotlin.incremental.recordPackageLookup
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.js.translate.utils.refineType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.CommonSupertypes
import org.jetbrains.kotlin.types.IntersectionTypeConstructor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.getAbbreviation
import org.jetbrains.kotlin.types.upperIfFlexible
import kotlin.math.absoluteValue

@Reader
val isInjektCompiler: Boolean
    get() = moduleDescriptor.name.asString() == "<injekt-compiler-plugin>"

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
            it.type.prepare()
                .constructor.declarationDescriptor?.fqNameSafe
                ?: error("Wtf broken ${it.type} ${it.type.javaClass} ${it.type.prepare()} ${it.type.prepare().javaClass}")
        })
    is PropertyDescriptor -> "${fqNameSafe}${
        if (visibility == Visibilities.LOCAL &&
            name.isSpecial
        ) findPsi()?.startOffset else ""
    }__property${
        listOfNotNull(
            dispatchReceiverParameter?.type?.prepare(),
            extensionReceiverParameter?.type?.prepare()
        ).map { it.constructor.declarationDescriptor!!.fqNameSafe }.hashCode().absoluteValue
    }"
    else -> error("Unsupported declaration $this")
}

fun KotlinType.prepare(): KotlinType {
    var tmp = refineType()
    if (constructor is IntersectionTypeConstructor) {
        tmp = CommonSupertypes.commonSupertype(constructor.supertypes)
    }
    tmp = tmp.upperIfFlexible()
    return tmp
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
        } else if (constructor.declarationDescriptor!! is TypeParameterDescriptor) {
            append(constructor.declarationDescriptor!!.name)
        } else {
            append(constructor.declarationDescriptor!!.fqNameSafe)
        }
        val arguments = abbreviation?.arguments ?: arguments
        if (arguments.isNotEmpty()) {
            append("<")
            arguments.forEachIndexed { index, argument ->
                if (argument.isStarProjection) append("*")
                else argument.type.prepare().renderInner()
                if (index != arguments.lastIndex) append(", ")
            }
            append(">")
        }

        if (isMarkedNullable) append("?")
    }
    prepare().renderInner()
}

fun TypeRef.render(): String = when (this) {
    is KotlinTypeRef -> kotlinType.render()
    is FqNameTypeRef -> buildString {
        append(fqName)
        if (typeArguments.isNotEmpty()) {
            append("<")
            typeArguments.forEachIndexed { index, typeArgument ->
                append(typeArgument.render())
                if (index != typeArguments.lastIndex) append(", ")
            }
            append(">")
        }
    }
}

fun TypeRef.uniqueTypeName(): Name {
    fun TypeRef.renderName(includeArguments: Boolean = true): String {
        return buildString {
            val qualifier = (this@renderName as? KotlinTypeRef)
                ?.kotlinType?.annotations?.findAnnotation(InjektFqNames.Qualifier)
                ?.allValueArguments?.values?.singleOrNull()?.value as? String
            if (qualifier != null) append("${qualifier}_")

            val fqName = when (this@renderName) {
                is KotlinTypeRef -> kotlinType.prepare()
                    .getAbbreviation()?.constructor?.declarationDescriptor?.fqNameSafe
                    ?: kotlinType.prepare().constructor.declarationDescriptor!!.fqNameSafe
                is FqNameTypeRef -> fqName
            }

            append(fqName.pathSegments().joinToString("_") { it.asString() })

            if (includeArguments) {
                when (this@renderName) {
                    is KotlinTypeRef -> {
                        kotlinType.arguments.forEachIndexed { index, typeArgument ->
                            if (index == 0) append("_")
                            if (typeArgument.isStarProjection) append("star")
                            else append(KotlinTypeRef(typeArgument.type).uniqueTypeName())
                            if (index != kotlinType.arguments.lastIndex) append("_")
                        }
                    }
                    is FqNameTypeRef -> {
                        typeArguments.forEachIndexed { index, typeArgument ->
                            if (index == 0) append("_")
                            append(typeArgument.uniqueTypeName())
                            if (index != typeArguments.lastIndex) append("_")
                        }
                    }
                }.let {}
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

lateinit var lookupTracker: LookupTracker

@Reader
fun recordLookup(
    sourceFilePath: String,
    lookedUp: DeclarationDescriptor
) {
    val location = object : LookupLocation {
        override val location: LocationInfo?
            get() = object : LocationInfo {
                override val filePath: String
                    get() = sourceFilePath
                override val position: Position
                    get() = Position.NO_POSITION
            }
    }

    lookupTracker.record(
        location,
        lookedUp.findPackage(),
        lookedUp.name
    )
}

@Reader
fun recordLookup(
    sourceFilePath: String,
    lookedUpFqName: FqName
) {
    val location = object : LookupLocation {
        override val location: LocationInfo?
            get() = object : LocationInfo {
                override val filePath: String
                    get() = sourceFilePath
                override val position: Position
                    get() = Position.NO_POSITION
            }
    }

    lookupTracker.recordPackageLookup(
        location,
        lookedUpFqName.parent().asString(),
        lookedUpFqName.shortName().asString()
    )
}
