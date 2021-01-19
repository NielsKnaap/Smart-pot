package nl.smartpot

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions

class RecyclerAdapter(plantList: MutableList<String>, context: Context): RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

    private var plants: MutableList<String> = plantList
    private var context: Context = context

    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.card_layout, parent, false)
        return ViewHolder(v, this.context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemTitle.text = plants[position]

        auth = FirebaseAuth.getInstance()
        functions = FirebaseFunctions.getInstance()

        val data = hashMapOf(
                "userId" to auth.currentUser!!.uid,
                "plantId" to plants[position],
                "limit" to 1
        )
        functions
                .getHttpsCallable("callableGetLastMeasurement")
                .call(data)
                .continueWith { task ->
                    val result: ArrayList<HashMap<String, Any>> = task.result?.data as ArrayList<HashMap<String, Any>>
                    val tempValue = result[0]["temperature"]
                    holder.itemTemp.text = tempValue.toString()
                }
    }

    override fun getItemCount(): Int {
        return plants.size
    }

    inner class ViewHolder(itemView: View, context: Context): RecyclerView.ViewHolder(itemView) {
        var itemImage: ImageView
        var itemTitle: TextView
        var itemTemp: TextView

        init {
            itemImage = itemView.findViewById(R.id.itemImage)
            itemTitle = itemView.findViewById(R.id.itemTitle)
            itemTemp = itemView.findViewById(R.id.itemTemp)

            itemView.setOnClickListener {
                val position: Int = adapterPosition
                Toast.makeText(itemView.context, "You have clicked on ${plants[position]}", Toast.LENGTH_SHORT).show()
                val intent = Intent(context, PlantActivity::class.java)
                intent.putExtra("id", plants[position])
                context.startActivity(intent)
            }
        }
    }
}