package com.ivianuu.injekt.sample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.injekt.*
import kotlin.reflect.KClass

class MainActivity : AppCompatActivity(), ComponentHolder {

    override val component by lazy {
        component {
            dependencies((application as ComponentHolder).component)
            modules(mainActivityModule())
        }
    }

    private val appDependency by inject<AppDependency>()
    private val mainActivityDependency by inject<MainActivityDependency>()

    private val commands by inject<MultiBindingSet<Command>>(COMMANDS)
    private val services by inject<MultiBindingMap<KClass<out Service>, Service>>(SERVICES)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appDependency
        mainActivityDependency

        Log.d("testt", "commands $commands")
        Log.d("testt", "services $services")
    }

}

fun MainActivity.mainActivityModule() = module {
    factory { this@mainActivityModule }
    single { MainActivityDependency(get(), get()) }
    factory { CommandThree() } intoSet COMMANDS
    factory { ServiceThree() } intoMap (SERVICES to ServiceThree::class)
}

class MainActivityDependency(
    val app: App,
    val mainActivity: MainActivity
)