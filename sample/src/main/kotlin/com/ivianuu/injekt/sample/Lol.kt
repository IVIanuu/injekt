package com.ivianuu.injekt.sample

import android.content.Context
import com.ivianuu.injekt.Assisted
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.internal.InjektAst
import com.ivianuu.injekt.internal.injektIntrinsic
import com.ivianuu.injekt.map
import com.ivianuu.injekt.transient
import kotlin.reflect.KClass

@Scope
annotation class ApplicationScoped

@Scope
annotation class RetainedActivityScoped

@Target(AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
@Qualifier
annotation class ForApplication

abstract class Worker(
    context: Context,
    workerParameters: WorkerParameters
)

interface WorkerParameters

abstract class WorkerFactory {
    abstract fun create(
        className: String,
        context: Context,
        workerParameters: WorkerParameters
    ): Worker
}

class InjektWorkerFactory(
    @Workers private val workers: Map<KClass<out Worker>, SingleWorkerFactory>
) : WorkerFactory() {
    override fun create(
        className: String,
        context: Context,
        workerParameters: WorkerParameters
    ): Worker {
        return workers.getValue(Class.forName(className).kotlin as KClass<out Worker>)(
            context,
            workerParameters
        )
    }
}

@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.EXPRESSION,
    AnnotationTarget.FUNCTION, AnnotationTarget.TYPE
)
@Retention(AnnotationRetention.SOURCE)
@Qualifier
private annotation class Workers

typealias SingleWorkerFactory = (Context, WorkerParameters) -> Worker

@Module
fun applicationModule() {
    instance("")
    @ForApplication
    transient { (context: Context, workerParameters: WorkerParameters) ->
        MyWorker(context, workerParameters, get())
    }
    @Workers
    map<KClass<out Worker>, SingleWorkerFactory> {
        put<(Context, WorkerParameters) -> MyWorker>(MyWorker::class)
    }
}

class applicationModuleImpl {
    val instance_0: String

    init {
        instance_0 = ""
    }

    @InjektAst.Module
    interface Descriptor {
        @InjektAst.Path.Field("instance_0")
        @InjektAst.Binding
        fun binding_0(): String

        @InjektAst.Path.Class<provider_0>
        @InjektAst.Binding
        fun binding_1(
            @Assisted p0: Context,
            @Assisted p1: WorkerParameters,
            repo: Repo
        ): @ForApplication MyWorker

        @InjektAst.Map
        fun map_0(): @Workers Map<String, SingleWorkerFactory>

        @InjektAst.Map.Entry
        fun map_0_entry_0(
            map: @Workers Map<String, SingleWorkerFactory>,
            @InjektAst.Map.ClassKey(MyWorker::class)
            entry: (Context, WorkerParameters) -> MyWorker
        )
    }

    class provider_0 : Provider<MyWorker> {
        override fun invoke(): MyWorker {
            return injektIntrinsic()
        }
    }
}

@InjektAst.ChildFactory
interface activityComponentFactoryDescriptor {
    @InjektAst.ChildFactory.Type
    fun type(): (MainActivity) -> ActivityComponent

    @InjektAst.Module
    fun module(): activityComponentFactoryModule
}

class activityComponentFactoryModule(mainActivity: MainActivity) {
    val instance_0: MainActivity

    init {
        instance_0 = mainActivity
    }

    @InjektAst.Module
    interface Descriptor {
        @InjektAst.Binding
        fun provide_0(): MainActivity
    }
}
