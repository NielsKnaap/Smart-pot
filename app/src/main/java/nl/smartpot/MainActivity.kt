package nl.smartpot

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions

    private lateinit var logoutBtn: Button
    private lateinit var updatePass: Button
    private lateinit var addPlantBtn: Button

    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerAdapter: RecyclerAdapter
    private var plantList = mutableListOf<String>()
    private var displayList = mutableListOf<String>()

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getFirebaseInstances()
        addGoogleTokenToUser()
        checkIfUserLoggedIn()

        setContentView(R.layout.activity_main)
        setActivityVariables()

        setLoginListeners()

        val userData = hashMapOf(
                "userId" to auth.currentUser!!.uid
        )
        getPlantsFB(userData)

        setOtherListeners()

    }

    override fun onResume() {
        super.onResume()

        val data = hashMapOf(
                "userId" to auth.currentUser!!.uid
        )
        this.updatePlants(data)
    }

    private fun updatePlants(data: HashMap<String, String>, swipeRefresh: Boolean = false){
        functions
                .getHttpsCallable("callableGetPlants")
                .call(data)
                .continueWith { task ->
                    val result: ArrayList<HashMap<String, String>> = task.result?.data as ArrayList<HashMap<String, String>>
                    displayList.clear()
                    result.forEach { item ->
                        displayList.add(item["plantId"].toString())
                    }
                    recyclerView.adapter!!.notifyDataSetChanged()

                    if(swipeRefresh){
                        swipeRefreshLayout.isRefreshing = false
                    }

                    result
                }
    }

    private fun getFirebaseInstances() {
        auth = FirebaseAuth.getInstance()
        functions = FirebaseFunctions.getInstance()
    }

    private fun addGoogleTokenToUserFB(data: HashMap<String, String>) {
        functions
                .getHttpsCallable("addGoogleTokenToUser")
                .call(data)
                .continueWith { task ->
                    val result: ArrayList<HashMap<String, String>> = task.result?.data as ArrayList<HashMap<String, String>>

                    result
                }
    }

    private fun addGoogleTokenToUser() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("GOOGLTOKEN", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
            val msg = token.toString()

            val data = hashMapOf(
                    "userId" to auth.currentUser!!.uid,
                    "googleToken" to msg
            )
            addGoogleTokenToUserFB(data)
        })
    }

    private fun checkIfUserLoggedIn() {
        if(auth.currentUser == null){
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        } else{
            Toast.makeText(this, "Already logged in", Toast.LENGTH_LONG).show()
        }
    }

    private fun setActivityVariables() {
        logoutBtn = findViewById(R.id.logout_btn)
        updatePass = findViewById(R.id.update_pass_btn)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
    }

    private fun getPlantsFB(data : HashMap<String, String>) {
        functions
                .getHttpsCallable("callableGetPlants")
                .call(data)
                .continueWith { task ->
                    val result: ArrayList<HashMap<String, String>> = task.result?.data as ArrayList<HashMap<String, String>>
                    result.forEach { item ->
                        plantList.add(item["plantId"].toString())
                    }
                    Log.d("plantData", plantList.toString())
                    displayList.addAll(plantList)

                    recyclerView = findViewById(R.id.recyclerview)
                    recyclerAdapter = RecyclerAdapter(displayList, this)
                    recyclerView.adapter = recyclerAdapter

                    result
                }
    }

    private fun setLoginListeners() {
        updatePass.setOnClickListener{
            val intent = Intent(this, UpdatePasswordActivity::class.java)
            startActivity(intent)
            finish()
        }

        logoutBtn.setOnClickListener{
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setOtherListeners() {
        findViewById<FloatingActionButton>(R.id.add_plant_btn).setOnClickListener {
            val intent = Intent(this, RegisterPlantActivity::class.java)
            startActivity(intent)
        }

        swipeRefreshLayout.setOnRefreshListener {
            val data = hashMapOf(
                    "userId" to auth.currentUser!!.uid
            )
            this.updatePlants(data, true)
        }
    }
}