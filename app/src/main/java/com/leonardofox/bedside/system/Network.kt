package com.leonardofox.bedside.system

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent

data class WifiNet(val ssid: String, val rssi: Int, val secured: Boolean, val level: Int)

object WifiShell {
    fun isEnabled(ctx: Context): Boolean {
        val viaManager = runCatching {
            ctx.getSystemService(android.net.wifi.WifiManager::class.java).isWifiEnabled
        }.getOrDefault(false)
        if (viaManager) return true
        return RootShell.exec("cmd wifi status")?.contains("Wifi is enabled") == true
    }

    fun connectedSsid(ctx: Context): String? {
        val viaManager = runCatching {
            ctx.getSystemService(android.net.wifi.WifiManager::class.java).connectionInfo?.ssid
                ?.trim('"')
        }.getOrNull()
        if (viaManager != null && viaManager.isNotBlank() && viaManager != "<unknown ssid>") return viaManager
        val out = RootShell.exec("cmd wifi status") ?: return null
        return Regex("connected to \"(.+?)\"").find(out)?.groupValues?.get(1)
    }

    fun connectedIp(): String? {
        val out = RootShell.exec("cmd wifi status") ?: return null
        return Regex("IP: /([0-9.]+)").find(out)?.groupValues?.get(1)
    }

    fun connectedRssi(): Int? {
        val out = RootShell.exec("cmd wifi status") ?: return null
        return Regex("RSSI: (-?\\d+)").find(out)?.groupValues?.get(1)?.toIntOrNull()
    }

    fun setEnabled(ctx: Context, on: Boolean) {
        if (RootShell.isRoot()) {
            RootShell.exec("cmd wifi set-wifi-enabled " + (if (on) "enabled" else "disabled"))
        } else {
            runCatching {
                ctx.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    fun triggerScan() {
        RootShell.exec("cmd wifi start-scan")
    }

    fun scan(ctx: Context): List<WifiNet> {
        val viaRoot = scanByRoot()
        if (viaRoot.isNotEmpty()) return viaRoot
        val results = runCatching {
            ctx.getSystemService(android.net.wifi.WifiManager::class.java).scanResults
        }.getOrNull() ?: return emptyList()
        return results.map { r ->
            val level = android.net.wifi.WifiManager.calculateSignalLevel(r.level, 4)
            WifiNet(
                ssid = r.SSID ?: "",
                rssi = r.level,
                secured = r.capabilities.contains("WPA") || r.capabilities.contains("WEP") || r.capabilities.contains("EAP"),
                level = level
            )
        }.filter { it.ssid.isNotBlank() }.distinctBy { it.ssid }.sortedByDescending { it.rssi }
    }

    private fun scanByRoot(): List<WifiNet> {
        val out = RootShell.exec("cmd wifi list-scan-results") ?: return emptyList()
        val re = Regex("^\\s*[0-9a-fA-F:]{17}\\s+\\d+\\s+(-?\\d+)\\([^)]*\\)\\s+\\S+\\s+(.*?)(\\s+\\[.*)?$")
        val list = ArrayList<WifiNet>()
        out.lineSequence().forEach { line ->
            val m = re.find(line) ?: return@forEach
            val rssi = m.groupValues[1].toIntOrNull() ?: -100
            val ssid = m.groupValues[2].trim()
            if (ssid.isEmpty()) return@forEach
            val flags = m.groupValues[3].trim()
            val secured = flags.contains("WPA") || flags.contains("RSN") || flags.contains("WEP")
            val level = when {
                rssi >= -55 -> 4
                rssi >= -67 -> 3
                rssi >= -78 -> 2
                rssi >= -88 -> 1
                else -> 0
            }
            list.add(WifiNet(ssid = ssid, rssi = rssi, secured = secured, level = level))
        }
        return list.distinctBy { it.ssid }.sortedByDescending { it.rssi }
    }

    fun connect(ssid: String, password: String?, secured: Boolean): String? {
        val safeSsid = ssid.replace("\"", "\\\"")
        val sec = if (secured) "wpa2" else "open"
        val cmd = if (!secured || password.isNullOrEmpty()) {
            "cmd wifi connect-network \"" + safeSsid + "\" " + sec
        } else {
            val safePass = password.replace("\"", "\\\"")
            "cmd wifi connect-network \"" + safeSsid + "\" " + sec + " \"" + safePass + "\""
        }
        return RootShell.exec(cmd, 15)
    }

    fun forget(ssid: String) {
        RootShell.exec("cmd wifi forget-network \"" + ssid + "\"")
    }
}

object BtShell {
    fun isEnabled(): Boolean =
        runCatching { BluetoothAdapter.getDefaultAdapter()?.isEnabled == true }.getOrDefault(false)

    fun enableRaw(on: Boolean): Boolean = runCatching { BluetoothAdapter.getDefaultAdapter()?.enable() == true }.getOrDefault(false)

    fun setEnabled(ctx: Context, on: Boolean) {
        if (RootShell.exec("cmd bluetooth_manager " + (if (on) "enable" else "disable")) != null) return
        runCatching {
            ctx.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    data class BtDevice(val name: String, val mac: String)

    fun bonded(): List<BtDevice> = runCatching {
        BluetoothAdapter.getDefaultAdapter()?.bondedDevices
            ?.map { BtDevice(it.name ?: "未知设备", it.address) }
            ?: emptyList()
    }.getOrElse { emptyList() }

    fun connect(ctx: Context, mac: String): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
        val device = runCatching { adapter.getRemoteDevice(mac) }.getOrNull() ?: return false
        var started = false
        val listener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                runCatching {
                    val m = proxy.javaClass.getMethod("connect", BluetoothDevice::class.java)
                    m.invoke(proxy, device)
                    started = true
                }
            }

            override fun onServiceDisconnected(profile: Int) {}
        }
        runCatching { adapter.getProfileProxy(ctx, listener, BluetoothProfile.A2DP) }
        return started
    }

    fun bond(mac: String): Boolean = runCatching {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
        val device = adapter.getRemoteDevice(mac)
        device.createBond()
    }.getOrDefault(false)
}
