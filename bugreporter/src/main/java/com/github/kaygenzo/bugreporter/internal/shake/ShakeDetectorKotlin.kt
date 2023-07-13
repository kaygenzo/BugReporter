package com.github.kaygenzo.bugreporter.internal.shake

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.github.kaygenzo.bugreporter.api.UnsupportedSensorException
import java.util.*

internal data class SensorBundle(val mXAcc: Float, val mYAcc: Float, val mZAcc: Float, val mTimestamp: Long)

/**
 * Interface definition for a callback to be invoked when the device has been shaken.
 */
internal interface OnShakeListener {
    /**
     * Called when the device has been shaken.
     */
    fun onShake()
}

/**
 * Based on ShakeDetector library https://github.com/tbouron/ShakeDetector
 */

/**
 * Helper class for easy shake detection of a Android device. It provides a easy way to detect a
 * shake movement using the build-in accelerometer and fire a callback on the UI thread every times
 * it happens.
 *
 * <p>The API is designed to follow closely an {@link Activity} or a {@link Fragment} lifecycle.
 *
 * <h3>Usage</h3>
 *
 * <p>Below is an example usage within an {@link Activity}:
 *
 * <pre>
 * public class myActivity extends Activity {
 *     {@literal @}Override
 *     protected void onCreate(Bundle savedInstanceState) {
 *         super.onCreate(savedInstanceState);
 *         setContentView(R.layout.my_activity);
 *
 *         ShakeDetector.create(this, new OnShakeListener() {
 *             {@literal @}Override
 *             public void OnShake() {
 *                 Toast.makeText(getApplicationContext(), "Device shaken!", Toast.LENGTH_SHORT).show();
 *             }
 *         });
 *     }
 *
 *     {@literal @}Override
 *     protected void onResume() {
 *         super.onResume();
 *         ShakeDetector.start();
 *     }
 *
 *     {@literal @}Override
 *     protected void onStop() {
 *         super.onStop();
 *         ShakeDetector.stop();
 *     }
 *
 *     {@literal @}Override
 *     protected void onDestroy() {
 *         super.onDestroy();
 *         ShakeDetector.destroy();
 *     }
 * }
 * </pre>
 *
 * <p>First, we need to initialize the shake detector with our custom {@link OnShakeListener} callback.
 * This is done by calling the {@link ShakeDetector#create(Context, OnShakeListener)} method.
 * Do not assume that the detector is created and started as this point. You should instead test the
 * returned value.
 *
 * <p>Second, we need to stop the listener when the {@link Activity} goes to the background. This is
 * done be calling the {@link ShakeDetector#stop()} method. Of course, you need to restart it when it
 * comes back to the foreground by calling the {@link ShakeDetector#start()} method
 *
 * <p>Finally, we need to destroy the listener once the {@link Activity} gets destroyed. This is done
 * by calling the {@link ShakeDetector#destroy()} method.
 *
 * <h3>Additional notes</h3>
 *
 * By default, the shake detector comes with a standard configuration for the {@link OnShakeListener#OnShake}
 * callback to be triggered. You can override this configuration by calling the {@link ShakeDetector#updateConfiguration(float, int)}
 * method.
 */
