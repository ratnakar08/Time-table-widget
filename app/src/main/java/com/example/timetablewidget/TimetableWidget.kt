package com.example.timetablewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class TimetableWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d("TimetableWidget", "onUpdate called for ${appWidgetIds.size} widgets")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        
        val (current, next) = getPeriods(context)
        Log.d("TimetableWidget", "Updating widget $appWidgetId: Current=$current, Next=$next")
        
        views.setTextViewText(R.id.currentPeriod, current)
        views.setTextViewText(R.id.nextPeriod, next)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getPeriods(context: Context): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayKey = when (dayOfWeek) {
            Calendar.MONDAY -> "MON"
            Calendar.TUESDAY -> "TUE"
            Calendar.WEDNESDAY -> "WED"
            Calendar.THURSDAY -> "THU"
            Calendar.FRIDAY -> "FRI"
            Calendar.SATURDAY -> "SAT"
            else -> "SUN"
        }

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val currentTimeInMinutes = hour * 60 + minute
        
        val schedule = loadScheduleFromPrefs(context)

        val periods = listOf(
            parseTime("09:00", "09:50"),
            parseTime("09:50", "10:40"),
            parseTime("10:40", "11:30"),
            parseTime("11:30", "12:20"),
            parseTime("12:20", "13:10"), // Lunch
            parseTime("13:10", "13:55"),
            parseTime("13:55", "14:40"),
            parseTime("14:40", "15:25"),
            parseTime("15:25", "16:10")
        )

        val daySchedule = schedule[dayKey] ?: return Pair("No Classes Today", "Enjoy your Sunday!")
        
        if (currentTimeInMinutes < periods[0].first) {
            return Pair("Not Started", daySchedule.getOrNull(0) ?: "-")
        }

        for (i in periods.indices) {
            val (start, end) = periods[i]
            if (currentTimeInMinutes in start until end) {
                val currentSubject = daySchedule.getOrNull(i)?.takeIf { it.isNotBlank() } ?: "Free Period"
                val nextSubject = daySchedule.getOrNull(i + 1)?.takeIf { it.isNotBlank() } ?: "Finished"
                return Pair(currentSubject, nextSubject)
            }
        }
        
        for (i in 0 until periods.size - 1) {
            if (currentTimeInMinutes in periods[i].second until periods[i+1].first) {
                return Pair("Transition", daySchedule.getOrNull(i + 1) ?: "-")
            }
        }

        if (currentTimeInMinutes >= periods.last().second) {
            return Pair("Finished", "See you tomorrow!")
        }

        return Pair("No Class", "Finished")
    }

    private fun loadScheduleFromPrefs(context: Context): Map<String, List<String>> {
        val prefs = context.getSharedPreferences("timetable_prefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("schedule_json", null)
        
        val defaultSchedule = mapOf(
            "MON" to listOf("CET", "CET", "CET", "CET", "LUNCH", "WT", "AI&DL", "ML", "ML(T)"),
            "TUE" to listOf("CET", "CET", "CET", "CET", "LUNCH", "OS", "WT(T)", "BDA", "BOE"),
            "WED" to listOf("CET", "CET", "CET", "CET", "LUNCH", "ML", "AI&DL", "CS&CL", "NPTEL/LIBRARY"),
            "THU" to listOf("OS&ML LAB", "OS&ML LAB", "OS&ML LAB", "OS&ML LAB", "LUNCH", "AI&DL", "BOE", "BDA", "BDA"),
            "FRI" to listOf("WT LAB", "WT LAB", "WT LAB", "MENTORING", "LUNCH", "OS", "WT", "WT", "CS&CL"),
            "SAT" to listOf("BDA", "OS", "WT", "ML", "LUNCH", "AI&DL", "BOE", "CS&CL", "SPORTS")
        )

        if (jsonStr == null) return defaultSchedule

        return try {
            val json = JSONObject(jsonStr)
            val result = mutableMapOf<String, List<String>>()
            val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT")
            days.forEach { day ->
                if (json.has(day)) {
                    val array = json.getJSONArray(day)
                    val list = mutableListOf<String>()
                    for (i in 0 until array.length()) {
                        list.add(array.getString(i))
                    }
                    result[day] = list
                } else {
                    result[day] = defaultSchedule[day] ?: emptyList()
                }
            }
            result
        } catch (e: Exception) {
            defaultSchedule
        }
    }

    private fun parseTime(startStr: String, endStr: String): Pair<Int, Int> {
        fun toMinutes(s: String): Int {
            val parts = s.split(":")
            return parts[0].toInt() * 60 + parts[1].toInt()
        }
        return Pair(toMinutes(startStr), toMinutes(endStr))
    }
}