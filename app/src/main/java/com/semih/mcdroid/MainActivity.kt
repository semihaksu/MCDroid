package com.semih.mcdroid

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONException
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {
    private lateinit var mWebService: WebServiceRequest
    private val scope = CoroutineScope(Dispatchers.Main)

    companion object {
        const val USER_PIN = "3020"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    fun loginButtonClicked(v: View) {
        val userName = findViewById<EditText>(R.id.userName)
        val userPass = findViewById<EditText>(R.id.userPass)

        val mcUser = userName.text.toString()
        val mcPass = userPass.text.toString()

        // Query the users table for a matching user record
        val dbHelper = dbHelper(this@MainActivity)
        val cursor = dbHelper.getUserByCredentials(mcUser.uppercase(), mcPass)

        if(cursor != null && cursor.moveToFirst()) {
            // User record found, extract the values
            val userId = cursor.getInt(cursor.getColumnIndexOrThrow("mc_id"))
            val userName = cursor.getString(cursor.getColumnIndexOrThrow("mc_user"))

            // Do something with the user record, such as display a welcome message or start a new activity
            val loggedInScreen = Intent(this@MainActivity, AfterLoginActivity::class.java)
            loggedInScreen.putExtra("userId", userId)
            loggedInScreen.putExtra("userName", userName)
            startActivity(loggedInScreen)
        } else {
            // No user record found, display an error message or take other appropriate action
            Toast.makeText(this@MainActivity, getString(R.string.user_not_found), Toast.LENGTH_LONG).show()
        }
    }

    fun syncButtonClicked(view: View) {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        val client = OkHttpClient()
        val dbHelper = dbHelper(this@MainActivity)
        val serviceURL = dbHelper.getSettings("service_url")
        dbHelper.close()

        val url = URL("$serviceURL$USER_ENDPOINT")
        scope.launch(Dispatchers.IO) {
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true

            // Set the request parameters
            val params = "HHT_ID=$HHT_ID"
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

            // Process the response on the UI thread
            withContext(Dispatchers.Main) {
                if (content.isNotEmpty()) {
                    try {
                        val dbHelper = dbHelper(this@MainActivity)
                        mWebService = WebServiceRequest(content)
                        val elements = mWebService.getElementsByTagName("USERS")
                        for (element in elements) {
                            val userId: Int =
                                element.getElementsByTagName("BST_ID")
                                    .item(0).textContent.toInt()
                            val userName =
                                element.getElementsByTagName("BST_NAME").item(0).textContent
                            val userPass =
                                element.getElementsByTagName("BST_PASSWD").item(0).textContent

                            // Check if the group already exists in the database
                            val db = dbHelper.readableDatabase
                            val projection = arrayOf("id")
                            val selection = "mc_id = ?"
                            val selectionArgs = arrayOf("$userId")
                            val cursor =
                                db.query(
                                    "users",
                                    projection,
                                    selection,
                                    selectionArgs,
                                    null,
                                    null,
                                    null
                                )
                            if (cursor.count > 0) {
                                // Update the unit value for the existing units
                                val result = dbHelper.updateUserPass(
                                    userId,
                                    "3020",
                                    userPass
                                )
                                if (result >= 0) {
                                    runOnUiThread {
                                        val updateToast = Toast.makeText(
                                            this@MainActivity,
                                            "Users updated successfully",
                                            Toast.LENGTH_SHORT
                                        )
                                        updateToast.show()
                                        Handler().postDelayed({
                                            updateToast.cancel()
                                        }, 200)
                                    }
                                } else {
                                    Log.e("OkHttp", "Error updating groups")
                                }
                            } else {
                                // Insert the user into the database
                                val user =
                                    User(
                                        0,
                                        userId,
                                        userName,
                                        "3020",
                                        userPass
                                    )
                                val result = dbHelper.insertUser(user)
                                if (result >= 0) {
                                    runOnUiThread {
                                        val insToast = Toast.makeText(
                                            this@MainActivity,
                                            "New users inserted successfully",
                                            Toast.LENGTH_SHORT
                                        )
                                        insToast.show()
                                        Handler().postDelayed({
                                            insToast.cancel()
                                        }, 200)
                                    }
                                }
                            }
                            cursor.close()
                        }
                    } catch (e: JSONException) {
                        Log.e("OkHttp", "Error parsing JSON: " + e.message)
                    }
                }
                progressBar.visibility = View.GONE
            }
        }
    }

    fun configButtonClicked(v: View) {
        val configScreen = Intent(this@MainActivity, AppConfigActivity::class.java)
        startActivity(configScreen)
    }
}