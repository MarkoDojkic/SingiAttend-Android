package dev.markodojkic.singiattend

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.razerdp.widget.animatedpieview.AnimatedPieView
import com.razerdp.widget.animatedpieview.AnimatedPieViewConfig
import com.razerdp.widget.animatedpieview.data.IPieInfo
import com.razerdp.widget.animatedpieview.data.SimplePieInfo
import org.json.JSONArray
import org.json.JSONException
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Locale
import java.util.Objects
import java.util.stream.Collectors
import kotlin.math.ceil
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var gameCache: SharedPreferences
    private lateinit var disabledGrayOut: View
    private lateinit var coursesDataJson: String
    private lateinit var attendanceDataJson: String
    private lateinit var serverInactive: TextView
    private lateinit var loggedInAs: TextView
    private var currentAttendanceClassID = 0

    private var someActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            disabledGrayOut.visibility = View.GONE
            gameCache.edit().putString("loggedInUserIndex", Objects.requireNonNull(result.data)?.getStringExtra("indexNo")).apply()
            Thread {
                var connection: HttpURLConnection? = null
                try {
                    connection = URL(String.format("%s/api/getStudentName/%s", BuildConfig.SERVER_URL, gameCache.getString("loggedInUserIndex", "null")!!.replace("/", ""))).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Accept", "text/plain;charset=UTF-8")
                    connection.setRequestProperty("Authorization", String.format("Basic %s", String(Base64.getEncoder().encode(BuildConfig.SERVER_CREDENTIALS.toByteArray(StandardCharsets.UTF_8)))))
                    connection.doInput = true
                    connection.connectTimeout = 1000
                    connection.connect()
                    when (connection.responseCode) {
                        200 -> {
                            val input = BufferedReader(InputStreamReader(connection.inputStream))
                            gameCache.edit().putString("loggedInUserName", input.readLine()).apply()
                            serverInactive.text = ""
                            input.close()
                            runOnUiThread { startDataStreaming() }
                        }
                        500 -> {
                            gameCache.edit().putString("loggedInUserName", "-SERVER ERROR-").apply()
                            serverInactive.text = ""
                        }
                        else -> {
                            gameCache.edit().putString("loggedInUserName", "").apply()
                            serverInactive.text = resources.getText(R.string.serverInactive)
                        }
                    }
                } catch (e: Exception) {
                    gameCache.edit().putString("loggedInUserName", "").apply()
                    serverInactive.text = resources.getText(R.string.serverInactive)
                    e.printStackTrace()
                } finally {
                    connection?.disconnect()
                    loggedInAs.text = String.format("%s (%s)", gameCache.getString("loggedInUserName", "null"), gameCache.getString("loggedInUserIndex", "null"))
                    finish()
                    startActivity(intent)
                }
            }.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        gameCache = applicationContext.getSharedPreferences("cachedData", MODE_PRIVATE)
        loggedInAs = findViewById(R.id.loggedInAs_text)
        disabledGrayOut = findViewById(R.id.disabledGrayOut)
        val logout = findViewById<Button>(R.id.login_btn)
        logout.visibility = View.INVISIBLE
        serverInactive = findViewById(R.id.serverInactive_text)
        if (gameCache.getString("loggedInUserIndex", null) == null) someActivityResultLauncher.launch(Intent(applicationContext, LoginActivity::class.java)) else {
            disabledGrayOut.visibility = View.GONE
            logout.visibility = View.VISIBLE
            Thread {
                var connection: HttpURLConnection? = null
                try {
                    connection = URL(String.format("%s/api/getStudentName/%s"), BuildConfig.SERVER_URL, gameCache.getString("loggedInUserIndex", "null")!!.replace("/", "")).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Accept", "text/plain;charset=UTF-8")
                    connection.setRequestProperty("Authorization", String.format("Basic %s", String(Base64.getEncoder().encode(BuildConfig.SERVER_CREDENTIALS.toByteArray(StandardCharsets.UTF_8)))))
                    connection.doInput = true
                    connection.connectTimeout = 1000
                    when (connection.responseCode) {
                        200 -> {
                            val input = BufferedReader(InputStreamReader(connection.inputStream))
                            gameCache.edit().putString("loggedInUserName", input.readLine()).apply()
                            serverInactive.text = ""
                            input.close()
                            startDataStreaming()
                        }
                        500 -> {
                            gameCache.edit().putString("loggedInUserName", "-SERVER ERROR-").apply()
                            serverInactive.text = ""
                            loggedInAs.text = String.format("%s (%s)", gameCache.getString("loggedInUserName", "null"), gameCache.getString("loggedInUserIndex", "null"))
                        }
                        else -> {
                            gameCache.edit().putString("loggedInUserName", "").apply()
                            serverInactive.text = resources.getText(R.string.serverInactive)
                            loggedInAs.text = String.format("%s (%s)", gameCache.getString("loggedInUserName", "null"), gameCache.getString("loggedInUserIndex", "null"))
                        }
                    }
                } catch (e: Exception) {
                    gameCache.edit().putString("loggedInUserName", "").apply()
                    serverInactive.text = resources.getText(R.string.serverInactive)
                    e.printStackTrace()
                } finally {
                    connection?.disconnect()
                    loggedInAs.text = String.format("%s (%s)", gameCache.getString("loggedInUserName", "null"), gameCache.getString("loggedInUserIndex", "null"))
                }
            }.start()
        }
    }

    private fun startDataStreaming() {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                val linearLayout = findViewById<LinearLayout>(R.id.sv_container)
                Thread {
                    var connection: HttpURLConnection? = null
                    try {
                        connection = URL(String.format("%s/api/getCourseData/%s", BuildConfig.SERVER_URL, gameCache.getString("loggedInUserIndex", "null")!!.replace("/", ""))).openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.setRequestProperty("Accept", "application/json;charset=UTF-8")
                        connection.setRequestProperty("Authorization", String.format("Basic %s", String(Base64.getEncoder().encode(BuildConfig.SERVER_CREDENTIALS.toByteArray(StandardCharsets.UTF_8)))))
                        connection.doInput = true
                        connection.connectTimeout = 1000
                        connection.connect()
                        if (connection.responseCode == 200) {
                            val input = BufferedReader(InputStreamReader(connection.inputStream))
                            coursesDataJson = input.lines().collect(Collectors.joining())
                            input.close()
                            serverInactive.text = ""
                        } else {
                            serverInactive.text = resources.getText(R.string.serverInactive)
                        }
                    } catch (e: Exception) {
                        serverInactive.text = resources.getText(R.string.serverInactive)
                        e.printStackTrace()
                    } finally {
                        connection?.disconnect()
                    }
                }.start()
                val windowMetrics = windowManager.currentWindowMetrics
                lateinit var json: JSONArray
                try {
                    json = JSONArray(coursesDataJson)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                linearLayout.removeAllViews()
                for (i in 0 until Objects.requireNonNull(json).length()) {
                    val singleClass = LinearLayout(this@MainActivity)
                    singleClass.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
                    singleClass.orientation = LinearLayout.HORIZONTAL
                    val sCCButton = ImageButton(this@MainActivity)
                    sCCButton.background = ContextCompat.getDrawable(applicationContext, R.mipmap.success_foreground)
                    sCCButton.scaleType = ImageView.ScaleType.CENTER_CROP
                    sCCButton.layoutParams = LinearLayout.LayoutParams((windowMetrics.bounds.width() * 0.09 * 2).toInt(), ceil(windowMetrics.bounds.height() * 0.09).toInt())
                    sCCButton.x = -25f
                    val sCText = TextView(this@MainActivity)
                    sCText.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    sCText.width = (windowMetrics.bounds.width() * 0.64).toInt()
                    try {
                        if (Locale.getDefault().displayLanguage == "српски" || Locale.getDefault().displayLanguage == "srpski") sCText.text = String.format("%s - %s", json.getJSONObject(i).getString("subject").split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0], json.getJSONObject(i).getString("subject").split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]) else sCText.text = String.format("%s - %s", json.getJSONObject(i).getString("subjectEnglish").split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0], json.getJSONObject(i).getString("subjectEnglish").split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])

                        val beginTime = DateFormat.format("dd.MM.yyyy HH:mm", SimpleDateFormat("E MM dd HH:mm:ss 'CEST' yyyy", Locale.getDefault()).parse(json.getJSONObject(i).getString("beginTime"))).toString()
                        val endTime = DateFormat.format("dd.MM.yyyy HH:mm", SimpleDateFormat("E MM dd HH:mm:ss 'CEST' yyyy", Locale.getDefault()).parse(json.getJSONObject(i).getString("endTime"))).toString()
                        sCText.text = String.format("%s\n%s\n(%s - %s)", sCText.text, json.getJSONObject(i).getString("nameSurname"), beginTime, endTime)
                        val sId = json.getJSONObject(i).getString("subjectId")
                        singleClass.id = json.getJSONObject(i).getString("subjectId").hashCode() * 21682
                        val isExercises = if (sCText.text.toString().contains("предавања") || sCText.text.toString().contains("lecture")) "0" else "1"
                        sCCButton.setOnClickListener {
                            Thread {
                                var buttonConnection: HttpURLConnection? = null
                                try {
                                    buttonConnection = URL(String.format("%s/api/recordAttendance/%s/%s/%s", BuildConfig.SERVER_URL, gameCache.getString("loggedInUserIndex", "null")!!.replace("/", ""), sId, isExercises.contains("1"))).openConnection() as HttpURLConnection
                                    buttonConnection.requestMethod = "GET"
                                    buttonConnection.setRequestProperty("Accept", "text/plain;charset=UTF-8")
                                    buttonConnection.setRequestProperty("Authorization", String.format("Basic %s", String(Base64.getEncoder().encode(BuildConfig.SERVER_CREDENTIALS.toByteArray(StandardCharsets.UTF_8)))))
                                    buttonConnection.doInput = true
                                    buttonConnection.connectTimeout = 1000
                                    buttonConnection.connect()
                                    println(buttonConnection.url)
                                    if (buttonConnection.responseCode == 200) {
                                        val buttonInput = BufferedReader(InputStreamReader(buttonConnection.inputStream))
                                        val response = buttonInput.readLine()
                                        runOnUiThread {
                                            if (response == "ALREADY RECORDED ATTENDANCE") {
                                                sCText.text = String.format("%s\n%s", sCText.text, resources.getString(R.string.alreadyRecordedAttendance))
                                            } else if (response == "SUCCESSFULLY RECORDED ATTENDANCE") {
                                                sCText.text = String.format("%s\n%s", sCText.text, resources.getString(R.string.newlyRecordedAttendance))
                                            }
                                            sCCButton.visibility = View.INVISIBLE
                                            serverInactive.text = ""
                                        }
                                    } else {
                                        runOnUiThread {
                                            val recordingAttendanceFailed = SweetAlertDialog(this@MainActivity, SweetAlertDialog.ERROR_TYPE)
                                            recordingAttendanceFailed
                                                    .setTitleText(R.string.recordAttendanceFailed)
                                                    .setContentText(resources.getString(R.string.recordAttendanceServerError))
                                                    .setConfirmText(resources.getString(R.string.ok))
                                                    .setConfirmClickListener { _: SweetAlertDialog? -> recordingAttendanceFailed.dismiss() }
                                                    .show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    serverInactive.text = resources.getText(R.string.serverInactive)
                                    runOnUiThread {
                                        val recordingAttendanceFailed = SweetAlertDialog(this@MainActivity, SweetAlertDialog.ERROR_TYPE)
                                        recordingAttendanceFailed
                                                .setTitleText(R.string.recordAttendanceFailed)
                                                .setContentText(resources.getString(R.string.recordAttendanceClientError))
                                                .setConfirmText(resources.getString(R.string.ok))
                                                .setConfirmClickListener { _: SweetAlertDialog? -> recordingAttendanceFailed.dismiss() }
                                                .show()
                                    }
                                    e.printStackTrace()
                                } finally {
                                    buttonConnection?.disconnect()
                                }
                            }.start()
                        }
                        singleClass.addView(sCText)
                        singleClass.addView(sCCButton)
                        linearLayout.addView(singleClass)
                    } catch (e: Exception) {
                        println("Error occurred while reading courses data JSON.")
                        e.printStackTrace()
                    }
                }
                Thread {
                    var connection: HttpURLConnection? = null
                    try {
                        connection = URL(String.format("%s/api/getAttendanceData/%s", BuildConfig.SERVER_URL, gameCache.getString("loggedInUserIndex", "null")!!.replace("/", ""))).openConnection() as HttpURLConnection
                        connection!!.requestMethod = "GET"
                        connection!!.setRequestProperty("Accept", "application/json;charset=UTF-8")
                        connection!!.setRequestProperty("Authorization", String.format("Basic %s", String(Base64.getEncoder().encode(BuildConfig.SERVER_CREDENTIALS.toByteArray(StandardCharsets.UTF_8)))))
                        connection!!.doInput = true
                        connection!!.connectTimeout = 1000
                        connection!!.connect()
                        if (connection!!.responseCode == 200) {
                            val input = BufferedReader(InputStreamReader(connection!!.inputStream))
                            attendanceDataJson = input.lines().collect(Collectors.joining())
                            input.close()
                            serverInactive.text = ""
                        } else {
                            serverInactive.text = resources.getText(R.string.serverInactive)
                        }
                    } catch (e: Exception) {
                        serverInactive.text = resources.getText(R.string.serverInactive)
                        e.printStackTrace()
                    } finally {
                        if (connection != null) connection!!.disconnect()
                    }
                }.start()
                try {
                    json = JSONArray(attendanceDataJson)
                    getAttendanceByCourseId(json, currentAttendanceClassID)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                handler.postDelayed(this, 10000)
            }
        }, 1000)
    }

    fun onLogout(v: View) {
        val confirmLogoutDialog = SweetAlertDialog(this@MainActivity, SweetAlertDialog.WARNING_TYPE)
        confirmLogoutDialog.setTitleText(R.string.confirmLogout)
        confirmLogoutDialog.contentText = resources.getString(R.string.wannaLogout)
        confirmLogoutDialog.confirmText = resources.getString(R.string.yes)
        confirmLogoutDialog.cancelText = resources.getString(R.string.no)
        confirmLogoutDialog.setConfirmClickListener { _: SweetAlertDialog? ->
            gameCache.edit().clear().apply()
            confirmLogoutDialog.dismiss()
            finish()
            startActivity(intent)
        }
        confirmLogoutDialog.show()
    }

    @SuppressLint("DefaultLocale")
    private fun getAttendanceByCourseId(json: JSONArray, i: Int) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val linearLayout2 = findViewById<LinearLayout>(R.id.hv_container)
        val singleClassAttendance = inflater.inflate(R.layout.single_lecture_template, linearLayout2, false)
        linearLayout2.removeAllViews()
        val attendancePie = singleClassAttendance.findViewById<AnimatedPieView>(R.id.attendancePie)
        val lectureText = singleClassAttendance.findViewById<TextView>(R.id.lecture_text)
        val infoText = singleClassAttendance.findViewById<TextView>(R.id.info_text)
        val detailText = singleClassAttendance.findViewById<TextView>(R.id.detail_text)
        val leftArrowButton = singleClassAttendance.findViewById<Button>(R.id.leftArrow_btn)
        val rightArrowButton = singleClassAttendance.findViewById<Button>(R.id.rightArrow_btn)
        if (i == 0) leftArrowButton.visibility = View.INVISIBLE
        if (i == json.length() - 1) rightArrowButton.visibility = View.INVISIBLE
        if (json.length() == 0) return
        try {
            val forecastAttendancePoints = ceil(10.0 / (json.getJSONObject(i).getString("totalLectures").toInt() + json.getJSONObject(i).getString("totalPractices").toInt())
                    * (json.getJSONObject(i).getString("attendedLectures").toInt() + json.getJSONObject(i).getString("attendedPractices").toInt())) // 10*ukupanBrojLekcija/brojPrisutnosti
            if (Locale.getDefault().displayLanguage == "српски" || Locale.getDefault().displayLanguage == "srpski") {
                lectureText.text = String.format("%s\n", json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getString("title"))
                infoText.text = String.format("Прогноза бодова за присуство: %d/10", forecastAttendancePoints.roundToInt())
                if (json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getString("isInactive") == "1") infoText.text = String.format("%s\n (КРАЈ НАСТАВЕ)", infoText.text)
            } else {
                lectureText.text = String.format("%s\n", json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getString("titleEnglish"))
                infoText.text = String.format("Forecast points for attendance: %d/10", forecastAttendancePoints.roundToInt())
                if (json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getString("isInactive") == "1") infoText.text = String.format("%s\n (LECTURES ARE OVER)", infoText.text)
            }
            if (json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getString("nameA") == "null") lectureText.text = String.format("%s%s", lectureText.text, json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getString("nameT")) else lectureText.text = String.format("%s%s\n (%s)", lectureText.text, json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getString("nameT"), json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getString("nameA"))
            val aL = json.getJSONObject(i).getInt("attendedLectures")
            val tL = json.getJSONObject(i).getInt("totalLectures")
            val aP = json.getJSONObject(i).getInt("attendedPractices")
            val tP = json.getJSONObject(i).getInt("totalPractices")
            attendancePie.applyConfig(AnimatedPieViewConfig().startAngle(-90f)
                    .addData(SimplePieInfo(aL.toDouble(), Color.GREEN, getString(R.string.descAL)))
                    .addData(SimplePieInfo((tL - aL).toDouble(), Color.RED, getString(R.string.descTL)))
                    .addData(SimplePieInfo(aP.toDouble(), Color.CYAN, getString(R.string.descAP)))
                    .addData(SimplePieInfo((tP - aP).toDouble(), Color.MAGENTA, getString(R.string.descTP)))
                    .selectListener { pieInfo: IPieInfo, isFloatUp: Boolean ->
                        var percentage = 0.0
                        var detail = ""
                        if (isFloatUp) {
                            when (pieInfo.color) {
                                Color.GREEN -> {
                                    percentage = aL.toDouble() / tL * 100.0
                                    detail = String.format("(%s/%s)", aL, tL)
                                    detailText.setTextColor(Color.GREEN)
                                }

                                Color.RED -> {
                                    percentage = (tL - aL).toDouble() / tL * 100.0
                                    detail = String.format("(%s/%s)", tL - aL, tL)
                                    detailText.setTextColor(Color.RED)
                                }

                                Color.CYAN -> {
                                    percentage = aP.toDouble() / tP * 100.0
                                    detail = String.format("(%s/%s)", aP, tP)
                                    detailText.setTextColor(Color.GREEN)
                                }

                                Color.MAGENTA -> {
                                    percentage = (tP - aP).toDouble() / tP * 100.0
                                    detail = String.format("(%s/%s)", tP - aP, tL)
                                    detailText.setTextColor(Color.RED)
                                }
                            }
                            detailText.text = String.format("%s%%\n%s", String.format("%.2f", percentage), detail)
                        } else {
                            detailText.text = ""
                        }
                    }
                    .duration(1000)
                    .drawText(true)
                    .pieRadius(160f)
                    .textSize(20f)
                    .textMargin(2)
                    .textGravity(AnimatedPieViewConfig.ABOVE)
                    .canTouch(true))
            attendancePie.start()
        } catch (e: JSONException) {
            System.out.printf("Error occurred while reading attendance data JSON for classID: %s\n", i)
            e.printStackTrace()
        }
        leftArrowButton.setOnClickListener {
            currentAttendanceClassID--
            rightArrowButton.visibility = View.VISIBLE
            getAttendanceByCourseId(json, currentAttendanceClassID)
        }
        rightArrowButton.setOnClickListener {
            currentAttendanceClassID++
            leftArrowButton.visibility = View.VISIBLE
            getAttendanceByCourseId(json, currentAttendanceClassID)
        }
        linearLayout2.addView(singleClassAttendance)
    }
}