package com.biomedixai.ricefieldsamplelogger

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.biomedixai.ricefieldsamplelogger.data.SampleDatabase
import com.biomedixai.ricefieldsamplelogger.data.SampleEntity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditSampleActivity : AppCompatActivity() {

    private lateinit var etEditSampleId: TextInputEditText
    private lateinit var spinnerEditRiceVariety: Spinner
    private lateinit var etEditCollectionDate: TextInputEditText
    private lateinit var etEditLatitude: TextInputEditText
    private lateinit var etEditLongitude: TextInputEditText
    private lateinit var btnUpdateSample: MaterialButton

    private var sampleDbId: Long = -1L
    private var existingSample: SampleEntity? = null

    private val RICE_VARIETIES = arrayOf(
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
        setContentView(R.layout.activity_edit_sample)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editSampleMain)) { v, insets ->
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
        setupVarietySpinner()
        loadExistingSampleData()

        btnUpdateSample.setOnClickListener {
            saveUpdatedSample()
        }
    }

    private fun initViews() {
        etEditSampleId = findViewById(R.id.etEditSampleId)
        spinnerEditRiceVariety = findViewById(R.id.spinnerEditRiceVariety)
        etEditCollectionDate = findViewById(R.id.etEditCollectionDate)
        etEditLatitude = findViewById(R.id.etEditLatitude)
        etEditLongitude = findViewById(R.id.etEditLongitude)
        btnUpdateSample = findViewById(R.id.btnUpdateSample)
    }

    private fun setupVarietySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, RICE_VARIETIES)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEditRiceVariety.adapter = adapter
    }

    private fun loadExistingSampleData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = SampleDatabase.getDatabase(this@EditSampleActivity).sampleDao()
            val sample = dao.getSampleById(sampleDbId)
            existingSample = sample

            withContext(Dispatchers.Main) {
                if (sample != null) {
                    etEditSampleId.setText(sample.sampleId)
                    etEditCollectionDate.setText(sample.collectionDate)
                    etEditLatitude.setText(sample.latitude.toString())
                    etEditLongitude.setText(sample.longitude.toString())

                    val index = RICE_VARIETIES.indexOf(sample.riceVariety)
                    if (index >= 0) {
                        spinnerEditRiceVariety.setSelection(index)
                    }
                } else {
                    Toast.makeText(this@EditSampleActivity, "Sample not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun saveUpdatedSample() {
        val current = existingSample ?: return
        val newSampleId = etEditSampleId.text.toString().trim()
        val newVariety = spinnerEditRiceVariety.selectedItem.toString()
        val newDate = etEditCollectionDate.text.toString().trim()
        val newLat = etEditLatitude.text.toString().toDoubleOrNull() ?: current.latitude
        val newLon = etEditLongitude.text.toString().toDoubleOrNull() ?: current.longitude

        if (newSampleId.isEmpty()) {
            etEditSampleId.error = "Sample ID cannot be empty"
            return
        }

        val updatedSample = current.copy(
            sampleId = newSampleId,
            riceVariety = newVariety,
            collectionDate = newDate,
            latitude = newLat,
            longitude = newLon
        )

        lifecycleScope.launch(Dispatchers.IO) {
            val dao = SampleDatabase.getDatabase(this@EditSampleActivity).sampleDao()
            dao.updateSample(updatedSample)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@EditSampleActivity, "Sample updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
