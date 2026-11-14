package com.leonardofox.bedside.data

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AlarmItem(val id: Int, val hour: Int, val minute: Int, val enabled: Boolean)

data class AppEntry(val pkg: String, val label: String, val icon: Drawable?)

data class Weather(
    val temp: Int,
    val desc: String,
    val high: Int,
    val low: Int,
    val updatedAt: Long
)

object Prefs {
    private var sp: SharedPreferences? = null

    val brightness = MutableStateFlow(0.72f)
    val autoBrightness = MutableStateFlow(false)
    // 0=全部隐藏 1=仅导航栏 2=仅状态栏 3=全部显示
    val barsMode = MutableStateFlow(1)
    val onboarded = MutableStateFlow(false)
    val privacyAgreed = MutableStateFlow(false)
    val forceLandscape = MutableStateFlow(false)
    val volume = MutableStateFlow(0.55f)
    val darkTheme = MutableStateFlow(true)
    val eyeCare = MutableStateFlow(false)
    val ultraDim = MutableStateFlow(false)
    val readingMode = MutableStateFlow(false)
    val shortcuts = MutableStateFlow<List<String>>(emptyList())
    val alarms = MutableStateFlow<List<AlarmItem>>(emptyList())
    val noisePreset = MutableStateFlow("white")
    val sleepMinutes = MutableStateFlow(0)
    val powerEnabled = MutableStateFlow(false)
    val powerOffTimes = MutableStateFlow(listOf("23:30"))
    val powerOnTimes = MutableStateFlow(listOf("06:30"))
    val recentLaunched = MutableStateFlow<List<String>>(emptyList())
    val favorites = MutableStateFlow<Set<String>>(emptySet())
    val weatherCity = MutableStateFlow("上海")
    val weatherLat = MutableStateFlow(31.2304)
    val weatherLon = MutableStateFlow(121.4737)
    val idleSeconds = MutableStateFlow(30)

