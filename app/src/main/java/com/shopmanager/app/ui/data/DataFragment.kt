package com.shopmanager.app.ui.data

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.shopmanager.app.R
import com.shopmanager.app.databinding.FragmentDataBinding
import com.shopmanager.app.util.BackupManager
import com.shopmanager.app.util.CsvExporter
import com.shopmanager.app.util.ThemeHelper
import com.shopmanager.app.util.addClickScaleAnimation

class DataFragment : Fragment(R.layout.fragment_data) {

    private var _binding: FragmentDataBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DataViewModel
    private var darkModeInit = false

    // SAF：创建备份文件
    private val createBackupLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            uri?.let { viewModel.backup(it) }
        }

    // SAF：选择恢复文件
    private val openRestoreLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { confirmRestore(it) }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDataBinding.bind(view)

        viewModel = ViewModelProvider(this)[DataViewModel::class.java]

        setupCards()
        setupDarkMode()
        setupAbout()
        observeResults()
    }

    private fun setupCards() {
        binding.cardBackup.addClickScaleAnimation()
        binding.cardBackup.setOnClickListener {
            createBackupLauncher.launch(BackupManager.generateBackupFileName())
        }
        binding.cardRestore.addClickScaleAnimation()
        binding.cardRestore.setOnClickListener {
            openRestoreLauncher.launch(arrayOf("*/*"))
        }
        binding.cardExport.addClickScaleAnimation()
        binding.cardExport.setOnClickListener {
            Toast.makeText(requireContext(), "正在生成 CSV…", Toast.LENGTH_SHORT).show()
            viewModel.exportCsv()
        }
    }

    private fun setupDarkMode() {
        darkModeInit = true
        binding.switchDarkMode.isChecked = ThemeHelper.isDarkMode(requireContext())
        darkModeInit = false

        binding.switchDarkMode.setOnCheckedChangeListener { _, checked ->
            if (darkModeInit) return@setOnCheckedChangeListener
            ThemeHelper.setDarkMode(checked)
            // setDefaultNightMode 会自动重建 Activity
        }
    }

    private fun setupAbout() {
        val version = try {
            val pkg = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            "v" + (pkg.versionName ?: "1.0.0")
        } catch (e: Exception) {
            "v1.0.0"
        }
        binding.tvAboutVersion.text = version
    }

    private fun observeResults() {
        viewModel.backupResult.observe(viewLifecycleOwner) { (ok, msg) ->
            if (ok) {
                Toast.makeText(requireContext(), "备份成功：$msg", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "备份失败：$msg", Toast.LENGTH_LONG).show()
            }
        }
        viewModel.restoreResult.observe(viewLifecycleOwner) { (ok, count) ->
            if (ok) {
                Toast.makeText(requireContext(), "恢复成功，共 $count 条商品", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "恢复失败", Toast.LENGTH_LONG).show()
            }
        }
        viewModel.exportResult.observe(viewLifecycleOwner) { file ->
            if (file != null) {
                CsvExporter.shareCsv(requireContext(), file)
            } else {
                Toast.makeText(requireContext(), "导出失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmRestore(uri: android.net.Uri) {
        AlertDialog.Builder(requireContext())
            .setTitle("恢复备份")
            .setMessage("恢复将覆盖当前所有商品数据，确定继续吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("恢复") { _, _ -> viewModel.restore(uri) }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
