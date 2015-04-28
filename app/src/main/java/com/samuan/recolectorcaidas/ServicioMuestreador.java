package com.samuan.recolectorcaidas;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

/**
 * Servicio encargado de escuchar el sensor acelerómetro y procesar los datos
 *
 * Created by SAMUAN on 13/04/2015.
 */
public class ServicioMuestreador extends Service implements SensorEventListener {

    private Monitor monitor;
    private WakeLock wakeLock;
    private PowerManager mgr;
    private HandlerThread handlerThread;
    private Handler handler;
    private SensorManager sensor;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("ServicioMuestreador","creando");

        mgr = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
        monitor=new Monitor(getResources());

        sensor =(SensorManager) getSystemService(SENSOR_SERVICE);

        handlerThread=new HandlerThread("hilosensor");
        handlerThread.start();
        handler=new Handler(handlerThread.getLooper());
    }

    /**
     * Activa el servicio. Iniciar la captación de datos del sensor acelerómetro y arranca un
     * nuevo hilo donde se recibirán los eventos del sensor.
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
        wakeLock.acquire();
        monitor=new Monitor(getResources());
        sensor.registerListener(ServicioMuestreador.this, sensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),  20000, handler);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handlerThread.quit();
        sensor.unregisterListener(this);
        wakeLock.release();
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        monitor.gestionar(event);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {   }


}
