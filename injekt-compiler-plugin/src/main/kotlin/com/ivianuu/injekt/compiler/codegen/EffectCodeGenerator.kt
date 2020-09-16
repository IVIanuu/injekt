package com.ivianuu.injekt.compiler.codegen

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.analysis.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.analysis.hasAnnotation
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.getJoinedName
import com.ivianuu.injekt.compiler.irLambda
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.namedDeclarationVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

class EffectCodeGenerator(
    private val bindingContext: BindingContext,
    private val fileManager: FileManager,
    private val module: ModuleDescriptor
) : CodeGenerator {

    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            file.accept(
                namedDeclarationVisitor { declaration ->
                    if (declaration !is KtNamedFunction &&
                        declaration !is KtClassOrObject
                    ) return@namedDeclarationVisitor
                    val descriptor =
                        bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
                            ?: return@namedDeclarationVisitor
                    if (!descriptor.hasAnnotatedAnnotations(InjektFqNames.Effect, module))
                        return@namedDeclarationVisitor
                    generateEffectForDeclaration(declaration, descriptor)
                }
            )
        }
    }

    private fun generateEffectForDeclaration(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor
    ) {
        val effectsName = "${descriptor.name}Effects"

        val effectCode = buildCodeString {
            emitInjektGeneratedHeader()

            emit("object $effectsName ")
            braced {

            }

            val givenType = when (descriptor) {
                is ClassDescriptor -> descriptor.defaultType
                is FunctionDescriptor -> {
                    if (descriptor.hasAnnotation(InjektFqNames.Given)) {
                        descriptor.returnType
                    } else {
                        val parametersSize = descriptor.valueParameters.size
                        (if (descriptor.isSuspend) module.builtIns.getSuspendFunction(parametersSize)
                        else module.builtIns.getFunction(parametersSize))
                            .defaultType
                            .replace(
                                newArguments = descriptor.valueParameters
                                    .map { it.returnType!!.asTypeProjection() } +
                                        descriptor.returnType!!.asTypeProjection()
                            )
                            .let {
                                if (descriptor.hasAnnotation(FqName("androidx.compose.runtime.Composable"))) {
                                    it.replaceAnnotations(
                                        Annotations.create(
                                            it.annotations + AnnotationDescriptorImpl(
                                                module.findClassAcrossModuleDependencies(
                                                    ClassId.topLevel(FqName("androidx.compose.runtime.Composable"))
                                                )!!.defaultType,
                                                emptyMap(),
                                                SourceElement.NO_SOURCE
                                            )
                                        )
                                    )
                                } else it
                            }
                            .let {
                                it.replaceAnnotations(
                                    Annotations.create(
                                        it.annotations + AnnotationDescriptorImpl(
                                            module.findClassAcrossModuleDependencies(
                                                ClassId.topLevel(InjektFqNames.Qualifier)
                                            )!!.defaultType,
                                            mapOf(
                                                "value".asNameId(),
                                                StringValue(descriptor)
                                            ),
                                            SourceElement.NO_SOURCE
                                        )
                                    )
                                )
                                it.withAnnotations(
                                    listOf(
                                        DeclarationIrBuilder(
                                            pluginContext,
                                            declaration.symbol
                                        ).run {
                                            irCall(injektSymbols.qualifier.constructors.single()).apply {
                                                putValueArgument(
                                                    0,
                                                    irString(declaration.uniqueKey())
                                                )
                                            }
                                        }
                                    )
                                )
                            }
                    }
                }
                is IrProperty -> declaration.getter!!.returnType
                is IrField -> declaration.type
                else -> error("Unexpected given declaration ${declaration.dump()}")
            }

            if (declaration is IrFunction && !declaration.hasAnnotation(InjektFqNames.Given)) {
                addFunction("function", givenType).apply function@{
                    addMetadataIfNotLocal()

                    dispatchReceiverParameter = thisReceiver!!.copyTo(this)

                    DeclarationIrBuilder(pluginContext, symbol).run {
                        annotations += irCall(injektSymbols.given.constructors.single())
                    }

                    body = DeclarationIrBuilder(pluginContext, symbol).run {
                        irExprBody(
                            irLambda(givenType) {
                                irCall(declaration.symbol).apply {
                                    if (declaration.dispatchReceiverParameter != null) {
                                        dispatchReceiver =
                                            irGetObject(declaration.dispatchReceiverParameter!!.type.classOrNull!!)
                                    }
                                    valueParameters.forEachIndexed { index, param ->
                                        putValueArgument(index, irGet(param))
                                    }
                                }
                            }
                        )
                    }
                }
            }

            effects
                .map { it.companionObject() as IrClass }
                .flatMap {
                    it.declarations
                        .filter {
                            it.hasAnnotation(InjektFqNames.Given) ||
                                    it.hasAnnotation(InjektFqNames.GivenMapEntries) ||
                                    it.hasAnnotation(InjektFqNames.GivenSetElements)
                        }
                        .filterIsInstance<IrFunction>()
                }
                .map { effectFunction ->
                    addFunction(
                        getJoinedName(
                            effectFunction.getPackageFragment()!!.fqName,
                            effectFunction.descriptor.fqNameSafe
                        ).asString(),
                        effectFunction.returnType
                            .substitute(
                                mapOf(
                                    effectFunction.typeParameters
                                        .single().symbol to givenType
                                )
                            ),
                        isSuspend = effectFunction.isSuspend
                    ).apply function@{
                        dispatchReceiverParameter = thisReceiver!!.copyTo(this)

                        annotations += effectFunction.annotations
                            .map { it.deepCopyWithSymbols() }

                        body = DeclarationIrBuilder(pluginContext, symbol).run {
                            irExprBody(
                                irCall(effectFunction.symbol).apply {
                                    dispatchReceiver =
                                        irGetObject(effectFunction.dispatchReceiverParameter!!.type.classOrNull!!)
                                    putTypeArgument(0, givenType)
                                }
                            )
                        }
                    }
                }
        }

        fileManager.writeFile(
            packageFqName = descriptor.findPackage().fqName,
            fileName = "$effectsName.kt",
            code = effectCode,
            originatingFiles = listOf(declaration.containingKtFile.virtualFilePath)
        )
    }

}
