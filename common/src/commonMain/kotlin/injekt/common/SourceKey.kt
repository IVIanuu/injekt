package injekt.common

import kotlin.jvm.*

/**
 * A key which is unique for each root call site
 */
@JvmInline value class SourceKey(val value: String)
