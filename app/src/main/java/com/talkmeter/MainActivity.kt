package com.talkmeter

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.MPPointF
import com.talkmeter.adapter.UsersAdapter
import com.talkmeter.databinding.ActivityMainBinding
import com.talkmeter.db.User
import com.talkmeter.ui.DialogCreateUser
import com.talkmeter.utils.MyApp
import kotlinx.coroutines.*
import java.lang.Runnable


class MainActivity : AppCompatActivity(), OnChartValueSelectedListener {
    lateinit var binding: ActivityMainBinding
    lateinit var prefs: SharedPreferences
    val leftRecyclerVisibility = MutableLiveData<Int>().apply {
        value = 0
    }
    val rightRecyclerVisibility = MutableLiveData<Int>().apply {
        value = 0
    }

    val timerHanler = Handler()
    val updateTalkedSecondsRunner = object : Runnable {
        override fun run() {
            timerHanler.postDelayed(this, 1000)
            GlobalScope.launch(Dispatchers.IO) {
                (application as MyApp).db?.userDao()?.updateTimers()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        timerHanler.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        super.onResume()
        timerHanler.postDelayed(updateTalkedSecondsRunner, 1000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        title = "PieChartActivity"


        val leftLayoutManager = LinearLayoutManager(this)
        val leftAdapter = UsersAdapter(this, 1)
        binding.leftRecyclerView.layoutManager = leftLayoutManager
        binding.leftRecyclerView.adapter = leftAdapter

        val rightLayoutManager = LinearLayoutManager(this)
        val rightAdapter = UsersAdapter(this, 2)
        binding.rightRecyclerView.layoutManager = rightLayoutManager
        binding.rightRecyclerView.adapter = rightAdapter

        leftRecyclerVisibility.observe(this, Observer {
            if (it == 1) {
                binding.leftRecyclerView.visibility = View.VISIBLE
                binding.leftSheetControlButton.setImageDrawable(getDrawable(R.drawable.ic_keyboard_arrow_right_black_24dp))
            } else {
                binding.leftRecyclerView.visibility = View.GONE
                binding.leftSheetControlButton.setImageDrawable(getDrawable(R.drawable.ic_keyboard_arrow_left_black_24dp))
            }
        })

        rightRecyclerVisibility.observe(this, Observer {
            if (it == 1) {
                binding.rightRecyclerView.visibility = View.VISIBLE
                binding.rightSheetControlButton.setImageDrawable(getDrawable(R.drawable.ic_keyboard_arrow_left_black_24dp))
            } else {
                binding.rightRecyclerView.visibility = View.GONE
                binding.rightSheetControlButton.setImageDrawable(getDrawable(R.drawable.ic_keyboard_arrow_right_black_24dp))
            }
        })

        binding.leftSheetControlButton.setOnClickListener {
            leftRecyclerVisibility.value = if (leftRecyclerVisibility.value == 1) 0 else 1
        }
        binding.rightSheetControlButton.setOnClickListener {
            rightRecyclerVisibility.value = if (rightRecyclerVisibility.value == 1) 0 else 1
        }

        (application as MyApp).db?.let {
            it.userDao().loadLeftUsers().observe(this, Observer { list ->
                //                Log.e("IGDSBGKHIKUEBVISBLVIUS", "loadLeftUsers updated: ${list}")
                leftAdapter.submitList(list)
            })

            it.userDao().loadRightUsers().observe(this, Observer { list ->
                //                Log.e("IGDSBGKHIKUEBVISBLVIUS", "loadRightUsers updated: ${list}")
                rightAdapter.submitList(list)
            })

            it.userDao().loadTimedUsers().observe(this, Observer { list ->
                setChartData(list)
            })
        }
        binding.chart1?.let { chart ->
            chart.setUsePercentValues(true)
            chart.description.isEnabled = false
            chart.setExtraOffsets(5f, 10f, 5f, 5f)

            chart.dragDecelerationFrictionCoef = 0.95f

            chart.centerText = generateCenterSpannableText()

            chart.isDrawHoleEnabled = true
            chart.setHoleColor(Color.WHITE)

            chart.setTransparentCircleColor(Color.WHITE)
            chart.setTransparentCircleAlpha(110)

            chart.holeRadius = 58f
            chart.transparentCircleRadius = 61f

            chart.setDrawCenterText(true)

            chart.rotationAngle = 0f
            // enable rotation of the chart by touch
            chart.isRotationEnabled = true
            chart.isHighlightPerTapEnabled = true

            // chart.setUnit(" €");
            // chart.setDrawUnitsInChart(true);

            // add a selection listener
            chart.setOnChartValueSelectedListener(this)

            chart.animateY(1400, Easing.EaseInOutQuad)
            // chart.spin(2000, 0, 360);

            val l = chart.legend
            l.verticalAlignment = Legend.LegendVerticalAlignment.TOP
            l.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            l.orientation = Legend.LegendOrientation.VERTICAL
            l.setDrawInside(false)
            l.xEntrySpace = 7f
            l.yEntrySpace = 0f
            l.yOffset = 0f

            // entry label styling
            chart.setEntryLabelColor(Color.WHITE)
            chart.setEntryLabelTextSize(12f)
        }
    }

    private fun setChartData(usersWithTalkedTime: List<User>) {
        val entries = ArrayList<PieEntry>()

        // NOTE: The order of the entries when being added to the entries array determines their position around the center of
        // the chart.
        for (user in usersWithTalkedTime) {
            entries.add(
                PieEntry(
                    user.spokeTime.toFloat(),
                    user.fullName
                )
            )
        }

        // add a lot of colors
        val colors = ArrayList<Int>()
        for (c in ColorTemplate.VORDIPLOM_COLORS)
            colors.add(c)
        for (c in ColorTemplate.JOYFUL_COLORS)
            colors.add(c)
        for (c in ColorTemplate.COLORFUL_COLORS)
            colors.add(c)
        for (c in ColorTemplate.LIBERTY_COLORS)
            colors.add(c)
        for (c in ColorTemplate.PASTEL_COLORS)
            colors.add(c)
        colors.add(ColorTemplate.getHoloBlue())

        val dataSet = PieDataSet(entries, "Spoken Users")

        dataSet.setDrawIcons(false)

        dataSet.sliceSpace = 3f
        dataSet.iconsOffset = MPPointF(0f, 40f)
        dataSet.selectionShift = 5f
        dataSet.colors = colors
        //dataSet.setSelectionShift(0f);

        binding.chart1?.let { chart ->
            val data = PieData(dataSet)
            data.setValueFormatter(PercentFormatter(chart))
            data.setValueTextSize(11f)
            data.setValueTextColor(Color.WHITE)
            chart.data = data

            // undo all highlights
            chart.highlightValues(null)

            chart.invalidate()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.pie, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.getItemId()) {
            R.id.clearUsers -> {
            }
            R.id.clearTimes -> {
            }
        }
        return true
    }

    private fun generateCenterSpannableText(): SpannableString {
        val s = SpannableString("TalkMeter\n by Ermek Kanybek uulu")
        s.setSpan(RelativeSizeSpan(1.7f), 0, 11, 0)
        s.setSpan(StyleSpan(Typeface.NORMAL), 11, s.length - 15, 0)
        s.setSpan(ForegroundColorSpan(Color.GRAY), 11, s.length - 15, 0)
        s.setSpan(RelativeSizeSpan(.8f), 11, s.length - 15, 0)
        s.setSpan(StyleSpan(Typeface.ITALIC), s.length - 11, s.length, 0)
        s.setSpan(ForegroundColorSpan(ColorTemplate.getHoloBlue()), s.length - 18, s.length, 0)
        return s
    }

    override fun onValueSelected(e: Entry?, h: Highlight) {

        if (e == null)
            return
        Log.i(
            "VAL SELECTED",
            "Value: " + e!!.getY() + ", index: " + h.getX()
                    + ", DataSet index: " + h.getDataSetIndex()
        )
    }

    override fun onNothingSelected() {
        Log.i("PieChart", "nothing selected")
    }

    fun createUser(categoryId: Int = 1) {
        var dialog: DialogCreateUser? = null
        val clickListener = View.OnClickListener { view ->
            dialog?.let {
                GlobalScope.launch(Dispatchers.IO) {
                    (application as MyApp).db?.userDao()
                        ?.insertAll(User(fullName = dialog!!.username, uCategory = categoryId))
                }
            }
            dialog?.dismiss()
        }
        dialog = DialogCreateUser(this, clickListener)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    fun selectUser(user: User?) {
//        Log.e("IGDSBGKHIKUEBVISBLVIUS", "user.uSelected: ${user?.uSelected}")
        user?.let {
            GlobalScope.launch(Dispatchers.IO) {
                user.uSelected = !user.uSelected
                (application as MyApp).db?.userDao()?.updateUserById(user)
            }
        }
    }

    fun opeationsWithUser(user: User) {
        val builder = AlertDialog.Builder(this)
            .setMessage(getString(R.string.operations_with_user_s, user.fullName))
            .setNegativeButton(R.string.delete) { _, _ ->
                GlobalScope.launch(Dispatchers.IO) {
                    (application as MyApp).db?.userDao()?.delete(user)
                }
            }
            .setNeutralButton(R.string.reset) { _, _ ->
                GlobalScope.launch(Dispatchers.IO) {
                    user.spokeTime = 0
                    user.uSelected = false
                    (application as MyApp).db?.userDao()?.updateUserById(user)
                }
            }
            .setPositiveButton(R.string.delete_all) { _, _ ->
                GlobalScope.launch(Dispatchers.IO) {
                    (application as MyApp).db?.userDao()?.clear()
                }
            }
        builder.create().show()
    }

}
