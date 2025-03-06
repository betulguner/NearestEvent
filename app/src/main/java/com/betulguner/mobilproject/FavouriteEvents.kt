package com.betulguner.mobilproject

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.collections.Map

class FavouriteEvents : AppCompatActivity() {

    private val eventList = mutableListOf<Event>()
    //private lateinit var favoriteEvents: MutableList<Event>

    private lateinit var recyclerView: RecyclerView
    private lateinit var eventAdapter: EventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_favourite_events)

        val intent = intent

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Beğenilen etkinlikleri al ve RecyclerView ile göster
        eventAdapter = EventAdapter(EventDetail.likedEvents) { event ->
            // Beğenilen etkinlik tıklanırsa, detay sayfasına git
            val intent = Intent(this, EventDetail::class.java)
            intent.putExtra("event", event)
            startActivity(intent)
        }
        recyclerView.adapter = eventAdapter

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navHome

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navHome -> {
                    startActivity(Intent(this, MainPage::class.java))
                    overridePendingTransition(0,0)
                    true // Zaten MainPage'desin, bir işlem yapmana gerek yok
                }
                R.id.navMap -> {
                    val intent = Intent(this, Map::class.java)
                    intent.putExtra("events", ArrayList(eventList)) // eventList'i aktar
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navProfile -> {
                    val intent = Intent(this, ProfileSettings::class.java)
                    intent.putExtra("events", ArrayList(eventList)) // eventList'i aktar
                    startActivity(intent)
                    overridePendingTransition(0, 0) // Geçiş animasyonunu kaldır
                    true
                }
                R.id.navFavourite -> {
                    true
                }
                else -> false
            }
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        navView.selectedItemId = R.id.navFavourite // Ana sayfanın simgesini seç
    }
}