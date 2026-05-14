package com.example.nalla_nudi

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nalla_nudi.data.Word
import com.example.nalla_nudi.ui.NallaNudiViewModel
import com.example.nalla_nudi.ui.theme.NallaNudiTheme
import com.example.nalla_nudi.util.TTSManager
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var ttsManager: TTSManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ttsManager = TTSManager(this)
        enableEdgeToEdge()
        setContent {
            NallaNudiTheme {
                val viewModel: NallaNudiViewModel = viewModel()
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                val authMessage by viewModel.authMessage.collectAsState()

                if (isLoggedIn) {
                    MainScreen(viewModel, ttsManager)
                } else {
                    LoginScreen(
                        authMessage = authMessage,
                        onLogin = { name -> viewModel.login(name) },
                        onRegister = { name -> viewModel.register(name) }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }
}

@Composable
fun LoginScreen(
    authMessage: String?,
    onLogin: (String) -> Unit,
    onRegister: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isNewLearner by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Nalla-Nudi", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4CAF50))
        Text(if (isNewLearner) "Create your learner account" else "Login to continue learning", fontSize = 18.sp, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Person, null) }
        )

        authMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                if (isNewLearner) {
                    onRegister(name)
                } else {
                    onLogin(name)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isNewLearner) "Register" else "Login", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { isNewLearner = !isNewLearner }) {
            Text(if (isNewLearner) "Already registered? Login" else "New learner? Register")
        }
    }
}

