package com.spvcxzzzz.nvsutimetable.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.spvcxzzzz.nvsutimetable.databinding.FragmentSearchBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Инициализация обработчиков кликов
        initClickListeners()

        // Включение свичей
        isWeekViewPrefs()

        binding.weekViewCheckBox.isChecked

        return root
    }

    private fun initClickListeners() {
        //2 чекбокса настройки просмотра недели
        binding.weekViewConstraintLayout.setOnClickListener {
            binding.weekViewCheckBox.isChecked = !binding.weekViewCheckBox.isChecked
            saveWeekViewToPrefs(binding.weekViewCheckBox.isChecked)
        }
        binding.weekViewCheckBox.setOnCheckedChangeListener { _, isChecked ->
            saveWeekViewToPrefs(isChecked)
        }
    }

    private fun saveWeekViewToPrefs(isChecked: Boolean) {
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("week_timetable_view", isChecked)
            apply()
        }
    }

    private fun isWeekViewPrefs() {
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        binding.weekViewCheckBox.isChecked = sharedPreferences.getBoolean("week_timetable_view", false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}