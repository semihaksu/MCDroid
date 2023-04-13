package com.semih.mcdroid

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONException
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class ArticleSync : AppCompatActivity() {
    private lateinit var mWebService: WebServiceRequest
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article_sync)

        var btn10 = findViewById<Button>(R.id.btn_group_10)
        var btn20 = findViewById<Button>(R.id.btn_group_20)
        var btn30 = findViewById<Button>(R.id.btn_group_30)
        var btn40 = findViewById<Button>(R.id.btn_group_40)
        var btn50 = findViewById<Button>(R.id.btn_group_50)
        var btn60 = findViewById<Button>(R.id.btn_group_60)
        var btn90 = findViewById<Button>(R.id.btn_group_90)
        var btn95 = findViewById<Button>(R.id.btn_group_95)
        var btn96 = findViewById<Button>(R.id.btn_group_96)

        btn10.setOnClickListener {
            syncGroupArticle(it, "btn10")
        }
        btn20.setOnClickListener {
            syncGroupArticle(it, "btn20")
        }
        btn30.setOnClickListener {
            syncGroupArticle(it, "btn30")
        }
        btn40.setOnClickListener {
            syncGroupArticle(it, "btn40")
        }
        btn50.setOnClickListener {
            syncGroupArticle(it, "btn50")
        }
        btn60.setOnClickListener {
            syncGroupArticle(it, "btn60")
        }
        btn90.setOnClickListener {
            syncGroupArticle(it, "btn90")
        }
        btn95.setOnClickListener {
            syncGroupArticle(it, "btn95")
        }
        btn96.setOnClickListener {
            syncGroupArticle(it, "btn96")
        }
    }

    fun syncGroupArticle(v: View, btnName: String){
        val masterTableName = "ARTIKEL"
        val tableName = "articles"

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        val logTextView = findViewById<TextView>(R.id.log_text_view)
        logTextView.append("### Article list import started...\n")

        fun syncContent(currentPage: Int, itemsPerPage: Int): String {
            var content = ""

            val dbGetHelper = dbHelper(this@ArticleSync)
            val helperURL = dbGetHelper.getSettings("helper_url")
            dbGetHelper.close()

            val url = URL("$helperURL/")
            runBlocking {
                delay(1000L)
                val response = async(Dispatchers.IO) {
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.doOutput = true

                    // Set the request parameters
                    val group = btnName.substring(btnName.length - 2)
                    val params = "page=$currentPage&items_per_page=$itemsPerPage&group=$group"
                    Log.e("helper", params)
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
                    content = stringBuilder.toString()

                    // Clean up resources
                    inputStream.close()
                    connection.disconnect()
                }
                response.await()
            }
            return content
        }

        // Set the initial page and items per page
        var currentPage: Int = 1;
        val itemsPerPage = 100;

        scope.launch(Dispatchers.Main) {
            while (true) {
                progressBar.visibility = View.VISIBLE
                val content = syncContent(currentPage, itemsPerPage)

                if (content.isNotEmpty()) {
                    try {
                        mWebService = WebServiceRequest(content)
                        val elements = mWebService.getElementsByTagName(masterTableName)

                        val newDbHelper = dbHelper(this@ArticleSync)
                        for (element in elements) {
                            val db = newDbHelper.readableDatabase

                            val idValue =
                                element.getElementsByTagName("ART_ID")
                                    .item(0).textContent.toInt()

                            val nameValue =
                                element.getElementsByTagName("ART_NAME").item(0).textContent

                            val wgrIdValue =
                                element.getElementsByTagName("WGR_ID")
                                    .item(0).textContent.toInt()

                            val vpkIdValue =
                                element.getElementsByTagName("VPK_ID")
                                    .item(0).textContent.toInt()

                            val artNumber =
                                element.getElementsByTagName("ART_NUMMER")
                                    .item(0).textContent.toInt()

                            val selection = "art_id = ?"

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
                            if (cursor.count == 0) {
                                // Insert the item into the database
                                val item: Any = Articles(
                                    0,
                                    idValue,
                                    artNumber,
                                    nameValue,
                                    vpkIdValue,
                                    wgrIdValue
                                )

                                val result = newDbHelper.insertItem(item)
                                if ( result < 0 ) {
                                    Log.e("OkHttp", "Error inserting $tableName")
                                } else {
                                    Handler(Looper.getMainLooper()).post {
                                        logTextView.append("+ $nameValue inserted \n")
                                    }
                                }
                            } else {
                                // Update the item value for the existing items
                                val result = newDbHelper.updateArticles(
                                    idValue,
                                    nameValue,
                                    wgrIdValue,
                                    vpkIdValue,
                                    artNumber
                                )

                                if (result < 0) {
                                    Log.e("OkHttp", "Error updating $tableName")
                                } else {
                                    Handler(Looper.getMainLooper()).post {
                                        logTextView.append("+ $nameValue updated \n")
                                    }
                                }
                            }
                            cursor.close()
                        }
                        newDbHelper.close()

                        // Increment the page number and loop again
                        currentPage++

                        logTextView.scrollTo(0, logTextView.bottom)
                        val container: ScrollView = findViewById(R.id.scrollContainer)
                        container.post { container.fullScroll(View.FOCUS_DOWN) }
                    } catch (e: JSONException) {
                        Log.e("JSON Parser", "Error parsing data $e")
                        break
                    }
                } else {
                    // No more data to import
                    logTextView.append("### Article list import finished.\n")
                    logTextView.scrollTo(0, logTextView.bottom)
                    progressBar.visibility = View.GONE
                    break
                }
            }
            logTextView.append("--------------------------------------------\n")
            logTextView.append("New articles inserted successfully \n")
            logTextView.append("--------------------------------------------\n")
            val container: ScrollView = findViewById(R.id.scrollContainer)
            container.post { container.fullScroll(View.FOCUS_DOWN) }
        }
    }
}