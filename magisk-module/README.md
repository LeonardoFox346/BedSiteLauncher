# 床头钟 Root 模块（Magisk / KernelSU）

把「床头钟」设为系统桌面，并自动完成几项需要手动点的设置，适合已 Root 的设备。

## 安装

1. 先安装桌面 APK（`dist/BedsideLauncher-1.0.1.apk`）。
2. 在 Magisk / KernelSU 里安装本模块 zip，然后重启。
3. 首次开机后等约 30 秒，模块会自动：
   - 把「床头钟」设为默认桌面
   - 授予「最近任务」的用量访问
   - 打开通知使用权（控制中心读媒体）
   - 打开无障碍服务（免 Root 锁屏）
   - 加入电池优化白名单

## 配置

安装后编辑 `/data/adb/modules/bedside_launcher/config.env`，重启生效：

| 项 | 默认 | 说明 |
|---|---|---|
| SET_HOME | 1 | 设为默认桌面 |
| GRANT_USAGE | 1 | 最近任务用量访问 |
| GRANT_NOTIFICATION | 1 | 通知使用权 |
| ENABLE_A11Y | 1 | 无障碍锁屏 |
| BATTERY_WHITELIST | 1 | 电池白名单 |
| DISABLE_STOCK | 0 | 停用原生桌面（谨慎） |
| STOCK_PKGS | launcher3/nexuslauncher | 要停用的桌面包名 |

## 卸载

在 Magisk / KernelSU 中删除模块并重启：原生桌面会被重新启用，默认桌面回到系统桌面。

## 多任务说明

系统的手势「最近任务」由 ROM 自带的多任务提供者负责。本模块可以把原生桌面（同时是 Quickstep 多任务提供者）停用，让系统回落到「床头钟」的桌面；应用内的最近任务面板在底部上滑打开，支持长按或点 × 结束应用后台（Root 下执行 `am force-stop`，会停掉服务、广播与推送）。
