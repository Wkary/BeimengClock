package beimeng.clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 开机后按用户选择恢复悬浮窗。 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent i) {
        Prefs p = new Prefs(c);
        if (!p.autostart() || !p.floatOn()) return;
        if (android.provider.Settings.canDrawOverlays(c)) FloatService.start(c);
    }
}
