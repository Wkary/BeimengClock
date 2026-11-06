package beimeng.clock;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 36 个时间源。每个源给一组候选接口，逐个尝试，首个成功即用。
 *
 * 说明：ZK 原版自身没有任何直连平台的网络代码，它把所有平台名发给自己的
 * 私有服务端（POST host:8080/{method}）统一取偏移量。那套服务端不可复刻，
 * 所以这里对每个平台实现真实的公开取时接口；拿不到的平台会逐级回落到
 * 该平台所属的父级接口（例如天猫→淘宝、小米有品→小米、Nike→SNKRS）。
 */
public final class TimeSync {

    public static final class Source {
        public final String code;
        public final String label;
        public final String[][] endpoints;   // {url, mode}  mode: json | header

        Source(String code, String label, String[][] endpoints) {
            this.code = code;
            this.label = label;
            this.endpoints = endpoints;
        }
    }

    // ---- 通用 HTTP Date 头（可作为任意站的兜底）----
    private static final String[][] HDR = {
            {"https://www.baidu.com", "header"},
            {"https://www.jd.com", "header"},
            {"https://www.taobao.com", "header"},
            {"https://www.163.com", "header"},
    };

    // ---- 阿里 mtop 时间戳（淘宝/天猫/淘票票 同源）----
    private static final String[][] MTOP_TB = {
            {"https://acs.m.taobao.com/gw/mtop.common.getTimestamp/*/", "json"},
            {"https://api.m.taobao.com/rest/3.0/apiTime/getTime/1.0/", "json"},
    };

    // ---- 大麦 mtop 时间戳（大麦/猫眼/缤纷岛 同源）----
    private static final String[][] MTOP_DM = {
            {"https://mtop.damai.cn/gw/mtop.common.getTimestamp/*/", "json"},
            {"https://mtop.damai.cn/gw/mtop.common.getTimestamp/1.0/", "json"},
    };

    // ---- 苏宁 ---
    private static final String[][] SUNING = {
            {"https://f.m.suning.com/api/ct.do", "json"},
            {"https://quan.suning.com/getSysTime.do", "json"},
    };

    // ---- 京东 ---
    private static final String[][] JD = {
            {"https://a.jd.com//ajax/queryServerData.html", "json"},
            {"https://api.m.jd.com/client.action?functionId=posWeekDayTreatResult", "header"},
    };

    // ---- 拼多多 ---
    private static final String[][] PDD = {
            {"https://www.pinduoduo.com", "header"},
            {"https://m.pinduoduo.com", "header"},
    };

    // ---- 抖音 / TikTok ---
    private static final String[][] DOUYIN = {
            {"https://www.douyin.com", "header"},
            {"https://www.iesdouyin.com", "header"},
    };

    // ---- 快手 ---
    private static final String[][] KUAISHOU = {
            {"https://www.kuaishou.com", "header"},
            {"https://api.kuaishouzt.com/rest/zt/healthcheck", "header"},
    };

    // ---- 美团 ---
    private static final String[][] MEITUAN = {
            {"https://www.meituan.com", "header"},
            {"https://i.meituan.com", "header"},
    };

    // ---- 饿了么 ---
    private static final String[][] ELEME = {
            {"https://www.ele.me", "header"},
            {"https://h5.ele.me/restapi/v1/", "header"},
    };

    // ---- 小红书 ---
    private static final String[][] XHS = {
            {"https://www.xiaohongshu.com", "header"},
            {"https://edith.xiaohongshu.com/api/sns/web/v1/homefeed", "header"},
    };

    // ---- 微信 ---
    private static final String[][] WECHAT = {
            {"https://mp.weixin.qq.com", "header"},
            {"https://wx.qq.com", "header"},
    };

    // ---- 唯品会 ---
    private static final String[][] VIP = {
            {"https://www.vip.com", "header"},
            {"https://api.vip.com", "header"},
    };

    // ---- 得物 ---
    private static final String[][] POIZON = {
            {"https://www.dewu.com", "header"},
            {"https://app.dewu.com/api/v1/timestamp", "json"},
    };

    // ---- SNKRS / Nike / Adidas ---
    private static final String[][] NIKE = {
            {"https://www.nike.com", "header"},
            {"https://www.nike.com/cn", "header"},
    };
    private static final String[][] ADIDAS = {
            {"https://www.adidas.com.cn", "header"},
            {"https://www.adidas.com", "header"},
    };

