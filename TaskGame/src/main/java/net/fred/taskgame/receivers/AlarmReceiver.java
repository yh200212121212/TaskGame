/*
 * Copyright (C) 2015 Federico Iosue (federico.iosue@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.fred.taskgame.receivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import net.fred.taskgame.R;
import net.fred.taskgame.activities.SnoozeActivity;
import net.fred.taskgame.models.Task;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.Dog;
import net.fred.taskgame.utils.NotificationsHelper;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.utils.date.DateHelper;

import org.parceler.Parcels;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Task task = Parcels.unwrap(intent.getExtras().getParcelable(Constants.INTENT_TASK));
            createNotification(context, task);
        } catch (Exception e) {
            Dog.e("Error while creating reminder notification", e);
        }
    }

    private void createNotification(Context context, Task task) {
        // Prepare text contents
        String title = task.title.length() > 0 ? task.title : task.content;
        String alarmText = DateHelper.getDateTimeShort(context, task.alarmDate);
        String text = task.title.length() > 0 && task.content.length() > 0 ? task.content : alarmText;

        Intent snoozeIntent = new Intent(context, SnoozeActivity.class);
        snoozeIntent.setAction(Constants.ACTION_SNOOZE);
        snoozeIntent.putExtra(Constants.INTENT_TASK, Parcels.wrap(task));
        snoozeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent piSnooze = PendingIntent.getActivity(context, 0, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent postponeIntent = new Intent(context, SnoozeActivity.class);
        postponeIntent.setAction(Constants.ACTION_POSTPONE);
        postponeIntent.putExtra(Constants.INTENT_TASK, Parcels.wrap(task));
        snoozeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent piPostpone = PendingIntent.getActivity(context, 0, postponeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        String snoozeDelay = PrefUtils.getString(PrefUtils.PREF_SETTINGS_NOTIFICATION_SNOOZE_DELAY, "10");

        // Next create the bundle and initialize it
        Intent intent = new Intent(context, SnoozeActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.INTENT_TASK, Parcels.wrap(task));
        intent.putExtras(bundle);

        // Sets the Activity to start in a new, empty task
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Workaround to fix problems with multiple notifications
        intent.setAction(Constants.ACTION_NOTIFICATION_CLICK + Long.toString(System.currentTimeMillis()));

        // Creates the PendingIntent
        PendingIntent notifyIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationsHelper notificationsHelper = new NotificationsHelper(context)
                .createNotification(R.drawable.ic_assignment_white_24dp, title, notifyIntent)
                .setLargeIcon(R.mipmap.ic_launcher)
                .setMessage(text);

        notificationsHelper.getBuilder()
                .addAction(R.drawable.ic_snooze_reminder, context.getString(R.string.snooze, Long.valueOf(snoozeDelay)), piSnooze)
                .addAction(R.drawable.ic_reminder, context.getString(R.string.set_reminder), piPostpone);

        // Ringtone options
        String ringtone = PrefUtils.getString(PrefUtils.PREF_SETTINGS_NOTIFICATION_RINGTONE, null);
        if (ringtone != null) {
            notificationsHelper.setRingtone(ringtone);
        }

        // Vibration options
        long[] pattern = {500, 500};
        if (PrefUtils.getBoolean(PrefUtils.PREF_SETTINGS_NOTIFICATION_VIBRATION, true))
            notificationsHelper.setVibration(pattern);

        notificationsHelper.show(task.id);
    }
}
