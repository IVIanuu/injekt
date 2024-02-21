package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.*

data class PersistedCallableInfo(
  val type: PersistedType,
  val parameterTypes: Map<Int, PersistedType>
)

data class PersistedClassifierInfo(
  val tags: List<PersistedType>,
  val superTypes: List<PersistedType>
)

data class PersistedType(
  val classId: PersistedClassId,
  val arguments: List<PersistedTypeProjection>,
  val isNullable: Boolean,
)

data class PersistedTypeProjection(
  val type: PersistedType?,
  val kind: ProjectionKind,
)

data class PersistedClassId(
  val packageFqName: FqName,
  val relativeClassName: FqName,
  val local: Boolean
)

fun ClassId.toPersistedClassId() =
  PersistedClassId(packageFqName, relativeClassName, isLocal)

fun ConeKotlinType.toPersistedType(session: FirSession): PersistedType = PersistedType(
  classId = classId!!.toPersistedClassId(),
  arguments = typeArguments.map { it.toPersistedTypeProjection(session) },
  isNullable = isNullable
)

fun ConeTypeProjection.toPersistedTypeProjection(session: FirSession): PersistedTypeProjection = PersistedTypeProjection(
  type = type?.toPersistedType(session),
  kind = kind
)
