package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektTrace
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.checkers.isReader
import com.ivianuu.injekt.compiler.filePositionOf
import com.ivianuu.injekt.compiler.irtransform.asNameId
import com.ivianuu.injekt.compiler.removeIllegalChars
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Given(GenerationContext::class)
class ContextFactoryGenerator : Generator {

    private val capturedTypeParameters = mutableListOf<TypeParameterDescriptor>()

    private val declarationStore = given<DeclarationStore>()
    private val injektTrace = given<InjektTrace>()

    private inline fun <R> withCapturedTypeParametersIfNeeded(
        owner: DeclarationDescriptor,
        typeParameters: List<TypeParameterDescriptor>,
        block: () -> R
    ): R {
        val isReader = owner.isReader(given())
        if (isReader) capturedTypeParameters += typeParameters
        val result = block()
        if (isReader) capturedTypeParameters -= typeParameters
        return result
    }

    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            file.accept(
                object : KtTreeVisitorVoid() {
                    override fun visitClass(klass: KtClass) {
                        val descriptor = klass.descriptor<ClassDescriptor>()
                        withCapturedTypeParametersIfNeeded(
                            descriptor,
                            descriptor.declaredTypeParameters
                        ) {
                            super.visitClass(klass)
                        }
                    }

                    override fun visitNamedFunction(function: KtNamedFunction) {
                        val descriptor = function.descriptor<FunctionDescriptor>()
                        withCapturedTypeParametersIfNeeded(descriptor, descriptor.typeParameters) {
                            super.visitNamedFunction(function)
                        }
                    }

                    override fun visitProperty(property: KtProperty) {
                        val descriptor = property.descriptor<VariableDescriptor>()
                        withCapturedTypeParametersIfNeeded(descriptor, descriptor.typeParameters) {
                            super.visitProperty(property)
                        }
                    }

                    override fun visitReferenceExpression(expression: KtReferenceExpression) {
                        super.visitReferenceExpression(expression)
                        val resolvedCall = expression.getResolvedCall(given()) ?: return
                        if (resolvedCall.resultingDescriptor.fqNameSafe.asString() == "com.ivianuu.injekt.rootContext" ||
                            resolvedCall.resultingDescriptor.fqNameSafe.asString() == "com.ivianuu.injekt.childContext" ||
                            (resolvedCall.resultingDescriptor.fqNameSafe.asString() == "com.ivianuu.injekt.runReader" &&
                                    resolvedCall.extensionReceiver == null) ||
                            resolvedCall.resultingDescriptor.fqNameSafe.asString() == "com.ivianuu.injekt.runChildReader"
                        ) {
                            generateContextFactory(resolvedCall)
                        }
                    }
                }
            )
        }
    }

    private fun generateContextFactory(call: ResolvedCall<*>) {
        val callElement = call.call.callElement
        val file = callElement.containingKtFile

        val isChild = call.resultingDescriptor.name.asString() == "childContext" ||
                call.resultingDescriptor.name.asString() == "runChildReader"

        val contextType = if (call.resultingDescriptor.name.asString() == "rootContext" ||
            call.resultingDescriptor.name.asString() == "childContext"
        ) {
            call.typeArguments.values.single().toTypeRef()
        } else {
            val contextName = callElement.containingKtFile.name.removeSuffix(".kt") +
                    "${callElement.startOffset}" +
                    "RunReaderContext"
                        .removeIllegalChars()
                        .asNameId()
            if (file.packageFqName.child(contextName.asNameId()) in declarationStore.internalReaderContextsByFqName)
                return
            generateFile(
                packageFqName = file.packageFqName,
                fileName = "$contextName.kt",
                code = buildCodeString {
                    emitLine("// injekt-generated")
                    emitLine("package ${file.packageFqName}")
                    emitLine("import com.ivianuu.injekt.Context")
                    emitLine("interface $contextName : Context")
                }
            )

            SimpleTypeRef(
                classifier = ClassifierRef(
                    fqName = file.packageFqName.child(contextName.asNameId()),
                    superTypes = listOf(
                        moduleDescriptor.findClassAcrossModuleDependencies(
                            ClassId.topLevel(InjektFqNames.Context)
                        )!!.defaultType.toTypeRef()
                    )
                ),
                isContext = true
            )
        }

        val factoryName = (contextType.classifier.fqName.pathSegments()
            .joinToString("_") + "_${file.name.removeSuffix(".kt")}" +
                "${callElement.startOffset}Factory")
            .removeIllegalChars()
            .asNameId()

        val packageFqName = file.packageFqName

        val factoryFqName = packageFqName.child(factoryName)

        if (factoryFqName in declarationStore.internalContextFactories) return

        val implFqName = if (isChild) null else
            packageFqName.child((factoryName.asString() + "Impl").asNameId())

        val typeParameters = if (!isChild) emptyList<ClassifierRef>()
        else capturedTypeParameters.map { capturedTypeParameter ->
            ClassifierRef(
                fqName = factoryFqName.child(capturedTypeParameter.name),
                superTypes = capturedTypeParameter.upperBounds.map { it.toTypeRef() },
                isTypeParameter = true
            )
        }

        val inputs = call.valueArguments
            .entries
            .single { it.key.name.asString() == "inputs" }
            .value
            ?.let { it as VarargValueArgument }
            ?.arguments
            ?.map { it.getArgumentExpression()?.getType(given())!!.toTypeRef() }
            ?: emptyList()

        val code = buildCodeString {
            emitLine("// injekt-generated")
            emitLine("package $packageFqName")

            if (isChild) {
                emitLine("import com.ivianuu.injekt.internal.ChildContextFactory")
                emitLine("@ChildContextFactory")
            } else {
                emitLine("import com.ivianuu.injekt.internal.RootContextFactory")

                emitLine("@RootContextFactory(factoryFqName = \"$implFqName\")")
            }

            emit("interface $factoryName")
            if (typeParameters.isNotEmpty()) {
                emit("<")
                typeParameters.forEachIndexed { index, typeParameter ->
                    emit(typeParameter.fqName.shortName())
                    if (index != typeParameters.lastIndex) emit(", ")
                }
                emit(">")
            }
            emitSpace()

            if (typeParameters.isNotEmpty()) {
                emit("where ")
                val typeParametersWithUpperBounds = typeParameters
                    .flatMap { typeParameter ->
                        typeParameter.superTypes.map { typeParameter to it }
                    }

                typeParametersWithUpperBounds.forEachIndexed { index, (typeParameter, upperBound) ->
                    emit("${typeParameter.fqName.shortName()} : ${upperBound.render()}")
                    if (index != typeParametersWithUpperBounds.lastIndex) emit(", ")
                }

                emitSpace()
            }

            braced {
                emit("fun create(")
                inputs.forEachIndexed { index, type ->
                    emit("p$index: ${type.render()}")
                    if (index != inputs.lastIndex) emit(", ")
                }
                emitLine("): ${contextType.render()}")
            }
        }

        val factoryFile = generateFile(
            packageFqName = packageFqName,
            fileName = "$factoryName.kt",
            code = code
        )

        val factoryDescriptor = ContextFactoryDescriptor(
            factoryType = SimpleTypeRef(
                classifier = ClassifierRef(
                    packageFqName.child(factoryName),
                    typeParameters = typeParameters
                ),
                typeArguments = typeParameters.map { it.defaultType },
                isChildContextFactory = true
            ),
            contextType = contextType,
            inputTypes = inputs
        )
        val declarationStore = given<DeclarationStore>()
        declarationStore.addInternalContextFactory(factoryDescriptor)
        injektTrace.record(
            InjektWritableSlices.CONTEXT_FACTORY,
            filePositionOf(
                file.virtualFilePath,
                callElement.startOffset
            ),
            factoryDescriptor
        )
        if (!isChild) {
            given<Indexer>().index(
                path = rootFactoriesPath,
                fqName = packageFqName.child(factoryName),
                type = "class"
            )
            declarationStore
                .addInternalRootFactory(
                    ContextFactoryImplDescriptor(
                        factoryImplFqName = implFqName!!,
                        factory = factoryDescriptor
                    )
                )
        }
    }
}
