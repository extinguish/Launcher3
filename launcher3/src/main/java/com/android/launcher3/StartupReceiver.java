package com.android.launcher3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartupReceiver extends BroadcastReceiver {

    static final String SYSTEM_READY = "com.android.launcher3.SYSTEM_READY";

    @Override
    public void onReceive(Context context, Intent intent) {

        // TODO: 关于系统当中的StickyBroadcastAndroid当中已经不建议使用了
        // TODO: 主要的原因就是由于不安全造成的
        // Sticky broadcasts should not be used.  They provide no security (anyone
        // can access them), no protection (anyone can modify them), and many other problems.
        // The recommended pattern is to use a non-sticky broadcast to report that <em>something</em>
        // has changed, with another mechanism for apps to retrieve the current value whenever
        // desired.
        context.sendStickyBroadcast(new Intent(SYSTEM_READY));
    }
}
