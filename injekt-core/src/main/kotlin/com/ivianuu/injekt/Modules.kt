@file:Suppress("EXPERIMENTAL_FEATURE_WARNING", "NOTHING_TO_INLINE")

package com.ivianuu.injekt

fun <A : B, B> alias(): @Binding (A) -> B = { it }

@Suppress("UNCHECKED_CAST")
fun <M : Map<*, *>, K, T> mapEntries(key: () -> K): @MapEntries (T) -> M = { mapOf(key to it) as M }

@Suppress("UNCHECKED_CAST")
fun <S : Set<*>, T> setElements(): @SetElements (T) -> S = { setOf(it) as S }

data class Module1<out A>(@Module val a: A)

inline fun <A> moduleOf(a: A): Module1<A> {
    return Module1(a)
}

data class Module2<out A, out B>(@Module val a: A, @Module val b: B)

inline fun <A, B> moduleOf(a: A, b: B): Module2<A, B> {
    return Module2(a, b)
}

data class Module3<out A, out B, out C>(@Module val a: A, @Module val b: B, @Module val c: C)

inline fun <A, B, C> moduleOf(a: A, b: B, c: C): Module3<A, B, C> {
    return Module3(a, b, c)
}

data class Module4<out A, out B, out C, out D>(
    @Module val a: A,
    @Module val b: B,
    @Module val c: C,
    @Module val d: D,
)

inline fun <A, B, C, D> moduleOf(a: A, b: B, c: C, d: D): Module4<A, B, C, D> {
    return Module4(a, b, c, d)
}

data class Module5<out A, out B, out C, out D, out E>(
    @Module val a: A,
    @Module val b: B,
    @Module val c: C,
    @Module val d: D,
    @Module val e: E,
)

inline fun <A, B, C, D, E> moduleOf(a: A, b: B, c: C, d: D, e: E): Module5<A, B, C, D, E> {
    return Module5(a, b, c, d, e)
}

data class Module6<out A, out B, out C, out D, out E, out F>(
    @Module val a: A,
    @Module val b: B,
    @Module val c: C,
    @Module val d: D,
    @Module val e: E,
    @Module val f: F,
)

inline fun <A, B, C, D, E, F> moduleOf(
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F,
): Module6<A, B, C, D, E, F> {
    return Module6(a, b, c, d, e, f)
}

data class Module7<out A, out B, out C, out D, out E, out F, out G>(
    @Module val a: A,
    @Module val b: B,
    @Module val c: C,
    @Module val d: D,
    @Module val e: E,
    @Module val f: F,
    @Module val g: G,
)

inline fun <A, B, C, D, E, F, G> moduleOf(
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F,
    g: G,
): Module7<A, B, C, D, E, F, G> {
    return Module7(a, b, c, d, e, f, g)
}

data class Module8<out A, out B, out C, out D, out E, out F, out G, out H>(
    @Module val a: A,
    @Module val b: B,
    @Module val c: C,
    @Module val d: D,
    @Module val e: E,
    @Module val f: F,
    @Module val g: G,
    @Module val h: H,
)

inline fun <A, B, C, D, E, F, G, H> moduleOf(
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F,
    g: G,
    h: H,
): Module8<A, B, C, D, E, F, G, H> {
    return Module8(a, b, c, d, e, f, g, h)
}

data class Module9<out A, out B, out C, out D, out E, out F, out G, out H, out I>(
    @Module val a: A,
    @Module val b: B,
    @Module val c: C,
    @Module val d: D,
    @Module val e: E,
    @Module val f: F,
    @Module val g: G,
    @Module val h: H,
    @Module val i: I,
)

inline fun <A, B, C, D, E, F, G, H, I> moduleOf(
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F,
    g: G,
    h: H,
    i: I,
): Module9<A, B, C, D, E, F, G, H, I> {
    return Module9(a, b, c, d, e, f, g, h, i)
}

data class Module10<out A, out B, out C, out D, out E, out F, out G, out H, out I, out J>(
    @Module val a: A,
    @Module val b: B,
    @Module val c: C,
    @Module val d: D,
    @Module val e: E,
    @Module val f: F,
    @Module val g: G,
    @Module val h: H,
    @Module val i: I,
    @Module val j: J,
)

inline fun <A, B, C, D, E, F, G, H, I, J> moduleOf(
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F,
    g: G,
    h: H,
    i: I,
    j: J,
): Module10<A, B, C, D, E, F, G, H, I, J> {
    return Module10(a, b, c, d, e, f, g, h, i, j)
}

