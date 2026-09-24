package com.bbk.updater.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;
import android.provider.Settings;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.PathInterpolator;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.bbk.updater.R;
import com.bbk.updater.rx.event.CheckEvent;
import com.bbk.updater.rx.event.ClickEvent;
import com.bbk.updater.rx.event.DialogEvent;
import com.bbk.updater.ui.customactivity.CustomUpdateActivity;
import com.bbk.updater.ui.customactivity.DialogButtonStyleRom13;
import com.bbk.updater.ui.fragment.UpdateFragment;
import com.bbk.updater.ui.fragment.UpdateFragment13;
import com.bbk.updater.utils.APIVersionUtils;
import com.bbk.updater.utils.CommonUtils;
import com.bbk.updater.utils.ConstantsUtils;
import com.bbk.updater.utils.JumpUtils;
import com.bbk.updater.utils.LogUtils;
import com.bbk.updater.utils.PrefsUtils;
import com.bbk.updater.utils.UiUtils;
import com.bbk.vgc.a.a;
import com.vivo.updaterbaseframe.rx.RxBus;

/* JADX INFO: loaded from: classes.dex */
public class UpdateActivity extends CustomUpdateActivity {
    public static final String ACTION = "com.bbk.updater.action.START_UPDATERACTIVITY";
    public static final String APP_UPGRADE_PACKAGE_NAME = "com.iqoo.trafficupgrade";
    private static final String CLICK_MORE_BUTTON_ACTION = "com.vivo.settings.action.CLICK_MORE_BUTTON";
    public static final int DEFAULT_DURATION = 300;
    public static final int DIALOG_INTELLIGENT_INSTALLATION = 1004;
    public static final int DIALOG_MOBILE_NETWORK = 1002;
    public static final int DIALOG_MOBILE_NETWORK_2G = 1009;
    public static final int DIALOG_MOBILE_NETWORK_MAXIMUM_SIZE = 1008;
    public static final int DIALOG_MOBILE_NETWORK_NOT_ALLOW_DOWNLOAD = 1011;
    public static final int DIALOG_MOBILE_NETWORK_PEAK_TIME = 1007;
    public static final int DIALOG_NETWORK_DISCONNECT = 1001;
    public static final int DIALOG_OPEN_AUTO_CHECK_UPGRADE = 1005;
    public static final int DIALOG_REQUEST_PAUSE_CONFIRM = 1010;
    public static final int DIALOG_ROAMING_NOT_ALLOW_DOWNLOAD = 1012;
    public static final int DIALOG_ROAMING_NOT_ALLOW_UPDATES = 1013;
    public static final int DIALOG_USE_DATA_NETWORK_CHECK_UPGRADE = 1006;
    private static final String TAG = "Updater/UpdateActivity";
    protected Handler mHandler;
    private boolean mIsLargeScreen;
    private boolean mIsPadOcean;
    private BroadcastReceiver mNetJumpReceiver;
    private RelativeLayout mRoot;
    private boolean mShutdownShowed;
    private UpdateFragment mUpdateFragment;
    private UpdateFragment13 mUpdateFragment13;
    private UpdatePresenter mUpdatePresenter;
    private DialogInterface.OnClickListener mClickListener = new DialogInterface.OnClickListener() { // from class: com.bbk.updater.ui.UpdateActivity.4
        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i) {
            if (dialogInterface == UpdateActivity.this.mDialogNetworkDisconnect) {
                if (i == -2) {
                    UpdateActivity.this.mDialogNetworkDisconnect.dismiss();
                    return;
                }
                if (i != -1) {
                    return;
                }
                JumpUtils.jumpToConnectNetwork(UpdateActivity.this);
                if (!JumpUtils.isDialogStyleSettingWifi() && !UiUtils.isBreakMode(UpdateActivity.this)) {
                    UpdateActivity.this.finish();
                    return;
                }
                LogUtils.i(UpdateActivity.TAG, "registerNetJumpReceiver");
                UpdateActivity updateActivity = UpdateActivity.this;
                updateActivity.registerNetJumpReceiver(updateActivity);
                return;
            }
            if (dialogInterface == UpdateActivity.this.mDialogMobileNetwork) {
                if (i == -2) {
                    RxBus.a().c(new ClickEvent(9));
                } else if (i == -1) {
                    RxBus.a().c(new ClickEvent(2));
                }
                UpdateActivity.this.mDialogMobileNetwork.dismiss();
                return;
            }
            if (dialogInterface == UpdateActivity.this.mDialogIntelligentInstallation) {
                if (i == -2) {
                    RxBus.a().c(new DialogEvent(18, ConstantsUtils.DialogType.OPEN_SMART_SWITCH_INDUCE, ""));
                } else if (i == -1) {
                    RxBus.a().c(new DialogEvent(17, ConstantsUtils.DialogType.OPEN_SMART_SWITCH_INDUCE, ""));
                }
                UpdateActivity.this.mDialogIntelligentInstallation.dismiss();
                return;
            }
            if (dialogInterface == UpdateActivity.this.mDialogOpenAutoCheckUpgrade) {
                if (i == -2) {
                    RxBus.a().c(new CheckEvent(4096));
                } else if (i == -1) {
                    Settings.Global.putInt(UpdateActivity.this.getContentResolver(), "updater_auto_check_upgrade_checked", 1);
                    RxBus.a().c(new CheckEvent(4096));
                }
                UpdateActivity.this.mDialogOpenAutoCheckUpgrade.dismiss();
                return;
            }
            if (dialogInterface == UpdateActivity.this.mDialogUseDataCheckUpgrade) {
                if (i == -2) {
                    RxBus.a().c(new CheckEvent(8192));
                    return;
                } else {
                    if (i != -1) {
                        return;
                    }
                    CommonUtils.putIntIntoSettings(UpdateActivity.this, "updater_use_data_net_check_upgrade_checked", 1);
                    CheckEvent checkEvent = new CheckEvent(4096);
                    checkEvent.setDataNetTypeCheck(false);
                    RxBus.a().c(checkEvent);
                    return;
                }
            }
            if (dialogInterface != UpdateActivity.this.mDialogMobileNetworkNotAllowDownload) {
                if (dialogInterface == UpdateActivity.this.mDialogRoamingNotAllowDownload || dialogInterface == UpdateActivity.this.mDialogRoamingNotAllowUpdates) {
                    if (i != -1) {
                        return;
                    }
                    JumpUtils.startActivitySafety(UpdateActivity.this, new Intent("android.settings.WIFI_SETTINGS"));
                    return;
                }
                if (dialogInterface != UpdateActivity.this.mDialogMobileNetwork2G) {
                    if (dialogInterface != UpdateActivity.this.mDialogMobileNetworkPeakTime) {
                        if (dialogInterface == UpdateActivity.this.mDialogMobileNetworkMaximumSize) {
                            if (i == -2) {
                                RxBus.a().c(new ClickEvent(9));
                            } else if (i == -1) {
                                RxBus.a().c(new ClickEvent(2));
                            }
                            UpdateActivity.this.mDialogMobileNetworkMaximumSize.dismiss();
                            return;
                        }
                        if (dialogInterface == UpdateActivity.this.mDialogDownloadPauseConfirm) {
                            if (i == -1) {
                                RxBus.a().c(new ClickEvent(27));
                            }
                            UpdateActivity.this.mDialogDownloadPauseConfirm.dismiss();
                            return;
                        }
                        return;
                    }
                    if (i != -2) {
                        if (i != -1) {
                            return;
                        }
                        JumpUtils.startActivitySafety(UpdateActivity.this, new Intent("android.settings.WIFI_SETTINGS"));
                    }
                    RxBus.a().c(new ClickEvent(9));
                    return;
                }
                RxBus.a().c(new ClickEvent(9));
                return;
            }
            if (i != -2) {
                if (i != -1) {
                    return;
                }
                JumpUtils.startActivitySafety(UpdateActivity.this, new Intent("android.settings.WIFI_SETTINGS"));
            }
            RxBus.a().c(new ClickEvent(9));
        }
    };
    private DialogInterface.OnCancelListener mCancelListener = new DialogInterface.OnCancelListener() { // from class: com.bbk.updater.ui.UpdateActivity.5
        @Override // android.content.DialogInterface.OnCancelListener
        public void onCancel(DialogInterface dialogInterface) {
            if (dialogInterface != UpdateActivity.this.mDialogOpenAutoCheckUpgrade) {
                if (dialogInterface != UpdateActivity.this.mDialogUseDataCheckUpgrade) {
                    if (dialogInterface != UpdateActivity.this.mDialogMobileNetwork) {
                        if (dialogInterface != UpdateActivity.this.mDialogMobileNetworkNotAllowDownload) {
                            if (dialogInterface != UpdateActivity.this.mDialogRoamingNotAllowDownload && dialogInterface != UpdateActivity.this.mDialogRoamingNotAllowUpdates) {
                                if (dialogInterface != UpdateActivity.this.mDialogMobileNetwork2G) {
                                    if (dialogInterface != UpdateActivity.this.mDialogMobileNetworkPeakTime) {
                                        if (dialogInterface == UpdateActivity.this.mDialogMobileNetworkMaximumSize) {
                                            RxBus.a().c(new ClickEvent(9));
                                            return;
                                        }
                                        return;
                                    }
                                    RxBus.a().c(new ClickEvent(9));
                                    return;
                                }
                                RxBus.a().c(new ClickEvent(9));
                                return;
                            }
                            RxBus.a().c(new ClickEvent(9));
                            return;
                        }
                        RxBus.a().c(new ClickEvent(9));
                        return;
                    }
                    RxBus.a().c(new ClickEvent(9));
                    return;
                }
                RxBus.a().c(new CheckEvent(8192));
                return;
            }
            RxBus.a().c(new CheckEvent(4096));
            PrefsUtils.putBoolean(UpdateActivity.this, PrefsUtils.Check.KEY_OPEN_AUTO_CHECK_UPGRADE_PACKAGE_NO_MORE_PROMPT, false);
        }
    };
    private CompoundButton.OnCheckedChangeListener mCheckChangedListener = new CompoundButton.OnCheckedChangeListener() { // from class: com.bbk.updater.ui.UpdateActivity.6
        @Override // android.widget.CompoundButton.OnCheckedChangeListener
        public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
            PrefsUtils.putBoolean(UpdateActivity.this, PrefsUtils.Check.KEY_OPEN_AUTO_CHECK_UPGRADE_PACKAGE_NO_MORE_PROMPT, z);
        }
    };
    private boolean needFinish = false;
    private boolean onStop = false;

    @Override // com.bbk.updater.ui.customactivity.CustomUpdateActivity, com.bbk.updater.ui.baseactivity.BaseActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        LogUtils.i(TAG, "onCreate");
        super.onCreate(bundle);
        Window window = getWindow();
        if (window != null) {
            View decorView = window.getDecorView();
            window.setStatusBarColor(0);
            decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | 1280);
        }
        setContentView(R.layout.activity_main);
        this.mIsPadOcean = APIVersionUtils.isPadOcean();
        initView();
        startWork();
    }

    @Override // android.app.Activity
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
    }

    private void startWork() {
        this.mHandler = new Handler();
        Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() { // from class: com.bbk.updater.ui.UpdateActivity.1
            @Override // android.os.MessageQueue.IdleHandler
            public boolean queueIdle() {
                LogUtils.i(UpdateActivity.TAG, "IdleHandler queueIdle");
                if (UpdateActivity.this.mUpdatePresenter == null) {
                    UpdateActivity.this.mUpdatePresenter = new UpdatePresenter();
                    boolean booleanExtra = UpdateActivity.this.getIntent().getBooleanExtra(ConstantsUtils.START_FROM_NOTIFICATION, false);
                    boolean booleanExtra2 = UpdateActivity.this.getIntent().getBooleanExtra(ConstantsUtils.START_FROM_NEW_VERSION_DIALOG, false);
                    UpdateActivity.this.mUpdatePresenter.setFromTrialVersion(UpdateActivity.this.getIntent().getBooleanExtra("isTrialVersion", false));
                    UpdateActivity.this.mUpdatePresenter.setFromNotice(booleanExtra2 || booleanExtra);
                    if (APIVersionUtils.isOcean()) {
                        UpdateActivity.this.mUpdatePresenter.attachView((UpdateView) UpdateActivity.this.mUpdateFragment13);
                    } else {
                        UpdateActivity.this.mUpdatePresenter.attachView((UpdateView) UpdateActivity.this.mUpdateFragment);
                    }
                }
                return false;
            }
        });
    }

    @Override // android.app.Activity
    protected void onResume() {
        LogUtils.i(TAG, "onResume");
        super.onResume();
        this.onStop = false;
        unregisterNetJumpReceiver(this);
        CommonUtils.setUpdaterForegroundTag(true);
        UpdatePresenter updatePresenter = this.mUpdatePresenter;
        if (updatePresenter != null) {
            updatePresenter.onResume();
        }
    }

    @Override // android.app.Activity
    protected void onPause() {
        LogUtils.i(TAG, "onPause");
        super.onPause();
        CommonUtils.setUpdaterForegroundTag(false);
        UpdatePresenter updatePresenter = this.mUpdatePresenter;
        if (updatePresenter != null) {
            updatePresenter.onPause();
        }
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        if (APIVersionUtils.isFoldable() && UiUtils.isBreakMode(this)) {
            moveTaskToBack(true);
        }
        super.onBackPressed();
    }

    @Override // android.app.Activity
    protected void onStop() {
        this.onStop = true;
        LogUtils.i(TAG, "onStop");
        super.onStop();
    }

    @Override // com.bbk.updater.ui.baseactivity.BaseActivity, android.app.Activity
    protected void onDestroy() {
        LogUtils.i(TAG, "onDestroy");
        this.onStop = false;
        unregisterNetJumpReceiver(this);
        UpdatePresenter updatePresenter = this.mUpdatePresenter;
        if (updatePresenter != null) {
            updatePresenter.detachView();
        }
        dismissAllDialog();
        super.onDestroy();
    }

    @Override // android.app.Activity
    public void finish() {
        super.finish();
        JumpUtils.finishWithAnim(this);
    }

    @Override // android.app.Activity
    protected void onNewIntent(Intent intent) {
        LogUtils.i(TAG, "onNewIntent" + this.mUpdatePresenter);
        super.onNewIntent(intent);
        if (this.mUpdatePresenter == null) {
            return;
        }
        if (intent != null && intent.getBooleanExtra("isTrialVersion", false)) {
            LogUtils.i(TAG, "TrialVersion");
            this.mUpdatePresenter.setFromTrialVersion(true);
        } else {
            this.mUpdatePresenter.setFromTrialVersion(false);
        }
    }

    private void initView() {
        this.mRoot = (RelativeLayout) findViewById(R.id.acitvity_main_layout);
        if (!APIVersionUtils.isTier()) {
            if (!APIVersionUtils.isOverRom(9.0f)) {
                this.mRoot.setBackground(ContextCompat.getDrawable(this, R.color.webview_background));
            } else if (!APIVersionUtils.isOverRom(12.0f) && CommonUtils.showMonsterUIorIqooUi()) {
                this.mRoot.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_main_iqoo));
                getWindow().getDecorView().setBackground(ContextCompat.getDrawable(this, R.color.vivo_white));
            }
        }
        initTitle(R.id.bbk_title_view);
        if (APIVersionUtils.isOcean()) {
            this.mUpdateFragment13 = UpdateFragment13.newInstance();
            getBaseFragmentTransaction().add(R.id.main_frame, this.mUpdateFragment13).commitNow();
            if (APIVersionUtils.isFoldable() || this.mIsPadOcean) {
                Configuration configuration = getResources().getConfiguration();
                this.mIsLargeScreen = !UiUtils.isScreenLong(configuration);
                fitFoldableScreen(configuration);
                LogUtils.d(TAG, "mIsLargeScreen:" + this.mIsLargeScreen);
                return;
            }
            return;
        }
        this.mUpdateFragment = UpdateFragment.newInstance();
        getBaseFragmentTransaction().add(R.id.main_frame, this.mUpdateFragment).commitNow();
    }

    @Override // com.bbk.updater.ui.customactivity.CustomUpdateActivity
    protected void handleTitleClickEvent() {
        UpdateFragment13 updateFragment13 = this.mUpdateFragment13;
        if (updateFragment13 != null) {
            updateFragment13.toTop();
        }
    }

    public void startInstallAnimation() {
        if (this.mShutdownShowed) {
            return;
        }
        this.mShutdownShowed = true;
        View viewInflate = LayoutInflater.from(this).inflate(R.layout.shut_down_first_frame, (ViewGroup) null);
        ((ViewGroup) getWindow().getDecorView()).addView(viewInflate);
        viewInflate.bringToFront();
        hideActionBar();
        hideNavigationBar();
        final ImageView imageView = (ImageView) viewInflate.findViewById(R.id.shut_down_logo);
        Bitmap bitmapDecodeFile = BitmapFactory.decodeFile(CommonUtils.getShutDownBitmapPath(this, getApplication().getCacheDir().getAbsolutePath()));
        if (APIVersionUtils.isPad() && getResources().getConfiguration().orientation == 1) {
            int height = bitmapDecodeFile.getHeight();
            int width = bitmapDecodeFile.getWidth();
            int i = getResources().getDisplayMetrics().widthPixels;
            int i2 = width - i;
            int i3 = i2 > 0 ? i2 / 2 : 0;
            if (i < width) {
                width = i;
            }
            bitmapDecodeFile = Bitmap.createBitmap(bitmapDecodeFile, i3, 0, width, height);
        }
        imageView.setImageBitmap(bitmapDecodeFile);
        ValueAnimator duration = ValueAnimator.ofFloat(0.0f, 1.0f).setDuration(800L);
        duration.setInterpolator(new PathInterpolator(0.28f, 0.85f, 0.36f, 1.0f));
        duration.addListener(new AnimatorListenerAdapter() { // from class: com.bbk.updater.ui.UpdateActivity.2
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
                UpdateActivity.this.mShutdownShowed = false;
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animator) {
                super.onAnimationStart(animator);
                UpdateActivity.this.makeBreakModeFullScreen();
            }
        });
        duration.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.bbk.updater.ui.UpdateActivity.3
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                imageView.setAlpha(((Float) valueAnimator.getAnimatedValue()).floatValue());
            }
        });
        duration.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void makeBreakModeFullScreen() {
        if (UiUtils.isBreakMode(this)) {
            try {
                Class<?> cls = Class.forName("android.app.VivoActivitySplitterImpl");
                cls.getDeclaredMethod("makeFullScreen", new Class[0]).invoke(cls.getMethod("getOrCreateInstance", Activity.class, Boolean.TYPE).invoke(null, this, true), new Object[0]);
            } catch (Exception e) {
                LogUtils.e(TAG, "makeFullScreen Excepiton " + e.getMessage());
            }
        }
    }

    private void hideNavigationBar() {
        Window window = getWindow();
        if (window != null) {
            window.getDecorView().setSystemUiVisibility(5638);
        }
    }

    public void onMovedToDisplay(int i, Configuration configuration) {
        LogUtils.v(TAG, "onMovedToDisplay displayId:" + i);
        if (APIVersionUtils.isOcean()) {
            if (APIVersionUtils.isFoldable() || this.mIsPadOcean) {
                this.mUpdateFragment13.fitFoldableScreen(true, configuration);
                return;
            }
            return;
        }
        this.mUpdateFragment.fitMultiDisplay(i, configuration.orientation);
    }

    public void showOneDialog(int i) {
        LogUtils.i(TAG, "createDialog:" + i);
        int i2 = R.string.warning;
        switch (i) {
            case 1001:
                int i3 = R.string.network_setting_now;
                if (APIVersionUtils.isPad()) {
                    i3 = R.string.network_setting_now_pad;
                }
                this.mDialogNetworkDisconnect = newAlertBuilder(this).setTitle(R.string.network_disconnect).setMessage(i3).setPositiveButton(R.string.network_setting, this.mClickListener).setNegativeButton(android.R.string.cancel, this.mClickListener).create();
                this.mDialogNetworkDisconnect.setCanceledOnTouchOutside(false);
                this.mDialogNetworkDisconnect.setOnShowListener(new DialogButtonStyleRom13(this));
                this.mDialogNetworkDisconnect.show();
                break;
            case 1002:
                String string = getString(APIVersionUtils.isTier() ? R.string.warning_text_sys_dtag : R.string.warning_text);
                if (CommonUtils.isCurrentDataRoaming(this)) {
                    LogUtils.i(TAG, "Is roaming.");
                    if (a.a().f(-1) == 1) {
                        i2 = R.string.warning_vgc1;
                        string = getString(R.string.warning_text_sys_dtag_roaming_vgc_1);
                    } else {
                        string = getString(APIVersionUtils.isTier() ? R.string.warning_text_sys_dtag_roaming : R.string.roaming_download_confirmation);
                    }
                }
                this.mDialogMobileNetwork = newAlertBuilder(this).setTitle(i2).setMessage(string).setOnCancelListener(this.mCancelListener).setPositiveButton(R.string.str_continue, this.mClickListener).setNegativeButton(android.R.string.cancel, this.mClickListener).create();
                this.mDialogMobileNetwork.setCanceledOnTouchOutside(false);
                this.mDialogMobileNetwork.show();
                break;
            case 1004:
                this.mDialogIntelligentInstallation = newAlertBuilder(this).setTitle((APIVersionUtils.isTier() || APIVersionUtils.isOverRom(5.0f)) ? R.string.intelligent_installation_rom5_0 : R.string.intelligent_installation).setCancelable(false).setMessage((APIVersionUtils.isTier() || APIVersionUtils.isOverRom(5.0f)) ? R.string.open_intelligent_installation_tips : R.string.open_intelligent_installation_tips_below_rom5_0).setPositiveButton(R.string.smart_install_switch_open, this.mClickListener).setNegativeButton(R.string.osupdater_cancle, this.mClickListener).create();
                this.mDialogIntelligentInstallation.setCanceledOnTouchOutside(false);
                RxBus.a().c(new DialogEvent(1, ConstantsUtils.DialogType.OPEN_SMART_SWITCH_INDUCE, ""));
                PrefsUtils.putLong(this, PrefsUtils.Other.KEY_LAST_POP_SMART_INSTALL_INDUCE_TIME, System.currentTimeMillis());
                this.mDialogIntelligentInstallation.setOnShowListener(new DialogButtonStyleRom13(this));
                this.mDialogIntelligentInstallation.show();
                break;
            case 1005:
                View viewInflate = View.inflate(this, R.layout.open_auto_check_upgrade, null);
                TextView textView = (TextView) viewInflate.findViewById(R.id.content);
                textView.setMovementMethod(LinkMovementMethod.getInstance());
                textView.setText(R.string.open_auto_check_upgrade_summary);
                this.mDialogOpenAutoCheckUpgrade = newAlertBuilder(this).setTitle(R.string.open_auto_check_upgrade).setView(viewInflate).setPositiveButton(R.string.smart_install_switch_open, this.mClickListener).setNegativeButton(R.string.no, this.mClickListener).setOnCancelListener(this.mCancelListener).create();
                ((CheckBox) viewInflate.findViewById(R.id.checkbox)).setOnCheckedChangeListener(this.mCheckChangedListener);
                this.mDialogOpenAutoCheckUpgrade.setCanceledOnTouchOutside(false);
                this.mDialogOpenAutoCheckUpgrade.show();
                break;
            case 1006:
                this.mDialogUseDataCheckUpgrade = newAlertBuilder(this).setTitle(R.string.using_data_network_check_upgrade_package).setMessage(R.string.auto_check_upgrade_package_summary).setPositiveButton(R.string.smart_install_switch_open, this.mClickListener).setNegativeButton(R.string.no, this.mClickListener).create();
                this.mDialogUseDataCheckUpgrade.setCanceledOnTouchOutside(false);
                this.mDialogUseDataCheckUpgrade.show();
                break;
            case 1007:
                this.mDialogMobileNetworkPeakTime = newAlertBuilder(this).setTitle(R.string.warning).setMessage(R.string.peak_time_dialog_message).setOnCancelListener(this.mCancelListener).setPositiveButton(R.string.peak_time_dialog_position, this.mClickListener).setNegativeButton(android.R.string.cancel, this.mClickListener).create();
                this.mDialogMobileNetworkPeakTime.setCanceledOnTouchOutside(false);
                this.mDialogMobileNetworkPeakTime.show();
                break;
            case 1008:
                this.mDialogMobileNetworkMaximumSize = newAlertBuilder(this).setTitle(R.string.warning).setMessage(String.format(getString(R.string.maximum_size_dialog_message), CommonUtils.getPackageSize(CommonUtils.getRemainLength(this)))).setOnCancelListener(this.mCancelListener).setPositiveButton(R.string.continue_to_download, this.mClickListener).setNegativeButton(android.R.string.cancel, this.mClickListener).create();
                this.mDialogMobileNetworkMaximumSize.setCanceledOnTouchOutside(false);
                this.mDialogMobileNetworkMaximumSize.show();
                break;
            case 1009:
                this.mDialogMobileNetwork2G = newAlertBuilder(this).setTitle(R.string.warning).setMessage(R.string.dialog_of_2g_message).setOnCancelListener(this.mCancelListener).setPositiveButton(R.string.i_know, this.mClickListener).create();
                this.mDialogMobileNetwork2G.setCanceledOnTouchOutside(false);
                this.mDialogMobileNetwork2G.show();
                break;
            case 1010:
                this.mDialogDownloadPauseConfirm = newAlertBuilder(this).setTitle(getString(R.string.warning)).setMessage(getString(R.string.download_paused_confim_tips)).setPositiveButton(R.string.pause_btn, this.mClickListener).setNegativeButton(android.R.string.cancel, this.mClickListener).setOnCancelListener(this.mCancelListener).create();
                this.mDialogDownloadPauseConfirm.setCanceledOnTouchOutside(false);
                this.mDialogDownloadPauseConfirm.show();
                RxBus.a().c(new DialogEvent(1, ConstantsUtils.DialogType.DOWNLOAD_PUSED_CONFIM, ""));
                break;
            case 1011:
                this.mDialogMobileNetworkNotAllowDownload = newAlertBuilder(this).setTitle(R.string.download_warn).setMessage(R.string.wifi_download_only_msg).setOnCancelListener(this.mCancelListener).setPositiveButton(R.string.wifi_connect, this.mClickListener).setNegativeButton(R.string.no, this.mClickListener).create();
                this.mDialogMobileNetworkNotAllowDownload.setCanceledOnTouchOutside(false);
                this.mDialogMobileNetworkNotAllowDownload.show();
                break;
            case 1012:
                this.mDialogRoamingNotAllowDownload = newAlertBuilder(this).setTitle(R.string.unable_download_upgrade_package).setMessage(R.string.unable_download_warning_set_wlan).setOnCancelListener(this.mCancelListener).setPositiveButton(R.string.set_wlan, this.mClickListener).setNegativeButton(R.string.no, this.mClickListener).create();
                this.mDialogRoamingNotAllowDownload.setCanceledOnTouchOutside(false);
                this.mDialogRoamingNotAllowDownload.show();
                break;
            case 1013:
                this.mDialogRoamingNotAllowUpdates = newAlertBuilder(this).setTitle(R.string.unable_get_updates).setMessage(R.string.unable_updates_warning_set_wlan).setOnCancelListener(this.mCancelListener).setPositiveButton(R.string.set_wlan, this.mClickListener).setNegativeButton(R.string.no, this.mClickListener).create();
                this.mDialogRoamingNotAllowUpdates.setCanceledOnTouchOutside(false);
                this.mDialogRoamingNotAllowUpdates.show();
                break;
        }
    }

    public void dismissOneDialog(int i) {
        switch (i) {
            case 1001:
                if (this.mDialogNetworkDisconnect != null && this.mDialogNetworkDisconnect.isShowing()) {
                    this.mDialogNetworkDisconnect.dismiss();
                    break;
                }
                break;
            case 1002:
                if (this.mDialogMobileNetwork != null && this.mDialogMobileNetwork.isShowing()) {
                    this.mDialogMobileNetwork.dismiss();
                    break;
                }
                break;
            case 1004:
                if (this.mDialogIntelligentInstallation != null && this.mDialogIntelligentInstallation.isShowing()) {
                    this.mDialogIntelligentInstallation.dismiss();
                    break;
                }
                break;
            case 1005:
                if (this.mDialogOpenAutoCheckUpgrade != null && this.mDialogOpenAutoCheckUpgrade.isShowing()) {
                    this.mDialogOpenAutoCheckUpgrade.dismiss();
                    break;
                }
                break;
            case 1006:
                if (this.mDialogUseDataCheckUpgrade != null && this.mDialogUseDataCheckUpgrade.isShowing()) {
                    this.mDialogUseDataCheckUpgrade.dismiss();
                    break;
                }
                break;
            case 1007:
                if (this.mDialogMobileNetworkPeakTime != null && this.mDialogMobileNetworkPeakTime.isShowing()) {
                    this.mDialogMobileNetworkPeakTime.dismiss();
                    break;
                }
                break;
            case 1008:
                if (this.mDialogMobileNetworkMaximumSize != null && this.mDialogMobileNetworkMaximumSize.isShowing()) {
                    this.mDialogMobileNetworkMaximumSize.dismiss();
                    break;
                }
                break;
            case 1009:
                if (this.mDialogMobileNetwork2G != null && this.mDialogMobileNetwork2G.isShowing()) {
                    this.mDialogMobileNetwork2G.dismiss();
                    break;
                }
                break;
            case 1010:
                if (this.mDialogDownloadPauseConfirm != null && this.mDialogDownloadPauseConfirm.isShowing()) {
                    this.mDialogDownloadPauseConfirm.dismiss();
                    break;
                }
                break;
            case 1011:
                if (this.mDialogMobileNetworkNotAllowDownload != null && this.mDialogMobileNetworkNotAllowDownload.isShowing()) {
                    this.mDialogMobileNetworkNotAllowDownload.dismiss();
                    break;
                }
                break;
            case 1012:
                if (this.mDialogRoamingNotAllowDownload != null && this.mDialogRoamingNotAllowDownload.isShowing()) {
                    this.mDialogRoamingNotAllowDownload.dismiss();
                    break;
                }
                break;
            case 1013:
                if (this.mDialogRoamingNotAllowUpdates != null && this.mDialogRoamingNotAllowUpdates.isShowing()) {
                    this.mDialogRoamingNotAllowUpdates.dismiss();
                    break;
                }
                break;
        }
    }

    public void dismissAllDialog() {
        if (this.mDialogNetworkDisconnect != null && this.mDialogNetworkDisconnect.isShowing()) {
            this.mDialogNetworkDisconnect.dismiss();
        }
        if (this.mDialogMobileNetwork != null && this.mDialogMobileNetwork.isShowing()) {
            this.mDialogMobileNetwork.dismiss();
        }
        if (this.mDialogIntelligentInstallation != null && this.mDialogIntelligentInstallation.isShowing()) {
            this.mDialogIntelligentInstallation.dismiss();
        }
        if (this.mDialogOpenAutoCheckUpgrade != null && this.mDialogOpenAutoCheckUpgrade.isShowing()) {
            this.mDialogOpenAutoCheckUpgrade.dismiss();
        }
        if (this.mDialogUseDataCheckUpgrade != null && this.mDialogUseDataCheckUpgrade.isShowing()) {
            this.mDialogUseDataCheckUpgrade.dismiss();
        }
        if (this.mDialogMobileNetwork2G != null && this.mDialogMobileNetwork2G.isShowing()) {
            this.mDialogMobileNetwork2G.dismiss();
        }
        if (this.mDialogMobileNetworkNotAllowDownload != null && this.mDialogMobileNetworkNotAllowDownload.isShowing()) {
            this.mDialogMobileNetworkNotAllowDownload.dismiss();
        }
        if (this.mDialogRoamingNotAllowDownload != null && this.mDialogRoamingNotAllowDownload.isShowing()) {
            this.mDialogRoamingNotAllowDownload.dismiss();
        }
        if (this.mDialogRoamingNotAllowUpdates != null && this.mDialogRoamingNotAllowUpdates.isShowing()) {
            this.mDialogRoamingNotAllowUpdates.dismiss();
        }
        if (this.mDialogMobileNetworkPeakTime != null && this.mDialogMobileNetworkPeakTime.isShowing()) {
            this.mDialogMobileNetworkPeakTime.dismiss();
        }
        if (this.mDialogMobileNetworkMaximumSize != null && this.mDialogMobileNetworkMaximumSize.isShowing()) {
            this.mDialogMobileNetworkMaximumSize.dismiss();
        }
        if (this.mDialogDownloadPauseConfirm == null || !this.mDialogDownloadPauseConfirm.isShowing()) {
            return;
        }
        this.mDialogDownloadPauseConfirm.dismiss();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerNetJumpReceiver(Context context) {
        this.mNetJumpReceiver = new BroadcastReceiver() { // from class: com.bbk.updater.ui.UpdateActivity.7
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if (intent == null || !UpdateActivity.CLICK_MORE_BUTTON_ACTION.equals(intent.getAction())) {
                    return;
                }
                UpdateActivity.this.needFinish = true;
                if (UpdateActivity.this.onStop) {
                    UpdateActivity.this.needFinish = false;
                    UpdateActivity.this.finish();
                }
            }
        };
        context.registerReceiver(this.mNetJumpReceiver, new IntentFilter(CLICK_MORE_BUTTON_ACTION));
    }

    private void unregisterNetJumpReceiver(Context context) {
        BroadcastReceiver broadcastReceiver = this.mNetJumpReceiver;
        if (broadcastReceiver != null) {
            context.unregisterReceiver(broadcastReceiver);
            this.mNetJumpReceiver = null;
        }
    }

    @Override // android.app.Activity, android.content.ComponentCallbacks
    public void onConfigurationChanged(Configuration configuration) {
        LogUtils.d(TAG, "onConfigurationChanged: " + configuration.densityDpi);
        super.onConfigurationChanged(configuration);
        if (APIVersionUtils.isOcean()) {
            if (APIVersionUtils.isFoldable() || this.mIsPadOcean) {
                boolean z = !UiUtils.isScreenLong(configuration);
                LogUtils.d(TAG, "newConfig isLargeScreen:" + z);
                if (z != this.mIsLargeScreen || this.mIsPadOcean) {
                    this.mIsLargeScreen = z;
                    fitFoldableScreen(configuration);
                }
                this.mUpdateFragment13.fitFoldableScreen(true, configuration);
                return;
            }
            this.mUpdateFragment13.fitMultiDisplay(getWindowManager().getDefaultDisplay().getDisplayId(), configuration.orientation);
            return;
        }
        this.mUpdateFragment.fitMultiDisplay(getWindowManager().getDefaultDisplay().getDisplayId(), configuration.orientation);
    }

    @Override // android.app.Activity, android.view.ContextThemeWrapper, android.content.ContextWrapper
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        if (APIVersionUtils.isOcean()) {
            UiUtils.disabledDisplayDpiChange(this);
        }
    }

    private void fitFoldableScreen(Configuration configuration) {
        boolean z = !UiUtils.isScreenLong(configuration) && UiUtils.isBreakMode(this);
        for (int i = 0; i < this.mRoot.getChildCount(); i++) {
            View childAt = this.mRoot.getChildAt(i);
            if ((childAt instanceof ImageView) && "back_rom13".equals(childAt.getTag())) {
                if (z) {
                    childAt.setEnabled(false);
                    childAt.setVisibility(8);
                    return;
                } else {
                    childAt.setEnabled(true);
                    childAt.setVisibility(0);
                    return;
                }
            }
        }
    }
}
