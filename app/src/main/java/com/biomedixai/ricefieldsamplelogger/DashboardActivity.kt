package com.biomedixai.ricefieldsamplelogger

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.biomedixai.ricefieldsamplelogger.biokt.rice.CisElementResult
import com.biomedixai.ricefieldsamplelogger.biokt.rice.SsrMarkerResult
import com.biomedixai.ricefieldsamplelogger.data.SampleDatabase
import com.biomedixai.ricefieldsamplelogger.data.SampleEntity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var welcomeUserText: TextView
    private lateinit var totalSamplesCount: TextView
    private lateinit var varietiesCount: TextView
    private lateinit var btnNewSample: MaterialButton
    private lateinit var btnSeedDemoSample: MaterialButton
    private lateinit var btnViewSavedSamples: MaterialButton
    private lateinit var btnSignOutDashboard: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dashboardMain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        try {
            auth = FirebaseAuth.getInstance()
            val user = auth.currentUser

            welcomeUserText = findViewById(R.id.welcomeUserText)
            totalSamplesCount = findViewById(R.id.totalSamplesCount)
            varietiesCount = findViewById(R.id.varietiesCount)
            btnNewSample = findViewById(R.id.btnNewSample)
            btnSeedDemoSample = findViewById(R.id.btnSeedDemoSample)
            btnViewSavedSamples = findViewById(R.id.btnViewSavedSamples)
            btnSignOutDashboard = findViewById(R.id.btnSignOutDashboard)

            if (user != null) {
                val name = if (user.isAnonymous) "Guest Researcher" else (user.displayName ?: user.email ?: "Researcher")
                welcomeUserText.text = "Researcher: $name"
            } else {
                welcomeUserText.text = "Researcher: Guest"
            }

            btnNewSample.setOnClickListener {
                val intent = Intent(this, NewSampleActivity::class.java)
                startActivity(intent)
            }

            btnSeedDemoSample.setOnClickListener {
                seedDemoSample()
            }

            btnViewSavedSamples.setOnClickListener {
                val intent = Intent(this, SavedSamplesActivity::class.java)
                startActivity(intent)
            }

            btnSignOutDashboard.setOnClickListener {
                auth.signOut()
                Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }

            loadSampleStats()
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error opening dashboard: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun seedDemoSample() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sampleId = "RICE-DEMO-" + SimpleDateFormat("mmss", Locale.getDefault()).format(Date())
                val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                val cisList = listOf(
                    CisElementResult("TATA-box", "TATAAA", 12, "Core promoter motif"),
                    CisElementResult("W-box", "TTGACC", 45, "WRKY stress response element"),
                    CisElementResult("G-box", "CACGTG", 88, "Light responsive element")
                )
                val ssrList = listOf(
                    SsrMarkerResult("(GA)5", 5, 20, 30),
                    SsrMarkerResult("(AT)6", 6, 110, 122)
                )

                val gson = Gson()
                val sample = SampleEntity(
                    sampleId = sampleId,
                    riceVariety = "IR64",
                    collectionDate = currentDate,
                    latitude = 17.3850,
                    longitude = 78.4867,
                    temperature = 29.2,
                    humidity = 68.0,
                    precipitation = 0.5,
                    soilMoisture = 0.38,
                    sequenceLength = 1250,
                    gcContent = 49.5,
                    cisElementsJson = gson.toJson(cisList),
                    ssrMarkersJson = gson.toJson(ssrList)
                )

                val dao = SampleDatabase.getDatabase(this@DashboardActivity).sampleDao()
                dao.insertSample(sample)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DashboardActivity, "Demo sample $sampleId added successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DashboardActivity, "Failed to seed demo sample: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadSampleStats() {
        lifecycleScope.launch {
            try {
                val dao = SampleDatabase.getDatabase(this@DashboardActivity).sampleDao()
                dao.getAllSamples().collectLatest { samples ->
                    totalSamplesCount.text = samples.size.toString()
                    val uniqueVarieties = samples.map { it.riceVariety }.distinct().count()
                    varietiesCount.text = uniqueVarieties.toString()
                }
            } catch (e: Exception) {
                Log.e("DashboardActivity", "Error loading stats", e)
            }
        }
    }
}