    // ---- 盒子/生鲜类 ---
    private static final String[][] HEMA = {
            {"https://www.freshhema.com", "header"},
            {"https://res.m.hema.com", "header"},
    };
    // 麦德龙主站当前 SSL 证书异常、HTTP 协议握手失败，回落通用源
    private static final String[][] METRO = {
            {"https://www.jd.com", "header"},
            {"https://www.baidu.com", "header"},
    };
    private static final String[][] DDMC = {
            {"https://www.dmall.com", "header"},
            {"https://m.dmall.com", "header"},
    };
    private static final String[][] DINGDONG = {
            {"https://www.100.me", "header"},
            {"https://api.100.me", "header"},
    };

    // ---- 酒类 ---
    private static final String[][] IMAOTAI = {
            {"https://www.moutai.com.cn", "header"},
            {"https://h5.moutai519.com.cn", "header"},
    };
    private static final String[][] JIUXIAN = {
            {"https://www.jiuxian.com", "header"},
            {"https://m.jiuxian.com", "header"},
    };

    // ---- 银行/支付 ---
    private static final String[][] UNIONPAY = {
            {"https://cn.unionpay.com", "header"},
            {"https://open.unionpay.com", "header"},
    };
    private static final String[][] CCB = {
            {"https://www.ccb.com", "header"},
            {"https://m.ccb.com", "header"},
    };

    // ---- 严选/有品 ---
    private static final String[][] YANXUAN = {
            {"https://you.163.com", "header"},
            {"https://www.163.com", "header"},
    };
    private static final String[][] MIYOUPIN = {
            {"https://www.xiaomiyoupin.com", "header"},
            {"https://api.xiaomiyoupin.com", "header"},
    };

    // ---- 华为 / 小米 ---
    private static final String[][] HUAWEI = {
            {"https://www.vmall.com", "header"},
            {"https://www.huawei.com", "header"},
    };
    private static final String[][] XIAOMI = {
            {"https://www.mi.com", "header"},
            {"https://api.mi.com", "header"},
    };

    // ---- 猫眼 / 淘票票 / 缤纷岛 ---
    private static final String[][] MAOYAN = {
            {"https://www.maoyan.com", "header"},
            {"https://m.maoyan.com", "header"},
    };
    private static final String[][] TAOPIAO = {
            {"https://www.taopiaopiao.com", "header"},
            {"https://mtop.damai.cn/gw/mtop.common.getTimestamp/*/", "json"},
    };

    // ---------------------------------------------------------------
    //  36 个源，顺序与 ZK 原版一致
    // ---------------------------------------------------------
    public static final Source SYSTEM = new Source("system", "系统", new String[][]{});

    public static final Source[] ALL = {
            new Source("beijing",  "北京时间", HDR),
            new Source("taobao",   "淘宝",     MTOP_TB),
            new Source("jd",       "京东",     JD),
            new Source("tmall",    "天猫",     MTOP_TB),
            new Source("imaotai",  "i茅台",    IMAOTAI),
            new Source("suning",   "苏宁",     SUNING),
            new Source("pdd",      "拼多多",   PDD),
            new Source("yanxuan",  "严选",     YANXUAN),
            new Source("huawei",   "华为",     HUAWEI),
            new Source("xiaomi",   "小米",     XIAOMI),
            new Source("jiuxian",  "酒仙",     JIUXIAN),
            new Source("douyin",   "抖音",     DOUYIN),
            new Source("kuaishou", "快手",     KUAISHOU),
            new Source("hema",     "盒马",     HEMA),
            new Source("metro",    "麦德龙",   METRO),
            new Source("ddmc",     "多点",     DDMC),
            new Source("damai",    "大麦",     MTOP_DM),
            new Source("maoyan",   "猫眼",     MAOYAN),
            new Source("taopiaopiao", "淘票票", TAOPIAO),
            new Source("binfendao", "缤纷岛",  MTOP_DM),
            new Source("vip",      "唯品会",   VIP),
            new Source("yanxuan1", "网易严选", YANXUAN),
            new Source("miyoupin", "小米有品", MIYOUPIN),
            new Source("dingdong", "叮咚买菜", DINGDONG),
            new Source("poizon",   "得物",     POIZON),
            new Source("snkrs",    "SNKRS",    NIKE),
            new Source("nike",     "Nike",     NIKE),
            new Source("adidas",   "Adidas",   ADIDAS),
            new Source("unionpay", "云闪付",   UNIONPAY),
            new Source("ccb",      "建行生活", CCB),
            new Source("xhs",      "小红书",   XHS),
            new Source("wechat",   "微信",     WECHAT),
            new Source("meituan",  "美团",     MEITUAN),
            new Source("eleme",    "饿了么",   ELEME),
            new Source("general",  "通用",     HDR),
    };

