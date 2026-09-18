package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

enum class OemBrand(val displayName: String, val systemUiName: String) {
    XIAOMI("Xiaomi / Redmi / POCO", "MIUI / HyperOS"),
    OPPO("OPPO / Realme / OnePlus", "ColorOS / Realme UI / OxygenOS"),
    VIVO("Vivo / iQOO", "FuntouchOS / OriginOS"),
    SAMSUNG("Samsung", "One UI"),
    HUAWEI("Huawei / Honor", "EMUI / Magic UI"),
    GENERIC("Standard Android", "Stock Android")
}

data class OemGuidance(
    val brand: OemBrand,
    val autostartSteps: List<String>,
    val batterySaverSteps: List<String>,
    val lockRecentsSteps: List<String>
)

object OemWorkaroundHelper {
    private const val TAG = "OemWorkaroundHelper"

    fun detectOem(): OemBrand {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()

        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || brand.contains("poco") || manufacturer.contains("blackshark") -> OemBrand.XIAOMI
            manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> OemBrand.OPPO
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> OemBrand.VIVO
            manufacturer.contains("samsung") -> OemBrand.SAMSUNG
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> OemBrand.HUAWEI
            else -> OemBrand.GENERIC
        }
    }

    fun getGuidance(): OemGuidance {
        val brand = detectOem()
        return when (brand) {
            OemBrand.XIAOMI -> OemGuidance(
                brand = brand,
                autostartSteps = listOf(
                    "Open MIUI / HyperOS Security app or tap 'Open Autostart' below.",
                    "Find 'PipraPay Connect' and toggle the Autostart switch to ON.",
                    "Tap on PipraPay Connect and enable 'Allow background start'."
                ),
                batterySaverSteps = listOf(
                    "Tap 'Disable Battery Saver' below.",
                    "Select 'No restrictions' instead of 'Battery saver (recommended)'."
                ),
                lockRecentsSteps = listOf(
                    "Open Recent Apps tray (swipe up and hold).",
                    "Long-press or pull down on PipraPay Connect window.",
                    "Tap the Padlock icon to lock app in memory."
                )
            )
            OemBrand.OPPO -> OemGuidance(
                brand = brand,
                autostartSteps = listOf(
                    "Tap 'Open Autostart Settings' below.",
                    "Under Startup Manager / Auto-launch, enable 'PipraPay Connect'.",
                    "Enable 'Allow background activity'."
                ),
                batterySaverSteps = listOf(
                    "Tap 'Disable Battery Saver' below.",
                    "Set PipraPay Connect Battery usage to 'Allow Background Activity' & 'Don't Optimize'."
                ),
                lockRecentsSteps = listOf(
                    "Open Recent Apps tray.",
                    "Tap the 2-dots / 3-dots icon at top right of PipraPay Connect.",
                    "Tap 'Lock' to prevent system task killer from closing it."
                )
            )
            OemBrand.VIVO -> OemGuidance(
                brand = brand,
                autostartSteps = listOf(
                    "Tap 'Open Autostart Settings' below (iManager / Settings).",
                    "Select 'Background Power Consumption' or 'Autostart'.",
                    "Enable PipraPay Connect."
                ),
                batterySaverSteps = listOf(
                    "Tap 'Disable Battery Saver' below.",
                    "Set to 'High Background Power Consumption' or 'No Restrictions'."
                ),
                lockRecentsSteps = listOf(
                    "Open Recent Apps overview.",
                    "Slide down on the PipraPay Connect card.",
                    "Tap the Lock icon."
                )
            )
            OemBrand.SAMSUNG -> OemGuidance(
                brand = brand,
                autostartSteps = listOf(
                    "Open Device Care > Battery > Background usage limits.",
                    "Add PipraPay Connect to 'Never sleeping apps'."
                ),
                batterySaverSteps = listOf(
                    "Tap 'Disable Battery Saver' below.",
                    "Under Battery > App Battery Usage, select 'Unrestricted'."
                ),
                lockRecentsSteps = listOf(
                    "Open Recent Apps screen.",
                    "Tap the PipraPay Connect app icon above its preview.",
                    "Select 'Keep open' / 'Lock this app'."
                )
            )
            else -> OemGuidance(
                brand = brand,
                autostartSteps = listOf(
                    "Ensure background activity is permitted in App Info settings."
                ),
                batterySaverSteps = listOf(
                    "Tap 'Disable Battery Saver' below.",
                    "Select 'Unrestricted' battery access."
                ),
                lockRecentsSteps = listOf(
                    "Open Recent Apps screen.",
                    "Lock PipraPay Connect if supported by your launcher."
                )
            )
        }
    }

    /**
     * Attempts to launch manufacturer-specific autostart or startup manager intent.
     */
    fun openAutostartSettings(context: Context): Boolean {
        val intents = when (detectOem()) {
            OemBrand.XIAOMI -> listOf(
                Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
                Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT),
                Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.securityscan.MainActivity"))
            )
            OemBrand.OPPO -> listOf(
                Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
                Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
                Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
                Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.sysfloatwindow.FloatWindowListActivity"))
            )
            OemBrand.VIVO -> listOf(
                Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
                Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
                Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
                Intent().setComponent(ComponentName("com.vivo.abe", "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity"))
            )
            OemBrand.SAMSUNG -> listOf(
                Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
                Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")),
                Intent().setComponent(ComponentName("com.samsung.android.sm_cn", "com.samsung.android.sm.ui.battery.BatteryActivity"))
            )
            OemBrand.HUAWEI -> listOf(
                Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
                Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
                Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"))
            )
            else -> emptyList()
        }

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.d(TAG, "Intent failed: ${intent.component?.className}: ${e.message}")
            }
        }

        // Fallback to Application Details Settings
        return try {
            val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(appDetailsIntent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Fallback to app details failed: ${e.message}")
            false
        }
    }

    /**
     * Opens system battery optimization exemption settings.
     */
    fun openBatterySaverSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
