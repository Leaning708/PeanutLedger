package com.peanut.ledger.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object LedgerRepository {
    const val CURRENT_SCHEMA_VERSION = 1
    private const val PREFS_NAME = "peanut_ledger"
    private const val DATA_KEY = "ledger_data_v1"

    fun load(context: Context): LedgerData {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(DATA_KEY, null)
            ?: return initialData().also { save(context, it) }
        return runCatching { decode(JSONObject(raw)) }
            .getOrElse {
                quarantine(context, raw)
                initialData().also { save(context, it) }
            }
    }

    fun save(context: Context, data: LedgerData) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(DATA_KEY, encode(data).toString())
            .apply()
    }

    fun encode(data: LedgerData): JSONObject = data.toJson()

    private fun quarantine(context: Context, raw: String) {
        runCatching {
            val file = java.io.File(
                context.filesDir,
                "corrupt-ledger-${System.currentTimeMillis()}.json"
            )
            file.writeText(raw, Charsets.UTF_8)
        }
    }

    fun decode(root: JSONObject): LedgerData = parseData(root)

    private fun LedgerData.toJson(): JSONObject {
        val root = JSONObject()
        root.put("schemaVersion", CURRENT_SCHEMA_VERSION)
        root.put("settings", settings.toJson())
        val daysObj = JSONObject()
        days.forEach { (key, day) -> daysObj.put(key, day.toJson()) }
        root.put("days", daysObj)
        return root
    }

    private fun LedgerSettings.toJson(): JSONObject = JSONObject()
        .put("purchaseTypes", purchaseTypes.optionsToJsonArray())
        .put("saleTypes", saleTypes.optionsToJsonArray())
        .put("buyers", buyers.optionsToJsonArray())
        .put("privacyMode", privacyMode)
        .put("fontScale", fontScale)
        .put("theme", theme)
        .put("defaultSettled", defaultSettled)
        .put("recordLocation", recordLocation)

    private fun DayData.toJson(): JSONObject = JSONObject()
        .put("remark", remark)
        .put("purchases", purchases.entriesToJsonArray())
        .put("sales", sales.entriesToJsonArray())

    private fun LedgerEntry.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("supplier", supplierName)
        .put("buyerName", buyerName)
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
        val schema = root.optInt("schemaVersion", 1)
        require(schema <= CURRENT_SCHEMA_VERSION) {
            "Unsupported local data schema $schema (max $CURRENT_SCHEMA_VERSION)"
        }
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
            purchaseTypes = if (obj.has("purchaseTypes")) {
                obj.optJSONArray("purchaseTypes").namedOptionList()
            } else {
                fallback.purchaseTypes
            },
            saleTypes = if (obj.has("saleTypes")) {
                obj.optJSONArray("saleTypes").namedOptionList()
            } else {
                fallback.saleTypes
            },
            buyers = if (obj.has("buyers")) obj.optJSONArray("buyers").namedOptionList() else fallback.buyers,
            privacyMode = obj.optBoolean("privacyMode", false),
            fontScale = obj.optDouble("fontScale", 1.0).toFloat().coerceIn(1.0f, 1.3f),
            theme = obj.optString("theme", "forest"),
            defaultSettled = obj.optBoolean("defaultSettled", false),
            recordLocation = obj.optBoolean("recordLocation", true)
        )
    }

    private fun parseDay(obj: JSONObject): DayData = DayData(
        purchases = obj.optJSONArray("purchases").entryList { parseEntry(it) },
        sales = obj.optJSONArray("sales").entryList { parseEntry(it) },
        remark = obj.optString("remark", "")
    )

    private fun parseEntry(obj: JSONObject): LedgerEntry = LedgerEntry(
        id = obj.optString("id", newId()),
        supplierName = obj.optString("supplier", ""),
        buyerName = obj.optString("buyerName", ""),
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

    fun initialData(): LedgerData =
        LedgerData(days = emptyMap(), settings = LedgerSettings())
}
