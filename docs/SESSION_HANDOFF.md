# 花生账本 · 接手摘要

## 当前状态

- 分支：`main`
- 版本：`0.1.13`，versionCode `19`
- 最新代码提交：`f6137ca`（图标留白调整）
- 预发布，未上架；无 release 签名。
- APK：`app/build/outputs/apk/debug/peanut-ledger.apk`

## 已确认的产品规则

1. 采购供货商默认 `供货商N`；销售收购商必须来自设置。
2. 销售页不能新增/改名收购商；设置改名会同步历史。
3. 称重：单价左、重量右；单价空→单价，有值→重量；单价回车→重量，重量回车→保存；不自动聚焦金额。
4. 销售保存后自动进入销售类型选择。
5. 隐私模式：销售姓氏框显示序号，标题 `收购商N`，金额脱敏。
6. 数据 schema v1，不做旧版本兼容。
7. 新安装空白账本，无演示数据。
8. 备份默认每天/保留30天；可自定义；恢复前安全备份；高版本备份阻断。

## 开发注意事项

- 用户可见术语：采购/销售/供货商/收购商，不要写农户/收购方。
- 修改 schema/备份字段必须同步 `docs/DATA_MODEL.md`，并考虑发布后的迁移。
- 统计缓存依赖 `StatsIndex` + `dataRevision`；修改数据入口后确认统计刷新。
- 备份列表分页每页 5 条。
- 图标资源：`res/drawable-xxxhdpi/app_icon_foreground.png` + `res/drawable/ic_launcher_background.xml` + `mipmap-*`。
- 构建 APK 名称固定 `peanut-ledger.apk`。

## 验证入口

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug --offline --console=plain
```

手工回归：`docs/QA_CHECKLIST.md`。
