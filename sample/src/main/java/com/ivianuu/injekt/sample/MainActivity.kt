package com.ivianuu.injekt.sample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.injekt.*
import kotlin.reflect.KClass

class MainActivity : AppCompatActivity(), ComponentHolder {

    override val component by lazy {
        component {
            modules(mainActivityModule())
            dependencies((application as ComponentHolder).component)
        }
    }

    private val appDependency by inject<AppDependency>()
    private val mainActivityDependency by inject<MainActivityDependency>()

    private val services by injectProviderMap<KClass<out Service>, Service>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appDependency
        mainActivityDependency

        Log.d("testt", "services $services")
    }

}

fun MainActivity.mainActivityModule() = module {
    factory { this@mainActivityModule }
    single { MainActivityDependency(get(), get(), getSet(), getMap()) }
    factory { CommandThree() } intoSet setBinding<Command>()
    factory { ServiceThree() } intoMap mapBinding<Service>(ServiceThree::class)
}

class MainActivityDependency(
    val app: App,
    val mainActivity: MainActivity,
    val commands: Set<Command>,
    val services: Map<KClass<out Service>, Service>
)