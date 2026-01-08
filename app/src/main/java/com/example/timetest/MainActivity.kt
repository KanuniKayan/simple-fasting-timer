// A timer app, designed for intermittent fasting

package com.example.timetest

import android.content.ContentValues.TAG
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.timetest.ui.theme.TimeTestTheme
import kotlinx.coroutines.delay
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit


@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TimeTestTheme {
                Time()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Time() {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("info", Context.MODE_PRIVATE)

    // Get saved time
    var savedTime = getSavedTime(sharedPref)

    // Create state variable for updating UI
    var startTime by remember { mutableStateOf(savedTime) }

    // Check if clock is already activated
    var started : Boolean = getStarted(sharedPref)
    var state by remember { mutableStateOf(started)}

    // Initializing for Time Picker
    var goalTime : TimePickerState by remember {
        mutableStateOf(getGoalTime(sharedPref))
    }

    // Initializing for completion activation
    var completionCount by remember { mutableIntStateOf(0) }

    // Music Settings button
    var musicState by remember { mutableStateOf(getMusicState(sharedPref)) }
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)then(Modifier.padding(top = 36.dp))
    )
    {
        IconButton(
            onClick = {
                if (musicState)
                {
                    // Turn off
                    setMusicState(false, sharedPref)
                    musicState = false
                } else {
                    // Turn on
                    setMusicState(true, sharedPref)
                    musicState = true
                }
            },
            modifier = Modifier
                .align(Alignment.TopStart)
        )
        {
            if (musicState) // ON
            {
                Icon(painter = painterResource(id = R.drawable.music_on), contentDescription = "")
            } else // OFF
            {
                Icon(painter = painterResource(id = R.drawable.music_off), contentDescription = "",)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Start Clock button that refreshes this composable each click
        IconButton(onClick = {
            // Check if current state is turned on or off
            if (state)
            {
                // Turn off
                state = !state // Toggle on-state
                setStarted(false, sharedPref)
            }
            else
            {
                // Configure starting time
                startTime = LocalDateTime.now()
                setSavedTime(startTime, sharedPref)
                // Turn on
                state = !state // Toggle on-state
                setStarted(true, sharedPref)
            }
        },
            modifier = Modifier.size(200.dp)
        )
        {
            if (state) {
                // ON
                Icon(painter = painterResource(id = R.drawable.stop_button),
                    contentDescription = "",
                    tint = Color(0xFFFF6969))
            }
            else
            {
                // OFF
                Icon(painter = painterResource(id = R.drawable.play_button),
                    contentDescription = "",
                    tint = Color(0xFF6CE16E)
                )
            }
        }

        // Display passed time depending on state
        if (state) {
            TimePassed(startTime, state)
        }
        else
        {
            TimePassed(startTime, state)
        }

        /*
        Set required time
        Copied from: https://developer.android.com/develop/ui/compose/components/time-pickers#input
        */

        // Display goal time
        // If hour or minute is 0, change spelling
        if (goalTime.hour != 0 || goalTime.minute != 0)
        {
            val hours = when (goalTime.hour)
            {
                0 -> ""
                1 -> "1 hour"
                else -> "${goalTime.hour} hours"
            }

            val minutes = when (goalTime.minute)
            {
                0 -> ""
                1 -> "1 minute"
                else -> "${goalTime.minute} minutes"
            }

            val seperator = if (goalTime.hour != 0 && goalTime.minute != 0) " and " else ""
            val goalText = "Goal: $hours$seperator$minutes"

            Text(goalText)
        }

        // Toggle showing of time picker
        var showDail: Boolean by remember { mutableStateOf(false) }

        if (!showDail)
        Button(onClick = { showDail = true }, modifier = Modifier)
        {
            Text("Change Goal Time")
        }
        else
        {
            DialUseState(
                saved = getGoalTime(sharedPref),
                onConfirm = { time ->
                    goalTime = time
                    setGoalTime(goalTime.hour, goalTime.minute, goalTime.is24hour, sharedPref)
                    completionCount++
                    showDail = false
                }
            )
        }
        /* End of copy */
    }

    var difference by remember { mutableStateOf(Duration.ZERO) }

    var goal = Duration.ofHours(goalTime.hour.toLong()).plusMinutes(
        goalTime.minute.toLong())

    // Update goal and time passed difference every second
    LaunchedEffect(started)
    {
        while(true)
        {
            difference = Duration.between(startTime, LocalDateTime.now())
            delay(1000)
        }
    }

    if (goal <= difference && started == true) {
        Completion(completionCount)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimePassed(startTime : LocalDateTime, state : Boolean) {
    // Show or hide depending on on-state
    if (state) {
        // Set state-variable
        var timePassed by remember { mutableStateOf(Duration.ZERO) }

        // Calculate time passed every second and re-compile this Composable
        LaunchedEffect(state)
        {
            while (state) {
                timePassed = Duration.between(startTime, LocalDateTime.now())
                delay(1000)
            }
        }

        Text(
            "%02d:%02d:%02d".format(
                timePassed.toHours(), (timePassed.toMinutes() % 60), (timePassed.seconds % 60)
                )
            )

        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM")
        val formattedStartTime = startTime.format(formatter)

        Text("Started on: $formattedStartTime")
    }
}


// Copied and modified from:
// https://developer.android.com/develop/ui/compose/components/time-pickers#input
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialUseState(
    onConfirm: (TimePickerState) -> Unit,
    saved : TimePickerState
) {

    // Base TimePicker state
    val timePickerState = rememberTimePickerState(
        initialHour = saved.hour,
        initialMinute = saved.minute,
        is24Hour = true,
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TimeInput(
            state = timePickerState
        )
        Button(onClick = { onConfirm(
            TimePickerState(
                initialHour = timePickerState.hour,
                initialMinute = timePickerState.minute,
                is24Hour = timePickerState.is24hour
                    ))}
        )
        {
            Text("Confirm selection")
        }
    }
}
// End of copy


@Composable
fun Completion(count : Int)
{
    // Initialization for recomposition
    var state by remember { mutableStateOf(0) }
    state = count

    // Play music unless turned off
    PlayMusic(state)

    // Re-activate with each completion
    key(state)
    {
        Box(modifier = Modifier.fillMaxSize())
        {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(
                    Party(
                        angle = Angle.TOP,
                        speed = 50f,
                        maxSpeed = 100f,
                        spread = 55,
                        delay = 500,
                        position = Position.Relative(0.5,1.0),
                        emitter = Emitter(duration = 4, TimeUnit.SECONDS).max(300),
                    )
                )
            )
            Log.d(TAG, "Confetti activated")
        }
    }
}

@Composable
fun PlayMusic(state : Int)
{
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("info", Context.MODE_PRIVATE)
    if (sharedPref.getBoolean("music", true))
    {
        val mp: MediaPlayer = remember { MediaPlayer.create(context, R.raw.victory) }
        LaunchedEffect(state)
        {
            mp.start()
            delay(5000)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun getSavedTime(sharedPref : SharedPreferences) : LocalDateTime
{
    // Returns saved time or default
    var savedTime : LocalDateTime
    val str = sharedPref.getString("start_time", "")
    if (str == "")
        savedTime = LocalDateTime.now() // Default
    else
        savedTime = LocalDateTime.parse(str) // Parse saved

    return savedTime
}

@RequiresApi(Build.VERSION_CODES.O)
fun setSavedTime(time : LocalDateTime, sharedPref : SharedPreferences)
{
    sharedPref.edit().putString("start_time", time.toString()).apply()
}

fun getStarted(sharedPref : SharedPreferences) : Boolean {
    return sharedPref.getBoolean("started", false)
}

fun setStarted(value : Boolean, sharedPref : SharedPreferences) {
    sharedPref.edit().putBoolean("started", value).apply()
}

@OptIn(ExperimentalMaterial3Api::class)
fun getGoalTime(sharedPref : SharedPreferences) : TimePickerState
{
    val hour = sharedPref.getInt("goal_hour", 0)
    val minute = sharedPref.getInt("goal_minute", 0)
    val is24hours = sharedPref.getBoolean("goal_24", true)

    return TimePickerState(hour,minute, is24hours)
}

fun setGoalTime(hour : Int, minute : Int, is24hours : Boolean, sharedPref : SharedPreferences)
{
    sharedPref.edit().putInt("goal_hour", hour).apply()
    sharedPref.edit().putInt("goal_minute", minute).apply()
    sharedPref.edit().putBoolean("goal_24", is24hours).apply()
}

fun getMusicState(sharedPref : SharedPreferences) : Boolean
{
    return sharedPref.getBoolean("music", true)
}

fun setMusicState(state : Boolean, sharedPref : SharedPreferences)
{
    sharedPref.edit().putBoolean("music", state).apply()
}



@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TimePreview() {
    TimeTestTheme {
        Time()
    }
}