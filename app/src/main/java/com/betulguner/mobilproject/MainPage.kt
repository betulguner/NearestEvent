package com.betulguner.mobilproject

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import android.os.Parcelable
import android.view.MenuItem
import android.widget.Filter
import android.widget.Filterable
import androidx.appcompat.widget.SearchView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.Serializable


class MainPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var recyclerView: RecyclerView
    private lateinit var eventAdapter: EventAdapter
    private val eventList = mutableListOf<Event>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_page)

        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navHome

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navHome -> {
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
                    startActivity(Intent(this, FavouriteEvents::class.java))
                    overridePendingTransition(0,0)
                    true
                }
                else -> false
            }
        }

        val searchView = findViewById<SearchView>(R.id.searchView)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        eventAdapter = EventAdapter(eventList) { event ->
            val intent = Intent(this, EventDetail::class.java)
            intent.putExtra("event", event)
            startActivity(intent)
        }
        recyclerView.adapter = eventAdapter

        checkLocationPermission()

        // SearchView ile filtreleme
        val queryTextListener = object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                eventAdapter.filter.filter(newText)
                return true
            }
        }

        searchView.setOnQueryTextListener(queryTextListener)



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getLastLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(
                    this,
                    "Location permission is needed for this feature",
                    Toast.LENGTH_SHORT
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) {
        isGranted: Boolean ->
        if (isGranted) {
            getLastLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        fetchEvents(it.latitude, it.longitude)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun fetchEvents(latitude: Double, longitude: Double) {
        val apiKey = "kyDmZWZ1AplaPdNb2bufAOqe38ePZDkq"
        val url = "https://app.ticketmaster.com/discovery/v2/events.json?apikey=$apiKey"

        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainPage, "Failed to fetch events", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body()?.use { responseBody ->
                        val json = JSONObject(responseBody.string())
                        val events = json.getJSONObject("_embedded").getJSONArray("events")
                        eventList.clear()

                        for (i in 0 until events.length()) {
                            val eventJson = events.getJSONObject(i)
                            val id = eventJson.optString("id", "defaultId") // eventJson'dan id alın, yoksa "defaultId" kullan
                            val name = eventJson.getString("name")
                            val date = eventJson.getJSONObject("dates").getJSONObject("start").getString("localDate")
                            val venue = eventJson.getJSONObject("_embedded").getJSONArray("venues").getJSONObject(0)
                            val location = venue.getString("name")
                            val description = eventJson.optString("info", "No description available")  // info ya da boş döndür
                            val category = eventJson.getJSONArray("classifications").getJSONObject(0).getJSONObject("segment").getString("name")

                            val images = eventJson.getJSONArray("images")
                            val imageUrl = images.getJSONObject(0).getString("url")

                            val latitude = venue.getJSONObject("location").getDouble("latitude")
                            val longitude = venue.getJSONObject("location").getDouble("longitude")

                            val organizer = if (eventJson.has("promoters")) {
                                val promoters = eventJson.getJSONArray("promoters")
                                promoters.getJSONObject(0).getString("name")
                            } else {
                                "Unknown Organizer" }

                            val price = if (eventJson.has("priceRanges")) {
                                val priceRanges = eventJson.getJSONArray("priceRanges").getJSONObject(0)
                                "${priceRanges.getDouble("min")} - ${priceRanges.getDouble("max")} ${priceRanges.getString("currency")}"
                            } else {
                                "Price Not Available" }



                            val event = Event(
                                id = eventJson.optString("id", "defaultId"),
                                name = name,
                                date = date,
                                latitude = latitude,
                                longitude =longitude,
                                description = description,
                                location = location,
                                category = category,
                                organizer = organizer,
                                price = price,
                                imageUrl = imageUrl
                            )
                            eventList.add(event)
                        }
                        runOnUiThread {
                            eventAdapter.notifyDataSetChanged()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@MainPage, "API Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Uygulamayı kapat
        finishAffinity()
    }

    override fun onResume() {
        super.onResume()
        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        navView.selectedItemId = R.id.navHome // Ana sayfanın simgesini seç
    }
}


data class Event(val id: String,val name: String, val date: String, val latitude: Double, val longitude: Double, val description: String, val location: String,val category: String, val price: String, val organizer: String, val imageUrl: String  ):
    Serializable

class EventAdapter(
    private var eventList: List<Event>,
    private val onEventClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>(),Filterable {


    private var eventListFiltered: List<Event> = eventList // Filtered list

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val eventName: TextView = itemView.findViewById(R.id.eventNameTextView)
        val eventDate: TextView = itemView.findViewById(R.id.eventDateTextView)
        val eventLocation: TextView = itemView.findViewById(R.id.eventLocationTextView)
        val eventDescription: TextView = itemView.findViewById(R.id.eventDescTextView)
        val eventCategory: TextView = itemView.findViewById(R.id.eventCategoryTextView)

        fun bind(event: Event) {
            eventName.text = event.name
            eventDate.text = event.date
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = eventList[position]
        holder.eventName.text = event.name
        holder.eventDate.text = event.date
        holder.eventLocation.text = event.location
        holder.eventDescription.text = event.description
        holder.eventCategory.text = event.category

        holder.itemView.setOnClickListener {
            onEventClick(event)
        }
        holder.bind(event)
    }

    override fun getItemCount() = eventList.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                val query = constraint?.toString()?.lowercase()?.trim()

                filterResults.values = if (query.isNullOrEmpty()) {
                    eventList // No filter applied
                } else {
                    eventList.filter {
                        it.name.lowercase().contains(query) ||
                                it.description.lowercase().contains(query) ||
                                it.location.lowercase().contains(query)
                    }
                }
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                eventListFiltered = results?.values as List<Event>
                notifyDataSetChanged()
            }

        }
    }

}