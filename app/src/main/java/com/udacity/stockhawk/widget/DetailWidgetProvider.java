package com.udacity.stockhawk.widget;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.ui.DetailActivity;
import com.udacity.stockhawk.ui.MainActivity;
import com.udacity.stockhawk.utils.Constants;

/**
 * Created by manvi on 13/4/17.
 */

public class DetailWidgetProvider extends AppWidgetProvider {

    @SuppressLint("ObsoleteSdkInt")
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for(int appWidgetId: appWidgetIds)
        {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_detail);

            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,0);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
            {
                setRemoteAdapter(context, views);
            }else {
                setRemoteAdapterV11(context, views);
            }

            Intent clickIntentTemplate = new Intent(context, DetailActivity.class);
//            PendingIntent clickPendingIntent = PendingIntent.getBroadcast(context,0,
//                                            clickIntentTemplate,PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent clickPendingIntent = TaskStackBuilder.create(context)
                                                .addNextIntentWithParentStack(clickIntentTemplate)
                                                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setPendingIntentTemplate(R.id.widget_list,clickPendingIntent);
            views.setEmptyView(R.id.widget_list, R.id.widget_empty);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId,views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if(Constants.ACTION_DATA_UPDATED.equals(intent.getAction())){
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context,getClass()));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds,R.id.widget_list);
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setRemoteAdapter(Context context, RemoteViews remoteViews){
        Intent intent = new Intent(context, DetailWidgetRemoteViewService.class);
        remoteViews.setRemoteAdapter(R.id.widget_list, intent);
    }

    @SuppressWarnings("deprecation")
    private void setRemoteAdapterV11(Context context, RemoteViews remoteViews)
    {
        Intent intent = new Intent(context, DetailWidgetRemoteViewService.class);
        remoteViews.setRemoteAdapter(0,R.id.widget_list,intent);
    }
}
