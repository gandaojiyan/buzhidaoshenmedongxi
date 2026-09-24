package com.bbk.updater.check.strategy;

import android.os.SystemClock;
import com.bbk.updater.bean.UpdateInfo;
import com.bbk.updater.bean.VgcUpdateInfo;
import com.bbk.updater.check.a.b;
import com.bbk.updater.config.Configs;
import com.bbk.updater.utils.APIVersionUtils;
import com.bbk.updater.utils.CommonUtils;
import com.bbk.updater.utils.ConstantsUtils;
import com.bbk.updater.utils.LogUtils;
import com.bbk.updater.utils.PrefsUtils;
import com.bbk.updater.utils.SimpleScheduler;
import com.bbk.updater.utils.StringUtils;
import com.bbk.updater.utils.VersionUtils;
import com.bbk.updater.utils.WebHelper;
import com.vivo.updaterbaseframe.a.c;
import com.vivo.updaterbaseframe.strategy.a;
import com.vivo.updaterbaseframe.utils.BaseFrameUtils;
import java.util.Iterator;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class UpdateCheckStrategy extends a {
    @Override // com.vivo.updaterbaseframe.strategy.a
    public boolean onAppFeaterSetUp() {
        registerStaticBroadcast("new.com.bbk.updater.action.AUTO_CHECK");
        registerStaticBroadcast("com.bbk.updater.action.CHECK_INSTANTLY");
        registerStaticBroadcast("com.bbk.updater.auto.check");
        registerStaticBroadcast("com.bbk.updater.action.AUTO_CHECK_HOURLY");
        registerStaticBroadcast("com.bbk.updater.action.TIME_SET_CHECK");
        registerStaticBroadcast(ConstantsUtils.BroadCastReceiverAction.PUSH_SERVICE_ACTION);
        registerStaticBroadcast("com.vivo.updater.action.POWER_CONNECT");
        registerStaticBroadcast("com.vivo.updater.RECOVERY_EXISTING_PLAN");
        registerStaticBroadcast("com.bbk.updater.action.AUTO_CHECK_BOOT_FIRST");
        if (!ConstantsUtils.ISEXPORT && !APIVersionUtils.isPad()) {
            registerStaticBroadcast("com.bbk.updater.action.UPDATE_CHECK_CONFIG_EFFECT");
        }
        if (!isAutoCheckRealFinished()) {
            onStaticBroadCastReceive("com.bbk.updater.action.CHECK_INSTANTLY", null);
        }
        registerStaticBroadcast("com.vivo.updater.action.POLICY_MANAGER_STATE_CHANGED");
        return super.onAppFeaterSetUp();
    }

    @Override // com.vivo.updaterbaseframe.strategy.a
    public boolean onBootComplete() {
        setAutocheckAlarmAfterBoot();
        return super.onBootComplete();
    }

    @Override // com.vivo.updaterbaseframe.strategy.a
    public boolean onNetworkChanged(boolean z, boolean z2) {
        long jRandom = (long) ((Math.random() * 10000.0d) + 5000.0d);
        LogUtils.d("Updater/strategy/UpdateCheckStrategy", "delay=" + jRandom);
        SimpleScheduler.runOnMainThread(new Runnable() { // from class: com.bbk.updater.check.strategy.UpdateCheckStrategy.1
            @Override // java.lang.Runnable
            public void run() {
                UpdateCheckStrategy.this.startAutoCheck(true, BaseFrameUtils.CheckTrigeType.NETWORK_CHANGED);
            }
        }, jRandom);
        return super.onNetworkChanged(z, z2);
    }

    @Override // com.vivo.updaterbaseframe.strategy.a
    public boolean onCotaCustTypeChanged() {
        startAutoCheck(false, BaseFrameUtils.CheckTrigeType.COTA_TYPE_CHANGE);
        return super.onCotaCustTypeChanged();
    }

    @Override // com.vivo.updaterbaseframe.strategy.a
    public boolean onCheckStart(boolean z, String str) {
        PrefsUtils.putLong(getContext(), PrefsUtils.Check.KEY_TIMESTAMP_OF_LAST_CHECK, System.currentTimeMillis());
        return super.onCheckStart(z, str);
    }

    @Override // com.vivo.updaterbaseframe.strategy.a
    public boolean onCheckEnd(int i, boolean z, c cVar, c cVar2, c cVar3, c cVar4, c cVar5, c cVar6, String str, BaseFrameUtils.CheckTrigeType checkTrigeType) {
        if (z) {
            traceAutoCheckCycle(true);
        }
        return super.onCheckEnd(i, z, cVar, cVar2, cVar3, cVar4, cVar5, cVar6, str, checkTrigeType);
    }

    @Override // com.vivo.updaterbaseframe.strategy.a
    public boolean onNewUpdatePackageChecked(c cVar, c cVar2, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6) {
        saveFileSizeWithVersion(cVar, cVar2);
        if (z5) {
            WebHelper.getInstance(getContext()).getLogHtmlPathAndCacheIt(cVar, cVar2);
        }
        return super.onNewUpdatePackageChecked(cVar, cVar2, z, z2, z3, z4, z5, z6);
    }

    private void saveFileSizeWithVersion(c cVar, c cVar2) {
        String versionSplice;
        VgcUpdateInfo vgcUpdateInfo = cVar2 instanceof VgcUpdateInfo ? (VgcUpdateInfo) cVar2 : null;
        long reservedStorage = cVar instanceof UpdateInfo ? ((UpdateInfo) cVar).getReservedStorage() : 0L;
        long jLongValue = CommonUtils.getTotalPackageLength(cVar, cVar2).longValue();
        if (cVar != null && vgcUpdateInfo != null) {
            versionSplice = VersionUtils.getVersionSplice(cVar.getVersion(), vgcUpdateInfo.getVersion());
        } else if (cVar != null) {
            versionSplice = VersionUtils.getVersionSplice(cVar.getVersion(), null);
        } else {
            versionSplice = vgcUpdateInfo != null ? VersionUtils.getVersionSplice(null, vgcUpdateInfo.getVersion(), vgcUpdateInfo.getVersionShow()) : "";
        }
        String softVersion = VersionUtils.getSoftVersion();
        String vgcSoftVersion = VersionUtils.getVgcSoftVersion();
        String versionSplice2 = VersionUtils.getVersionSplice(softVersion, vgcSoftVersion);
        Iterator<Map.Entry<String, ?>> it = PrefsUtils.getAll(getContext(), PrefsUtils.Prefs.Main).entrySet().iterator();
        while (it.hasNext()) {
            String key = it.next().getKey();
            if (key != null && key.startsWith(PrefsUtils.Other.KEY_LOG_NOTE_FILE_SIZE)) {
                LogUtils.d("Updater/strategy/UpdateCheckStrategy", "key:" + key + ", version:" + versionSplice + " currentVersion:" + versionSplice2);
                if (!StringUtils.stringContains(key, versionSplice) && !StringUtils.stringContains(key, versionSplice2) && !StringUtils.stringContains(key, softVersion) && !StringUtils.stringContains(key, vgcSoftVersion)) {
                    PrefsUtils.removePrefs(getContext(), key);
                }
            }
        }
        PrefsUtils.putLongApply(getContext(), PrefsUtils.Other.KEY_LOG_NOTE_FILE_SIZE + versionSplice, reservedStorage + jLongValue);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Removed duplicated region for block: B:41:0x009f  */
    @Override // com.vivo.updaterbaseframe.strategy.a
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean onStaticBroadCastReceive(java.lang.String r4, android.content.Intent r5) {
        /*
            Method dump skipped, instruction units count: 336
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.bbk.updater.check.strategy.UpdateCheckStrategy.onStaticBroadCastReceive(java.lang.String, android.content.Intent):boolean");
    }

    private void traceAutoCheckCycle(boolean z) {
        PrefsUtils.putBoolean(getContext(), PrefsUtils.Check.KEY_AUTO_CHECK_IS_FINISHED, z);
    }

    private boolean isAutoCheckRealFinished() {
        return PrefsUtils.getBoolean(getContext(), PrefsUtils.Check.KEY_AUTO_CHECK_IS_FINISHED, true);
    }

    /* JADX WARN: Removed duplicated region for block: B:12:0x0055  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private boolean isTimeToAutoCheck(com.vivo.updaterbaseframe.utils.BaseFrameUtils.CheckTrigeType r10) {
        /*
            r9 = this;
            android.content.Context r0 = r9.getContext()
            boolean r0 = com.bbk.updater.utils.CommonUtils.isNetworkConnect(r0)
            r1 = 1
            java.lang.String r2 = "Updater/strategy/UpdateCheckStrategy"
            r3 = 0
            if (r0 == 0) goto L55
            long r4 = java.lang.System.currentTimeMillis()
            android.content.Context r0 = r9.getContext()
            r6 = 0
            java.lang.String r8 = "time_stamp_of_last_check"
            long r6 = com.bbk.updater.utils.PrefsUtils.getLong(r0, r8, r6)
            long r4 = r4 - r6
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r6 = "Network is connected, interval is "
            r0.append(r6)
            r0.append(r4)
            java.lang.String r0 = r0.toString()
            com.bbk.updater.utils.LogUtils.i(r2, r0)
            android.content.Context r0 = r9.getContext()
            boolean r0 = com.bbk.updater.utils.CommonUtils.isNetworkWifi(r0)
            if (r0 == 0) goto L49
            long r4 = java.lang.Math.abs(r4)
            r6 = 10800000(0xa4cb80, double:5.335909E-317)
            int r0 = (r4 > r6 ? 1 : (r4 == r6 ? 0 : -1))
            if (r0 < 0) goto L55
            goto L56
        L49:
            long r4 = java.lang.Math.abs(r4)
            r6 = 21600000(0x1499700, double:1.0671818E-316)
            int r0 = (r4 > r6 ? 1 : (r4 == r6 ? 0 : -1))
            if (r0 < 0) goto L55
            goto L56
        L55:
            r1 = r3
        L56:
            if (r1 == 0) goto L6e
            boolean r10 = r9.needInterceptAutoCheck(r10)
            if (r10 == 0) goto L6e
            com.bbk.updater.config.Configs$UpdateCheckConfig r10 = com.bbk.updater.config.Configs.UpdateCheckConfig
            long r4 = java.lang.System.currentTimeMillis()
            boolean r10 = r10.isDelayUpdateCheck(r4)
            if (r10 == 0) goto L6e
            r9.setDelayCheck()
            goto L6f
        L6e:
            r3 = r1
        L6f:
            java.lang.StringBuilder r9 = new java.lang.StringBuilder
            r9.<init>()
            java.lang.String r10 = "Is it time to check? "
            r9.append(r10)
            r9.append(r3)
            java.lang.String r9 = r9.toString()
            com.bbk.updater.utils.LogUtils.i(r2, r9)
            return r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.bbk.updater.check.strategy.UpdateCheckStrategy.isTimeToAutoCheck(com.vivo.updaterbaseframe.utils.BaseFrameUtils$CheckTrigeType):boolean");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startAutoCheck(boolean z, BaseFrameUtils.CheckTrigeType checkTrigeType) {
        if (CommonUtils.isForbidUseNetForRoaming(getContext())) {
            LogUtils.i("Updater/strategy/UpdateCheckStrategy", "Forbid checking! reason:roaming");
            return;
        }
        if (CommonUtils.isUpdating()) {
            LogUtils.i("Updater/strategy/UpdateCheckStrategy", "updating !!!");
            return;
        }
        boolean zIsNetworkConnect = CommonUtils.isNetworkConnect(getContext());
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        LogUtils.i("Updater/strategy/UpdateCheckStrategy", "AutoCheck Trige arrival, trigeType: " + checkTrigeType + ", elaplseTime: " + jElapsedRealtime);
        com.bbk.updaterdsassistant.b.a.a(getContext(), PrefsUtils.Check.KEY_AUTOCHECK_TRIGER_TIMES);
        if (!zIsNetworkConnect || jElapsedRealtime <= ConstantsUtils.ONE_MINUTE_TIME) {
            if (zIsNetworkConnect) {
                com.bbk.updaterdsassistant.b.a.a(getContext(), PrefsUtils.Check.KEY_CANNOT_AUTOCHECK_DUE_BOOT);
                return;
            } else {
                com.bbk.updaterdsassistant.b.a.a(getContext(), PrefsUtils.Check.KEY_CANNOT_AUTOCHECK_DUE_NET);
                return;
            }
        }
        if (!z || isTimeToAutoCheck(checkTrigeType)) {
            b bVar = new b(getContext());
            bVar.a(checkTrigeType);
            bVar.b(true);
            bVar.execute(new Void[0]);
            return;
        }
        com.bbk.updaterdsassistant.b.a.a(getContext(), PrefsUtils.Check.KEY_CANNOT_AUTOCHECK_DUE_TIME);
    }

    private boolean needInterceptAutoCheck(BaseFrameUtils.CheckTrigeType checkTrigeType) {
        LogUtils.i("Updater/strategy/UpdateCheckStrategy", "auto check type: " + checkTrigeType);
        if (ConstantsUtils.ISEXPORT || APIVersionUtils.isPad()) {
            return false;
        }
        return checkTrigeType == BaseFrameUtils.CheckTrigeType.NETWORK_CHANGED || checkTrigeType == BaseFrameUtils.CheckTrigeType.REGULAR_ALARM || checkTrigeType == BaseFrameUtils.CheckTrigeType.HOURLY_ALARM || checkTrigeType == BaseFrameUtils.CheckTrigeType.POWER_CONNECTED || checkTrigeType == BaseFrameUtils.CheckTrigeType.TIME_SET;
    }

    private boolean isInDelayCheckScope() {
        long jAbs = Math.abs(System.currentTimeMillis() - PrefsUtils.getLong(getContext(), PrefsUtils.Check.KEY_LAST_TIME_SET_DELAY_CHECK, 0L));
        LogUtils.d("Updater/strategy/UpdateCheckStrategy", "delayCheckIntervalTime h: " + (jAbs / ConstantsUtils.ONE_HOUR_TIME));
        return jAbs < (Configs.UpdateCheckConfig.getBlockTime() * 10) * 1000;
    }

    private synchronized void setDelayCheck() {
        long blockTime;
        if (isInDelayCheckScope()) {
            return;
        }
        String imei = VersionUtils.getIMEI(getContext());
        long jRandom = (long) (Math.random() * 10.0d * 512.0d);
        try {
            blockTime = Configs.UpdateCheckConfig.getBlockTime();
        } catch (Exception e) {
            LogUtils.i("Updater/strategy/UpdateCheckStrategy", "updateCheckDelay exception：" + e.toString());
        }
        if (Character.isDigit(imei.charAt(imei.length() - 1))) {
            int iCharAt = imei.charAt(imei.length() - 1) - '0';
            jRandom = (long) ((((long) iCharAt) * blockTime) + (Math.random() * blockTime));
            long j = jRandom;
            LogUtils.i("Updater/strategy/UpdateCheckStrategy", "setDelayCheck intervalTime: " + j);
            CommonUtils.setAlarmClock(getContext(), "com.bbk.updater.action.UPDATE_CHECK_CONFIG_EFFECT", j, false, true);
            PrefsUtils.putLong(getContext(), PrefsUtils.Check.KEY_LAST_TIME_SET_DELAY_CHECK, System.currentTimeMillis());
            return;
        }
        long j2 = jRandom;
        LogUtils.i("Updater/strategy/UpdateCheckStrategy", "setDelayCheck intervalTime: " + j2);
        CommonUtils.setAlarmClock(getContext(), "com.bbk.updater.action.UPDATE_CHECK_CONFIG_EFFECT", j2, false, true);
        PrefsUtils.putLong(getContext(), PrefsUtils.Check.KEY_LAST_TIME_SET_DELAY_CHECK, System.currentTimeMillis());
        return;
    }

    private void setHourlyCheckAlarm() {
        CommonUtils.setAlarmClock(getContext(), "com.bbk.updater.action.AUTO_CHECK_HOURLY", (long) ((Math.random() * 60.0d * 30.0d) + 10800.0d), false);
    }

    private void setAutocheckAlarmAfterBoot() {
        CommonUtils.setAlarmClock(getContext(), "com.bbk.updater.action.AUTO_CHECK_BOOT_FIRST", 60L, false);
    }
}
