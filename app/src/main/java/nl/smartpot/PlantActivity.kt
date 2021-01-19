package nl.smartpot

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartView
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAStyle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions

class PlantActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions

    private lateinit var title: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var temperature = mutableListOf<Any>()
    private var soilMoisture = mutableListOf<Any>()
    private var lightIntensity = mutableListOf<Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant)

        auth = FirebaseAuth.getInstance()
        functions = FirebaseFunctions.getInstance()

        val intent: Intent = getIntent();
        val id: String? = intent.getStringExtra("id")

        if (id === null) {
            // Go back home
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        title = findViewById(R.id.titleView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        title.setText(id)

        val aaChartView = findViewById<AAChartView>(R.id.aa_chart_view)

        val aaStyle : AAStyle = AAStyle()
                .color("#fff")

        val aaChartModel : AAChartModel = AAChartModel()
                .chartType(AAChartType.Area)
                .title("Afgelopen 24 uur")
                .backgroundColor("#0d1117")
                .dataLabelsEnabled(false)
                .axesTextColor("#fff")
                .titleStyle(aaStyle)

        val data = hashMapOf(
                "userId" to auth.currentUser!!.uid,
                "plantId" to id
        )
        this.getGraphData(data, aaChartModel, aaChartView)
        swipeRefreshLayout.setOnRefreshListener {
            this.getGraphData(data, aaChartModel, aaChartView)
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun getGraphData(data: HashMap<String, String?>, aaChartModel:AAChartModel, aaChartView: AAChartView){
        functions
                .getHttpsCallable("callableGetLastMeasurement")
                .call(data)
                .continueWith { task ->
                    val result: ArrayList<HashMap<String, Any>> = task.result?.data as ArrayList<HashMap<String, Any>>
                    result.forEach { item ->
                        temperature.add(item["temperature"]!!)
                        soilMoisture.add(item["soilMoisture"]!!)
                        lightIntensity.add(item["lightIntensity"]!!)
                    }

                    aaChartModel.series(arrayOf(
                            AASeriesElement()
                                    .name("Temperatuur")
                                    .data(temperature.toTypedArray()),
                            AASeriesElement()
                                    .name("Vochtigheid")
                                    .data(soilMoisture.toTypedArray()),
                            AASeriesElement()
                                    .name("Licht")
                                    .data(lightIntensity.toTypedArray())
                    )
                    )
                    //The chart view object calls the instance object of AAChartModel and draws the final graphic
                    aaChartView.aa_drawChartWithChartModel(aaChartModel)

                    result
                }
    }
}