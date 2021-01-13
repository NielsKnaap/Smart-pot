package nl.smartpot

import android.icu.text.CaseMap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class RecyclerAdapter (plantList: MutableList<String>): RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {
    private var plants: MutableList<String> = plantList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.card_layout, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemTitle.text = plants[position]
    }

    override fun getItemCount(): Int {
        return plants.size
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var itemImage: ImageView
        var itemTitle: TextView

        init {
            itemImage = itemView.findViewById(R.id.itemImage)
            itemTitle = itemView.findViewById(R.id.itemTitle)

            itemView.setOnClickListener{
                val position: Int = adapterPosition
                Toast.makeText(itemView.context, "You have clicked on ${plants[position]}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}