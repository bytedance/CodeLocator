package com.bytedance.tools.codelocator.demo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bytedance.tools.codelocator.demo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvwStartActivity.setOnClickListener {
            startActivity(Intent(this, TestActivity::class.java))
        }
        binding.tvwShowDialog.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                MainDialogFragment().show(supportFragmentManager, "CodeLocator")
            }
            return@setOnTouchListener true
        }
        binding.tvwShowPop.setOnClickListener {
            PopupWindow(
                LayoutInflater.from(it.context).inflate(R.layout.pop_view, null),
                500,
                90
            ).showAsDropDown(it)
        }
        binding.tvwShowToast.setOnClickListener {
            Toast.makeText(this, "Hello CodeLocator!", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.ivw_show_res).setOnClickListener {
            findViewById<ImageView>(R.id.ivw_show_res).setImageResource(R.drawable.codelocator)
        }

        getSharedPreferences("test_sp", Context.MODE_PRIVATE).edit().putString("Hello", "World")
            .commit()

        findViewById<View>(R.id.tvw_test_sp).setOnClickListener {
            Toast.makeText(
                it.context,
                "Sp: " + getSharedPreferences("test_sp", Context.MODE_PRIVATE).getString(
                    "Hello",
                    "World"
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

}