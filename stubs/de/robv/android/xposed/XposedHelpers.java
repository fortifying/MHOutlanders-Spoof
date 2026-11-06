package de.robv.android.xposed;
public final class XposedHelpers {
    public static Class<?> findClass(String name, ClassLoader cl) { return null; }
    public static Class<?> findClassIfExists(String name, ClassLoader cl) { return null; }
    public static XC_MethodHook.Unhook findAndHookMethod(String className, ClassLoader cl, String methodName, Object... parameterTypesAndCallback) { return null; }
    public static XC_MethodHook.Unhook findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) { return null; }
    public static void setStaticObjectField(Class<?> clazz, String fieldName, Object value) {}
}
