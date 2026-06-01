package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.*
import com.example.data.*
import com.example.ui.theme.KeyboardThemeColors
import com.example.ui.theme.KeyboardThemes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KeyboardService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val myViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = myViewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var prefs: PreferenceManager
    private lateinit var database: AppDatabase
    private lateinit var repository: KeyboardRepository

    private var audioManager: AudioManager? = null
    private var vibrator: Vibrator? = null
    private var speechRecognizer: SpeechRecognizer? = null

    // Keyboard states monitored inside Compose UI
    private val activeLayoutState = mutableStateOf("Phonetic")
    private val themeNameState = mutableStateOf("Cosmic Dark")
    private val vibrationMsState = mutableIntStateOf(40)
    private val isSoundEnabledState = mutableStateOf(true)
    private val isPopupEnabledState = mutableStateOf(true)
    private val heightPortraitState = mutableFloatStateOf(1.0f)

    // Clipboard reactive updates
    private val localClipboardItems = mutableStateListOf<ClipboardItem>()

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        prefs = PreferenceManager(this)
        database = AppDatabase.getDatabase(this)
        repository = KeyboardRepository(database)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        // Initialize state levels from configs
        activeLayoutState.value = prefs.activeLayout
        themeNameState.value = prefs.theme
        vibrationMsState.intValue = prefs.vibrationMs
        isSoundEnabledState.value = prefs.isVibrationEnabled
        isPopupEnabledState.value = prefs.isPopupEnabled
        heightPortraitState.value = prefs.heightPortrait

        // Observe local clipboard history from DB
        CoroutineScope(Dispatchers.IO).launch {
            repository.allClipboardItems.collectLatest { list ->
                withContext(Dispatchers.Main) {
                    localClipboardItems.clear()
                    localClipboardItems.addAll(list)
                }
            }
        }
    }

    override fun onWindowShown() {
        super.onWindowShown()
        // Refresh preferences dynamically
        activeLayoutState.value = prefs.activeLayout
        themeNameState.value = prefs.theme
        vibrationMsState.intValue = prefs.vibrationMs
        isSoundEnabledState.value = prefs.isVibrationEnabled
        isPopupEnabledState.value = prefs.isPopupEnabled
        heightPortraitState.value = prefs.heightPortrait

        // Pull standard device clipboard and record to database for easy access
        try {
            val cb = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (cb != null && cb.hasPrimaryClip()) {
                val clipData = cb.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0).text?.toString() ?: ""
                    if (text.isNotBlank()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val currentList = localClipboardItems
                            if (currentList.none { it.text == text }) {
                                repository.insertClipboard(ClipboardItem(text = text))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@KeyboardService)
            setViewTreeViewModelStoreOwner(this@KeyboardService)
            setViewTreeSavedStateRegistryOwner(this@KeyboardService)
        }

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        composeView.setContent {
            val colors = KeyboardThemes.getColors(themeNameState.value)
            
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    background = colors.background,
                    surface = colors.keyBackground,
                    onSurface = colors.keyLabel,
                    primary = colors.accentColor
                )
            ) {
                KeyboardLayoutContent(colors)
            }
        }

        return composeView
    }

    private fun handleFeedback() {
        if (isSoundEnabledState.value) {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
        }
        if (prefs.isVibrationEnabled) {
            val ms = vibrationMsState.intValue.toLong()
            if (ms > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(ms)
                }
            }
        }
    }

    @Composable
    fun KeyboardLayoutContent(colors: KeyboardThemeColors) {
        var shiftOn by remember { mutableStateOf(false) }
        var currentSubmode by remember { mutableStateOf("KEYS") } // KEYS, EMOJI, CLIPBOARD, TEXT_EDIT, AI_CHAT, VOICE
        var voiceLanguage by remember { mutableStateOf("bn-BD") } // bn-BD or en-US
        var voiceStatus by remember { mutableStateOf("Tap mic to speak...") }
        var voiceIsListening by remember { mutableStateOf(false) }
        
        // Typing buffers for phonetic translation (Avro Style)
        var phoneticBuffer by remember { mutableStateOf("") }

        // AI Chat status support (persists in state)
        var aiQuery by remember { mutableStateOf("") }
        var aiReply by remember { mutableStateOf("") }
        var aiLoading by remember { mutableStateOf(false) }

        val keyboardHeight = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
            (260 * heightPortraitState.value).dp
        } else {
            (180 * prefs.heightLandscape).dp
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(bottom = 8.dp)
        ) {
            // Suggestion / Tool bar row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(colors.background.copy(alpha = 0.9f))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // If there's phonetic typing, show real transliterated suggestions!
                if (phoneticBuffer.isNotEmpty() && activeLayoutState.value == "Phonetic") {
                    val converted = BanglaPhoneticParser.parse(phoneticBuffer)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.accentColor),
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                handleFeedback()
                                commitBuffer(converted)
                                phoneticBuffer = ""
                            }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "আমি টাইপ: \"$converted\"",
                                color = colors.specialKeyLabel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    // Show a secondary direct input suggestion as well
                    IconButton(
                        onClick = {
                            handleFeedback()
                            commitBuffer(phoneticBuffer)
                            phoneticBuffer = ""
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel phonetic transliteration",
                            tint = colors.keyLabel
                        )
                    }
                } else {
                    // Toolbar Mode Toggle keys
                    IconButton(onClick = { handleFeedback(); currentSubmode = "KEYS" }) {
                        Icon(Icons.Default.Keyboard, contentDescription = "Standard Keys Mode", tint = if (currentSubmode == "KEYS") colors.accentColor else colors.keyLabel)
                    }
                    IconButton(onClick = { handleFeedback(); currentSubmode = "AI_CHAT" }) {
                        Icon(Icons.Default.Android, contentDescription = "AI Assistant chat", tint = if (currentSubmode == "AI_CHAT") colors.accentColor else colors.keyLabel)
                    }
                    IconButton(onClick = { handleFeedback(); currentSubmode = "VOICE" }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice Typing", tint = if (currentSubmode == "VOICE") colors.accentColor else colors.keyLabel)
                    }
                    IconButton(onClick = { handleFeedback(); currentSubmode = "EMOJI" }) {
                        Icon(Icons.Default.SentimentSatisfiedAlt, contentDescription = "Emoji Dashboard", tint = if (currentSubmode == "EMOJI") colors.accentColor else colors.keyLabel)
                    }
                    IconButton(onClick = { handleFeedback(); currentSubmode = "CLIPBOARD" }) {
                        Icon(Icons.Default.Assignment, contentDescription = "Clipboard history manager", tint = if (currentSubmode == "CLIPBOARD") colors.accentColor else colors.keyLabel)
                    }
                    IconButton(onClick = { handleFeedback(); currentSubmode = "TEXT_EDIT" }) {
                        Icon(Icons.Default.OpenWith, contentDescription = "Navigation cursor controls", tint = if (currentSubmode == "TEXT_EDIT") colors.accentColor else colors.keyLabel)
                    }

                    // Spacer push
                    Spacer(modifier = Modifier.weight(1f))

                    // Layout display label (easy quick button to cycle languages)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.keyBackground),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                handleFeedback()
                                cycleLanguage()
                            }
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = activeLayoutState.value,
                            color = colors.keyLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Central board based on submode selection
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(keyboardHeight)
            ) {
                AnimatedContent(
                    targetState = currentSubmode,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "SubmodeBoard"
                ) { targetMode ->
                    when (targetMode) {
                        "KEYS" -> {
                            KeysGridBoard(
                                colors = colors,
                                shiftOn = shiftOn,
                                phoneticBuffer = phoneticBuffer,
                                onPhoneticUpdate = { phoneticBuffer = it },
                                onShiftChange = { shiftOn = it },
                                onAction = { action ->
                                    handleKeyboardAction(action, colors)
                                }
                            )
                        }
                        "EMOJI" -> {
                            EmojiGridBoard(colors = colors) { emoji ->
                                handleFeedback()
                                currentInputConnection?.commitText(emoji, 1)
                            }
                        }
                        "CLIPBOARD" -> {
                            ClipboardBoard(colors = colors, items = localClipboardItems) { tappedText ->
                                handleFeedback()
                                currentInputConnection?.commitText(tappedText, 1)
                            }
                        }
                        "TEXT_EDIT" -> {
                            NavigationTextEditingBoard(colors = colors)
                        }
                        "AI_CHAT" -> {
                            AiChatOverlay(
                                colors = colors,
                                queryValue = aiQuery,
                                onQueryChange = { aiQuery = it },
                                replyValue = aiReply,
                                isLoading = aiLoading,
                                onSend = {
                                    aiLoading = true
                                    CoroutineScope(Dispatchers.Main).launch {
                                        val key = prefs.groqApiKey
                                        val model = prefs.aiModel
                                        aiReply = GroqService.getAiResponse(key, model, aiQuery)
                                        aiLoading = false
                                    }
                                },
                                onPaste = {
                                    handleFeedback()
                                    currentInputConnection?.commitText(aiReply, 1)
                                },
                                onAskSelected = {
                                    handleFeedback()
                                    val conn = currentInputConnection
                                    if (conn != null) {
                                        val extracted = conn.getExtractedText(ExtractedTextRequest(), 0)
                                        val text = extracted?.text?.toString() ?: ""
                                        if (text.isNotBlank()) {
                                            aiQuery = text
                                        }
                                    }
                                }
                            )
                        }
                        "VOICE" -> {
                            VoiceTypingOverlay(
                                colors = colors,
                                statusMsg = voiceStatus,
                                isListening = voiceIsListening,
                                language = voiceLanguage,
                                onLangToggle = {
                                    voiceLanguage = if (voiceLanguage == "bn-BD") "en-US" else "bn-BD"
                                },
                                onStartListening = {
                                    voiceIsListening = true
                                    voiceStatus = "Listening..."
                                    startVoiceRecognizer(voiceLanguage) { result ->
                                        currentInputConnection?.commitText(result, 1)
                                        voiceIsListening = false
                                        voiceStatus = "Finished: \"$result\""
                                    }
                                },
                                onStopListening = {
                                    try {
                                        speechRecognizer?.stopListening()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    voiceIsListening = false
                                    voiceStatus = "Stopped."
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startVoiceRecognizer(lang: String, onResultText: (String) -> Unit) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this@KeyboardService)
                }
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        onResultText("No speech recognized.")
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onResultText(matches[0])
                        } else {
                            onResultText("Speech recognizer blank.")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                onResultText("Speech Recognizer unavailable (Mic permission required).")
            }
        }
    }

    private fun cycleLanguage() {
        val list = listOf("English", "Phonetic", "Probhat", "National", "Arabic", "Chakma")
        val currentIndex = list.indexOf(activeLayoutState.value)
        val nextIndex = (currentIndex + 1) % list.size
        activeLayoutState.value = list[nextIndex]
        prefs.activeLayout = list[nextIndex]
    }

    private fun handleKeyboardAction(key: String, colors: KeyboardThemeColors) {
        handleFeedback()
        val conn = currentInputConnection ?: return

        when (key) {
            "DEL" -> {
                conn.deleteSurroundingText(1, 0)
            }
            "SPACE" -> {
                conn.commitText(" ", 1)
            }
            "ENTER" -> {
                conn.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                conn.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            else -> {
                conn.commitText(key, 1)
            }
        }
    }

    private fun commitBuffer(text: String) {
        val conn = currentInputConnection ?: return
        conn.commitText("$text ", 1) // Commit written translation with space ending
    }

    // Individual Submode Layout Composables
    @Composable
    fun KeysGridBoard(
        colors: KeyboardThemeColors,
        shiftOn: Boolean,
        phoneticBuffer: String,
        onPhoneticUpdate: (String) -> Unit,
        onShiftChange: (Boolean) -> Unit,
        onAction: (String) -> Unit
    ) {
        val rows = getKeysForActiveLayout(shiftOn)
        
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            rows.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowKeys.forEach { keyLabel ->
                        val isSpecial = keyLabel == "SHIFT" || keyLabel == "DEL" || keyLabel == "LANG" || keyLabel == "VOICE" || keyLabel == "CLIP" || keyLabel == "AI" || keyLabel == "ENTER" || keyLabel == "SPACE"
                        
                        val weight = when (keyLabel) {
                            "SPACE" -> 4f
                            "ENTER" -> 1.5f
                            "DEL" -> 1.5f
                            "SHIFT" -> 1.5f
                            else -> 1f
                        }

                        // Spacebar drag gesture to move cursor left/right
                        val dragModifier = if (keyLabel == "SPACE") {
                            var accumulatedX = 0f
                            Modifier.pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { accumulatedX = 0f },
                                    onDrag = { _, dragAmount ->
                                        accumulatedX += dragAmount.x
                                        val threshold = 40f
                                        if (accumulatedX > threshold) {
                                            moveCursor(1)
                                            accumulatedX = 0f
                                        } else if (accumulatedX < -threshold) {
                                            moveCursor(-1)
                                            accumulatedX = 0f
                                        }
                                    }
                                )
                            }
                        } else Modifier

                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .height(52.dp)
                                .padding(horizontal = 2.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSpecial) colors.specialKeyBackground else colors.keyBackground)
                                .clickable {
                                    if (keyLabel == "SHIFT") {
                                        onShiftChange(!shiftOn)
                                    } else if (keyLabel == "LANG") {
                                        cycleLanguage()
                                    } else if (activeLayoutState.value == "Phonetic" && !isSpecial) {
                                        onPhoneticUpdate(phoneticBuffer + keyLabel)
                                    } else {
                                        onAction(keyLabel)
                                    }
                                }
                                .then(dragModifier),
                            contentAlignment = Alignment.Center
                        ) {
                            when (keyLabel) {
                                "DEL" -> Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = colors.specialKeyLabel, modifier = Modifier.size(20.dp))
                                "SHIFT" -> Icon(Icons.Default.ArrowUpward, contentDescription = "Shift", tint = if (shiftOn) colors.accentColor else colors.specialKeyLabel, modifier = Modifier.size(20.dp))
                                "LANG" -> Icon(Icons.Default.Language, contentDescription = "Select language layout", tint = colors.specialKeyLabel, modifier = Modifier.size(20.dp))
                                "VOICE" -> Icon(Icons.Default.Mic, contentDescription = "Voice key", tint = colors.specialKeyLabel)
                                "CLIP" -> Icon(Icons.Default.Assignment, contentDescription = "Clipboard history shortcut", tint = colors.specialKeyLabel, modifier = Modifier.size(18.dp))
                                "AI" -> Icon(Icons.Default.Android, contentDescription = "Instant AI key", tint = colors.specialKeyLabel, modifier = Modifier.size(18.dp))
                                "SPACE" -> Text(text = "SPACE (⇆ Drag to slide cursor)", color = colors.specialKeyLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                else -> Text(text = keyLabel, color = if (isSpecial) colors.specialKeyLabel else colors.keyLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun moveCursor(offset: Int) {
        val conn = currentInputConnection ?: return
        try {
            val extracted = conn.getExtractedText(ExtractedTextRequest(), 0)
            if (extracted != null) {
                val start = extracted.selectionStart
                val end = extracted.selectionEnd
                if (start >= 0) {
                    conn.setSelection(start + offset, end + offset)
                }
            } else {
                val keyEvent = if (offset > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
                conn.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyEvent))
                conn.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyEvent))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getKeysForActiveLayout(shift: Boolean): List<List<String>> {
        return when (activeLayoutState.value) {
            "Phonetic", "English" -> if (shift) KeyboardLayouts.ENGLISH_SHIFT_ROWS else KeyboardLayouts.ENGLISH_ROWS
            "Probhat" -> if (shift) KeyboardLayouts.PROBHAT_SHIFT_ROWS else KeyboardLayouts.PROBHAT_ROWS
            "National" -> if (shift) KeyboardLayouts.NATIONAL_SHIFT_ROWS else KeyboardLayouts.NATIONAL_ROWS
            "Arabic" -> if (shift) KeyboardLayouts.ARABIC_SHIFT_ROWS else KeyboardLayouts.ARABIC_ROWS
            "Chakma" -> if (shift) KeyboardLayouts.CHAKMA_SHIFT_ROWS else KeyboardLayouts.CHAKMA_ROWS
            else -> KeyboardLayouts.ENGLISH_ROWS
        }
    }

    @Composable
    fun EmojiGridBoard(colors: KeyboardThemeColors, onEmojiClick: (String) -> Unit) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "😀 Full Emoji Collection & Quick Sticker Shortcuts",
                color = colors.keyLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 4.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 48.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp)
            ) {
                items(KeyboardLayouts.FULL_EMOJIS) { emoji ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.keyBackground)
                            .clickable { onEmojiClick(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 20.sp)
                    }
                }
            }
        }
    }

    @Composable
    fun ClipboardBoard(colors: KeyboardThemeColors, items: List<ClipboardItem>, onInsert: (String) -> Unit) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 Local Copied Clipboard History Manager",
                    color = colors.keyLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Clear All",
                    color = colors.accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        handleFeedback()
                        CoroutineScope(Dispatchers.IO).launch {
                            repository.clearClipboard()
                        }
                    }
                )
            }

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No clipboard entries found.\nCopied texts will automatically appear here.", textAlign = TextAlign.Center, color = colors.keyLabel.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                    items(items) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = colors.keyBackground),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onInsert(item.text) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (item.text.length > 50) item.text.substring(0, 47) + "..." else item.text,
                                    color = colors.keyLabel,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        handleFeedback()
                                        CoroutineScope(Dispatchers.IO).launch {
                                            repository.deleteClipboard(item.id)
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete clipboard text row", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun NavigationTextEditingBoard(colors: KeyboardThemeColors) {
        val conn = currentInputConnection
        
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "✂️ Precision Core Text Editing Visual Control Keys",
                color = colors.keyLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = {
                        handleFeedback()
                        conn?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
                        conn?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.keyBackground)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Left", tint = colors.keyLabel)
                }

                Button(
                    onClick = {
                        handleFeedback()
                        conn?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP))
                        conn?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.keyBackground)
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Up", tint = colors.keyLabel)
                }

                Button(
                    onClick = {
                        handleFeedback()
                        conn?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN))
                        conn?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.keyBackground)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Down", tint = colors.keyLabel)
                }

                Button(
                    onClick = {
                        handleFeedback()
                        conn?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
                        conn?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.keyBackground)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Right", tint = colors.keyLabel)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = {
                        handleFeedback()
                        conn?.performContextMenuAction(android.R.id.selectAll)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.specialKeyBackground)
                ) {
                    Text("Select All", color = colors.specialKeyLabel, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        handleFeedback()
                        conn?.performContextMenuAction(android.R.id.copy)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.specialKeyBackground)
                ) {
                    Text("Copy", color = colors.specialKeyLabel, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        handleFeedback()
                        conn?.performContextMenuAction(android.R.id.cut)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.specialKeyBackground)
                ) {
                    Text("Cut", color = colors.specialKeyLabel, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        handleFeedback()
                        conn?.performContextMenuAction(android.R.id.paste)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.specialKeyBackground)
                ) {
                    Text("Paste", color = colors.specialKeyLabel, fontSize = 11.sp)
                }
            }
        }
    }

    @Composable
    fun AiChatOverlay(
        colors: KeyboardThemeColors,
        queryValue: String,
        onQueryChange: (String) -> Unit,
        replyValue: String,
        isLoading: Boolean,
        onSend: () -> Unit,
        onPaste: () -> Unit,
        onAskSelected: () -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Android, contentDescription = "AI Icon", tint = colors.accentColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Groq Keyboard Assistant (${prefs.aiModel})", color = colors.keyLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Scrollable response box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.keyBackground)
                    .padding(8.dp)
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.accentColor, modifier = Modifier.size(24.dp))
                    }
                } else if (replyValue.isBlank()) {
                    Text(
                        text = "Reply outputs will be shown here.\nType a quick prompt or fetch the text currently stored in the input stream with the button below.",
                        color = colors.keyLabel.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = replyValue,
                            color = colors.keyLabel,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = queryValue,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Ask anything...", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f).height(42.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.keyLabel,
                        unfocusedTextColor = colors.keyLabel,
                        focusedContainerColor = colors.keyBackground,
                        unfocusedContainerColor = colors.keyBackground,
                        focusedBorderColor = colors.accentColor,
                        unfocusedBorderColor = colors.keyLabel.copy(alpha = 0.3f)
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onSend,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(colors.accentColor)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Query Groq API", tint = colors.specialKeyLabel, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onAskSelected) {
                    Text("Get Selected Text as Query", color = colors.accentColor, fontSize = 11.sp)
                }
                TextButton(onClick = onPaste) {
                    Text("Insert Response Into App Input", color = colors.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    @Composable
    fun VoiceTypingOverlay(
        colors: KeyboardThemeColors,
        statusMsg: String,
        isListening: Boolean,
        language: String,
        onLangToggle: () -> Unit,
        onStartListening: () -> Unit,
        onStopListening: () -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🎤 Voice Continuous Dictation", color = colors.keyLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.keyBackground),
                    modifier = Modifier.clickable { onLangToggle() }
                ) {
                    Text(
                        text = if (language == "bn-BD") "Language: বাংলা (Bangla)" else "Language: English",
                        color = colors.accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(if (isListening) colors.accentColor else colors.keyBackground)
                    .clickable {
                        handleFeedback()
                        if (isListening) onStopListening() else onStartListening()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Hearing else Icons.Default.Mic,
                    contentDescription = "Trigger Speech Input",
                    tint = if (isListening) colors.specialKeyLabel else colors.keyLabel,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = statusMsg,
                color = colors.keyLabel,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