    fun init(ctx: Context) {
        if (sp != null) return
        val p = ctx.applicationContext.getSharedPreferences("bedside", Context.MODE_PRIVATE)
        sp = p
        brightness.value = p.getFloat("brightness", 0.72f)
        autoBrightness.value = p.getBoolean("autoBrightness", false)
        barsMode.value = p.getInt("barsMode", 1)
        onboarded.value = p.getBoolean("onboarded", false)
        privacyAgreed.value = p.getBoolean("privacyAgreed", false)
        forceLandscape.value = p.getBoolean("forceLandscape", false)
        volume.value = p.getFloat("volume", 0.55f)
        darkTheme.value = p.getBoolean("darkTheme", true)
        eyeCare.value = p.getBoolean("eyeCare", false)
        ultraDim.value = p.getBoolean("ultraDim", false)
        readingMode.value = p.getBoolean("readingMode", false)
        shortcuts.value = p.getString("shortcuts", "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        noisePreset.value = p.getString("noisePreset", "white") ?: "white"
        sleepMinutes.value = p.getInt("sleepMinutes", 0)
        powerEnabled.value = p.getBoolean("powerEnabled", false)
        powerOffTimes.value = p.getString("powerOffTimes", "23:30")?.split(",")?.filter { it.isNotBlank() } ?: listOf("23:30")
        powerOnTimes.value = p.getString("powerOnTimes", "06:30")?.split(",")?.filter { it.isNotBlank() } ?: listOf("06:30")
        idleSeconds.value = p.getInt("idleSeconds", 30)
        alarms.value = decodeAlarms(p.getString("alarms", "") ?: "")
        recentLaunched.value = p.getString("recentLaunched", "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        favorites.value = p.getString("favorites", "")?.split(";;")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
        weatherCity.value = p.getString("weatherCity", "上海") ?: "上海"
        weatherLat.value = p.getFloat("weatherLat", 31.2304f).toDouble()
        weatherLon.value = p.getFloat("weatherLon", 121.4737f).toDouble()
    }

    private fun put(block: (SharedPreferences.Editor) -> Unit) {
        val p = sp ?: return
        block(p.edit().apply { })
    }

    fun saveAll() {
        val p = sp ?: return
        p.edit()
            .putFloat("brightness", brightness.value)
            .putBoolean("autoBrightness", autoBrightness.value)
            .putInt("barsMode", barsMode.value)
            .putBoolean("onboarded", onboarded.value)
            .putBoolean("privacyAgreed", privacyAgreed.value)
            .putBoolean("forceLandscape", forceLandscape.value)
            .putFloat("volume", volume.value)
            .putBoolean("darkTheme", darkTheme.value)
            .putBoolean("eyeCare", eyeCare.value)
            .putBoolean("ultraDim", ultraDim.value)
            .putBoolean("readingMode", readingMode.value)
            .putString("shortcuts", shortcuts.value.joinToString(","))
            .putString("noisePreset", noisePreset.value)
            .putInt("sleepMinutes", sleepMinutes.value)
            .putBoolean("powerEnabled", powerEnabled.value)
            .putString("powerOffTimes", powerOffTimes.value.joinToString(","))
            .putString("powerOnTimes", powerOnTimes.value.joinToString(","))
            .putInt("idleSeconds", idleSeconds.value)
            .putString("alarms", encodeAlarms(alarms.value))
            .putString("recentLaunched", recentLaunched.value.joinToString(","))
            .putString("favorites", favorites.value.joinToString(";;"))
            .putString("weatherCity", weatherCity.value)
            .putFloat("weatherLat", weatherLat.value.toFloat())
            .putFloat("weatherLon", weatherLon.value.toFloat())
            .apply()
    }

    fun setShortcuts(list: List<String>) {
        shortcuts.value = list
        saveAll()
    }

    fun noteLaunch(pkg: String) {
        val cur = recentLaunched.value.filter { it != pkg }
        recentLaunched.value = (listOf(pkg) + cur).take(12)
        saveAll()
    }

    fun toggleFavorite(key: String): Boolean {
        val cur = favorites.value
        val added = key !in cur
        favorites.value = if (added) cur + key else cur - key
        saveAll()
        return added
    }

    fun setAlarms(list: List<AlarmItem>) {
        alarms.value = list
        saveAll()
    }

    private fun encodeAlarms(list: List<AlarmItem>): String =
        list.joinToString(";") { it.id.toString() + "," + it.hour + "," + it.minute + "," + (if (it.enabled) 1 else 0) }

    private fun decodeAlarms(raw: String): List<AlarmItem> {
        if (raw.isBlank()) return emptyList()
        return raw.split(";").mapNotNull { part ->
            val f = part.split(",")
            if (f.size < 4) null else AlarmItem(f[0].toIntOrNull() ?: 0, f[1].toIntOrNull() ?: 7, f[2].toIntOrNull() ?: 0, f[3] == "1")
        }
    }
}

object AppRepository {
    private var cacheAt = 0L
    private var cache: List<AppEntry> = emptyList()
    val apps = MutableStateFlow<List<AppEntry>>(emptyList())

    fun load(ctx: Context, force: Boolean = false): List<AppEntry> {
        val now = SystemClock.elapsedRealtime()
        if (!force && cache.isNotEmpty() && now - cacheAt < 30_000) return cache
        val launcherApps = ctx.getSystemService(LauncherApps::class.java)
        val self = ctx.packageName
        val list = runCatching {
            launcherApps.getActivityList(null, Process.myUserHandle())
                .map { info ->
                    val ai = info.applicationInfo
                    AppEntry(ai.packageName, info.label?.toString() ?: ai.packageName, info.getBadgedIcon(0))
                }
                .filter { it.pkg != self }
                .distinctBy { it.pkg }
                .sortedBy { it.label.lowercase() }
        }.getOrElse { emptyList() }
        cache = list
        cacheAt = now
        apps.value = list
        return list
    }

    fun find(ctx: Context, pkg: String): AppEntry? = load(ctx).firstOrNull { it.pkg == pkg }

    fun launch(ctx: Context, pkg: String): Boolean {
        val lm = ctx.getSystemService(LauncherApps::class.java)
        val info = runCatching { lm.getActivityList(pkg, Process.myUserHandle()).firstOrNull() }.getOrNull() ?: return false
        return runCatching {
            lm.startMainActivity(info.componentName, Process.myUserHandle(), android.graphics.Rect(), null)
            Prefs.noteLaunch(pkg)
            true
        }.getOrDefault(false)
    }
}

object RecentsProvider {
    fun recent(ctx: Context, limit: Int = 8): List<AppEntry> {
        val all = AppRepository.load(ctx).associateBy { it.pkg }
        val usm = ctx.getSystemService(android.app.usage.UsageStatsManager::class.java)
        val now = System.currentTimeMillis()
        val fromStats = runCatching {
            usm.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, now - 3L * 24 * 3600 * 1000, now)
                ?.filter { it.lastTimeUsed > 0 }
                ?.sortedByDescending { it.lastTimeUsed }
                ?.map { it.packageName }
                ?: emptyList()
        }.getOrElse { emptyList() }
        val merged = (Prefs.recentLaunched.value + fromStats).distinct()
        return merged.mapNotNull { all[it] }.take(limit)
    }
}

object WeatherRepo {
    val state = MutableStateFlow<Weather?>(null)
    val attempted = MutableStateFlow(false)

