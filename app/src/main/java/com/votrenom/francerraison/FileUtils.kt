package com.votrenom.francerraison // Adaptez à votre package

import android.content.Context
import java.io.IOException
import java.io.InputStream

/**
 * Lit le contenu d'un fichier depuis le dossier assets de l'application.
 * @param fileName Le nom du fichier à lire dans le dossier assets (ex: "histoire_principale.json").
 * @param context Le contexte de l'application, nécessaire pour accéder aux assets.
 * @return Le contenu du fichier sous forme de String, ou null en cas d'erreur.
 */
fun chargerJsonDepuisAssets(fileName: String, context: Context): String? {var jsonString: String? = null
try {
val inputStream: InputStream = context.assets.open(fileName)
val size: Int = inputStream.available()
val buffer = ByteArray(size)
inputStream.read(buffer)
inputStream.close()
jsonString = String(buffer, Charsets.UTF_8)
} catch (e: IOException) {
e.printStackTrace() // Affiche l'erreur dans Logcat
System.err.println("[ERREUR FileUtils] Impossible de lire le fichier '$fileName' depuis les assets: ${e.message}")
return null
}
return jsonString
}
