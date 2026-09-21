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
                    Toast.makeText(this, "Services are still loading. Please try again.", Toast.LENGTH_SHORT).show()
                } else {
                    startActivity(
                        Intent(this, BookServiceActivity::class.java)
                            .putExtra("serviceId", service.id)
                            .putExtra("service", service.name)
                    )
                }
            }
        }

        loadServices()
    }

    private fun loadServices() {
        ApiClient.service.getServices().enqueue(object : Callback<List<ServiceDto>> {
            override fun onResponse(call: Call<List<ServiceDto>>, response: Response<List<ServiceDto>>) {
                if (response.isSuccessful) {
                    response.body().orEmpty().forEach { servicesByName[it.name] = it }
                    Log.d("HomeHub", "Loaded ${servicesByName.size} services from API")
                } else {
                    Toast.makeText(this@ServicesActivity, "Could not load services", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<List<ServiceDto>>, t: Throwable) {
                Log.e("HomeHub", "Services API failed", t)
                Toast.makeText(this@ServicesActivity, "Could not connect to the HomeHub server", Toast.LENGTH_LONG).show()
            }
        })
    }
}
