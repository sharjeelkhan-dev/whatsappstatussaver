package com.example.whatsappstatussaver.ui.chat

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

private val AppTeal = Color(0xFF00897B)
private val LightBg = Color(0xFFE0F2F1)

data class Country(val name: String, val code: String, val flag: String, val abbrev: String)

val allCountries = listOf(
    Country("Afghanistan", "+93", "🇦🇫", "AF"),
    Country("Albania", "+355", "🇦🇱", "AL"),
    Country("Algeria", "+213", "🇩🇿", "DZ"),
    Country("Andorra", "+376", "🇦🇩", "AD"),
    Country("Angola", "+244", "🇦🇴", "AO"),
    Country("Argentina", "+54", "🇦🇷", "AR"),
    Country("Armenia", "+374", "🇦🇲", "AM"),
    Country("Australia", "+61", "🇦🇺", "AU"),
    Country("Austria", "+43", "🇦🇹", "AT"),
    Country("Azerbaijan", "+994", "🇦🇿", "AZ"),
    Country("Bahrain", "+973", "🇧🇭", "BH"),
    Country("Bangladesh", "+880", "🇧🇩", "BD"),
    Country("Belarus", "+375", "🇧🇾", "BY"),
    Country("Belgium", "+32", "🇧🇪", "BE"),
    Country("Bhutan", "+975", "🇧🇹", "BT"),
    Country("Bolivia", "+591", "🇧🇴", "BO"),
    Country("Brazil", "+55", "🇧🇷", "BR"),
    Country("Bulgaria", "+359", "🇧🇬", "BG"),
    Country("Cambodia", "+855", "🇰🇭", "KH"),
    Country("Cameroon", "+237", "🇨🇲", "CM"),
    Country("Canada", "+1", "🇨🇦", "CA"),
    Country("Chile", "+56", "🇨🇱", "CL"),
    Country("China", "+86", "🇨🇳", "CN"),
    Country("Colombia", "+57", "🇨🇴", "CO"),
    Country("Costa Rica", "+506", "🇨🇷", "CR"),
    Country("Croatia", "+385", "🇭🇷", "HR"),
    Country("Cuba", "+53", "🇨🇺", "CU"),
    Country("Cyprus", "+357", "🇨🇾", "CY"),
    Country("Czech Republic", "+420", "🇨🇿", "CZ"),
    Country("Denmark", "+45", "🇩🇰", "DK"),
    Country("Egypt", "+20", "🇪🇬", "EG"),
    Country("Estonia", "+372", "🇪🇪", "EE"),
    Country("Ethiopia", "+251", "🇪🇹", "ET"),
    Country("Finland", "+358", "🇫🇮", "FI"),
    Country("France", "+33", "🇫🇷", "FR"),
    Country("Georgia", "+995", "🇬🇪", "GE"),
    Country("Germany", "+49", "🇩🇪", "DE"),
    Country("Ghana", "+233", "🇬🇭", "GH"),
    Country("Greece", "+30", "🇬🇷", "GR"),
    Country("Hong Kong", "+852", "🇭🇰", "HK"),
    Country("Hungary", "+36", "🇭🇺", "HU"),
    Country("Iceland", "+354", "🇮🇸", "IS"),
    Country("India", "+91", "🇮🇳", "IN"),
    Country("Indonesia", "+62", "🇮🇩", "ID"),
    Country("Iran", "+98", "🇮🇷", "IR"),
    Country("Iraq", "+964", "🇮🇶", "IQ"),
    Country("Ireland", "+353", "🇮🇪", "IE"),
    Country("Israel", "+972", "🇮🇱", "IL"),
    Country("Italy", "+39", "🇮🇹", "IT"),
    Country("Japan", "+81", "🇯🇵", "JP"),
    Country("Jordan", "+962", "🇯🇴", "JO"),
    Country("Kazakhstan", "+7", "🇰🇿", "KZ"),
    Country("Kenya", "+254", "🇰🇪", "KE"),
    Country("Kuwait", "+965", "🇰🇼", "KW"),
    Country("Latvia", "+371", "🇱🇻", "LV"),
    Country("Lebanon", "+961", "🇱🇧", "LB"),
    Country("Libya", "+218", "🇱🇾", "LY"),
    Country("Lithuania", "+370", "🇱🇹", "LT"),
    Country("Luxembourg", "+352", "🇱🇺", "LU"),
    Country("Malaysia", "+60", "🇲🇾", "MY"),
    Country("Maldives", "+960", "🇲🇻", "MV"),
    Country("Mexico", "+52", "🇲🇽", "MX"),
    Country("Monaco", "+377", "🇲🇨", "MC"),
    Country("Mongolia", "+976", "🇲🇳", "MN"),
    Country("Morocco", "+212", "🇲🇦", "MA"),
    Country("Myanmar", "+95", "🇲🇲", "MM"),
    Country("Nepal", "+977", "🇳🇵", "NP"),
    Country("Netherlands", "+31", "🇳🇱", "NL"),
    Country("New Zealand", "+64", "🇳🇿", "NZ"),
    Country("Nigeria", "+234", "🇳🇬", "NG"),
    Country("North Korea", "+850", "🇰🇵", "KP"),
    Country("Norway", "+47", "🇳🇴", "NO"),
    Country("Oman", "+968", "🇴🇲", "OM"),
    Country("Pakistan", "+92", "🇵🇰", "PK"),
    Country("Palestine", "+970", "🇵🇸", "PS"),
    Country("Panama", "+507", "🇵🇦", "PA"),
    Country("Paraguay", "+595", "🇵🇾", "PY"),
    Country("Peru", "+51", "🇵🇪", "PE"),
    Country("Philippines", "+63", "🇵🇭", "PH"),
    Country("Poland", "+48", "🇵🇱", "PL"),
    Country("Portugal", "+351", "🇵🇹", "PT"),
    Country("Qatar", "+974", "🇶🇦", "QA"),
    Country("Romania", "+40", "🇷🇴", "RO"),
    Country("Russia", "+7", "🇷🇺", "RU"),
    Country("Saudi Arabia", "+966", "🇸🇦", "SA"),
    Country("Senegal", "+221", "🇸🇳", "SN"),
    Country("Serbia", "+381", "🇷🇸", "RS"),
    Country("Singapore", "+65", "🇸🇬", "SG"),
    Country("Slovakia", "+421", "🇸🇰", "SK"),
    Country("Slovenia", "+386", "🇸🇮", "SI"),
    Country("South Africa", "+27", "🇿🇦", "ZA"),
    Country("South Korea", "+82", "🇰🇷", "KR"),
    Country("Spain", "+34", "🇪🇸", "ES"),
    Country("Sri Lanka", "+94", "🇱🇰", "LK"),
    Country("Sudan", "+249", "🇸🇩", "SD"),
    Country("Sweden", "+46", "🇸🇪", "SE"),
    Country("Switzerland", "+41", "🇨🇭", "CH"),
    Country("Syria", "+963", "🇸🇾", "SY"),
    Country("Taiwan", "+886", "🇹🇼", "TW"),
    Country("Tajikistan", "+992", "🇹🇯", "TJ"),
    Country("Tanzania", "+255", "🇹🇿", "TZ"),
    Country("Thailand", "+66", "🇹🇭", "TH"),
    Country("Tunisia", "+216", "🇹🇳", "TN"),
    Country("Turkey", "+90", "🇹🇷", "TR"),
    Country("Uganda", "+256", "🇺🇬", "UG"),
    Country("Ukraine", "+380", "🇺🇦", "UA"),
    Country("United Arab Emirates", "+971", "🇦🇪", "AE"),
    Country("United Kingdom", "+44", "🇬🇧", "GB"),
    Country("United States", "+1", "🇺🇸", "US"),
    Country("Uruguay", "+598", "🇺🇾", "UY"),
    Country("Uzbekistan", "+998", "🇺🇿", "UZ"),
    Country("Vatican City", "+39", "🇻🇦", "VA"),
    Country("Venezuela", "+58", "🇻🇪", "VE"),
    Country("Vietnam", "+84", "🇻🇳", "VN"),
    Country("Yemen", "+967", "🇾🇪", "YE"),
    Country("Zambia", "+260", "🇿🇲", "ZM"),
    Country("Zimbabwe", "+263", "🇿🇼", "ZW")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectChatScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var phoneNumber by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val context = LocalContext.current
    var selectedCountry by remember { mutableStateOf(allCountries.find { it.name == "Latvia" } ?: allCountries[0]) }
    var showCountryPicker by remember { mutableStateOf(false) }

    if (showCountryPicker) {
        CountryPickerDialog(
            onDismiss = { showCountryPicker = false },
            onSelect = { 
                selectedCountry = it
                showCountryPicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WhatsApp Direct Chat", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LightBg)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back", 
                            tint = AppTeal,
                            modifier = Modifier.size(18.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
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
            // Country Picker
            Surface(
                onClick = { showCountryPicker = true },
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
                         Text(selectedCountry.flag, fontSize = 24.sp)
                         Spacer(modifier = Modifier.width(12.dp))
                         Text("${selectedCountry.name} (${selectedCountry.abbrev}) ${selectedCountry.code}",
                             color = AppTeal, fontWeight = FontWeight.Medium)
                    }
                    Icon(Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = AppTeal)
                }
            }

            // Phone Number Input
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

            // Message Input
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                placeholder = { Text("Write Your Message...", color = Color.LightGray) },
                modifier = Modifier.fillMaxWidth().height(160.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedBorderColor = AppTeal,
                    cursorColor = AppTeal
                )
            )

            // Send Button
            Button(
                onClick = {
                    if (phoneNumber.isEmpty()) {
                        Toast.makeText(context, "Please enter a phone number", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val formattedNumber = selectedCountry.code.replace("+", "") + phoneNumber
                    val url = "https://api.whatsapp.com/send?phone=$formattedNumber&text=${Uri.encode(message)}"
                    
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(url)
                    }
                    
                    val chooserIntent = Intent.createChooser(intent, "Open with")
                    
                    // Create explicit intents for both WhatsApp versions to force them into the chooser
                    val whatsappIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(url)
                        setPackage("com.whatsapp")
                    }
                    val businessIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(url)
                        setPackage("com.whatsapp.w4b")
                    }
                    
                    val targetedIntents = mutableListOf<Intent>()
                    if (context.packageManager.getLaunchIntentForPackage("com.whatsapp") != null) {
                        targetedIntents.add(whatsappIntent)
                    }
                    if (context.packageManager.getLaunchIntentForPackage("com.whatsapp.w4b") != null) {
                        targetedIntents.add(businessIntent)
                    }
                    
                    if (targetedIntents.isNotEmpty()) {
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedIntents.toTypedArray())
                    }
                    
                    try {
                        context.startActivity(chooserIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
                    }
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

@Composable
fun CountryPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (Country) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCountries = allCountries.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || it.code.contains(searchQuery)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Country", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = AppTeal)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search country...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredCountries) { country ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(country) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(country.flag, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("${country.name} (${country.abbrev})", modifier = Modifier.weight(1f), fontSize = 16.sp)
                            Text(country.code, fontWeight = FontWeight.Bold, color = AppTeal)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DirectChatScreenPreview() {
    MaterialTheme {
        DirectChatScreen(onNavigateBack = {})
    }
}
