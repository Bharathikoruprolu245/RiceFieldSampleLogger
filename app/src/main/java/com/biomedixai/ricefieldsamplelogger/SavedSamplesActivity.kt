package com.biomedixai.ricefieldsamplelogger

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.biomedixai.ricefieldsamplelogger.adapter.SampleAdapter
import com.biomedixai.ricefieldsamplelogger.data.SampleDatabase
import com.biomedixai.ricefieldsamplelogger.data.SampleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavedSamplesActivity : AppCompatActivity() {

    private lateinit var spinnerFilterVariety: Spinner
    private lateinit var rvSavedSamples: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var sampleAdapter: SampleAdapter

    private var allSamplesList: List<SampleEntity> = emptyList()
    private var currentSelectedVariety: String = "All Varieties"

    private val RICE_VARIETIES_FILTER = arrayOf(
        "All Varieties",
        "IR64",
        "Swarna (MTU 7029)",
        "Samba Mahsuri (BPT 5204)",
        "Pusa Basmati 1121",
        "MTU 1010",
        "CR Dhan 307",
        "Telangana Sona (RNR 15048)",
        "Kala Namak",
        "Other Research Variety"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_saved_samples)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.savedSamplesMain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        try {
            spinnerFilterVariety = findViewById(R.id.spinnerFilterVariety)
            rvSavedSamples = findViewById(R.id.rvSavedSamples)
            tvEmptyState = findViewById(R.id.tvEmptyState)

            setupAdapter()
            setupFilterSpinner()
            loadSavedSamples()
        } catch (e: Exception) {
            Log.e("SavedSamplesActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error opening saved samples: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupAdapter() {
        sampleAdapter = SampleAdapter(
            onEditClick = { sample ->
                val intent = Intent(this, EditSampleActivity::class.java).apply {
                    putExtra("SAMPLE_DB_ID", sample.id)
                }
                startActivity(intent)
            },
            onDeleteClick = { sample ->
                confirmAndDeleteSample(sample)
            },
            onItemClick = { sample ->
                val intent = Intent(this, FastaAnalysisActivity::class.java).apply {
                    putExtra("SAMPLE_DB_ID", sample.id)
                }
                startActivity(intent)
            }
        )

        rvSavedSamples.layoutManager = LinearLayoutManager(this)
        rvSavedSamples.adapter = sampleAdapter
    }

    private fun setupFilterSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, RICE_VARIETIES_FILTER)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilterVariety.adapter = adapter

        spinnerFilterVariety.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentSelectedVariety = RICE_VARIETIES_FILTER[position]
                filterAndDisplaySamples()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadSavedSamples() {
        lifecycleScope.launch {
            try {
                val dao = SampleDatabase.getDatabase(this@SavedSamplesActivity).sampleDao()
                dao.getAllSamples().collectLatest { samples ->
                    allSamplesList = samples
                    filterAndDisplaySamples()
                }
            } catch (e: Exception) {
                Log.e("SavedSamplesActivity", "Error loading saved samples", e)
            }
        }
    }

    private fun filterAndDisplaySamples() {
        val filteredList = if (currentSelectedVariety == "All Varieties") {
            allSamplesList
        } else {
            allSamplesList.filter { it.riceVariety == currentSelectedVariety }
        }

        if (filteredList.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            rvSavedSamples.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            rvSavedSamples.visibility = View.VISIBLE
        }

        sampleAdapter.updateData(filteredList)
    }

    private fun confirmAndDeleteSample(sample: SampleEntity) {
        AlertDialog.Builder(this)
            .setTitle("Delete Sample Record")
            .setMessage("Are you sure you want to delete sample ${sample.sampleId} (${sample.riceVariety})?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val dao = SampleDatabase.getDatabase(this@SavedSamplesActivity).sampleDao()
                    dao.deleteSample(sample)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SavedSamplesActivity, "Sample deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
