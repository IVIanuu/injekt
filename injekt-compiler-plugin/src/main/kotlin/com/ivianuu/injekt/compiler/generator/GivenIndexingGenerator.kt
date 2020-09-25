package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
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
                    private var inGivenSet = false
                    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
                        val previousInGivenSet = inGivenSet
                        val descriptor = classOrObject.descriptor<ClassDescriptor>()
                        inGivenSet = descriptor.hasAnnotation(InjektFqNames.GivenSet)
                        super.visitClassOrObject(classOrObject)
                        inGivenSet = previousInGivenSet
                    }

                    override fun visitClass(klass: KtClass) {
                        super.visitClass(klass)
                        val descriptor = klass.descriptor<ClassDescriptor>()
                        if ((descriptor.hasAnnotation(InjektFqNames.Given) ||
                                    descriptor.hasAnnotatedAnnotations(InjektFqNames.Effect)) ||
                            descriptor.constructors
                                .any {
                                    it.hasAnnotation(InjektFqNames.Given) ||
                                            it.hasAnnotatedAnnotations(InjektFqNames.Effect)
                                }
                        ) {
                            indexer.index(descriptor)
                            declarationStore
                                .addInternalGiven(
                                    descriptor.getReaderConstructor(given())!!.toCallableRef()
                                )
                        }
                    }

                    override fun visitNamedFunction(function: KtNamedFunction) {
                        super.visitNamedFunction(function)
                        if (!inGivenSet) {
                            val descriptor = function.descriptor<FunctionDescriptor>()
                            if (descriptor.hasAnnotation(InjektFqNames.Given) ||
                                descriptor.hasAnnotation(InjektFqNames.GivenMapEntries) ||
                                descriptor.hasAnnotation(InjektFqNames.GivenSetElements)
                            ) {
                                indexer.index(descriptor)
                                declarationStore
                                    .addInternalGiven(descriptor.toCallableRef())
                            }
                        }
                    }

                    override fun visitProperty(property: KtProperty) {
                        super.visitProperty(property)
                        if (!inGivenSet) {
                            val descriptor = property.descriptor<VariableDescriptor>()
                            if (descriptor is PropertyDescriptor &&
                                descriptor.hasAnnotation(InjektFqNames.Given)
                            ) {
                                indexer.index(descriptor)
                                declarationStore
                                    .addInternalGiven(descriptor.getter!!.toCallableRef())
                            }
                        }
                    }
                }
            )
        }
    }

}
