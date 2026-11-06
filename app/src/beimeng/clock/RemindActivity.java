package beimeng.clock;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Locale;

/** 定时提醒 + 倒计时提醒。 */
public class RemindActivity extends Activity {

    private Prefs prefs;
    private TextView dailyTime, cdState, listState;
    private Switch swDaily, swRepeat;
    private EditText etMin;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        setContentView(R.layout.activity_remind);

        dailyTime = findViewById(R.id.btn_daily_time);
        cdState = findViewById(R.id.countdown_state);
        listState = findViewById(R.id.remind_list);
        swDaily = findViewById(R.id.sw_daily);
        swRepeat = findViewById(R.id.sw_daily_repeat);
        etMin = findViewById(R.id.et_countdown_min);

        dailyTime.setOnClickListener(v -> pickTime());

        swDaily.setOnCheckedChangeListener((v, on) -> {
            prefs.putBoolean(Prefs.K_DAILY_ON, on);
            if (on) RemindReceiver.Remind.scheduleDaily(this);
            else RemindReceiver.Remind.cancelDaily(this);
            refresh();
        });
        swRepeat.setOnCheckedChangeListener((v, on) -> {
            prefs.putBoolean(Prefs.K_DAILY_REPEAT, on);
            if (prefs.getBoolean(Prefs.K_DAILY_ON, false)) RemindReceiver.Remind.scheduleDaily(this);
            refresh();
        });

        findViewById(R.id.btn_countdown_start).setOnClickListener(v -> startCountdown());
        findViewById(R.id.btn_countdown_stop).setOnClickListener(v -> {
            RemindReceiver.Remind.cancelCountdown(this);
            prefs.putBoolean(Prefs.K_CD_PENDING, false);
            refresh();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        RemindReceiver.Remind.ensureChannels(this);
        swDaily.setChecked(prefs.getBoolean(Prefs.K_DAILY_ON, false));
        swRepeat.setChecked(prefs.getBoolean(Prefs.K_DAILY_REPEAT, true));
        etMin.setText(String.valueOf(prefs.getInt(Prefs.K_CD_MIN, 5)));
        refresh();
    }

    private void pickTime() {
        int h = prefs.getInt(Prefs.K_DAILY_H, 8);
        int m = prefs.getInt(Prefs.K_DAILY_M, 0);
        new TimePickerDialog(this, (TimePicker view, int hh, int mm) -> {
            prefs.putInt(Prefs.K_DAILY_H, hh);
            prefs.putInt(Prefs.K_DAILY_M, mm);
            if (prefs.getBoolean(Prefs.K_DAILY_ON, false)) RemindReceiver.Remind.scheduleDaily(this);
            refresh();
        }, h, m, true).show();
    }

    private void startCountdown() {
        int min;
        try {
            min = Integer.parseInt(etMin.getText().toString().trim());
        } catch (Exception e) {
            min = -1;
        }
        if (min < 1 || min > 599) {
            Toast.makeText(this, R.string.countdown_bad, Toast.LENGTH_SHORT).show();
            return;
        }
        prefs.putInt(Prefs.K_CD_MIN, min);
        prefs.putBoolean(Prefs.K_CD_PENDING, true);
        RemindReceiver.Remind.startCountdown(this, min);
        refresh();
    }

    private void refresh() {
        int h = prefs.getInt(Prefs.K_DAILY_H, 8);
        int m = prefs.getInt(Prefs.K_DAILY_M, 0);
        dailyTime.setText(String.format(Locale.US, "%02d:%02d", h, m));

        boolean on = prefs.getBoolean(Prefs.K_DAILY_ON, false);
        StringBuilder sb = new StringBuilder();
        if (on) {
            sb.append(getString(R.string.remind_on, h, m));
            if (!prefs.getBoolean(Prefs.K_DAILY_REPEAT, true)) sb.append(" · ").append(getString(R.string.once));
        } else {
            sb.append(getString(R.string.remind_off));
        }
        if (prefs.getBoolean(Prefs.K_CD_PENDING, false)) {
            sb.append("\n").append(getString(R.string.countdown_running))
                    .append(" (").append(prefs.getInt(Prefs.K_CD_MIN, 5)).append(getString(R.string.minute_unit)).append(")");
        }
        listState.setText(sb.toString());
        cdState.setText(prefs.getBoolean(Prefs.K_CD_PENDING, false)
                ? R.string.countdown_running : R.string.countdown_idle);
    }
}
