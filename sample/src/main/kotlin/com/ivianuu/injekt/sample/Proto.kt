package com.ivianuu.injekt.sample

import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.get
import com.ivianuu.injekt.internal.BindingMetadata
import com.ivianuu.injekt.internal.ComponentMetadata
import com.ivianuu.injekt.internal.ModuleMetadata
import com.ivianuu.injekt.internal.ProviderMetadata
import com.ivianuu.injekt.internal.SingleProvider
import com.ivianuu.injekt.parent
import com.ivianuu.injekt.single

class Foo
data class Bar(val foo: Foo)
data class Baz(val foo: Foo, val bar: Bar)
data class Qux(val foo: Foo, val bar: Bar, val baz: Baz)
data class Quux(val foo: Foo, val bar: Bar, val baz: Baz, val qux: Qux)

val parent = Component("parent") {
    factory { Foo() }
    factory { Bar(get()) }
}

@ModuleMetadata(
    scopes = [],
    parents = [],
    bindings = [
        "com.ivianuu.injekt.sample.parentImplModule.provider_0",
        "com.ivianuu.injekt.sample.parentImplModule.provider_1"
    ],
    includedModules = []
)
class parentImplModule {
    @BindingMetadata(qualifiers = [])
    @ProviderMetadata(isSingle = false)
    object provider_0 : Provider<Foo> {
        override fun invoke() = create()
        inline fun create() = Foo()
    }

    @BindingMetadata(qualifiers = [])
    @ProviderMetadata(isSingle = false)
    class provider_1(
        private val p0: Provider<Foo>
    ) : Provider<Bar> {
        override fun invoke() = create(p0())

        companion object {
            inline fun create(foo: Foo) = Bar(foo)
        }
    }
}

@ComponentMetadata(
    scopes = [],
    parents = [],
    bindings = [
        "com.ivianuu.injekt.sample.parentImplModule.provider_0",
        "com.ivianuu.injekt.sample.parentImplModule.provider_1"
    ]
)
class parentImpl : Component {
    override fun <T> get(key: Int): T {
        return when (key) {
            0 -> get_0() as T
            1 -> get_1() as T
            else -> super.get(key)
        }
    }

    inline fun get_0() = parentImplModule.provider_0.create()
    inline fun get_1() = parentImplModule.provider_1.create(get_0())
}

val child = Component("child") {
    parent("p", parent)
    single { Baz(get(), get()) }
    factory { Qux(get(), get(), get()) }
}

@ModuleMetadata(
    scopes = [],
    parents = ["com.ivianuu.injekt.sample.parentImpl"],
    bindings = [
        "com.ivianuu.injekt.sample.childImplModule.provider_0",
        "com.ivianuu.injekt.sample.childImplModule.provider_1"
    ],
    includedModules = []
)
class childImplModule {
    @BindingMetadata(qualifiers = [])
    @ProviderMetadata(isSingle = true)
    class provider_0(
        private val p0: Provider<Foo>,
        private val p1: Provider<Bar>
    ) : Provider<Baz> {
        override fun invoke() = create(p0(), p1())

        companion object {
            inline fun create(p0: Foo, p1: Bar) = Baz(p0, p1)
        }
    }

    @BindingMetadata(qualifiers = [])
    @ProviderMetadata(isSingle = false)
    class provider_1(
        private val p0: Provider<Foo>,
        private val p1: Provider<Bar>,
        private val p2: Provider<Baz>
    ) : Provider<Qux> {
        override fun invoke() = create(p0(), p1(), p2())

        companion object {
            inline fun create(p0: Foo, p1: Bar, p2: Baz) = Qux(p0, p1, p2)
        }
    }
}

@ComponentMetadata(
    scopes = [],
    parents = ["com.ivianuu.injekt.sample.parentImpl"],
    bindings = [
        "/provider_0",
        "com.ivianuu.injekt.sample.childImplModule.provider_1"
    ]
)
class childImpl : Component {
    val provider_0 = SingleProvider(
        childImplModule.provider_0(
            parentImplModule.provider_0,
            parentImplModule.provider_1(parentImplModule.provider_0)
        )
    )

    override fun <T> get(key: Int): T {
        return when (key) {
            0 -> get_0() as T
            1 -> get_1() as T
            2 -> get_2() as T
            3 -> get_3() as T
            else -> super.get(key)
        }
    }

    inline fun get_0() = parentImplModule.provider_0.create()
    inline fun get_1() = parentImplModule.provider_1.create(get_0())
    inline fun get_2() = provider_0()
    inline fun get_3() = childImplModule.provider_1.create(
        get_0(),
        get_1(),
        get_2()
    )
}

val bottom = Component("bottom") {
    bottomNestedModule()
    factory { Quux(get(), get(), get(), get()) }
}

@Module
fun bottomNestedModule() {
    parent("child", child)
}

@ModuleMetadata(
    scopes = [],
    parents = ["/parent_0"],
    bindings = [],
    includedModules = []
)
class bottomNestedModuleImpl {
    val parent_0 = child as childImpl
}

@ModuleMetadata(
    scopes = [],
    parents = [],
    bindings = ["com.ivianuu.injekt.sample.bottomImplModule.provider_0"],
    includedModules = ["/module_0"]
)
class bottomImplModule {
    val module_0 = bottomNestedModuleImpl()

    @BindingMetadata(qualifiers = [])
    @ProviderMetadata(isSingle = false)
    class provider_0(
        private val p0: Provider<Foo>,
        private val p1: Provider<Bar>,
        private val p2: Provider<Baz>,
        private val p3: Provider<Qux>
    ) : Provider<Quux> {
        override fun invoke() = create(p0(), p1(), p2(), p3())

        companion object {
            inline fun create(p0: Foo, p1: Bar, p2: Baz, p3: Qux) = Quux(p0, p1, p2, p3)
        }
    }
}

@ComponentMetadata(
    scopes = [],
    parents = ["/module_0/module_0/parent_0"],
    bindings = ["com.ivianuu.injekt.sample.bottomImplModule.provider_0"]
)
class bottomImpl : Component {
    val module_0 = bottomImplModule()

    override fun <T> get(key: Int): T {
        return when (key) {
            0 -> get_0() as T
            1 -> get_1() as T
            2 -> get_2() as T
            3 -> get_3() as T
            4 -> get_4() as T
            else -> super.get(key)
        }
    }

    inline fun get_0() = parentImplModule.provider_0.create()
    inline fun get_1() = parentImplModule.provider_1.create(get_0())
    inline fun get_2() = module_0.module_0.parent_0.get_2()
    inline fun get_3() = childImplModule.provider_1.create(
        get_0(),
        get_1(),
        get_2()
    )

    inline fun get_4() = bottomImplModule.provider_0.create(
        get_0(),
        get_1(),
        get_2(),
        get_3()
    )
}

fun main() {
    val c = bottomImpl()
    println(c.get<Quux>())
    println(
        Quux(
            Foo(),
            Bar(Foo()),
            c.module_0.module_0.parent_0.get_2(),
            Qux(Foo(), Bar(Foo()), c.module_0.module_0.parent_0.get_2())
        )
    )
}
