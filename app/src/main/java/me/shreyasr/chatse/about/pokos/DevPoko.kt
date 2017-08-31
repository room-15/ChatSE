package me.shreyasr.chatse.about.pokos

/**
 * Created by mauker on 31/08/17.
 * Class that represents a dev card.
 */
data class DevPoko(
        val name: String,
        val job: String,
        val aboutMe: String,
        val imageRes: Int,
        val icons : ArrayList<AboutIconPoko>
)