    suspend fun refresh() = withContext(Dispatchers.IO) {
        attempted.value = true
        val lat = Prefs.weatherLat.value
        val lon = Prefs.weatherLon.value
        val url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon +
            "&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min&timezone=auto"
        runCatching {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val json = JSONObject(text)
            val cur = json.getJSONObject("current")
            val daily = json.getJSONObject("daily")
            val code = cur.optInt("weather_code", 0)
            val w = Weather(
                temp = Math.round(cur.optDouble("temperature_2m", 0.0)).toInt(),
                desc = describe(code),
                high = Math.round(daily.getJSONArray("temperature_2m_max").optDouble(0)).toInt(),
                low = Math.round(daily.getJSONArray("temperature_2m_min").optDouble(0)).toInt(),
                updatedAt = System.currentTimeMillis()
            )
            state.value = w
        }
        Unit
    }

    /** 按城市名查坐标（Open-Meteo Geocoding），成功则写入 Prefs。 */
    suspend fun setCity(name: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val q = java.net.URLEncoder.encode(name, "UTF-8")
            val url = "https://geocoding-api.open-meteo.com/v1/search?name=" + q + "&count=1&language=zh&format=json"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val results = JSONObject(text).optJSONArray("results") ?: return@runCatching false
            if (results.length() == 0) return@runCatching false
            val first = results.getJSONObject(0)
            Prefs.weatherCity.value = first.optString("name", name)
            Prefs.weatherLat.value = first.getDouble("latitude")
            Prefs.weatherLon.value = first.getDouble("longitude")
            Prefs.saveAll()
            refresh()
            true
        }.getOrDefault(false)
    }

    fun setCoords(title: String, lat: Double, lon: Double) {
        Prefs.weatherCity.value = title
        Prefs.weatherLat.value = lat
        Prefs.weatherLon.value = lon
        Prefs.saveAll()
    }

    private fun describe(code: Int): String = when (code) {
        0 -> "晴"
        1, 2 -> "多云"
        3 -> "阴"
        45, 48 -> "雾"
        51, 53, 55, 56, 57 -> "毛毛雨"
        61, 63, 65 -> "雨"
        66, 67 -> "冻雨"
        71, 73, 75, 77 -> "雪"
        80, 81, 82 -> "阵雨"
        85, 86 -> "阵雪"
        95, 96, 99 -> "雷雨"
        else -> "未知"
    }
}
