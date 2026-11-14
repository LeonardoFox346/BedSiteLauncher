#!/system/bin/sh
# 床头钟：开机自动设为桌面并配置相关设置
# 兼容 Magisk / KernelSU（仅使用 Android 自带命令，不依赖 magisk 专有工具）
MODDIR=${0%/*}
[ -f "$MODDIR/config.env" ] && . "$MODDIR/config.env"

PKG=com.leonardofox.bedside
HOME_ACT="$PKG/.MainActivity"

# 等待系统启动完成
i=0
while [ "$(getprop sys.boot_completed)" != "1" ] && [ $i -lt 150 ]; do
  sleep 2
  i=$((i + 1))
done
sleep 5

# 应用未安装则退出
pm list packages | grep -q "$PKG" || exit 0

# 记录当前（原生）桌面，供停用时使用
STOCK_HOME=""
if [ "$DISABLE_STOCK" = "1" ]; then
  STOCK_HOME=$(cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2>/dev/null | tail -n 1 | cut -d/ -f1)
fi

if [ "$SET_HOME" = "1" ]; then
  cmd package set-home-activity "$HOME_ACT" >/dev/null 2>&1
fi

if [ "$GRANT_USAGE" = "1" ]; then
  appops set "$PKG" GET_USAGE_STATS allow >/dev/null 2>&1
fi

if [ "$GRANT_NOTIFICATION" = "1" ]; then
  current=$(settings get secure enabled_notification_listeners)
  case "$current" in
    *"$PKG"*) ;;
    *) settings put secure enabled_notification_listeners "${current}:$PKG/.media.MediaListenerService" >/dev/null 2>&1 ;;
  esac
fi

if [ "$ENABLE_A11Y" = "1" ]; then
  current=$(settings get secure enabled_accessibility_services)
  case "$current" in
    *"$PKG"*) ;;
    *) settings put secure enabled_accessibility_services "${current}:$PKG/.system.LockAccessibilityService" >/dev/null 2>&1 ;;
  esac
  settings put secure accessibility_enabled 1 >/dev/null 2>&1
fi

if [ "$BATTERY_WHITELIST" = "1" ]; then
  dumpsys deviceidle whitelist +"$PKG" >/dev/null 2>&1
fi

# 停用原生桌面（可选）：连同它的多任务提供者一起让出
if [ "$DISABLE_STOCK" = "1" ]; then
  for p in $STOCK_HOME $STOCK_PKGS; do
    [ -n "$p" ] || continue
    [ "$p" = "$PKG" ] && continue
    pm disable-user --user 0 "$p" >/dev/null 2>&1
  done
fi
