package dev.markodojkic.singiattend

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.ArrayRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import dev.markodojkic.singiattend.MainActivity.Companion.csrfTokenManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.stream.Collectors
import java.util.stream.IntStream
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val APPLICATION_JSON_CHARSET_UTF_8 = "application/json;charset=UTF-8"

class RegisterActivity : AppCompatActivity() {
    private var isSerbian = false
    private var hasError = false
    private lateinit var nameSurnameText: EditText
    private lateinit var indexTxt: EditText
    private lateinit var studentMailText: EditText
    private lateinit var passText: EditText
    private lateinit var faculties: Spinner
    private lateinit var courses: Spinner
    private lateinit var indexYear: Spinner
    private lateinit var studyId: MutableList<String>
    private lateinit var englishListener: RadioGroup.OnCheckedChangeListener
    private lateinit var serbianListener: RadioGroup.OnCheckedChangeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        nameSurnameText = findViewById(R.id.nameSurnameREG_txt)
        indexTxt = findViewById(R.id.indexREG_txt)
        studentMailText = findViewById(R.id.singiMailREG_txt)
        passText = findViewById(R.id.passREG_txt)
        faculties = findViewById(R.id.faculty_spin)
        courses = findViewById(R.id.course_spin)
        indexYear = findViewById(R.id.indexYear_spin)
        studyId = ArrayList()

