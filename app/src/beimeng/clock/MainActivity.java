package beimeng.clock;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends Activity {

    private static final int TICK_MS = 32;

    private Prefs prefs;
    private TextView time, ms, date, calibState, adjValue, senseValue, remindState, toggle;
    private Switch swSmart;
    private LinearLayout sourceGroup;
    private View sourceHead, sourceArrow;
    private TextView sourceSummary;

    private final android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            render();
            h.postDelayed(this, TICK_MS);
        }
    };
    private final SimpleDateFormat hms = new SimpleDateFormat("HH:mm:ss", Locale.US);
    private final SimpleDateFormat dfull = new SimpleDateFormat("yyyy\u5E74M\u6708d\u65E5 EEEE", Locale.CHINA);
    private boolean syncing;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        setContentView(R.layout.activity_main);

        time = findViewById(R.id.main_time);
        ms = findViewById(R.id.main_ms);
        date = findViewById(R.id.main_date);
        calibState = findViewById(R.id.calib_state);
        adjValue = findViewById(R.id.adj_value);
        senseValue = findViewById(R.id.sense_value);
        remindState = findViewById(R.id.remind_state);
        toggle = findViewById(R.id.toggle_float);
        sourceGroup = findViewById(R.id.source_group);
        sourceHead = findViewById(R.id.row_source_head);
        sourceSummary = findViewById(R.id.source_summary);
        sourceArrow = findViewById(R.id.source_arrow);
        swSmart = findViewById(R.id.sw_smart);

        sourceHead.setOnClickListener(v -> toggleSources());
        buildSources();

        toggle.setOnClickListener(v -> onToggleFloat());
        findViewById(R.id.row_sense).setOnClickListener(v -> editSense());
        findViewById(R.id.row_settings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.row_remind).setOnClickListener(v ->
                startActivity(new Intent(this, RemindActivity.class)));
        findViewById(R.id.row_smart).setOnClickListener(v ->
                swSmart.setChecked(!swSmart.isChecked()));
        findViewById(R.id.adj_minus).setOnClickListener(v -> nudge(-10));
        findViewById(R.id.adj_plus).setOnClickListener(v -> nudge(10));
        adjValue.setOnClickListener(v -> editManual());

        swSmart.setChecked(prefs.smart());
        swSmart.setOnCheckedChangeListener((v, on) -> prefs.putBoolean(Prefs.K_SMART, on));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshChips();
        adjValue.setText(String.valueOf(prefs.manualMs()));
        senseValue.setText(prefs.senseMs() + " ms");
        swSmart.setChecked(prefs.smart());
        refreshToggle();
        refreshRemindState();
        render();
        h.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        h.removeCallbacks(tick);
    }

    // ---------------- 闁哄啫鐖煎Λ鍨攦?----------------

    private boolean expanded() { return prefs.getBoolean(Prefs.K_SRC_EXPANDED, false); }

    private void toggleSources() {
        prefs.putBoolean(Prefs.K_SRC_EXPANDED, !expanded());
        buildSources();
    }

    /** 闁硅埖锚瑜版棃寮捄鍝勬锭闁?QUICK 濞戞挶鍊濋妴宥夋晬濞戞娼旂€殿喒鍋撻柛姘唉閹风兘宕濋悩鎻掑緭濞达絾鐟ラ崣蹇涙焾椤帞绀夊ù婧垮€楃划鐗堝緞閺嵮呮勾 ScrollView 婵犲﹥鑹炬慨鈺呭Υ?*/
    private void buildSources() {
        sourceGroup.removeAllViews();
        boolean open = expanded();

        TimeSync.Source[] shown;
        if (open) {
            TimeSync.Source[] more = TimeSync.MORE();
            shown = new TimeSync.Source[TimeSync.QUICK.length + more.length];
            System.arraycopy(TimeSync.QUICK, 0, shown, 0, TimeSync.QUICK.length);
            System.arraycopy(more, 0, shown, TimeSync.QUICK.length, more.length);
        } else {
            shown = TimeSync.QUICK;
        }

        int gap = dp(8);
        int cols = 3;
        LinearLayout row = null;
        for (int i = 0; i < shown.length; i++) {
            if (i % cols == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                if (i > 0) rlp.topMargin = gap;
                sourceGroup.addView(row, rlp);
            }
            TimeSync.Source s = shown[i];
            TextView chip = chipView(s.label);
            chip.setTag(s.code);
            chip.setOnClickListener(v -> pickSource(s));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if ((i % cols) != cols - 1) lp.rightMargin = gap;
            row.addView(chip, lp);
        }

        sourceArrow.setRotation(open ? 180f : 0f);
        int total = TimeSync.QUICK.length + TimeSync.MORE().length;
        sourceSummary.setText(open ? getString(R.string.collapse)
                : getString(R.string.expand_more) + " (" + total + ")");
        refreshChips();
    }
    private TextView chipView(String label) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        t.setBackgroundResource(R.drawable.bg_chip);
        t.setTextColor(getResources().getColorStateList(R.color.chip_text));
        t.setGravity(Gravity.CENTER);
        int px = dp(14), py = dp(7);
        t.setPadding(px, py, px, py);
        t.setClickable(true);
        return t;
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }

    private void refreshChips() {
        String cur = prefs.source();
        applySelection(sourceGroup, cur);
    }

    /** 递归标记选中的 chip。 */
    private void applySelection(ViewGroup group, String cur) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View v = group.getChildAt(i);
            if (v instanceof ViewGroup) {
                applySelection((ViewGroup) v, cur);
            } else if (v.getTag() instanceof String) {
                v.setSelected(cur.equals(v.getTag()));
            }
        }
    }

    private void pickSource(final TimeSync.Source src) {
        prefs.putString(Prefs.K_SOURCE, src.code);
        refreshChips();

        if ("system".equals(src.code)) {
            prefs.putLong(Prefs.K_NET_OFFSET, 0);
            prefs.putString(Prefs.K_NET_LABEL, "");
            calibState.setText(R.string.sync_none);
            return;
        }

        syncing = true;
        calibState.setText(getString(R.string.sync_running, src.label));

        new Thread(() -> {
            final TimeSync.Result r = TimeSync.fetch(src, System.currentTimeMillis());
            runOnUiThread(() -> {
                syncing = false;
                if (r.ok && r.offsetMs != 0) {
                    prefs.putLong(Prefs.K_NET_OFFSET, r.offsetMs);
                    prefs.putString(Prefs.K_NET_LABEL, r.sourceLabel);
                    String word = r.offsetMs >= 0 ? getString(R.string.faster) : getString(R.string.slower);
                    calibState.setText(getString(R.string.sync_ok, r.sourceLabel, word,
                            Math.abs(r.offsetMs) / 1000.0, r.rttMs));
                } else if (r.ok && r.offsetMs == 0) {
                    prefs.putLong(Prefs.K_NET_OFFSET, 0);
                    prefs.putString(Prefs.K_NET_LABEL, r.sourceLabel);
                    calibState.setText(getString(R.string.sync_ok, r.sourceLabel,
                            getString(R.string.faster), 0.0, r.rttMs));
                } else {
                    prefs.putLong(Prefs.K_NET_OFFSET, 0);
                    prefs.putString(Prefs.K_NET_LABEL, "");
                    calibState.setText(getString(R.string.sync_fail, src.label, r.error));
                }
            });
        }).start();
    }

    // ---------------- 闁哄啫鐖煎Λ鍨€掗崣澶屽帬 ----------------

    private void render() {
        long wall = prefs.displayNow();
        boolean bj = "beijing".equals(prefs.source());
        TimeZone tz = bj ? TimeZone.getTimeZone("GMT") : TimeZone.getDefault();
        hms.setTimeZone(tz);
        dfull.setTimeZone(tz);
        long shown = bj ? TimeSync.beijingWall(wall) : wall;
        time.setText(hms.format(new Date(shown)));
        ms.setText("." + (Math.floorMod(shown, 1000L) / 100L));
        date.setText(dfull.format(new Date(shown)));
    }

    // ---------------- 濞存嚎鍊撶花?----------------

    private void nudge(int delta) {
        int v = prefs.manualMs() + delta;
        prefs.putInt(Prefs.K_MANUAL_MS, v);
        adjValue.setText(String.valueOf(v));
    }

    private void editManual() {
        input(getString(R.string.manual_adjust), String.valueOf(prefs.manualMs()),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED, s -> {
                    try {
                        prefs.putInt(Prefs.K_MANUAL_MS, Integer.parseInt(s.trim()));
                    } catch (Exception e) {
                        prefs.putInt(Prefs.K_MANUAL_MS, 0);
                    }
                    adjValue.setText(String.valueOf(prefs.manualMs()));
                });
    }

    private void editSense() {
        input(getString(R.string.sense_error), String.valueOf(prefs.senseMs()),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED, s -> {
                    try {
                        prefs.putInt(Prefs.K_SENSE_MS, Integer.parseInt(s.trim()));
                    } catch (Exception e) {
                        prefs.putInt(Prefs.K_SENSE_MS, 0);
                    }
                    senseValue.setText(prefs.senseMs() + " ms");
                });
    }

    private void onToggleFloat() {
        if (prefs.floatOn()) {
            prefs.putBoolean(Prefs.K_FLOAT_ON, false);
            FloatService.stop(this);
            refreshToggle();
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.perm_need, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
            return;
        }
        prefs.putBoolean(Prefs.K_FLOAT_ON, true);
        FloatService.start(this);
        refreshToggle();
    }

    private void refreshToggle() {
        boolean on = prefs.floatOn();
        toggle.setText(on ? R.string.close_float : R.string.open_float);
        toggle.setBackgroundResource(on ? R.drawable.bg_btn_ghost : R.drawable.bg_btn_primary);
        toggle.setTextColor(on ? getResources().getColor(R.color.text_secondary)
                : getResources().getColor(R.color.white));
    }

    private void refreshRemindState() {
        boolean on = prefs.getBoolean(Prefs.K_DAILY_ON, false);
        int hh = prefs.getInt(Prefs.K_DAILY_H, 0);
        int mm = prefs.getInt(Prefs.K_DAILY_M, 0);
        remindState.setText(on ? String.format(Locale.US, "%02d:%02d", hh, mm) : "");
    }

    interface OnText { void got(String s); }

    private void input(String title, String value, int type, OnText cb) {
        final EditText et = new EditText(this);
        et.setText(value);
        et.setInputType(type);
        et.setSelection(et.getText().length());

        LinearLayout wrap = new LinearLayout(this);
        wrap.setPadding(dp(24), dp(8), dp(24), 0);
        wrap.addView(et, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        new android.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setView(wrap)
                .setPositiveButton(R.string.ok, (d, w) -> cb.got(et.getText().toString()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
