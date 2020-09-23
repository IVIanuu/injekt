package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektAttributes
import com.ivianuu.injekt.compiler.InjektAttributes.ContextFactoryKey
import com.ivianuu.injekt.compiler.irtransform.asNameId
import com.ivianuu.injekt.compiler.removeIllegalChars
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
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

    private val internalFactories = mutableMapOf<TypeRef, ContextFactoryDescriptor>()
    private val externalFactories = mutableMapOf<TypeRef, ContextFactoryDescriptor>()

    fun getContextFactoryDescriptorForType(type: TypeRef): ContextFactoryDescriptor {
        return internalFactories[type] ?: externalFactories[type] ?: kotlin.run {
            val descriptor = moduleDescriptor.findClassAcrossModuleDependencies(
                ClassId.topLevel(type.fqName)
            )!!
            val createFunction = descriptor.unsubstitutedMemberScope
                .getContributedFunctions("create".asNameId(), NoLookupLocation.FROM_BACKEND)
                .single()
            ContextFactoryDescriptor(
                factoryType = type,
                contextType = KotlinTypeRef(createFunction.returnType!!),
                inputTypes = createFunction.valueParameters
                    .map { KotlinTypeRef(it.type) }
            )
        }
    }

    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            file.accept(
                object : KtTreeVisitorVoid() {
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

    private fun generateContextFactoryFor(call: ResolvedCall<*>) {
        val isChild = call.resultingDescriptor.name.asString() == "childContext"

        val contextType = call.typeArguments.values.single()

        val inputs = call.valueArguments.values.singleOrNull()
            ?.let { it as VarargValueArgument }
            ?.arguments
            ?.map { it.getArgumentExpression()?.getType(given())!! }
            ?: emptyList()

        val containingFile = call.call.callElement.containingKtFile

        val callElement = call.call.callElement
        val factoryName = (contextType.constructor.declarationDescriptor!!.fqNameSafe.pathSegments()
            .joinToString("_") + "_${callElement.containingKtFile.name.removeSuffix(".kt")}${callElement.startOffset}Factory")
            .removeIllegalChars()
            .asNameId()

        val implFqName = if (isChild) null else
            containingFile.packageFqName.child((factoryName.asString() + "Impl").asNameId())

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

            emit("interface $factoryName ")
            braced {
                emit("fun create(")
                inputs.forEachIndexed { index, type ->
                    emit("p$index: ${type.render()}")
                    if (index != inputs.lastIndex) emit(", ")
                }
                emitLine("): ${contextType.render()}")
            }
        }

        given<InjektAttributes>()[ContextFactoryKey(
            callElement.containingKtFile.virtualFilePath,
            callElement.startOffset
        )] = containingFile.packageFqName.child(factoryName)

        val factoryFile = fileManager.generateFile(
            packageFqName = containingFile.packageFqName,
            fileName = "$factoryName.kt",
            code = code,
            originatingFiles = listOf(File(callElement.containingKtFile.virtualFilePath))
        )

        val factoryDescriptor = ContextFactoryDescriptor(
            factoryType = SimpleTypeRef(
                fqName = containingFile.packageFqName.child(factoryName),
                isChildContextFactory = true
            ),
            contextType = KotlinTypeRef(contextType),
            inputTypes = inputs.map { KotlinTypeRef(it) }
        )
        internalFactories[factoryDescriptor.factoryType] = factoryDescriptor
        if (!isChild) {
            given<Indexer>().index(
                fqName = containingFile.packageFqName.child(factoryName),
                type = "class",
                originatingFiles = listOf(factoryFile)
            )
            given<RootContextFactoryImplGenerator>()
                .addRootFactory(
                    ContextFactoryImplDescriptor(
                        factoryImplFqName = implFqName!!,
                        factory = factoryDescriptor
                    )
                )
        }
    }
}
