package com.example.uniformecircular

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

class UniformeCircularApp : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Priorizar IPv4 para el stack de red
        System.setProperty("java.net.preferIPv4Stack", "true")
        
        FirebaseApp.initializeApp(this)
        // Configurar Firestore con caché persistente para mejorar el rendimiento
        try {
            val db = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            db.firestoreSettings = settings
            
            // Forzar reconexión manual para saltar bloqueos de DNS iniciales
            db.enableNetwork()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
