package dev.markodojkic.singiattend

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
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.ArrayRes
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.stream.Collectors
import java.util.stream.IntStream
import kotlin.math.ceil
import kotlin.math.roundToInt

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
    private lateinit var connection: HttpURLConnection

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
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, IntStream.rangeClosed(2000, 2999).boxed().collect(Collectors.toList()))
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        indexYear.adapter = yearAdapter
        indexTxt.filters = arrayOf<InputFilter>(LengthFilter(6))
        indexTxt.inputType = InputType.TYPE_CLASS_NUMBER

        studentMailText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (studentMailText.text.toString().matches("^[a-z]+\\.[a-z]+\\.[0-9]{2,3}@$".toRegex())) {
                    studentMailText.setText(String.format("%s%s", studentMailText.text.subSequence(0, studentMailText.text.toString().indexOf("@") + 1), "singimail.rs"))
                    studentMailText.setSelection(studentMailText.text.toString().indexOf("@"))
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })

        indexYear.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                (parent?.getChildAt(0) as TextView).setTextColor(Color.WHITE)
                (parent?.getChildAt(0) as TextView).textSize = 20.0F
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        faculties.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
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

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        courses.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                (parent?.getChildAt(0) as TextView).setTextColor(Color.WHITE)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
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
        val adapter = ArrayAdapter(this, R.layout.multiline_simple_spinner, resources.getStringArray(fID))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        courses.adapter = adapter
    }

    fun englishCourses(v: View?) {
        isSerbian = false
        findViewById<View>(R.id.english_course_btn).setBackgroundResource(R.drawable.border_green)
        findViewById<View>(R.id.serbian_course_btn).setBackgroundColor(Color.BLACK)
        val adapter = ArrayAdapter(
                this, R.layout.multiline_simple_spinner, resources.getStringArray(R.array.faculties_eng))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        faculties.adapter = adapter
        courses.adapter = null
    }

    fun serbianCourses(v: View?) {
        isSerbian = true
        findViewById<View>(R.id.serbian_course_btn).setBackgroundResource(R.drawable.border_green)
        findViewById<View>(R.id.english_course_btn).setBackgroundColor(Color.BLACK)
        val adapter = ArrayAdapter(this, R.layout.multiline_simple_spinner, resources.getStringArray(R.array.faculties_srb))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        faculties.adapter = adapter
        courses.adapter = null
    }

    fun onRegister(v: View?) {
        if (nameSurnameText.text == null || nameSurnameText.text.length < 2) {
            nameSurnameText.setBackgroundColor(Color.RED)
            hasError = true
            return
        } else {
            nameSurnameText.setBackgroundColor(Color.GREEN)
            hasError = false
        }
        if (indexYear.selectedItem == null) {
            indexYear.setBackgroundColor(Color.RED)
            hasError = true
            return
        } else {
            indexYear.setBackgroundColor(Color.GREEN)
            hasError = false
        }
        if (indexTxt.text == null || indexTxt.text.length != 6) {
            indexTxt.setBackgroundColor(Color.RED)
            hasError = true
            return
        } else {
            indexTxt.setBackgroundColor(Color.GREEN)
            hasError = false
        }
        if (studentMailText.text == null || !studentMailText.text.toString().matches("^[a-z]+\\.[a-z]+\\.[0-9]{2,3}@singimail.rs$".toRegex())) {
            studentMailText.setBackgroundColor(Color.RED)
            hasError = true
            return
        } else {
            studentMailText.setBackgroundColor(Color.GREEN)
            hasError = false
        }
        if (passText.text == null || passText.text.length < 8) {
            passText.setBackgroundColor(Color.RED)
            hasError = true
            return
        } else {
            passText.setBackgroundColor(Color.GREEN)
            hasError = false
        }
        if (faculties.selectedItem == null) {
            faculties.setBackgroundColor(Color.RED)
            hasError = true
            return
        } else {
            faculties.setBackgroundColor(Color.GREEN)
            hasError = false
        }
        hasError = if (courses.selectedItem == null) {
            courses.setBackgroundColor(Color.RED)
            true
        } else {
            courses.setBackgroundColor(Color.GREEN)
            false
        }
        if (!hasError) {
            val thread = Thread {
                try {
                    connection = URL(BuildConfig.SERVER_URL + "/api/insert/student").openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Accept", "application/json;charset=UTF-8")
                    connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
                    connection.setRequestProperty("Authorization", String.format("Basic %s", String(Base64.getEncoder().encode(BuildConfig.SERVER_CREDENTIALS.toByteArray(StandardCharsets.UTF_8)))))
                    connection.doInput = true
                    connection.doOutput = true
                    connection.connectTimeout = 1000
                    val output = OutputStreamWriter(connection.outputStream)
                    val data = JSONObject()
                    data.put("name_surname", nameSurnameText.text.toString())
                    data.put("index", String.format("%s/%s", indexYear.selectedItem.toString(), indexTxt.text.toString()))
                    data.put("password_hash", passText.text.toString())
                    data.put("email", studentMailText.text.toString())
                    data.put("studyId", studyId[courses.selectedItemPosition])
                    data.put("year", ceil(Math.random() * if (studyId[courses.selectedItemPosition].compareTo("61c7328fe22ce55efb31ac02") == 0) 5 else 4).roundToInt().toString()) //Pharmacy is 5 years course
                    connection.connect()
                    output.write(data.toString())
                    output.flush()
                    output.close()
                    when (connection.responseCode) {
                        200 -> {
                            runOnUiThread {
                                val successDialog = SweetAlertDialog(this@RegisterActivity, SweetAlertDialog.SUCCESS_TYPE)
                                successDialog
                                        .setTitleText(R.string.regTitleSuccess)
                                        .setConfirmText(resources.getString(R.string.regMessageSuccess))
                                        .setConfirmText(resources.getString(R.string.ok))
                                        .setConfirmClickListener { _: SweetAlertDialog? ->
                                            successDialog.dismiss()
                                            startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                                        }
                                        .show()
                            }
                        }
                        500 -> {
                            runOnUiThread {
                                val failedDialog = SweetAlertDialog(this@RegisterActivity, SweetAlertDialog.ERROR_TYPE)
                                failedDialog
                                        .setTitleText(R.string.regTitleFailed)
                                        .setContentText(resources.getString(R.string.regMessageFailed))
                                        .setConfirmText(resources.getString(R.string.ok))
                                        .setConfirmClickListener { _: SweetAlertDialog? -> failedDialog.dismiss() }
                                        .show()
                            }
                        }
                        else -> {
                            runOnUiThread {
                                val failedDialog = SweetAlertDialog(this@RegisterActivity, SweetAlertDialog.WARNING_TYPE)
                                failedDialog
                                        .setTitleText(R.string.regTitleFailed)
                                        .setContentText(resources.getString(R.string.regMessageServerError))
                                        .setConfirmText(resources.getString(R.string.ok))
                                        .setConfirmClickListener { _: SweetAlertDialog? -> failedDialog.dismiss() }
                                        .show()
                            }
                        }
                    }
                } catch (e: IOException) {
                    if (e.javaClass.name == SocketTimeoutException::class.java.name) {
                        runOnUiThread {
                            val warningDialog = SweetAlertDialog(this@RegisterActivity, SweetAlertDialog.WARNING_TYPE)
                            warningDialog
                                    .setTitleText(R.string.regTitleFailed)
                                    .setConfirmText(resources.getString(R.string.regMessageServerError))
                                    .setConfirmText(resources.getString(R.string.ok))
                                    .show()
                        }
                    }
                    e.printStackTrace()
                } catch (e: JSONException) {
                    if (e.javaClass.name == SocketTimeoutException::class.java.name) {
                        runOnUiThread {
                            val warningDialog = SweetAlertDialog(this@RegisterActivity, SweetAlertDialog.WARNING_TYPE)
                            warningDialog
                                    .setTitleText(R.string.regTitleFailed)
                                    .setConfirmText(resources.getString(R.string.regMessageServerError))
                                    .setConfirmText(resources.getString(R.string.ok))
                                    .show()
                        }
                    }
                    e.printStackTrace()
                } finally {
                    connection.disconnect()
                }
            }
            thread.start()
        }
    }
}