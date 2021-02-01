package nl.smartpot

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartView
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAStyle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import java.text.SimpleDateFormat
import java.util.*

class PlantActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions

    private lateinit var title: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var editButton: Button

    private lateinit var switchTitle: TextView
    private lateinit var switchRide: Switch


    private var temperature = mutableListOf<Any>()
    private var soilMoisture = mutableListOf<Any>()
    private var lightIntensity = mutableListOf<Any>()
    private var inputTime = mutableListOf<String>()
    private var inputTimestamp = mutableListOf<String>()

    private lateinit var dateOfLastMeasurement: String

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
        editButton = findViewById(R.id.editButton)
        switchRide = findViewById(R.id.switchRide)

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
        this.getGraphData(data, aaChartModel, aaChartView, true)
        swipeRefreshLayout.setOnRefreshListener {
            val newdata = hashMapOf(
                    "userId" to auth.currentUser!!.uid,
                    "plantId" to id,
                    "lastDate" to dateOfLastMeasurement
            )
            this.getGraphData(newdata, aaChartModel, aaChartView, false)
            swipeRefreshLayout.isRefreshing = false
        }

        editButton.setOnClickListener{
            val intent = Intent(this, RegisterPlantActivity::class.java)
            intent.putExtra("id", id)
            startActivity(intent)
            finish()
        }

        if (id != null) {
            getMoveRobot(id)

            switchRide.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    editMoveRobot(true, id)
                }
                else {
                    editMoveRobot(false, id)
                }
            }
        }
    }

    private fun getMoveRobot(plantId: String) {
        val data = hashMapOf(
                "userId" to auth.currentUser!!.uid,
                "plantId" to plantId
        )
        functions
                .getHttpsCallable("callableGetMoveRobot")
                .call(data)
                .continueWith { task ->
                    val moveRobot: Boolean = task.result?.data as Boolean
                    switchRide.setChecked(moveRobot)
                }

    }

    private fun editMoveRobot(move: Boolean, plantId: String) {
        val data = hashMapOf(
                "userId" to auth.currentUser!!.uid,
                "plantId" to plantId,
                "moveRobot" to move
        )
        functions
                .getHttpsCallable("callableEditMoveRobot")
                .call(data)
    }

    public fun getGraphData(data: HashMap<String, String?>, aaChartModel: AAChartModel, aaChartView: AAChartView, init: Boolean = false){
        functions
                .getHttpsCallable("callableGetLastMeasurement")
                .call(data)
                .continueWith { task ->

                    val result: ArrayList<HashMap<String, Any>> = task.result?.data as ArrayList<HashMap<String, Any>>
                    result.forEach { item ->
                        temperature.add(item["temperature"]!!)
                        soilMoisture.add(item["soilMoisture"]!!)
                        lightIntensity.add(item["lightIntensity"]!!)

                        val timestamp = item["timeStamp"] as HashMap<*, *>
                        val seconds = timestamp["_seconds"] as Int
                        val nanoseconds = timestamp["_nanoseconds"] as Int

                        val longseconds = seconds.toLong()
                        val longnanoseconds = nanoseconds.toLong()

                        val milliseconds = (longseconds * 1000) + (longnanoseconds / 1000000)
                        val sdf = SimpleDateFormat("MM/dd/yyyy hh:mm")
                        val netDate = Date(milliseconds)
                        val date = sdf.format(netDate).toString()

                        inputTime.add(date)
                        inputTimestamp.add(milliseconds.toString())
                    }
                    dateOfLastMeasurement = inputTimestamp.last()

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
                    var testsddd = arrayOf(
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

                    aaChartModel.categories(inputTime.toTypedArray())
                    //The chart view object calls the instance object of AAChartModel and draws the final graphic
                    if(init){
                        aaChartView.aa_drawChartWithChartModel(aaChartModel)
                        val mainHandler = Handler(Looper.getMainLooper())
                        val obj = MyRunnable(data, dateOfLastMeasurement, aaChartModel, aaChartView,this, mainHandler)
                        mainHandler.post(obj)
                    } else {
                        aaChartView.aa_onlyRefreshTheChartDataWithChartOptionsSeriesArray(testsddd)
                    }


                    result
                }
    }
}
class MyRunnable: Runnable {
    private lateinit var data: HashMap<String, String?>
    private lateinit var aaChartModel: AAChartModel
    private lateinit var aaChartView: AAChartView
    private lateinit var plantActivity: PlantActivity
    private lateinit var mainHandler: Handler
    private lateinit var dateOfLastMeasurement: String

    constructor(data: HashMap<String, String?>, dateOfLastMeasurement: String,aaChartModel: AAChartModel, aaChartView: AAChartView, plantActivity: PlantActivity, mainHandler: Handler) {
        this.data = data
        this.aaChartModel = aaChartModel
        this.aaChartView = aaChartView
        this.plantActivity = plantActivity
        this.mainHandler = mainHandler
        this.dateOfLastMeasurement = dateOfLastMeasurement

        this.data.put("lastDate", this.dateOfLastMeasurement)
    }

    override fun run() {
        plantActivity.getGraphData(data, aaChartModel, aaChartView, false)
        mainHandler.postDelayed(this, 5000);
    }
}