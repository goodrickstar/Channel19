package com.cb3g.channel19;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import com.example.android.multidex.myapplication.R;

public class Channel19 extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        final SharedPreferences widget = context.getSharedPreferences("widget", Context.MODE_PRIVATE);
        if (widget.getBoolean("available", false)) {
            for (int id : appWidgetIds) {
                RemoteViews view = new RemoteViews("com.cb3g.channel19", R.layout.channel19);
                PendingIntent openNine = PendingIntent.getActivity(context, 0, new Intent(context, LoginActivity.class), PendingIntent.FLAG_IMMUTABLE);
                PendingIntent muteNine = PendingIntent.getBroadcast(context, 0, new Intent("muteChannelNineTeen"), PendingIntent.FLAG_IMMUTABLE);
                view.setOnClickPendingIntent(R.id.wBox, openNine);
                view.setOnClickPendingIntent(R.id.muting, muteNine);
                view.setTextViewText(R.id.wmain, widget.getString("handle", ""));
                view.setTextViewText(R.id.wcarrier, widget.getString("carrier", "Keep The Shiny Side Up Driver"));
                view.setTextViewText(R.id.wstatus, widget.getString("status", "Offline"));
                view.setTextViewText(R.id.wchannel, widget.getString("channel", "Truck Radio USA"));
                view.setTextViewText(R.id.wtitle, widget.getString("title", ""));
                appWidgetManager.updateAppWidget(id, view);
            }
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        SharedPreferences sharedPreferences = context.getSharedPreferences("widget", Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean("available", true).apply();
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        SharedPreferences sharedPreferences = context.getSharedPreferences("widget", Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean("available", false).apply();
    }
}
