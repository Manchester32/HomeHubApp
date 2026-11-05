package com.example.homehubapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.homehubapp.model.ServiceDto
import com.example.homehubapp.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ServicesActivity : AppCompatActivity() {

    private val servicesByName = mutableMapOf<String, ServiceDto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_services)

        findViewById<TextView>(R.id.tvBack).setOnClickListener { finish() }

        // Keys are the English names stored in the database; the button text itself is translated
        val buttons = mapOf(
            R.id.btnPlumbing to "Plumbing",
            R.id.btnElectrical to "Electrical",
            R.id.btnCleaning to "Cleaning",
            R.id.btnPainting to "Painting",
            R.id.btnPestControl to "Pest Control",
            R.id.btnApplianceRepair to "Appliance Repair",
            R.id.btnCarpentry to "Carpentry",
            R.id.btnGardening to "Garden & Landscaping",
            R.id.btnHandyman to "Handyman"
        )

        buttons.forEach { (id, name) ->
            findViewById<Button>(id).setOnClickListener {
                val service = servicesByName[name]
                if (service == null) {
                    Toast.makeText(this, getString(R.string.msg_services_loading), Toast.LENGTH_SHORT).show()
                } else {
                    startActivity(
                        Intent(this, BookServiceActivity::class.java)
                            .putExtra("serviceId", service.id)
                            .putExtra("service", findViewById<Button>(id).text.toString())
                    )
                }
            }
        }

        // Step 1: use the saved copy straight away so the buttons work even with no connection
        val cached = LocalDb.get(this).getServices()
        cached.forEach { servicesByName[it.name] = it }
        Log.d("HomeHub", "Loaded ${cached.size} services from the local cache")

        // Step 2: refresh from the API in the background
        loadServices(cached.isNotEmpty())
    }

    private fun loadServices(haveCache: Boolean) {
        ApiClient.service.getServices().enqueue(object : Callback<List<ServiceDto>> {
            override fun onResponse(call: Call<List<ServiceDto>>, response: Response<List<ServiceDto>>) {
                if (response.isSuccessful) {
                    val list = response.body().orEmpty()
                    list.forEach { servicesByName[it.name] = it }
                    // Save the fresh list locally so booking works offline next time
                    LocalDb.get(this@ServicesActivity).saveServices(list)
                    Log.d("HomeHub", "Loaded ${list.size} services from API and cached them")
                } else if (!haveCache) {
                    Toast.makeText(this@ServicesActivity, getString(R.string.msg_services_failed), Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<List<ServiceDto>>, t: Throwable) {
                Log.e("HomeHub", "Services API failed", t)
                if (haveCache) {
                    // Offline: the saved services are already on screen
                    Toast.makeText(this@ServicesActivity, getString(R.string.msg_services_offline), Toast.LENGTH_LONG).show()
                } else {
                    // Offline and nothing saved yet: the user must open Services once while online
                    Toast.makeText(this@ServicesActivity, getString(R.string.msg_server_down), Toast.LENGTH_LONG).show()
                }
            }
        })
    }
}