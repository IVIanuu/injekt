package com.ivianuu.injekt

inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
    val size = size
    for (index in 0 until size) {
        val item = get(index)
        action(item)
    }
}

inline fun <T> List<T>.fastForEachIndexed(action: (Int, T) -> Unit) {
    val size = size
    for (index in 0 until size) {
        val item = get(index)
        action(index, item)
    }
}

inline fun <T> Array<T>.fastForEach(action: (T) -> Unit) {
    val size = size
    for (index in 0 until size) {
        val item = get(index)
        action(item)
    }
}

inline fun <T> Array<T>.fastForEachIndexed(action: (Int, T) -> Unit) {
    val size = size
    for (index in 0 until size) {
        val item = get(index)
        action(index, item)
    }
}
