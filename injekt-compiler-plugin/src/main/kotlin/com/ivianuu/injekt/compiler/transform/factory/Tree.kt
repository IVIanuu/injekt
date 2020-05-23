/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.MapKey
import com.ivianuu.injekt.compiler.getQualifierFqNames
import com.ivianuu.injekt.compiler.getQualifiers
import com.ivianuu.injekt.compiler.isTypeParameter
import com.ivianuu.injekt.compiler.toAnnotationDescriptor
import com.ivianuu.injekt.compiler.type
import com.ivianuu.injekt.compiler.typeArguments
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.FqNameEqualityChecker
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName

typealias InitializerAccessor = IrBuilderWithScope.(() -> IrExpression) -> IrExpression

fun InitializerAccessor.child(
    child: InitializerAccessor
): InitializerAccessor = {
    child {
        invoke(this, it)
    }
}

fun InitializerAccessor.child(
    accessor: IrFunction
): InitializerAccessor = child {
    irCall(accessor).apply {
        dispatchReceiver = it()
    }
}

interface Node

interface RequirementNode : Node {
    val key: Key
    val initializerAccessor: InitializerAccessor
}

class InstanceNode(
    override val key: Key,
    override val initializerAccessor: InitializerAccessor
) : RequirementNode

class ModuleNode(
    val module: IrClass,
    override val key: Key,
    override val initializerAccessor: InitializerAccessor,
    val typeParametersMap: Map<IrTypeParameterSymbol, IrType>,
    val isStateless: Boolean
) : RequirementNode {
    val descriptor = module.declarations.single {
        it.hasAnnotation(InjektFqNames.AstModule)
    } as IrClass
    val descriptorTypeParametersMap = descriptor.typeParameters.associateWith {
        typeParametersMap.values.toList()[it.index]
    }.mapKeys { it.key.symbol }

    init {
        typeParametersMap.forEach {
            check(!it.value.isTypeParameter()) {
                "Must be concrete type ${it.key.owner.dump()} -> ${it.value.render()}"
            }
        }
    }
}

class FactoryImplementationNode(
    val implFactory: ImplFactory,
    override val key: Key,
    override val initializerAccessor: InitializerAccessor
) : RequirementNode

class DependencyNode(
    val dependency: IrClass,
    override val key: Key,
    override val initializerAccessor: InitializerAccessor
) : RequirementNode

data class BindingRequest(
    val key: Key,
    val requestOrigin: FqName?,
    val requestType: RequestType = key.inferRequestType()
)

fun Key.inferRequestType() = when {
    type.isFunction() && type.typeArguments.size == 1 && InjektFqNames.Provider in type.getQualifierFqNames() -> RequestType.Provider
    else -> RequestType.Instance
}

enum class RequestType {
    Instance,
    Provider
}

sealed class BindingNode(
    val key: Key,
    val dependencies: List<BindingRequest>,
    val targetScope: FqName?,
    val scoped: Boolean,
    val module: ModuleNode?,
    val owner: AbstractFactory,
    val origin: FqName?
) : Node

class AssistedProvisionBindingNode(
    key: Key,
    dependencies: List<BindingRequest>,
    targetScope: FqName?,
    scoped: Boolean,
    module: ModuleNode?,
    owner: AbstractFactory,
    origin: FqName?,
    val provider: IrClass,
    val typeArguments: List<IrType>
) : BindingNode(key, dependencies, targetScope, scoped, module, owner, origin)

class ChildFactoryBindingNode(
    key: Key,
    owner: ImplFactory,
    origin: FqName?,
    val childImplFactory: ImplFactory,
    val childFactory: IrClass
) : BindingNode(
    key, listOf(
        BindingRequest(
            childImplFactory.parent!!.clazz.defaultType
                .asKey(),
            null
        )
    ),
    null, false, null, owner, origin
)

class DelegateBindingNode(
    key: Key,
    owner: AbstractFactory,
    origin: FqName?,
    val originalKey: Key,
    val requestOrigin: FqName
) : BindingNode(
    key, listOf(
        BindingRequest(originalKey, requestOrigin)
    ), null, false, null, owner, origin
)

class DependencyBindingNode(
    key: Key,
    owner: AbstractFactory,
    origin: FqName?,
    val provider: IrClass,
    val requirementNode: DependencyNode
) : BindingNode(key, emptyList(), null, false, null, owner, origin)

class FactoryImplementationBindingNode(
    val factoryImplementationNode: FactoryImplementationNode,
) : BindingNode(
    factoryImplementationNode.key,
    emptyList(),
    null,
    false,
    null,
    factoryImplementationNode.implFactory,
    factoryImplementationNode.key.type.getClass()!!.fqNameForIrSerialization
)

