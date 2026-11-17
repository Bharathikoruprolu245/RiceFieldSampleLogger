package com.biomedixai.ricefieldsamplelogger

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.biomedixai.ricefieldsamplelogger.adapter.CisElementAdapter
import com.biomedixai.ricefieldsamplelogger.adapter.SsrMarkerAdapter
import com.biomedixai.ricefieldsamplelogger.biokt.SeqIO
import com.biomedixai.ricefieldsamplelogger.biokt.rice.CisElementResult
import com.biomedixai.ricefieldsamplelogger.biokt.rice.SsrMarkerResult
import com.biomedixai.ricefieldsamplelogger.biokt.rice.detectSSRs
import com.biomedixai.ricefieldsamplelogger.biokt.rice.scanRicePromoter
import com.biomedixai.ricefieldsamplelogger.data.SampleDatabase
import com.biomedixai.ricefieldsamplelogger.data.SampleEntity
import com.biomedixai.ricefieldsamplelogger.network.RetrofitClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NewSampleActivity : AppCompatActivity() {

    private lateinit var etSampleId: TextInputEditText
    private lateinit var spinnerRiceVariety: Spinner
    private lateinit var etCollectionDate: TextInputEditText

    private lateinit var tvLocationCoords: TextView
    private lateinit var btnFetchLocation: MaterialButton

    private lateinit var tvWeatherData: TextView
    private lateinit var btnFetchWeather: MaterialButton

    private lateinit var btnPickFasta: MaterialButton
    private lateinit var tvFastaFileName: TextView
    private lateinit var tvFastaSummary: TextView

    private lateinit var rvCisElements: RecyclerView
    private lateinit var rvSsrMarkers: RecyclerView
    private lateinit var btnSaveSample: MaterialButton

    private lateinit var cisAdapter: CisElementAdapter
    private lateinit var ssrAdapter: SsrMarkerAdapter

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0

    private var currentTemp: Double = 0.0
    private var currentHumidity: Double = 0.0
    private var currentPrecipitation: Double = 0.0
    private var currentSoilMoisture: Double = 0.0

    private var sequenceLength: Int = 0
    private var gcContent: Double = 0.0
    private var cisElementsList: List<CisElementResult> = emptyList()
    private var ssrMarkersList: List<SsrMarkerResult> = emptyList()

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

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            fetchGpsLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val fastaFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processFastaUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_sample)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.newSampleMain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initViews()
        setupVarietySpinner()
        setupAutoDateTime()
        setupRecyclerViews()
        setupClickListeners()
    }

    private fun initViews() {
        etSampleId = findViewById(R.id.etSampleId)
        spinnerRiceVariety = findViewById(R.id.spinnerRiceVariety)
        etCollectionDate = findViewById(R.id.etCollectionDate)

        tvLocationCoords = findViewById(R.id.tvLocationCoords)
        btnFetchLocation = findViewById(R.id.btnFetchLocation)

        tvWeatherData = findViewById(R.id.tvWeatherData)
        btnFetchWeather = findViewById(R.id.btnFetchWeather)

        btnPickFasta = findViewById(R.id.btnPickFasta)
        tvFastaFileName = findViewById(R.id.tvFastaFileName)
        tvFastaSummary = findViewById(R.id.tvFastaSummary)

        rvCisElements = findViewById(R.id.rvCisElements)
        rvSsrMarkers = findViewById(R.id.rvSsrMarkers)
        btnSaveSample = findViewById(R.id.btnSaveSample)

        // Generate auto sample ID
        val timeStampId = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())
        etSampleId.setText("RICE-$timeStampId")
    }

    private fun setupVarietySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, RICE_VARIETIES)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRiceVariety.adapter = adapter
    }

    private fun setupAutoDateTime() {
        val currentDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        etCollectionDate.setText(currentDateTime)
    }

    private fun setupRecyclerViews() {
        cisAdapter = CisElementAdapter()
        rvCisElements.layoutManager = LinearLayoutManager(this)
        rvCisElements.adapter = cisAdapter

        ssrAdapter = SsrMarkerAdapter()
        rvSsrMarkers.layoutManager = LinearLayoutManager(this)
        rvSsrMarkers.adapter = ssrAdapter
    }

    private fun setupClickListeners() {
        btnFetchLocation.setOnClickListener {
            checkLocationPermissionsAndFetch()
        }

        btnFetchWeather.setOnClickListener {
            fetchOpenMeteoWeather()
        }

        btnPickFasta.setOnClickListener {
            fastaFilePickerLauncher.launch("*/*")
        }

        btnSaveSample.setOnClickListener {
            saveSampleToRoom()
        }
    }

    private fun checkLocationPermissionsAndFetch() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            fetchGpsLocation()
        }
    }

    @Suppress("MissingPermission")
    private fun fetchGpsLocation() {
        tvLocationCoords.text = "Fetching GPS location..."

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        currentLatitude = location.latitude
                        currentLongitude = location.longitude
                        tvLocationCoords.text = "Latitude: ${String.format("%.6f", currentLatitude)}\nLongitude: ${String.format("%.6f", currentLongitude)}"
                        Toast.makeText(this, "Location fetched successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        tvLocationCoords.text = "Unable to get current GPS fix. Make sure Location/GPS is ON."
                    }
                }
                .addOnFailureListener { e ->
                    tvLocationCoords.text = "Error fetching location: ${e.localizedMessage}"
                }
        }
    }

    private fun fetchOpenMeteoWeather() {
        if (currentLatitude == 0.0 && currentLongitude == 0.0) {
            Toast.makeText(this, "Please fetch GPS location first", Toast.LENGTH_SHORT).show()
            return
        }

        tvWeatherData.text = "Fetching weather from Open-Meteo API..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getWeather(
                    latitude = currentLatitude,
                    longitude = currentLongitude
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val currentWeather = response.body()?.current
                        if (currentWeather != null) {
                            currentTemp = currentWeather.temperature2m ?: 0.0
                            currentHumidity = currentWeather.relativeHumidity2m ?: 0.0
                            currentPrecipitation = currentWeather.precipitation ?: 0.0
                            currentSoilMoisture = currentWeather.soilMoisture0To1cm ?: 0.0

                            tvWeatherData.text = """
                                Temperature: $currentTemp °C
                                Relative Humidity: $currentHumidity %
                                Precipitation: $currentPrecipitation mm
                                Soil Moisture (0-1cm): $currentSoilMoisture m³/m³
                            """.trimIndent()
                            Toast.makeText(this@NewSampleActivity, "Weather data updated", Toast.LENGTH_SHORT).show()
                        } else {
                            tvWeatherData.text = "No current weather payload returned."
                        }
                    } else {
                        tvWeatherData.text = "Open-Meteo API Error: ${response.code()}"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("Weather", "Failed to fetch weather", e)
                    tvWeatherData.text = "Failed to fetch weather: ${e.localizedMessage}"
                }
            }
        }
    }

    private fun processFastaUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val content = contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).readText()
                } ?: ""

                val records = SeqIO.parseString(content)
                val primaryRecord = records.firstOrNull()

                withContext(Dispatchers.Main) {
                    if (primaryRecord != null) {
                        tvFastaFileName.text = "File: ${uri.lastPathSegment ?: "FASTA File"}"
                        sequenceLength = primaryRecord.length
                        gcContent = primaryRecord.calculateGcContent()

                        tvFastaSummary.text = "Seq Length: $sequenceLength bp | GC Content: ${String.format("%.2f", gcContent)}%"

                        // bioKT Rice Analysis
                        cisElementsList = scanRicePromoter(primaryRecord.sequence)
                        ssrMarkersList = detectSSRs(primaryRecord.sequence)

                        cisAdapter.updateData(cisElementsList)
                        ssrAdapter.updateData(ssrMarkersList)

                        Toast.makeText(
                            this@NewSampleActivity,
                            "FASTA parsed: ${cisElementsList.size} Cis-elements & ${ssrMarkersList.size} SSRs found",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        tvFastaFileName.text = "No valid FASTA records found"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("FASTA", "Failed to read FASTA file", e)
                    Toast.makeText(this@NewSampleActivity, "Error reading FASTA file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveSampleToRoom() {
        val sampleIdInput = etSampleId.text.toString().trim()
        val variety = spinnerRiceVariety.selectedItem.toString()
        val dateInput = etCollectionDate.text.toString().trim()

        if (sampleIdInput.isEmpty()) {
            etSampleId.error = "Sample ID is required"
            return
        }

        val gson = Gson()
        val cisJson = gson.toJson(cisElementsList)
        val ssrJson = gson.toJson(ssrMarkersList)

        val sample = SampleEntity(
            sampleId = sampleIdInput,
            riceVariety = variety,
            collectionDate = dateInput,
            latitude = currentLatitude,
            longitude = currentLongitude,
            temperature = currentTemp,
            humidity = currentHumidity,
            precipitation = currentPrecipitation,
            soilMoisture = currentSoilMoisture,
            sequenceLength = sequenceLength,
            gcContent = gcContent,
            cisElementsJson = cisJson,
            ssrMarkersJson = ssrJson
        )

        lifecycleScope.launch(Dispatchers.IO) {
            val dao = SampleDatabase.getDatabase(this@NewSampleActivity).sampleDao()
            dao.insertSample(sample)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@NewSampleActivity, "Sample $sampleIdInput saved successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
