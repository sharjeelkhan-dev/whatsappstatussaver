package com.example.whatsappstatussaver.ui.chat

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AppTeal = Color(0xFF00897B)
private val LightBg = Color(0xFFE0F2F1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectChatScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var phoneNumber by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val context = LocalContext.current
    var countryCode by remember { mutableStateOf("+371") }
    var countryName by remember { mutableStateOf("Latvia (LV)") }
    var expanded by remember { mutableStateOf(false) }

    val countries = listOf(
        "Latvia (LV)" to "+371",
        "Pakistan (PK)" to "+92",
        "India (IN)" to "+91",
        "USA (US)" to "+1",
        "UK (GB)" to "+44",
        "UAE (AE)" to "+971",
        "Saudi Arabia (SA)" to "+966"
    )

    Scaffold(
        topBar = {
            Surface(shadowElevation = 4.dp) {
                TopAppBar(
                    title = { Text("WhatsApp Direct Chat", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(36.dp)
                                .background(LightBg, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.ArrowBackIosNew, 
                                contentDescription = "Back", 
                                tint = AppTeal,
                                modifier = Modifier.size(18.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color.Black
                    )
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Country Code Picker - Modern Style as per screenshot
            Box {
                Surface(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = LightBg
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                             // Latvia Flag Placeholder Icon
                             Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFF670E1A), modifier = Modifier.size(24.dp))
                             Spacer(modifier = Modifier.width(12.dp))
                             Text("$countryName $countryCode", color = AppTeal, fontWeight = FontWeight.Medium)
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = AppTeal)
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f).background(Color.White)
                ) {
                    countries.forEach { (name, code) ->
                        DropdownMenuItem(
                            text = { Text("$name $code") },
                            onClick = {
                                countryName = name
                                countryCode = code
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Phone Number Input
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Phone Number", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { if (it.all { char -> char.isDigit() }) phoneNumber = it },
                    placeholder = { Text("Enter Your Number", color = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedBorderColor = AppTeal,
                        cursorColor = AppTeal
                    )
                )
            }

            // Message Input
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Your Message", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = { Text("Write Your Message...", color = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedBorderColor = AppTeal,
                        cursorColor = AppTeal
                    )
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Send Button
            Button(
                onClick = {
                    if (phoneNumber.isEmpty()) {
                        Toast.makeText(context, "Please enter a phone number", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val formattedNumber = countryCode.replace("+", "") + phoneNumber
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://wa.me/$formattedNumber?text=${Uri.encode(message)}")
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppTeal)
            ) {
                Text("Send via WhatsApp", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun DirectChatScreenPreview() {
    MaterialTheme {
        DirectChatScreen(onNavigateBack = {})
    }
}
