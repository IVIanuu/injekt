package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstConstructor
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstExternalPackageFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstProperty
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstSimpleFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeAlias
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeParameter
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstValueParameter
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeAbbreviation
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeArgument
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeProjection
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.model.TypeArgumentMarker

class Psi2AstStorage {

    val files = mutableMapOf<KtFile, AstFile>()

    val externalPackageFragments =
        mutableMapOf<PackageFragmentDescriptor, AstExternalPackageFragment>()

    val classes = mutableMapOf<ClassDescriptor, AstClass>()
    val simpleFunctions = mutableMapOf<SimpleFunctionDescriptor, AstSimpleFunction>()
    val constructors = mutableMapOf<ConstructorDescriptor, AstConstructor>()
    val properties = mutableMapOf<PropertyDescriptor, AstProperty>()
    val typeParameters = mutableMapOf<TypeParameterDescriptor, AstTypeParameter>()
    val valueParameters = mutableMapOf<ValueParameterDescriptor, AstValueParameter>()
    val typeAliases = mutableMapOf<TypeAliasDescriptor, AstTypeAlias>()

    val types = mutableMapOf<KotlinType, AstType>()
    val typeAbbreviations = mutableMapOf<SimpleType, AstTypeAbbreviation>()
    val typeArguments = mutableMapOf<TypeArgumentMarker, AstTypeArgument>()
    val typeProjections = mutableMapOf<TypeProjection, AstTypeProjection>()

}
