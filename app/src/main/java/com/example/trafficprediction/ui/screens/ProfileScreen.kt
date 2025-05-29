package com.example.trafficprediction.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import coil.compose.AsyncImage
import com.example.trafficprediction.R // Eğer bir placeholder drawable kullanacaksanız
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trafficprediction.ui.theme.TrafficPredictionTheme
import com.example.trafficprediction.ui.viewmodels.AuthUiState
import com.example.trafficprediction.ui.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.collectLatest // Flow dinleme için import

// OptIn anotasyonu genellikle Composable fonksiyonun üzerine eklenir
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel = viewModel()
) {
    // ViewModel'den state'leri topla
    val authState by authViewModel.authUiState.collectAsState()
    val email by authViewModel.email.collectAsState()
    val password by authViewModel.password.collectAsState()
    val isPasswordVisible by authViewModel.isPasswordVisible.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    // newDisplayName, currentUserPhotoUrl, selectedImageUri AuthViewModel'den UserProfileSection içinde kullanılacak.
    // ProfileScreen seviyesinde ayrıca collect etmeye gerek yok, doğrudan UserProfileSection'a geçilecek.
    // Ancak, UserProfileSection çağrısında bu state'leri ViewModel'den alıp geçmemiz gerekiyor.

    // Hata mesajını AuthUiState'ten veya genel errorMessage'dan al
    val errorMessage = when(authState) {
        is AuthUiState.Error -> (authState as AuthUiState.Error).message
        else -> authViewModel.errorMessage.collectAsState().value // Genel hatalar için
    }


    // Snackbar State
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current // Focus Manager

    // ViewModel Olaylarını Dinleme (Snackbar için)
    LaunchedEffect(Unit) {
        authViewModel.eventFlow.collectLatest { message ->
            // Başka bir snackbar gösteriliyorsa onu iptal et (isteğe bağlı)
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    // Scaffold ile Ekran Yapısı
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Scaffold padding'ini uygula
                .padding(16.dp), // Ekstra kendi padding'imiz
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Duruma göre UI'ı göster
            when (authState) {
                is AuthUiState.Loading -> {
                    Spacer(modifier = Modifier.height(32.dp)) // Adjusted spacer
                    CircularProgressIndicator()
                }
                is AuthUiState.SignedIn -> {
                    val user = (authState as AuthUiState.SignedIn).user
                    val collectedNewDisplayName by authViewModel.newDisplayName.collectAsState()
                    val collectedCurrentUserPhotoUrl by authViewModel.currentUserPhotoUrl.collectAsState()
                    val collectedSelectedImageUri by authViewModel.selectedImageUri.collectAsState()
                    val currentIsLoading by authViewModel.isLoading.collectAsState()


                    UserProfileSection(
                        email = user.email,
                        currentDisplayName = collectedNewDisplayName,
                        currentPhotoUrl = collectedCurrentUserPhotoUrl,
                        selectedImageUri = collectedSelectedImageUri,
                        isLoading = currentIsLoading, // isLoading'i doğru şekilde al
                        onNewDisplayNameChange = authViewModel::onNewDisplayNameChange,
                        onUpdateDisplayName = { authViewModel.updateDisplayName() },
                        onSignOut = { authViewModel.signOut() },
                        onPickImage = { uri -> authViewModel.onSelectedImageUriChange(uri) },
                        onUploadAndSetProfilePicture = { authViewModel.uploadAndSetProfilePicture() }
                    )
                }
                is AuthUiState.SignedOut, is AuthUiState.Error -> {
                    // Hata mesajını LoginRegisterSection'a iletiyoruz
                    LoginRegisterSection(
                        email = email,
                        password = password,
                        isPasswordVisible = isPasswordVisible,
                        isLoading = isLoading,
                        errorMessage = errorMessage, // Düzeltilmiş hata mesajı
                        onEmailChange = authViewModel::onEmailChange,
                        onPasswordChange = authViewModel::onPasswordChange,
                        onTogglePasswordVisibility = authViewModel::togglePasswordVisibility,
                        onSignIn = {
                            focusManager.clearFocus() // Klavyeyi kapat
                            authViewModel.signIn()
                        },
                        onSignUp = {
                            focusManager.clearFocus() // Klavyeyi kapat
                            authViewModel.signUp()
                        },
                        onForgotPasswordClicked = { // YENİ LAMBDA BAĞLANTISI
                            focusManager.clearFocus()
                            authViewModel.sendPasswordResetEmail()
                        },
                        signUpDisplayName = authViewModel.signUpDisplayName.collectAsState().value, // YENİ
                        onSignUpDisplayNameChange = authViewModel::onSignUpDisplayNameChange // YENİ
                    )
                }
            }
        }
    }
}

