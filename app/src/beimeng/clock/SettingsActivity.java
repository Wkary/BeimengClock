package beimeng.clock;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Switch;
import android.widget.TextView;

/** 设置页：只保留悬浮时钟相关项，无用户协议/隐私政策/官方群/广告。 */
public class SettingsActivity extends Activity {

    private Prefs prefs;
    private TextView styleSummary, permState;
    private Switch swAutostart;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        setContentView(R.layout.activity_settings);
        setTitle(R.string.title_settings);

        styleSummary = findViewById(R.id.style_summary);
        permState = findViewById(R.id.perm_state);
        swAutostart = findViewById(R.id.sw_autostart);

        findViewById(R.id.row_style).setOnClickListener(v ->
                startActivity(new Intent(this, StyleActivity.class)));
        findViewById(R.id.row_perm).setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });
        swAutostart.setOnCheckedChangeListener((v, on) ->
                prefs.putBoolean(Prefs.K_AUTOSTART, on));

        ((TextView) findViewById(R.id.about_state))
                .setText(BuildConfig.VERSION_NAME);
    }

    @Override
    protected void onResume() {
        super.onResume();
        styleSummary.setText(Style.PRESET_NAMES[Math.max(0, prefs.preset())]);
        permState.setText(Settings.canDrawOverlays(this)
                ? getString(R.string.perm_granted) : getString(R.string.perm_denied));
        swAutostart.setChecked(prefs.autostart());
    }
}
