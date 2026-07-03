package com.example.whatsappstatussaver.ui.reminder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.whatsappstatussaver.R
import com.example.whatsappstatussaver.data.local.entity.ReminderEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val AppTeal = Color(0xFF00897B)

enum class ReminderScreenType {
    DASHBOARD,
    LIST,
    ADD
}

@Composable
fun ReminderScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReminderViewModel = hiltViewModel()
) {
    var currentScreen by remember { mutableStateOf(ReminderScreenType.DASHBOARD) }
    val reminders by viewModel.allReminders.collectAsState()

    when (currentScreen) {
        ReminderScreenType.DASHBOARD -> {
            ReminderDashboardScreen(
                reminders = reminders,
                onBack = onNavigateBack,
                onNavigateToList = { currentScreen = ReminderScreenType.LIST },
                onAddNew = { currentScreen = ReminderScreenType.ADD }
            )
        }
        ReminderScreenType.LIST -> {
            ReminderListScreen(
                reminders = reminders,
                onBack = { currentScreen = ReminderScreenType.DASHBOARD },
                onAddNew = { currentScreen = ReminderScreenType.ADD },
                onDelete = { viewModel.deleteReminder(it) },
                onToggleCompletion = { viewModel.toggleReminderCompletion(it) }
            )
        }
        ReminderScreenType.ADD -> {
            AddReminderForm(
                onCancel = { currentScreen = ReminderScreenType.DASHBOARD },
                onAdd = { reminder ->
                    viewModel.addReminder(reminder)
                    currentScreen = ReminderScreenType.DASHBOARD
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDashboardScreen(
    reminders: List<ReminderEntity>,
    onBack: () -> Unit,
    onNavigateToList: () -> Unit,
    onAddNew: () -> Unit
) {
    val todayRemindersCount = reminders.count {
        val cal = Calendar.getInstance()
        val reminderCal = Calendar.getInstance().apply { timeInMillis = it.date }
        cal.get(Calendar.YEAR) == reminderCal.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == reminderCal.get(Calendar.DAY_OF_YEAR)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminder", fontWeight = FontWeight.Bold,
                    fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE0F2F1))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppTeal,
                            modifier = Modifier.size(18.dp)
                        )
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
                    icon = ImageVector.vectorResource(id = R.drawable.calendar_blank_line_icon),
                    modifier = Modifier.weight(1f)
                )
                ReminderStatCard(
                    title = "Scheduled",
                    count = reminders.size,
                    icon = ImageVector.vectorResource(id = R.drawable.date_icon),
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
                    icon = ImageVector.vectorResource(id = R.drawable.check_list_icon),
                    onClick = onNavigateToList
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
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1).copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppTeal),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = count.toString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238)
                )
            }
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ReminderListItem(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                Icon(icon, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(20.dp))
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
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    reminders: List<ReminderEntity>,
    onBack: () -> Unit,
    onAddNew: () -> Unit,
    onDelete: (ReminderEntity) -> Unit,
    onToggleCompletion: (ReminderEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredReminders = reminders.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
    }

    val incompleteCount = filteredReminders.count { !it.isCompleted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminder List", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE0F2F1))
                    ) {
                        Icon(
                            Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = AppTeal,
                            modifier = Modifier.size(18.dp)
                        )
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
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "You have got $incompleteCount tasks to complete",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search Task Here", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.LightGray,
                    unfocusedBorderColor = Color.LightGray,
                    cursorColor = AppTeal
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (filteredReminders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No reminders found", color = Color.Gray)
                }
            } else {
                val grouped = filteredReminders.groupBy {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                    val now = Calendar.getInstance()
                    if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                    ) "Today" else "Upcoming"
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    grouped.forEach { (header, list) ->
                        Text(
                            text = header,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        list.forEach { reminder ->
                            ReminderItem(
                                reminder = reminder,
                                onToggle = { onToggleCompletion(reminder) },
                                onDelete = { onDelete(reminder) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(80.dp)) // FAB space
                }
            }
        }
    }
}

@Composable
fun ReminderItem(
    reminder: ReminderEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Teal Stripe
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .fillMaxHeight()
                    .background(AppTeal)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateFormatter.format(Date(reminder.date)),
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            // Radio/Checkbox Circle
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (reminder.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Toggle Complete",
                    tint = if (reminder.isCompleted) AppTeal else Color.LightGray,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Delete button (Optional but useful)
            IconButton(onClick = onDelete) {
                Icon( painter = painterResource(id = R.drawable.recycle_bin_icon),
                    contentDescription = "Delete",
                    tint = Color.Red.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(8.dp))
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    scrolledContainerColor = Color.Unspecified,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = Color.Unspecified,
                    actionIconContentColor = Color.Unspecified
                )
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

@Preview(showBackground = true, name = "Reminder Dashboard")
@Composable
fun ReminderDashboardPreview() {
    MaterialTheme {
        ReminderDashboardScreen(
            reminders = listOf(
                ReminderEntity(id = 1, title = "Task 1", description = "", date = 0, time = 0, repeatType = "", priority = "", isAlertEnabled = true)
            ),
            onBack = {},
            onNavigateToList = {},
            onAddNew = {}
        )
    }
}

@Preview(showBackground = true, name = "Reminder List")
@Composable
fun ReminderListPreview() {
    MaterialTheme {
        ReminderListScreen(
            reminders = listOf(
                ReminderEntity(
                    id = 1,
                    title = "Check Status",
                    description = "Description",
                    date = System.currentTimeMillis(),
                    time = System.currentTimeMillis(),
                    repeatType = "Daily",
                    priority = "High",
                    isAlertEnabled = true,
                    isCompleted = false
                ),
                ReminderEntity(
                    id = 2,
                    title = "Update Story",
                    description = "Description",
                    date = System.currentTimeMillis(),
                    time = System.currentTimeMillis(),
                    repeatType = "None",
                    priority = "Medium",
                    isAlertEnabled = true,
                    isCompleted = true
                )
            ),
            onBack = {},
            onAddNew = {},
            onDelete = {},
            onToggleCompletion = {}
        )
    }
}