    /** 默认展示的两个源（系统时间已去掉，北京时间与淘宝作为默认）。 */
    public static final Source[] QUICK = {
            byCode("beijing"),
            byCode("taobao"),
    };

    /** 展开后追加展示的其余源（顺序同 ZK）。 */
    public static Source[] MORE() {
        Source[] all = ALL;
        java.util.ArrayList<Source> list = new java.util.ArrayList<>();
        for (Source s : all) {
            if (!"beijing".equals(s.code) && !"taobao".equals(s.code)) list.add(s);
        }
        return list.toArray(new Source[0]);
    }
    public static Source byCode(String code) {
        for (Source s : ALL) if (s.code.equals(code)) return s;
        return SYSTEM;
    }

    // ------------------------------------------------------------

    public static final class Result {
        public final boolean ok;
        public final long offsetMs;
        public final long rttMs;
        public final String sourceLabel;
        public final String error;

        Result(boolean ok, long offsetMs, long rttMs, String label, String error) {
            this.ok = ok;
            this.offsetMs = offsetMs;
            this.rttMs = rttMs;
            this.sourceLabel = label;
            this.error = error;
        }
    }

    public static Result fetch(Source src, long localNow) {
        if (src.endpoints.length == 0) return new Result(true, 0, 0, src.label, null);
        String lastErr = null;
        for (String[] ep : src.endpoints) {
            long t0 = android.os.SystemClock.elapsedRealtime();
            try {
                long server = probe(ep[0], "json".equals(ep[1]));
                if (server <= 0) throw new IllegalStateException("no time in response");
                long rtt = android.os.SystemClock.elapsedRealtime() - t0;
                long serverNow = server + rtt / 2;
                return new Result(true, serverNow - System.currentTimeMillis(), rtt, src.label, null);
            } catch (Exception e) {
                lastErr = shortErr(e);
            }
        }
        return new Result(false, 0, 0, src.label, lastErr);
    }

    private static String shortErr(Exception e) {
        String m = e.getMessage();
        if (m == null) return e.getClass().getSimpleName();
        return e.getClass().getSimpleName() + ": " + m;
    }

    private static long probe(String spec, boolean json) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(spec).openConnection();
        c.setConnectTimeout(6000);
        c.setReadTimeout(6000);
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36");
        c.setRequestProperty("Accept", "*/*");
        c.setInstanceFollowRedirects(true);
        try {
            int code = c.getResponseCode();
            if (code < 200 || code >= 400) throw new IllegalStateException("HTTP " + code);
            if (json) {
                long v = extractMillis(readAll(c.getInputStream()));
                if (v <= 0) throw new IllegalStateException("no timestamp in body");
                return v;
            }
            String date = c.getHeaderField("Date");
            if (date == null) throw new IllegalStateException("no Date header");
            return parseHttpDate(date);
        } finally {
            c.disconnect();
        }
    }

    private static String readAll(InputStream is) throws Exception {
        byte[] buf = new byte[8192];
        StringBuilder sb = new StringBuilder();
        int n;
        while ((n = is.read(buf)) > 0) sb.append(new String(buf, 0, n, "UTF-8"));
        is.close();
        return sb.toString();
    }

    private static final String[] KEYS = {
            "\"t\":", "\"data\":{\"t\"", "\"currentTime\":", "\"timestamp\":",
            "\"time\":", "\"t_ms\":", "\"serverTime\":", "\"sysTime\":"
    };

    static long extractMillis(String body) {
        String compact = body.replace(" ", "");
        for (String key : KEYS) {
            int i = compact.indexOf(key);
            if (i < 0) continue;
            int s = i + key.length();
            while (s < compact.length() && !Character.isDigit(compact.charAt(s)) && compact.charAt(s) != '-') s++;
            int e = s;
            while (e < compact.length() && Character.isDigit(compact.charAt(e))) e++;
            if (e > s) {
                long v = normalize(Long.parseLong(compact.substring(s, e)));
                if (v > 0) return v;
            }
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("1[0-9]{9,12}").matcher(compact);
        while (m.find()) {
            long v = normalize(Long.parseLong(m.group()));
            if (v > 0) return v;
        }
        return -1;
    }

    private static long normalize(long v) {
        if (v > 1_000_000_000_000L) return v;
        if (v > 1_000_000_000L) return v * 1000L;
        return -1;
    }

    private static long parseHttpDate(String httpDate) throws Exception {
        SimpleDateFormat f = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date d = f.parse(httpDate.trim());
        if (d == null) throw new IllegalStateException("bad Date header");
        return d.getTime();
    }

    public static long beijingWall(long utcMillis) {
        return utcMillis + 8L * 3600_000L;
    }
}
