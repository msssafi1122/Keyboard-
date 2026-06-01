package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.data.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.KeyboardThemes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var prefs: PreferenceManager
    private lateinit var database: AppDatabase
    private lateinit var repository: KeyboardRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefs = PreferenceManager(this)
        database = AppDatabase.getDatabase(this)
        repository = KeyboardRepository(database)

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF0F0E17), Color(0xFFE53170))
                                    )
                                )
                                .statusBarsPadding()
                                .padding(vertical = 16.dp, horizontal = 20.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Bangla AI Keyboard 🌟",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "উন্নত কৃত্রিম বুদ্ধিমত্তা সম্পন্ন বাংলা কীবোর্ড",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    DashboardScreen(
                        modifier = Modifier.padding(innerPadding),
                        prefs = prefs,
                        repository = repository
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(modifier: Modifier = Modifier, prefs: PreferenceManager, repository: KeyboardRepository) {
    val context = LocalContext.current
    
    // States for custom configurations
    var selectedTheme by remember { mutableStateOf(prefs.theme) }
    var heightPortrait by remember { mutableFloatStateOf(prefs.heightPortrait) }
    var heightLandscape by remember { mutableFloatStateOf(prefs.heightLandscape) }
    var vibrationMs by remember { mutableFloatStateOf(prefs.vibrationMs.toFloat()) }
    var soundEnabled by remember { mutableStateOf(prefs.isSoundEnabled) }
    var vibrationEnabled by remember { mutableStateOf(prefs.isVibrationEnabled) }
    var popupEnabled by remember { mutableStateOf(prefs.isPopupEnabled) }
    var suggestionStripEnabled by remember { mutableStateOf(prefs.isSuggestionStripEnabled) }

    // API Configurations
    var groqApiKey by remember { mutableStateOf(prefs.groqApiKey) }
    var aiModelState by remember { mutableStateOf(prefs.aiModel) }
    var apiVisible by remember { mutableStateOf(false) }

    // Custom learning dictionary entries
    var customWordInput by remember { mutableStateOf("") }
    val customWords = remember { mutableStateListOf<DictionaryEntry>() }

    // Onboarding validations
    var isKeyboardEnabled by remember { mutableStateOf(false) }
    var isKeyboardSelected by remember { mutableStateOf(false) }

    // Pull database updates with coroutines
    LaunchedEffect(Unit) {
        repository.allDictionary.collect { list ->
            customWords.clear()
            customWords.addAll(list)
        }
    }

    // Checking system keyboard status
    LaunchedEffect(Unit) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        if (imm != null) {
            val enabledList = imm.enabledInputMethodList
            isKeyboardEnabled = enabledList.any { it.packageName == context.packageName }
            
            val activeId = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            isKeyboardSelected = activeId != null && activeId.contains(context.packageName)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAF9F6))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // STEP 1: Enable System IME configuration Wizard
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SettingsSuggest,
                            contentDescription = "IME Onboard Info",
                            tint = Color(0xFFE53170),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Set Up Keyboard (কীবোর্ড চালু করুন)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F0E17)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    StatusStepRow(
                        stepNum = "1",
                        title = "Enable in System Settings",
                        status = isKeyboardEnabled,
                        onAction = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                                Toast.makeText(context, "Enable 'Bangla AI Keyboard' switch", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Please open keyboard settings manually.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    StatusStepRow(
                        stepNum = "2",
                        title = "Select as Default Input Method",
                        status = isKeyboardSelected,
                        onAction = {
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            if (imm != null) {
                                imm.showInputMethodPicker()
                            } else {
                                Toast.makeText(context, "Unsupported on this device.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        // STEP 2: Groq gpt-oss-120b API Configuration Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = "Groq Setup Icon",
                            tint = Color(0xFF12B76A),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Groq Key Setup (এআই চ্যাট সেটিংস)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F0E17)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Paste your Groq API key here. Tap the AI button on your keyboard to chat natively while typing.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = groqApiKey,
                        onValueChange = {
                            groqApiKey = it
                            prefs.groqApiKey = it
                        },
                        label = { Text("Groq Bearer API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (apiVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { apiVisible = !apiVisible }) {
                                Icon(
                                    imageVector = if (apiVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle visibility"
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF12B76A)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = aiModelState,
                        onValueChange = {
                            aiModelState = it
                            prefs.aiModel = it
                        },
                        label = { Text("AI Model ID") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF12B76A)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Default Model: openai/gpt-oss-120b",
                        fontSize = 11.sp,
                        color = Color(0xFF12B76A),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            aiModelState = "openai/gpt-oss-120b"
                            prefs.aiModel = "openai/gpt-oss-120b"
                        }
                    )
                }
            }
        }

        // STEP 3: Customize Heights, Feeds and Sizes
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Haptics setup",
                            tint = Color(0xFF1570EF),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Haptics & Custom Sizing (কীবোর্ড সাইজ ও ফিডব্যাক)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F0E17)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Heights Sliders
                    Text(text = "Portrait Height Multiplier: ${String.format("%.2f", heightPortrait)}x", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = heightPortrait,
                        onValueChange = {
                            heightPortrait = it
                            prefs.heightPortrait = it
                        },
                        valueRange = 0.7f..1.5f,
                        colors = SliderDefaults.colors(activeTrackColor = Color(0xFF1570EF))
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Landscape Height Multiplier: ${String.format("%.2f", heightLandscape)}x", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = heightLandscape,
                        onValueChange = {
                            heightLandscape = it
                            prefs.heightLandscape = it
                        },
                        valueRange = 0.7f..1.5f,
                        colors = SliderDefaults.colors(activeTrackColor = Color(0xFF1570EF))
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Vibration Duration: ${vibrationMs.toInt()} ms", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = vibrationMs,
                        onValueChange = {
                            vibrationMs = it
                            prefs.vibrationMs = it.toInt()
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(activeTrackColor = Color(0xFF1570EF))
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Switches
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Key vibration feedback", fontSize = 13.sp)
                        Switch(
                            checked = vibrationEnabled,
                            onCheckedChange = {
                                vibrationEnabled = it
                                prefs.isVibrationEnabled = it
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Keypress sound clicks", fontSize = 13.sp)
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = {
                                soundEnabled = it
                                prefs.isSoundEnabled = it
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Key hover popup previews", fontSize = 13.sp)
                        Switch(
                            checked = popupEnabled,
                            onCheckedChange = {
                                popupEnabled = it
                                prefs.isPopupEnabled = it
                            }
                        )
                    }
                }
            }
        }

        // STEP 4: Keyboard Theme Store Dashboard
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Themes Store info",
                            tint = Color(0xFF7433FF),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Keyboard Theme Store (কীবোর্ড থিম স্টোর)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F0E17)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(KeyboardThemes.themes.keys.toList()) { themeName ->
                            val isSelected = selectedTheme == themeName
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF7433FF) else Color(0xFFF3F4F6)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clickable {
                                        selectedTheme = themeName
                                        prefs.theme = themeName
                                        Toast.makeText(context, "Theme updated to $themeName", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Text(
                                    text = themeName,
                                    color = if (isSelected) Color.White else Color(0xFF2E2D30),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // STEP 5: Bangla Custom Dictionary Learning Board
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Dictionary Store",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Custom learned Dictionary (শব্দকোষ)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F0E17)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Add typical or personal words for fast automatic predictions.", fontSize = 11.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = customWordInput,
                            onValueChange = { customWordInput = it },
                            placeholder = { Text("নতুন শব্দ লিখুন...") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF9800)
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (customWordInput.isNotBlank()) {
                                    val newEntry = DictionaryEntry(word = customWordInput, language = "bn")
                                    context.getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE)
                                    // save to DB
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                        repository.insertWord(newEntry)
                                    }
                                    customWordInput = ""
                                    Toast.makeText(context, "Word learned successfully!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Text("যোগ করুন")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Saved custom word list:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    
                    if (customWords.isEmpty()) {
                        Text(text = "Dictionary is blank. Add your custom words.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                    } else {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            customWords.take(15).forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = item.word, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = {
                                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                                repository.deleteWord(item.word)
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete from dictionary", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // STEP 6: Live interactive Keyboard layout Playground
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEDF4FE)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🚀 Live Keyboard Playground (টাইপ টেস্ট)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1570EF)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Tap inside the input box below to open the 'Bangla AI Keyboard' and write dynamically! See and test haptics, voice and AI instantly.", fontSize = 11.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(12.dp))

                    var testInput by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = testInput,
                        onValueChange = { testInput = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        placeholder = { Text("এখানে ক্লিক করে কীবোর্ড পরীক্ষা করুন...", color = Color.DarkGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF1570EF)
                        )
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "🛡️ Private and Secure local processing. Ridmik-inspired user privacy protocols: No keystroke logs or private typed data is ever transmitted or collected.",
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = Color.Gray
            )
        }
    }
}

@Composable
fun StatusStepRow(stepNum: String, title: String, status: Boolean, onAction: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF9FAFB))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (status) Color(0xFF12B76A) else Color(0xFFF04438)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = stepNum, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF344054))
            Text(
                text = if (status) "Status: Enabled" else "Status: Click to configure",
                fontSize = 11.sp,
                color = if (status) Color(0xFF12B76A) else Color(0xFFF04438)
            )
        }

        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (status) Color(0xFFD1FADF) else Color(0xFFFECDCA),
                contentColor = if (status) Color(0xFF027A48) else Color(0xFFB42318)
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text(text = if (status) "Active" else "Setup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
