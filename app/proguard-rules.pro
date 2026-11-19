# Glance creates widget action callbacks by class name via reflection
# (actionRunCallback<T>()). R8 must keep them and their no-arg constructor,
# otherwise taps in the widget silently do nothing in release builds.
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    <init>();
}

# WorkManager instantiates workers via reflection as well.
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}
