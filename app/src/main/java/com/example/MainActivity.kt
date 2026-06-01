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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.scale
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
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF121212),
                    topBar = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFF1C1B1F),
                                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                                )
                                .statusBarsPadding()
                                .padding(vertical = 18.dp, horizontal = 20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Bangla AI ",
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Pro",
                                            color = Color(0xFFA855F7),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "ADVANCED INPUT SYSTEM",
                                        color = Color(0xFF64748B),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFF25232A), shape = RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFF334155), shape = RoundedCornerShape(12.dp))
                                        .clickable {
                                            try {
                                                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                                imm?.showInputMethodPicker()
                                            } catch (e: Exception) {
                                                Toast.makeText(this@MainActivity, "Picker not supported.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "IME Settings Picker",
                                        tint = Color(0xFFA855F7),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
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

    // Active Layout Selection state
    var activeLayout by remember { mutableStateOf(prefs.activeLayout) }

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
            .background(Color(0xFF121212))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // STEP 1: Enable System IME configuration Wizard
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF2D2A37)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF3B0764), shape = RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SettingsSuggest,
                                contentDescription = "IME Onboard Info",
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Set Up Keyboard",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "কীবোর্ড সচল করার ধাপসমূহ",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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

                    Spacer(modifier = Modifier.height(12.dp))

                    StatusStepRow(
                        stepNum = "2",
                        title = "Select as Default Input",
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

        // STEP 2: Groq Powered AI Chat Bento Card (Large)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF3B0764).copy(alpha = 0.45f), Color(0xFF1E1B4B).copy(alpha = 0.45f))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF581C87), shape = RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Android,
                                    contentDescription = "Groq Setup Icon",
                                    tint = Color(0xFFC084FC),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Smart AI Chat Configuration",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "এআই চ্যাট সেটিংস ও মডেল আইডি",
                                    fontSize = 11.sp,
                                    color = Color(0xFFC084FC)
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color(0xFF581C87).copy(alpha = 0.5f))
                                .border(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f), RoundedCornerShape(100.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Groq Powered",
                                color = Color(0xFFD8B4FE),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Paste your Groq API key below. You can trigger the native AI chat in your keyboard view at any time while typing.",
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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
                                    contentDescription = "Toggle visibility",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFA855F7),
                            unfocusedBorderColor = Color(0xFF2D2A37),
                            focusedContainerColor = Color(0xFF121212).copy(alpha = 0.5f),
                            unfocusedContainerColor = Color(0xFF121212).copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFFA855F7),
                            unfocusedLabelColor = Color(0xFF64748B)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = aiModelState,
                        onValueChange = {
                            aiModelState = it
                            prefs.aiModel = it
                        },
                        label = { Text("AI Model ID") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFA855F7),
                            unfocusedBorderColor = Color(0xFF2D2A37),
                            focusedContainerColor = Color(0xFF121212).copy(alpha = 0.5f),
                            unfocusedContainerColor = Color(0xFF121212).copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFFA855F7),
                            unfocusedLabelColor = Color(0xFF64748B)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Default Model: openai/gpt-oss-120b",
                        fontSize = 11.sp,
                        color = Color(0xFFC084FC),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            aiModelState = "openai/gpt-oss-120b"
                            prefs.aiModel = "openai/gpt-oss-120b"
                        }
                    )
                }
            }
        }

        // STEP 3: Bento Grid Layout Asymmetric Row (Side-by-Side Cards)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Column: Active Layout Switcher Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFF2D2A37)),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(220.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SYSTEM LAYOUT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Phonetic", "Probhat", "English", "National").forEach { layout ->
                                val isSelected = activeLayout == layout
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFF25232A) else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF4ADE80).copy(alpha = 0.3f) else Color.Transparent,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            activeLayout = layout
                                            prefs.activeLayout = layout
                                            Toast.makeText(context, "$layout layout set as default", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSelected) Color(0xFF4ADE80) else Color(0xFF475569))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = layout,
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Column: Expressive/Feature Switches Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFF2D2A37)),
                    modifier = Modifier
                        .weight(1f)
                        .height(220.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PREFEEDS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Sound click toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Sounds", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = soundEnabled,
                                    onCheckedChange = {
                                        soundEnabled = it
                                        prefs.isSoundEnabled = it
                                    },
                                    modifier = Modifier.scale(0.75f)
                                )
                            }

                            // Vibration toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Haptic", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = vibrationEnabled,
                                    onCheckedChange = {
                                        vibrationEnabled = it
                                        prefs.isVibrationEnabled = it
                                    },
                                    modifier = Modifier.scale(0.75f)
                                )
                            }

                            // Popup previews toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Popups", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = popupEnabled,
                                    onCheckedChange = {
                                        popupEnabled = it
                                        prefs.isPopupEnabled = it
                                    },
                                    modifier = Modifier.scale(0.75f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // STEP 4: Customize Heights & Sizing Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF2D2A37)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF1E293B), shape = RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Haptics setup",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Haptics & Custom Sizing",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "কীবোর্ড আকার ও স্পন্দনকাল নির্ধারণ",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Portrait Height Multiplier: ${String.format("%.2f", heightPortrait)}x",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Slider(
                        value = heightPortrait,
                        onValueChange = {
                            heightPortrait = it
                            prefs.heightPortrait = it
                        },
                        valueRange = 0.7f..1.5f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFF38BDF8),
                            thumbColor = Color(0xFF38BDF8)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Landscape Height Multiplier: ${String.format("%.2f", heightLandscape)}x",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Slider(
                        value = heightLandscape,
                        onValueChange = {
                            heightLandscape = it
                            prefs.heightLandscape = it
                        },
                        valueRange = 0.7f..1.5f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFF38BDF8),
                            thumbColor = Color(0xFF38BDF8)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Vibration Duration: ${vibrationMs.toInt()} ms",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Slider(
                        value = vibrationMs,
                        onValueChange = {
                            vibrationMs = it
                            prefs.vibrationMs = it.toInt()
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFF38BDF8),
                            thumbColor = Color(0xFF38BDF8)
                        )
                    )
                }
            }
        }

        // STEP 5: Keyboard Theme Store Dashboard
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF2D2A37)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF1E293B), shape = RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Themes Store info",
                                tint = Color(0xFFC084FC),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Keyboard Theme Store",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "কীবোর্ড থিম সিলেক্ট করুন",
                                fontSize = 11.sp,
                                color = Color(0xFFC084FC)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(KeyboardThemes.themes.keys.toList()) { themeName ->
                            val isSelected = selectedTheme == themeName
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFA855F7) else Color(0xFF25232A)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF2D2A37)),
                                modifier = Modifier
                                    .clickable {
                                        selectedTheme = themeName
                                        prefs.theme = themeName
                                        Toast.makeText(context, "Theme updated to $themeName", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Text(
                                    text = themeName,
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // STEP 6: Custom learned Dictionary Learning Board
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF2D2A37)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF332005), shape = RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Dictionary Store",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Custom Learned Dictionary",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "নতুন বা ব্যক্তিগত শব্দকোষের তালিকা",
                                fontSize = 11.sp,
                                color = Color(0xFFF59E0B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Add typical or personal words for fast automatic predictions.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customWordInput,
                            onValueChange = { customWordInput = it },
                            placeholder = { Text("নতুন শব্দ লিখুন...", color = Color(0xFF64748B)) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFF59E0B),
                                unfocusedBorderColor = Color(0xFF2D2A37),
                                focusedContainerColor = Color(0xFF121212),
                                unfocusedContainerColor = Color(0xFF121212)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (customWordInput.isNotBlank()) {
                                    val newEntry = DictionaryEntry(word = customWordInput, language = "bn")
                                    context.getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE)
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                        repository.insertWord(newEntry)
                                    }
                                    customWordInput = ""
                                    Toast.makeText(context, "Word learned successfully!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("যোগ করুন", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Saved custom word list:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))

                    if (customWords.isEmpty()) {
                        Text(
                            text = "Dictionary is blank. Add your custom words.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            customWords.take(15).forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(Color(0xFF25232A), shape = RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.word,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    IconButton(
                                        onClick = {
                                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                                repository.deleteWord(item.word)
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete from dictionary",
                                            tint = Color(0xFFF87171),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // STEP 7: Live interactive Keyboard layout Playground
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF0F172A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "🚀 Live Keyboard Playground",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap inside the input box below to open 'Bangla AI Keyboard' and write dynamically! Verify the layout, haptics, and instant AI lookup.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    var testInput by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = testInput,
                        onValueChange = { testInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text("এখানে ক্লিক করে কীবোর্ড পরীক্ষা করুন...", color = Color(0xFF64748B)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF0F172A),
                            focusedContainerColor = Color(0xFF121212),
                            unfocusedContainerColor = Color(0xFF121212)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "🛡️ Private and Secure local processing. Ridmik-inspired user privacy protocols: No keystroke logs or private typed data is ever transmitted or collected.",
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF64748B),
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun StatusStepRow(stepNum: String, title: String, status: Boolean, onAction: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF25232A))
            .border(1.dp, Color(0xFF2D2A37), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (status) Color(0xFF10B981) else Color(0xFFEF4444)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = stepNum, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(
                text = if (status) "Status: Active" else "Status: Click to setup",
                fontSize = 11.sp,
                color = if (status) Color(0xFF10B981) else Color(0xFFEF4444)
            )
        }

        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (status) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                contentColor = if (status) Color(0xFF34D399) else Color(0xFFFCA5A5)
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text(text = if (status) "Active" else "Setup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
