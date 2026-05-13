package com.xsyusign.mobile.util

/** 从窗口反推原始签到时间点（取 start 和 end 的中值） */
fun midpointTime(start: String, end: String): String {
    val s = start.split(":").mapNotNull { it.toIntOrNull() }
    val e = end.split(":").mapNotNull { it.toIntOrNull() }
    if (s.size != 2 || e.size != 2) return start
    var sm = s[0] * 60 + s[1]
    var em = e[0] * 60 + e[1]
    if (em < sm) em += 1440
    val mid = (sm + em) / 2 % 1440
    return String.format("%02d:%02d", mid / 60, mid % 60)
}

/** 给定签到时间点，返回 ±15 分钟的窗口 (start, end) */
fun expandTimeWindow(time: String): Pair<String, String> {
    val parts = time.split(":").mapNotNull { it.toIntOrNull() }
    if (parts.size != 2) return (time to time)
    val totalMinutes = parts[0] * 60 + parts[1]
    val start = ((totalMinutes - 15 + 1440) % 1440)
    val end = ((totalMinutes + 15) % 1440)
    return (String.format("%02d:%02d", start / 60, start % 60) to
            String.format("%02d:%02d", end / 60, end % 60))
}
