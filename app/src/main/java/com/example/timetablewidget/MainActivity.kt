package com.example.timetablewidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timetablewidget.ui.theme.TimeTableWidgetTheme
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TimeTableWidgetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F111A)
                ) {
                    TimetableScreen(this)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(context: Context) {
    val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT")
    val periods = listOf(
        "9:00 - 9:50", "9:50 - 10:40", "10:40 - 11:30", "11:30 - 12:20",
        "12:20 - 1:10", "1:10 - 1:55", "1:55 - 2:40", "2:40 - 3:25", "3:25 - 4:10"
    )

    val initialSchedule = remember { loadSchedule(context, days) }
    
    val schedule = remember {
        mutableStateMapOf<String, SnapshotStateList<String>>().apply {
            initialSchedule.forEach { (day, list) ->
                put(day, mutableStateListOf<String>().apply { addAll(list) })
            }
        }
    }

    var editingCell by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var textValue by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "III B.Tech II Sem",
                    color = Color(0xFF9FA8DA),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Computer Science and Engineering - Section F",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = { /* Save or Edit Settings */ }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161922), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            LazyColumn {
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "DAY",
                            modifier = Modifier.width(60.dp),
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow {
                            itemsIndexed(periods) { _: Int, period: String ->
                                Text(
                                    period,
                                    modifier = Modifier.width(100.dp),
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                itemsIndexed(days) { _: Int, day: String ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            day,
                            modifier = Modifier.width(60.dp),
                            color = Color(0xFF5C6BC0),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow {
                            val dayList = schedule[day]
                            if (dayList != null) {
                                itemsIndexed(dayList) { index: Int, subject: String ->
                                    SubjectCard(subject) {
                                        editingCell = day to index
                                        textValue = subject
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingCell != null) {
        AlertDialog(
            onDismissRequest = { editingCell = null },
            title = { Text("Edit Period") },
            text = {
                TextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val (day, index) = editingCell!!
                    schedule[day]?.let { it[index] = textValue }
                    saveSchedule(context, schedule)
                    updateWidget(context)
                    editingCell = null
                }) {
                    Text("Save")
                }
            }
        )
    }
}

fun saveSchedule(context: Context, schedule: Map<String, List<String>>) {
    val prefs = context.getSharedPreferences("timetable_prefs", Context.MODE_PRIVATE)
    val json = JSONObject()
    schedule.forEach { (day, list) ->
        json.put(day, JSONArray(list))
    }
    prefs.edit().putString("schedule_json", json.toString()).apply()
}

fun loadSchedule(context: Context, days: List<String>): Map<String, List<String>> {
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

fun updateWidget(context: Context) {
    val intent = Intent(context, TimetableWidget::class.java)
    intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    val ids = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, TimetableWidget::class.java))
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
    context.sendBroadcast(intent)
}

@Composable
fun SubjectCard(subject: String, onClick: () -> Unit) {
    val bgColor = when {
        subject.contains("LAB") -> Color(0xFF2E3B4E)
        subject == "LUNCH" -> Color(0xFF262930)
        subject == "CET" -> Color(0xFF1F222C)
        else -> Color(0xFF1F222C)
    }
    
    val textColor = when (subject) {
        "CET" -> Color.White
        "LUNCH" -> Color.DarkGray
        "AI&DL" -> Color(0xFF81D4FA)
        "OS" -> Color(0xFFFFCC80)
        "WT" -> Color(0xFFEF9A9A)
        "BDA" -> Color(0xFFA5D6A7)
        "BOE" -> Color(0xFFFFAB91)
        "CS&CL" -> Color(0xFFB39DDB)
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .width(100.dp)
            .height(45.dp)
            .background(bgColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = subject,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}