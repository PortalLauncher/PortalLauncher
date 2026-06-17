# Portal Launcher（桌面启动器）

一个强大的 Android 桌面启动器工具，支持 Android 5.0+ 设备，TV 兼容。可替换系统桌面、设置开机自启动、自定义延迟时间和网络可用后启动。An Android Home Launcher for TV boxes and legacy devices with auto-launch, Home-key remapping, and emergency recovery.

## 📱 创意来源

很多老旧电视盒子 / Android TV 设备无法修改默认启动器，也无法开机后自动切换信号输入源或者启动指定应用。Portal Launcher 正是为了解决这个问题而生——它可以：

- **替换系统桌面**：按 Home 键直接进入你指定的应用
- **开机自启动**：设备通电后自动打开指定 App，支持延迟和网络条件
- **紧急恢复**：10 秒内连续按 Home 键 N 次自动回到设置页面，防止锁死

## 🚀 快速开始

### 安装与使用

| 模式 | 条件 | 操作 |
|------|------|------|
| **初级** | 设备支持修改默认桌面 | 安装 Portal Launcher → 系统设置中设为默认桌面 |
| **中级** | 不支持修改但有 Root/ADB | 安装后冻结或删除系统默认桌面 |
| **高级** | 仅支持特定包名的桌面 + Root | 用 Apktool M 修改包名为系统默认桌面包名后替换 |
| **究极** | 仅支持特定包名的桌面 + 有固件 | 修改包名后解包固件替换系统桌面 |

### 构建

```bash
./gradlew assemblePortalLauncherNoadsDebug
```

## 🧭 功能

### 桌面管理（Launcher 页签）

| 功能 | 说明 |
|------|------|
| **默认桌面** | 点击选择卡片 → 从收藏列表中选择 → 设为 HOME 键目标 |
| **开机启动项** | 点击 `+` → 选择应用 → 设备启动后自动打开 |
| **启动模式** | 回到前台 / 重新启动 |
| **急救模式** | 10 秒内连续启动 N 次 → 自动打开设置 |

### 开机启动项配置

| 类型 | 说明 |
|------|------|
| **立即启动**（延迟 0） | 开机后立刻启动 |
| **立即启动**（延迟 > 0） | 开机后等待指定秒数后启动 |
| **网络可用后启动** | 等设备联网后再启动，可额外设置延迟 |

## ❓ 常见问题

- **设置默认桌面或开机启动项后不生效**

    确认已将 Portal Launcher 设为默认桌面，尝试在设置中开启"允许 Root"开关。

- **如何找到需要的 Activity**

    部分设备存在工厂菜单、信号源切换等隐藏 Activity，在"全部"页签中按包名搜索并逐个测试。

- **如何进入急救模式**

    10 秒内连续按 Home 键 3 次（可在设置中调整），自动进入 Portal Launcher 设置页面。

## 📄 许可证

本项目基于 [ActivityLauncher](https://github.com/butzist/ActivityLauncher) 开发，采用开源许可证，详情参见 [LICENSE](LICENSE) 文件。

## 🙏 致谢

- [ActivityLauncher](https://github.com/butzist/ActivityLauncher) — 原始项目
- [AnyLauncher](https://github.com/tumuyan/AnyLauncher) — 参考设计
