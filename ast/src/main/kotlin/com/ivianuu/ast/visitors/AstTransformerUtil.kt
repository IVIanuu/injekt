/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.visitors

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstPureAbstractElement

fun <T : AstElement, D> T.transformSingle(transformer: AstTransformer<D>, data: D): T {
    return (this as AstPureAbstractElement).transform<T, D>(transformer, data).single
}

fun <T : AstElement, D> MutableList<T>.transformInplace(transformer: AstTransformer<D>, data: D) {
    val iterator = this.listIterator()
    while (iterator.hasNext()) {
        val next = iterator.next() as AstPureAbstractElement
        val result = next.transform<T, D>(transformer, data)
        if (result.isSingle) {
            iterator.set(result.single)
        } else {
            val resultIterator = result.elements.listIterator()
            if (!resultIterator.hasNext()) {
                iterator.remove()
            } else {
                iterator.set(resultIterator.next())
            }
            while (resultIterator.hasNext()) {
                iterator.add(resultIterator.next())
            }
        }

    }
}

fun <T : AstElement, D> MutableList<T?>.transformInplaceNullable(transformer: AstTransformer<D>, data: D) {
    val iterator = this.listIterator()
    while (iterator.hasNext()) {
        val next = iterator.next() as? AstPureAbstractElement
        val result = next?.transform<T, D>(transformer, data) ?: continue
        if (result.isSingle) {
            iterator.set(result.single)
        } else {
            val resultIterator = result.elements.listIterator()
            if (!resultIterator.hasNext()) {
                iterator.remove()
            } else {
                iterator.set(resultIterator.next())
            }
            while (resultIterator.hasNext()) {
                iterator.add(resultIterator.next())
            }
        }
    }
}

sealed class TransformData<out D> {
    class Data<D>(val value: D) : TransformData<D>()
    object Nothing : TransformData<kotlin.Nothing>()
}

inline fun <T : AstElement, D> MutableList<T>.transformInplace(
    transformer: AstTransformer<D>,
    dataProducer: (Int) -> TransformData<D>
) {
    val iterator = this.listIterator()
    var index = 0
    while (iterator.hasNext()) {
        val next = iterator.next() as AstPureAbstractElement
        val data = when (val data = dataProducer(index++)) {
            is TransformData.Data<D> -> data.value
            TransformData.Nothing -> continue
        }
        val result = next.transform<T, D>(transformer, data).single
        iterator.set(result)
    }
}

inline fun <T : AstElement, D> MutableList<T?>.transformInplaceNullable(
    transformer: AstTransformer<D>,
    dataProducer: (Int) -> TransformData<D>
) {
    val iterator = this.listIterator()
    var index = 0
    while (iterator.hasNext()) {
        val next = iterator.next() as? AstPureAbstractElement ?: continue
        val data = when (val data = dataProducer(index++)) {
            is TransformData.Data<D> -> data.value
            TransformData.Nothing -> continue
        }
        val result = next.transform<T, D>(transformer, data).single
        iterator.set(result)
    }
}
