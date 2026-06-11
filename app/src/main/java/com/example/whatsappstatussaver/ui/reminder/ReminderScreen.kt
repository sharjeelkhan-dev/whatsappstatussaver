package com.example.whatsappstatussaver.ui.reminder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.whatsappstatussaver.data.local.entity.ReminderEntity
import java.text.SimpleDateFormat
import java.util.*

private val AppTeal = Color(0xFF00897B)

@Composable
fun ReminderScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReminderViewModel = hiltViewModel()
) {
    var isAddingNew by remember { mutableStateOf(false) }
    val reminders by viewModel.allReminders.collectAsState()

    if (isAddingNew) {
        AddReminderForm(
            onCancel = { isAddingNew = false },
            onAdd = { reminder ->
                viewModel.addReminder(reminder)
                isAddingNew = false
            }
        )
    } else {
        ReminderListScreen(
            reminders = reminders,
            onBack = onNavigateBack,
            onAddNew = { isAddingNew = true },
            onDelete = { viewModel.deleteReminder(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    reminders: List<ReminderEntity>,
    onBack: () -> Unit,
    onAddNew: () -> Unit,
    onDelete: (ReminderEntity) -> Unit
) {
    val todayRemindersCount = reminders.count {
        val cal = Calendar.getInstance()
        val reminderCal = Calendar.getInstance().apply { timeInMillis = it.date }
        cal.get(Calendar.YEAR) == reminderCal.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == reminderCal.get(Calendar.DAY_OF_YEAR)
    }
    
    val scheduledCount = reminders.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminder", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE0F2F1))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppTeal, modifier = Modifier.size(18.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNew,
                containerColor = AppTeal,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Reminder")
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ReminderStatCard(
                    title = "Today",
                    count = todayRemindersCount,
                    icon = Icons.Default.CalendarToday,
                    modifier = Modifier.weight(1f)
                )
                ReminderStatCard(
                    title = "Scheduled",
                    count = scheduledCount,
                    icon = Icons.Default.Event,
                    modifier = Modifier.weight(1f)
                )
            }

            // My Lists Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "My Lists",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF263238)
                )
                
                ReminderListItem(
                    title = "Reminders",
                    count = reminders.size,
                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                    onClick = { /* Could navigate to detailed list if needed */ }
                )
            }
        }
    }
}

@Composable
fun ReminderStatCard(
    title: String,
    count: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F7F7))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AppTeal),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Text(
                    text = count.toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238)
                )
            }
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ReminderListItem(
    title: String,
    count: Int,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AppTeal),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF263238)
            )
            Text(
                text = count.toString(),
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(end = 8.dp)
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ReminderItem(reminder: ReminderEntity, onDelete: () -> Unit) {
    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(reminder.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "${dateFormatter.format(Date(reminder.date))} at ${timeFormatter.format(Date(reminder.time))}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderForm(
    onCancel: () -> Unit,
    onAdd: (ReminderEntity) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableLongStateOf(Calendar.getInstance().timeInMillis) }
    var selectedTime by remember { mutableLongStateOf(Calendar.getInstance().timeInMillis) }
    var repeatType by remember { mutableStateOf("Daily") }
    var priority by remember { mutableStateOf("High") }
    var isAlertEnabled by remember { mutableStateOf(true) }

    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("hh : mm a", Locale.getDefault())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("New Reminder", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    TextButton(onClick = onCancel) {
                        Text("Cancel", color = AppTeal, fontWeight = FontWeight.SemiBold)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (title.isBlank()) {
                            Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        onAdd(
                            ReminderEntity(
                                title = title,
                                description = description,
                                date = selectedDate,
                                time = selectedTime,
                                repeatType = repeatType,
                                priority = priority,
                                isAlertEnabled = isAlertEnabled
                            )
                        )
                        Toast.makeText(context, "Reminder Added!", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Add", color = AppTeal, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Field
            Text("Title", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Reminder title", color = Color.LightGray) },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Gray,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            // Description Field
            Text("Description", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                placeholder = { Text("Description", color = Color.LightGray) },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Gray,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            // Date Picker Field
            Text("Date", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            OutlinedCard(
                onClick = {
                    val calendar = Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val cal = Calendar.getInstance()
                            cal.set(year, month, dayOfMonth)
                            selectedDate = cal.timeInMillis
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.LightGray),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = AppTeal, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(dateFormatter.format(Date(selectedDate)), fontSize = 16.sp)
                }
            }

            // Time Picker Field
            Text("Time", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            OutlinedCard(
                onClick = {
                    val calendar = Calendar.getInstance()
                    TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            val cal = Calendar.getInstance()
                            cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            cal.set(Calendar.MINUTE, minute)
                            selectedTime = cal.timeInMillis
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        false
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.LightGray),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = AppTeal, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(timeFormatter.format(Date(selectedTime)), fontSize = 16.sp)
                }
            }

            // Repeat Option
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(AppTeal),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Repeat, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Repeat", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                    repeatType = if (repeatType == "Daily") "None" else "Daily"
                }) {
                    Text(repeatType, color = Color.Gray)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }

            // Priority Selection
            Text("Priority", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PriorityButton(label = "High", isSelected = priority == "High", onClick = { priority = "High" }, modifier = Modifier.weight(1f))
                PriorityButton(label = "Medium", isSelected = priority == "Medium", onClick = { priority = "Medium" }, modifier = Modifier.weight(1f))
                PriorityButton(label = "Low", isSelected = priority == "Low", onClick = { priority = "Low" }, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Alert Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Get alert for this task", color = Color.DarkGray)
                Switch(
                    checked = isAlertEnabled,
                    onCheckedChange = { isAlertEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppTeal)
                )
            }
        }
    }
}

@Composable
fun PriorityButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) AppTeal else Color.White,
            contentColor = if (isSelected) Color.White else Color.Gray
        ),
        shape = RoundedCornerShape(8.dp),
        border = if (!isSelected) BorderStroke(1.dp, Color.LightGray) else null,
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true)
@Composable
fun ReminderScreenPreview() {
    MaterialTheme {
        ReminderScreen(onNavigateBack = {})
    }
}
