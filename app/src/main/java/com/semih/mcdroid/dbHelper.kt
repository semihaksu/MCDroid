package com.semih.mcdroid

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Define a data class for each table
data class User(
    val id: Int,
    val mc_id: Int,
    val mc_user: String,
    val mc_pass: String,
    val mc_pass2: String
)

data class Units(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    val unit_id: Int,
    val unit_name: String,
    val unit_convert: Double,
    val unit_main: Int
)

data class Articles(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int, val art_id: Int,
    val art_number: Int,
    val art_name: String,
    val art_unit: Int,
    val art_group: Int
)

data class Groups(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int,
    val group_id: Int,
    val group_name: String
)

data class Config(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    val name: String,
    val value: String
)

class dbHelper (context: Context) : SQLiteOpenHelper(context, "mcdroid.db", null, 1) {
    private val CREATE_USERS = "CREATE TABLE users (id INTEGER PRIMARY KEY, mc_id INTEGER UNIQUE, mc_user TEXT, mc_pass TEXT, mc_pass2 TEXT)"
    private val CREATE_UNITS = "CREATE TABLE units (id INTEGER PRIMARY KEY, unit_id INTEGER UNIQUE, unit_name TEXT, unit_convert DOUBLE, unit_main INTEGER)"
    private val CREATE_ARTIKELS = "CREATE TABLE articles (id INTEGER PRIMARY KEY, art_id INTEGER UNIQUE, art_number INTEGER UNIQUE, art_name TEXT, art_unit INTEGER, art_group INTEGER)"
    private val CREATE_GROUPS = "CREATE TABLE groups (id INTEGER PRIMARY KEY, group_id INTEGER UNIQUE, group_name TEXT)"
    private val CREATE_SETTINGS = "CREATE TABLE settings (id INTEGER PRIMARY KEY, name TEXT UNIQUE, value TEXT)"

    // Override the onCreate() method to execute the SQL statements
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(CREATE_USERS)
        db?.execSQL(CREATE_UNITS)
        db?.execSQL(CREATE_ARTIKELS)
        db?.execSQL(CREATE_GROUPS)
        db?.execSQL(CREATE_SETTINGS)
    }

    // Override the onUpgrade() method to handle upgrading your database schema
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Handle upgrades here
    }

    // Insert a user into the users table
    fun insertUser(user: User): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("mc_id", user.mc_id)
            put("mc_user", user.mc_user)
            put("mc_pass", user.mc_pass)
            put("mc_pass2", user.mc_pass2)
        }
        return db.insert("users", null, values).toLong()
    }

    // Update the userPass value for an existing user
    fun updateUserPass(userId: Int, userPass: String, userPass2: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("mc_pass", userPass)
            put("mc_pass2", userPass2)
        }
        val selection = "mc_id = ?"
        val selectionArgs = arrayOf("$userId")
        return db.update("users", values, selection, selectionArgs).toLong()
    }

    fun getUserByCredentials(username: String, password: String): Cursor? {
        val db = this.readableDatabase
        val query = "SELECT * FROM users WHERE mc_user = ? AND mc_pass = ?"
        return db.rawQuery(query, arrayOf(username, password))
    }

    fun insertUnit(unit: Units): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("unit_id", unit.unit_id)
            put("unit_name", unit.unit_name)
            put("unit_convert", unit.unit_convert)
            put("unit_main", unit.unit_main)
        }
        return db.insert("units", null, values).toLong()
    }

    fun updateUnits(unit_id: Int, unit_name: String, unit_convert: Double, unit_main: Int): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("unit_name", unit_name)
            put("unit_convert", unit_convert)
            put("unit_main", unit_main)
        }
        val selection = "unit_id = ?"
        val selectionArgs = arrayOf("$unit_id")
        return db.update("units", values, selection, selectionArgs).toLong()
    }

    fun insertGroup(group: Groups): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("group_id", group.group_id)
            put("group_name", group.group_name)
        }
        return db.insert("groups", null, values).toLong()
    }

    fun updateGroups(group_id: Int, group_name: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("group_name", group_name)
        }
        val selection = "group_id = ?"
        val selectionArgs = arrayOf("$group_id")
        return db.update("groups", values, selection, selectionArgs).toLong()
    }

    fun updateArticles(artID: Int, artName: String, wgrId: Int, vpkId: Int, artNumber: Int): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("art_id", artID)
            put("art_number", artNumber)
            put("art_name", artName)
            put("art_unit", vpkId)
            put("art_group", wgrId)
        }
        val selection = "art_id = ?"
        val selectionArgs = arrayOf("$artID")
        return db.update("articles", values, selection, selectionArgs).toLong()
    }

    fun getSettings(settingName: String): String? {
        val db = writableDatabase

        val query = "SELECT value FROM settings WHERE name = ?"
        val cursor = db.rawQuery(query, arrayOf(settingName))
        var settingValue: String? = null
        if (cursor != null && cursor.moveToFirst()) {
            settingValue = cursor.getString(cursor.getColumnIndexOrThrow("value"))
        }
        cursor.close()
        return settingValue
    }

    fun insertItem(item: Any): Long {
        return writableDatabase.use{db ->
            when(item) {
                is Units -> {
                    val values = ContentValues().apply {
                        put("unit_id", item.unit_id)
                        put("unit_name", item.unit_name)
                        put("unit_convert", item.unit_convert)
                        put("unit_main", item.unit_main)
                    }
                    db.insert("units", null, values)
                }
                is Groups -> {
                    val values = ContentValues().apply {
                        put("group_id", item.group_id)
                        put("group_name", item.group_name)
                    }
                    db.insert("groups", null, values)
                }
                is Articles -> {
                    val values = ContentValues().apply {
                        put("art_id", item.art_id)
                        put("art_number", item.art_number)
                        put("art_name", item.art_name)
                        put("art_unit", item.art_unit)
                        put("art_group", item.art_group)
                    }
                    db.insert("articles", null, values)
                }
                else -> {
                    0
                }
            }
        }
    }
}