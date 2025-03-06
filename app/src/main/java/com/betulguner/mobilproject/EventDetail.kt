package com.betulguner.mobilproject

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlin.collections.Map

class EventDetail : AppCompatActivity() {

    private val eventList = mutableListOf<Event>()
    private lateinit var event: Event
    private lateinit var currentEvent: Event
    private lateinit var joinButton: Button
    private lateinit var eventId: String
    companion object {
        val likedEvents = mutableListOf<Event>()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_event_detail)

        //val event = intent.getSerializableExtra("event") as Event
        event = intent.getSerializableExtra("event") as Event

        val eventNameTextView: TextView = findViewById(R.id.eventName)
        val eventDateTextView: TextView = findViewById(R.id.eventDate)
        val eventLocationTextView: TextView = findViewById(R.id.eventLocation)
        val eventDescriptionTextView: TextView = findViewById(R.id.eventDescription)
        val eventPriceTextView: TextView = findViewById(R.id.eventPrice)
        val eventOrganizerTextView: TextView = findViewById(R.id.eventOrganizer)
        val eventImage: ImageView = findViewById(R.id.eventImage)
        val likeButton: Button = findViewById(R.id.likeBtn)

        joinButton = findViewById(R.id.joinBtn)
        eventId = intent.getStringExtra("EVENT_ID") ?: ""

        joinButton.setOnClickListener {
            joinEvent(eventId)
        }

        // Event nesnesini al
        //val event = intent.getParcelableExtra<Event>("event")

        if (event == null) {
            Toast.makeText(this, "Event data is missing!", Toast.LENGTH_SHORT).show()
            finish() // Veriler eksikse etkinlik detay sayfasını kapat
            return
        }

        currentEvent = event // Gelen etkinliği currentEvent değişkenine ata

        eventNameTextView.text = event.name
        eventDateTextView.text = "Date: ${event.date}"
        eventLocationTextView.text = "Location: ${event.location}"
        eventDescriptionTextView.text = "Description: ${event.description}"
        eventPriceTextView.text = "Price: ${event.price}"
        eventOrganizerTextView.text = "Organizer: ${event.organizer}"

        Picasso.get()
            .load(event.imageUrl) // Resmin URL'sini buraya ekliyoruz
            .placeholder(R.drawable.ic_launcher_foreground) // Resim yüklenene kadar gösterilecek resim
            .error(R.drawable.baseline_error_24) // Resim yüklenemezse gösterilecek hata resmi
            .into(eventImage) // Resmin yükleneceği ImageView

        //Log.d("Event Image URL", "Image URL: ${event.imageUrl}")

        // Butonlar için Click Listener ekle
        val remember = findViewById<Button>(R.id.rememberBtn)
        remember.setOnClickListener { rememberOnClick() }

        val like = findViewById<Button>(R.id.likeBtn)
        like.setOnClickListener { likeOnClick() }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navHome

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navHome -> {
                    startActivity(Intent(this, MainPage::class.java))
                    overridePendingTransition(0, 0)
                    true
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
                    startActivity(Intent(this, FavouriteEvents::class.java))
                    overridePendingTransition(0,0)
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

    private fun joinEvent(eventId: String) {
        // Katılım bilgisini kaydedin (örneğin, SharedPreferences veya bir veritabanı kullanarak)
        val sharedPreferences = getSharedPreferences("EventPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Mevcut etkinlikleri al ve yeni etkinliği ekle
        val joinedEvents = sharedPreferences.getStringSet("joinedEvents", mutableSetOf()) ?: mutableSetOf()
        joinedEvents.add(eventId)

        editor.putStringSet("joinedEvents", joinedEvents)
        editor.apply()

        Toast.makeText(this, "Etkinliğe Katıldınız!", Toast.LENGTH_SHORT).show()
    }

    fun rememberOnClick() {
        // Hatırla butonu için işlevsellik buraya yazılacak
    }

    fun likeOnClick() {
        currentEvent?.let { event ->
            if (likedEvents.contains(event)) {
                likedEvents.remove(event) // Etkinliği listeden çıkar
                Toast.makeText(this, "Event removed from liked list!", Toast.LENGTH_SHORT).show()
            } else {
                likedEvents.add(event) // Etkinliği listeye ekle
                Toast.makeText(this, "Event added to liked list!", Toast.LENGTH_SHORT).show()
            }
            // SharedPreferences'e kaydet
            saveLikedEvents()
            updateLikeButtonUI()
        } ?: run {
            Toast.makeText(this, "No event to like/unlike!", Toast.LENGTH_SHORT).show()
        }
    }


    fun saveLikedEvents() {
        val sharedPreferences = getSharedPreferences("liked_events", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Etkinliklerin isimlerini alıyoruz
        val likedEventNames = likedEvents.map { it.name }
        editor.putStringSet("liked_event_names", likedEventNames.toSet()) // Etkinlik isimlerini SharedPreferences'a kaydediyoruz
        editor.apply()
    }

    fun updateLikeButtonUI() {
        val likeButton = findViewById<Button>(R.id.likeBtn)
        if (likedEvents.contains(currentEvent)) {
            likeButton.text = "Unlike" // Buton yazısını değiştir
            likeButton.setBackgroundColor(Color.GRAY)
        } else {
            likeButton.text = "Like"
            // Arka planı kırmızı yapmak için
            likeButton.setBackgroundColor(Color.RED)
        }
    }


}