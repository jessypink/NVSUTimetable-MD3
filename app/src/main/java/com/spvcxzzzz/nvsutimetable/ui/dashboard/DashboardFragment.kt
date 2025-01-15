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
        checkEnabledSwitchesFromPrefs()

        binding.weekViewSettingSwitch.isChecked

        return root
    }

    private fun initClickListeners() {
        //2 clickListener настройки просмотра недели
        binding.weekViewConstraintLayout.setOnClickListener {
            binding.weekViewSettingSwitch.isChecked = !binding.weekViewSettingSwitch.isChecked
            saveWeekViewToPrefs(binding.weekViewSettingSwitch.isChecked)
        }
        binding.weekViewSettingSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveWeekViewToPrefs(isChecked)
        }

        //2 clickListener настройки управления жестами
        binding.gestureControlConstraintLayout.setOnClickListener {
            binding.gestureSettingSwitch.isChecked = !binding.gestureSettingSwitch.isChecked
            saveGestureControlToPrefs(binding.weekViewSettingSwitch.isChecked)
        }
        binding.gestureSettingSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveGestureControlToPrefs(isChecked)
        }
    }

    private fun saveGestureControlToPrefs(isChecked: Boolean) {
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("gesture_control", isChecked)
            apply()
        }
    }

    private fun saveWeekViewToPrefs(isChecked: Boolean) {
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("week_timetable_view", isChecked)
            apply()
        }
    }

    private fun checkEnabledSwitchesFromPrefs() {
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        binding.weekViewSettingSwitch.isChecked = sharedPreferences.getBoolean("week_timetable_view", false)
        binding.gestureSettingSwitch.isChecked = sharedPreferences.getBoolean("gesture_control", true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}