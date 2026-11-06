package com.lab.mhohook;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Hook implements IXposedHookLoadPackage {
    private static final String PKG = "com.garena.game.mho";
    private static final int LEVEL = 7;
    private static final Set<String> HOOKED_METHODS = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) {
        if (!PKG.equals(lpparam.packageName)) return;
        XposedBridge.log("MHO Hook: loaded into " + lpparam.packageName + " [process: " + lpparam.processName + "]");

        spoofBuildFields();
        spoofSystemProperties(lpparam.classLoader);
        spoofMemoryInfo();
        installEngineHooks(lpparam.classLoader);

        try {
            XposedHelpers.findAndHookMethod("android.app.Application", lpparam.classLoader,
                    "attachBaseContext", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Context ctx = (Context) param.args[0];
                    if (ctx != null) {
                        installEngineHooks(ctx.getClassLoader());
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("MHO Hook: Application.attachBaseContext hook notice: " + t);
        }
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

        XposedBridge.log("MHO Hook: Build identity spoofed to Samsung Galaxy S24 Ultra (SM8650/qcom)");
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
            XposedBridge.log("MHO Hook: SystemProperties hooks installed");
        } catch (Throwable t) {
            XposedBridge.log("MHO Hook: SystemProperties hook notice: " + t);
        }
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
            XposedBridge.log("MHO Hook: ActivityManager.getMemoryInfo hooked (forced 16GB totalMem)");
        } catch (Throwable t) {
            XposedBridge.log("MHO Hook: MemoryInfo hook error: " + t);
        }
    }

    private static void installEngineHooks(ClassLoader cl) {
        if (cl == null) return;

        hookResult(cl, "com.perfsight.gpm.portal.GPMAgent", "checkDCLSByQcc",
                String.class, String.class, String.class);
        hookResult(cl, "com.perfsight.gpm.portal.GPMAgent", "checkDCLSByQccSync",
                String.class, String.class, String.class);
        hookResult(cl, "com.perfsight.gpm.portal.GPMAgent", "checkDCLSByString",
                String.class, String.class, String.class, String.class);

        hookResult(cl, "com.epicgames.unreal.GameActivity", "AndroidThunkJava_getDeviceLevelByQcc",
                String.class, String.class);
        hookResult(cl, "com.epicgames.unreal.GameActivity", "AndroidThunkJava_getDeviceLevelByString",
                String.class, String.class, String.class);
        hookResult(cl, "com.epicgames.unreal.GameActivity", "AndroidThunkJava_getDeviceLevelSyncFromServerByQcc",
                String.class, String.class);

        hookSet(cl, "com.epicgames.unreal.GameActivity", "AndroidThunkJava_setDeviceLevel");
        hookSet(cl, "com.perfsight.gpm.portal.GPMAgent", "setDefinedDeviceClass");

        logArgs(cl, "com.epicgames.unreal.GameActivity", "AndroidThunkJava_setQuality", int.class, int.class);
        logArgs(cl, "com.perfsight.gpm.portal.GPMAgent", "setQulaity", int.class, int.class);
        logArgs(cl, "com.epicgames.unreal.GameActivity", "AndroidThunkJava_setTargetFramerate", int.class);
    }

    private static void hookResult(ClassLoader cl, String cls, String method, Object... types) {
        String key = cls + "#" + method;
        if (HOOKED_METHODS.contains(key)) return;

        try {
            XposedHelpers.findAndHookMethod(cls, cl, method, append(types, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    param.setResult(LEVEL);
                    XposedBridge.log("MHO Hook: " + cls + "." + method + " -> " + LEVEL);
                }
            }));
            HOOKED_METHODS.add(key);
            XposedBridge.log("MHO Hook: installed result hook " + key);
        } catch (Throwable ignored) {}
    }

    private static void hookSet(ClassLoader cl, String cls, String method) {
        String key = cls + "#" + method;
        if (HOOKED_METHODS.contains(key)) return;

        try {
            XposedHelpers.findAndHookMethod(cls, cl, method, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    int old = (Integer) param.args[0];
                    param.args[0] = LEVEL;
                    XposedBridge.log("MHO Hook: " + cls + "." + method + " " + old + " -> " + LEVEL);
                }
            });
            HOOKED_METHODS.add(key);
            XposedBridge.log("MHO Hook: installed setter hook " + key);
        } catch (Throwable ignored) {}
    }

    private static void logArgs(ClassLoader cl, String cls, String method, Object... types) {
        String key = cls + "#" + method + "_log";
        if (HOOKED_METHODS.contains(key)) return;

        try {
            XposedHelpers.findAndHookMethod(cls, cl, method, append(types, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    StringBuilder b = new StringBuilder();
                    for (Object a : param.args) b.append(a).append(',');
                    XposedBridge.log("MHO Hook: " + cls + "." + method + " args=" + b);
                }
            }));
            HOOKED_METHODS.add(key);
        } catch (Throwable ignored) {}
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
