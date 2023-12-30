package com.example.gemini_pro

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.gemini_pro.ui.theme.Gemini_ProTheme
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    var response: GenerateContentResponse? = null
    private val _responseFlow: MutableStateFlow<GenerateContentResponse?> = MutableStateFlow(response)
    val responseFlow = _responseFlow.asStateFlow()

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isResponseReceived = remember { mutableStateOf(false) }
            val generativeModel =
                GenerativeModel(modelName = "gemini-pro", apiKey = BuildConfig.apiKey)
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusRequester = remember { FocusRequester() }
            val value = remember { mutableStateOf("") }
            Gemini_ProTheme {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (isResponseReceived.value) {
                        Greeting(responseFlow)
                        Row(
                            modifier = Modifier.fillMaxSize()
                                .padding(start = 8.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                modifier = Modifier
                                    .weight(0.6f)
                                    .clickable {
                                        focusRequester.requestFocus()
                                        keyboardController?.show()
                                    },
                                value = value.value,
                                textStyle = LocalTextStyle.current.copy(
                                    textAlign = TextAlign.Justify,
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight(400)
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(Color.Black),
                                onValueChange = { it: String ->
                                    value.value = it
                                },
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (value.value.isEmpty()) {
                                            Text(
                                                text = "Enter prompt",
                                                style = TextStyle(
                                                    fontSize = 16.sp,
                                                    lineHeight = 16.sp,
                                                    fontWeight = FontWeight(500),
                                                    color = Color.Gray
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text, imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = {
                                    generateResponse(generativeModel, value.value)
                                    keyboardController?.hide()
                                })
                            )
                            Image(
                                modifier = Modifier.clickable {
                                    generateResponse(
                                        generativeModel,
                                        value.value
                                    )
                                    keyboardController?.hide()
                                    isResponseReceived.value = false
                                    value.value = ""
                                },
                                painter = painterResource(R.drawable.baseline_send_24),
                                contentDescription = "Send Prompt to model",
                                alignment = Alignment.CenterEnd
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()
                            .background(Color.Black),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(50.dp),
                                color = Color.Blue,
                                strokeWidth = 6.dp
                            )
                            Text(
                                text = "Wait for some time...Gemini is trying its best...",
                                modifier = Modifier.width(140.dp),
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight(900)
                            )
                        }
                    }
                }

                LaunchedEffect(true) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        responseFlow.collectLatest {
                            isResponseReceived.value = true
                        }
                    }
                }
            }
        }
    }


    private fun generateResponse(generativeModel: GenerativeModel, prompt: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            response = generativeModel.generateContent(prompt)
            _responseFlow.emit(response)
            Log.d("Gemini Pro", response?.text ?: "No response")
        }
    }
}

@Composable
fun Greeting(flow: StateFlow<GenerateContentResponse?>) {
    val response = flow.collectAsState().value
    Column {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f)
                .background(Color.Black.copy(alpha = 0.8f))
        ) {
            item {
                Text(
                    text = response?.text ?: "Enter a prompt and send it to me so that I can help you...",
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}


//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    Gemini_ProTheme {
//        Greeting(flow = MutableStateFlow<GenerateContentResponse>(null).asStateFlow())
//    }
//}