package com.example.capstone.view

import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.capstone.R
import com.example.capstone.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigationView: BottomNavigationView
    private var lastBackPressTime: Long = 0
    private lateinit var backToast: Toast

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = ""

        viewPager = binding.viewPager
        bottomNavigationView = binding.bottomNavigation

        viewPager.adapter = ViewPagerAdapter(this)
        viewPager.setCurrentItem(0, false)

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> viewPager.setCurrentItem(0, true)
                R.id.navigation_theater -> viewPager.setCurrentItem(1, true)
                R.id.navigation_ticket -> viewPager.setCurrentItem(2, true)
                else -> false
            }
            true
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> bottomNavigationView.selectedItemId = R.id.navigation_home
                    1 -> bottomNavigationView.selectedItemId = R.id.navigation_theater
                    2 -> bottomNavigationView.selectedItemId = R.id.navigation_ticket
                }
            }
        })

        bottomNavigationView.selectedItemId = R.id.navigation_home

        backToast = Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT)
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isTaskRoot) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastBackPressTime < 2000) {
                        backToast.cancel()
                        finish()
                    } else {
                        lastBackPressTime = currentTime
                        backToast.show()
                    }
                } else {
                    super@MainActivity.onBackPressed()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
}
