package beimeng.clock;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

import java.util.Calendar;
import java.util.Locale;

/** 定时提醒 + 倒计时提醒的排程与触发。 */
public class RemindReceiver extends BroadcastReceiver {

    public static final String ACT_DAILY = "beimeng.clock.DAILY";
    public static final String ACT_COUNTDOWN = "beimeng.clock.COUNTDOWN";

    @Override
    public void onReceive(Context c, Intent i) {
        String act = i == null ? null : i.getAction();
        if (ACT_COUNTDOWN.equals(act)) {
            Remind.notifyCountdown(c);
            return;
        }
        Remind.notifyDaily(c);
        // 单次提醒触发后关掉开关，重复提醒保持开启
        Prefs p = new Prefs(c);
        if (!p.getBoolean(Prefs.K_DAILY_REPEAT, true)) {
            p.putBoolean(Prefs.K_DAILY_ON, false);
            Remind.cancelDaily(c);
        }
    }

    /** 排程工具，供 RemindActivity 与自身复用。 */
    public static final class Remind {

        public static final String CH_DAILY = "remind";
        public static final String CH_COUNTDOWN = "countdown";

        private static final int REQ_DAILY = 1001;
        private static final int REQ_COUNTDOWN = 1002;
        private static final int NID_DAILY = 11;
        private static final int NID_COUNTDOWN = 12;

        public static void ensureChannels(Context c) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel(CH_DAILY,
                    c.getString(R.string.remind_channel), NotificationManager.IMPORTANCE_HIGH));
            nm.createNotificationChannel(new NotificationChannel(CH_COUNTDOWN,
                    c.getString(R.string.countdown_channel), NotificationManager.IMPORTANCE_HIGH));
        }

        // ---- 定时提醒 ----

        /** 返回下一次触发的毫秒时间戳，便于界面回显。 */
        public static long scheduleDaily(Context c) {
            Prefs p = new Prefs(c);
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, p.getInt(Prefs.K_DAILY_H, 8));
            cal.set(Calendar.MINUTE, p.getInt(Prefs.K_DAILY_M, 0));
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pi = dailyIntent(c);
            am.cancel(pi);
            if (p.getBoolean(Prefs.K_DAILY_REPEAT, true)) {
                am.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            }
            return cal.getTimeInMillis();
        }

        public static void cancelDaily(Context c) {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            am.cancel(dailyIntent(c));
        }

        private static PendingIntent dailyIntent(Context c) {
            Intent i = new Intent(c, RemindReceiver.class).setAction(ACT_DAILY);
            return PendingIntent.getBroadcast(c, REQ_DAILY, i, flags(PendingIntent.FLAG_UPDATE_CURRENT));
        }

        static void notifyDaily(Context c) {
            ensureChannels(c);
            Prefs p = new Prefs(c);
            String t = String.format(Locale.US, "%02d:%02d",
                    p.getInt(Prefs.K_DAILY_H, 8), p.getInt(Prefs.K_DAILY_M, 0));
            post(c, CH_DAILY, NID_DAILY, c.getString(R.string.remind_text), t);
        }

        // ---- 倒计时提醒 ----

        public static void startCountdown(Context c, int minutes) {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            long at = SystemClock.elapsedRealtime() + minutes * 60_000L;
            PendingIntent pi = countdownIntent(c);
            am.cancel(pi);
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi);
        }

        public static void cancelCountdown(Context c) {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            am.cancel(countdownIntent(c));
        }

        private static PendingIntent countdownIntent(Context c) {
            Intent i = new Intent(c, RemindReceiver.class).setAction(ACT_COUNTDOWN);
            return PendingIntent.getBroadcast(c, REQ_COUNTDOWN, i,
                    flags(PendingIntent.FLAG_UPDATE_CURRENT));
        }

        static void notifyCountdown(Context c) {
            ensureChannels(c);
            post(c, CH_COUNTDOWN, NID_COUNTDOWN, c.getString(R.string.countdown_done), null);
        }

        // ---- 公共 ----

        private static int flags(int extra) {
            int f = extra;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) f |= PendingIntent.FLAG_IMMUTABLE;
            return f;
        }

        private static void post(Context c, String cid, int id, String title, String sub) {
            PendingIntent pi = PendingIntent.getActivity(c, id,
                    new Intent(c, MainActivity.class), flags(PendingIntent.FLAG_UPDATE_CURRENT));
            Notification n;
            Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? new Notification.Builder(c, cid)
                    : new Notification.Builder(c);
            b.setContentTitle(title)
                    .setSmallIcon(R.drawable.ic_stat_clock)
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .setDefaults(Notification.DEFAULT_ALL);
            if (sub != null) b.setContentText(sub);
            n = b.build();
            ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, n);
        }

        private Remind() { }
    }
}
