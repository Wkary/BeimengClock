package beimeng.clock;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;

/** 样式模型：预设模板 + 由偏好推导出的绘制参数。 */
public final class Style {

    public static final int PRESET_DARK = 0;
    public static final int PRESET_LIGHT = 1;
    public static final int PRESET_TRANS = 2;
    public static final int PRESET_GRADIENT = 3;
    public static final int PRESET_COUNT = 4;

    public static final String[] PRESET_NAMES = {
            "黑底白字", "白底黑字", "透明无背景", "渐变彩色"
    };

    /** 每个预设的默认值：文字色、文字透明度、背景色、背景透明度、圆角、阴影开关、阴影色、阴影透明度、偏移档 */
    private static final int[][] PRESETS = {
            // textColor, textAlpha, bgColor, bgAlpha, corner, shadowOn, shadowColor, shadowAlpha, offset
            {0xFFFFFFFF, 100, 0xFF000000, 50, 8, 1, 0xFF000000, 100, 2},   // 黑底白字
            {0xFF1C1D1F, 100, 0xFFFFFFFF, 60, 8, 0, 0xFF000000, 40, 1},    // 白底黑字
            {0xFFFFFFFF, 100, 0xFF000000, 0, 0, 1, 0xFF000000, 100, 3},    // 透明无背景（靠阴影保证可读）
            {0xFFFFFFFF, 100, 0xFF12C2E9, 55, 14, 1, 0xFF000000, 60, 2},   // 渐变彩色
    };

    public static final int[] TEXT_SIZES_SP = {14, 18, 24};
    private static final float[] OFFSET_DX = {0f, 0.6f, 1.0f, 1.6f};
    private static final float[] OFFSET_DY = {0f, 0.6f, 1.0f, 1.6f};
    private static final float[] OFFSET_RADIUS = {0f, 1.5f, 2.0f, 3.0f};

    /** 套用预设，写回偏好。 */
    public static void applyPreset(Prefs p, int idx) {
        if (idx < 0 || idx >= PRESET_COUNT) return;
        int[] v = PRESETS[idx];
        p.putInt(Prefs.K_PRESET, idx);
        p.putInt(Prefs.K_TEXT_COLOR, v[0]);
        p.putInt(Prefs.K_TEXT_ALPHA, v[1]);
        p.putInt(Prefs.K_BG_COLOR, v[2]);
        p.putInt(Prefs.K_BG_ALPHA, v[3]);
        p.putInt(Prefs.K_BG_CORNER, v[4]);
        p.putBoolean(Prefs.K_SHADOW_ON, v[5] == 1);
        p.putInt(Prefs.K_SHADOW_COLOR, v[6]);
        p.putInt(Prefs.K_SHADOW_ALPHA, v[7]);
        p.putInt(Prefs.K_SHADOW_OFFSET, v[8]);
    }

    /** 用户手动改动后，预设标记为"自定义"。 */
    public static void markCustom(Prefs p) {
        p.putInt(Prefs.K_PRESET, -1);
    }

    /** 把 alpha 百分比合成到颜色上。 */
    public static int withAlpha(int color, int alphaPercent) {
        int a = Math.max(0, Math.min(100, alphaPercent));
        return Color.argb(a * 255 / 100, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static int textColor(Prefs p) {
        return withAlpha(p.textColor(), p.textAlpha());
    }

    public static float textSizeSp(Prefs p) {
        int i = Math.max(0, Math.min(TEXT_SIZES_SP.length - 1, p.textSize()));
        return TEXT_SIZES_SP[i];
    }

    /** 阴影色（含透明度）。关闭时返回透明。 */
    public static int shadowColor(Prefs p) {
        if (!p.shadowOn()) return Color.TRANSPARENT;
        return withAlpha(p.shadowColor(), p.shadowAlpha());
    }

    public static float shadowDx(Prefs p) { return OFFSET_DX[clampOff(p.shadowOffset())]; }
    public static float shadowDy(Prefs p) { return OFFSET_DY[clampOff(p.shadowOffset())]; }
    public static float shadowRadius(Prefs p) { return OFFSET_RADIUS[clampOff(p.shadowOffset())]; }

    private static int clampOff(int v) { return Math.max(0, Math.min(3, v)); }

    /** 悬浮窗背景：渐变预设走渐变，其余走纯色圆角。 */
    public static GradientDrawable background(Prefs p, android.content.Context c) {
        GradientDrawable d = new GradientDrawable();
        int corner = Math.max(0, p.bgCorner());
        float r = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, corner,
                c.getResources().getDisplayMetrics());
        d.setCornerRadius(r);
        if (p.preset() == PRESET_GRADIENT) {
            d.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
            d.setColors(new int[]{
                    withAlpha(0xFF12C2E9, p.bgAlpha()),
                    withAlpha(0xFF7026DA, p.bgAlpha()),
                    withAlpha(0xFFE6246F, p.bgAlpha())
            });
        } else {
            d.setColor(withAlpha(p.bgColor(), p.bgAlpha()));
        }
        return d;
    }

    /** 把样式套到文本控件上。 */
    public static void applyText(android.widget.TextView tv, Prefs p) {
        tv.setTextColor(textColor(p));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp(p));
        if (p.shadowOn()) {
            tv.setShadowLayer(shadowRadius(p), shadowDx(p), shadowDy(p), shadowColor(p));
        } else {
            tv.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
        }
    }
}
