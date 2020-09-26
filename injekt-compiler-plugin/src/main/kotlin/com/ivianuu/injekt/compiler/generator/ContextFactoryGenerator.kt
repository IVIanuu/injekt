package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
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
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import java.io.File

@Given(GenerationContext::class)
class ContextFactoryGenerator : Generator {

    private val fileManager = given<KtFileManager>()
    private val capturedTypeParameters = mutableListOf<TypeParameterDescriptor>()
    private val declarationStore = given<DeclarationStore>()
    private val injektTrace = given<InjektTrace>()

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

                    override fun visitCallExpression(expression: KtCallExpression) {
                        super.visitCallExpression(expression)
                        val resolvedCall = expression.getResolvedCall(given()) ?: return
                        if (resolvedCall.resultingDescriptor.fqNameSafe.asString() == "com.ivianuu.injekt.rootContext" ||
                            resolvedCall.resultingDescriptor.fqNameSafe.asString() == "com.ivianuu.injekt.childContext"
                        ) {
                            generateContextFactoryFor(resolvedCall)
                        }
                    }
                }
            )
        }
    }

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

    private fun generateContextFactoryFor(call: ResolvedCall<*>) {
        val isChild = call.resultingDescriptor.name.asString() == "childContext"

        val contextType = call.typeArguments.values.single().toTypeRef()
            .makeNotNull()

        val inputs = call.valueArguments.values.singleOrNull()
            ?.let { it as VarargValueArgument }
            ?.arguments
            ?.map { it.getArgumentExpression()?.getType(given())!!.toTypeRef() }
            ?: emptyList()

        val containingFile = call.call.callElement.containingKtFile

        val callElement = call.call.callElement
        val factoryName = (contextType.classifier.fqName.pathSegments()
            .joinToString("_") + "_${callElement.containingKtFile.name.removeSuffix(".kt")}${callElement.startOffset}Factory")
            .removeIllegalChars()
            .asNameId()
        val factoryFqName = containingFile.packageFqName.child(factoryName)

        val implFqName = if (isChild) null else
            containingFile.packageFqName.child((factoryName.asString() + "Impl").asNameId())

        val typeParameters = if (!isChild) emptyList<ClassifierRef>()
        else capturedTypeParameters.map {
            ClassifierRef(
                fqName = factoryFqName.child(it.name),
                upperBounds = it.upperBounds.map { it.toTypeRef() },
                isTypeParameter = true
            )
        }

        val code = buildCodeString {
            emitLine("// injekt-generated")
            emitLine("package ${containingFile.packageFqName}")

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
                        typeParameter.upperBounds.map { typeParameter to it }
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

        val factoryFile = fileManager.generateFile(
            packageFqName = containingFile.packageFqName,
            fileName = "$factoryName.kt",
            code = code,
            originatingFiles = listOf(File(callElement.containingKtFile.virtualFilePath))
        )

        val factoryDescriptor = ContextFactoryDescriptor(
            factoryType = SimpleTypeRef(
                classifier = ClassifierRef(
                    containingFile.packageFqName.child(factoryName),
                    typeParameters = typeParameters
                ),
                typeArguments = typeParameters.map { it.defaultType },
                isChildContextFactory = true
            ),
            contextType = contextType,
            inputTypes = inputs
        )
        declarationStore.addInternalContextFactory(factoryDescriptor)
        injektTrace.record(
            InjektWritableSlices.CONTEXT_FACTORY,
            filePositionOf(callElement.containingKtFile.virtualFilePath, callElement.startOffset),
            factoryDescriptor
        )
        if (!isChild) {
            given<Indexer>().index(
                path = rootFactoriesPath,
                fqName = containingFile.packageFqName.child(factoryName),
                type = "class",
                originatingFiles = listOf(factoryFile)
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
