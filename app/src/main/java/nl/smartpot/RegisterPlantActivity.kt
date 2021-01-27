package nl.smartpot

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarChangeListener
import com.crystal.crystalrangeseekbar.interfaces.OnSeekbarChangeListener
import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar
import com.crystal.crystalrangeseekbar.widgets.CrystalSeekbar
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import java.util.*


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

        val intent: Intent = getIntent();
        val id: String? = intent.getStringExtra("id")

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

        addPlantButton.setOnClickListener {
            var plantId: String = plantId.text.toString()
            if (TextUtils.isEmpty(plantId)) {
                Toast.makeText(this, "Please enter plantId", Toast.LENGTH_LONG).show()
            } else {
                if (auth.currentUser == null) {
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Plant id is entered
                    val data = hashMapOf(
                            "userId" to auth.currentUser!!.uid,
                            "plantId" to plantId,
                            "minLightIntensity" to lightRangeMinValue,
                            "maxLightIntensity" to lightRangeMaxValue,
                            "minSoilMoisture" to moistureRangeMinValue,
                            "maxSoilMoisture" to moistureRangeMaxValue,
                            "minTemperature" to tempRangeMinValue,
                            "maxTemperature" to tempRangeMaxValue,
                            "measureFrequency" to measurementFrequencyValue
                    )

                    var function = if (id !== null) "callableEditPlant" else "callableAddPlant"

                    functions
                            .getHttpsCallable(function)
                            .call(data)
                            .continueWith { task ->
                                val result = task.result?.data as String
                                result
                            }
                            .addOnCompleteListener(OnCompleteListener { task ->
                                if (!task.isSuccessful) {
                                    val e = task.exception
                                    if (e is FirebaseFunctionsException) {
                                        val code = e.code
                                        val details = e.details
                                        //TODO: Show user feedback
                                    }
                                }
                                finish()
                            })
                }

            }
        }

        tempRange.setOnRangeSeekbarChangeListener(OnRangeSeekbarChangeListener { minValue, maxValue ->
            tempRangeMinValue = minValue
            tempRangeMaxValue = maxValue
            tempRangeMin.setText("$minValue°")
            tempRangeMax.setText("$maxValue°")
        })

        lightRange.setOnRangeSeekbarChangeListener(OnRangeSeekbarChangeListener { minValue, maxValue ->
            lightRangeMinValue = minValue
            lightRangeMaxValue = maxValue
            lightRangeMin.setText("$minValue lux")
            lightRangeMax.setText("$maxValue lux")
        })

        moistureRange.setOnRangeSeekbarChangeListener(OnRangeSeekbarChangeListener { minValue, maxValue ->
            moistureRangeMinValue = minValue
            moistureRangeMaxValue = maxValue
            moistureRangeMin.setText("$minValue")
            moistureRangeMax.setText("$maxValue")
        })

        measurementFrequencyRange.setOnSeekbarChangeListener(OnSeekbarChangeListener { minValue ->
            measurementFrequencyText.setText(minValue.toString())
            measurementFrequencyValue = minValue
        })

        if (id !== null) {
            addPlantButton.setText("Update plant")
            // Get the current values of the plant
            Toast.makeText(this, "Edit mode", Toast.LENGTH_LONG).show()

            val data = hashMapOf(
                    "userId" to auth.currentUser!!.uid,
                    "plantId" to id
            )
            functions
                    .getHttpsCallable("callableGetPlant")
                    .call(data)
                    .continueWith { task ->
                        val result: HashMap<String, Int> = task.result?.data as HashMap<String, Int>
                        var maxTemperature = result["maxTemperature"]!!.toFloat()
                        var minTemperature = result["minTemperature"]!!.toFloat()
                        var maxSoilMoisture = result["maxSoilMoisture"]!!.toFloat()
                        var minSoilMoisture = result["minSoilMoisture"]!!.toFloat()
                        var minLightIntensity = result["minLightIntensity"]!!.toFloat()
                        var maxLightIntensity = result["maxLightIntensity"]!!.toFloat()
                        var measureFrequency = result["measureFrequency"]!!.toFloat()

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
}