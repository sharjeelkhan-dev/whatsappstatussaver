package com.sharjeel.whatsappstatussaver.ui.reminder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
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
import com.sharjeel.whatsappstatussaver.R
import com.sharjeel.whatsappstatussaver.data.local.entity.ReminderEntity
import com.sharjeel.whatsappstatussaver.theme.WhatsAppStatusSaverTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val PrimaryGreen = Color(0xFF00A884)
private val SecondaryGreen = Color(0xFF005E4C)
private val SoftGreen = Color(0xFFE7FFFA)
private val DarkText = Color(0xFF1C2D2A)

enum class ReminderScreenType { DASHBOARD, LIST, ADD }

@Composable
fun ReminderScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReminderViewModel = hiltViewModel()
) {
    var currentScreen by remember { mutableStateOf(ReminderScreenType.DASHBOARD) }
    val reminders by viewModel.allReminders.collectAsState()

    when (currentScreen) {
        ReminderScreenType.DASHBOARD -> ReminderDashboardScreen(
            reminders = reminders,
            onBack = onNavigateBack,
            onNavigateToList = { currentScreen = ReminderScreenType.LIST },
            onAddNew = { currentScreen = ReminderScreenType.ADD }
        )
        ReminderScreenType.LIST -> ReminderListScreen(
            reminders = reminders,
            onBack = { currentScreen = ReminderScreenType.DASHBOARD },
            onAddNew = { currentScreen = ReminderScreenType.ADD },
            onDelete = { viewModel.deleteReminder(it) },
            onToggleCompletion = { viewModel.toggleReminderCompletion(it) }
        )
        ReminderScreenType.ADD -> AddReminderForm(
            onCancel = { currentScreen = ReminderScreenType.DASHBOARD },
            onAdd = { reminder ->
                viewModel.addReminder(reminder)
                currentScreen = ReminderScreenType.DASHBOARD
            }
        )
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
        topBar = { ReminderTopBar(title = "Reminders", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNew,
                containerColor = PrimaryGreen,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) { Icon(Icons.Default.Add, contentDescription = "Add") }
        },
        containerColor = Color(0xFFFBFDFF)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ReminderStatCard(
                    title = "Today",
                    count = todayRemindersCount,
                    icon = ImageVector.vectorResource(id = R.drawable.calendar_blank_line_icon),
                    modifier = Modifier.weight(1f),
                    color = PrimaryGreen
                )
                ReminderStatCard(
                    title = "Scheduled",
                    count = reminders.size,
                    icon = ImageVector.vectorResource(id = R.drawable.date_icon),
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF2196F3)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("My Lists", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = DarkText)
                ReminderListCard(
                    title = "All Reminders",
                    count = reminders.size,
                    icon = ImageVector.vectorResource(id = R.drawable.check_list_icon),
                    onClick = onNavigateToList
                )
            }
        }
    }
}

