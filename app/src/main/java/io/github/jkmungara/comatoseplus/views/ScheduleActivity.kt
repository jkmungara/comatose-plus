package io.github.jkmungara.comatoseplus.views

import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import io.github.jkmungara.comatoseplus.R
import io.github.jkmungara.comatoseplus.repositories.profiles.ProfileId
import io.github.jkmungara.comatoseplus.utils.ProfileNames
import io.github.jkmungara.comatoseplus.utils.Scheduler
import io.github.jkmungara.comatoseplus.utils.ScheduleSettings
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class ScheduleActivity : AppCompatActivity() {
    private lateinit var enableSwitch: SwitchMaterial
    private lateinit var status: TextView
    private lateinit var startButton: MaterialButton
    private lateinit var endButton: MaterialButton
    private lateinit var sleepSpinner: Spinner
    private lateinit var daySpinner: Spinner
    private lateinit var revertSpinner: Spinner

    private val profiles = ProfileId.values()
    private val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    /** Spinner callbacks fire while the screen is being set up; ignore them */
    private var loading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        enableSwitch = findViewById(R.id.schedule_switch)
        status = findViewById(R.id.schedule_status)
        startButton = findViewById(R.id.start_button)
        endButton = findViewById(R.id.end_button)
        sleepSpinner = findViewById(R.id.sleep_spinner)
        daySpinner = findViewById(R.id.day_spinner)
        revertSpinner = findViewById(R.id.revert_spinner)

        val profileNames = profiles.map { getString(ProfileNames.titleRes(it)) }
        sleepSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, profileNames)
        daySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, profileNames)

        val revertNames = ScheduleSettings.revertChoicesHours.map {
            if (it == 0) getString(R.string.revert_off) else getString(R.string.revert_hours, it)
        }
        revertSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, revertNames)

        loadState()
        loading = false

        enableSwitch.setOnCheckedChangeListener { _, checked ->
            if (loading) return@setOnCheckedChangeListener
            ScheduleSettings.setEnabled(this, checked)
            onScheduleChanged()
        }
        startButton.setOnClickListener {
            pickTime(ScheduleSettings.start(this)) {
                ScheduleSettings.setStart(this, it)
                onScheduleChanged()
            }
        }
        endButton.setOnClickListener {
            pickTime(ScheduleSettings.end(this)) {
                ScheduleSettings.setEnd(this, it)
                onScheduleChanged()
            }
        }
        sleepSpinner.onItemSelectedListener = selection { position ->
            if (profiles[position] == ScheduleSettings.sleepProfile(this)) return@selection
            ScheduleSettings.setSleepProfile(this, profiles[position])
            onScheduleChanged()
        }
        daySpinner.onItemSelectedListener = selection { position ->
            if (profiles[position] == ScheduleSettings.dayProfile(this)) return@selection
            ScheduleSettings.setDayProfile(this, profiles[position])
            onScheduleChanged()
        }
        revertSpinner.onItemSelectedListener = selection { position ->
            val hours = ScheduleSettings.revertChoicesHours[position]
            if (hours == ScheduleSettings.revertHours(this)) return@selection
            ScheduleSettings.setRevertHours(this, hours)
            refreshStatus()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadState() {
        enableSwitch.isChecked = ScheduleSettings.enabled(this)
        sleepSpinner.setSelection(ScheduleSettings.sleepProfile(this).ordinal)
        daySpinner.setSelection(ScheduleSettings.dayProfile(this).ordinal)
        revertSpinner.setSelection(
            ScheduleSettings.revertChoicesHours.indexOf(ScheduleSettings.revertHours(this)).coerceAtLeast(0)
        )
        refreshStatus()
    }

    /** Re-arm the alarms and, if the schedule is on, switch to the profile it wants right now */
    private fun onScheduleChanged() {
        Scheduler.ensureScheduled(this)
        refreshStatus()

        if (ScheduleSettings.enabled(this)) {
            val appContext = applicationContext
            lifecycleScope.launch {
                Scheduler.applyDesired(appContext)
                refreshStatus()
            }
        }
    }

    private fun refreshStatus() {
        startButton.text = ScheduleSettings.start(this).format(timeFormat)
        endButton.text = ScheduleSettings.end(this).format(timeFormat)

        val next = Scheduler.nextChange(this)
        val nextText = if (next == null)
            getString(R.string.schedule_status_off)
        else
            getString(
                R.string.schedule_status_next,
                getString(ProfileNames.titleRes(next.first)),
                next.second.toLocalTime().format(timeFormat)
            )

        val last = ScheduleSettings.lastRun(this)
        status.text = if (last == null) {
            nextText
        } else {
            val profile = getString(ProfileNames.titleRes(ProfileNames.fromName(last.profile, ProfileId.DEFAULT)))
            val at = DateUtils.formatDateTime(
                this, last.atMillis,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL
            )
            nextText + "\n\n" + getString(
                if (last.ok) R.string.schedule_last_ok else R.string.schedule_last_failed, profile, at
            )
        }
    }

    private fun pickTime(current: LocalTime, onPicked: (LocalTime) -> Unit) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(if (DateFormat.is24HourFormat(this)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .setHour(current.hour)
            .setMinute(current.minute)
            .build()
        picker.addOnPositiveButtonClickListener { onPicked(LocalTime.of(picker.hour, picker.minute)) }
        picker.show(supportFragmentManager, "time")
    }

    private fun selection(onSelected: (Int) -> Unit) = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (!loading) onSelected(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
    }
}
