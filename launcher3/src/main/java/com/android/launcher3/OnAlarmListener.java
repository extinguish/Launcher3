package com.android.launcher3;

/**
 * 这是一个回调接口，我们可以通过实现这个接口，从而完成Alarm类的功能回调
 * 而Alarm的主要功能就类似于一个简单的BroadcastReceiver来使用，只不过比
 * BroadcastReceiver更加精确，同时实时性很好！！！
 */
public interface OnAlarmListener {
    public void onAlarm(Alarm alarm);
}
