package com.bbk.updater;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.provider.Settings;
import android.text.TextUtils;
import com.bbk.local.strategy.PackageCopyStrategy;
import com.bbk.updater.check.strategy.UpdateCheckStrategy;
import com.bbk.updater.codemanagement.ComponentManagerStrategy;
import com.bbk.updater.config.ConfigEngine;
import com.bbk.updater.cota.CotaStandardStrategy;
import com.bbk.updater.countrycode.CountryCodeStrategy;
import com.bbk.updater.download.strategy.AutoDownloadStrategy;
import com.bbk.updater.download.strategy.DownloadStrategy;
import com.bbk.updater.install.strategy.BaseUpdateStrategy;
import com.bbk.updater.install.strategy.DemoProductStrategy;
import com.bbk.updater.install.strategy.EnhancedUpdateStrategy;
import com.bbk.updater.install.strategy.ShutdownAndUpdateStrategy;
import com.bbk.updater.install.strategy.SmartInstallStrategy;
import com.bbk.updater.install.strategy.UpdateByOtherAppStrategy;
import com.bbk.updater.remote.c;
import com.bbk.updater.selfupgrade.SelfUpgradeStrategy;
import com.bbk.updater.strategy.CommonInstallStrategy;
import com.bbk.updater.strategy.OtherStrategy;
import com.bbk.updater.strategy.PrivacyTermsStrategy;
import com.bbk.updater.strategy.RedNoticeStrategy;
import com.bbk.updater.utils.APIVersionUtils;
import com.bbk.updater.utils.CommonUtils;
import com.bbk.updater.utils.ConstantsUtils;
import com.bbk.updater.utils.LogUtils;
import com.bbk.updater.utils.ReflectUtils;
import com.bbk.updater.utils.SDKUtils;
import com.bbk.updater.utils.UiUtils;
import com.bbk.updaterdsassistant.strategy.DSStrategy;
import com.vivo.push.PushManager;
import com.vivo.push.util.ContextDelegate;
import com.vivo.updaterassistant.strategy.Assistant4Strategy;
import com.vivo.updaterbaseframe.strategy.StrategyFactory;
import com.vivo.vgc.VgcSdkManager;
import vivo.app.epm.ExceptionReceiver;

/* JADX INFO: loaded from: classes.dex */
public class AppFeature extends Application {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private static AppFeature f148a;
    private boolean b;
    private Handler c;
    private a d;

    public static synchronized AppFeature a() {
        return f148a;
    }

    @Override // android.app.Application
    public void onCreate() {
        super.onCreate();
        LogUtils.eventLog("Application create");
        f148a = this;
        if ((APIVersionUtils.isOverAndroidP() && !CommonUtils.isFBE() && !CommonUtils.isUserUnlocked(this)) || !CommonUtils.isCurrentDefaultUser()) {
            System.exit(0);
        }
        CommonUtils.init(this);
        LogUtils.i("Updater/Application", "process name: " + CommonUtils.getProcessName());
        if (CommonUtils.isMainProcess()) {
            a((Context) this);
            c();
            if (com.bbk.updater.install.a.a.a()) {
                b((Context) this);
            }
            com.bbk.updater.download.a.a(this);
            ConfigEngine.getInstance(this);
            com.bbk.vgc.a.a.a();
            StrategyFactory.getInstance(this);
            b(this);
            CommonUtils.startRemoteService(this);
            if (PushManager.getInstance(this).isEnablePush()) {
                PushManager.getInstance(this).initialize();
            }
            com.vivo.updaterbaseframe.c.a.a().a((Context) this, true);
            e();
            d();
            SDKUtils.initTipsSDK(this);
        } else if (CommonUtils.isRemoteProcess()) {
            if (com.bbk.updater.codemanagement.a.e(this)) {
                c.a(this);
            } else {
                LogUtils.e("Updater/Application", "not allowed to start kill self!!!!");
                Process.killProcess(Process.myPid());
            }
        }
        if (Build.VERSION.SDK_INT < 31 || !ConstantsUtils.ISEXPORT) {
            return;
        }
        a(APIVersionUtils.isSupportMaterialYou(this));
        this.d = new a(null);
        getContentResolver().registerContentObserver(Settings.Secure.getUriFor("theme_customization_overlay_packages"), false, this.d);
    }

    public void a(boolean z) {
        LogUtils.i("Updater/MaterailYou", "isOpen:" + z);
        if (this.b == z) {
            return;
        }
        this.b = z;
        UiUtils.applyToActivitiesIfAvailable(this, z ? 0 : R.style.main_style_without_material_you);
    }

    public boolean b() {
        return this.b;
    }

