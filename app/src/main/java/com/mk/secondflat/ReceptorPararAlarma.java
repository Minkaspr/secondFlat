package com.mk.secondflat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class ReceptorPararAlarma extends BroadcastReceiver {
    public static final String ACCION_PARAR_ALARMA = "ACCION_PARAR_ALARMA";
    @Override
    public void onReceive(Context context, Intent intent) {
        // Detén la alarma aquí
        Intent intentPararAlarma = new Intent(ACCION_PARAR_ALARMA);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intentPararAlarma);
    }
}