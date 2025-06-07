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
import androidx.core.content.edit
import androidx.core.os.ConfigurationCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import cn.pedant.SweetAlert.SweetAlertDialog
import com.razerdp.widget.animatedpieview.AnimatedPieView
import com.razerdp.widget.animatedpieview.AnimatedPieViewConfig
import com.razerdp.widget.animatedpieview.data.IPieInfo
import com.razerdp.widget.animatedpieview.data.SimplePieInfo
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.round

private const val X_TENANT_ID = "X-Tenant-ID"
private const val PIE_CHART_GRAPH_FORMAT = "(%.0f/%.0f)"

class MainActivity : AppCompatActivity() {
    companion object {
        lateinit var sharedPreferences: SharedPreferences
        var csrfTokenManager: CsrfTokenManager = CsrfTokenManager(
            serverUrl = BuildConfig.SERVER_URL,
            credentials = Base64.getEncoder()
                .encodeToString(BuildConfig.SERVER_CREDENTIALS.toByteArray()),
            proxyIdentifier = "",
            sessionData = CsrfSession("", "", "", "")
        )
    }

    private lateinit var disabledGrayOut: View
    private lateinit var coursesData: JSONArray
    private lateinit var attendancesData: JSONArray
    private lateinit var serverInactive: TextView
    private lateinit var loggedInAs: TextView
    private lateinit var logout: Button
    private var currentAttendanceClassID = 0
    private val client = OkHttpClient()

    private var loginActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            disabledGrayOut.visibility = View.GONE
            logout.visibility = View.VISIBLE

