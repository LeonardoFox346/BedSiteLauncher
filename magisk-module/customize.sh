#!/system/bin/sh
SKIPUNZIP=0

ui_print "- 床头钟桌面模块"
ui_print "- 安装后将自动把「床头钟」设为默认桌面"
ui_print "- 首次开机若未生效，请重启一次"
ui_print "- 配置项在 /data/adb/modules/bedside_launcher/config.env"

chmod 755 "$MODPATH/service.sh" 2>/dev/null
chmod 755 "$MODPATH/uninstall.sh" 2>/dev/null
