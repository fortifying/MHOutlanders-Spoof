package com.lab.mhohook;

import android.app.ActivityManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Hook implements IXposedHookLoadPackage {
    private static final Set<String> HOOKED_METHODS = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private static boolean isTarget(String pkg) {
        return "com.garena.game.mho".equals(pkg) || "com.tencent.mhadv".equals(pkg);
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) {
        if (!isTarget(lpparam.packageName)) return;
        XposedBridge.log("MHO Hook: loaded into " + lpparam.packageName + " [process: " + lpparam.processName + "]");

        spoofBuildFields();
        spoofSystemProperties(lpparam.classLoader);
        spoofMemoryInfo();
        installEngineHooks(lpparam.classLoader);

        try {
            XposedHelpers.findAndHookMethod(ContextWrapper.class, "attachBaseContext",
                    Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Context ctx = (Context) param.args[0];
                    if (ctx != null) {
                        installEngineHooks(ctx.getClassLoader());
                    }
                }
            });
        } catch (Throwable ignored) {}
    }

    private static void spoofBuildFields() {
        setStaticField(Build.class, "MANUFACTURER", "samsung");
        setStaticField(Build.class, "BRAND", "samsung");
        setStaticField(Build.class, "MODEL", "SM-S928B");
        setStaticField(Build.class, "PRODUCT", "e3q");
        setStaticField(Build.class, "DEVICE", "e3q");
        setStaticField(Build.class, "BOARD", "kalama");
        setStaticField(Build.class, "HARDWARE", "qcom");

        try {
            setStaticField(Build.class, "SOC_MODEL", "SM8650");
            setStaticField(Build.class, "SOC_MANUFACTURER", "Qualcomm");
        } catch (Throwable ignored) {}
    }

    private static void spoofSystemProperties(ClassLoader cl) {
        try {
            Class<?> sp = XposedHelpers.findClass("android.os.SystemProperties", cl);
            if (sp == null) return;

            XC_MethodHook propHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String key = (String) param.args[0];
                    if (key == null) return;

                    if ("ro.product.model".equals(key)) param.setResult("SM-S928B");
                    else if ("ro.product.manufacturer".equals(key)) param.setResult("samsung");
                    else if ("ro.product.brand".equals(key)) param.setResult("samsung");
                    else if ("ro.board.platform".equals(key)) param.setResult("pineapple");
                    else if ("ro.hardware".equals(key)) param.setResult("qcom");
                    else if ("ro.soc.model".equals(key)) param.setResult("SM8650");
                }
            };

            XposedHelpers.findAndHookMethod(sp, "get", String.class, propHook);
            XposedHelpers.findAndHookMethod(sp, "get", String.class, String.class, propHook);
        } catch (Throwable ignored) {}
    }

    private static void spoofMemoryInfo() {
        try {
            XposedHelpers.findAndHookMethod(ActivityManager.class, "getMemoryInfo",
                    ActivityManager.MemoryInfo.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    ActivityManager.MemoryInfo mi = (ActivityManager.MemoryInfo) param.args[0];
                    if (mi != null) {
                        mi.totalMem = 16L * 1024 * 1024 * 1024;
                        mi.availMem = 12L * 1024 * 1024 * 1024;
                        mi.threshold = 1024 * 1024 * 1024;
                        mi.lowMemory = false;
                    }
                }
            });
        } catch (Throwable ignored) {}
    }

    private static void installEngineHooks(ClassLoader cl) {
        if (cl == null) return;

        hookQccMethod(cl, "com.perfsight.gpm.portal.GPMAgent", "checkDCLSByQcc",
                String.class, String.class, String.class);
        hookQccMethod(cl, "com.perfsight.gpm.portal.GPMAgent", "checkDCLSByQccSync",
                String.class, String.class, String.class);
        hookQccMethod(cl, "com.perfsight.gpm.portal.GPMAgent", "checkDCLSByString",
                String.class, String.class, String.class, String.class);

        hookQccMethod(cl, "com.epicgames.unreal.GameActivity", "AndroidThunkJava_getDeviceLevelByQcc",
                String.class, String.class);
        hookQccMethod(cl, "com.epicgames.unreal.GameActivity", "AndroidThunkJava_getDeviceLevelByString",
                String.class, String.class, String.class);
        hookQccMethod(cl, "com.epicgames.unreal.GameActivity", "AndroidThunkJava_getDeviceLevelSyncFromServerByQcc",
                String.class, String.class);

        hookSetter(cl, "com.epicgames.unreal.GameActivity", "AndroidThunkJava_setDeviceLevel", 6);
        hookSetter(cl, "com.perfsight.gpm.portal.GPMAgent", "setDefinedDeviceClass", 6);

        hookIntlAnalytics(cl);

        logMethodArgs(cl, "com.epicgames.unreal.GameActivity", "AndroidThunkJava_setQuality", int.class, int.class);
        logMethodArgs(cl, "com.perfsight.gpm.portal.GPMAgent", "setQulaity", int.class, int.class);
        logMethodArgs(cl, "com.epicgames.unreal.GameActivity", "AndroidThunkJava_setTargetFramerate", int.class);
    }

    private static void hookIntlAnalytics(ClassLoader cl) {
        String key = "com.intlgame.api.analytics.INTLAnalytics";
        if (HOOKED_METHODS.contains(key)) return;

        try {
            Class<?> cls = XposedHelpers.findClass(key, cl);
            if (cls == null) return;

            XposedHelpers.findAndHookMethod(cls, "getDeviceLevel", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    param.setResult(6);
                    XposedBridge.log("MHO Hook: INTLAnalytics.getDeviceLevel -> 6");
                }
            });

            XposedHelpers.findAndHookMethod(cls, "setDeviceLevel", int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.args[0] = 6;
                    XposedBridge.log("MHO Hook: INTLAnalytics.setDeviceLevel -> 6");
                }
            });

            HOOKED_METHODS.add(key);
        } catch (Throwable ignored) {}
    }

    private static void hookQccMethod(ClassLoader cl, String cls, String method, Object... types) {
        String key = cls + "#" + method;
        if (HOOKED_METHODS.contains(key)) return;

        try {
            XposedHelpers.findAndHookMethod(cls, cl, method, append(types, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    int level = resolveQccLevel(param.args);
                    param.setResult(level);
                    XposedBridge.log("MHO Hook: " + cls + "." + method + "(" + formatArgs(param.args) + ") -> " + level);
                }
            }));
            HOOKED_METHODS.add(key);
        } catch (Throwable ignored) {}
    }

    private static int resolveQccLevel(Object[] args) {
        if (args != null && args.length > 0 && args[0] instanceof String) {
            String config = ((String) args[0]).trim().toLowerCase(Locale.ROOT);
            if ("android".equals(config) || "ios".equals(config)) {
                return 2;
            }
            if ("blacklist".equals(config)) {
                return 1;
            }
            if ("default".equals(config)) {
                return 7;
            }
        }
        return 6;
    }

    private static void hookSetter(ClassLoader cl, String cls, String method, int forcedValue) {
        String key = cls + "#" + method;
        if (HOOKED_METHODS.contains(key)) return;

        try {
            XposedHelpers.findAndHookMethod(cls, cl, method, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    int original = (Integer) param.args[0];
                    param.args[0] = forcedValue;
                    XposedBridge.log("MHO Hook: " + cls + "." + method + " " + original + " -> " + forcedValue);
                }
            });
            HOOKED_METHODS.add(key);
        } catch (Throwable ignored) {}
    }

    private static void logMethodArgs(ClassLoader cl, String cls, String method, Object... types) {
        String key = cls + "#" + method + "_log";
        if (HOOKED_METHODS.contains(key)) return;

        try {
            XposedHelpers.findAndHookMethod(cls, cl, method, append(types, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    XposedBridge.log("MHO Hook: " + cls + "." + method + "(" + formatArgs(param.args) + ")");
                }
            }));
            HOOKED_METHODS.add(key);
        } catch (Throwable ignored) {}
    }

    private static String formatArgs(Object[] args) {
        if (args == null || args.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private static void setStaticField(Class<?> clazz, String fieldName, Object value) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            Field modifiersField = null;
            try {
                modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            } catch (Throwable ignored) {}
            field.set(null, value);
        } catch (Throwable t) {
            try {
                XposedHelpers.setStaticObjectField(clazz, fieldName, value);
            } catch (Throwable ignored) {}
        }
    }

    private static Object[] append(Object[] a, Object x) {
        Object[] r = new Object[a.length + 1];
        System.arraycopy(a, 0, r, 0, a.length);
        r[a.length] = x;
        return r;
    }
}
