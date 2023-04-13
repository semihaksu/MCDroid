package com.semih.mcdroid

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class InventoryImportActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var importButton: Button

    companion object {
        private const val TAG = "InventoryImport"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.inventory_import)

        // Find views by ID
        listView = findViewById(R.id.listView)
        importButton = findViewById(R.id.importButton)

        // Set up the list view adapter
        val adapter = InventoryAdapter(this, mutableListOf())
        listView.adapter = adapter

        // Set up click listener for import button
        importButton.setOnClickListener {
            val checkedItems = adapter.getCheckedItems()
            val checkedIds = checkedItems.joinToString(",") { it.invid.toString() }
            Log.d(TAG, "Checked item IDs: $checkedIds")
        }

        // Fetch inventory items from web service
        fetchInventoryItems()
    }

    private fun fetchInventoryItems() {
        val dbGetHelper = dbHelper(this@InventoryImportActivity)
        val serviceURL = dbGetHelper.getSettings("service_url")
        dbGetHelper.close()
        val url = "$serviceURL/mc/getData.php?w=inventory"
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to fetch inventory items", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch inventory items - status code ${response.code}")
                    return
                }

                val responseBody = response.body?.string() ?: ""
                val inventoryItems = parseInventoryItems(responseBody)

                runOnUiThread {
                    // Update the list view adapter with the new inventory items
                    val adapter = listView.adapter as InventoryAdapter
                    adapter.updateItems(inventoryItems)
                }
            }
        })
    }

    private fun parseInventoryItems(json: String): List<InventoryItem> {
        val jsonArray = JSONArray(json)
        val items = mutableListOf<InventoryItem>()

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)

            val invid = jsonObject.getInt("invid")
            val invname = jsonObject.getString("invname")
            val depname = jsonObject.getString("depname")
            val invdateString = jsonObject.getString("invdate")
            val invdate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse(invdateString)

            val item = InventoryItem(invid, invname, depname, invdate)
            items.add(item)
        }

        return items
    }

    inner class InventoryAdapter(context: Context, items: List<InventoryItem>) :
        ArrayAdapter<InventoryItem>(context, R.layout.inventory_item_layout, items) {

        private val inflater = LayoutInflater.from(context)

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = convertView

            if (view == null) {
                view = inflater.inflate(R.layout.inventory_item_layout, parent, false)
            }

            val item = getItem(position)

            val checkBox = view?.findViewById<CheckBox>(R.id.checkBox)
            checkBox?.setOnCheckedChangeListener { _, isChecked ->
                item?.checked = isChecked
            }

            val nameTextView = view?.findViewById<TextView>(R.id.nameTextView)
            nameTextView?.text = item?.invname

            val depNameTextView = view?.findViewById<TextView>(R.id.depNameTextView)
            depNameTextView?.text = item?.depname

            val dateTextView = view?.findViewById<TextView>(R.id.dateTextView)
            dateTextView?.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(item?.invdate)

            return view!!
        }

        fun getCheckedItems(): List<InventoryItem> {
            val items = mutableListOf<InventoryItem>()
            for (i in 0 until count) {
                val item = getItem(i)
                if (item?.checked == true) {
                    items.add(item)
                }
            }
            return items
        }

        fun updateItems(items: List<InventoryItem>) {
            clear()
            addAll(items)
            notifyDataSetChanged()
        }
    }
}