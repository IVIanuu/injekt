/*
 * Copyright 2021 Manuel Wrage
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

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.metadata.*
import org.jetbrains.kotlin.metadata.deserialization.Flags.*
import org.jetbrains.kotlin.metadata.serialization.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.serialization.*

class InfoDescriptorSerializationPlugin : DescriptorSerializerPlugin {
    private val hasAnnotationFlag = HAS_ANNOTATIONS.toFlags(true)
    override fun afterClass(
        descriptor: ClassDescriptor,
        proto: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable,
        childSerializer: DescriptorSerializer,
        extension: SerializerExtension
    ) {
        if (proto.flags and hasAnnotationFlag == 0) {
            val context = InjektContext(descriptor.module)
            if (descriptor.toClassifierRef(context, null)
                    .typeParameters
                    .any { it.isForTypeKey || it.isGivenConstraint } ||
                descriptor.unsubstitutedMemberScope
                    .getContributedDescriptors()
                    .any { it.isGiven(context, null) }) {
                proto.flags = proto.flags or hasAnnotationFlag
            }
        }
    }
}