// Giriş yapmış kullanıcı arayüzü
@Composable
fun UserProfileSection(
    email: String?,
    currentDisplayName: String,
    currentPhotoUrl: String?,
    selectedImageUri: android.net.Uri?,
    isLoading: Boolean,
    onNewDisplayNameChange: (String) -> Unit,
    onUpdateDisplayName: () -> Unit, // Adı değişti
    onSignOut: () -> Unit,
    onPickImage: (android.net.Uri?) -> Unit, // Seçilen URI'yi ViewModel'e iletmek için
    onUploadAndSetProfilePicture: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: android.net.Uri? ->
            onPickImage(uri) // Seçilen URI'yi ViewModel'e bildir
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = selectedImageUri ?: currentPhotoUrl ?: R.drawable.ic_launcher_foreground, // Placeholder ekle
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.ic_launcher_foreground) // Hata durumunda gösterilecek resim
            )
            // TODO: Resim üzerinde bir "düzenle" ikonu eklenebilir.
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
            Text("Change Profile Picture")
        }

        selectedImageUri?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onUploadAndSetProfilePicture,
                enabled = !isLoading
            ) {
                Text("Upload Picture")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = currentDisplayName,
            onValueChange = onNewDisplayNameChange,
            label = { Text("Display Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                if (currentDisplayName.isNotBlank()) {
                    onUpdateDisplayName()
                }
            })
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = email ?: "No email",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                onUpdateDisplayName()
            },
            enabled = !isLoading && currentDisplayName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Display Name")
        }

        Spacer(modifier = Modifier.height(24.dp)) // Tema seçimi öncesi boşluk

        // --- Tema Seçimi Bölümü ---
        Text(
            text = "Theme Settings",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start) // Başlığı sola yasla
        )
        Divider(modifier = Modifier.padding(vertical = 4.dp))

        val themeViewModel: com.example.trafficprediction.ui.viewmodels.ThemeViewModel = viewModel()
        val currentThemePreference by themeViewModel.themePreference.collectAsState()
        val themeOptions = com.example.trafficprediction.data.ThemePreference.values()

        Column(Modifier.selectableGroup()) {
            themeOptions.forEach { themeOption ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (themeOption == currentThemePreference),
                            onClick = { themeViewModel.updateThemePreference(themeOption) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (themeOption == currentThemePreference),
                        onClick = null // null recommended for accessibility with screenreaders
                    )
                    Text(
                        text = themeOption.name.replace("_", " ").lowercase()
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }, // Enum adını güzelleştir
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
        // --- Tema Seçimi Bölümü Sonu ---

        Spacer(modifier = Modifier.weight(1f)) // Çıkış butonunu en alta iter

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Log Out")
        }
    }
}