internal class ShakeDetectorKotlin
private constructor(
    private var mThresholdAcceleration: Float = DEFAULT_THRESHOLD_ACCELERATION,
    private var mThresholdShakeNumber: Float = DEFAULT_THRESHOLD_SHAKE_NUMBER,
    private val mListener: OnShakeListener,
    private val sensorManager: SensorManager,
    private val sensor: Sensor
): SensorEventListener {

    private val mSensorBundles: MutableList<SensorBundle> = Collections.synchronizedList(mutableListOf())

    companion object {
        private const val DEFAULT_THRESHOLD_ACCELERATION = 2.0f
        private const val DEFAULT_THRESHOLD_SHAKE_NUMBER = 3f
        private const val INTERVAL = 200

        /**
         * Creates a shake detector and starts listening for device shakes. Neither {@code context} nor
         * {@code listener} can be null. In that case, a {@link IllegalArgumentException} will be thrown.
         *
         * @param context The current Android context.
         * @param listener The callback triggered when the device is shaken.
         * @return true if the shake detector has been created and started correctly, false otherwise.
         */
        @Throws(UnsupportedSensorException::class)
        fun create(context: Context, listener: OnShakeListener): ShakeDetectorKotlin {
            (context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager)?.let {  sensorManager ->
                val defaultSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                defaultSensor?.let { sensor ->
                    return ShakeDetectorKotlin(mListener = listener, sensorManager = sensorManager, sensor = sensor)
                } ?: throw UnsupportedSensorException()

            } ?: throw UnsupportedSensorException()
        }
    }

    /**
     * Starts a previously created shake detector. If no detector has been created before, the method
     * won't create one and will return false.
     *
     * @return true if the shake detector has been started correctly, false otherwise.
     */
    fun start(): Boolean {
        return sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    /**
     * Stops a previously created shake detector. If no detector has been created before, the method
     * will do anything.
     */
    fun stop() {
        sensorManager.unregisterListener(this)
    }

    /**
     * You can update the configuration of the shake detector based on your usage but the default settings
     * should work for the majority of cases. It uses [ShakeDetector.DEFAULT_THRESHOLD_ACCELERATION]
     * for the sensibility and [ShakeDetector.DEFAULT_THRESHOLD_SHAKE_NUMBER] for the number
     * of shake required.
     *
     * @param sensibility The sensibility, in G, is the minimum acceleration need to be considered
     * as a shake. The higher number you go, the harder you have to shake your
     * device to trigger a shake.
     * @param shakeNumber The number of shake (roughly) required to trigger a shake.
     */
    fun updateConfiguration(sensibility: Float, shakeNumber: Int) {
        setConfiguration(sensibility, shakeNumber)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val bundle = SensorBundle(it.values[0], it.values[1], it.values[2], it.timestamp)
            synchronized(mSensorBundles) {
                when {
                    mSensorBundles.isEmpty() -> {
                        mSensorBundles.add(bundle)
                    }
                    bundle.mTimestamp - mSensorBundles.last().mTimestamp > INTERVAL -> {
                        mSensorBundles.add(bundle)
                    }
                    else -> {

                    }
                }
                performCheck()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // The accuracy is not likely to change on a real device. Just ignore it.
    }

    private fun performCheck() {
        val vector = intArrayOf(0, 0, 0)
        val matrix = arrayOf(intArrayOf(0, 0), intArrayOf(0, 0), intArrayOf(0, 0))
        for (sensorBundle in mSensorBundles) {
            if (sensorBundle.mXAcc > mThresholdAcceleration && vector[0] < 1) {
                vector[0] = 1
                matrix[0][0]++
            }
            if (sensorBundle.mXAcc < -mThresholdAcceleration && vector[0] > -1) {
                vector[0] = -1
                matrix[0][1]++
            }
            if (sensorBundle.mYAcc > mThresholdAcceleration && vector[1] < 1) {
                vector[1] = 1
                matrix[1][0]++
            }
            if (sensorBundle.mYAcc < -mThresholdAcceleration && vector[1] > -1) {
                vector[1] = -1
                matrix[1][1]++
            }
            if (sensorBundle.mZAcc > mThresholdAcceleration && vector[2] < 1) {
                vector[2] = 1
                matrix[2][0]++
            }
            if (sensorBundle.mZAcc < -mThresholdAcceleration && vector[2] > -1) {
                vector[2] = -1
                matrix[2][1]++
            }
        }
        for (axis in matrix) {
            for (direction in axis) {
                if (direction < mThresholdShakeNumber) {
                    return
                }
            }
        }
        mSensorBundles.clear()
        mListener.onShake()
    }

    private fun setConfiguration(sensibility: Float, shakeNumber: Int) {
        mThresholdAcceleration = sensibility
        mThresholdShakeNumber = shakeNumber.toFloat()
        synchronized(mSensorBundles) {
            mSensorBundles.clear()
        }
    }
}