class InstanceBindingNode(
    key: Key,
    owner: AbstractFactory,
    origin: FqName?,
    val requirementNode: InstanceNode,
) : BindingNode(key, emptyList(), null, false, null, owner, origin)

class LazyBindingNode(
    key: Key,
    origin: FqName?,
    owner: AbstractFactory
) : BindingNode(
    key,
    listOf(
        BindingRequest(
            key = key.type.typeArguments.single().asKey(),
            origin
        )
    ),
    null,
    false,
    null,
    owner,
    origin
)

class MapBindingNode(
    key: Key,
    owner: AbstractFactory,
    origin: FqName?,
    val entries: Map<MapKey, BindingRequest>
) : BindingNode(key, entries.values.toList(), null, false, null, owner, origin) {
    val keyKey = key.type.typeArguments[0].asKey()
    val valueKey = key.type.typeArguments[1].asKey()
}

class MembersInjectorBindingNode(
    key: Key,
    owner: AbstractFactory,
    origin: FqName?,
    val membersInjector: IrClass
) : BindingNode(
    key,
    membersInjector.constructors.singleOrNull()
        ?.valueParameters
        ?.map { it.type.typeArguments.single() }
        ?.map { BindingRequest(it.asKey(), null) }
        ?: emptyList(),
    null,
    false,
    null,
    owner,
    origin
)

class NullBindingNode(
    key: Key,
    owner: AbstractFactory
) : BindingNode(
    key,
    emptyList(),
    null,
    false,
    null,
    owner,
    null
)

class ProviderBindingNode(
    key: Key,
    owner: AbstractFactory,
    origin: FqName?
) : BindingNode(
    key,
    listOf(BindingRequest(key.type.typeArguments.single().asKey(), origin)),
    null,
    false,
    null,
    owner,
    origin
)

class ProvisionBindingNode(
    key: Key,
    dependencies: List<BindingRequest>,
    targetScope: FqName?,
    scoped: Boolean,
    module: ModuleNode?,
    owner: AbstractFactory,
    origin: FqName?,
    val provider: IrClass,
    val typeArguments: List<IrType>
) : BindingNode(key, dependencies, targetScope, scoped, module, owner, origin)

class SetBindingNode(
    key: Key,
    owner: AbstractFactory,
    origin: FqName?,
    val elements: Set<BindingRequest>
) : BindingNode(key, elements.toList(), null, false, null, owner, origin) {
    val elementKey = key.type.typeArguments.single().asKey()
}

fun IrType.asKey(): Key {
    return Key(this)
}

class Key(val type: IrType) {

    init {
        check(type !is IrErrorType) {
            "Cannot be error type ${type.render()}"
        }
        check(!type.isTypeParameter()) {
            "Must be concrete type ${type.render()}"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Key

        if (!type.equalsForKey(other.type)) return false

        return true
    }

    override fun hashCode(): Int {
        return type.hashCodeForKey()
    }

    override fun toString(): String = type.render()

    private fun IrType.hashCodeForKey(): Int {
        var result = classifierOrNull?.hashCode() ?: 0
        result = 31 * result + qualifiersHash()
        result = 32 * result + typeArguments.map { it.hashCodeForKey() }.hashCode()
        return result
    }

    private fun IrType.equalsForKey(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IrSimpleType) return false
        this as IrSimpleType
        if (!FqNameEqualityChecker.areEqual(classifier, other.classifier)) return false
        if (!getQualifiers().qualifiersEquals(other.getQualifiers())) return false

        if (arguments.size != arguments.size) return false

        arguments
            .forEachIndexed { index, argument ->
                if (!argument.type.equalsForKey(other.arguments[index].type)) {
                    return false
                }
            }

        return true
    }

    private fun List<IrConstructorCall>.qualifiersEquals(other: List<IrConstructorCall>): Boolean {
        if (size != other.size) return false
        for (i in indices) {
            val thisAnnotation = this[i].toAnnotationDescriptor()
            val otherAnnotation = other[i].toAnnotationDescriptor()
            if (thisAnnotation.fqName != otherAnnotation.fqName) return false
            val thisValues = thisAnnotation.allValueArguments.entries.toList()
            val otherValues = otherAnnotation.allValueArguments.entries.toList()
            if (thisValues.size != otherValues.size) return false
            for (j in thisValues.indices) {
                val thisValue = thisValues[j]
                val otherValue = otherValues[j]
                if (thisValue.key != otherValue.key) return false
                if (thisValue.value.value != otherValue.value.value) return false
            }
        }

        return true
    }

    private fun IrType.qualifiersHash() = getQualifiers()
        .map { it.hash() }
        .hashCode()

    private fun IrConstructorCall.hash(): Int {
        var result = type.hashCode()
        result = 31 * result + toAnnotationDescriptor()
            .allValueArguments
            .map { it.key to it.value.value }
            .hashCode()
        return result
    }

}
