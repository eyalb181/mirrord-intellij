package com.metalbear.mirrord

import com.intellij.notification.NotificationType
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent

class MirrordActiveConfigWatch(private val service: MirrordProjectService) : AsyncFileListener {
    override fun prepareChange(events: MutableList<out VFileEvent>): AsyncFileListener.ChangeApplier? {
        val notification = service
            .activeConfig
            ?.url
            ?.let { activeUrl ->
                events
                    .filter { it.file?.url == activeUrl }
                    .firstNotNullOfOrNull {
                        when (it) {
                            is VFileDeleteEvent -> service.notifier.notification(
                                "mirrord active config has been removed",
                                NotificationType.WARNING
                            )
                                .withDontShowAgain(MirrordSettingsState.NotificationId.ACTIVE_CONFIG_REMOVED)

                            is VFileMoveEvent -> service.notifier.notification(
                                "mirrord active config has been moved to ${it.newParent.presentableUrl}",
                                NotificationType.WARNING
                            )
                                .withDontShowAgain(MirrordSettingsState.NotificationId.ACTIVE_CONFIG_REMOVED)

                            else -> null
                        }
                    }
            }

        return object : AsyncFileListener.ChangeApplier {
            override fun afterVfsChange() {
                notification?.let {
                    service.activeConfig = null
                    it.fire()
                    return
                }

                service.activeConfig?.let {
                    if (!it.isValid) {
                        service.activeConfig = null
                        service.notifier.notification(
                            "directory containing mirrord active config has been moved or removed",
                            NotificationType.WARNING
                        )
                            .withDontShowAgain(MirrordSettingsState.NotificationId.ACTIVE_CONFIG_REMOVED)
                            .fire()
                    }
                }
            }
        }
    }
}