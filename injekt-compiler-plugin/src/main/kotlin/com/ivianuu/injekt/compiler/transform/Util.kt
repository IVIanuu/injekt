package com.ivianuu.injekt.compiler.transform

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorVisitor
import org.jetbrains.kotlin.descriptors.FieldDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.builders.declarations.IrFieldBuilder
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedDeclarationDescriptor
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor

fun IrFieldBuilder.buildField(): IrField {
    val wrappedDescriptor = WrappedFieldDescriptor()
    return IrFieldImpl(
        startOffset, endOffset, origin,
        IrFieldSymbolImpl(wrappedDescriptor),
        name, type, visibility, isFinal, isExternal, isStatic, isFakeOverride
    ).also {
        it.metadata = metadata
        wrappedDescriptor.bind(it)
    }
}

inline fun buildField(builder: IrFieldBuilder.() -> Unit) =
    IrFieldBuilder().run {
        builder()
        buildField()
    }

inline fun IrDeclarationContainer.addField(builder: IrFieldBuilder.() -> Unit) =
    buildField(builder).also { field ->
        field.parent = this
        declarations.add(field)
    }

fun IrClass.addField(
    fieldName: Name,
    fieldType: IrType,
    fieldVisibility: Visibility = Visibilities.PRIVATE
): IrField =
    addField {
        name = fieldName
        type = fieldType
        visibility = fieldVisibility
    }

fun IrClass.addField(
    fieldName: String,
    fieldType: IrType,
    fieldVisibility: Visibility = Visibilities.PRIVATE
): IrField =
    addField(Name.identifier(fieldName), fieldType, fieldVisibility)

open class WrappedFieldDescriptor(
    annotations: Annotations = Annotations.EMPTY,
    private val sourceElement: SourceElement = SourceElement.NO_SOURCE
) : PropertyDescriptor, WrappedDeclarationDescriptor<IrField>(annotations) {
    override fun getModality() = if (owner.isFinal) Modality.FINAL else Modality.OPEN

    override fun setOverriddenDescriptors(overriddenDescriptors: MutableCollection<out CallableMemberDescriptor>) {
        TODO("not implemented")
    }

    override fun getKind() = CallableMemberDescriptor.Kind.SYNTHESIZED

    override fun getName() = owner.name

    override fun getSource() = sourceElement

    override fun hasSynthesizedParameterNames() = false

    override fun getOverriddenDescriptors(): MutableCollection<out PropertyDescriptor> =
        owner.overriddenSymbols.map { it.descriptor }.toMutableList()

    override fun copy(
        newOwner: DeclarationDescriptor?,
        modality: Modality?,
        visibility: Visibility?,
        kind: CallableMemberDescriptor.Kind?,
        copyOverrides: Boolean
    ): CallableMemberDescriptor {
        TODO("not implemented")
    }

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> = mutableListOf()

    override fun getCompileTimeInitializer(): ConstantValue<*>? = null

    override fun isSetterProjectedOut(): Boolean = false

    override fun getAccessors(): MutableList<PropertyAccessorDescriptor> = mutableListOf()

    override fun getTypeParameters(): List<TypeParameterDescriptor> = emptyList()

    override fun getVisibility() = owner.visibility

    override val setter: PropertySetterDescriptor? get() = null

    override fun getOriginal() = this

    override fun isExpect() = false

    override fun substitute(substitutor: TypeSubstitutor): PropertyDescriptor =
        throw UnsupportedOperationException("Wrapped descriptors SHOULD NOT be substituted")

    override fun isActual() = false

    override fun getReturnType() = owner.type.toKotlinType()

    override fun hasStableParameterNames(): Boolean = false

    override fun getType(): KotlinType = owner.type.toKotlinType()

    override fun isVar() = !owner.isFinal

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? =
        if (owner.isStatic) null else (owner.parentAsClass.thisReceiver?.descriptor as ReceiverParameterDescriptor)

    override fun isConst() = false

    override fun getContainingDeclaration() = (owner.parent as IrSymbolOwner).symbol.descriptor

    override fun isLateInit() = owner.correspondingPropertySymbol?.owner?.isLateinit ?: false

    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? =
        owner.correspondingPropertySymbol?.owner?.descriptor?.extensionReceiverParameter

    override fun isExternal() = owner.isExternal

    override fun <R : Any?, D : Any?> accept(
        visitor: DeclarationDescriptorVisitor<R, D>?,
        data: D
    ) =
        visitor!!.visitPropertyDescriptor(this, data)

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        visitor!!.visitPropertyDescriptor(this, null)
    }

    override val getter: PropertyGetterDescriptor? get() = null

    override fun newCopyBuilder(): CallableMemberDescriptor.CopyBuilder<out PropertyDescriptor> {
        TODO("not implemented")
    }

    override val isDelegated get() = false

    // Following functions are used in error reporting when rendering annotations on properties
    override fun getBackingField(): FieldDescriptor? = null // TODO
    override fun getDelegateField(): FieldDescriptor? = null // TODO

    override fun <V : Any?> getUserData(key: CallableDescriptor.UserDataKey<V>?): V? = null
}