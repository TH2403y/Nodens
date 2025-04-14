package org.gitee.orryx.core.reload

import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.events.OrryxPluginReloadEvent
import org.gitee.orryx.api.interfaces.IReloadAPI
import org.gitee.orryx.utils.debug
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.function.info
import taboolib.common.util.unsafeLazy
import taboolib.library.reflex.ClassMethod
import taboolib.library.reflex.ReflexClass
import taboolib.module.chat.colored

@Awake
object ReloadAPI: IReloadAPI, ClassVisitor(3) {

    class ReloadFunction(val method: ClassMethod, val obj: Any, val weight: Int)

    override fun getLifeCycle(): LifeCycle {
        return LifeCycle.ENABLE
    }

    private val methodList by unsafeLazy { mutableListOf<ReloadFunction>() }

    override fun visit(method: ClassMethod, owner: ReflexClass) {
        if (method.isAnnotationPresent(Reload::class.java)) {
            methodList += ReloadFunction(
                method,
                owner.getInstance() ?: return,
                method.getAnnotation(Reload::class.java).property<Int>("weight")!!
            )
            debug("&e┣&7Reload loaded &e${method.owner.name}/${method.name} &a√")
        }
    }

    override fun reload() {
        val event = OrryxPluginReloadEvent()
        if (event.call()) {
            info("&e┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━".colored())
            Orryx.config.reload()
            debug = Orryx.config.getBoolean("Debug")
            val extensions = event.getFunctions()
            val weights = (methodList.map { it.weight } + extensions.map { it.weight }).distinct()
            weights.sorted().forEach { weight ->
                methodList.asSequence().filter { it.weight == weight }.forEach {
                    it.method.invoke(it.obj)
                }
                extensions.asSequence().filter { it.weight == weight }.forEach {
                    it.run()
                }
            }
            info("&e┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━".colored())
        }
    }

    @Awake(LifeCycle.CONST)
    fun init() {
        PlatformFactory.registerAPI<IReloadAPI>(ReloadAPI)
    }

}