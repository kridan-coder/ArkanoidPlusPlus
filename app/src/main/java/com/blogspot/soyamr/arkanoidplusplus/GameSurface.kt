package com.blogspot.soyamr.arkanoidplusplus

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.blogspot.soyamr.arkanoidplusplus.model.IModel
import com.blogspot.soyamr.arkanoidplusplus.model.Model
import com.blogspot.soyamr.arkanoidplusplus.model.game_elements.State


enum class PaddleControlMode {
    TOUCH, GYROSCOPE
}

@SuppressLint("ViewConstructor")
class GameSurface(
    context: Context,
    private val phoneScreenHeight: Int,
    private val phoneScreenWidth: Int
) :
    SurfaceView(context),
    SurfaceHolder.Callback, Controller, IGameSurface, SensorEventListener {


    private lateinit var gameThread: GameThread
    private val model: IModel
    private var controlMode = PaddleControlMode.GYROSCOPE

    private val sManager: SensorManager
    private val accelerometer: Sensor?

    init {
        // Make Game Surface focusable so it can handle events.
        this.isFocusable = true

        this.holder.addCallback(this)

        model = Model(context, this)

        //get sensors
        sManager = context.getSystemService(Context.SENSOR_SERVICE) as (SensorManager)
        if (sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometer = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            accelerometer = null
        }

    }


    override fun update(fps: Int) {
        model.update(fps)
    }

    override fun drawScene(canvas: Canvas) {

        canvas.save()
        canvas.drawColor(Color.WHITE)
        model.draw(canvas)
        canvas.restore()

    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        resume()
        setWillNotDraw(false)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pause()
    }

    fun pause() {
        var retry = true
        sManager.unregisterListener(this);
        while (retry) {
            try {
                gameThread.setRunning(false)
                gameThread.join()
                retry = false
            } catch (e: Exception) {
                e.stackTrace
            }
        }
    }

    fun resume() {
        sManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        gameThread = GameThread(this)
        gameThread.setRunning(true)
        gameThread.start()
    }

    override fun getScreenWidth(): Int {
        return phoneScreenWidth
    }

    override fun getScreenHeight(): Int {
        return phoneScreenHeight
    }

    override fun setPaused(paused: Boolean) {
        gameThread.paused = paused
    }

    override fun startScoreActivity(score: Int) {
        val intent = Intent(context, ScoreActivity::class.java).apply {
            putExtra(ScoreActivity.SCORE, score)
        }
        gameThread.setRunning(false)
        context.startActivity(intent)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        setPaused(false)
        if (controlMode == PaddleControlMode.TOUCH) {
            when (motionEvent.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    if (motionEvent.x > getScreenWidth() / 2) {
                        model.setMovementState(State.RIGHT)
                    } else {
                        model.setMovementState(State.LEFT)
                    }
                }
                MotionEvent.ACTION_UP -> model.setMovementState(State.STOPPED)
            }
        }
        return true
    }


    override fun onSensorChanged(sensorEvent: SensorEvent) {
        if (controlMode == PaddleControlMode.GYROSCOPE) {
            if (sensorEvent.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = sensorEvent.values[0].toInt()
                when {
                    x > 0 -> {
                        model.setMovementState(State.LEFT)
                    }
                    x < 0 -> {
                        model.setMovementState(State.RIGHT)
                    }
                    else -> {
                        model.setMovementState(State.STOPPED)
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, i: Int) {}
}

