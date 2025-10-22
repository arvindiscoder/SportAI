package com.example.sportai

import android.content.Intent
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.example.sportai.ui.theme.SportAITheme
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch

// Data class to hold chat message information
data class ChatMessageData(val text: String, val isUser: Boolean, val sources: List<String> = emptyList())

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SportAITheme {
                SportAnalystChatScreen(intent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportAnalystChatScreen(intent: Intent) {
    var userInput by remember { mutableStateOf(TextFieldValue("")) }
    var messages by remember { mutableStateOf(listOf(ChatMessageData("Hello! I am your **Advanced Sport AI Analyst**. Select a service and provide the necessary info to get started.", isUser = false))) }
    var isLoading by remember { mutableStateOf(false) }
    var openAiApiKey by remember { mutableStateOf("") }
    var geminiApiKey by remember { mutableStateOf("") }
    var selectedService by remember { mutableStateOf("OpenAI") }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Handle shortcut intent
    if (intent.action == Intent.ACTION_VIEW) {
        val service = intent.getStringExtra("service")
        if (service == "gemini") {
            selectedService = "Gemini"
        }
    }

    // Token Details State
    var showTokenDetailsDialog by remember { mutableStateOf(false) }
    var promptTokens by remember { mutableStateOf(0) }
    var responseTokens by remember { mutableStateOf(0) }

    val darkBackgroundColor = Color(0xFF161b22)
    val headerColor = Color(0xFF1f2937)
    val userMessageColor = Color(0xFF3b82f6)
    val aiMessageColor = Color(0xFF24292e)
    val inputBackgroundColor = Color(0xFF161b22)
    val highlightColor = Color(0xFF3b82f6)

    Scaffold(
        topBar = { TopAppBar(title = { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Text("SPORT ", fontWeight = FontWeight.Bold, fontSize = 20.sp); Text("AI ", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 20.sp); Text("ANALYST", fontWeight = FontWeight.Bold, fontSize = 20.sp) } }, actions = { IconButton(onClick = { messages = listOf(ChatMessageData("Hello! I am your **Advanced Sport AI Analyst**. Select a service and provide the necessary info to get started.", isUser = false)) }) { Icon(Icons.Default.Delete, contentDescription = "Reset Chat", tint = Color.Red) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = headerColor, titleContentColor = Color.White)) },
        containerColor = darkBackgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.Bottom) { items(messages) { message -> ChatMessageItem(message, userMessageColor, aiMessageColor) } }
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth()) { Button(onClick = { selectedService = "OpenAI" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (selectedService == "OpenAI") highlightColor else Color(0xFF3C3C3E)), shape = MaterialTheme.shapes.medium) { if (selectedService == "OpenAI") { Icon(imageVector = Icons.Default.Done, contentDescription = "Selected", modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)) }; Text("OpenAI") }; Button(onClick = { selectedService = "Gemini" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (selectedService == "Gemini") highlightColor else Color(0xFF3C3C3E)), shape = MaterialTheme.shapes.medium) { if (selectedService == "Gemini") { Icon(imageVector = Icons.Default.Done, contentDescription = "Selected", modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)) }; Text("Gemini") }; Button(onClick = { selectedService = "Ollama" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (selectedService == "Ollama") highlightColor else Color(0xFF3C3C3E)), shape = MaterialTheme.shapes.medium) { if (selectedService == "Ollama") { Icon(imageVector = Icons.Default.Done, contentDescription = "Selected", modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)) }; Text("Ollama") } }
                Spacer(modifier = Modifier.height(16.dp))
                when (selectedService) {
                    "OpenAI" -> {
                        OutlinedTextField(value = openAiApiKey, onValueChange = { openAiApiKey = it }, label = { Text("Enter your OpenAI API Key here") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White, focusedBorderColor = userMessageColor, unfocusedBorderColor = Color.Gray))
                    }
                    "Gemini" -> {
                        OutlinedTextField(value = geminiApiKey, onValueChange = { geminiApiKey = it }, label = { Text("Enter your Gemini API Key here") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White, focusedBorderColor = userMessageColor, unfocusedBorderColor = Color.Gray))
                    }
                    "Ollama" -> {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Ollama support is coming soon!", color = Color.Gray, textAlign = TextAlign.Center)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { showTokenDetailsDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Show Token Details")
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
                            messages = messages + ChatMessageData(query, isUser = true)
                            userInput = TextFieldValue("")
                            isLoading = true

                            coroutineScope.launch {
                                val aiMessageIndex = messages.size
                                messages = messages + ChatMessageData("", isUser = false)
                                listState.animateScrollToItem(messages.size)

                                when (selectedService) {
                                    "OpenAI" -> {
                                        try {
                                            if (openAiApiKey.isBlank()) throw IllegalStateException("OpenAI API Key is missing.")
                                            val openAI = OpenAI(openAiApiKey)
                                            val chatCompletionRequest = ChatCompletionRequest(
                                                model = ModelId("gpt-3.5-turbo"),
                                                messages = listOf(
                                                    ChatMessage(role = ChatRole.System, content = "You are a helpful sports AI assistant."),
                                                    ChatMessage(role = ChatRole.User, content = query)
                                                )
                                            )
                                            val completion = openAI.chatCompletion(chatCompletionRequest)
                                            promptTokens = completion.usage?.promptTokens ?: 0
                                            responseTokens = completion.usage?.completionTokens ?: 0
                                            val aiResponse = completion.choices.first().message.content ?: "No response from API."
                                            messages = messages.toMutableList().also { it[aiMessageIndex] = it[aiMessageIndex].copy(text = aiResponse) }

                                        } catch (e: Exception) {
                                            val errorMessage = "Error: ${e.message}"
                                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                            messages = messages.toMutableList().also { it[aiMessageIndex] = it[aiMessageIndex].copy(text = errorMessage) }
                                        } finally {
                                            isLoading = false
                                            listState.animateScrollToItem(messages.size - 1)
                                        }
                                    }
                                    "Gemini" -> {
                                        try {
                                            if (geminiApiKey.isBlank()) throw IllegalStateException("Gemini API Key is missing.")
                                            val generativeModel = GenerativeModel(
                                                modelName = "gemini-2.5-flash",
                                                apiKey = geminiApiKey
                                            )
                                            val response = generativeModel.generateContent(query)
                                            promptTokens = 0 // usageMetadata not available in this version
                                            responseTokens = 0 // usageMetadata not available in this version
                                            messages = messages.toMutableList().also { it[aiMessageIndex] = it[aiMessageIndex].copy(text = response.text ?: "No response from API.") }
                                        } catch (e: Exception) {
                                            val errorMessage = "Error: ${e.message}"
                                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                            messages = messages.toMutableList().also { it[aiMessageIndex] = it[aiMessageIndex].copy(text = errorMessage) }
                                        } finally {
                                            isLoading = false
                                            listState.animateScrollToItem(messages.size - 1)
                                        }
                                    }
                                    "Ollama" -> {
                                        val comingSoonMessage = "Sorry, Ollama support is not yet implemented."
                                        messages = messages.toMutableList().also { it[aiMessageIndex] = it[aiMessageIndex].copy(text = comingSoonMessage) }
                                        isLoading = false
                                        listState.animateScrollToItem(messages.size - 1)
                                    }
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

        if (showTokenDetailsDialog) {
            TokenDetailsDialog(
                selectedService = selectedService,
                promptTokens = promptTokens,
                responseTokens = responseTokens,
                onDismiss = { showTokenDetailsDialog = false }
            )
        }
    }
}

@Composable
fun TokenDetailsDialog(
    selectedService: String,
    promptTokens: Int,
    responseTokens: Int,
    onDismiss: () -> Unit
) {
    val modelLimit = when (selectedService) {
        "OpenAI" -> "4,096"
        "Gemini" -> "32,768"
        else -> "N/A"
    }
    val lastQueryCount = promptTokens + responseTokens

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Token Details", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Model Token Limit:", color = Color.Gray)
                    Text("$modelLimit tokens", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Last Query Token Count:", color = Color.Gray)
                    Text("$lastQueryCount tokens", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessageData, userMessageColor: Color, aiMessageColor: Color) {
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
        SportAnalystChatScreen(Intent())
    }
}
