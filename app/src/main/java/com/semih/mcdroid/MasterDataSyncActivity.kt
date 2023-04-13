package com.semih.mcdroid

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils.isEmpty
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONException
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class MasterDataSyncActivity : AppCompatActivity() {
    private lateinit var mWebService: WebServiceRequest
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_master_sync)

        val unitsButton = findViewById<Button>(R.id.units_btn)
        unitsButton.setOnClickListener {
            unitsSyncClicked(it)
        }

        val groupsButton = findViewById<Button>(R.id.groups_btn)
        groupsButton.setOnClickListener {
            groupsSyncClicked(it)
        }

        val articlesButton = findViewById<Button>(R.id.articles_btn)
        articlesButton.setOnClickListener {
            //articleSyncClicked(it)
            val articleSyncScreen = Intent(this@MasterDataSyncActivity, ArticleSync::class.java)
            startActivity(articleSyncScreen)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun unitsSyncClicked(v: View) {
        syncMasterData("VPCKEINH", "units")
    }

    private fun groupsSyncClicked(v: View) {
        syncMasterData("WARENGRUPPE", "groups")
    }

    private fun syncMasterData(masterTableName: String, tableName: String) {
        // Show loading bar
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        val logTextView = findViewById<TextView>(R.id.log_text_view)
        val container: ScrollView = findViewById(R.id.scrollContainer)

        if( tableName == "units" ){
            logTextView.append("### Units import started...\n")
        } else {
            logTextView.append("### Groups import started...\n")
        }

        val dbGetHelper = dbHelper(this@MasterDataSyncActivity)
        val serviceURL = dbGetHelper.getSettings("service_url")
        dbGetHelper.close()

        val url = URL("$serviceURL$DATA_ENDPOINT")
        scope.launch(Dispatchers.IO) {
            Log.e("Custom", "$serviceURL$DATA_ENDPOINT")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true

            // Set the request parameters
            val params = "HHT_ID=$HHT_ID&masterTableName=$masterTableName&differential=true"
            val bytes = params.toByteArray(StandardCharsets.UTF_8)
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setRequestProperty("Content-Length", bytes.size.toString())
            connection.outputStream.write(bytes)

            // Read the response
            val inputStream: InputStream = connection.inputStream
            val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
            val stringBuilder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }
            val content = stringBuilder.toString()
            // Clean up resources
            inputStream.close()
            connection.disconnect()

            delay(1000L)

            // Process the response on the UI thread
            withContext(Dispatchers.Main) {
                if (content.isNotEmpty()) {
                    try {
                        mWebService = WebServiceRequest(content)
                        val elements = mWebService.getElementsByTagName(masterTableName)

                        val newDbHelper = dbHelper(this@MasterDataSyncActivity)
                        for (element in elements) {
                            val db = newDbHelper.readableDatabase

                            var idValue = 0
                            var nameValue = ""
                            var grmengeValue = 0.00
                            var grundValue = 0
                            var wgrIdValue = 0
                            var vpkIdValue = 0
                            var artNumber = 0
                            var selection = ""

                            when(tableName){
                                "units" -> {
                                    idValue =
                                        element.getElementsByTagName("VPK_ID")
                                            .item(0).textContent.toInt()
                                    nameValue =
                                        element.getElementsByTagName("VPK_NAME").item(0).textContent
                                    grmengeValue =
                                        element.getElementsByTagName("VPK_GRMENGE")
                                            .item(0).textContent.toDouble()
                                    grundValue = element.getElementsByTagName("VPK_GRUND")
                                        .item(0).textContent.toInt()

                                    selection = "unit_id = ?"
                                }
                                "groups" -> {
                                    idValue =
                                        element.getElementsByTagName("WGR_ID")
                                            .item(0).textContent.toInt()
                                    nameValue =
                                        element.getElementsByTagName("WGR_NAME").item(0).textContent

                                    selection = "group_id = ?"
                                }
                                else -> throw IllegalArgumentException("Unexpected table name: $tableName")
                            }

                            // Check if the item already exists in the database
                            val projection = arrayOf("id")
                            val selectionArgs = arrayOf("$idValue")

                            val cursor =
                                db.query(
                                    tableName,
                                    projection,
                                    selection,
                                    selectionArgs,
                                    null,
                                    null,
                                    null
                                )
                            if (cursor.count > 0) {
                                // Update the item value for the existing items
                                val result = when(tableName) {
                                    "units" -> {
                                        newDbHelper.updateUnits(idValue, nameValue, grmengeValue, grundValue)
                                    }
                                    "groups" -> {
                                        newDbHelper.updateGroups(idValue, nameValue)
                                    }
                                    else -> throw IllegalArgumentException("Unexpected table name: $tableName")
                                }

                                if (result < 0) {
                                    Log.e("OkHttp", "Error updating $tableName")
                                } else {
                                    logTextView.append("+ $nameValue updated. \n")
                                }
                            } else {
                                // Insert the item into the database
                                val item: Any = when(tableName){
                                    "units" -> {
                                        Units(
                                            0,
                                            idValue,
                                            nameValue,
                                            grmengeValue,
                                            grundValue
                                        )
                                    }
                                    "groups" -> {
                                        Groups(
                                            0,
                                            idValue,
                                            nameValue
                                        )
                                    }
                                    else -> throw IllegalArgumentException("Unexpected table name: $tableName")
                                }

                                val result = newDbHelper.insertItem(item)
                                if( result >= 0 ){
                                    logTextView.append("$nameValue inserted. \n")
                                }
                            }
                            cursor.close()
                            db.close()
                        }
                    } catch (e: JSONException) {
                        Log.e("OkHttp", "Error parsing JSON: " + e.message)
                    }
                }
                progressBar.visibility = View.GONE
                logTextView.append("--------------------------------------------\n")
                logTextView.append("New ${tableName.uppercase()} inserted successfully \n")
                logTextView.append("--------------------------------------------\n")
                container.post { container.fullScroll(View.FOCUS_DOWN) }
            }
        }
    }
}