package com.biomedixai.ricefieldsamplelogger

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.biomedixai.ricefieldsamplelogger.adapter.CisElementAdapter
import com.biomedixai.ricefieldsamplelogger.adapter.SsrMarkerAdapter
import com.biomedixai.ricefieldsamplelogger.biokt.rice.CisElementResult
import com.biomedixai.ricefieldsamplelogger.biokt.rice.SsrMarkerResult
import com.biomedixai.ricefieldsamplelogger.data.SampleDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FastaAnalysisActivity : AppCompatActivity() {

    private lateinit var tvDetailSampleHeader: TextView
    private lateinit var tvDetailLocationWeather: TextView
    private lateinit var tvDetailGenomicMetrics: TextView

    private lateinit var rvDetailCisElements: RecyclerView
    private lateinit var rvDetailSsrMarkers: RecyclerView

    private lateinit var cisAdapter: CisElementAdapter
    private lateinit var ssrAdapter: SsrMarkerAdapter

    private var sampleDbId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fasta_analysis)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fastaAnalysisMain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sampleDbId = intent.getLongExtra("SAMPLE_DB_ID", -1L)
        if (sampleDbId == -1L) {
            Toast.makeText(this, "Invalid sample reference", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupRecyclerViews()
        loadSampleData()
    }

    private fun initViews() {
        tvDetailSampleHeader = findViewById(R.id.tvDetailSampleHeader)
        tvDetailLocationWeather = findViewById(R.id.tvDetailLocationWeather)
        tvDetailGenomicMetrics = findViewById(R.id.tvDetailGenomicMetrics)

        rvDetailCisElements = findViewById(R.id.rvDetailCisElements)
        rvDetailSsrMarkers = findViewById(R.id.rvDetailSsrMarkers)
    }

    private fun setupRecyclerViews() {
        cisAdapter = CisElementAdapter()
        rvDetailCisElements.layoutManager = LinearLayoutManager(this)
        rvDetailCisElements.adapter = cisAdapter

        ssrAdapter = SsrMarkerAdapter()
        rvDetailSsrMarkers.layoutManager = LinearLayoutManager(this)
        rvDetailSsrMarkers.adapter = ssrAdapter
    }

    private fun loadSampleData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = SampleDatabase.getDatabase(this@FastaAnalysisActivity).sampleDao()
            val sample = dao.getSampleById(sampleDbId)

            withContext(Dispatchers.Main) {
                if (sample != null) {
                    tvDetailSampleHeader.text = "Sample: ${sample.sampleId} (${sample.riceVariety})"
                    tvDetailLocationWeather.text = "📅 ${sample.collectionDate} | 📍 Lat: ${String.format("%.4f", sample.latitude)}, Lon: ${String.format("%.4f", sample.longitude)}\n🌤️ ${sample.temperature}°C | 💧 ${sample.humidity}% Humidity"

                    tvDetailGenomicMetrics.text = "Sequence Length: ${sample.sequenceLength} bp | GC Content: ${String.format("%.2f", sample.gcContent)}%"

                    val gson = Gson()
                    val cisType = object : TypeToken<List<CisElementResult>>() {}.type
                    val ssrType = object : TypeToken<List<SsrMarkerResult>>() {}.type

                    val cisList: List<CisElementResult> = try {
                        gson.fromJson(sample.cisElementsJson, cisType) ?: emptyList()
                    } catch (e: Exception) { emptyList() }

                    val ssrList: List<SsrMarkerResult> = try {
                        gson.fromJson(sample.ssrMarkersJson, ssrType) ?: emptyList()
                    } catch (e: Exception) { emptyList() }

                    cisAdapter.updateData(cisList)
                    ssrAdapter.updateData(ssrList)
                } else {
                    Toast.makeText(this@FastaAnalysisActivity, "Sample details unavailable", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}
