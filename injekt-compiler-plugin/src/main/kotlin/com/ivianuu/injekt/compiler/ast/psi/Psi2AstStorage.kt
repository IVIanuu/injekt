package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.ast.tree.declaration.AstAnonymousInitializer
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstExternalPackageFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstProperty
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeAlias
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeParameter
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstValueParameter
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeAbbreviation
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeArgument
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeProjection
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.model.TypeArgumentMarker

class Psi2AstStorage {

    val externalPackageFragments =
        mutableMapOf<PackageFragmentDescriptor, AstExternalPackageFragment>()

    val files = mutableMapOf<KtFile, AstFile>()
    val classes = mutableMapOf<ClassDescriptor, AstClass>()
    val functions = mutableMapOf<FunctionDescriptor, AstFunction>()
    val properties = mutableMapOf<VariableDescriptor, AstProperty>()
    val anonymousInitializers = mutableMapOf<KtAnonymousInitializer, AstAnonymousInitializer>()
    val typeParameters = mutableMapOf<TypeParameterDescriptor, AstTypeParameter>()
    val valueParameters = mutableMapOf<VariableDescriptor, AstValueParameter>()
    val typeAliases = mutableMapOf<TypeAliasDescriptor, AstTypeAlias>()

    val types = mutableMapOf<KotlinType, AstType>()
    val typeAbbreviations = mutableMapOf<SimpleType, AstTypeAbbreviation>()
    val typeArguments = mutableMapOf<TypeArgumentMarker, AstTypeArgument>()
    val typeProjections = mutableMapOf<TypeProjection, AstTypeProjection>()

}
