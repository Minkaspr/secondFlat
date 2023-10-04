package com.mk.secondflat;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.IBinder;
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
    private int tiempoTrabajo = 5, tiempoDescanso = 1;

    private ServicioTemporizador servicioTemporizador;
    private boolean vinculado = false;
    //-------
    private BroadcastReceiver receptorPararAlarma;
    private MediaPlayer mediaPlayer;
    private ServiceConnection conexion = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ServicioTemporizador.VinculadorTemporizador vinculador = (ServicioTemporizador.VinculadorTemporizador) service;
            servicioTemporizador = vinculador.obtenerServicio();
            vinculado = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            vinculado = false;
        }
    };
    @Override
    public void onStart() {
        super.onStart();
        Intent intento = new Intent(getActivity(), ServicioTemporizador.class);
        getActivity().bindService(intento, conexion, Context.BIND_AUTO_CREATE);
        //-------
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receptorPararAlarma, new IntentFilter(ReceptorPararAlarma.ACCION_PARAR_ALARMA));
    }

    @Override
    public void onStop() {
        super.onStop();
        if (vinculado) {
            getActivity().unbindService(conexion);
            vinculado = false;
        }
        //----
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receptorPararAlarma);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        tabLayout = view.findViewById(R.id.tlOptionsTime);
        tiempo = view.findViewById(R.id.tvTime);
        barraProgresoCircular = view.findViewById(R.id.pbCircle);
        bParar = view.findViewById(R.id.btnStop);
        bIniciar = view.findViewById(R.id.btnPlay);
        bPausar = view.findViewById(R.id.btnPause);
        bContinuar = view.findViewById(R.id.btnContinue);
        bPararAlarma = view.findViewById(R.id.btnStopAlarm);
        Uri alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        mediaPlayer = MediaPlayer.create(getActivity(), alarm);

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
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
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
            // Detiene el sonido de la alarma.
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                mediaPlayer.seekTo(0);
            }
            // Desactiva la repetición del MediaPlayer.
            mediaPlayer.setLooping(false);
            // Oculta el botón para detener la alarma.
            bPararAlarma.setVisibility(View.GONE);
            temporizador.reiniciarTemporizador(barraProgresoCircular, tiempo);
            bIniciar.setVisibility(View.VISIBLE);
        });
        //----------
        receptorPararAlarma = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Detén la alarma aquí
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    mediaPlayer.seekTo(0);
                }
                // Desactiva la repetición del MediaPlayer.
                mediaPlayer.setLooping(false);
                // Oculta el botón para detener la alarma.
                bPararAlarma.setVisibility(View.GONE);
            }
        };
        return view;
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
            tiempo.setText("00:00");
            // Reproduce el sonido de la alarma.
            mediaPlayer.start();
            // Configura el MediaPlayer para que se repita.
            mediaPlayer.setLooping(true);
            // Oculta los otros botones.
            bIniciar.setVisibility(View.GONE);
            bPausar.setVisibility(View.GONE);
            bContinuar.setVisibility(View.GONE);
            bParar.setVisibility(View.GONE);
            // Muestra el botón para detener la alarma.
            bPararAlarma.setVisibility(View.VISIBLE);
        });
        // Mostramos el tiempo inicial sin iniciar el temporizador
        int segundosRestantes = segundos;
        tiempo.setText(String.format(Locale.getDefault(), "%02d:%02d", segundosRestantes / 60, segundosRestantes % 60));
        barraProgresoCircular.setProgress(segundosRestantes);
        // Iniciamos el temporizador solo si iniciarTemporizador es verdadero
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
}