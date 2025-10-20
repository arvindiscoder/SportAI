package com.example.sportai

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sportai.ui.theme.SportAITheme
import io.ktor.client.* 
import io.ktor.client.call.*
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.* 
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.* 
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Data classes for Ollama API response
@Serializable
data class OllamaTagsResponse(val models: List<OllamaModel>)

@Serializable
data class OllamaModel(val name: String)

@Serializable
data class OllamaGenerateRequest(val model: String, val prompt: String, val stream: Boolean = false)

@Serializable
data class OllamaGenerateResponse(val response: String, val done: Boolean)

// Data class to hold chat message information
data class ChatMessage(val text: String, val isUser: Boolean, val sources: List<String> = emptyList())

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SportAITheme {
                SportAnalystChatScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportAnalystChatScreen() {
    var userInput by remember { mutableStateOf(TextFieldValue("")) }
    var messages by remember { mutableStateOf(listOf(ChatMessage("Hello! I am your **Advanced Sport AI Analyst**. Select a service and provide the necessary info to get started.", isUser = false))) }
    var isLoading by remember { mutableStateOf(false) }
    var openAiApiKey by remember { mutableStateOf("") }
    var ollamaUrl by remember { mutableStateOf("") }
    var selectedService by remember { mutableStateOf("Ollama") }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var detectedModels by remember { mutableStateOf(emptyList<String>()) }
    var selectedModel by remember { mutableStateOf<String?>(null) }
    var isModelDropdownExpanded by remember { mutableStateOf(false) }
    var isDetectingModels by remember { mutableStateOf(false) }

    val client = remember {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    val darkBackgroundColor = Color(0xFF0D1117)
    val headerColor = Color(0xFF1F2937)
    val userMessageColor = Color(0xFF3B82F6)
    val aiMessageColor = Color(0xFF24292E)
    val inputBackgroundColor = Color(0xFF161B22)

    Scaffold(
        topBar = { TopAppBar(title = { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Text("SPORT ", fontWeight = FontWeight.Bold, fontSize = 20.sp); Text("AI ", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 20.sp); Text("ANALYST", fontWeight = FontWeight.Bold, fontSize = 20.sp) } }, actions = { IconButton(onClick = { messages = listOf(ChatMessage("Hello! I am your **Advanced Sport AI Analyst**. Select a service and provide the necessary info to get started.", isUser = false)) }) { Icon(Icons.Default.Delete, contentDescription = "Reset Chat", tint = Color.Red) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = headerColor, titleContentColor = Color.White)) },
        containerColor = darkBackgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.Bottom) { items(messages) { message -> ChatMessageItem(message, userMessageColor, aiMessageColor) } }
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth()) { Button(onClick = { selectedService = "OpenAI" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (selectedService == "OpenAI") Color(0xFF6F4E37) else Color(0xFF3C3C3E)), shape = MaterialTheme.shapes.medium) { if (selectedService == "OpenAI") { Icon(imageVector = Icons.Default.Done, contentDescription = "Selected", modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)) }; Text("OpenAI") }; Button(onClick = { selectedService = "Ollama" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (selectedService == "Ollama") Color(0xFF6F4E37) else Color(0xFF3C3C3E)), shape = MaterialTheme.shapes.medium) { if (selectedService == "Ollama") { Icon(imageVector = Icons.Default.Done, contentDescription = "Selected", modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)) }; Text("Ollama") } }
                Spacer(modifier = Modifier.height(16.dp))
                if (selectedService == "OpenAI") { OutlinedTextField(value = openAiApiKey, onValueChange = { openAiApiKey = it }, label = { Text("Enter your OpenAI API Key here") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White, focusedBorderColor = userMessageColor, unfocusedBorderColor = Color.Gray)) } else {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = ollamaUrl, onValueChange = { ollamaUrl = it }, label = { Text("Ollama Host URL (e.g., http://192.168.1.5:11434)") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White, focusedBorderColor = userMessageColor, unfocusedBorderColor = Color.Gray))
                            Spacer(modifier = Modifier.width(8.dp))
                            // PING BUTTON
                            Button(onClick = { 
                                coroutineScope.launch {
                                    try {
                                        val cleanUrl = ollamaUrl.trimEnd('/')
                                        val response: HttpResponse = client.get(cleanUrl)
                                        if (response.status == HttpStatusCode.OK) {
                                            Toast.makeText(context, "Ping Successful!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Ping Failed: Server responded with ${response.status}", Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Ping Failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }, modifier = Modifier.height(56.dp)) {
                                Text("Ping")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                             Button(onClick = { coroutineScope.launch { isDetectingModels = true; try { val cleanUrl = ollamaUrl.trimEnd('/'); val response: OllamaTagsResponse = client.get("$cleanUrl/api/tags/").body(); detectedModels = response.models.map { it.name }; selectedModel = detectedModels.firstOrNull(); if (detectedModels.isEmpty()) { Toast.makeText(context, "No models found. Check URL and that Ollama is running.", Toast.LENGTH_SHORT).show() } } catch (e: Exception) { Toast.makeText(context, "Detection Failed: ${e.message}", Toast.LENGTH_LONG).show(); detectedModels = emptyList(); selectedModel = null }; isDetectingModels = false } }, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = !isDetectingModels) { 
                                if (isDetectingModels) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White) } else { Text("Detect Models") } 
                            }
                        }
                        if (detectedModels.isNotEmpty()) { Box { OutlinedButton(onClick = { isModelDropdownExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selectedModel ?: "Select a model"); Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown arrow") }; DropdownMenu(expanded = isModelDropdownExpanded, onDismissRequest = { isModelDropdownExpanded = false }, modifier = Modifier.fillMaxWidth()) { detectedModels.forEach { model -> DropdownMenuItem(text = { Text(model) }, onClick = { selectedModel = model; isModelDropdownExpanded = false }) } } } }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().background(inputBackgroundColor).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = userInput, onValueChange = { userInput = it }, placeholder = { Text("Enter your sports query here...") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White, focusedBorderColor = userMessageColor, unfocusedBorderColor = Color.Gray))
                Button(
                    onClick = {
                        val query = userInput.text
                        if (query.isNotBlank()) {
                            messages = messages + ChatMessage(query, isUser = true)
                            userInput = TextFieldValue("")
                            isLoading = true
                            val aiMessageIndex = messages.size
                            messages = messages + ChatMessage("", isUser = false)

                            coroutineScope.launch {
                                listState.animateScrollToItem(messages.size)
                                try {
                                    val cleanUrl = ollamaUrl.trimEnd('/')
                                    if (cleanUrl.isBlank() || selectedModel == null) throw IllegalStateException("Ollama URL or model not selected.")

                                    val response: OllamaGenerateResponse = client.post("$cleanUrl/api/generate/") {
                                        contentType(ContentType.Application.Json)
                                        setBody(OllamaGenerateRequest(model = selectedModel!!, prompt = query, stream = false))
                                    }.body()

                                    messages = messages.toMutableList().also { list -> list[aiMessageIndex] = list[aiMessageIndex].copy(text = response.response) }
                                    listState.animateScrollToItem(messages.size - 1)

                                } catch (e: Exception) {
                                    val errorMessage = when(e) {
                                        is IllegalStateException -> "Please enter the Ollama URL and detect a model first."
                                        is java.net.ConnectException -> "Connection failed. Verify the IP address and that Ollama is running on your computer. Also check your computer's firewall."
                                        else -> "An error occurred: ${e.message}"
                                    }
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                    messages = messages.toMutableList().also { list -> list[aiMessageIndex] = list[aiMessageIndex].copy(text = errorMessage) }
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    enabled = !isLoading && userInput.text.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = userMessageColor),
                    modifier = Modifier.height(56.dp)
                ) { if (isLoading) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp) } else { Text("Analyze") } }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage, userMessageColor: Color, aiMessageColor: Color) {
    val horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    val bubbleColor = if (message.isUser) userMessageColor else aiMessageColor
    val shape = if (message.isUser) { RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp) } else { RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = horizontalArrangement) {
        Surface(color = bubbleColor, shape = shape, modifier = Modifier.widthIn(max = 300.dp)) {
            val annotatedString = buildAnnotatedString {
                val boldRegex = "\\*\\*(.*?)\\*\\*".toRegex()
                var lastIndex = 0
                boldRegex.findAll(message.text).forEach { matchResult ->
                    val range = matchResult.range
                    if (range.first > lastIndex) { append(message.text.substring(lastIndex, range.first)) }
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(matchResult.groupValues[1])
                    pop()
                    lastIndex = range.last + 1
                }
                if (lastIndex < message.text.length) { append(message.text.substring(lastIndex)) }
            }
            Text(text = annotatedString, color = Color.White, modifier = Modifier.padding(12.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SportAnalystScreenPreview() {
    SportAITheme {
        SportAnalystChatScreen()
    }
}
