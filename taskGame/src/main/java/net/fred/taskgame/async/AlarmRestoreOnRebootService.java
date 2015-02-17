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

package net.fred.taskgame.async;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import net.fred.taskgame.activity.BaseActivity;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.receiver.AlarmReceiver;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.DbHelper;

import java.util.List;

public class AlarmRestoreOnRebootService extends Service {

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {


		Context mContext = getApplicationContext();

//		PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
//		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Constants.TAG);
//		// Acquire the lock
//		wl.acquire();

		// Refresh widgets data
		BaseActivity.notifyAppWidgets(mContext);

		// Retrieves all tasks with reminder set
		try {
			List<Task> tasks = DbHelper.getTasksWithReminder(true);

			for (Task task : tasks) {
				setAlarm(mContext, task);
			}
		}


		// Release the lock
		finally {
//			wl.release();
		}

		return Service.START_NOT_STICKY;
	}

	private void setAlarm(Context ctx, Task task) {
		Intent intent = new Intent(ctx, AlarmReceiver.class);
		intent.putExtra(Constants.INTENT_NOTE, task);
		PendingIntent sender = PendingIntent.getBroadcast(ctx, Constants.INTENT_ALARM_CODE, intent,
				PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager am = (AlarmManager) ctx.getSystemService(ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, Long.parseLong(task.getAlarm()), sender);
	}

}