data class Module11<out A, out B, out C, out D, out E, out F, out G, out H, out I, out J, out K>(
    @Module val a: A,
    @Module val b: B,
    @Module val c: C,
    @Module val d: D,
    @Module val e: E,
    @Module val f: F,
    @Module val g: G,
    @Module val h: H,
    @Module val i: I,
    @Module val j: J,
    @Module val k: K,
)

inline fun <A, B, C, D, E, F, G, H, I, J, K> moduleOf(
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F,
    g: G,
    h: H,
    i: I,
    j: J,
    k: K,
): Module11<A, B, C, D, E, F, G, H, I, J, K> {
    return Module11(a, b, c, d, e, f, g, h, i, j, k)
}

data class Module12<out A, out B, out C, out D, out E, out F, out G, out H, out I, out J, out K, out L>(
    @Module val a: A,
    @Module val b: B,
    @Module val c: C,
    @Module val d: D,
    @Module val e: E,
    @Module val f: F,
    @Module val g: G,
    @Module val h: H,
    @Module val i: I,
    @Module val j: J,
    @Module val k: K,
    @Module val l: L,
)

inline fun <A, B, C, D, E, F, G, H, I, J, K, L> moduleOf(
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F,
    g: G,
    h: H,
    i: I,
    j: J,
    k: K,
    l: L,
): Module12<A, B, C, D, E, F, G, H, I, J, K, L> {
    return Module12(a, b, c, d, e, f, g, h, i, j, k, l)
}

data class Module13<out A, out B, out C, out D, out E, out F, out G, out H, out I, out J, out K, out L, out M>(
    @Module val a: A,
    @Module val b: B,
    @Module val c: C,
    @Module val d: D,
    @Module val e: E,
    @Module val f: F,
    @Module val g: G,
    @Module val h: H,
    @Module val i: I,
    @Module val j: J,
    @Module val k: K,
    @Module val l: L,
    @Module val m: M,
)

inline fun <A, B, C, D, E, F, G, H, I, J, K, L, M> moduleOf(
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F,
    g: G,
    h: H,
    i: I,
    j: J,
    k: K,
    l: L,
    m: M,
): Module13<A, B, C, D, E, F, G, H, I, J, K, L, M> {
    return Module13(a, b, c, d, e, f, g, h, i, j, k, l, m)
}

data class Module14<out A, out B, out C, out D, out E, out F, out G, out H, out I, out J, out K, out L, out M, out N>(
    @Module val a: A,
    @Module val b: B,
    @Module val c: C,
    @Module val d: D,
    @Module val e: E,
    @Module val f: F,
    @Module val g: G,
    @Module val h: H,
    @Module val i: I,
    @Module val j: J,
    @Module val k: K,
    @Module val l: L,
    @Module val m: M,
    @Module val n: N,
)

inline fun <A, B, C, D, E, F, G, H, I, J, K, L, M, N> moduleOf(
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F,
    g: G,
    h: H,
    i: I,
    j: J,
    k: K,
    l: L,
    m: M,
    n: N,
): Module14<A, B, C, D, E, F, G, H, I, J, K, L, M, N> {
    return Module14(a, b, c, d, e, f, g, h, i, j, k, l, m, n)
}

data class Module15<out A, out B, out C, out D, out E, out F, out G, out H, out I, out J, out K, out L, out M, out N, out O>(
    @Module val a: A,
    @Module val b: B,
    @Module val c: C,
    @Module val d: D,
    @Module val e: E,
    @Module val f: F,
    @Module val g: G,
    @Module val h: H,
    @Module val i: I,
    @Module val j: J,
    @Module val k: K,
    @Module val l: L,
    @Module val m: M,
    @Module val n: N,
    @Module val o: O,
)

inline fun <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O> moduleOf(
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F,
    g: G,
    h: H,
    i: I,
    j: J,
    k: K,
    l: L,
    m: M,
    n: N,
    o: O,
): Module15<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O> {
    return Module15(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o)
}

data class Module16<out A, out B, out C, out D, out E, out F, out G, out H, out I, out J, out K, out L, out M, out N, out O, out P>(
    @Module val a: A,
    @Module val b: B,
    @Module val c: C,
    @Module val d: D,
    @Module val e: E,
    @Module val f: F,
    @Module val g: G,
    @Module val h: H,
    @Module val i: I,
    @Module val j: J,
    @Module val k: K,
    @Module val l: L,
    @Module val m: M,
    @Module val n: N,
    @Module val o: O,
    @Module val p: P,
)

inline fun <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P> moduleOf(
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F,
    g: G,
    h: H,
    i: I,
    j: J,
    k: K,
    l: L,
    m: M,
    n: N,
    o: O,
    p: P,
): Module16<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P> {
    return Module16(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p)
}

