# 花生账本 · 数据模型与存储

## 1. 存储位置

| 内容 | 位置 |
| --- | --- |
| 账本 JSON | `SharedPreferences("peanut_ledger")` → `ledger_data_v1` |
| 备份偏好 | `SharedPreferences("peanut_backup_prefs")` |
| 备份目录 | `context.getExternalFilesDir("backups")`，不可用回退 `files/backups` |
| 损坏数据隔离 | `files/corrupt-ledger-<timestamp>.json` |

当前 schema：**v1**。项目尚未发布，不保留旧 schema/旧字段兼容层。

## 2. 本地 JSON schema v1

```json
{
  "schemaVersion": 1,
  "settings": {
    "purchaseTypes": [{ "name": "花生果", "active": true }],
    "saleTypes": [{ "name": "商品米", "active": true }],
    "buyers": [{ "name": "超市", "active": true }],
    "privacyMode": false,
    "fontScale": 1.0,
    "theme": "forest",
    "defaultSettled": false,
    "recordLocation": true
  },
  "days": {
    "2026-10-23": {
      "remark": "",
      "purchases": [
        {
          "id": "entry-id",
          "supplier": "供货商1",
          "buyerName": "",
          "remark": "",
          "settled": false,
          "adjust": 0.0,
          "finalOverride": null,
          "createdAt": 1790000000000,
          "location": null,
          "types": [
            {
              "id": "type-id",
              "name": "花生果",
              "items": [
                { "id": "item-id", "weight": 100.0, "price": 6.2, "manualAmount": null }
              ]
            }
          ]
        }
      ],
      "sales": []
    }
  }
}
```

### 2.1 顶层

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| schemaVersion | Int | 固定为 1 |
| settings | Object | 全局设置 |
| days | Map<String,Day> | key 为 ISO 日期 `yyyy-MM-dd` |

### 2.2 Settings

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| purchaseTypes | NamedOption[] | 采购类型 |
| saleTypes | NamedOption[] | 销售类型 |
| buyers | NamedOption[] | 收购商 |
| privacyMode | Bool | 隐私模式 |
| fontScale | Float | 1.0 / 1.15 / 1.3 |
| theme | String | forest/warm/ink/rose/jade/graphite/ocean/sakura/amber |
| defaultSettled | Bool | 新销售默认已结清 |
| recordLocation | Bool | 新增采购记录位置 |

`NamedOption = { name: String, active: Bool }`。

### 2.3 Day

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| remark | String | 当日备注 |
| purchases | Entry[] | 采购记录 |
| sales | Entry[] | 销售记录 |

### 2.4 Entry

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | String | 16 位随机 id |
| supplier | String | 供货商名称（采购） |
| buyerName | String | 收购商名称（销售） |
| remark | String | 单条备注 |
| settled | Bool | 销售是否结清 |
| adjust | Double | 实付/实收调整值 |
| finalOverride | Double? | 手动最终金额 |
| createdAt | Long | 毫秒时间戳 |
| location | Object? | {latitude,longitude,label} |
| types | Type[] | 类型明细 |

### 2.5 Type / Item

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | String | 类型 id |
| name | String | 类型名称 |
| items | Item[] | 称重明细 |

`Item = { id, weight: Double, price: Double, manualAmount: Double? }`。

## 3. 计算规则

```text
itemAutoAmount = round2(weight * price)
itemAmount     = round2(manualAmount ?: itemAutoAmount)
typeSubtotal   = round2(sum(itemAmount))
entryTotal     = round2(sum(typeSubtotal))
entryFinal     = round2(finalOverride ?: entryTotal + adjust)
```

- 所有金额/重量统一 `round2`（两位小数）。
- 实付差额 = `entryFinal - entryTotal`。
- 当天统计：
  - 采购 = Σ entryFinal(purchases)
  - 销售 = Σ entryFinal(sales)
  - 盈亏 = 销售 - 采购
- 赊账金额 = Σ entryFinal(sales where !settled)
- 账龄 = 今天 - 记录日期（天）。

## 4. 不变量

- `purchases`、`sales` 按创建顺序展示。
- 采购名称为空时由 UI/状态层生成 `供货商N`；不应写入空名称。
- 销售 `buyerName` 必须来自设置中的收购商。
- 空类型/空记录会在编辑/关闭时清理。
- 日期 key 为本地时区的 `LocalDate.toString()`。
- 新增销售保存后自动打开销售类型选择。

## 5. 备份格式

文件为 JSON envelope：

```json
{
  "format": "peanut-ledger-backup",
  "formatVersion": 1,
  "schemaVersion": 1,
  "appVersionName": "0.1.13",
  "appVersionCode": 19,
  "createdAt": 1790000000000,
  "reason": "auto | manual | before_restore",
  "data": { "...LedgerData schema v1..." }
}
```

### 版本规则

- `formatVersion > 当前` → 阻断恢复。
- `appVersionCode > 当前` → 阻断，并显示备份/APP 版本。
- `schemaVersion > 当前` → 阻断。
- 当前为 schema v1；不提供旧 schema 兼容迁移。
- 恢复成功前必须创建 `before_restore` 安全备份。

## 6. 备份清理

- 按 envelope `createdAt` 滚动清理，而不是文件 mtime。
- 默认保留 30 天；用户可选 7/30/365 或自定义。
- 删除单条/全部后，如果无备份则重置 `last_success_at`。
- 自动备份只会在后台/跨天检查时触发，每周期最多一次。

## 7. 数据重置

- APP 内「清空所有记账数据」保留设置（类型/收购商等），删除 days。
- 清除 APP 数据/卸载会删除 SharedPreferences、本地账本和备份目录。
- 开发调试可 `adb shell pm clear com.peanut.ledger`。
