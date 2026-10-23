# 花生账本 · 技术架构

## 1. 约束与前提

- 原生 Android，Kotlin + Jetpack Compose + Material 3。
- 单 Activity、单 module，无网络、无登录、无服务端。
- 数据本地持久化：`SharedPreferences` + `org.json`。
- `minSdk 26`，`targetSdk 37`，`compileSdk 37`。
- 当前版本：0.1.13（versionCode 19），预发布。
- APK 固定输出：`peanut-ledger.apk`。

## 2. 模块地图

```text
app/src/main/java/com/peanut/ledger/
├── MainActivity.kt              # 入口、edge-to-edge、生命周期备份触发
├── data/
│   ├── Models.kt                # 领域模型、金额/名称/日期工具
│   ├── LedgerRepository.kt      # JSON schema v1 编解码、SP 读写、损坏隔离
│   ├── BackupManager.kt         # 备份文件、滚动清理、版本校验、导入导出
│   └── Statistics.kt            # StatsIndex、区间聚合和图表数据
├── location/
│   └── LocationHelper.kt        # 定位权限/坐标/地址
└── ui/
    ├── PeanutLedgerApp.kt       # 根路由、Toast、字体缩放
    ├── LedgerState.kt           # 单状态容器：页面/数据/操作/备份/恢复
    ├── MainScreen.kt            # 主页、日结卡、账目卡片
    ├── Sheets.kt                # 新增/编辑/称重/备注/确认等弹层
    ├── CalendarSheet.kt         # 日/月/年日历
    ├── SettingsScreen.kt        # 设置
    ├── StatsScreen.kt           # 统计页
    ├── StatsChart.kt            # 自绘折线图 + 长按/拖动浮层
    ├── BackupScreen.kt          # 备份与恢复
    ├── LedgerDatePicker.kt      # 品牌化日期选择器
    ├── Common.kt                # 卡片/按钮/输入框/标签
    ├── LedgerIcons.kt           # 自绘线性图标 + 花生标记
    └── theme/                   # 九套配色、字体/形状
```

## 3. 数据流

```text
Compose UI
   │ 用户操作
   ▼
LedgerState (mutableStateOf<LedgerData>)
   │ persist()
   ▼
LedgerRepository.save()
   │  encode()
   ▼
SharedPreferences("peanut_ledger").ledger_data_v1 (JSON schema v1)
```

- `LedgerState.data` 是唯一账本真相源。
- 所有修改通过 `updateDay` / `updateEntry` / `updateSettingsList` 等收口。
- 每次持久化后递增 `dataRevision`，统计页据此重建 `StatsIndex`。
- 备份/恢复成功后递增 `backupRevision`，备份列表刷新。
- `LedgerState` 同时承担页面路由（`AppScreen`）和弹层（`Sheet`）状态。

## 4. 关键组件职责

### LedgerState

- 页面/日期/tab/弹层/Toast/隐私显示状态。
- 采购与销售、称重、实付调整、备注、设置、日历、统计入口、备份/恢复 API。
- 业务规则集中地；文件较大（约 1.1k 行），是当前主要技术债。

### LedgerRepository

- schema v1 完整 JSON 编解码。
- 存储 key：`ledger_data_v1`。
- 解析失败时把原始 JSON 写入 `files/corrupt-ledger-*.json`，再初始化空数据，避免静默覆盖。
- 只读取当前 schema/字段，不兼容旧 schema。

### BackupManager

- 备份目录：`getExternalFilesDir("backups")`（不可用时回退 `files/backups`）。
- 文件 envelope：format、formatVersion、schemaVersion、appVersionName/Code、createdAt、reason、data。
- 自动备份：APP 进入后台时触发；周期内最多一次；跨天检查补备。
- 保留策略：按 `createdAt` 滚动清理；用户自定义保留天数。
- 恢复前安全备份、导入/导出、版本阻断。

### StatsIndex

- 一次性扁平化所有 purchase/sale 记录。
- 按 `StatsRange` 和筛选缓存 summary/credit/ranking/charts，避免每次重组重复遍历。
- 图表使用 Canvas + TextMeasurer，无第三方图表库。

## 5. 生命周期与备份时机

```text
APP onCreate
  └─ LedgerState.init：加载/迁移本地数据（不做旧 schema 兼容）
APP 启动
  └─ 不主动创建自动备份
APP 进入后台 (MainActivity.onStop)
  └─ maybeAutoBackup()：到期则写 auto 备份
跨天仍在 APP 内
  └─ checkDayChange() → maybeAutoBackup()
手动备份
  └─ 立即写 manual 备份
恢复
  └─ 先写 before_restore 安全备份，再覆盖
```

设计取舍：后台备份比编辑过程中频繁写入更稳定，且每个周期最多一次，避免产生大量半成品快照。

## 6. 线程与性能

- 本地数据量定位为个人/小商户规模；JSON 全量读写和备份文件 IO 当前在主线程完成，单次数据约 KB 级，暂可接受。
- 当记录数/备份数增长到万级时，应改为协程/IO dispatcher + Room/增量写入。
- `StatsIndex` 已有按范围/筛选缓存；`StatsScreen` 使用 `remember(dataRevision)` 重建。
- 备份列表分页只渲染当前页最多 5 条。

## 7. 错误处理

- 本地 JSON 解析失败：隔离原始文件 → 初始化空数据 → 保存新结构。
- 备份损坏/格式不符：阻止恢复并提示。
- 备份 schema/APP 版本过高：阻断并展示双方版本。
- 自动备份失败：不弹错误，等待下一次触发；手动备份失败 Toast。
- 导入失败：删除导入副本并提示。

## 8. 依赖与构建

- Compose BOM、Activity Compose、Material3、Lifecycle Runtime、Core KTX。
- 无网络库、无数据库库、无图表库。
- `androidComponents.onVariants` 将输出 APK 固定为 `peanut-ledger.apk`。
- 当前只生成 debug APK；release 签名/CI 尚未配置。

## 9. 架构评估

**合理之处**

- 单 Activity + 单一状态容器符合当前体量和本地应用定位。
- 序列化、备份、统计已经拆成独立对象，业务边界清晰。
- schema/version/损坏隔离已显式化。

**主要债务**

1. `LedgerState` 过大，后续可拆成 Ledger/Settings/Backup/Calendar 控制器。
2. JSON 全量持久化和同步 IO 是规模扩展瓶颈。
3. 缺少 repository/备份的 JVM 测试；目前只有命名规则单测。
4. 无 release 签名、CI、崩溃采集。
5. 备份文件位于应用目录，卸载/清数据会丢失；未来应提供用户可见导出/云同步。