data class Module17<out A, out B, out C, out D, out E, out F, out G, out H, out I, out J, out K, out L, out M, out N, out O, out P, out Q>(
    @Module val a: A,
    @Module val b: B,
    @Module val c: C,
    @Module val d: D,
    @Module val e: E,
    @Module val f: F,
    @Module val g: G,
    @Module val h: H,
    @Module val i: I,
    @Module val j: J,
    @Module val k: K,
    @Module val l: L,
    @Module val m: M,
    @Module val n: N,
    @Module val o: O,
    @Module val p: P,
    @Module val q: Q,
)

inline fun <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q> moduleOf(
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F,
    g: G,
    h: H,
    i: I,
    j: J,
    k: K,
    l: L,
    m: M,
    n: N,
    o: O,
    p: P,
    q: Q,
): Module17<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q> {
    return Module17(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q)
}

data class Module18<out A, out B, out C, out D, out E, out F, out G, out H, out I, out J, out K, out L, out M, out N, out O, out P, out Q, out R>(
    @Module val a: A,
    @Module val b: B,
    @Module val c: C,
    @Module val d: D,
    @Module val e: E,
    @Module val f: F,
    @Module val g: G,
    @Module val h: H,
    @Module val i: I,
    @Module val j: J,
    @Module val k: K,
    @Module val l: L,
    @Module val m: M,
    @Module val n: N,
    @Module val o: O,
    @Module val p: P,
    @Module val q: Q,
    @Module val r: R,
)

inline fun <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R> moduleOf(
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F,
    g: G,
    h: H,
    i: I,
    j: J,
    k: K,
    l: L,
    m: M,
    n: N,
    o: O,
    p: P,
    q: Q,
    r: R,
): Module18<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R> {
    return Module18(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r)
}

data class Module19<out A, out B, out C, out D, out E, out F, out G, out H, out I, out J, out K, out L, out M, out N, out O, out P, out Q, out R, out S>(
    @Module val a: A,
    @Module val b: B,
    @Module val c: C,
    @Module val d: D,
    @Module val e: E,
    @Module val f: F,
    @Module val g: G,
    @Module val h: H,
    @Module val i: I,
    @Module val j: J,
    @Module val k: K,
    @Module val l: L,
    @Module val m: M,
    @Module val n: N,
    @Module val o: O,
    @Module val p: P,
    @Module val q: Q,
    @Module val r: R,
    @Module val s: S,
)

inline fun <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S> moduleOf(
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F,
    g: G,
    h: H,
    i: I,
    j: J,
    k: K,
    l: L,
    m: M,
    n: N,
    o: O,
    p: P,
    q: Q,
    r: R,
    s: S,
): Module19<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S> {
    return Module19(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s)
}

data class Module20<out A, out B, out C, out D, out E, out F, out G, out H, out I, out J, out K, out L, out M, out N, out O, out P, out Q, out R, out S, out T>(
    @Module val a: A,
    @Module val b: B,
    @Module val c: C,
    @Module val d: D,
    @Module val e: E,
    @Module val f: F,
    @Module val g: G,
    @Module val h: H,
    @Module val i: I,
    @Module val j: J,
    @Module val k: K,
    @Module val l: L,
    @Module val m: M,
    @Module val n: N,
    @Module val o: O,
    @Module val p: P,
    @Module val q: Q,
    @Module val r: R,
    @Module val s: S,
    @Module val t: T,
)

inline fun <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T> moduleOf(
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F,
    g: G,
    h: H,
    i: I,
    j: J,
    k: K,
    l: L,
    m: M,
    n: N,
    o: O,
    p: P,
    q: Q,
    r: R,
    s: S,
    t: T,
): Module20<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T> {
    return Module20(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t)
}

data class Module21<out A, out B, out C, out D, out E, out F, out G, out H, out I, out J, out K, out L, out M, out N, out O, out P, out Q, out R, out S, out T, out U>(
    @Module val a: A,
    @Module val b: B,
    @Module val c: C,
    @Module val d: D,
    @Module val e: E,
    @Module val f: F,
    @Module val g: G,
    @Module val h: H,
    @Module val i: I,
    @Module val j: J,
    @Module val k: K,
    @Module val l: L,
    @Module val m: M,
    @Module val n: N,
    @Module val o: O,
    @Module val p: P,
    @Module val q: Q,
    @Module val r: R,
    @Module val s: S,
    @Module val t: T,
    @Module val u: U,
)

inline fun <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U> moduleOf(
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F,
    g: G,
    h: H,
    i: I,
    j: J,
    k: K,
    l: L,
    m: M,
    n: N,
    o: O,
    p: P,
    q: Q,
    r: R,
    s: S,
    t: T,
    u: U,
): Module21<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U> {
    return Module21(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u)
}
