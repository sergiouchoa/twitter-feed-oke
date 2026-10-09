package br.com.escolamais.app.ui.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import br.com.escolamais.app.EstadoApp

@Composable
fun LoginTela(estado: EstadoApp.Deslogado, onEntrar: (url: String, email: String, senha: String) -> Unit) {
    var url by rememberSaveable { mutableStateOf(estado.url) }
    var email by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var verSenha by rememberSaveable { mutableStateOf(false) }
    var mostrarServidor by rememberSaveable { mutableStateOf(false) }

    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary).systemBarsPadding().imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.School, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(72.dp))
            Text("Escola+", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            Text("Tudo da escola em um só lugar", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
            Spacer(Modifier.height(24.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = email, onValueChange = { email = it }, label = { Text("E-mail") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = senha, onValueChange = { senha = it }, label = { Text("Senha") }, singleLine = true,
                        visualTransformation = if (verSenha) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        trailingIcon = {
                            IconButton(onClick = { verSenha = !verSenha }) {
                                Icon(if (verSenha) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Mostrar senha")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (mostrarServidor) {
                        OutlinedTextField(
                            value = url, onValueChange = { url = it }, label = { Text("Endereço do servidor") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    estado.erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        onClick = { onEntrar(url, email, senha) },
                        enabled = !estado.carregando,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        if (estado.carregando) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        else Text("Entrar")
                    }
                    TextButton(onClick = { mostrarServidor = !mostrarServidor }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(if (mostrarServidor) "Ocultar servidor" else "Configurar servidor")
                    }
                }
            }
        }
    }
}
