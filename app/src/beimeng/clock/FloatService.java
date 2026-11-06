package beimeng.clock;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 悬浮时钟本体：可拖动、大小模式、样式实时生效。 */
public class FloatService extends Service {

    private static final int TICK_MS = 16;

    private WindowManager wm;
    private View root;
    private TextView time, ms;
    private WindowManager.LayoutParams lp;
    private final Handler h = new Handler(Looper.getMainLooper());
    private Prefs prefs;
    private boolean big;
    private final SimpleDateFormat hm = new SimpleDateFormat("HH:mm:ss", Locale.US);

    public static void start(Context c) {
        Intent i = new Intent(c, FloatService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) c.startForegroundService(i);
        else c.startService(i);
    }

    public static void stop(Context c) {
        c.stopService(new Intent(c, FloatService.class));
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new Prefs(this);
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        big = prefs.floatBig();
        show();
        h.post(tick);
    }

    private void show() {
        if (root != null) { wm.removeView(root); root = null; }

        root = LayoutInflater.from(new android.view.ContextThemeWrapper(
                this, R.style.Theme_Beimeng)).inflate(R.layout.float_clock, null);
        time = root.findViewById(R.id.float_time);
        ms = root.findViewById(R.id.float_ms);

        applyStyle();

        lp = new WindowManager.LayoutParams();
        lp.format = PixelFormat.TRANSLUCENT;
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        lp.type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.TOP | Gravity.START;

        int sw = getResources().getDisplayMetrics().widthPixels;
        int sh = getResources().getDisplayMetrics().heightPixels;
        lp.x = prefs.floatX() < 0 ? sw / 10 : Math.min(prefs.floatX(), sw - 60);
        lp.y = prefs.floatY() < 0 ? sh / 6 : Math.min(prefs.floatY(), sh - 120);

        bindDrag(root.findViewById(R.id.float_box));
        View close = root.findViewById(R.id.float_close);
        if (close != null) close.setOnClickListener(v -> stopSelf());
        root.findViewById(R.id.float_box).setOnClickListener(v -> {
            big = !big;
            prefs.putBoolean(Prefs.K_FLOAT_BIG, big);
            applyStyle();
        });

        try {
            wm.addView(root, lp);
            goForeground();
        } catch (Exception e) {
            stopSelf();
        }
    }

    /** 拖动 + 松手落位持久化；位移超过阈值不视为点击。 */
    @SuppressLint("ClickableViewAccessibility")
    private void bindDrag(View v) {
        v.setOnTouchListener(new View.OnTouchListener() {
            int sx, sy, stx, sty;
            boolean moved;

            @Override
            public boolean onTouch(View view, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        sx = (int) e.getRawX(); sy = (int) e.getRawY();
                        stx = lp.x; sty = lp.y; moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) e.getRawX() - sx;
                        int dy = (int) e.getRawY() - sy;
                        if (Math.abs(dx) > 6 || Math.abs(dy) > 6) moved = true;
                        if (moved) {
                            lp.x = stx + dx; lp.y = sty + dy;
                            wm.updateViewLayout(root, lp);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (moved) {
                            prefs.putInt(Prefs.K_FLOAT_X, lp.x);
                            prefs.putInt(Prefs.K_FLOAT_Y, lp.y);
                        } else {
                            view.performClick();
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    private void applyStyle() {
        View box = root.findViewById(R.id.float_box);
        box.setBackground(Style.background(prefs, this));
        Style.applyText(time, prefs);
        Style.applyText(ms, prefs);
        // 大模式：秒与毫秒同字号；小模式：毫秒缩小
        ms.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,
                big ? Style.textSizeSp(prefs) : Math.max(9f, Style.textSizeSp(prefs) - 6f));
        TextPaint tp = time.getPaint();
        tp.setFakeBoldText(big);
        time.invalidate();
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            long wall = prefs.displayNow();
            // 北京时间源：把 UTC 平移 +8 后用 UTC 格式化，不受设备时区影响
            if (Prefs.SRC_BEIJING.equals(prefs.source())) {
                hm.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
                wall = TimeSync.beijingWall(wall);
            } else {
                hm.setTimeZone(java.util.TimeZone.getDefault());
            }
            time.setText(hm.format(new Date(wall)));
            ms.setText("." + (Math.floorMod(wall, 1000L) / 100L));
            h.postDelayed(this, TICK_MS);
        }
    };

    private void goForeground() {
        String cid = "float";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel ch = new NotificationChannel(cid, getString(R.string.float_channel),
                    NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        Intent i = new Intent(this, MainActivity.class);
        int fl = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pi = PendingIntent.getActivity(this, 1, i, fl);
        Notification n;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            n = new Notification.Builder(this, cid)
                    .setContentTitle(getString(R.string.float_running))
                    .setSmallIcon(R.drawable.ic_stat_clock)
                    .setContentIntent(pi)
                    .setOngoing(true)
                    .build();
        } else {
            n = new Notification.Builder(this)
                    .setContentTitle(getString(R.string.float_running))
                    .setSmallIcon(R.drawable.ic_stat_clock)
                    .setContentIntent(pi)
                    .setOngoing(true)
                    .build();
        }
        startForeground(2, n);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("style")) {
            if (root != null) applyStyle();
        }
        return START_STICKY;
    }

    /** 设置页改完样式后实时刷新。 */
    public static void refresh(Context c) {
        Intent i = new Intent(c, FloatService.class);
        i.putExtra("style", true);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) c.startForegroundService(i);
            else c.startService(i);
        } catch (Exception ignored) { }
    }

    @Override
    public void onDestroy() {
        h.removeCallbacks(tick);
        if (root != null && wm != null) {
            try { wm.removeView(root); } catch (Exception ignored) { }
            root = null;
        }
        super.onDestroy();
    }
}