        val yearAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            IntStream.rangeClosed(2000, 2999).boxed().collect(Collectors.toList())
        )
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        indexYear.adapter = yearAdapter
        indexTxt.filters = arrayOf<InputFilter>(LengthFilter(6))
        indexTxt.inputType = InputType.TYPE_CLASS_NUMBER

        studentMailText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { /* Not used */ }

            @SuppressLint("SetTextI18n")
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (studentMailText.text.toString()
                        .matches("^[a-z]+\\.[a-z]+\\.[0-9]{2,3}@$".toRegex())
                ) {
                    studentMailText.setText(
                        "${
                            studentMailText.text.subSequence(
                                0,
                                studentMailText.text.toString().indexOf("@") + 1
                            )
                        }singimail.rs"
                    )
                    studentMailText.setSelection(studentMailText.text.toString().indexOf("@"))
                }
            }

            override fun afterTextChanged(s: Editable) { /* Not used */ }
        })

        indexYear.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                (parent?.getChildAt(0) as TextView).setTextColor(Color.WHITE)
                (parent.getChildAt(0) as TextView).textSize = 20.0F
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { /* Not used */ }
        }

        faculties.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                @ArrayRes val facultyID: Int = when (position) {
                    0 -> if (isSerbian) R.array.courses_f1_srb else R.array.courses_f1_eng
                    1 -> if (isSerbian) R.array.courses_f2_srb else R.array.courses_f2_eng
                    2 -> if (isSerbian) R.array.courses_f3_srb else R.array.courses_f3_eng
                    3 -> if (isSerbian) R.array.courses_f4_srb else R.array.courses_f4_eng
                    4 -> if (isSerbian) R.array.courses_f5_srb else R.array.courses_f5_eng
                    else -> R.array.courses_f6_srb
                }

                (parent?.getChildAt(0) as TextView).setTextColor(Color.WHITE)

                updateCourses(facultyID)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { /* Not used */ }
        }

        courses.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                (parent?.getChildAt(0) as TextView).setTextColor(Color.WHITE)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { /* Not used */ }
        }

        val englishGroup = findViewById<RadioGroup>(R.id.english_course_rg)
        val serbianGroup = findViewById<RadioGroup>(R.id.serbian_course_rg)

        englishListener = RadioGroup.OnCheckedChangeListener { group, checkedId ->
            if (checkedId != -1) {
                serbianGroup.setOnCheckedChangeListener(null)
                serbianGroup.clearCheck()
                serbianGroup.foreground = null
                serbianGroup.setOnCheckedChangeListener(serbianListener)

                isSerbian = false
                group.foreground = ContextCompat.getDrawable(this, R.drawable.border_green)
                val adapter = ArrayAdapter(
                    this,
                    R.layout.multiline_simple_spinner,
                    resources.getStringArray(R.array.faculties_eng).toMutableList()
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                when (findViewById<RadioButton>(checkedId).tag.toString()) {
                    "SingidunumBG" -> { /* Not used */ }
                    "SingidunumNS" -> {
                        adapter.remove(adapter.getItem(4))
                    }

                    "SingidunumNIS" -> {
                        adapter.remove(adapter.getItem(4))
                    }
                }

                faculties.adapter = adapter
                courses.adapter = null
            } else group.foreground = null
        }

        serbianListener = RadioGroup.OnCheckedChangeListener { group, checkedId ->
            if (checkedId != -1) {
                englishGroup.setOnCheckedChangeListener(null)
                englishGroup.clearCheck()
                englishGroup.foreground = null
                englishGroup.setOnCheckedChangeListener(englishListener)

                isSerbian = true

                group.foreground = ContextCompat.getDrawable(this, R.drawable.border_green)
                val adapter = ArrayAdapter(
                    this,
                    R.layout.multiline_simple_spinner,
                    resources.getStringArray(R.array.faculties_srb).toMutableList()
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                when (findViewById<RadioButton>(checkedId).tag.toString()) {
                    "SingidunumBG" -> { /* Not used */ }
                    "SingidunumNS" -> {
                        adapter.remove(adapter.getItem(4))
                    }

                    "SingidunumNIS" -> {
                        adapter.remove(adapter.getItem(3))
                        adapter.remove(adapter.getItem(4))
                    }
                }

                faculties.adapter = adapter
                courses.adapter = null
            } else group.foreground = null
        }

        englishGroup.setOnCheckedChangeListener(englishListener)
        serbianGroup.setOnCheckedChangeListener(serbianListener)

        findViewById<Button>(R.id.register_btn).setOnClickListener {
            if (nameSurnameText.text == null || nameSurnameText.text.length < 2) {
                nameSurnameText.setBackgroundColor(Color.RED)
                hasError = true
                return@setOnClickListener
            } else {
                nameSurnameText.setBackgroundColor(Color.GREEN)
                hasError = false
            }
            if (indexYear.selectedItem == null) {
                indexYear.setBackgroundColor(Color.RED)
                hasError = true
                return@setOnClickListener
            } else {
                indexYear.setBackgroundColor(Color.GREEN)
                hasError = false
            }
            if (indexTxt.text == null || indexTxt.text.length != 6) {
                indexTxt.setBackgroundColor(Color.RED)
                hasError = true
                return@setOnClickListener
            } else {
                indexTxt.setBackgroundColor(Color.GREEN)
                hasError = false
            }
            if (studentMailText.text == null || !studentMailText.text.toString()
                    .matches("^[a-z]+\\.[a-z]+\\.[0-9]{2,3}@singimail.rs$".toRegex())
            ) {
                studentMailText.setBackgroundColor(Color.RED)
                hasError = true
                return@setOnClickListener
            } else {
                studentMailText.setBackgroundColor(Color.GREEN)
                hasError = false
            }
            if (passText.text == null || passText.text.length < 8) {
                passText.setBackgroundColor(Color.RED)
                hasError = true
                return@setOnClickListener
            } else {
                passText.setBackgroundColor(Color.GREEN)
                hasError = false
            }

            val selectedId = listOf(
                serbianGroup.checkedRadioButtonId,
                englishGroup.checkedRadioButtonId
            ).firstOrNull { it != -1 }

            if (selectedId == null) {
                findViewById<TextView>(R.id.courseREG_text).setBackgroundColor(Color.RED)
                hasError = true
                return@setOnClickListener
            } else {
                findViewById<TextView>(R.id.courseREG_text).setBackgroundColor(Color.GREEN)
                hasError = false
            }

            if (faculties.selectedItem == null) {
                faculties.setBackgroundColor(Color.RED)
                hasError = true
                return@setOnClickListener
            } else {
                faculties.setBackgroundColor(Color.GREEN)
                hasError = false
            }

            if (courses.selectedItem == null) {
                courses.setBackgroundColor(Color.RED)
                hasError = true
            } else {
                courses.setBackgroundColor(Color.GREEN)
                hasError = false
            }
            if (!hasError) {
                csrfTokenManager.proxyIdentifier = findViewById<RadioButton>(selectedId).tag.toString()

                csrfTokenManager.fetchCsrfSession { success ->
                    if (!success) {
                        runOnUiThread {
                            val failedDialog =
                                SweetAlertDialog(this@RegisterActivity, SweetAlertDialog.ERROR_TYPE)
                            failedDialog
                                .setTitleText(R.string.regTitleFailed)
                                .setContentText(resources.getString(R.string.serverInactive))
                                .setConfirmText(resources.getString(R.string.ok))
                                .setConfirmClickListener { _: SweetAlertDialog? -> failedDialog.dismiss() }
                                .show()
                        }
                    }


                    if (csrfTokenManager.proxyIdentifier.isEmpty() || csrfTokenManager.sessionData.jsessionId.isEmpty() || csrfTokenManager.sessionData.xsrfToken.isEmpty() || csrfTokenManager.sessionData.csrfTokenSecret.isEmpty() || csrfTokenManager.sessionData.csrfHeaderName.isEmpty()) {
                        runOnUiThread {
                            val failedDialog =
                                SweetAlertDialog(this@RegisterActivity, SweetAlertDialog.ERROR_TYPE)
                            failedDialog
                                .setTitleText(R.string.regTitleFailed)
                                .setContentText(resources.getString(R.string.serverInactive))
                                .setConfirmText(resources.getString(R.string.ok))
                                .setConfirmClickListener { _: SweetAlertDialog? -> failedDialog.dismiss() }
                                .show()
                        }

                        return@fetchCsrfSession
                    }

                    OkHttpClient().newCall(
                        Request.Builder()
                            .url("${BuildConfig.SERVER_URL}/api/insert/student")
                            .post(
                                JSONObject().apply {
                                    put("nameSurname", nameSurnameText.text.toString())
                                    put("index", "${indexYear.selectedItem}/${indexTxt.text}")
                                    put("passwordHash", passText.text.toString())
                                    put("email", studentMailText.text.toString())
                                    put("studyId", studyId[courses.selectedItemPosition])
                                    put(
                                        "year",
                                        ceil(
                                            Math.random() * if (studyId[courses.selectedItemPosition].compareTo(
                                                    "61c7328fe22ce55efb31ac02"
                                                ) == 0
                                            ) 5 else 4
                                        ).roundToInt().toString()
                                    )
                                }.toString()
                                    .toRequestBody(APPLICATION_JSON_CHARSET_UTF_8.toMediaType())
                            )
                            .addHeader("Accept", APPLICATION_JSON_CHARSET_UTF_8)
                            .addHeader("Content-Type", APPLICATION_JSON_CHARSET_UTF_8)
                            .addHeader(
                                "Authorization",
                                "Basic ${
                                    Base64.getEncoder().encodeToString(
                                        BuildConfig.SERVER_CREDENTIALS.toByteArray(StandardCharsets.UTF_8)
                                    )
                                }"
                            )
                            .addHeader("X-Tenant-ID", csrfTokenManager.proxyIdentifier)
                            .addHeader(
                                csrfTokenManager.sessionData.csrfHeaderName,
                                csrfTokenManager.sessionData.csrfTokenSecret
                            )
                            .addHeader(
                                "Cookie",
                                "JSESSIONID=${csrfTokenManager.sessionData.jsessionId}; XSRF-TOKEN=${csrfTokenManager.sessionData.xsrfToken}"
                            )
                            .build()
                    ).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            runOnUiThread {
                                showFailDialog(
                                    R.string.regMessageServerError,
                                    SweetAlertDialog.WARNING_TYPE
                                )
                            }
                        }

                        override fun onResponse(call: Call, response: Response) {
                            runOnUiThread {
                                when (response.code) {
                                    200 -> {
                                        SweetAlertDialog(
                                            this@RegisterActivity,
                                            SweetAlertDialog.SUCCESS_TYPE
                                        )
                                            .setTitleText(R.string.regTitleSuccess)
                                            .setContentText(getString(R.string.regMessageSuccess))
                                            .setConfirmText(getString(R.string.ok))
                                            .setConfirmClickListener {
                                                it?.dismiss()
                                                startActivity(
                                                    Intent(
                                                        this@RegisterActivity,
                                                        MainActivity::class.java
                                                    )
                                                )
                                                finish()
                                            }
                                            .show()
                                    }

                                    500 -> showFailDialog(R.string.regMessageFailed)
                                    else -> showFailDialog(
                                        R.string.regMessageServerError,
                                        SweetAlertDialog.WARNING_TYPE
                                    )
                                }
                            }
                        }

                        private fun showFailDialog(
                            msgRes: Int,
                            type: Int = SweetAlertDialog.ERROR_TYPE
                        ) {
                            SweetAlertDialog(this@RegisterActivity, type)
                                .setTitleText(R.string.regTitleFailed)
                                .setContentText(getString(msgRes))
                                .setConfirmText(getString(R.string.ok))
                                .setConfirmClickListener { it?.dismiss() }
                                .show()
                        }
                    })
                }
            }
        }
    }

    private fun updateCourses(@ArrayRes fID: Int) {
        studyId.clear()
        when (fID) {
            2130903041 -> {
                studyId.add("61b612d3e1534b76962f2568")
                studyId.add("61b612d3e1534b76962f256d")
            }

            2130903043 -> studyId.add("61b612d3e1534b76962f2572")
            2130903045 -> studyId.add("61b612d3e1534b76962f256c")
            2130903047 -> studyId.add("61b612d3e1534b76962f256f")
            2130903049 -> studyId.add("61b612d3e1534b76962f2570")
            2130903042 -> {
                studyId.add("61b612d3e1534b76962f2564")
                studyId.add("61b612d3e1534b76962f256b")
            }

            2130903044 -> studyId.add("61b612d3e1534b76962f2563")
            2130903046 -> {
                studyId.add("61b612d3e1534b76962f256e")
                studyId.add("61b612d3e1534b76962f2566")
            }

            2130903048 -> studyId.add("61b612d3e1534b76962f2569")
            2130903050 -> {
                studyId.add("61b612d3e1534b76962f2571")
                studyId.add("61b612d3e1534b76962f256a")
            }

            else -> {
                studyId.add("61b612d3e1534b76962f2565")
                studyId.add("61c7328fe22ce55efb31ac02")
            }
        }
        val adapter =
            ArrayAdapter(this, R.layout.multiline_simple_spinner, resources.getStringArray(fID))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        courses.adapter = adapter
    }
}