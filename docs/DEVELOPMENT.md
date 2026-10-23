# 花生账本 · 开发与发布

## 1. 环境

- JDK 17+
- Android SDK 37（compileSdk 37）
- Android Studio 或命令行
- Gradle Wrapper（项目自带）
- 目标安装：Android 8.0+（minSdk 26）

## 2. 命令

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export GRADLE_USER_HOME="$HOME/.gradle"

./gradlew :app:assembleDebug --offline --console=plain
./gradlew :app:testDebugUnitTest --offline --console=plain

adb install -r app/build/outputs/apk/debug/peanut-ledger.apk
```

APK 固定名称由 `app/build.gradle.kts` 中的 `androidComponents` 配置为 `peanut-ledger.apk`。

## 3. 版本策略

- 当前语义版本：`0.1.13`；`versionCode = 19`。
- 未经明确要求不修改 major/minor，只递增 patch：`0.1.13 → 0.1.14`。
- `versionCode` 独立递增，用于安装升级。
- 每次发版同步：
  - `app/build.gradle.kts`
  - `SettingsScreen.kt` 底部版本
  - `VERSION.md`
  - 相关文档/截图
  - Git commit + tag `vX.Y.Z`

## 4. 发布前检查

1. 更新版本号和 VERSION.md。
2. `./gradlew testDebugUnitTest` 通过。
3. `./gradlew assembleDebug` 通过。
4. 真机/模拟器按 QA_CHECKLIST.md 走一遍关键链路。
5. 检查备份 JSON schema/version 未被意外改动。
6. 检查 APK 名称、图标、应用名。
7. 提交并打 tag。
8. 归档 APK 与更新说明。

## 5. 签名状态

- 当前仅构建 debug APK，未配置 release keystore。
- `buildTypes.release` 只有优化配置，没有签名配置；正式发布前必须补：
  - keystore/密码管理
  - `signingConfigs.release`
  - 版本号/构建产物归档
  - 安装升级验证

## 6. 代码约定

- Kotlin + Compose，不引入网络/图表/数据库库，除非明确需要。
- 业务规则集中在 `LedgerState`；数据编解码在 `LedgerRepository`；备份在 `BackupManager`。
- 组合式 UI 优先复用 `Common.kt` 的卡片/按钮/输入框/标签。
- 颜色、圆角、字体必须来自 `ui/theme` 或现有 token，不在页面里硬编码新的品牌色。
- 用户可见文案统一中文；代码标识使用采购/销售/供货商/收购商语义。
- 数据 schema 变更必须同步 DATA_MODEL.md；未发布阶段不保留旧 schema 兼容层。

## 7. 调试与数据

```bash
# 清空应用数据（会清空账本、偏好、备份偏好）
adb shell pm clear com.peanut.ledger

# 查看本地账本 prefs（debug 包）
adb shell run-as com.peanut.ledger cat shared_prefs/peanut_ledger.xml

# 查看备份
adb shell ls /sdcard/Android/data/com.peanut.ledger/files/backups
```

## 8. 模拟器

当前开发验证使用 AVD `Medium_Phone_API_37.0`，串口 `emulator-5554`。无窗口启动示例：

```bash
$ANDROID_HOME/emulator/emulator -avd Medium_Phone_API_37.0 \
  -no-window -no-audio -no-boot-anim -no-snapshot \
  -gpu swiftshader_indirect -no-metrics
```

模拟器可能较慢，必要时等待 `sys.boot_completed=1` 后再安装。

## 9. CI / 自动化现状

- 目前无 CI。
- 单元测试：`SupplierNamingTest`（供应商序号/兼容旧自动名称/partyName）。
- 无 Compose UI 测试、无截图自动化、无备份恢复自动化。
- 后续建议接 GitHub Actions/本地脚本：assemble + unit test + lint + 截图回归。