    private void a(Context context) {
        StrategyFactory strategyFactory = StrategyFactory.getInstance(context);
        strategyFactory.addStrategy(UpdateCheckStrategy.class);
        strategyFactory.addStrategy(DownloadStrategy.class);
        strategyFactory.addStrategy(AutoDownloadStrategy.class);
        strategyFactory.addStrategy(RedNoticeStrategy.class);
        strategyFactory.addStrategy(BaseUpdateStrategy.class);
        strategyFactory.addStrategy(DemoProductStrategy.class);
        strategyFactory.addStrategy(Assistant4Strategy.class);
        strategyFactory.addStrategy(EnhancedUpdateStrategy.class);
        strategyFactory.addStrategy(DSStrategy.class);
        strategyFactory.addStrategy(OtherStrategy.class);
        strategyFactory.addStrategy(UpdateByOtherAppStrategy.class);
        strategyFactory.addStrategy(PrivacyTermsStrategy.class);
        strategyFactory.addStrategy(ShutdownAndUpdateStrategy.class);
        strategyFactory.addStrategy(SmartInstallStrategy.class);
        strategyFactory.addStrategy(CommonInstallStrategy.class);
        strategyFactory.addStrategy(SelfUpgradeStrategy.class);
        strategyFactory.addStrategy(PackageCopyStrategy.class);
        strategyFactory.addStrategy(ComponentManagerStrategy.class);
        strategyFactory.addStrategy(CountryCodeStrategy.class);
        strategyFactory.addStrategy(CotaStandardStrategy.class);
        strategyFactory.onAppFeaterSetUp();
        com.vivo.updaterbaseframe.c.a.a().a((Context) this, true);
    }

    @Override // android.app.Application
    public void onTerminate() {
        LogUtils.i("Updater/Application", "Application is killed !");
        super.onTerminate();
        if (this.d != null) {
            getContentResolver().unregisterContentObserver(this.d);
        }
    }

    @Override // android.app.Application, android.content.ComponentCallbacks
    public void onLowMemory() {
        LogUtils.i("Updater/Application", "Application onLowMemory !");
        super.onLowMemory();
    }

    private void c() {
        com.bbk.updater.b.b.a(new Runnable() { // from class: com.bbk.updater.AppFeature.1
            @Override // java.lang.Runnable
            public void run() {
                com.bbk.updater.codemanagement.a.a(AppFeature.a());
            }
        });
    }

    private void b(Context context) {
        com.bbk.updater.install.abinstall.b.a(context);
    }

    @Override // android.content.ContextWrapper
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(c(context));
    }

    private Context c(Context context) {
        if (!CommonUtils.isFBE()) {
            return context;
        }
        Context context2 = (Context) ReflectUtils.invokeDeclaredMethod("android.content.Context", context, "createDeviceProtectedStorageContext", new Object[0]);
        LogUtils.i("Updater/Application", "isFBE, application context change to de.");
        ContextDelegate.setEnable(true);
        return context2;
    }

    private void d() {
        registerReceiver(new BroadcastReceiver() { // from class: com.bbk.updater.AppFeature.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                if (intent == null || TextUtils.isEmpty(intent.getStringExtra(ExceptionReceiver.KEY_REASON))) {
                    return;
                }
                String stringExtra = intent.getStringExtra(ExceptionReceiver.KEY_REASON);
                byte b = -1;
                if (stringExtra.hashCode() == 1092716832 && stringExtra.equals("homekey")) {
                    b = 0;
                }
                if (b != 0) {
                    return;
                }
                if (AppFeature.this.c == null) {
                    AppFeature.this.c = new Handler();
                }
                AppFeature.this.c.removeCallbacksAndMessages(null);
                AppFeature.this.c.postDelayed(new Runnable() { // from class: com.bbk.updater.AppFeature.2.1
                    @Override // java.lang.Runnable
                    public void run() {
                        if (CommonUtils.isUpdaterMainActivity(AppFeature.this.getApplicationContext())) {
                            return;
                        }
                        com.bbk.a.a();
                    }
                }, 500L);
            }
        }, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
    }

    private void b(final AppFeature appFeature) {
        appFeature.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() { // from class: com.bbk.updater.AppFeature.3
            @Override // android.app.Application.ActivityLifecycleCallbacks
            public void onActivityDestroyed(Activity activity) {
            }

            @Override // android.app.Application.ActivityLifecycleCallbacks
            public void onActivityPaused(Activity activity) {
            }

            @Override // android.app.Application.ActivityLifecycleCallbacks
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
            }

            @Override // android.app.Application.ActivityLifecycleCallbacks
            public void onActivityStarted(Activity activity) {
            }

            @Override // android.app.Application.ActivityLifecycleCallbacks
            public void onActivityStopped(Activity activity) {
            }

            /* JADX WARN: Multi-variable type inference failed */
            @Override // android.app.Application.ActivityLifecycleCallbacks
            public void onActivityCreated(Activity activity, Bundle bundle) {
                if (activity != null) {
                    StrategyFactory.getInstance(appFeature).onActivityCreated(activity.getClass(), activity.getIntent());
                }
            }

            /* JADX WARN: Multi-variable type inference failed */
            @Override // android.app.Application.ActivityLifecycleCallbacks
            public void onActivityResumed(Activity activity) {
                if (activity != null) {
                    StrategyFactory.getInstance(appFeature).onActivityResumed(activity.getClass());
                }
            }
        });
    }

    private void e() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(VgcSdkManager.ACTION_CBS_UPDATE_RES);
        registerReceiver(new BroadcastReceiver() { // from class: com.bbk.updater.AppFeature.4
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                LogUtils.i("Updater/Application", "onReceive: " + intent.getAction());
                com.bbk.vgc.a.a.a().a(context, intent);
            }
        }, intentFilter);
    }

    static class a extends ContentObserver {
        public a(Handler handler) {
            super(new Handler(Looper.getMainLooper()));
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean z, Uri uri) {
            LogUtils.i("Updater/MaterailYou", "switch onChange:" + z + "|" + uri.getPath() + "|" + Thread.currentThread());
            AppFeature.a().a(APIVersionUtils.isSupportMaterialYou(AppFeature.a()));
        }
    }
}
