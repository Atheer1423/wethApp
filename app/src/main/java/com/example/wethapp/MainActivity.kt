package com.example.wethapp

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.lang.Exception
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var CZip = "10001"
    private val API = "8a316bae40ca552c86771c6d73150592"
    var tempMin=""
    var tempMax=""
    var temp=""
    var minTemperature=""
    var maxTemperature=""
    var currentTemperature=""
    private lateinit var bE: Button
    private lateinit var tvf: TextView

    private lateinit var rlZip: RelativeLayout
    private lateinit var Rcolor: RelativeLayout
    private lateinit var etZip: EditText
    private lateinit var etname: EditText
    private lateinit var btZip: Button
    var C=true
    var data=""
    var Cname="us"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Rcolor=findViewById(R.id.Rcolor)



        bE = findViewById(R.id.btError)
        tvf = findViewById(R.id.f)
        bE.setOnClickListener {
            CZip = "10001"
            requestAPI()
        }
        tvf.setOnClickListener {
          if (C==true) {
              var newTM =
                  (minTemperature.substring(0, minTemperature.indexOf(".")).toInt() * 9 / 5) + 32
              findViewById<TextView>(R.id.tvTempMin).text = "Low: " + newTM + "F"
              var newTMa =
                  (maxTemperature.substring(0, maxTemperature.indexOf(".")).toInt() * 9 / 5) + 32
              findViewById<TextView>(R.id.tvTempMax).text = "High: " + newTMa + "F"
              var CT = try {
                  currentTemperature.substring(0, currentTemperature.indexOf("."))
              } catch (e: Exception) {
                  currentTemperature
              }
              var newT = (CT.toInt() * 9 / 5) + 32
              findViewById<TextView>(R.id.tvTemperature).text = "$newT F"

              tvf.text = "to C"
              C=false
          }else{
              tvf.text = "to C"
              C=true
              CoroutineScope(IO).launch {
                  updateData(data)
              }
          }


        }

        rlZip = findViewById(R.id.rlZip)
        etZip = findViewById(R.id.etZip)
        etname = findViewById(R.id.etname)
        btZip = findViewById(R.id.btZip)
        btZip.setOnClickListener {
            CZip = etZip.text.toString();
            Cname = etname.text.toString();
            requestAPI()
            etZip.text.clear()
            // Hide Keyboard
         val imm = ContextCompat.getSystemService(this, InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(this.currentFocus?.windowToken, 0)
           rlZip.isVisible = false
        }

        requestAPI()
    }

    private fun requestAPI(){
//        println("CITY: $CZip")
        CoroutineScope(IO).launch {
            updateStatus(-1)
            data = async {
                fetchData()
            }.await()
            if(data.isNotEmpty()){
                updateData(data)
                updateStatus(0)
            }else{
                updateStatus(1)
            }
        }
    }

    private suspend fun updateData(result: String){
        withContext(Main){
            val jsonObj = JSONObject(result)
            val main = jsonObj.getJSONObject("main")
            val sys = jsonObj.getJSONObject("sys")
            val wind = jsonObj.getJSONObject("wind")
            val weather = jsonObj.getJSONArray("weather").getJSONObject(0)

            val lastUpdate:Long = jsonObj.getLong("dt")
            val lastUpdateText = "Updated at: " + SimpleDateFormat(
                "dd/MM/yyyy hh:mm a",
                Locale.ENGLISH).format(Date(lastUpdate*1000))
             currentTemperature = main.getString("temp")
             temp = try{
                currentTemperature.substring(0, currentTemperature.indexOf(".")) + "°C"
            }catch(e: Exception){
                currentTemperature + "°C"
            }
              minTemperature = main.getString("temp_min")
             tempMin = "Low: " + minTemperature.substring(0, minTemperature.indexOf("."))+"°C"
             maxTemperature = main.getString("temp_max")
             tempMax = "High: " + maxTemperature.substring(0, maxTemperature.indexOf("."))+"°C"
            val pressure = main.getString("pressure")
            val humidity = main.getString("humidity")

            val sunrise:Long = sys.getLong("sunrise")
            val sunset:Long = sys.getLong("sunset")
            val windSpeed = wind.getString("speed")
            val weatherDescription = weather.getString("description")

            val address = jsonObj.getString("name")+", "+sys.getString("country")

            findViewById<TextView>(R.id.tvAddress).text = address
            findViewById<TextView>(R.id.tvAddress).setOnClickListener {
                rlZip.isVisible = true
            }
            findViewById<TextView>(R.id.tvLastUpdated).text =  lastUpdateText
            findViewById<TextView>(R.id.tvStatus).text = weatherDescription.capitalize(Locale.getDefault())
            findViewById<TextView>(R.id.tvTemperature).text = temp
            findViewById<TextView>(R.id.tvTempMin).text = tempMin
            findViewById<TextView>(R.id.tvTempMax).text = tempMax
            findViewById<TextView>(R.id.tvSunrise).text = SimpleDateFormat("hh:mm a",
                Locale.ENGLISH).format(Date(sunrise*1000))
            findViewById<TextView>(R.id.tvSunset).text = SimpleDateFormat("hh:mm a",
                Locale.ENGLISH).format(Date(sunset*1000))
            findViewById<TextView>(R.id.tvWind).text = windSpeed
            findViewById<TextView>(R.id.tvPressure).text = pressure
            findViewById<TextView>(R.id.tvHumidity).text = humidity
            findViewById<LinearLayout>(R.id.llRefresh).setOnClickListener { requestAPI() }
        }
    }

    private fun fetchData(): String{
        var response = ""
        try {
            response = URL("https://api.openweathermap.org/data/2.5/weather?zip=$CZip,$Cname&units=metric&appid=$API")
                .readText(Charsets.UTF_8)
        }catch (e: Exception){
            println("Error: $e")
        }
        return response
    }

    private suspend fun updateStatus(state: Int){
//        states: -1 = loading, 0 = loaded, 1 = error
        withContext(Main){
            when{
                state < 0 -> {
                    findViewById<ProgressBar>(R.id.pbProgress).visibility = View.VISIBLE
                    findViewById<RelativeLayout>(R.id.rlMain).visibility = View.GONE
                    findViewById<LinearLayout>(R.id.llErrorContainer).visibility = View.GONE
                }
                state == 0 -> {
                    findViewById<ProgressBar>(R.id.pbProgress).visibility = View.GONE
                    findViewById<RelativeLayout>(R.id.rlMain).visibility = View.VISIBLE
                }
                state > 0 -> {
                    findViewById<ProgressBar>(R.id.pbProgress).visibility = View.GONE
                    findViewById<LinearLayout>(R.id.llErrorContainer).visibility = View.VISIBLE
                }
            }
        }
    }
}