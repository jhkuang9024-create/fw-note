#!/system/bin/sh
# 用法: sh /path/boot_monitor.sh [间隔秒] [总时长秒]
INTERVAL=${1:-5}
DURATION=${2:-120}
END_TIME=$(( $(date +%s) + DURATION ))

echo "[BootMonitor] start $(date), interval=${INTERVAL}s, duration=${DURATION}s"

while [ $(date +%s) -lt $END_TIME ]; do
    echo "===== $(date +%H:%M:%S) ====="
    echo "--- CPU ---"
    top -b -n 1 -m 5 -s 1 | head -12
    echo "--- MEM ---"
    free -m | head -3
    echo "--- STAT ---"
    head -1 /proc/stat
    grep -E 'MemTotal|MemFree|MemAvailable' /proc/meminfo
    sleep $INTERVAL
done

echo "[BootMonitor] done $(date)"