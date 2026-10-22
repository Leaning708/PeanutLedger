package com.peanut.ledger.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

object LedgerRepository {
    private const val PREFS_NAME = "peanut_ledger"
    private const val DATA_KEY = "ledger_data_v1"

    fun load(context: Context): LedgerData {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(DATA_KEY, null) ?: return seed().also { save(context, it) }
        return runCatching { parseData(JSONObject(raw)) }
            .getOrElse { seed().also { save(context, it) } }
    }

    fun save(context: Context, data: LedgerData) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(DATA_KEY, data.toJson().toString())
            .apply()
    }

    private fun LedgerData.toJson(): JSONObject {
        val root = JSONObject()
        root.put("settings", settings.toJson())
        val daysObj = JSONObject()
        days.forEach { (key, day) -> daysObj.put(key, day.toJson()) }
        root.put("days", daysObj)
        return root
    }

    private fun LedgerSettings.toJson(): JSONObject = JSONObject()
        .put("buyTypes", buyTypes.optionsToJsonArray())
        .put("sellTypes", sellTypes.optionsToJsonArray())
        .put("buyers", buyers.optionsToJsonArray())
        .put("privacyMode", privacyMode)
        .put("fontScale", fontScale)
        .put("theme", theme)
        .put("defaultSettled", defaultSettled)
        .put("recordLocation", recordLocation)

    private fun DayData.toJson(): JSONObject = JSONObject()
        .put("remark", remark)
        .put("buys", buys.entriesToJsonArray())
        .put("sells", sells.entriesToJsonArray())

    private fun LedgerEntry.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("buyer", buyer)
        .put("remark", remark)
        .put("settled", settled)
        .put("adjust", adjust)
        .put("finalOverride", finalOverride ?: JSONObject.NULL)
        .put("createdAt", createdAt)
        .put("location", location?.let {
            JSONObject()
                .put("latitude", it.latitude)
                .put("longitude", it.longitude)
                .put("label", it.label ?: JSONObject.NULL)
        } ?: JSONObject.NULL)
        .put("types", types.typesToJsonArray())

    private fun LedgerType.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("items", items.itemsToJsonArray())

    private fun LedgerItem.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("weight", weight)
        .put("price", price)
        .put("manualAmount", manualAmount ?: JSONObject.NULL)

    private fun List<NamedOption>.optionsToJsonArray(): JSONArray = JSONArray().also { array ->
        forEach { option ->
            array.put(
                JSONObject()
                    .put("name", option.name)
                    .put("active", option.active)
            )
        }
    }

    private fun List<LedgerEntry>.entriesToJsonArray(): JSONArray = JSONArray().also { array ->
        forEach { array.put(it.toJson()) }
    }

    private fun List<LedgerType>.typesToJsonArray(): JSONArray = JSONArray().also { array ->
        forEach { array.put(it.toJson()) }
    }

    private fun List<LedgerItem>.itemsToJsonArray(): JSONArray = JSONArray().also { array ->
        forEach { array.put(it.toJson()) }
    }

    private fun parseData(root: JSONObject): LedgerData {
        val days = mutableMapOf<String, DayData>()
        val daysObj = root.optJSONObject("days")
        if (daysObj != null) {
            val keys = daysObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                daysObj.optJSONObject(key)?.let { days[key] = parseDay(it) }
            }
        }
        return LedgerData(days = days, settings = parseSettings(root.optJSONObject("settings")))
    }

    private fun parseSettings(obj: JSONObject?): LedgerSettings {
        val fallback = LedgerSettings()
        if (obj == null) return fallback
        return LedgerSettings(
            buyTypes = if (obj.has("buyTypes")) obj.optJSONArray("buyTypes").namedOptionList() else fallback.buyTypes,
            sellTypes = if (obj.has("sellTypes")) obj.optJSONArray("sellTypes").namedOptionList() else fallback.sellTypes,
            buyers = if (obj.has("buyers")) obj.optJSONArray("buyers").namedOptionList() else fallback.buyers,
            privacyMode = obj.optBoolean("privacyMode", false),
            fontScale = obj.optDouble("fontScale", 1.0).toFloat().coerceIn(1.0f, 1.3f),
            theme = obj.optString("theme", "forest"),
            defaultSettled = obj.optBoolean("defaultSettled", false),
            recordLocation = obj.optBoolean("recordLocation", true)
        )
    }

    private fun parseDay(obj: JSONObject): DayData = DayData(
        buys = obj.optJSONArray("buys").entryList { parseEntry(it) },
        sells = obj.optJSONArray("sells").entryList { parseEntry(it) },
        remark = obj.optString("remark", "")
    )

    private fun parseEntry(obj: JSONObject): LedgerEntry = LedgerEntry(
        id = obj.optString("id", newId()),
        name = obj.optString("name", ""),
        buyer = obj.optString("buyer", ""),
        remark = obj.optString("remark", ""),
        settled = obj.optBoolean("settled", false),
        adjust = obj.optDouble("adjust", 0.0),
        finalOverride = if (!obj.has("finalOverride") || obj.isNull("finalOverride")) {
            null
        } else {
            obj.optDouble("finalOverride")
        },
        createdAt = obj.optLong("createdAt", 0L),
        location = obj.optJSONObject("location")?.let {
            EntryLocation(
                latitude = it.optDouble("latitude", 0.0),
                longitude = it.optDouble("longitude", 0.0),
                label = if (it.has("label") && !it.isNull("label")) it.optString("label") else null
            )
        },
        types = obj.optJSONArray("types").entryList { parseType(it) }
    )

    private fun parseType(obj: JSONObject): LedgerType = LedgerType(
        id = obj.optString("id", newId()),
        name = obj.optString("name", ""),
        items = obj.optJSONArray("items").entryList { parseItem(it) }
    )

    private fun parseItem(obj: JSONObject): LedgerItem = LedgerItem(
        id = obj.optString("id", newId()),
        weight = obj.optDouble("weight", 0.0),
        price = obj.optDouble("price", 0.0),
        manualAmount = if (!obj.has("manualAmount") || obj.isNull("manualAmount")) {
            null
        } else {
            obj.optDouble("manualAmount")
        }
    )

    private fun JSONArray?.namedOptionList(): List<NamedOption> {
        if (this == null) return emptyList()
        val options = mutableListOf<NamedOption>()
        for (i in 0 until length()) {
            when (val value = opt(i)) {
                is String -> if (value.isNotBlank()) options += NamedOption(value, true)
                is JSONObject -> {
                    val name = value.optString("name", "")
                    if (name.isNotBlank()) {
                        options += NamedOption(name, value.optBoolean("active", true))
                    }
                }
            }
        }
        return options.distinctBy { it.name }
    }

    private fun <T> JSONArray?.entryList(parse: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                optJSONObject(i)?.let { add(parse(it)) }
            }
        }
    }

    fun seed(today: LocalDate = LocalDate.now()): LedgerData {
        val settings = LedgerSettings()
        val days = mutableMapOf<String, DayData>()

        days[today.toString()] = DayData(
            remark = "上午下雨，只收半天",
            buys = listOf(
                LedgerEntry(
                    id = newId(),
                    name = "户1",
                    remark = "本村老客户",
                    createdAt = timestampFor(today, 8, 25),
                    types = listOf(
                        LedgerType(
                            id = newId(),
                            name = "花生果",
                            items = listOf(LedgerItem(newId(), 520.0, 3.6))
                        )
                    )
                ),
                LedgerEntry(
                    id = newId(),
                    name = "户2",
                    adjust = -0.3,
                    createdAt = timestampFor(today, 9, 40),
                    types = listOf(
                        LedgerType(
                            id = newId(),
                            name = "商品米",
                            items = listOf(
                                LedgerItem(newId(), 300.0, 4.9),
                                LedgerItem(newId(), 120.0, 5.1)
                            )
                        )
                    )
                )
            ),
            sells = listOf(
                LedgerEntry(
                    id = newId(),
                    buyer = "超市",
                    remark = "现金结算",
                    settled = true,
                    createdAt = timestampFor(today, 14, 20),
                    types = listOf(
                        LedgerType(
                            id = newId(),
                            name = "商品米",
                            items = listOf(LedgerItem(newId(), 280.0, 6.2))
                        ),
                        LedgerType(
                            id = newId(),
                            name = "下料米",
                            items = listOf(LedgerItem(newId(), 35.0, 3.8))
                        )
                    )
                ),
                LedgerEntry(
                    id = newId(),
                    buyer = "油厂",
                    settled = false,
                    createdAt = timestampFor(today, 16, 5),
                    types = listOf(
                        LedgerType(
                            id = newId(),
                            name = "商品米",
                            items = listOf(LedgerItem(newId(), 120.0, 6.3))
                        )
                    )
                )
            )
        )

        val yesterday = today.minusDays(1)
        days[yesterday.toString()] = DayData(
            buys = listOf(
                LedgerEntry(
                    id = newId(),
                    name = "户1",
                    createdAt = timestampFor(yesterday, 9, 10),
                    types = listOf(
                        LedgerType(
                            id = newId(),
                            name = "花生果",
                            items = listOf(LedgerItem(newId(), 400.0, 3.5))
                        )
                    )
                )
            ),
            sells = listOf(
                LedgerEntry(
                    id = newId(),
                    buyer = "超市",
                    settled = true,
                    createdAt = timestampFor(yesterday, 15, 30),
                    types = listOf(
                        LedgerType(
                            id = newId(),
                            name = "商品米",
                            items = listOf(LedgerItem(newId(), 260.0, 6.1))
                        )
                    )
                )
            )
        )

        val threeDaysAgo = today.minusDays(3)
        days[threeDaysAgo.toString()] = DayData(
            sells = listOf(
                LedgerEntry(
                    id = newId(),
                    buyer = "油厂",
                    settled = false,
                    createdAt = timestampFor(threeDaysAgo, 11, 0),
                    types = listOf(
                        LedgerType(
                            id = newId(),
                            name = "商品米",
                            items = listOf(LedgerItem(newId(), 300.0, 6.3))
                        )
                    )
                )
            )
        )

        val lastMonth = today.minusDays(32)
        days[lastMonth.toString()] = DayData(
            buys = listOf(
                LedgerEntry(
                    id = newId(),
                    name = "户3",
                    createdAt = timestampFor(lastMonth, 10, 15),
                    types = listOf(
                        LedgerType(
                            id = newId(),
                            name = "种子果",
                            items = listOf(LedgerItem(newId(), 600.0, 4.1))
                        )
                    )
                )
            )
        )

        return LedgerData(days = days, settings = settings)
    }
}
