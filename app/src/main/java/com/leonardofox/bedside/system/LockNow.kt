package com.leonardofox.bedside.system

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

class LockAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        var instance: LockAccessibilityService? = null
    }
}

object LockNow {
    private fun component(ctx: Context): ComponentName = ComponentName(ctx, LockAccessibilityService::class.java)

    fun isAccessibilityEnabled(ctx: Context): Boolean {
        val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val expected = component(ctx).flattenToString()
        val expectedShort = component(ctx).flattenToShortString()
        return enabled.split(":").any { it.equals(expected, true) || it.equals(expectedShort, true) }
    }

    fun openAccessibilitySettings(ctx: Context) {
        runCatching { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    /** 优先无障碍（免 Root），其次 Root 的 input keyevent。 */
    fun lock(ctx: Context): Boolean {
        LockAccessibilityService.instance?.let { svc ->
            if (svc.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)) return true
        }
        if (RootShell.isRoot()) {
            RootShell.exec("input keyevent 223")
            return true
        }
        return false
    }
}
