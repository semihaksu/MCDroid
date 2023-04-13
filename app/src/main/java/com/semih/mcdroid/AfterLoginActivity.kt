package com.semih.mcdroid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AfterLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logged_in)

        // Add any necessary initialization or processing code here
        val loggedInScreen = intent

        if (loggedInScreen.hasExtra("userName")) {
            // Retrieve the extra data
            val userName = loggedInScreen.getStringExtra("userName");
        }

        val importButton = findViewById<Button>(R.id.importButton)
        importButton.setOnClickListener {
            importButtonClicked(it)
        }

        var masterSyncButton = findViewById<Button>(R.id.masterSyncButton)
        masterSyncButton.setOnClickListener {
            masterSyncButtonClicked(it)
        }

        val logoutButton = findViewById<Button>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            logoutButtonClicked(it)
        }
    }

    fun logoutButtonClicked(v: View) {
        // Create an Intent to start the MainActivity
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        startActivity(mainActivityIntent)
        finish()
    }

    fun importButtonClicked(v: View) {
        val importScreen = Intent(this@AfterLoginActivity, InventoryImportActivity::class.java)
        startActivity(importScreen)
        finish()
    }

    fun masterSyncButtonClicked(v: View) {
        val masterScreen = Intent(this@AfterLoginActivity, MasterDataSyncActivity::class.java)
        startActivity(masterScreen)
    }
}