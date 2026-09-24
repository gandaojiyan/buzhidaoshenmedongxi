package com.bbk.updater.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.bbk.updater.utils.JumpUtils;

/* JADX INFO: loaded from: classes.dex */
public class UpdateTempActivity extends Activity {
    private static final String START_FROM_SETTINGS = "com.vivo.action.ACTION_START_UPDATEACTIVITY";
    private static final String TAG = "Updater/UpdateTempActivity";

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = new Intent(START_FROM_SETTINGS);
        intent.setPackage(getPackageName());
        intent.setType("application/zip");
        JumpUtils.startActivitySafety(this, intent);
        finish();
    }
}