            retrieveStudentName()
        }
    }

    private fun retrieveStudentName() {
        client.newCall(
            Request.Builder()
                .url(
                    "${BuildConfig.SERVER_URL}/api/getStudentName/${
                        sharedPreferences.getString(
                            "loggedInStudentIndex",
                            "null"
                        )!!.replace("/", "")
                    }"
                )
                .get()
                .addHeader(
                    "Authorization",
                    "Basic ${
                        Base64.getEncoder().encodeToString(
                            BuildConfig.SERVER_CREDENTIALS.toByteArray(StandardCharsets.UTF_8)
                        )
                    }"
                )
                .addHeader("Accept", "text/plain;charset=UTF-8")
                .addHeader(X_TENANT_ID, csrfTokenManager.proxyIdentifier)
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
                sharedPreferences.edit { putString("loggedInStudentName", "") }
                serverInactive.text = resources.getText(R.string.serverInactive)
            }

            @SuppressLint("SetTextI18n")
            override fun onResponse(call: Call, response: Response) {
                when (response.code) {
                    200 -> {
                        runOnUiThread {
                            sharedPreferences.edit {
                                putString(
                                    "loggedInStudentName",
                                    response.body?.string()
                                )
                            }
                            loggedInAs.text = "${sharedPreferences.getString("loggedInStudentName", "null")}\n(${sharedPreferences.getString("loggedInStudentIndex", "null")})"
                            serverInactive.text = ""
                            startDataStreaming()
                        }
                    }

                    else -> {
                        runOnUiThread {
                            sharedPreferences.edit {
                                putString(
                                    "loggedInStudentName",
                                    "-SERVER ERROR-"
                                )
                            }
                            loggedInAs.text = "${sharedPreferences.getString("loggedInStudentName", "null")}\n(${sharedPreferences.getString("loggedInStudentIndex", "null")})"
                            serverInactive.text = ""
                        }
                    }
                }
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = EncryptedSharedPreferences.create(
            "SingiAttend-SharedPreferences",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        setContentView(R.layout.activity_main)
        loggedInAs = findViewById(R.id.loggedInAs_text)
        disabledGrayOut = findViewById(R.id.disabledGrayOut)
        logout = findViewById(R.id.logout_btn)
        serverInactive = findViewById(R.id.serverInactive_text)

        logout.visibility = View.INVISIBLE
        logout.setOnClickListener {
            val confirmLogoutDialog = SweetAlertDialog(this@MainActivity, SweetAlertDialog.WARNING_TYPE).setTitleText(R.string.confirmLogout)
            confirmLogoutDialog
            .setContentText(resources.getString(R.string.wannaLogout))
            .setConfirmText(resources.getString(R.string.yes))
            .setCancelText(resources.getString(R.string.no))
            .setConfirmClickListener { _: SweetAlertDialog? ->
                runOnUiThread {
                    sharedPreferences.edit { clear() }
                    logout.visibility = View.INVISIBLE
                    loggedInAs.text = ""
                    confirmLogoutDialog.dismiss()
                    csrfTokenManager.logoutFromCsrfSession()
                    loginActivityResultLauncher.launch(Intent(this@MainActivity, LoginActivity::class.java))
                }
            }.show()
        }

        if (sharedPreferences.getString(
                "loggedInStudentIndex",
                null
            ) == null
        ) loginActivityResultLauncher.launch(
            Intent(
                this@MainActivity,
                LoginActivity::class.java
            )
        ) else {
            disabledGrayOut.visibility = View.GONE
            logout.visibility = View.VISIBLE

            if(csrfTokenManager.proxyIdentifier.isEmpty()){
                val proxyIdentifier = sharedPreferences.getString("loggedInStudentProxyIdentifier", null)

                if(proxyIdentifier == null) {
                    sharedPreferences.edit { clear() }
                    loginActivityResultLauncher.launch(Intent(this@MainActivity, LoginActivity::class.java))
                    return
                } else csrfTokenManager.proxyIdentifier = proxyIdentifier
            }

            csrfTokenManager.fetchCsrfSession { success ->
                if (!success) {
                    runOnUiThread {
                        val failedDialog =
                            SweetAlertDialog(this@MainActivity, SweetAlertDialog.ERROR_TYPE)
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
                            SweetAlertDialog(this@MainActivity, SweetAlertDialog.ERROR_TYPE)
                        failedDialog
                            .setTitleText(R.string.regTitleFailed)
                            .setContentText(resources.getString(R.string.serverInactive))
                            .setConfirmText(resources.getString(R.string.ok))
                            .setConfirmClickListener { _: SweetAlertDialog? -> failedDialog.dismiss() }
                            .show()
                        sharedPreferences.edit { clear() }
                        loginActivityResultLauncher.launch(Intent(this@MainActivity, LoginActivity::class.java))
                    }

                    return@fetchCsrfSession
                }

                runOnUiThread {
                    retrieveStudentName()
                }
            }
        }
    }

    private fun startDataStreaming() {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            @SuppressLint("SetTextI18n")
            override fun run() {
                val linearLayout = findViewById<LinearLayout>(R.id.sv_container)

                client.newCall(Request.Builder()
                    .url(
                        "${BuildConfig.SERVER_URL}/api/getCourseData/${
                            sharedPreferences.getString(
                                "loggedInStudentIndex",
                                "null"
                            )!!.replace("/", "")
                        }"
                    )
                    .get()
                    .addHeader(
                        "Authorization",
                        "Basic ${
                            Base64.getEncoder().encodeToString(
                                BuildConfig.SERVER_CREDENTIALS.toByteArray(StandardCharsets.UTF_8)
                            )
                        }"
                    )
                    .addHeader("Accept", "application/json;charset=UTF-8")
                    .addHeader(X_TENANT_ID, csrfTokenManager.proxyIdentifier)
                    .addHeader(
                        csrfTokenManager.sessionData.csrfHeaderName,
                        csrfTokenManager.sessionData.csrfTokenSecret
                    )
                    .addHeader(
                        "Cookie",
                        "JSESSIONID=${csrfTokenManager.sessionData.jsessionId}; XSRF-TOKEN=${csrfTokenManager.sessionData.xsrfToken}"
                    )
                    .build()).enqueue(object: Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        serverInactive.text = resources.getText(R.string.serverInactive)
                        e.printStackTrace()
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.code == 200)
                            runOnUiThread {
                                coursesData = try {
                                    JSONArray(response.body?.string())
                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                    JSONArray()
                                }
                                serverInactive.text = ""

                                val windowMetrics = windowManager.currentWindowMetrics

                                linearLayout.removeAllViews()

                                for (i in 0 until coursesData.length()) { //For-loop range must have an 'iterator()' method
                                    val courseData = coursesData.getJSONObject(i)
                                    val singleClass = LinearLayout(this@MainActivity)
                                    singleClass.layoutParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.MATCH_PARENT
                                    )
                                    singleClass.orientation = LinearLayout.HORIZONTAL
                                    val sCCButton = ImageButton(this@MainActivity)
                                    sCCButton.background =
                                        ContextCompat.getDrawable(
                                            applicationContext,
                                            R.mipmap.success_foreground
                                        )
                                    sCCButton.scaleType = ImageView.ScaleType.CENTER_CROP
                                    sCCButton.layoutParams = LinearLayout.LayoutParams(
                                        (windowMetrics.bounds.width() * 0.09 * 2).toInt(),
                                        ceil(windowMetrics.bounds.height() * 0.09).toInt()
                                    )
                                    sCCButton.x = -25f
                                    val sCText = TextView(this@MainActivity)
                                    sCText.textAlignment = View.TEXT_ALIGNMENT_CENTER
                                    sCText.width = (windowMetrics.bounds.width() * 0.64).toInt()
                                    try {
                                        sCText.text =
                                            "${
                                                courseData.getString(
                                                    if (ConfigurationCompat.getLocales(resources.configuration)[0]?.language?.contains(
                                                            "sr"
                                                        ) == true
                                                    ) "subject" else "subjectEnglish"
                                                )
                                                    .split("-").map { it.trim() }
                                                    .getOrElse(0) { "" }
                                            } - ${
                                                courseData.getString(
                                                    if (ConfigurationCompat.getLocales(resources.configuration)[0]?.language?.contains(
                                                            "sr"
                                                        ) == true
                                                    ) "subject" else "subjectEnglish"
                                                )
                                                    .split("-").map { it.trim() }
                                                    .getOrElse(1) { "" }
                                            }"

                                        sCText.text =
                                            "${sCText.text}\n${courseData.getString("nameSurname")}\n(" +
                                                    "${
                                                        DateFormat.format(
                                                            "dd.MM.yyyy HH:mm",
                                                            SimpleDateFormat(
                                                                "E MM dd HH:mm:ss 'CEST' yyyy",
                                                                Locale.getDefault()
                                                            ).parse(courseData.getString("beginTime"))
                                                        )
                                                    } - " +
                                                    "${
                                                        DateFormat.format(
                                                            "dd.MM.yyyy HH:mm",
                                                            SimpleDateFormat(
                                                                "E MM dd HH:mm:ss 'CEST' yyyy",
                                                                Locale.getDefault()
                                                            ).parse(courseData.getString("endTime"))
                                                        )
                                                    })"

                                        singleClass.id = courseData.hashCode()

                                        val isExercises =
                                            if (sCText.text.contains("предавања") || sCText.text.contains(
                                                    "lecture"
                                                )
                                            ) "0" else "1"

                                        sCCButton.setOnClickListener {
                                            client.newCall(
                                                Request.Builder()
                                                    .url(
                                                        "${BuildConfig.SERVER_URL}//api/recordAttendance/${
                                                            sharedPreferences.getString(
                                                                "loggedInStudentIndex",
                                                                "null"
                                                            )!!.replace("/", "")
                                                        }/${courseData.getString("subjectId")}/${
                                                            isExercises.contains(
                                                                "1"
                                                            )
                                                        }"
                                                    )
                                                    .get()
                                                    .addHeader(
                                                        "Authorization",
                                                        "Basic ${
                                                            Base64.getEncoder().encodeToString(
                                                                BuildConfig.SERVER_CREDENTIALS.toByteArray(
                                                                    StandardCharsets.UTF_8
                                                                )
                                                            )
                                                        }"
                                                    )
                                                    .addHeader("Accept", "text/plain;charset=UTF-8")
                                                    .addHeader(
                                                        X_TENANT_ID,
                                                        csrfTokenManager.proxyIdentifier
                                                    )
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
                                                    serverInactive.text =
                                                        resources.getText(R.string.serverInactive)
                                                    runOnUiThread {
                                                        val recordingAttendanceFailed =
                                                            SweetAlertDialog(
                                                                this@MainActivity,
                                                                SweetAlertDialog.ERROR_TYPE
                                                            )
                                                        recordingAttendanceFailed
                                                            .setTitleText(R.string.recordAttendanceFailed)
                                                            .setContentText(resources.getString(R.string.recordAttendanceClientError))
                                                            .setConfirmText(resources.getString(R.string.ok))
                                                            .setConfirmClickListener { _: SweetAlertDialog? -> recordingAttendanceFailed.dismiss() }
                                                            .show()
                                                    }
                                                    e.printStackTrace()
                                                }

                                                override fun onResponse(
                                                    call: Call,
                                                    response: Response
                                                ) {
                                                    if (response.code == 200) {
                                                        runOnUiThread {
                                                            if (response.body?.string() == "ALREADY RECORDED ATTENDANCE")
                                                                sCText.text =
                                                                    "${sCText.text}\n${
                                                                        resources.getString(
                                                                            R.string.alreadyRecordedAttendance
                                                                        )
                                                                    }"
                                                            else if (response.body?.string() == "SUCCESSFULLY RECORDED ATTENDANCE")
                                                                sCText.text =
                                                                    "${sCText.text}\n${
                                                                        resources.getString(
                                                                            R.string.newlyRecordedAttendance
                                                                        )
                                                                    }"
                                                            sCCButton.visibility = View.INVISIBLE
                                                            serverInactive.text = ""
                                                            singleClass.addView(sCText)
                                                            singleClass.addView(sCCButton)
                                                            linearLayout.addView(singleClass)
                                                        }
                                                    } else {
                                                        runOnUiThread {
                                                            val recordingAttendanceFailed =
                                                                SweetAlertDialog(
                                                                    this@MainActivity,
                                                                    SweetAlertDialog.ERROR_TYPE
                                                                )
                                                            recordingAttendanceFailed
                                                                .setTitleText(R.string.recordAttendanceFailed)
                                                                .setContentText(
                                                                    resources.getString(
                                                                        R.string.recordAttendanceServerError
                                                                    )
                                                                )
                                                                .setConfirmText(
                                                                    resources.getString(
                                                                        R.string.ok
                                                                    )
                                                                )
                                                                .setConfirmClickListener { _: SweetAlertDialog? -> recordingAttendanceFailed.dismiss() }
                                                                .show()
                                                        }
                                                    }
                                                }

                                            })
                                        }
                                    } catch (e: Exception) {
                                        println("Error occurred while reading courses data JSON.")
                                        e.printStackTrace()
                                    }
                                }
                            }
                        else serverInactive.text = resources.getText(R.string.serverInactive)
                    }
                })

                client.newCall(Request.Builder()
                    .url(
                        "${BuildConfig.SERVER_URL}/api/getAttendanceData/${
                            sharedPreferences.getString(
                                "loggedInStudentIndex",
                                "null"
                            )!!.replace("/", "")
                        }"
                    )
                    .get()
                    .addHeader(
                        "Authorization",
                        "Basic ${
                            Base64.getEncoder().encodeToString(
                                BuildConfig.SERVER_CREDENTIALS.toByteArray(StandardCharsets.UTF_8)
                            )
                        }"
                    )
                    .addHeader("Accept", "application/json;charset=UTF-8")
                    .addHeader(X_TENANT_ID, csrfTokenManager.proxyIdentifier)
                    .addHeader(
                        csrfTokenManager.sessionData.csrfHeaderName,
                        csrfTokenManager.sessionData.csrfTokenSecret
                    )
                    .addHeader(
                        "Cookie",
                        "JSESSIONID=${csrfTokenManager.sessionData.jsessionId}; XSRF-TOKEN=${csrfTokenManager.sessionData.xsrfToken}"
                    )
                    .build()).enqueue(object: Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        serverInactive.text = resources.getText(R.string.serverInactive)
                        e.printStackTrace()
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.code == 200)
                            runOnUiThread {
                                attendancesData = try {
                                    JSONArray(response.body?.string())
                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                    JSONArray()
                                }
                                serverInactive.text = ""
                                runOnUiThread {
                                    getAttendanceByCourseId(
                                        if (attendancesData.length() > 0) attendancesData.getJSONObject(
                                            0
                                        ) else null
                                    )
                                }
                            }
                        else serverInactive.text = resources.getText(R.string.serverInactive)
                    }
                })

                handler.postDelayed(this, 1000 * 60 * 60) //...and after that repeat retrieval every hour
            }
        }, 1000) //First time get data instantly...
    }

    @SuppressLint("SetTextI18n")
    private fun getAttendanceByCourseId(attendanceData: JSONObject?) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val linearLayout2 = findViewById<LinearLayout>(R.id.hv_container)
        val singleClassAttendance =
            inflater.inflate(R.layout.single_lecture_template, linearLayout2, false)
        linearLayout2.removeAllViews()
        val attendancePie = singleClassAttendance.findViewById<AnimatedPieView>(R.id.attendancePie)
        val lectureText = singleClassAttendance.findViewById<TextView>(R.id.lecture_text)
        val infoText = singleClassAttendance.findViewById<TextView>(R.id.info_text)
        val detailText = singleClassAttendance.findViewById<TextView>(R.id.detail_text)
        val leftArrowButton = singleClassAttendance.findViewById<Button>(R.id.leftArrow_btn)
        val rightArrowButton = singleClassAttendance.findViewById<Button>(R.id.rightArrow_btn)

        lectureText.setTextColor(Color.GRAY)
        infoText.setTextColor(Color.GRAY)
        leftArrowButton.setTextColor(Color.GRAY)
        rightArrowButton.setTextColor(Color.GRAY)

        leftArrowButton.setOnClickListener {
            rightArrowButton.visibility = View.VISIBLE
            attendancesData.getJSONObject(--currentAttendanceClassID)
        }
        rightArrowButton.setOnClickListener {
            leftArrowButton.visibility = View.VISIBLE
            attendancesData.getJSONObject(++currentAttendanceClassID)
        }

        if (currentAttendanceClassID == 0) leftArrowButton.visibility = View.INVISIBLE
        if (currentAttendanceClassID == attendancesData.length()) rightArrowButton.visibility = View.INVISIBLE
        if (attendanceData != null) {
            try {
                val attendedLectures = attendanceData.getDouble("attendedLectures")
                val totalLectures = attendanceData.getDouble("totalLectures")
                val attendedPractices = attendanceData.getDouble("attendedPractices")
                val totalPractices = attendanceData.getDouble("totalPractices")

                val forecastAttendancePoints =
                    if ((totalLectures + totalPractices) == 0.0 || (attendedLectures + attendedPractices) == 0.0) 0.0 else round(
                        (attendedLectures + attendedPractices) / (totalLectures + totalPractices) * 10.0
                    )

                val attendanceInstance =
                    attendanceData.getJSONObject("attendanceSubobjectInstance")
                val locale = ConfigurationCompat.getLocales(resources.configuration)[0]!!
                val titleKey = if (locale.language.contains("sr")) "title" else "titleEnglish"

                lectureText.text = attendanceInstance.getString(titleKey)
                    .replaceFirstChar { it.titlecase(locale) } + "\n" +
                        attendanceInstance.getString("nameT") +
                        attendanceInstance.getString("nameA").takeIf { it.isNotEmpty() }
                            ?.let { "\n ($it)" }.orEmpty()

                infoText.text = String.format(
                    resources.getString(R.string.forecastAttendancePoints),
                    forecastAttendancePoints
                ) +
                        attendanceInstance.getString("isInactive").takeIf { it == "1" }
                            ?.let { "\n(${resources.getString(R.string.classOver)})" }.orEmpty()

                attendancePie.applyConfig(
                    AnimatedPieViewConfig().startAngle(-90f)
                        .addData(
                            SimplePieInfo(
                                attendedLectures,
                                Color.GREEN,
                                getString(R.string.descAL)
                            )
                        )
                        .addData(
                            SimplePieInfo(
                                totalLectures - attendedLectures,
                                Color.RED,
                                getString(R.string.descTL)
                            )
                        )
                        .addData(
                            SimplePieInfo(
                                attendedPractices,
                                Color.CYAN,
                                getString(R.string.descAP)
                            )
                        )
                        .addData(
                            SimplePieInfo(
                                totalPractices - attendedPractices,
                                Color.MAGENTA,
                                getString(R.string.descTP)
                            )
                        )
                        .selectListener { pieInfo: IPieInfo, isFloatUp: Boolean ->
                            var percentage = 0.0
                            var detail = ""
                            if (isFloatUp) {
                                when (pieInfo.color) {
                                    Color.GREEN -> {
                                        percentage = attendedLectures / totalLectures * 100.0
                                        detail = String.format(
                                            Locale.getDefault(),
                                            PIE_CHART_GRAPH_FORMAT,
                                            attendedLectures,
                                            totalLectures
                                        )
                                        detailText.setTextColor(Color.GREEN)
                                    }

                                    Color.RED -> {
                                        percentage =
                                            (totalLectures - attendedLectures) / totalLectures * 100.0
                                        detail = String.format(
                                            Locale.getDefault(),
                                            PIE_CHART_GRAPH_FORMAT,
                                            totalLectures - attendedLectures,
                                            totalLectures
                                        )
                                        detailText.setTextColor(Color.RED)
                                    }

                                    Color.CYAN -> {
                                        percentage = attendedPractices / totalPractices * 100.0
                                        detail = String.format(
                                            Locale.getDefault(),
                                            PIE_CHART_GRAPH_FORMAT,
                                            attendedPractices,
                                            totalPractices
                                        )
                                        detailText.setTextColor(Color.CYAN)
                                    }

                                    Color.MAGENTA -> {
                                        percentage =
                                            (totalPractices - attendedPractices) / totalPractices * 100.0
                                        detail = String.format(
                                            Locale.getDefault(),
                                            PIE_CHART_GRAPH_FORMAT,
                                            totalPractices - attendedPractices,
                                            totalPractices
                                        )
                                        detailText.setTextColor(Color.MAGENTA)
                                    }
                                }
                                detailText.text = String.format("%.0f%%\n${detail}", percentage)
                            } else {
                                detailText.text = ""
                            }
                        }
                        .duration(1000)
                        .drawText(true)
                        .pieRadius(160f)
                        .textSize(24f)
                        .textMargin(2)
                        .textGravity(AnimatedPieViewConfig.ABOVE)
                        .canTouch(true))
                attendancePie.start()
            } catch (e: JSONException) {
                System.err.println("Error occurred while reading attendanceData[${currentAttendanceClassID}]")
                e.printStackTrace()
            }
        }

        linearLayout2.addView(singleClassAttendance)
    }
}