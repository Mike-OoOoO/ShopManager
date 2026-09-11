package com.shopmanager.app

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.shopmanager.app.databinding.ActivityMainBinding
import com.shopmanager.app.util.ThemeHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 检查上次崩溃日志并显示
        showCrashLogIfExists()

        // FragmentContainerView 必须通过 NavHostFragment 获取 NavController
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showBottomNav = when (destination.id) {
                R.id.homeFragment,
                R.id.productsFragment,
                R.id.dataFragment -> true
                else -> false
            }
            binding.bottomNavigation.visibility =
                if (showBottomNav) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun showCrashLogIfExists() {
        val log = App.readCrashLog(this)
        if (!log.isNullOrEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("检测到上次崩溃")
                .setMessage(log)
                .setPositiveButton("清除并继续") { _, _ ->
                    App.clearCrashLog(this)
                }
                .setNegativeButton("保留", null)
                .show()
        }
    }
}
