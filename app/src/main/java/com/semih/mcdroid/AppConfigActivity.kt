package com.semih.mcdroid

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AppConfigActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_config)

        val dbHelper = dbHelper(this@AppConfigActivity)
        val currentUrl = dbHelper.getSettings("service_url")
        if( currentUrl != null ){
            val www_area = findViewById<EditText>(R.id.serviceUrl)
            www_area.setText(currentUrl)
        }

        val helperUrl = dbHelper.getSettings("helper_url")
        if( helperUrl != null ) {
            val helper_area = findViewById<EditText>(R.id.helperUrl)
            helper_area.setText(helperUrl)
        }

        val saveConfigButton = findViewById<Button>(R.id.saveConfigButton)
        saveConfigButton.setOnClickListener {
            saveConfigClicked(it)
        }
    }

    fun saveConfigClicked(v: View) {
        val serviceUrl = findViewById<EditText>(R.id.serviceUrl)
        val serviceUrlText = serviceUrl.text.toString()

        val helperUrl = findViewById<EditText>(R.id.helperUrl)
        val helperUrlText = helperUrl.text.toString()

        val serviceName: String = "service_url"
        val helperName: String = "helper_url"

        val dbHelper = dbHelper(this@AppConfigActivity)
        val db = dbHelper.readableDatabase
        val query = "SELECT * FROM settings WHERE name = ?"
        val cursor = db.rawQuery(query, arrayOf(serviceName))

        if(cursor != null && cursor.moveToFirst()) {
            // Config found, update values
            val configId = cursor.getInt(cursor.getColumnIndexOrThrow("id"))

            val values = ContentValues().apply {
                put("name", "service_url")
                put("value", serviceUrlText)
            }
            val selection = "id = ?"
            val selectionArgs = arrayOf("$configId")
            val result = db.update("settings", values, selection, selectionArgs).toLong()
            if( result > 0 ){
                runOnUiThread {
                    val updToast = Toast.makeText(this@AppConfigActivity, getString(R.string.config_updated), Toast.LENGTH_SHORT)
                    updToast.show()
                    Handler().postDelayed({
                        updToast.cancel()
                    }, 200)
                }
            } else {
                runOnUiThread {
                    val updToast = Toast.makeText(this@AppConfigActivity, getString(R.string.config_error), Toast.LENGTH_SHORT)
                    updToast.show()
                    Handler().postDelayed({
                        updToast.cancel()
                    }, 200)
                }
            }
        } else {
            // No config records found
            val values = ContentValues().apply {
                put("name", "service_url")
                put("value", serviceUrlText)
            }
            val insConfig = db.insert("settings", null, values).toLong()
            if( insConfig > 0 ){
                runOnUiThread {
                    val updToast = Toast.makeText(this@AppConfigActivity, getString(R.string.config_inserted), Toast.LENGTH_SHORT)
                    updToast.show()
                    Handler().postDelayed({
                        updToast.cancel()
                    }, 200)
                }
            } else {
                runOnUiThread {
                    val updToast = Toast.makeText(this@AppConfigActivity, getString(R.string.config_error), Toast.LENGTH_SHORT)
                    updToast.show()
                    Handler().postDelayed({
                        updToast.cancel()
                    }, 200)
                }
            }
        }

        val query2 = "SELECT * FROM settings WHERE name = ?"
        val cursor2 = db.rawQuery(query2, arrayOf(helperName))

        if(cursor2 != null && cursor2.moveToFirst()) {
            // Config found, update values
            val configId = cursor2.getInt(cursor2.getColumnIndexOrThrow("id"))

            val values = ContentValues().apply {
                put("name", helperName)
                put("value", helperUrlText)
            }
            val selection = "id = ?"
            val selectionArgs = arrayOf("$configId")
            val result = db.update("settings", values, selection, selectionArgs).toLong()
            if( result > 0 ){
                runOnUiThread {
                    val updToast = Toast.makeText(this@AppConfigActivity, getString(R.string.config_updated), Toast.LENGTH_SHORT)
                    updToast.show()
                    Handler().postDelayed({
                        updToast.cancel()
                    }, 200)
                }
            } else {
                runOnUiThread {
                    val updToast = Toast.makeText(this@AppConfigActivity, getString(R.string.config_error), Toast.LENGTH_SHORT)
                    updToast.show()
                    Handler().postDelayed({
                        updToast.cancel()
                    }, 200)
                }
            }
        } else {
            // No config records found
            val values = ContentValues().apply {
                put("name", helperName)
                put("value", helperUrlText)
            }
            val insConfig = db.insert("settings", null, values).toLong()
            if( insConfig > 0 ){
                runOnUiThread {
                    val updToast = Toast.makeText(this@AppConfigActivity, getString(R.string.config_inserted), Toast.LENGTH_SHORT)
                    updToast.show()
                    Handler().postDelayed({
                        updToast.cancel()
                    }, 200)
                }
            } else {
                runOnUiThread {
                    val updToast = Toast.makeText(this@AppConfigActivity, getString(R.string.config_error), Toast.LENGTH_SHORT)
                    updToast.show()
                    Handler().postDelayed({
                        updToast.cancel()
                    }, 200)
                }
            }
        }
    }
}