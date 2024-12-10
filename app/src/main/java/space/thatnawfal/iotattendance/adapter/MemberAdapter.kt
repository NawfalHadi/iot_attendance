package space.thatnawfal.iotattendance.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.icu.util.Calendar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import space.thatnawfal.iotattendance.R
import space.thatnawfal.iotattendance.data.User
import java.text.SimpleDateFormat

class MemberAdapter(private val data: MutableList<User>, private val listener: OnItemClickListener) : RecyclerView.Adapter<MemberAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_member, parent, false)
        return ViewHolder(view, listener)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.itemView.findViewById<TextView>(R.id.item_name).text = item.name
        holder.itemView.findViewById<TextView>(R.id.item_phone).text = item.phone_number

        val today = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("dd/MM/yyyy")
        val expiredDate = dateFormat.parse(item.expired_at.toString())

        if (expiredDate?.before(today) == true){
            holder.itemView.findViewById<TextView>(R.id.item_expired).setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.red))
        } else {
            holder.itemView.findViewById<TextView>(R.id.item_expired).setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.green))
        }

        holder.itemView.findViewById<TextView>(R.id.item_expired).text = "Expired At : ${item.expired_at.toString()}"
    }

    class ViewHolder(itemView: View, listener: OnItemClickListener) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.item_name)
        val phone: TextView = itemView.findViewById(R.id.item_phone)
        val expired: TextView = itemView.findViewById(R.id.item_expired)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(position)
                }
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(pos: Int)
    }

}

