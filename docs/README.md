# 花生账本 · 项目文档

> 归档快照：2026-10-23 · v0.1.13（versionCode 19）· 预发布阶段  
> 代码基线：`f6137ca`（图标留白调整）；文档更新单独提交。

这是面向后续持续迭代的项目文档入口。根目录 [README](../README.md) 面向使用者，本目录面向产品、设计、开发和测试。

## 文档索引

| 文档 | 用途 |
| --- | --- |
| [PRODUCT.md](PRODUCT.md) | 产品定位、术语、功能清单和业务规则 |
| [ARCHITECTURE.md](ARCHITECTURE.md) | 模块结构、数据流、状态管理和技术债 |
| [DATA_MODEL.md](DATA_MODEL.md) | 本地 JSON schema v1、备份格式和版本策略 |
| [DEVELOPMENT.md](DEVELOPMENT.md) | 构建、测试、发布流程和代码约定 |
| [QA_CHECKLIST.md](QA_CHECKLIST.md) | 手工回归清单与发布门禁 |
| [UI_GUIDE.md](UI_GUIDE.md) | 视觉规范、主题、组件和页面清单 |
| [DECISIONS.md](DECISIONS.md) | 关键架构/产品决策记录（ADR） |
| [ROADMAP.md](ROADMAP.md) | 下一阶段优先级、技术债和风险 |
| [RELEASES.md](RELEASES.md) | 当前 APK/提交/标签与发布归档模板 |

## 快速开始

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export GRADLE_USER_HOME="$HOME/.gradle"

./gradlew :app:assembleDebug --offline --console=plain
./gradlew :app:testDebugUnitTest --offline --console=plain

adb install -r app/build/outputs/apk/debug/peanut-ledger.apk
```

APK 固定输出为：

```text
app/build/outputs/apk/debug/peanut-ledger.apk
```

## 阅读顺序建议

1. 新产品/需求评审：PRODUCT.md → UI_GUIDE.md → ROADMAP.md
2. 接手开发：ARCHITECTURE.md → DATA_MODEL.md → DEVELOPMENT.md
3. 发版前：QA_CHECKLIST.md → DEVELOPMENT.md → VERSION.md

## 归档维护约定

- 每次功能性发版更新：版本号、VERSION.md、对应文档中的“当前版本/快照”。
- schema 或备份格式变化时，必须同步 DATA_MODEL.md 和 DECISIONS.md。
- 新的视觉规范/主题变化同步 UI_GUIDE.md。
- 已完成的路线图项移入“已完成/历史”，不要删除关键决策记录。
