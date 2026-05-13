package com.xsyusign.mobile.util

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {

    private const val NAME = "hongchu_sign_prefs"
    private const val KEY_NOTIFICATION = "notification_enabled"
    private const val KEY_SHOW_GUIDE = "show_battery_guide"
    private const val KEY_FIRST_LAUNCH = "first_launch_done"
    private const val KEY_KEEP_ALIVE = "keep_alive_enabled"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun isNotificationEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_NOTIFICATION, true)

    fun setNotificationEnabled(ctx: Context, enabled: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_NOTIFICATION, enabled).apply()

    fun isShowGuide(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_SHOW_GUIDE, true)

    fun setShowGuide(ctx: Context, show: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_SHOW_GUIDE, show).apply()

    fun isKeepAliveEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_KEEP_ALIVE, false)

    fun setKeepAliveEnabled(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_KEEP_ALIVE, enabled).apply()
    }

    /** 首次启动返回 true，调用后标记为已启动 */
    fun isFirstLaunch(ctx: Context): Boolean {
        val p = prefs(ctx)
        if (p.getBoolean(KEY_FIRST_LAUNCH, false)) return false
        p.edit().putBoolean(KEY_FIRST_LAUNCH, true).apply()
        return true
    }
}
