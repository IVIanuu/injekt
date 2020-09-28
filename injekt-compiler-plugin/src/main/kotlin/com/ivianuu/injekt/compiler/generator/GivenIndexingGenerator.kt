package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

@Given
class GivenIndexingGenerator : Generator {

    private val declarationStore = given<DeclarationStore>()
    private val indexer = given<Indexer>()

    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            file.accept(
                object : KtTreeVisitorVoid() {
                    override fun visitClass(klass: KtClass) {
                        super.visitClass(klass)
                        val descriptor = klass.descriptor<ClassDescriptor>()
                        if (descriptor.hasAnnotation(InjektFqNames.Given) ||
                            descriptor.constructors
                                .any {
                                    it.hasAnnotation(InjektFqNames.Given)
                                }
                        ) {
                            indexer.index(
                                givensPathOf(descriptor.defaultType.toTypeRef()),
                                descriptor
                            )
                            declarationStore
                                .addInternalGiven(
                                    descriptor.getReaderConstructor(given())!!.toCallableRef()
                                )
                        }
                    }

                    override fun visitNamedFunction(function: KtNamedFunction) {
                        super.visitNamedFunction(function)
                        val descriptor = function.descriptor<FunctionDescriptor>()
                        when {
                            descriptor.hasAnnotation(InjektFqNames.Given) -> {
                                val returnType = descriptor.toCallableRef().type
                                indexer.index(
                                    givensPathOf(returnType),
                                    descriptor
                                )
                                declarationStore
                                    .addInternalGiven(descriptor.toCallableRef())
                            }
                            descriptor.hasAnnotation(InjektFqNames.GivenMapEntries) -> {
                                val returnType = descriptor.toCallableRef().type
                                indexer.index(
                                    givenMapEntriesPathOf(returnType),
                                    descriptor
                                )
                                declarationStore
                                    .addInternalGiven(descriptor.toCallableRef())
                            }
                            descriptor.hasAnnotation(InjektFqNames.GivenSetElements) -> {
                                val returnType = descriptor.toCallableRef().type
                                indexer.index(
                                    givenSetElementsPathOf(returnType),
                                    descriptor
                                )
                                declarationStore
                                    .addInternalGiven(descriptor.toCallableRef())
                            }
                        }
                    }

                    override fun visitProperty(property: KtProperty) {
                        super.visitProperty(property)
                        val descriptor = property.descriptor<VariableDescriptor>()
                        if (descriptor is PropertyDescriptor &&
                            descriptor.hasAnnotation(InjektFqNames.Given)
                        ) {
                            indexer.index(
                                givensPathOf(descriptor.type.toTypeRef()),
                                descriptor
                            )
                            declarationStore
                                .addInternalGiven(descriptor.getter!!.toCallableRef())
                        }
                    }
                }
            )
        }
    }

}
