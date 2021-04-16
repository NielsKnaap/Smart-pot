package nl.smartpot

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar
import com.crystal.crystalrangeseekbar.widgets.CrystalSeekbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlin.collections.HashMap


class RegisterPlantActivity : AppCompatActivity() {
    private lateinit var functions: FirebaseFunctions
    private lateinit var plantId: EditText
    private lateinit var addPlantButton: Button
    private lateinit var auth: FirebaseAuth

    private lateinit var tempRange: CrystalRangeSeekbar
    private lateinit var tempRangeMin: TextView
    private lateinit var tempRangeMax: TextView
    private var tempRangeMinValue: Number = 0
    private var tempRangeMaxValue: Number = 100

    private lateinit var lightRange: CrystalRangeSeekbar
    private lateinit var lightRangeMin: TextView
    private lateinit var lightRangeMax: TextView
    private var lightRangeMinValue: Number = 5000
    private var lightRangeMaxValue: Number = 30000

    private lateinit var moistureRange: CrystalRangeSeekbar
    private lateinit var moistureRangeMin: TextView
    private lateinit var moistureRangeMax: TextView
    private var moistureRangeMinValue: Number = 0
    private var moistureRangeMaxValue: Number = 1024

    private lateinit var measurementFrequencyRange: CrystalSeekbar
    private lateinit var measurementFrequencyText: TextView
    private var measurementFrequencyValue: Number = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_plant)

        setTextVariables()

        val intent: Intent = intent
        val id: String? = intent.getStringExtra("id")

        addPlantListener(id)
        setRegistrySliders()

        if (id !== null) {
            addPlantButton.text = "Update plant"
            Toast.makeText(this, "Edit mode", Toast.LENGTH_LONG).show()

            val data = hashMapOf(
                    "userId" to auth.currentUser!!.uid,
                    "plantId" to id
            )

            getPlantFB(id, data)
        }
    }

    private fun setTextVariables() {
        functions = FirebaseFunctions.getInstance()
        plantId = findViewById(R.id.plant_id)
        addPlantButton = findViewById(R.id.add_plant)
        auth = FirebaseAuth.getInstance()

        tempRange = findViewById(R.id.tempRange)
        tempRangeMin = findViewById(R.id.tempRangeMin)
        tempRangeMax = findViewById(R.id.tempRangeMax)

        lightRange = findViewById(R.id.lightRange)
        lightRangeMin = findViewById(R.id.lightRangeMin)
        lightRangeMax = findViewById(R.id.lightRangeMax)

        moistureRange = findViewById(R.id.moistureRange)
        moistureRangeMin = findViewById(R.id.moistureRangeMin)
        moistureRangeMax = findViewById(R.id.moistureRangeMax)

        measurementFrequencyRange = findViewById(R.id.measureFrequencyBar)
        measurementFrequencyText = findViewById(R.id.measureFrequencyText)
    }

    @SuppressLint("SetTextI18n")
    private fun setRegistrySliders() {
        tempRange.setOnRangeSeekbarChangeListener { minValue, maxValue ->
            tempRangeMinValue = minValue
            tempRangeMaxValue = maxValue
            tempRangeMin.text = "$minValue°"
            tempRangeMax.text = "$maxValue°"
        }

        lightRange.setOnRangeSeekbarChangeListener { minValue, maxValue ->
            lightRangeMinValue = minValue
            lightRangeMaxValue = maxValue
            lightRangeMin.text = "$minValue lux"
            lightRangeMax.text = "$maxValue lux"
        }

        moistureRange.setOnRangeSeekbarChangeListener { minValue, maxValue ->
            moistureRangeMinValue = minValue
            moistureRangeMaxValue = maxValue
            moistureRangeMin.text = "$minValue"
            moistureRangeMax.text = "$maxValue"
        }

        measurementFrequencyRange.setOnSeekbarChangeListener { minValue ->
            measurementFrequencyText.text = minValue.toString()
            measurementFrequencyValue = minValue
        }
    }

    private fun addPlantListener(id: String?) {
        addPlantButton.setOnClickListener {
            val plantId: String = plantId.text.toString()
            if (TextUtils.isEmpty(plantId)) {
                Toast.makeText(this, "Please enter plantId", Toast.LENGTH_LONG).show()
            } else {
                if (auth.currentUser == null) {
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    val data = createPlantRegistryHashMap()
                    val function = if (id !== null) "callableEditPlant" else "callableAddPlant"
                    registerPlantFB(function, data)
                }
            }
        }
    }

    private fun createPlantRegistryHashMap(): HashMap<String, Any> {
        return hashMapOf(
                "userId" to auth.currentUser!!.uid,
                "plantId" to plantId.text.toString(),
                "minLightIntensity" to lightRangeMinValue,
                "maxLightIntensity" to lightRangeMaxValue,
                "minSoilMoisture" to moistureRangeMinValue,
                "maxSoilMoisture" to moistureRangeMaxValue,
                "minTemperature" to tempRangeMinValue,
                "maxTemperature" to tempRangeMaxValue,
                "measureFrequency" to measurementFrequencyValue)
    }

    private fun registerPlantFB(function: String, data: HashMap<String, Any>) {

        functions
                .getHttpsCallable(function)
                .call(data)
                .continueWith { task ->
                    val result = task.result?.data as String
                    result
                }
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        val e = task.exception
                        if (e is FirebaseFunctionsException) {
                            e.code
                            e.details
                        }
                    }
                    finish()
                }
    }

    private fun getPlantFB(id: String, data: HashMap<String, String>) {
        functions
                .getHttpsCallable("callableGetPlant")
                .call(data)
                .continueWith { task ->
                    val result: HashMap<String, Int> = task.result?.data as HashMap<String, Int>
                    val maxTemperature = result["maxTemperature"]!!.toFloat()
                    val minTemperature = result["minTemperature"]!!.toFloat()
                    val maxSoilMoisture = result["maxSoilMoisture"]!!.toFloat()
                    val minSoilMoisture = result["minSoilMoisture"]!!.toFloat()
                    val minLightIntensity = result["minLightIntensity"]!!.toFloat()
                    val maxLightIntensity = result["maxLightIntensity"]!!.toFloat()
                    val measureFrequency = result["measureFrequency"]!!.toFloat()

                    plantId.setText(id)
                    plantId.isEnabled = false

                    if(minSoilMoisture != 0.0F){
                        moistureRange.setMinStartValue(minSoilMoisture).apply()
                    }
                    moistureRange.setMaxStartValue(maxSoilMoisture).apply()

                    if(minTemperature != 0.0F){
                        tempRange.setMinStartValue(minTemperature).apply()
                    }
                    tempRange.setMaxStartValue(maxTemperature).apply()

                    if(minLightIntensity != 0.0F){
                        lightRange.setMinStartValue(minLightIntensity).apply()
                    }
                    lightRange.setMaxStartValue(maxLightIntensity).apply()

                    measurementFrequencyRange.setMinStartValue(measureFrequency).apply()

                }
    }
}