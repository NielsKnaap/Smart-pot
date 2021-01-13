package nl.smartpot

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        functions = FirebaseFunctions.getInstance()

        if(auth.currentUser == null){
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }else{
            Toast.makeText(this, "Already logged in", Toast.LENGTH_LONG).show()
        }

        setContentView(R.layout.activity_main)

        logoutBtn = findViewById(R.id.logout_btn)
        updatePass = findViewById(R.id.update_pass_btn)

        logoutBtn.setOnClickListener{
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        val data = hashMapOf(
                "userId" to auth.currentUser!!.uid
        )
        functions
                .getHttpsCallable("callableGetPlants")
                .call(data)
                .continueWith { task ->
                    val result = task.result?.data
                    result
                }

        // TODO: Change this array to the dynamic result of the Google Cloud Function
        plantList.add("test1")
        plantList.add("test2")
        plantList.add("test3")
        plantList.add("test4")

        displayList.addAll(plantList)
        recyclerView = findViewById(R.id.recyclerview)
        recyclerAdapter = RecyclerAdapter(displayList)

        recyclerView.adapter = recyclerAdapter

        findViewById<FloatingActionButton>(R.id.add_plant_btn).setOnClickListener {
            val intent = Intent(this, RegisterPlantActivity::class.java)
            startActivity(intent)
        }
    }
}