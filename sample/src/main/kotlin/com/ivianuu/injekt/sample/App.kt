package com.ivianuu.injekt.sample

import android.app.Application
import android.content.Context
import androidx.fragment.app.Fragment
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.ProviderDefinition
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.component
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.internal.Alias
import com.ivianuu.injekt.internal.Binding
import com.ivianuu.injekt.internal.ClassKey
import com.ivianuu.injekt.internal.Include
import com.ivianuu.injekt.internal.MapDeclaration
import com.ivianuu.injekt.internal.MapEntry
import com.ivianuu.injekt.internal.ModuleDescriptor
import com.ivianuu.injekt.internal.Qualified
import com.ivianuu.injekt.internal.SetDeclaration
import com.ivianuu.injekt.internal.SetElement
import com.ivianuu.injekt.internal.StringKey
import com.ivianuu.injekt.map
import com.ivianuu.injekt.scope
import com.ivianuu.injekt.set
import kotlin.reflect.KClass

@Qualifier
annotation class ApplicationContext

@Target(
    AnnotationTarget.TYPE,
    AnnotationTarget.EXPRESSION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER
)
@Retention(AnnotationRetention.SOURCE)
@Qualifier
annotation class Initializers

@Target(
    AnnotationTarget.TYPE,
    AnnotationTarget.EXPRESSION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER
)
@Retention(AnnotationRetention.SOURCE)
@Qualifier
annotation class Services

@Scope
annotation class ApplicationScoped

@Scope
annotation class ActivityScoped

interface ApplicationComponent {
    val application: Application
    val packageName: String
}

interface ActivityComponent {
    val activity: MainActivity
}

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        val component = component<ApplicationComponent> {
            scope<ApplicationScoped>()
            application(this)
            fragment { MyFragment() }
        }


        val component2 = ApplicationComponentImpl(applicationImpl(this))
    }

}

@ApplicationScoped
class ApplicationComponentImpl(
    private val module_0: applicationImpl<App>
) : ApplicationComponent {
    override val application: Application
        get() = get_0()
    override val packageName: String
        get() = get_1()

    private fun get_0() = applicationImpl.provider_0.create(module_0)
    private fun get_1() = applicationImpl.provider_1.create(get_0())
}

class MyFragment : Fragment()

class fragmentImpl_123 : fragmentImpl<MyFragment>() {
    @ModuleDescriptor
    interface Descriptor : fragmentImpl.Descriptor<MyFragment>
    class provider_0 : Provider<MyFragment> {
        override fun invoke() = MyFragment()
    }
}

@Module
inline fun <reified T : Fragment> fragment(noinline definition: ProviderDefinition<T>) {
    factory(definition)
    map<KClass<out Fragment>, Fragment> {
        put<@ApplicationContext T>(T::class)
    }
}

abstract class fragmentImpl<T : Fragment> {
    @ModuleDescriptor
    interface Descriptor<T : Fragment> {
        @Binding
        fun provide_0(): T

        @MapDeclaration
        fun map_0(): Map<KClass<out Fragment>, Fragment>

        @MapEntry
        fun map_0_entry_0(
            map: Map<KClass<out Fragment>, Fragment>,
            @ClassKey<T> element: @Qualified<ApplicationContext> T
        )
    }
}

@Module
fun <T : Application> application(instance: T) {
    nested("hello")
    nestedTyped(0)
    instance(instance)
    factory { get<Application>().packageName!! }
    alias<T, Application>()
    alias<Application, @ApplicationContext Context>()
    @Initializers set<Any> {
        add<Application>()
    }
    @Services map<String, Any> {
        put<Application>("one")
    }
}

@Module
fun nested(captured: String) {
}

class nestedImpl(val captured: String) {
    @ModuleDescriptor
    interface Descriptor {
    }
}

@Module
fun <T> nestedTyped(captured: T) {

}

class nestedTypedImpl<T>(val captured: T) {
    @ModuleDescriptor
    interface Descriptor<T> {
    }
}

class applicationImpl<T : Application>(val instance: T) {
    val include_0 = nestedImpl("hello")
    val include_1 = nestedTypedImpl(0)

    @ModuleDescriptor
    interface Descriptor<T : Application> {
        @Include
        fun include_0(): nestedImpl.Descriptor
        @Include
        fun include_1(): nestedTypedImpl.Descriptor<Int>

        @Binding
        fun provide_0(): T

        @Binding
        fun provide_1(p0: Application): String

        @Alias
        fun alias_0(p0: T): Application

        @Alias
        fun alias_1(p0: Application): @Qualified<ApplicationContext> Context

        @SetDeclaration
        fun set_0(): @Qualified<Initializers> Set<Any>

        @SetElement
        fun set_0_element_0(
            set: @Qualified<Initializers> Set<Any>,
            element: Application
        )

        @MapDeclaration
        fun map_0(): @Qualified<Services> Map<String, Any>

        @MapEntry
        fun map_0_entry_0(
            map: @Qualified<Services> Map<String, Any>,
            @StringKey("one") element: Application
        )
    }

    class provider_0<T : Application>(
        private val module: applicationImpl<T>
    ) : Provider<T> {
        override fun invoke() = create(module)

        companion object {
            fun <T : Application> create(module: applicationImpl<T>) = module.instance
        }
    }

    class provider_1(
        private val p0: Provider<Application>
    ) : Provider<String> {
        override fun invoke() = create(p0())

        companion object {
            fun create(application: Application) = application.packageName!!
        }
    }

}
