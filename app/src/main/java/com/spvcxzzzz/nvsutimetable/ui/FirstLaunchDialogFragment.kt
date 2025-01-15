package com.spvcxzzzz.nvsutimetable.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputLayout
import com.spvcxzzzz.nvsutimetable.R

class FirstLaunchDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Инфлейтите пользовательский интерфейс
        return inflater.inflate(R.layout.first_launch_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Настройка элементов интерфейса
        val textInputLayout: TextInputLayout = view.findViewById(R.id.textInputLayout)
        val textInputEditText: EditText = view.findViewById(R.id.textInputEditText)

        // Получение текста
        val inputText = textInputEditText.text.toString()

        // Установка ошибки
        if (inputText.isEmpty()) {
            textInputLayout.error = "Поле не должно быть пустым"
        } else {
            textInputLayout.error = null // Убираем ошибку
        }

    }
}
