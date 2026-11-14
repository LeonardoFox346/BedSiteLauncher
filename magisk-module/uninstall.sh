#!/system/bin/sh
# 卸载模块：恢复原生桌面
for p in com.android.launcher3 com.google.android.apps.nexuslauncher; do
  pm enable --user 0 "$p" >/dev/null 2>&1
done

# 找到任意可用的系统桌面并设回默认
home=$(cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2>/dev/null | tail -n 1)
case "$home" in
  com.leonardofox.bedside*|"") ;;
  *) cmd package set-home-activity "$home" >/dev/null 2>&1 ;;
esac
