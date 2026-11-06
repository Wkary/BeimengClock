package beimeng.clock;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 悬浮窗样式页：文字颜色/透明度/大小、背景颜色/透明度/圆角、
 * 阴影开关/颜色/透明度/偏移、以及 4 套预设模板。全部实时预览并即时下发到悬浮窗。
 */
public class StyleActivity extends Activity {

    private static final int[] SWATCHES = {
            0xFFFFFFFF, 0xFF000000, 0xFFE53935, 0xFFFB8C00,
            0xFFFDD835, 0xFF3D8A5F, 0xFF00ACC1, 0xFF1E88E5,
            0xFF8E24AA, 0xFFD81B60, 0xFF8C9A91, 0xFF7A5B45
    };

    private Prefs prefs;
    private View previewBox;
    private TextView previewTime, previewMs;
    private TextView svTextAlpha, svBgAlpha, svCorner, svShadowAlpha;
    private SeekBar sbTextAlpha, sbBgAlpha, sbCorner, sbShadowAlpha;
    private Switch swShadow;
    private View shadowBlock;
    private LinearLayout textSwatchRow, bgSwatchRow, shadowSwatchRow;
    private LinearLayout presetRow, sizeRow, offsetRow;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        setContentView(R.layout.activity_style);

        previewBox = findViewById(R.id.style_preview_box);
        previewTime = findViewById(R.id.style_preview_time);
        previewMs = findViewById(R.id.style_preview_ms);
        svTextAlpha = findViewById(R.id.sv_text_alpha);
        svBgAlpha = findViewById(R.id.sv_bg_alpha);
        svCorner = findViewById(R.id.sv_corner);
        svShadowAlpha = findViewById(R.id.sv_shadow_alpha);
        sbTextAlpha = findViewById(R.id.sb_text_alpha);
        sbBgAlpha = findViewById(R.id.sb_bg_alpha);
        sbCorner = findViewById(R.id.sb_corner);
        sbShadowAlpha = findViewById(R.id.sb_shadow_alpha);
        swShadow = findViewById(R.id.sw_shadow);
        shadowBlock = findViewById(R.id.shadow_block);
        textSwatchRow = findViewById(R.id.text_swatch_group);
        bgSwatchRow = findViewById(R.id.bg_swatch_group);
        shadowSwatchRow = findViewById(R.id.shadow_swatch_group);
        presetRow = findViewById(R.id.preset_group);
        sizeRow = findViewById(R.id.size_group);
        offsetRow = findViewById(R.id.offset_group);

        buildPresets();
        buildSizes();
        buildOffsets();
        buildSwatchRow(textSwatchRow, c -> {
            prefs.putInt(Prefs.K_TEXT_COLOR, c);
            Style.markCustom(prefs);
            refreshSelections();
            update();
        });
        buildSwatchRow(bgSwatchRow, c -> {
            prefs.putInt(Prefs.K_BG_COLOR, c);
            Style.markCustom(prefs);
            refreshSelections();
            update();
        });
        buildSwatchRow(shadowSwatchRow, c -> {
            prefs.putInt(Prefs.K_SHADOW_COLOR, c);
            Style.markCustom(prefs);
            refreshSelections();
            update();
        });

        wireBar(sbTextAlpha, svTextAlpha, Prefs.K_TEXT_ALPHA, true);
        wireBar(sbBgAlpha, svBgAlpha, Prefs.K_BG_ALPHA, true);
        wireBar(sbShadowAlpha, svShadowAlpha, Prefs.K_SHADOW_ALPHA, true);
        wireBar(sbCorner, svCorner, Prefs.K_BG_CORNER, false);

        swShadow.setOnCheckedChangeListener((v, on) -> {
            prefs.putBoolean(Prefs.K_SHADOW_ON, on);
            Style.markCustom(prefs);
            shadowBlock.setVisibility(on ? View.VISIBLE : View.GONE);
            refreshSelections();
            update();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        restore();
        update();
    }

    // ---------- 构建 ----------

    private void buildPresets() {
        presetRow.removeAllViews();
        for (int i = 0; i < Style.PRESET_COUNT; i++) {
            final int idx = i;
            TextView t = dividerRow(Style.PRESET_NAMES[i], i == Style.PRESET_DARK,
                    i < Style.PRESET_COUNT - 1);
            t.setOnClickListener(v -> {
                Style.applyPreset(prefs, idx);
                restore();
                update();
            });
            t.setTag(("preset" + i).hashCode());
            presetRow.addView(t);
        }
    }

    /** 纵向卡片里的整行条目：左侧名称、右侧勾选提示，底部细分隔线。 */
    private TextView dividerRow(String text, boolean recommended, boolean divider) {
        TextView t = new TextView(this);
        t.setText(recommended ? text + "  " + getString(R.string.preset_recommend_tag) : text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        t.setTextColor(getResources().getColorStateList(R.color.chip_text));
        int px = dp(16), py = dp(15);
        t.setPadding(px, py, px, py);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setBackgroundResource(divider ? R.drawable.bg_row_ripple_line : R.drawable.bg_row_ripple);
        t.setClickable(true);
        return t;
    }

    private void buildSizes() {
        sizeRow.removeAllViews();
        String[] names = {getString(R.string.size_small),
                getString(R.string.size_medium), getString(R.string.size_large)};
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            TextView t = chip(names[i]);
            t.setOnClickListener(v -> {
                prefs.putInt(Prefs.K_TEXT_SIZE, idx);
                Style.markCustom(prefs);
                refreshSelections();
                update();
            });
            t.setTag(("size" + i).hashCode());
            sizeRow.addView(t, chipLp());
        }
    }

    private void buildOffsets() {
        offsetRow.removeAllViews();
        String[] names = {getString(R.string.offset_none), getString(R.string.offset_soft),
                getString(R.string.offset_normal), getString(R.string.offset_strong)};
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            TextView t = chip(names[i]);
            t.setOnClickListener(v -> {
                prefs.putInt(Prefs.K_SHADOW_OFFSET, idx);
                Style.markCustom(prefs);
                refreshSelections();
                update();
            });
            t.setTag(("off" + i).hashCode());
            offsetRow.addView(t, chipLp());
        }
    }

