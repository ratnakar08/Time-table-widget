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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
            // Set dynamicColor = false to ensure our custom theme colors are used correctly in dark/light mode
            TimeTableWidgetTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
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

    val horizontalScrollState = rememberScrollState()
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
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Computer Science and Engineering - Section F",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = { /* Save or Edit Settings */ }) {
                Icon(
                    Icons.Default.Edit, 
                    contentDescription = "Edit", 
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant, 
                    RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            Row {
                // Fixed column for "DAY" labels
                Column(modifier = Modifier.width(60.dp)) {
                    Text(
                        "DAY",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(24.dp)) // Adjust spacing to match row heights
                    days.forEach { day ->
                        Text(
                            day,
                            modifier = Modifier.height(61.dp).wrapContentHeight(Alignment.CenterVertically),
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Scrollable column for "Periods" and "Subjects"
                Column(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                    // Header Row: Periods
                    Row {
                        periods.forEach { period ->
                            Text(
                                period,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .width(100.dp),
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Subject Rows
                    days.forEach { day ->
                        Row(modifier = Modifier.padding(vertical = 8.dp)) {
                            val dayList = schedule[day]
                            if (dayList != null) {
                                dayList.forEachIndexed { index, subject ->
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
    val isDark = isSystemInDarkTheme()
    
    val bgColor = when {
        subject.contains("LAB") -> if (isDark) Color(0xFF2E3B4E) else Color(0xFFE3F2FD)
        subject == "LUNCH" -> if (isDark) Color(0xFF262930) else Color(0xFFF5F5F5)
        subject == "CET" -> if (isDark) Color(0xFF1F222C) else Color(0xFFFAFAFA)
        else -> if (isDark) Color(0xFF1F222C) else Color(0xFFFAFAFA)
    }
    
    val textColor = when (subject) {
        "CET" -> if (isDark) Color.White else Color.Black
        "LUNCH" -> Color.Gray
        "AI&DL" -> if (isDark) Color(0xFF81D4FA) else Color(0xFF0288D1)
        "OS" -> if (isDark) Color(0xFFFFCC80) else Color(0xFFF57C00)
        "WT" -> if (isDark) Color(0xFFEF9A9A) else Color(0xFFD32F2F)
        "BDA" -> if (isDark) Color(0xFFA5D6A7) else Color(0xFF388E3C)
        "BOE" -> if (isDark) Color(0xFFFFAB91) else Color(0xFFE64A19)
        "CS&CL" -> if (isDark) Color(0xFFB39DDB) else Color(0xFF512DA8)
        else -> if (isDark) Color.White else Color.Black
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
