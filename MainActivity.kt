package com.example.encryptedchat
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val KEY_ALIAS = "EncryptedChatAESKey"

data class ChatMessage(
    val encryptedText: String,
    val sender: String,
    val time: String
)

private fun getOrCreateKey(): SecretKey {
    val keyStore = KeyStore.getInstance("AndroidKeyStore")
    keyStore.load(null)

    val existingKey = keyStore.getKey(KEY_ALIAS, null)

    if (existingKey is SecretKey) {
        return existingKey
    }

    val keyGenerator = KeyGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_AES,
        "AndroidKeyStore"
    )

    val keySpec = KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .build()

    keyGenerator.init(keySpec)

    return keyGenerator.generateKey()
}

private fun encryptMessage(message: String): String {
    val key = getOrCreateKey()

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key)

    val encryptedBytes = cipher.doFinal(
        message.toByteArray(StandardCharsets.UTF_8)
    )

    val iv = cipher.iv

    val combined = ByteArray(iv.size + encryptedBytes.size)

    System.arraycopy(iv, 0, combined, 0, iv.size)
    System.arraycopy(
        encryptedBytes,
        0,
        combined,
        iv.size,
        encryptedBytes.size
    )

    return Base64.encodeToString(
        combined,
        Base64.NO_WRAP
    )
}

private fun decryptMessage(encryptedMessage: String): String {
    val key = getOrCreateKey()

    val combined = Base64.decode(
        encryptedMessage,
        Base64.NO_WRAP
    )

    if (combined.size <= 12) {
        throw IllegalArgumentException("Invalid encrypted message")
    }

    val iv = combined.copyOfRange(0, 12)
    val encryptedBytes = combined.copyOfRange(
        12,
        combined.size
    )

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")

    val gcmSpec = GCMParameterSpec(128, iv)

    cipher.init(
        Cipher.DECRYPT_MODE,
        key,
        gcmSpec
    )

    val decryptedBytes = cipher.doFinal(encryptedBytes)

    return String(
        decryptedBytes,
        StandardCharsets.UTF_8
    )
}

private fun getCurrentTime(): String {
    return SimpleDateFormat(
        "HH:mm",
        Locale.getDefault()
    ).format(Date())
}

@Composable
fun WelcomeScreen(
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Encrypted Chat",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "Secure messaging using AES encryption",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        Button(
            onClick = onStart
        ) {
            Text(
                text = "Start Messaging"
            )
        }
    }
}

@Composable
fun ChatScreen(
    onBack: () -> Unit
) {
    var currentUser by remember {
        mutableStateOf("Arshad")
    }

    var message by remember {
        mutableStateOf("")
    }

    val messages = remember {
        mutableStateListOf<ChatMessage>()
    }

    var sendError by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.primary
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Encrypted Chat",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center
        ) {

            Text(
                text = if (currentUser == "Arshad")
                    "● Arshad"
                else
                    "○ Arshad",

                modifier = Modifier
                    .clickable {
                        currentUser = "Arshad"
                    }
                    .padding(8.dp)
            )

            Text(
                text = if (currentUser == "Charan")
                    "● Charan"
                else
                    "○ Charan",

                modifier = Modifier
                    .clickable {
                        currentUser = "Charan"
                    }
                    .padding(8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 10.dp),

            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            itemsIndexed(messages) { index, item ->

                var showDecrypted by remember {
                    mutableStateOf(false)
                }

                var decryptedText by remember {
                    mutableStateOf("")
                }

                var decryptError by remember {
                    mutableStateOf("")
                }

                val isArshad =
                    item.sender == "Arshad"

                Row(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        if (isArshad)
                            Arrangement.End
                        else
                            Arrangement.Start
                ) {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(0.82f)
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {

                            Text(
                                text = item.sender,
                                style =
                                    MaterialTheme.typography.titleSmall
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(5.dp)
                            )

                            Text(
                                text =
                                    "🔐 " +
                                            item.encryptedText,

                                style =
                                    MaterialTheme.typography.bodySmall
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(5.dp)
                            )

                            Text(
                                text = item.time,

                                style =
                                    MaterialTheme.typography.bodySmall,

                                modifier =
                                    Modifier.align(
                                        Alignment.End
                                    )
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(5.dp)
                            )

                            Row(
                                modifier =
                                    Modifier.fillMaxWidth(),

                                horizontalArrangement =
                                    Arrangement.End
                            ) {

                                Text(
                                    text =
                                        if (showDecrypted)
                                            "Hide"
                                        else
                                            "Decrypt",

                                    color = Color.Red,

                                    modifier =
                                        Modifier
                                            .clickable {

                                                if (showDecrypted) {

                                                    showDecrypted =
                                                        false

                                                } else {

                                                    try {

                                                        decryptedText =
                                                            decryptMessage(
                                                                item.encryptedText
                                                            )

                                                        decryptError =
                                                            ""

                                                        showDecrypted =
                                                            true

                                                    } catch (
                                                        _: Exception
                                                    ) {

                                                        decryptError =
                                                            "Unable to decrypt message"

                                                        showDecrypted =
                                                            false
                                                    }
                                                }
                                            }
                                            .padding(6.dp)
                                )

                                Text(
                                    text = "Delete",

                                    color = Color.Red,

                                    modifier =
                                        Modifier
                                            .clickable {

                                                messages.removeAt(
                                                    index
                                                )
                                            }
                                            .padding(6.dp)
                                )
                            }

                            if (showDecrypted) {

                                Spacer(
                                    modifier =
                                        Modifier.height(5.dp)
                                )

                                Text(
                                    text =
                                        "Original Message:"
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(3.dp)
                                )

                                Text(
                                    text = decryptedText
                                )
                            }

                            if (decryptError.isNotEmpty()) {

                                Spacer(
                                    modifier =
                                        Modifier.height(5.dp)
                                )

                                Text(
                                    text = decryptError,

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .error
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "Clear Conversation",

                color = Color.Red,

                modifier =
                    Modifier
                        .clickable {
                            messages.clear()
                        }
                        .padding(8.dp)
            )

            Text(
                text = "Close",

                color = Color.Red,

                modifier =
                    Modifier
                        .clickable {
                            onBack()
                        }
                        .padding(8.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            OutlinedTextField(
                value = message,

                onValueChange = {
                    message = it
                    sendError = ""
                },

                label = {
                    Text("Message")
                },

                modifier = Modifier.weight(1f)
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Button(
                onClick = {

                    if (message.isBlank()) {

                        sendError =
                            "Please enter a message"

                    } else {

                        try {

                            val encrypted =
                                encryptMessage(message)

                            messages.add(
                                ChatMessage(
                                    encryptedText = encrypted,
                                    sender = currentUser,
                                    time = getCurrentTime()
                                )
                            )

                            message = ""
                            sendError = ""

                        } catch (
                            _: Exception
                        ) {

                            sendError =
                                "Unable to encrypt message"
                        }
                    }
                }
            ) {
                Text(
                    text = "Send Securely"
                )
            }
        }

        if (sendError.isNotEmpty()) {

            Text(
                text = sendError,

                color =
                    MaterialTheme
                        .colorScheme
                        .error,

                modifier =
                    Modifier.padding(
                        horizontal = 12.dp
                    )
            )
        }

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )
    }
}

@Composable
fun EncryptedChatApp() {

    var showChat by remember {
        mutableStateOf(false)
    }

    if (showChat) {

        ChatScreen(
            onBack = {
                showChat = false
            }
        )

    } else {

        WelcomeScreen(
            onStart = {
                showChat = true
            }
        )
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {

            MaterialTheme {

                EncryptedChatApp()
            }
        }
    }
}
