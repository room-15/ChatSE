package me.shreyasr.chatse.chat

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import me.shreyasr.chatse.R


class RoomSearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_search)
        val recyclerView = findViewById(R.id.room_recyclerview) as RecyclerView
        val layoutManager = GridLayoutManager(this@RoomSearchActivity, calculateNoOfColumns(applicationContext))

        val adapter = TaskAdapter(getDefaultRooms())
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

    }

    private fun getDefaultRooms(): MutableList<Room> {
        return mutableListOf(Room("Android", 15, null), Room("Sandbox", 1, null), Room("General", 3, null), Room("Python", 6, null), Room("C#", 7, null), Room("Lounge<C++>", 10, null), Room("php", 11, null), Room("JavaScript", 17, null))
    }

    fun calculateNoOfColumns(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density
        val noOfColumns = (dpWidth / 180).toInt()
        return noOfColumns
    }
}

class TaskAdapter(var tasks: MutableList<Room>) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): TaskViewHolder {
        val context = parent?.context
        val view = LayoutInflater.from(context)?.inflate(R.layout.room_item, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder?, position: Int) {
        holder?.bindTask(tasks[position])
    }

    override fun getItemCount(): Int {
        return tasks.size
    }

    inner class TaskViewHolder(view: View?) : RecyclerView.ViewHolder(view) {
        val name = view?.findViewById(R.id.name) as TextView
        val roomID = view?.findViewById(R.id.roomID) as TextView
        val lastActive = view?.findViewById(R.id.last_active) as TextView

        fun bindTask(task: Room) {
            name.text = task.name
            roomID.text = task.roomID.toString()
            if (task.lastActive != null) {
                lastActive.text = task.lastActive.toString()
            } else {
                lastActive.visibility = View.GONE
            }
        }
    }
}

data class Room(val name: String, val roomID: Long, val lastActive: Long?)