// Giriş/Kayıt formu arayüzü
@Composable
fun LoginRegisterSection(
    email: String,
    password: String,
    isPasswordVisible: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onForgotPasswordClicked: () -> Unit, // YENİ PARAMETRE
    signUpDisplayName: String, // Kayıt için görünen ad state'i
    onSignUpDisplayNameChange: (String) -> Unit // Kayıt için görünen ad değiştirme fonksiyonu
) {
    val focusManager = LocalFocusManager.current

    Column( // Formu da Column içine almak daha iyi
        modifier = Modifier.fillMaxWidth(), // Column genişliği
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Login or Register",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp)) // Adjusted spacer

        // Hata mesajı
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Kayıt için Görünen Ad alanı
        OutlinedTextField(
            value = signUpDisplayName,
            onValueChange = onSignUpDisplayNameChange,
            label = { Text("Display Name (for Sign Up)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (email.isNotBlank() && password.isNotBlank()) {
                        onSignIn()
                    }
                }
            ),
            trailingIcon = {
                val image = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = onTogglePasswordVisibility, enabled = !isLoading) {
                    Icon(imageVector = image, contentDescription = if (isPasswordVisible) "Hide password" else "Show password")
                }
            },
            singleLine = true,
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Yükleme göstergesi veya Butonlar
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onSignIn()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank() // isLoading kontrolü eklendi
                ) {
                    Text("Log In")
                }
                OutlinedButton(
                    onClick = {
                        focusManager.clearFocus()
                        onSignUp()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank() // isLoading kontrolü eklendi
                ) {
                    Text("Register")
                }
            }
            Spacer(modifier = Modifier.height(8.dp)) // Butonlar ve şifre sıfırlama arasında boşluk
            TextButton(
                onClick = {
                    focusManager.clearFocus() // Klavyeyi kapat
                    onForgotPasswordClicked() // Yeni lambda'yı çağır
                },
                modifier = Modifier.align(Alignment.End), // Sağa yasla
                enabled = !isLoading && email.isNotBlank() // E-posta boş değilse aktif
            ) {
                Text("Forgot Password?")
            }
        }
        // Spacer(modifier = Modifier.weight(1f)) // Bu Spacer burada gereksiz olabilir
    } // Column sonu
}


// --- Preview Fonksiyonları ---
@Preview(showBackground = true, name = "Profile Logged In")
@Composable
fun ProfileScreenLoggedInPreview() {
    TrafficPredictionTheme {
        // UserProfileSection'ı doğrudan sahte String değerlerle çağır
        UserProfileSection(
            email = "preview@example.com",
            currentDisplayName = "Preview User",
            currentPhotoUrl = null, // Preview için null
            selectedImageUri = null, // Preview için null
            isLoading = false,
            onNewDisplayNameChange = {},
            onUpdateDisplayName = {},
            onSignOut = {},
            onPickImage = {},
            onUploadAndSetProfilePicture = {}
        )
    }
}

@Preview(showBackground = true, name = "Profile Logged Out")
@Composable
fun ProfileScreenLoggedOutPreview() {
    TrafficPredictionTheme {
        // Column içinde gösterelim
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            LoginRegisterSection(
                email = "test@example.com",
                password = "password",
                isPasswordVisible = false,
                isLoading = false, // Yüklenmiyor durumu
                errorMessage = null, // Hata yok durumu
                onEmailChange = {},
                onPasswordChange = {},
                onTogglePasswordVisibility = {},
                onSignIn = {},
                onSignUp = {},
                onForgotPasswordClicked = {}, // Preview için boş lambda
                signUpDisplayName = "", // Preview için boş
                onSignUpDisplayNameChange = {} // Preview için boş lambda
            )
        }
    }
}

@Preview(showBackground = true, name = "Profile Login Loading")
@Composable
fun ProfileScreenLoadingPreview() {
    TrafficPredictionTheme {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            LoginRegisterSection(
                email = "test@example.com",
                password = "password",
                isPasswordVisible = false,
                isLoading = true, // Yükleniyor durumu
                errorMessage = null,
                onEmailChange = {},
                onPasswordChange = {},
                onTogglePasswordVisibility = {},
                onSignIn = {},
                onSignUp = {},
                onForgotPasswordClicked = {}, // Preview için boş lambda
                signUpDisplayName = "", // Preview için boş
                onSignUpDisplayNameChange = {} // Preview için boş lambda
            )
        }
    }
}

@Preview(showBackground = true, name = "Profile Login Error")
@Composable
fun ProfileScreenErrorPreview() {
    TrafficPredictionTheme {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            LoginRegisterSection(
                email = "test@example.com",
                password = "password",
                isPasswordVisible = false,
                isLoading = false,
                errorMessage = "Invalid email or password.", // Hata durumu
                onEmailChange = {},
                onPasswordChange = {},
                onTogglePasswordVisibility = {},
                onSignIn = {},
                onSignUp = {},
                onForgotPasswordClicked = {}, // Preview için boş lambda
                signUpDisplayName = "", // Preview için boş
                onSignUpDisplayNameChange = {} // Preview için boş lambda
            )
        }
    }
}
