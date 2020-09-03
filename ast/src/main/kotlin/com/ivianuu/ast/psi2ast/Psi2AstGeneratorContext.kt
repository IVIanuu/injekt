package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.AstBuiltIns
import com.ivianuu.ast.extension.AstGeneratorContext
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.BindingContext

class Psi2AstGeneratorContext(
    val module: ModuleDescriptor,
    val bindingContext: BindingContext,
    val kotlinBuiltIns: KotlinBuiltIns,
    val typeConverter: TypeConverter,
    val symbolTable: DescriptorSymbolTable,
    val constantValueGenerator: ConstantValueGenerator,
    override val builtIns: AstBuiltIns
) : AstGeneratorContext
