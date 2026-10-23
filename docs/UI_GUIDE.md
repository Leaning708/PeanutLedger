# 花生账本 · UI 规范

## 1. 设计原则

- **暖纸感**：低饱和奶油背景、细描边、轻阴影，避免冷硬工具感。
- **账本层级**：日期是主页视觉主体；金额/斤数/类型形成清晰层级。
- **一致性**：弹层、卡片、按钮、输入框、空态使用统一圆角和品牌色。
- **本地安全感**：隐私模式、备份、版本校验在文案中明确可见。

## 2. 主题

九套主题：

| id | 名称 |
| --- | --- |
| forest | 松绿（默认） |
| warm | 暖棕 |
| ink | 黛蓝 |
| rose | 绛紫 |
| jade | 青黛 |
| graphite | 墨灰 |
| ocean | 海盐 |
| sakura | 樱粉 |
| amber | 琥珀 |

松绿关键 token（`ui/theme/Color.kt`）：

| token | 值 |
| --- | --- |
| Bg | `#F5F7F0` |
| SurfaceSoft | `#F8FAF4` |
| CardBorder | `#E1E8D7` |
| Primary | `#5B7D45` |
| PrimaryDark | `#3F5C2E` |
| HeaderStart/Mid/End | `#7A9A63` / `#5B7D45` / `#3F5C2E` |
| TextMain | `#202A1B` |
| Muted | `#77836D` |
| Red | `#C74C3B` |
| Green | `#4F8B69` |
| AvatarBuy | `#5B7D45` |
| AvatarSell | `#8A6A2F` |

主题只影响品牌 token，不改变业务语义颜色（赊账红、结清绿等保持可读）。

## 3. 字体与间距

- 系统字体，无自定义字体包。
- 字体档位：标准 1.0 / 大 1.15 / 特大 1.3（通过 `Density.fontScale` 全局缩放）。
- 正文 12–15sp，卡片标题 15–17sp，主页日期 26–32sp。
- 页面左右边距 16dp；卡片内边距 16–18dp；卡片圆角 20dp；弹层 28dp。
- 按钮最小高度 50dp；图标线性 24dp 基准。

## 4. 组件

| 组件 | 位置 | 说明 |
| --- | --- | --- |
| LedgerCard | Common.kt | 白卡 + 细描边 + 20dp 圆角 |
| PrimaryButton | Common.kt | 品牌渐变、14dp 圆角 |
| GhostButton / DangerButton | Common.kt | 次级/危险操作 |
| LedgerTextField | Common.kt | 输入、焦点动画、IME action 可配置 |
| PillChip / CounterTag | Common.kt | 标签/计数 |
| CircleIconButton / IconBadge | Common.kt | 图标按钮/图标底 |
| ChevronDivider | Common.kt | 列表分隔 |
| SheetTitle / SheetHandle | Common.kt | 弹层标题/拖拽柄 |
| LedgerDatePickerDialog | LedgerDatePicker.kt | 品牌化日历弹窗 |
| LineChart | StatsChart.kt | 自绘折线图、长按/拖动浮层 |

## 5. 页面清单

| 页面/弹层 | 主要文件 |
| --- | --- |
| 主页采购/销售 | MainScreen.kt |
| 更多 | Sheets.kt |
| 新增采购/销售 | Sheets.kt |
| 选择类型/新增称重 | Sheets.kt |
| 日历 | CalendarSheet.kt |
| 设置 | SettingsScreen.kt |
| 统计 | StatsScreen.kt |
| 备份与恢复 | BackupScreen.kt |
| 日期选择 | LedgerDatePicker.kt |
| 确认弹层 | Sheets.kt |

## 6. 图标

- 启动图标：奶油纸底 + 深蓝翻页账本 + 小花生 + 美元硬币 + 珊瑚色阴影。
- 自适应图标前景内容约占画布 54%，四周保留奶油留白。
- 应用内图标统一使用 `LedgerIcons.kt` 自绘线性图标，避免 emoji 平台差异。

资源：

- `res/drawable-xxxhdpi/app_icon_foreground.png`
- `res/drawable/ic_launcher_background.xml`
- `res/mipmap-*/ic_launcher*.webp`
- `res/mipmap-anydpi/ic_launcher*.xml`
- 原始设计稿：`docs/assets/icon-source.png`

## 7. 文案

- 采购、销售、供货商、收购商为统一术语。
- 采购：`新增采购` / `供货商` / `供货商1`。
- 销售：`新增销售` / `收购商` / `收购商1`（隐私模式）。
- 避免“农户”“收购方”“新增收购”等旧文案。
- 金额单位：`元`；均价：`元/斤`；斤数：`斤`。
- 隐私模式明确提示“统计需要显示完整金额”等。

## 8. 截图索引

- 主页：`docs/screenshots/home.png`
- 称重：`docs/screenshots/weighing.png`
- 隐私：`docs/screenshots/privacy.png`
- 日结卡：`docs/screenshots/daily-summary.png`
- 统计：`docs/screenshots/stats-*.png`
- 备份：`docs/screenshots/backup*.png`
- 图标：`docs/screenshots/icon.png`

## 9. 可访问性

- 字体支持 1.3 倍；关键页面必须验证。
- 点击目标 ≥ 40dp；危险操作二次确认。
- 颜色不是唯一信息载体：赊账/结清同时有文案。
- 隐私模式不依赖颜色区分。
