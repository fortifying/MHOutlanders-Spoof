package de.robv.android.xposed;
public abstract class XC_MethodHook {
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}
    public static final class MethodHookParam {
        public Object[] args;
        public Object thisObject;
        public java.lang.reflect.Member method;
        private Object result;
        private Throwable throwable;
        public Object getResult() { return result; }
        public void setResult(Object r) { result = r; }
        public Throwable getThrowable() { return throwable; }
        public boolean hasThrowable() { return throwable != null; }
    }
    public static class Unhook { public void unhook() {} }
}