    interface OnColor { void picked(int color); }

    /** 12 色板 + 末尾一个"自定义"格，走输入十六进制。 */
    private void buildSwatchRow(LinearLayout row, OnColor cb) {
        row.removeAllViews();
        int sz = dp(30);
        int gap = dp(8);
        for (int c : SWATCHES) {
            View v = new View(this);
            v.setBackground(swatch(c));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sz, sz);
            lp.rightMargin = gap;
            v.setOnClickListener(x -> cb.picked(c));
            v.setTag(Integer.valueOf(c));
            row.addView(v, lp);
        }
        View custom = new View(this);
        custom.setBackground(swatchCustom());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sz, sz);
        custom.setOnClickListener(v -> pickCustomColor(cb));
        row.addView(custom, lp);
    }

    private GradientDrawable swatch(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(8));
        d.setStroke(dp(1), 0x33000000);
        return d;
    }

    /** "自定义"格：四色渐变，表示取色器。 */
    private GradientDrawable swatchCustom() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFFE53935, 0xFFFDD835, 0xFF3D8A5F, 0xFF1E88E5});
        d.setCornerRadius(dp(8));
        d.setStroke(dp(1), 0x66000000);
        return d;
    }

    /** 十六进制取色对话框；确定后回调到调用方。 */
    private void pickCustomColor(final OnColor cb) {
        final EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setHint("#RRGGBB");
        et.setText("#FFFFFF");
        et.setSelection(et.getText().length());
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.custom_color)
                .setView(et)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    String s = et.getText().toString().trim();
                    try {
                        if (s.length() == 7) s = "FF" + s.substring(1);   // #RRGGBB -> #AARRGGBB
                        cb.picked(Color.parseColor(s));
                        refreshSelections();
                        update();
                    } catch (Exception e) {
                        Toast.makeText(this, R.string.color_bad,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private TextView chip(String text) {
        TextView t = new TextView(new android.view.ContextThemeWrapper(
                this, R.style.Theme_Beimeng));
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        t.setBackgroundResource(R.drawable.bg_chip);
        t.setTextColor(getResources().getColorStateList(R.color.chip_text));
        t.setGravity(Gravity.CENTER);
        int p = dp(8);
        t.setPadding(dp(12), p, dp(12), p);
        return t;
    }

    private LinearLayout.LayoutParams chipLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(8);
        return lp;
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }

    // ---------- 同步 ----------

    private void wireBar(SeekBar bar, TextView label, final String key, final boolean percent) {
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (!fromUser) return;
                prefs.putInt(key, p);
                Style.markCustom(prefs);
                label.setText(percent ? p + "%" : String.valueOf(p));
                refreshSelections();
                update();
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) { }

            @Override
            public void onStopTrackingTouch(SeekBar s) { }
        });
    }

    /** 把偏好读回控件。 */
    private void restore() {
        sbTextAlpha.setProgress(prefs.textAlpha());
        svTextAlpha.setText(prefs.textAlpha() + "%");
        sbBgAlpha.setProgress(prefs.bgAlpha());
        svBgAlpha.setText(prefs.bgAlpha() + "%");
        sbCorner.setProgress(prefs.bgCorner());
        svCorner.setText(String.valueOf(prefs.bgCorner()));
        sbShadowAlpha.setProgress(prefs.shadowAlpha());
        svShadowAlpha.setText(prefs.shadowAlpha() + "%");
        swShadow.setChecked(prefs.shadowOn());
        shadowBlock.setVisibility(prefs.shadowOn() ? View.VISIBLE : View.GONE);
        refreshSelections();
    }

    /** 高亮当前选中的 chip / swatch。 */
    private void refreshSelections() {
        markChips(presetRow, prefs.preset() < 0 ? -1 : ("preset" + prefs.preset()).hashCode());
        markChips(sizeRow, ("size" + prefs.textSize()).hashCode());
        markChips(offsetRow, ("off" + prefs.shadowOffset()).hashCode());
        markSwatch(textSwatchRow, prefs.textColor());
        markSwatch(bgSwatchRow, prefs.bgColor());
        markSwatch(shadowSwatchRow, prefs.shadowColor());
    }

    private void markChips(LinearLayout row, int tag) {
        for (int i = 0; i < row.getChildCount(); i++) {
            View v = row.getChildAt(i);
            v.setSelected(v.getTag() != null && ((Integer) v.getTag()) == tag);
        }
    }

    private void markSwatch(LinearLayout row, int color) {
        for (int i = 0; i < row.getChildCount(); i++) {
            View v = row.getChildAt(i);
            Object t = v.getTag();
            boolean sel = t instanceof Integer && ((Integer) t) == color;
            if (v.getBackground() instanceof GradientDrawable) {
                GradientDrawable d = (GradientDrawable) v.getBackground();
                d.setStroke(dp(sel ? 3 : 1), sel ? 0xFF3D8A5F : 0x33000000);
            }
        }
    }

    /** 预览 + 实时下发到悬浮窗。 */
    private void update() {
        previewBox.setBackground(Style.background(prefs, this));
        Style.applyText(previewTime, prefs);
        Style.applyText(previewMs, prefs);
        previewMs.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                Math.max(9f, Style.textSizeSp(prefs) - 6f));
        if (prefs.floatOn()) FloatService.refresh(this);
    }
}
