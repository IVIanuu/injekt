package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.AstBuiltIns
import com.ivianuu.ast.AstContext
import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstNamedFunction
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.declarations.AstRegularClass
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.symbols.impl.AstNamedFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstPropertySymbol
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.MemberScope

class Psi2AstGeneratorContext(
    val module: ModuleDescriptor,
    val bindingContext: BindingContext,
    val kotlinBuiltIns: KotlinBuiltIns,
    val typeConverter: TypeConverter,
    val symbolTable: DescriptorSymbolTable,
    val constantValueGenerator: ConstantValueGenerator,
    val stubGenerator: DeclarationStubGenerator
) : AstContext {
    override lateinit var builtIns: AstBuiltIns

    private val classes = mutableMapOf<FqName, AstRegularClassSymbol?>()
    private val functions = mutableMapOf<FqName, List<AstNamedFunctionSymbol>>()
    private val constructors = mutableMapOf<FqName, List<AstConstructorSymbol>>()
    private val properties = mutableMapOf<FqName, List<AstPropertySymbol>>()

    override fun referenceClass(fqName: FqName): AstRegularClassSymbol? = classes.getOrPut(fqName) {
        resolveMemberScope(fqName.parent())
            ?.getContributedClassifier(fqName.shortName(), NoLookupLocation.FROM_BACKEND)
            ?.let { it as ClassDescriptor }
            ?.let { descriptor ->
                symbolTable.getClassSymbol(descriptor)
                    .also { stubGenerator.getDeclaration(it, descriptor) }
            }
    }

    override fun referenceFunctions(fqName: FqName): List<AstNamedFunctionSymbol> =
        functions.getOrPut(fqName) {
            val scope = resolveMemberScope(fqName.parent())
            scope?.getContributedFunctions(fqName.shortName(), NoLookupLocation.FROM_BACKEND)
                ?.map { descriptor ->
                    symbolTable.getNamedFunctionSymbol(descriptor)
                        .also { stubGenerator.getDeclaration(it, descriptor) }
                }
                ?: emptyList()
        }

    override fun referenceConstructors(classFqName: FqName): List<AstConstructorSymbol> =
        constructors.getOrPut(classFqName) {
            resolveMemberScope(classFqName.parent())
                ?.getContributedClassifier(classFqName.shortName(), NoLookupLocation.FROM_BACKEND)
                ?.let { it as ClassDescriptor }
                ?.constructors
                ?.map { descriptor ->
                    symbolTable.getConstructorSymbol(descriptor)
                        .also { stubGenerator.getDeclaration(it, descriptor) }
                }
                ?: emptyList()
        }

    override fun referenceProperties(fqName: FqName): List<AstPropertySymbol>? = properties.getOrPut(fqName) {
        resolveMemberScope(fqName.parent())
            ?.getContributedVariables(fqName.shortName(), NoLookupLocation.FROM_BACKEND)
            ?.map { descriptor ->
                symbolTable.getPropertySymbol(descriptor)
                    .also { stubGenerator.getDeclaration(it, descriptor) }
            }
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
