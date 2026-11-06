package com.lab.mhohook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public final class Hook implements IXposedHookLoadPackage {
    private static final String PKG = "com.garena.game.mho";
    private static final int LEVEL = 7;

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) {
        if (!PKG.equals(lpparam.packageName)) return;
        XposedBridge.log("MHO Hook: loaded into " + lpparam.packageName);
        ClassLoader cl = lpparam.classLoader;

        // GPM device-level checks used by UE Java thunks.
        hookResult(cl, "com.perfsight.gpm.portal.GPMAgent", "checkDCLSByQcc",
                String.class, String.class, String.class);
        hookResult(cl, "com.perfsight.gpm.portal.GPMAgent", "checkDCLSByQccSync",
                String.class, String.class, String.class);
        hookResult(cl, "com.perfsight.gpm.portal.GPMAgent", "checkDCLSByString",
                String.class, String.class, String.class, String.class);

        // UE -> Java wrappers.
        hookResult(cl, "com.epicgames.unreal.GameActivity", "AndroidThunkJava_getDeviceLevelByQcc",
                String.class, String.class);
        hookResult(cl, "com.epicgames.unreal.GameActivity", "AndroidThunkJava_getDeviceLevelByString",
                String.class, String.class, String.class);
        hookResult(cl, "com.epicgames.unreal.GameActivity", "AndroidThunkJava_getDeviceLevelSyncFromServerByQcc",
                String.class, String.class);

        // Force the defined device class high, and log the game's own setter calls.
        hookSet(cl, "com.epicgames.unreal.GameActivity", "AndroidThunkJava_setDeviceLevel");
        hookSet(cl, "com.perfsight.gpm.portal.GPMAgent", "setDefinedDeviceClass");
        logArgs(cl, "com.epicgames.unreal.GameActivity", "AndroidThunkJava_setQuality", int.class, int.class);
        logArgs(cl, "com.perfsight.gpm.portal.GPMAgent", "setQulaity", int.class, int.class);
        logArgs(cl, "com.epicgames.unreal.GameActivity", "AndroidThunkJava_setTargetFramerate", int.class);
    }

    private static void hookResult(ClassLoader cl, String cls, String method, Object... types) {
        try {
            XposedHelpers.findAndHookMethod(cls, cl, method, append(types, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    param.setResult(LEVEL);
                    XposedBridge.log("MHO Hook: " + cls + "." + method + " -> " + LEVEL);
                }
            }));
            XposedBridge.log("MHO Hook: installed result hook " + cls + "." + method);
        } catch (Throwable t) {
            XposedBridge.log("MHO Hook: FAIL result " + cls + "." + method + " : " + t);
        }
    }

    private static void hookSet(ClassLoader cl, String cls, String method) {
        try {
            XposedHelpers.findAndHookMethod(cls, cl, method, int.class, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    int old = (Integer) param.args[0];
                    param.args[0] = LEVEL;
                    XposedBridge.log("MHO Hook: " + cls + "." + method + " " + old + " -> " + LEVEL);
                }
            });
            XposedBridge.log("MHO Hook: installed setter hook " + cls + "." + method);
        } catch (Throwable t) {
            XposedBridge.log("MHO Hook: FAIL setter " + cls + "." + method + " : " + t);
        }
    }

    private static void logArgs(ClassLoader cl, String cls, String method, Object... types) {
        try {
            XposedHelpers.findAndHookMethod(cls, cl, method, append(types, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    StringBuilder b = new StringBuilder();
                    for (Object a : param.args) b.append(a).append(',');
                    XposedBridge.log("MHO Hook: " + cls + "." + method + " args=" + b);
                }
            }));
        } catch (Throwable t) {
            XposedBridge.log("MHO Hook: FAIL log " + cls + "." + method + " : " + t);
        }
    }

    private static Object[] append(Object[] a, Object x) {
        Object[] r = new Object[a.length + 1];
        System.arraycopy(a, 0, r, 0, a.length);
        r[a.length] = x;
        return r;
    }
}
