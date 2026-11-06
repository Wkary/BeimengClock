package beimeng.clock;

import android.content.Context;
import android.content.SharedPreferences;

/** 全部偏好集中在这里，避免各处散乱读取。 */
public final class Prefs {

    public static final String SRC_SYSTEM = "system";
    public static final String SRC_BEIJING = "beijing";
    public static final String SRC_TAOBAO = "taobao";

    /** 时间源 */
    public static final String K_SOURCE = "time_source";
    public static final String K_SRC_EXPANDED = "src_expanded";
    public static final String K_NET_OFFSET = "net_offset";
    public static final String K_NET_LABEL = "net_label";
    public static final String K_MANUAL_MS = "manual_ms";
    public static final String K_SENSE_MS = "sense_ms";
    public static final String K_SMART = "smart";

    /** 悬浮窗开关与形态 */
    public static final String K_FLOAT_ON = "float_on";
    public static final String K_FLOAT_BIG = "float_big";
    public static final String K_FLOAT_X = "float_x";
    public static final String K_FLOAT_Y = "float_y";
    public static final String K_AUTOSTART = "autostart";

    /** 样式：文字 */
    public static final String K_TEXT_COLOR = "text_color";
    public static final String K_TEXT_ALPHA = "text_alpha";      // 0..100
    public static final String K_TEXT_SIZE = "text_size";        // 0小 1中 2大
    /** 样式：背景 */
    public static final String K_BG_COLOR = "bg_color";
    public static final String K_BG_ALPHA = "bg_alpha";
    public static final String K_BG_CORNER = "bg_corner";        // dp 0..24
    /** 样式：阴影 */
    public static final String K_SHADOW_ON = "shadow_on";
    public static final String K_SHADOW_COLOR = "shadow_color";
    public static final String K_SHADOW_ALPHA = "shadow_alpha";
    public static final String K_SHADOW_OFFSET = "shadow_offset";// 0..3
    /** 样式：当前预设 */
    public static final String K_PRESET = "preset";

    /** 提醒 */
    public static final String K_DAILY_ON = "daily_on";
    public static final String K_DAILY_H = "daily_h";
    public static final String K_DAILY_M = "daily_m";
    public static final String K_DAILY_REPEAT = "daily_repeat";
    public static final String K_CD_MIN = "cd_min";
    public static final String K_CD_PENDING = "cd_pending";

    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getApplicationContext().getSharedPreferences("beimeng", Context.MODE_PRIVATE);
    }

    public String source() { return sp.getString(K_SOURCE, SRC_SYSTEM); }
    public long netOffset() { return sp.getLong(K_NET_OFFSET, 0L); }
    public String netLabel() { return sp.getString(K_NET_LABEL, ""); }
    public int manualMs() { return sp.getInt(K_MANUAL_MS, 0); }
    public int senseMs() { return sp.getInt(K_SENSE_MS, 0); }
    public boolean smart() { return sp.getBoolean(K_SMART, false); }

    public int textColor() { return sp.getInt(K_TEXT_COLOR, 0xFFFFFFFF); }
    public int textAlpha() { return sp.getInt(K_TEXT_ALPHA, 100); }
    public int textSize() { return sp.getInt(K_TEXT_SIZE, 1); }

    public int bgColor() { return sp.getInt(K_BG_COLOR, 0xFF000000); }
    public int bgAlpha() { return sp.getInt(K_BG_ALPHA, 50); }
    public int bgCorner() { return sp.getInt(K_BG_CORNER, 8); }

    public boolean shadowOn() { return sp.getBoolean(K_SHADOW_ON, true); }
    public int shadowColor() { return sp.getInt(K_SHADOW_COLOR, 0xFF000000); }
    public int shadowAlpha() { return sp.getInt(K_SHADOW_ALPHA, 100); }
    public int shadowOffset() { return sp.getInt(K_SHADOW_OFFSET, 2); }
    public int preset() { return sp.getInt(K_PRESET, 0); }

    public boolean floatOn() { return sp.getBoolean(K_FLOAT_ON, false); }
    public boolean floatBig() { return sp.getBoolean(K_FLOAT_BIG, false); }
    public int floatX() { return sp.getInt(K_FLOAT_X, -1); }
    public int floatY() { return sp.getInt(K_FLOAT_Y, -1); }
    public boolean autostart() { return sp.getBoolean(K_AUTOSTART, false); }

    public void putString(String k, String v) { sp.edit().putString(k, v).apply(); }
    public void putInt(String k, int v) { sp.edit().putInt(k, v).apply(); }
    public void putLong(String k, long v) { sp.edit().putLong(k, v).apply(); }
    public void putBoolean(String k, boolean v) { sp.edit().putBoolean(k, v).apply(); }
    public int getInt(String k, int d) { return sp.getInt(k, d); }
    public boolean getBoolean(String k, boolean d) { return sp.getBoolean(k, d); }

    /** 叠加了网络偏移、手动微调、感官补偿之后的"当前应显示时刻"。 */
    public long displayNow() {
        return System.currentTimeMillis() + netOffset() + manualMs() + senseMs();
    }
}
