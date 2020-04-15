package com.ivianuu.injekt.sample

import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.Qualifier

annotation class CoolMap {
    companion object : Qualifier
}

class MyModule(
    activity: MainActivity
) : Module({
    factory { activity }
    factory { Foo() }
    factory { Bar(get()) }
    map<String, String>(CoolMap) {

    }
}) {

    class _678(private val activity: MainActivity) : Provider<MainActivity> {
        override fun invoke(): MainActivity = activity
    }

    object _123 : Provider<Foo> {
        override fun invoke(): Foo = Foo()
    }

    class _345(
        private val _123Provider: Provider<Foo>
    ) : Provider<Bar> {
        override fun invoke(): Bar = Bar(_123Provider())
    }
}

class Foo

class Bar(foo: Foo)