@Composable
private fun ReminderTopBar(title: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(
                brush = Brush.verticalGradient(listOf(PrimaryGreen, SecondaryGreen)),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun ReminderStatCard(title: String, count: Int, icon: ImageVector, modifier: Modifier, color: Color) {
    Card(
        modifier = modifier.height(130.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp)) }
            Column {
                Text(text = count.toString(), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
                Text(text = title, fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ReminderListCard(title: String, count: Int, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(PrimaryGreen.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(24.dp)) }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, modifier = Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DarkText)
            Text(text = count.toString(), fontSize = 16.sp, color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
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
        it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = { ReminderTopBar(title = "My Reminders", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNew, containerColor = PrimaryGreen, contentColor = Color.White, shape = CircleShape)
            { Icon(Icons.Default.Add, contentDescription = "Add") }
        },
        containerColor = Color(0xFFFBFDFF)
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search your tasks...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryGreen) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color.Transparent,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (filteredReminders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No reminders found.", color = Color.Gray) }
            } else {
                val grouped = filteredReminders.groupBy {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                    val now = Calendar.getInstance()
                    if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) "Today" else "Upcoming"
                }
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    grouped.forEach { (header, list) ->
                        Text(text = header, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = DarkText, modifier = Modifier.padding(vertical = 12.dp))
                        list.forEach { reminder ->
                            ReminderItem(reminder = reminder, onToggle = { onToggleCompletion(reminder) }, onDelete = { onDelete(reminder) })
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun ReminderItem(reminder: ReminderEntity, onToggle: () -> Unit, onDelete: () -> Unit) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (reminder.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (reminder.isCompleted) PrimaryGreen else Color.LightGray,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = reminder.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkText)
                Text(text = dateFormatter.format(Date(reminder.date)), fontSize = 13.sp, color = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(painter = painterResource(id = R.drawable.recycle_bin_icon), contentDescription = null, tint = Color.Red.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderForm(onCancel: () -> Unit, onAdd: (ReminderEntity) -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableLongStateOf(Calendar.getInstance().timeInMillis) }
    var selectedTime by remember { mutableLongStateOf(Calendar.getInstance().timeInMillis) }
    var repeatType by remember { mutableStateOf("Daily") }
    var priority by remember { mutableStateOf("High") }
    var isAlertEnabled by remember { mutableStateOf(true) }

    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("New Reminder", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Cancel", color = PrimaryGreen) } },
                actions = {
                    Button(
                        onClick = {
                            if (title.isBlank()) { Toast.makeText(context, "Title is required", Toast.LENGTH_SHORT).show(); return@Button }
                            onAdd(ReminderEntity(title = title, description = description, date = selectedDate, time = selectedTime, repeatType = repeatType, priority = priority, isAlertEnabled = isAlertEnabled))
                            Toast.makeText(context, "Reminder Added!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Save") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFFBFDFF)
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Details", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkText)
            OutlinedTextField(
                value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("What should we remind you?") },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryGreen, unfocusedBorderColor = Color.LightGray)
            )
            OutlinedTextField(
                value = description, onValueChange = { description = it }, modifier = Modifier.fillMaxWidth().height(120.dp),
                placeholder = { Text("Description (Optional)") },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryGreen, unfocusedBorderColor = Color.LightGray)
            )

            Text("Schedule", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkText)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ScheduleItem(icon = Icons.Default.CalendarToday, label = dateFormatter.format(Date(selectedDate)), modifier = Modifier.weight(1f)) {
                    val cal = Calendar.getInstance(); DatePickerDialog(context, { _, y, m, d -> val c = Calendar.getInstance(); c.set(y, m, d); selectedDate = c.timeInMillis }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                }
                ScheduleItem(icon = Icons.Default.AccessTime, label = timeFormatter.format(Date(selectedTime)), modifier = Modifier.weight(1f)) {
                    val cal = Calendar.getInstance(); TimePickerDialog(context, { _, h, m -> val c = Calendar.getInstance(); c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, m); selectedTime = c.timeInMillis }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                }
            }

            Text("Priority", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkText)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PriorityChoice(label = "High", isSelected = priority == "High") { priority = "High" }
                PriorityChoice(label = "Medium", isSelected = priority == "Medium") { priority = "Medium" }
                PriorityChoice(label = "Low", isSelected = priority == "Low") { priority = "Low" }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 1.dp) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Get notified", fontWeight = FontWeight.Bold, color = DarkText)
                    Switch(checked = isAlertEnabled, onCheckedChange = { isAlertEnabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryGreen))
                }
            }
        }
    }
}

@Composable
fun ScheduleItem(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Color.LightGray)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RowScope.PriorityChoice(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(12.dp), color = if (isSelected) PrimaryGreen else Color.White, border = if (!isSelected) BorderStroke(1.dp, Color.LightGray) else null) {
        Box(contentAlignment = Alignment.Center) { Text(label, color = if (isSelected) Color.White else Color.Gray, fontWeight = FontWeight.Bold) }
    }
}

@Preview(showBackground = true)
@Composable
fun ReminderPreview() {
    WhatsAppStatusSaverTheme { ReminderDashboardScreen(emptyList(), {}, {}, {}) }
}

