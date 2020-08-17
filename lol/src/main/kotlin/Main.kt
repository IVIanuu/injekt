package com.ivianuu.injekt.lol

import com.ivianuu.injekt.Context
import com.ivianuu.injekt.InitializeInjekt
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.childContext
import com.ivianuu.injekt.given
import com.ivianuu.injekt.rootContext
import com.ivianuu.injekt.runReader

@Context
interface TestContext

@InitializeInjekt
fun main() {
    rootContext<TestContext>().runReader {
        withString("string") {
            withInt(0) {
                withLong(0L) {
                    withBoolean(true) {
                        listOf(given<String>(), given<Int>(), given<Long>(), given<Boolean>())
                    }
                }
            }
        }
    }
}

@Context
interface StringContext
@Reader
private fun <R> withString(
    value: String,
    block: @Reader () -> R
) = childContext<StringContext>(value).runReader(block = block)

@Context
interface IntContext
@Reader
private fun <R> withInt(
    value: Int,
    block: @Reader () -> R
) = childContext<IntContext>(value).runReader(block = block)

@Context
interface LongContext
@Reader
private fun <R> withLong(
    value: Long,
    block: @Reader () -> R
) = childContext<LongContext>(value).runReader(block = block)

@Context
interface BooleanContext
@Reader
private fun <R> withBoolean(
    value: Boolean,
    block: @Reader () -> R
) = childContext<BooleanContext>(value).runReader(block = block)