package com.myofficework000.cicdandroid // Asegúrate que coincida con el package real de tu archivo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.*
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. GESTIÓN DE MEMORIA (El cerebro persistente)
        val memoryFile = File(filesDir, "ac_uni_memory.txt")
        if (!memoryFile.exists()) memoryFile.writeText("Perfil de Usuario: Tabula Rasa. \nPreferencias: Ninguna registrada.")

        // 2. DEFINICIÓN DE HERRAMIENTAS (Lo que hace que sea como Manus)
        
        // Herramienta A: Aprender/Guardar Preferencias
        val updateMemoryTool = FunctionDeclaration(
            name = "updateMemory",
            description = "Guarda un nuevo hecho o preferencia importante sobre el usuario para recordarlo siempre.",
            parameters = mapOf(
                "type" to "OBJECT",
                "properties" to mapOf(
                    "new_knowledge" to mapOf("type" to "STRING", "description" to "El dato a memorizar")
                ),
                "required" to listOf("new_knowledge")
            )
        )

        // Herramienta B: Crear Archivos (Código, Webs, Notas)
        val createFileTool = FunctionDeclaration(
            name = "createFile",
            description = "Crea un archivo con código o texto en el dispositivo.",
            parameters = mapOf(
                "type" to "OBJECT",
                "properties" to mapOf(
                    "filename" to mapOf("type" to "STRING"),
                    "content" to mapOf("type" to "STRING")
                ),
                "required" to listOf("filename", "content")
            )
        )

        // Inicializamos el modelo con estas herramientas
        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-pro",
            apiKey = "AIzaSyCaHd4T1p9p-6RZBwZThazwxeFLjY9oJR0", // ¡Recuerda poner tu API Key o usar BuildConfig!
            tools = listOf(Tool(listOf(updateMemoryTool, createFileTool))),
            systemInstruction = content { text("Eres AC_Uni. Tu objetivo es ser un Agente General Autónomo. " +
                    "No asumas nada. Lee el contexto, aprende del usuario y USA TUS HERRAMIENTAS para modificar tu memoria o crear archivos reales.") }
        )

        setContent {
            var input by remember { mutableStateOf("") }
            // Cargamos la memoria actual en la interfaz para que veas qué sabe de ti
            var currentMemory by remember { mutableStateOf(memoryFile.readText()) }
            var log by remember { mutableStateOf("Sistema AC_Uni Online.\nEsperando instrucciones...") }
            val scope = rememberCoroutineScope()

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("AC_Uni: Universal Agent", style = MaterialTheme.typography.titleLarge)
                
                // Visor de Memoria (Para que veas cómo aprende)
                Text("Memoria Actual:", style = MaterialTheme.typography.labelSmall)
                Text(currentMemory, style = MaterialTheme.typography.bodySmall, modifier = Modifier.height(50.dp))
                
                Divider()

                // Log de Chat/Acciones
                Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Text(text = log)
                }

                TextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Instrucción (ej: Guarda que uso Julia)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        scope.launch {
                            log += "\n\nTU: $input"
                            // Inyectamos la memoria aprendida en cada prompt
                            val fullPrompt = "MEMORIA ACUMULADA:\n${memoryFile.readText()}\n\nINSTRUCCIÓN:\n$input"
                            
                            try {
                                val response = generativeModel.generateContent(fullPrompt)
                                
                                // Lógica de Ejecución de Herramientas
                                response.functionCalls.forEach { call ->
                                    when (call.name) {
                                        "updateMemory" -> {
                                            val newFact = call.args["new_knowledge"]
                                            memoryFile.appendText("\n- $newFact")
                                            currentMemory = memoryFile.readText()
                                            log += "\n[SISTEMA] Memoria actualizada: $newFact"
                                        }
                                        "createFile" -> {
                                            val name = call.args["filename"]
                                            val body = call.args["content"]
                                            File(filesDir, name!!).writeText(body!!)
                                            log += "\n[SISTEMA] Archivo creado: $name"
                                        }
                                    }
                                }
                                
                                // Si hay respuesta de texto normal, la mostramos
                                if (response.text != null) {
                                    log += "\nAGENTE: ${response.text}"
                                }
                                
                            } catch (e: Exception) {
                                log += "\nERROR: ${e.message}"
                            }
                            input = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Ejecutar") }
            }
        }
    }
}
