package com.example.base

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.firestoreSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.robolectric.shadows.ShadowLooper
import java.net.HttpURLConnection
import java.net.URL

abstract class FirestoreEmulatorTestBase {

    protected lateinit var appContext: Context
    protected lateinit var db: FirebaseFirestore
    protected lateinit var auth: FirebaseAuth
    protected lateinit var databaseId: String
    protected lateinit var projectId: String

    companion object {
        private const val FIRESTORE_HOST = "127.0.0.1"
        private const val FIRESTORE_PORT = 8085
        private const val AUTH_HOST = "127.0.0.1"
        private const val AUTH_PORT = 9099

        @Volatile
        private var emulatorsBound = false
    }

    @Before
    fun setUpFirestoreEmulator() {
        appContext = ApplicationProvider.getApplicationContext()
        val app = FirebaseApp.getApps(appContext).firstOrNull()
            ?: FirebaseApp.initializeApp(appContext)
            ?: error("FirebaseApp failed to initialize in Robolectric.")

        projectId = app.options.projectId ?: "gen-lang-client-0892654250"
        databaseId = appContext.getString(R.string.firestore_database_id)

        db = FirebaseFirestore.getInstance(app, databaseId)
        auth = FirebaseAuth.getInstance(app)

        if (!emulatorsBound) {
            synchronized(FirestoreEmulatorTestBase::class.java) {
                if (!emulatorsBound) {
                    db.useEmulator(FIRESTORE_HOST, FIRESTORE_PORT)
                    db.firestoreSettings = firestoreSettings {
                        setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    }
                    auth.useEmulator(AUTH_HOST, AUTH_PORT)
                    emulatorsBound = true
                }
            }
        }
        auth.signOut()
        clearEmulatorFirestoreSync()
    }

    @After
    fun tearDownFirestoreEmulator() {
        if (::auth.isInitialized) {
            auth.signOut()
        }
        ShadowLooper.idleMainLooper()
    }

    protected suspend fun signInTestUser(email: String, password: String = "TestPass123!"): String =
        withContext(Dispatchers.IO) {
            auth.signOut()
            val result = try {
                auth.signInWithEmailAndPassword(email, password).await()
            } catch (_: Exception) {
                auth.createUserWithEmailAndPassword(email, password).await()
            }
            ShadowLooper.idleMainLooper()
            result.user?.uid ?: error("Auth emulator returned null user.")
        }

    protected suspend fun signOutTestUser() = withContext(Dispatchers.IO) {
        auth.signOut()
        ShadowLooper.idleMainLooper()
    }

    private fun clearEmulatorFirestoreSync() {
        val thread = Thread {
            try {
                val url = URL(
                    "http://$FIRESTORE_HOST:$FIRESTORE_PORT/emulator/v1/projects/$projectId/databases/$databaseId/documents"
                )
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                    connectTimeout = 4000
                    readTimeout = 4000
                }
                conn.responseCode
                conn.disconnect()
            } catch (_: Exception) {
                // Ignore if emulator already empty
            }
        }
        thread.start()
        thread.join(5000)
    }
}
