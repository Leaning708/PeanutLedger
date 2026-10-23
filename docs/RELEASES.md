# 花生账本 · 发布归档

> 当前仅输出 debug APK；release 签名与 CI 尚未配置。  
> 产品版本与 Android versionCode 分开维护，详见根目录 [VERSION.md](../VERSION.md)。

## 当前版本

| 项目 | 值 |
| --- | --- |
| 版本 | 0.1.13 |
| versionCode | 19 |
| Git 提交 | `f6137ca` |
| 标签 | `v0.1.13` |
| APK | `app/build/outputs/apk/debug/peanut-ledger.apk` |
| 构建 | Debug，未签名发布 |
| 自动化 | `testDebugUnitTest` 通过 |
| 状态 | 预发布，可本地安装验证 |

当前 APK 内容：新图标（增加奶油留白）、称重键盘跳转、schema v1、采购/销售命名与数据字典重整。

## 近期发布轨迹

| 版本 | 重点 | 代码提交 |
| --- | --- | --- |
| 0.1.13 | 图标四周奶油留白 | `f6137ca` |
| 0.1.12 | 新应用图标、称重键盘跳转 | `94d05db` |
| 0.1.11 | 单价/重量条件聚焦、schema 统一 v1 | `96af1c3` |
| 0.1.10 | 价格/重量布局、销售类型联动、数据字典重整 | `6f5e801` |
| 0.1.9 | 恢复供货商/收购商默认名称 | `856edcc` |
| 0.1.8 | 供货商/收购商文案与销售绑定 | `d1d294b` |

更早版本见 [VERSION.md](../VERSION.md)。

## 发布归档模板

每次正式发版补齐：

```text
版本：
versionCode：
Git commit：
Git tag：
构建类型（debug/release）：
APK 路径/大小：
签名信息（keystore 版本，不记录密码）：
主要变更：
已知问题：
回归结果：
```

## 发布前检查

1. `VERSION.md`、`app/build.gradle.kts`、设置页版本号一致。
2. `./gradlew testDebugUnitTest :app:assembleDebug --offline --console=plain` 通过。
3. APK 路径为 `peanut-ledger.apk`。
4. QA_CHECKLIST 冒烟与高风险项通过。
5. 备份 schema/版本字段未意外变化。
6. 图标、应用名、隐私模式、首次安装空账本确认。
7. commit + tag；归档 APK/说明。
