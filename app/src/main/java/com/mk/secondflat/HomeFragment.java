package com.mk.secondflat;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.tabs.TabLayout;

import java.util.Locale;

public class HomeFragment extends Fragment {
    AppCompatTextView tiempo;
    TabLayout tabLayout;
    private CircularProgressIndicator barraProgresoCircular;
    private MaterialButton bParar, bIniciar, bPausar, bContinuar, bPararAlarma;
    private Temporizador temporizador;
    private boolean iniciarTemporizador = false;
    private final int tiempoTrabajo = 5, tiempoDescanso = 1;
    private MediaPlayer mediaPlayer;

    private final static String CHANNEL_ID = "TemporizadorServiceChannel";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        tabLayout = view.findViewById(R.id.tlOptionsTime);
        tiempo = view.findViewById(R.id.tvTime);
        barraProgresoCircular = view.findViewById(R.id.pbCircle);
        bParar = view.findViewById(R.id.btnStop);
        bIniciar = view.findViewById(R.id.btnPlay);
        bPausar = view.findViewById(R.id.btnPause);
        bContinuar = view.findViewById(R.id.btnContinue);
        bPararAlarma = view.findViewById(R.id.btnStopAlarm);
        mediaPlayer = MediaPlayer.create(getActivity(), R.raw.racing_into_the_night_yoasobi);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                iniciarTemporizador = false;
                if (position == 0) {
                    prepararTemporizador(tiempoTrabajo * 60);
                } else if (position == 1) {
                    prepararTemporizador(tiempoDescanso * 60);
                }
                bIniciar.setVisibility(View.VISIBLE);
                bPausar.setVisibility(View.GONE);
                bContinuar.setVisibility(View.GONE);
                bParar.setVisibility(View.GONE);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        prepararTemporizador(tiempoTrabajo * 60);
        bParar.setOnClickListener(v -> reiniciarTemporizadorYActualizarBotones());
        bIniciar.setOnClickListener(v -> {
            iniciarTemporizador = true;
            temporizador.iniciarTemporizador();
            bIniciar.setVisibility(View.GONE);
            bPausar.setVisibility(View.VISIBLE);
            bParar.setVisibility(View.VISIBLE);
        });
        bPausar.setOnClickListener(v -> {
            temporizador.pausarTemporizador();
            bPausar.setVisibility(View.GONE);
            bContinuar.setVisibility(View.VISIBLE);
        });
        bContinuar.setOnClickListener(v -> {
            temporizador.reanudarTemporizador();
            bContinuar.setVisibility(View.GONE);
            bPausar.setVisibility(View.VISIBLE);
        });
        bPararAlarma.setOnClickListener(v -> {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                mediaPlayer.seekTo(0);
            }
            mediaPlayer.setLooping(false);
            bPararAlarma.setVisibility(View.GONE);
            temporizador.reiniciarTemporizador(barraProgresoCircular, tiempo);
            bIniciar.setVisibility(View.VISIBLE);
        });
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            showNotification();
        } else {
            showNewNotificacion(CHANNEL_ID);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity activity = getActivity();
        if (activity != null) {
            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(activity.getApplicationContext());
            managerCompat.cancel(1);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void showNotification() {
        String CHANNEL_ID = "TemporizadorServiceChannel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Activity activity = getActivity();
            if (activity != null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Temporizador en ejecución", NotificationManager.IMPORTANCE_DEFAULT);
                NotificationManager manager = activity.getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            }
        }
        showNewNotificacion(CHANNEL_ID);
    }

    private void showNewNotificacion(String CHANNEL_ID) {
        Activity activity = getActivity();
        if (activity != null) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(activity.getApplicationContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_temporizador)
                    .setContentTitle("Temporizador en ejecución")
                    .setContentText("El temporizador está funcionando en segundo plano.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(activity.getApplicationContext());
            if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            managerCompat.notify(1, builder.build());
        }
    }

    private void prepararTemporizador(int segundos) {
        if (temporizador != null) {
            temporizador.destruirTemporizador();
        }
        barraProgresoCircular.setMax(segundos);
        temporizador = new Temporizador(segundos * 1000L, 1000);
        temporizador.setEscuchadorTick(millisUntilFinished -> {
            int segundosRestantes = (int) (millisUntilFinished / 1000f);
            tiempo.setText(String.format(Locale.getDefault(), "%02d:%02d", segundosRestantes / 60, segundosRestantes % 60));
            barraProgresoCircular.setProgress(segundosRestantes);
            System.out.println(segundosRestantes);
        });
        temporizador.setEscuchadorFinalizacion(() -> {
            tiempo.setText(R.string.time_zero);
            if (mediaPlayer != null) {
                mediaPlayer.start();
                mediaPlayer.setLooping(true);
            }
            bIniciar.setVisibility(View.GONE);
            bPausar.setVisibility(View.GONE);
            bContinuar.setVisibility(View.GONE);
            bParar.setVisibility(View.GONE);
            bPararAlarma.setVisibility(View.VISIBLE);
            // Emitir una nueva notificación
            showFinishedNotification(CHANNEL_ID);
        });
        int segundosRestantes = segundos;
        tiempo.setText(String.format(Locale.getDefault(), "%02d:%02d", segundosRestantes / 60, segundosRestantes % 60));
        barraProgresoCircular.setProgress(segundosRestantes);
        if (iniciarTemporizador) {
            temporizador.iniciarTemporizador();
        }
    }

    private void reiniciarTemporizadorYActualizarBotones() {
        temporizador.reiniciarTemporizador(barraProgresoCircular, tiempo);
        bIniciar.setVisibility(View.VISIBLE);
        bPausar.setVisibility(View.GONE);
        bContinuar.setVisibility(View.GONE);
        bParar.setVisibility(View.GONE);
    }

    private void showFinishedNotification(String CHANNEL_ID) {
        Activity activity = getActivity();
        if (activity != null) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(activity.getApplicationContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_temporizador)
                    .setContentTitle("Nombre de la aplicación")
                    .setContentText("Tiempo completado")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(activity.getApplicationContext());
            if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            managerCompat.notify(1, builder.build());
        }
    }
}