@Composable
fun MainScreen(viewModel: NallaNudiViewModel, ttsManager: TTSManager) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(navController, startDestination = "home") {
                composable("home") { HomeScreen(viewModel, navController) }
                composable("explore") { ExploreScreen(viewModel, ttsManager, navController) }
                composable("saved") { SavedScreen(viewModel, ttsManager, navController) }
                composable("practice") { PracticeScreen(viewModel, ttsManager) }
                composable("detail/{wordId}") { backStackEntry ->
                    val wordIdStr = backStackEntry.arguments?.getString("wordId")
                    val wordId = wordIdStr?.toIntOrNull()
                    WordDetailScreen(wordId, viewModel, ttsManager, navController)
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Triple("Home", "home", Icons.Default.Home),
        Triple("Explore", "explore", Icons.Default.Search),
        Triple("Saved", "saved", Icons.Default.Favorite),
        Triple("Practice", "practice", Icons.Default.Book)
    )
    NavigationBar(containerColor = Color.White) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.third, contentDescription = item.first) },
                label = { Text(item.first) },
                selected = currentRoute == item.second,
                onClick = {
                    navController.navigate(item.second) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

@Composable
fun HomeScreen(viewModel: NallaNudiViewModel, navController: NavHostController) {
    val userName by viewModel.userName.collectAsState()
    val learnedCount by viewModel.learnedCount.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val wordOfDay by viewModel.wordOfDay.collectAsState()

    val progressValue = if (totalCount > 0) learnedCount.toFloat() / totalCount else 0f

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Brush.horizontalGradient(listOf(Color(0xFF4CAF50), Color(0xFF2196F3))))
                .padding(24.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("WELCOME BACK", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Text("Hi, $userName!", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { viewModel.logout() }) {
                    Icon(Icons.AutoMirrored.Filled.Logout, "Logout", tint = Color.White)
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp).offset(y = (-40).dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier.size(72.dp),
                        strokeWidth = 8.dp,
                        color = Color(0xFF4CAF50),
                        trackColor = Color(0xFFE8F5E9)
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    Column {
                        Text("Learning Progress", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("$learnedCount / $totalCount Words", fontSize = 20.sp, color = Color(0xFF2E7D32))
                        Text("${(progressValue * 100).toInt()}% Complete", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Daily Spotlight", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
            wordOfDay?.let { word ->
                WordCard(
                    word = word, 
                    onSave = { viewModel.toggleSave(word) }, 
                    onClick = { navController.navigate("detail/${word.id}") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Knowledge Domains", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DomainChip("Science", Color(0xFFE3F2FD), Color(0xFF2196F3)) { 
                    viewModel.setCategory("Science")
                    navController.navigate("explore") 
                }
                DomainChip("Math", Color(0xFFF1F8E9), Color(0xFF4CAF50)) { 
                    viewModel.setCategory("Math")
                    navController.navigate("explore") 
                }
                DomainChip("Commerce", Color(0xFFFFFDE7), Color(0xFFFFC107)) { 
                    viewModel.setCategory("Commerce")
                    navController.navigate("explore") 
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { navController.navigate("practice") },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Icon(Icons.Default.Psychology, null)
                Spacer(Modifier.width(12.dp))
                Text("Begin Practice Session", fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun DomainChip(label: String, bgColor: Color, textColor: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            label, 
            color = textColor, 
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), 
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(viewModel: NallaNudiViewModel, ttsManager: TTSManager, navController: NavHostController) {
    val query by viewModel.searchQuery.collectAsState()
    val words by viewModel.filteredWords.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            null
        }
    }

    DisposableEffect(speechRecognizer) {
        if (speechRecognizer != null) {
            val listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() {
                    isListening = false
                }

                override fun onError(error: Int) {
                    isListening = false
                    Toast.makeText(context, "Offline Kannada voice search did not recognize speech.", Toast.LENGTH_SHORT).show()
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val spokenText = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                        .trim()
                    if (spokenText.isNotBlank()) {
                        viewModel.setSearchQuery(spokenText)
                    } else {
                        Toast.makeText(context, "No Kannada speech was recognized offline.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            }
            speechRecognizer.setRecognitionListener(listener)
        }

        onDispose {
            speechRecognizer?.destroy()
        }
    }

    val speechIntent = remember {
        android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "kn-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "kn-IN")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            speechRecognizer?.startListening(speechIntent)
        } else {
            Toast.makeText(context, "Microphone permission is needed for offline voice search.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Explore Terms", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.setSearchQuery(it) },
            label = { Text("Search English or Kannada word") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { 
                Row {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) { 
                            Icon(Icons.Default.Clear, "Clear") 
                        }
                    }
                    IconButton(onClick = {
                        if (speechRecognizer == null) {
                            Toast.makeText(context, "This device has no offline speech recognizer installed.", Toast.LENGTH_SHORT).show()
                        } else if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            speechRecognizer.startListening(speechIntent)
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                            contentDescription = "Kannada Offline Voice Search",
                            tint = if (isListening) Color(0xFF4CAF50) else Color(0xFF2196F3)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(24.dp)
        )

        Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
            val categories = listOf("All", "Science", "Math", "Commerce")
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { viewModel.setCategory(category) },
                    label = { Text(category) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        if (words.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (query.isEmpty()) "Type or tap mic and speak in Kannada" else "No matching word found", color = Color.Gray)
            }
        } else {
            LazyColumn {
                items(words) { word ->
                    WordCard(
                        word = word, 
                        onSave = { viewModel.toggleSave(word) }, 
                        onSpeak = { ttsManager.speak(word.english_term) }, 
                        onClick = { navController.navigate("detail/${word.id}") }
                    )
                }
            }
        }
    }
}

@Composable
fun WordCard(word: Word, onSave: () -> Unit, onSpeak: (() -> Unit)? = null, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(word.english_term, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(word.kannada_meaning, color = Color(0xFF4CAF50), fontSize = 20.sp)
                Text(word.kannada_explanation, fontSize = 14.sp, color = Color.DarkGray, maxLines = 2)
            }
            if (onSpeak != null) {
                IconButton(onClick = onSpeak) { Icon(Icons.Default.VolumeUp, "Speak", tint = Color(0xFF2196F3)) }
            }
            IconButton(onClick = onSave) {
                Icon(
                    imageVector = if (word.is_saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Save",
                    tint = if (word.is_saved) Color.Red else Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailScreen(wordId: Int?, viewModel: NallaNudiViewModel, ttsManager: TTSManager, navController: NavHostController) {
    val words by viewModel.filteredWords.collectAsState()
    val word = words.find { it.id == wordId }

    LaunchedEffect(word) {
        word?.let { viewModel.markAsLearned(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Word Detail") },
                navigationIcon = { 
                    IconButton(onClick = { navController.popBackStack() }) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") 
                    } 
                }
            )
        }
    ) { padding ->
        word?.let {
            Column(modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize()) {
                Text(it.english_term, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp)) {
                    Text(it.kannada_meaning, fontSize = 32.sp, color = Color(0xFF2E7D32), modifier = Modifier.padding(20.dp), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text("Example Usage:", fontWeight = FontWeight.Bold, color = Color.Gray)
                Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                    Text(it.example_sentence, modifier = Modifier.padding(20.dp), fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    LargeFloatingActionButton(onClick = { ttsManager.speak(it.english_term) }, containerColor = Color(0xFF2196F3)) {
                        Icon(Icons.Default.VolumeUp, "Speak", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    LargeFloatingActionButton(onClick = { viewModel.toggleSave(it) }, containerColor = if (it.is_saved) Color.Red else Color.Gray) {
                        Icon(Icons.Default.Favorite, "Save", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SavedScreen(viewModel: NallaNudiViewModel, ttsManager: TTSManager, navController: NavHostController) {
    val savedWords by viewModel.savedWords.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Saved Terms", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        if (savedWords.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No saved words yet ❤️", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(savedWords) { word ->
                    WordCard(
                        word = word, 
                        onSave = { viewModel.toggleSave(word) }, 
                        onSpeak = { ttsManager.speak(word.english_term) }, 
                        onClick = { navController.navigate("detail/${word.id}") }
                    )
                }
            }
            Button(
                onClick = { navController.navigate("practice") }, 
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Start Practice")
            }
        }
    }
}

@Composable
fun PracticeScreen(viewModel: NallaNudiViewModel, ttsManager: TTSManager) {
    val words by viewModel.filteredWords.collectAsState()
    val practiceWords = if (words.size > 10) words.take(10) else words
    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    if (practiceWords.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No words to practice. Explore more!", color = Color.Gray)
        }
    } else {
        val word = practiceWords[currentIndex]
        val rotation by animateFloatAsState(targetValue = if (isFlipped) 180f else 0f, label = "rotation")

        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Practice Mode", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
            Text("Word ${currentIndex + 1} of ${practiceWords.size}", color = Color.Gray)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f
                    }
                    .clickable { 
                        isFlipped = !isFlipped 
                        if (isFlipped) viewModel.markAsLearned(word)
                    },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(2.dp, Color(0xFFE0E0E0))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (rotation <= 90f) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(word.english_term, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Tap to Reveal", color = Color.Gray, fontSize = 14.sp)
                            }
                        } else {
                            Column(
                                modifier = Modifier.graphicsLayer { rotationY = 180f }.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(word.kannada_meaning, fontSize = 32.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(word.example_sentence, fontSize = 18.sp, color = Color.DarkGray, modifier = Modifier.padding(horizontal = 16.dp))
                                Spacer(modifier = Modifier.height(32.dp))
                                IconButton(onClick = { ttsManager.speak(word.english_term) }) {
                                    Icon(Icons.Default.VolumeUp, "Speak", modifier = Modifier.size(56.dp), tint = Color(0xFF2196F3))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FilledTonalButton(
                    onClick = { if (currentIndex > 0) { currentIndex--; isFlipped = false } }, 
                    enabled = currentIndex > 0,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, null)
                    Text("Previous")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = { if (currentIndex < practiceWords.size - 1) { currentIndex++; isFlipped = false } }, 
                    enabled = currentIndex < practiceWords.size - 1,
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Next")
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
    }
}
