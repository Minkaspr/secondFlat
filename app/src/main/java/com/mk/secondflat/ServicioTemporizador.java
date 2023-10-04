package com.mk.secondflat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class ServicioTemporizador extends Service {
    private final IBinder vinculador = new VinculadorTemporizador();
    private Temporizador temporizador;
    private Handler manejador = new Handler(Looper.getMainLooper());
    private Runnable actualizadorInterfaz;

    @Override
    public IBinder onBind(Intent intent) {
        return vinculador;
    }

    public class VinculadorTemporizador extends Binder {
        ServicioTemporizador obtenerServicio() {
            return ServicioTemporizador.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        crearCanalNotificacion();
    }

    private void crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence nombre = "Canal Temporizador";
            String descripcion = "Canal para notificaciones del temporizador";
            int importancia = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel canal = new NotificationChannel("CANAL_TEMPORIZADOR", nombre, importancia);
            canal.setDescription(descripcion);
            NotificationManager manejadorNotificaciones = getSystemService(NotificationManager.class);
            manejadorNotificaciones.createNotificationChannel(canal);
        }
    }

    private void actualizarNotificacion() {
        Intent intentoParar = new Intent(this, ReceptorPararAlarma.class);
        PendingIntent accionParar;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            accionParar = PendingIntent.getBroadcast(this, 0, intentoParar, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            accionParar = PendingIntent.getBroadcast(this, 0, intentoParar, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        NotificationCompat.Builder constructorNotificacion = new NotificationCompat.Builder(this, "CANAL_TEMPORIZADOR")
                .setSmallIcon(R.drawable.ic_temporizador)
                .setContentTitle("Trabajo")
                .setContentText("Tiempo completado")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(R.drawable.ic_parar, "Parar", accionParar);

        NotificationManagerCompat manejadorNotificaciones = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        manejadorNotificaciones.notify(1, constructorNotificacion.build());
    }




    public void iniciarTemporizador(int segundos) {
        temporizador = new Temporizador(segundos * 1000L, 1000);
        temporizador.iniciarTemporizador();
        actualizadorInterfaz = new Runnable() {
            @Override
            public void run() {
                // Actualiza la notificación aquí
                actualizarNotificacion();
                manejador.postDelayed(this, 1000);
            }
        };
        manejador.post(actualizadorInterfaz);
    }

    public void pausarTemporizador() {
        if (temporizador != null) {
            temporizador.pausarTemporizador();
            manejador.removeCallbacks(actualizadorInterfaz);
        }
    }

    public void reanudarTemporizador() {
        if (temporizador != null) {
            temporizador.reanudarTemporizador();
            manejador.post(actualizadorInterfaz);
        }
    }

    public void pararTemporizador() {
        if (temporizador != null) {
            temporizador.pararTemporizador();
            manejador.removeCallbacks(actualizadorInterfaz);
        }
    }

}