package nl.smartpot

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException


class RegisterPlantActivity : AppCompatActivity() {
    private lateinit var functions: FirebaseFunctions
    private lateinit var plantId: EditText
    private lateinit var addPlantButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_plant)

        functions = FirebaseFunctions.getInstance()
        plantId = findViewById(R.id.plant_id)
        addPlantButton = findViewById(R.id.add_plant)
        auth = FirebaseAuth.getInstance()

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
                            "minLightIntensity" to 100,
                            "maxLightIntensity" to 500,
                            "minSoilMoisture" to 100,
                            "maxSoilMoisture" to 500,
                            "minTemperature" to 100,
                            "maxTemperature" to 500
                    )

                    functions
                            .getHttpsCallable("callableAddPlant")
                            .call(data)
                            .continueWith { task ->
                                // This continuation runs on either success or failure, but if the task
                                // has failed then result will throw an Exception which will be
                                // propagated down.
                                val result = task.result?.data as String
                                println(result)
                                result
                            }
                            .addOnCompleteListener(OnCompleteListener { task ->
                                if (!task.isSuccessful) {
                                    val e = task.exception
                                    if (e is FirebaseFunctionsException) {
                                        val code = e.code
                                        val details = e.details
                                        println(code)
                                        println(e)
                                    }
                                }
                            })
                }

            }
        }
    }
}