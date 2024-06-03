package com.app.livestreamingapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.google.android.material.button.MaterialButtonToggleGroup

class InitialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initial)

        val admin = findViewById<Button>(R.id.admin)
        val client = findViewById<Button>(R.id.client)

        admin.setOnClickListener {
            Intent(this,AdminActivity::class.java).apply {
                startActivity(this)
                finish()
            }
        }
        client.setOnClickListener {
            Intent(this,ClientActivity::class.java).apply {
                startActivity(this)
                finish()
            }
        }

    }
}