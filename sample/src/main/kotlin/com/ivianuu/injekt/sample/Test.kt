package com.ivianuu.injekt.sample

/**

fun ComponentDsl.apiModule() {
factory { get<Context>().packageName }
parent("application", Component("application", {}))
}

class apiModule_Impl(
val applicationComponent: Component
) {
class stringProvider(private val p1Provider: Provider<Context>) : Provider<String> {
override fun invoke(): String {
return p1Provider().packageName
}
}
}

fun main(context: Context) {
val myOtherComponent = Component("application") {
factory { 0 }
}
val myOtherComponent_ = main_myOtherComponent_Impl()
val myComponent = Component("my_component") {
parent("application", myOtherComponent)
instance(context)
factory { Foo() }
factory { Bar(get()) }
apiModule()
}
val myComponent_ = main_myComponent_Impl(myOtherComponent_, context)
}

class main_myOtherComponent_Impl : Component {
val provider0: Provider<Int> = Provider { 0 }
override fun <T> get(key: Int): T = error("")
}

@ComponentMetadata(
key = "my_component",
scopes = [],
parents = ["application"],
bindings = [
"android.content.Context",
"java.lang.String",
"long"
]
)
class main_myComponent_Impl(
private val myOtherComponent_Impl: Component,
context: Context
) : Component {
private val provider0: Provider<Context> = InstanceProvider(context)
private val provider1: Provider<String> = apiModule_Impl.stringProvider(provider0)
private val provider2: Provider<Long> = Provider { myOtherComponent_Impl.get(0) }
override fun <T> get(key: Int): T {
return when (key) {
0 -> provider0()
1 -> provider1()
2 -> provider2()
12, 13, 14, 15, 16, 17 -> myOtherComponent_Impl.get(key)
else -> error("Unexpected key $key")
} as T
    }

class fooProvider : Provider<Foo> {
override fun invoke(): Foo {

}
    }

class barProvider(private val fooProvider: Provider<Foo>) : Provider<Bar> {
override fun invoke(): Bar = Bar(fooProvider())
    }
}

class Foo

class Bar(foo: Foo)*/