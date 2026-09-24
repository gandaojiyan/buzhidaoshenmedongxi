package com.bbk.updater.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import com.bbk.updater.R;
import com.bbk.updater.bean.PrivacyTerms;
import com.bbk.updater.bean.UpdateInfo;
import com.bbk.updater.bean.VgcUpdateInfo;
import com.bbk.updater.check.a.b;
import com.bbk.updater.config.CallBack;
import com.bbk.updater.config.Configs;
import com.bbk.updater.config.bean.AppInfo;
import com.bbk.updater.cota.CotaStatusChangeEvent;
import com.bbk.updater.rx.event.CheckEvent;
import com.bbk.updater.rx.event.ClickEvent;
import com.bbk.updater.rx.event.DialogEvent;
import com.bbk.updater.rx.event.DownloadEvent;
import com.bbk.updater.rx.event.InstallEvent;
import com.bbk.updater.rx.event.SmartInstallEvent;
import com.bbk.updater.ui.dialogcontent.panel.ButtonClickInterface;
import com.bbk.updater.ui.widget.CheckIndicatorView;
import com.bbk.updater.ui.widget.DownloadControllerView;
import com.bbk.updater.ui.widget.DownloadIndicatorView;
import com.bbk.updater.utils.CommonUtils;
import com.bbk.updater.utils.ConstantsUtils;
import com.bbk.updater.utils.JumpUtils;
import com.bbk.updater.utils.LogUtils;
import com.bbk.updater.utils.PopDialogUtils;
import com.bbk.updater.utils.PrefsUtils;
import com.bbk.updater.utils.PrivacyTermsHelper;
import com.bbk.updater.utils.StringUtils;
import com.bbk.updater.utils.VersionUtils;
import com.bbk.updater.utils.WebHelper;
import com.bbk.vgc.event.TypeChangeEvent;
import com.vivo.updaterbaseframe.a.c;
import com.vivo.updaterbaseframe.b.a;
import com.vivo.updaterbaseframe.rx.RxBus;
import com.vivo.updaterbaseframe.strategy.StrategyFactory;
import com.vivo.updaterbaseframe.utils.BaseFrameUtils;
import java.util.ArrayList;
import java.util.List;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/* JADX INFO: loaded from: classes.dex */
public class UpdatePresenter extends a<UpdateView> {
    private static final String TAG = "Updater/UpdatePresenter";
    private Activity mActivity;
    private b mCheckUpdateTask;
    private Context mContext;
    private DownloadControllerView.DownloadControllerStatus mDcStatus;
    private UpdateInfo mFotaUpdateInfo;
    private boolean mFromNotice;
    private boolean mFromTrialVersion;
    private AlertDialog mIncompatibleWarning;
    private boolean mIsResumeDownload;
    private com.bbk.updater.download.a.b mPrepareDownloadCheckTask;
    private VgcUpdateInfo mVgcUpdateInfo;
    private com.bbk.updater.install.abinstall.b sAbUpdateManager;
    private com.bbk.updater.download.a sDownloadInfoManager;
    private ConstantsUtils.NetWorkType mPreNetworkType = ConstantsUtils.NetWorkType.NULL;
    private boolean isPreNetRoamingForbid = false;
    private int mFotaDownloadStatus = -1;
    private int mVgcDownloadStatus = -1;
    private float mDownloadProgress = -1.0f;
    private int mResumeNet = -1;
    private boolean mUseDataCheckChanged = false;
    private boolean mShowedOpenAutoCheck = false;

    @Override // com.vivo.updaterbaseframe.b.a
    public void attachView(UpdateView updateView) {
        super.attachView(updateView);
        RxBus.a().a(this);
        Context context = updateView.getContext();
        this.mContext = context;
        this.mActivity = (Activity) context;
        this.sDownloadInfoManager = com.bbk.updater.download.a.a(context);
        this.sAbUpdateManager = com.bbk.updater.install.abinstall.b.a(this.mContext);
        this.mPreNetworkType = CommonUtils.getNetworkConnectType(this.mContext);
        if (!this.mFromTrialVersion) {
            init();
        } else {
            trailCheck();
        }
    }

    @Override // com.vivo.updaterbaseframe.b.a
    public void detachView() {
        super.detachView();
        cancelTask(this.mCheckUpdateTask, this.mPrepareDownloadCheckTask);
        RxBus.a().b(this);
    }

    @Override // com.vivo.updaterbaseframe.b.a
    public void onResume() {
        super.onResume();
        this.mIncompatibleWarning = null;
        ConstantsUtils.NetWorkType networkConnectType = CommonUtils.getNetworkConnectType(this.mContext);
        boolean z = true;
        boolean z2 = networkConnectType == ConstantsUtils.NetWorkType.MOBILE && com.bbk.vgc.a.a.a().a(this.mContext);
        if (this.mFromTrialVersion && this.mFotaUpdateInfo == null && this.mVgcUpdateInfo == null) {
            if (z2) {
                if (m10getMvpView() == null) {
                    return;
                }
                m10getMvpView().setCheckIndicatorStatus(CheckIndicatorView.CheckIndicatorStatus.NETWORK_ROAMING_FORBID_UPDATE);
                return;
            }
            trailCheck();
            return;
        }
        boolean z3 = this.isPreNetRoamingForbid && !z2;
        ConstantsUtils.NetWorkType netWorkType = this.mPreNetworkType;
        if ((!(networkConnectType != netWorkType && netWorkType == ConstantsUtils.NetWorkType.NULL) || z2) && !z3) {
            z = false;
        }
        if ((z || this.mUseDataCheckChanged) && this.mFotaUpdateInfo == null && this.mVgcUpdateInfo == null) {
            if (VersionUtils.iSVgc()) {
                com.bbk.updater.download.a.a(this.mContext).a("ota_pacakge", "vgc_package");
            } else {
                com.bbk.updater.download.a.a(this.mContext).w();
            }
            if (!z2) {
                checkUpdate();
            }
        }
        this.isPreNetRoamingForbid = z2;
        this.mPreNetworkType = networkConnectType;
        this.mUseDataCheckChanged = false;
        if (networkConnectType != ConstantsUtils.NetWorkType.NULL) {
            m10getMvpView().dismissOneDialog(1001);
        }
    }

    private void trailCheck() {
        LogUtils.d(TAG, "ifTrailCheck");
        if (m10getMvpView() == null) {
            return;
        }
        m10getMvpView().setCheckIndicatorStatus(CheckIndicatorView.CheckIndicatorStatus.CHECKING);
        com.bbk.trialversion.b.a().a(this.mContext, "touch", new com.bbk.trialversion.trialversion.a.a.a() { // from class: com.bbk.updater.ui.UpdatePresenter.1
            @Override // com.bbk.trialversion.trialversion.a.a.a
            public void checkSucceed(UpdateInfo updateInfo) {
                if (UpdatePresenter.this.m10getMvpView() == null) {
                    return;
                }
                if (updateInfo != null) {
                    UpdatePresenter.this.mFromTrialVersion = true;
                    LogUtils.i(UpdatePresenter.TAG, "trialVersionChecked");
                    UpdatePresenter.this.refreshUpdateInfo(updateInfo, null);
                    UpdatePresenter.this.refreshLogContent(updateInfo, null);
                    UpdatePresenter.this.refreshUI(DownloadControllerView.DownloadControllerStatus.TO_DOWNLOAD, DownloadIndicatorView.DownloadIndicatorStatus.TO_DOWNLOAD);
                    return;
                }
                UpdatePresenter.this.m10getMvpView().setCheckIndicatorStatus(CheckIndicatorView.CheckIndicatorStatus.UP_TO_DATE);
            }

            @Override // com.bbk.trialversion.trialversion.a.a.a
            public void conditionsNotMet() {
                if (UpdatePresenter.this.m10getMvpView() != null) {
                    UpdatePresenter.this.m10getMvpView().setCheckIndicatorStatus(CheckIndicatorView.CheckIndicatorStatus.ERROR_SERVER_RESPONSE);
                }
            }

            @Override // com.bbk.trialversion.trialversion.a.a.a
            public void checkError() {
                if (UpdatePresenter.this.m10getMvpView() != null) {
                    UpdatePresenter.this.m10getMvpView().setCheckIndicatorStatus(CheckIndicatorView.CheckIndicatorStatus.ERROR_SERVER_RESPONSE);
                }
            }
        });
    }

    @Override // com.vivo.updaterbaseframe.b.a
    public void onPause() {
        super.onPause();
    }

    @Override // com.vivo.updaterbaseframe.b.a
    public void onDestroy() {
        super.onDestroy();
        long j = PrefsUtils.getLong(this.mContext, PrefsUtils.Check.KEY_LAST_SHOW_OPEN_AUTO_UPGRADE_PACKAGE_TIME, -1L);
        if (j == -1) {
            PrefsUtils.putLong(this.mContext, PrefsUtils.Check.KEY_LAST_SHOW_OPEN_AUTO_UPGRADE_PACKAGE_TIME, System.currentTimeMillis());
        } else if (CommonUtils.checkTimeOutSevenDay(j)) {
            PrefsUtils.removePrefs(this.mContext, PrefsUtils.Check.KEY_LAST_SHOW_OPEN_AUTO_UPGRADE_PACKAGE_TIME);
        } else {
            PrefsUtils.putLong(this.mContext, PrefsUtils.Check.KEY_LAST_SHOW_OPEN_AUTO_UPGRADE_PACKAGE_TIME, System.currentTimeMillis());
        }
    }

    private void init() {
        this.isPreNetRoamingForbid = com.bbk.vgc.a.a.a().a(this.mContext);
        UpdateInfo availableFotaInfo = CommonUtils.getAvailableFotaInfo(this.mContext, true);
        VgcUpdateInfo availableVgcInfo = CommonUtils.getAvailableVgcInfo(this.mContext, true);
        int iC = this.sDownloadInfoManager.c();
        int iD = this.sDownloadInfoManager.d();
        StringBuilder sb = new StringBuilder();
        sb.append("fota info: ");
        sb.append(availableFotaInfo == null ? "null" : availableFotaInfo.getVersion());
        sb.append("; vgc info: ");
        sb.append(availableVgcInfo != null ? availableVgcInfo.getVersion() : "null");
        LogUtils.i(TAG, sb.toString());
        int iCheckShowInfoType = checkShowInfoType(availableFotaInfo, availableVgcInfo);
        if (iCheckShowInfoType != 0) {
            if (iCheckShowInfoType == 1) {
                LogUtils.i(TAG, "Exist update info, and download started!");
                if (!com.bbk.updater.download.b.a.b(iC)) {
                    availableFotaInfo = null;
                }
                if (!com.bbk.updater.download.b.a.b(iD)) {
                    availableVgcInfo = null;
                }
                refreshUpdateInfo(availableFotaInfo, availableVgcInfo);
                refreshLogContent(this.mFotaUpdateInfo, this.mVgcUpdateInfo);
                m10getMvpView().setCheckIndicatorStatus(CheckIndicatorView.CheckIndicatorStatus.LOAD_SUCCEED);
                if (checkIfDownloadSucceed(availableFotaInfo, availableVgcInfo, iC, iD)) {
                    boolean zIsOnInstallorPreInstall = UpdateStatusManager.getInstance().isOnInstallorPreInstall();
                    LogUtils.d(TAG, "init checkIfDownloadSucceed = " + this.sAbUpdateManager.g() + ", isOnInstallPre=" + zIsOnInstallorPreInstall);
                    if (com.bbk.updater.install.a.a.a(this.mFotaUpdateInfo, this.mVgcUpdateInfo) && this.sAbUpdateManager.d() && BaseFrameUtils.PackageType.OTA_FOTA.toString().equals(this.sAbUpdateManager.e())) {
                        int iF = this.sAbUpdateManager.f();
                        LogUtils.d(TAG, "installStatus=" + iF);
                        if (6 == iF || 5 == iF) {
                            refreshUI(DownloadControllerView.DownloadControllerStatus.INSTALL_SUCCEED, DownloadIndicatorView.DownloadIndicatorStatus.INSTALL_SUCCEED);
                        } else if (3 == iF) {
                            refreshUI(DownloadControllerView.DownloadControllerStatus.INSTALL_PAUSED, DownloadIndicatorView.DownloadIndicatorStatus.INSTALL_PAUSED);
                        } else if (7 == iF) {
                            refreshUI(DownloadControllerView.DownloadControllerStatus.VERIFYING_PAUSED, DownloadIndicatorView.DownloadIndicatorStatus.VERIFYING_PAUSED);
                        } else if (2 == iF) {
                            refreshUI(DownloadControllerView.DownloadControllerStatus.ON_INSTALL, DownloadIndicatorView.DownloadIndicatorStatus.ON_INSTALL);
                            m10getMvpView().refreshProgress(this.sAbUpdateManager.h());
                        } else if (4 == iF) {
                            refreshUI(DownloadControllerView.DownloadControllerStatus.ON_AB_INSTALL_VERIFYING, DownloadIndicatorView.DownloadIndicatorStatus.ON_AB_FINALIZING);
                            m10getMvpView().refreshProgress(this.sAbUpdateManager.h());
                        }
                    } else if (com.bbk.updater.install.a.a.a(this.mFotaUpdateInfo, this.mVgcUpdateInfo) && zIsOnInstallorPreInstall) {
                        refreshUI(DownloadControllerView.DownloadControllerStatus.ON_INSTALL, DownloadIndicatorView.DownloadIndicatorStatus.ON_INSTALL);
                        m10getMvpView().refreshProgress(0.0f);
                    } else {
                        refreshUI(DownloadControllerView.DownloadControllerStatus.TO_INSTALL, DownloadIndicatorView.DownloadIndicatorStatus.DOWNLOAD_SUCCEED);
                    }
                } else {
                    if (this.sDownloadInfoManager.b(iC) || this.sDownloadInfoManager.b(iD)) {
                        refreshUI(DownloadControllerView.DownloadControllerStatus.PAUSED, DownloadIndicatorView.DownloadIndicatorStatus.DOWNLOAD_PAUSED);
                    } else if (this.sDownloadInfoManager.c(iC) || this.sDownloadInfoManager.c(iD)) {
                        refreshUI(DownloadControllerView.DownloadControllerStatus.RUNNING, DownloadIndicatorView.DownloadIndicatorStatus.DOWNLOAD_RUNNING);
                    }
                    if (this.sDownloadInfoManager.a(iC) && !this.sDownloadInfoManager.d(iC)) {
                        this.sDownloadInfoManager.c("ota_pacakge");
                    }
                    if (this.sDownloadInfoManager.a(iD) && !this.sDownloadInfoManager.d(iD)) {
                        this.sDownloadInfoManager.c("vgc_package");
                    }
                }
            } else if (iCheckShowInfoType == 2) {
                m10getMvpView().setCheckIndicatorStatus(CheckIndicatorView.CheckIndicatorStatus.LOAD_SUCCEED);
                if (availableVgcInfo != null && !availableVgcInfo.isEnhance() && !com.vivo.updaterassistant.a.a(availableVgcInfo) && availableFotaInfo != null && (availableFotaInfo.isEnhancedDownload() || com.vivo.updaterassistant.a.a(availableFotaInfo))) {
                    LogUtils.i(TAG, "Exist update info,fota is f and on download, show fota only");
                    availableVgcInfo = null;
                }
                int i = AnonymousClass12.$SwitchMap$com$bbk$updater$utils$ConstantsUtils$NetWorkType[this.mPreNetworkType.ordinal()];
                if (i != 1) {
                    if (i == 2) {
                        m10getMvpView().showOneDialog(1001);
                        m10getMvpView().setCheckIndicatorStatus(CheckIndicatorView.CheckIndicatorStatus.NETWORK_DISABLED);
                        return;
                    }
                } else if (com.bbk.vgc.a.a.a().a(this.mContext)) {
                    m10getMvpView().showOneDialog(1013);
                    m10getMvpView().setCheckIndicatorStatus(CheckIndicatorView.CheckIndicatorStatus.NETWORK_ROAMING_FORBID_UPDATE);
                    return;
                }
                refreshUpdateInfo(availableFotaInfo, availableVgcInfo);
                refreshLogContent(this.mFotaUpdateInfo, this.mVgcUpdateInfo);
                refreshUI(DownloadControllerView.DownloadControllerStatus.TO_DOWNLOAD, DownloadIndicatorView.DownloadIndicatorStatus.TO_DOWNLOAD);
            } else if (iCheckShowInfoType == 3) {
                m10getMvpView().setCheckIndicatorStatus(CheckIndicatorView.CheckIndicatorStatus.LOAD_SUCCEED);
                if (availableFotaInfo != null && !availableFotaInfo.isEnhancedDownload() && !com.vivo.updaterassistant.a.a(availableFotaInfo) && availableVgcInfo != null && (availableVgcInfo.isEnhance() || com.vivo.updaterassistant.a.a(availableVgcInfo))) {
                    LogUtils.i(TAG, "Exist update info,vgc is f and on download, show vgc only");
                    availableFotaInfo = null;
                }
                int i2 = AnonymousClass12.$SwitchMap$com$bbk$updater$utils$ConstantsUtils$NetWorkType[this.mPreNetworkType.ordinal()];
                if (i2 != 1) {
                    if (i2 == 2) {
                        m10getMvpView().showOneDialog(1001);
                        m10getMvpView().setCheckIndicatorStatus(CheckIndicatorView.CheckIndicatorStatus.NETWORK_DISABLED);
                        return;
                    }
                } else if (com.bbk.vgc.a.a.a().a(this.mContext)) {
                    m10getMvpView().showOneDialog(1013);
                    m10getMvpView().setCheckIndicatorStatus(CheckIndicatorView.CheckIndicatorStatus.NETWORK_ROAMING_FORBID_UPDATE);
                    return;
                }
                refreshUpdateInfo(availableFotaInfo, availableVgcInfo);
                refreshLogContent(this.mFotaUpdateInfo, this.mVgcUpdateInfo);
                refreshUI(DownloadControllerView.DownloadControllerStatus.TO_DOWNLOAD, DownloadIndicatorView.DownloadIndicatorStatus.TO_DOWNLOAD);
            }
        } else if (this.mFromNotice && (availableFotaInfo != null || availableVgcInfo != null)) {
            LogUtils.i(TAG, "Exist update info, and start from Notification!");
            int iCheckPackageAvailableType = CommonUtils.checkPackageAvailableType(availableFotaInfo, availableVgcInfo);
            if (iCheckPackageAvailableType == 1) {
                availableVgcInfo = null;
            } else if (iCheckPackageAvailableType == 2) {
                availableFotaInfo = null;
            }
            refreshUpdateInfo(availableFotaInfo, availableVgcInfo);
            int i3 = AnonymousClass12.$SwitchMap$com$bbk$updater$utils$ConstantsUtils$NetWorkType[this.mPreNetworkType.ordinal()];
            if (i3 != 1) {
                if (i3 == 2) {
                    m10getMvpView().showOneDialog(1001);
                    m10getMvpView().setCheckIndicatorStatus(CheckIndicatorView.CheckIndicatorStatus.NETWORK_DISABLED);
                    return;
                }
            } else if (com.bbk.vgc.a.a.a().a(this.mContext)) {
                m10getMvpView().showOneDialog(1013);
                m10getMvpView().setCheckIndicatorStatus(CheckIndicatorView.CheckIndicatorStatus.NETWORK_ROAMING_FORBID_UPDATE);
                return;
            }
            refreshLogContent(this.mFotaUpdateInfo, this.mVgcUpdateInfo);
            refreshUI(DownloadControllerView.DownloadControllerStatus.TO_DOWNLOAD, DownloadIndicatorView.DownloadIndicatorStatus.TO_DOWNLOAD);
        } else {
            fetchUpdateInfo();
        }
        int i4 = Settings.Global.getInt(this.mContext.getContentResolver(), ConstantsUtils.INTELLIGENT_INSTALLATION_KEY_IN_DATABASE, -999);
        LogUtils.i(TAG, "Intelligent install switch value : " + i4);
        long j = PrefsUtils.getLong(this.mContext, PrefsUtils.Other.KEY_LAST_POP_SMART_INSTALL_INDUCE_TIME, 0L);
        LogUtils.d(TAG, "last sii time : " + j + ", current " + System.currentTimeMillis());
        if (i4 == -999 || i4 == -1 || (i4 == 0 && Math.abs(System.currentTimeMillis() - j) > 5184000000L)) {
            m10getMvpView().showOneDialog(1004);
            if (i4 == -999 || i4 == -1) {
                com.bbk.updater.codemanagement.a.b(this.mContext);
            }
        }
    }

    /* JADX INFO: renamed from: com.bbk.updater.ui.UpdatePresenter$12, reason: invalid class name */
    static /* synthetic */ class AnonymousClass12 {
        static final /* synthetic */ int[] $SwitchMap$com$bbk$updater$utils$ConstantsUtils$NetWorkType;

        static {
            int[] iArr = new int[ConstantsUtils.NetWorkType.values().length];
            $SwitchMap$com$bbk$updater$utils$ConstantsUtils$NetWorkType = iArr;
            try {
                iArr[ConstantsUtils.NetWorkType.MOBILE.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$com$bbk$updater$utils$ConstantsUtils$NetWorkType[ConstantsUtils.NetWorkType.NULL.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                $SwitchMap$com$bbk$updater$utils$ConstantsUtils$NetWorkType[ConstantsUtils.NetWorkType.BLUETOOTH.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                $SwitchMap$com$bbk$updater$utils$ConstantsUtils$NetWorkType[ConstantsUtils.NetWorkType.WIFI.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshUI(DownloadControllerView.DownloadControllerStatus downloadControllerStatus, DownloadIndicatorView.DownloadIndicatorStatus downloadIndicatorStatus) {
        this.mDcStatus = downloadControllerStatus;
        if (downloadControllerStatus == DownloadControllerView.DownloadControllerStatus.ON_INSTALL) {
            UpdateStatusManager.getInstance().setOnInstallorPreInstall(true);
        } else {
            UpdateStatusManager.getInstance().setOnInstallorPreInstall(false);
        }
        m10getMvpView().setDownloadControllerStatus(downloadControllerStatus);
        if (downloadIndicatorStatus == DownloadIndicatorView.DownloadIndicatorStatus.NTEGRITY_CHECK_SUCCEED && com.bbk.updater.install.a.a.a(this.mFotaUpdateInfo, this.mVgcUpdateInfo)) {
            m10getMvpView().setDownloadIndicatorStatus(DownloadIndicatorView.DownloadIndicatorStatus.NTEGRITY_CHECK_SUCCEED_AB);
        } else {
            m10getMvpView().setDownloadIndicatorStatus(downloadIndicatorStatus);
        }
    }

    @RxBus.a(a = RxBus.RunningThreadType.mainThread)
    private void onInstallEvent(InstallEvent installEvent) {
        LogUtils.i(TAG, "Get install event : " + installEvent.getEventId());
        int eventId = installEvent.getEventId();
        if (eventId == 4122) {
            LogUtils.i(TAG, "agreement refused, load log Update interface to download succeed");
            refreshUI(DownloadControllerView.DownloadControllerStatus.TO_INSTALL, DownloadIndicatorView.DownloadIndicatorStatus.DOWNLOAD_SUCCEED);
        }
        switch (eventId) {
            case InstallEvent.AB_INTALL_PROGRESS /* 4101 */:
                if (BaseFrameUtils.PackageType.OTA_FOTA.toString().equals(installEvent.getPackageType())) {
                    if (installEvent.getInstallStatus() == 4105) {
                        refreshUI(DownloadControllerView.DownloadControllerStatus.TO_INSTALL, DownloadIndicatorView.DownloadIndicatorStatus.DOWNLOAD_SUCCEED);
                    } else if (installEvent.getInstallStatus() == 2) {
                        LogUtils.d(TAG, "installProgress=" + installEvent.getInstallProgress());
                        m10getMvpView().refreshProgress(installEvent.getInstallProgress());
                        refreshUI(DownloadControllerView.DownloadControllerStatus.ON_INSTALL, DownloadIndicatorView.DownloadIndicatorStatus.ON_INSTALL);
                    } else if (installEvent.getInstallStatus() == 4) {
                        LogUtils.d(TAG, "finalizing Progress=" + installEvent.getInstallProgress());
                        m10getMvpView().refreshProgress(installEvent.getInstallProgress());
                        refreshUI(DownloadControllerView.DownloadControllerStatus.ON_AB_INSTALL_VERIFYING, DownloadIndicatorView.DownloadIndicatorStatus.ON_AB_FINALIZING);
                    } else if (installEvent.getInstallStatus() == 3) {
                        m10getMvpView().refreshProgress(installEvent.getInstallProgress());
                        refreshUI(DownloadControllerView.DownloadControllerStatus.INSTALL_PAUSED, DownloadIndicatorView.DownloadIndicatorStatus.INSTALL_PAUSED);
                    } else if (installEvent.getInstallStatus() == 7) {
                        m10getMvpView().refreshProgress(installEvent.getInstallProgress());
                        refreshUI(DownloadControllerView.DownloadControllerStatus.VERIFYING_PAUSED, DownloadIndicatorView.DownloadIndicatorStatus.VERIFYING_PAUSED);
                    } else if (installEvent.getInstallStatus() == 6) {
                        if (m10getMvpView().isResumeNow()) {
                            com.bbk.updater.install.a.a.a(this.mContext, BaseFrameUtils.UpdateMethod.UPDATE_DIRECT_ACTIVITY);
                        } else {
                            refreshUI(DownloadControllerView.DownloadControllerStatus.INSTALL_SUCCEED, DownloadIndicatorView.DownloadIndicatorStatus.INSTALL_SUCCEED);
                        }
                    } else if (installEvent.getInstallStatus() == 1) {
                        refreshUI(DownloadControllerView.DownloadControllerStatus.INSTALL_FAILED, DownloadIndicatorView.DownloadIndicatorStatus.INSTALL_FAILED);
                    } else if (installEvent.getInstallStatus() == 33) {
                        LogUtils.d(TAG, "mDcSttus=" + this.mDcStatus);
                        if (this.mDcStatus == DownloadControllerView.DownloadControllerStatus.ON_INSTALL || this.mDcStatus == DownloadControllerView.DownloadControllerStatus.INSTALL_PAUSED || this.mDcStatus == DownloadControllerView.DownloadControllerStatus.INSTALL_SUCCEED || this.mDcStatus == DownloadControllerView.DownloadControllerStatus.ON_AB_INSTALL_VERIFYING) {
                            refreshUI(DownloadControllerView.DownloadControllerStatus.TO_INSTALL, DownloadIndicatorView.DownloadIndicatorStatus.DOWNLOAD_SUCCEED);
                        }
                    }
                }
                break;
            case InstallEvent.INSTALL_CHECK_SUCCEED /* 4102 */:
                refreshUI(DownloadControllerView.DownloadControllerStatus.ON_INSTALL, DownloadIndicatorView.DownloadIndicatorStatus.ON_INSTALL);
                break;
            case InstallEvent.CHECK_UPDATE_AB_INSTALL_VERIFY /* 4103 */:
                if (m10getMvpView().isResumeNow()) {
                    CommonUtils.postToast(this.mContext, R.string.error_ab_install_on_verify, 0);
                }
                refreshUI(DownloadControllerView.DownloadControllerStatus.TO_INSTALL, DownloadIndicatorView.DownloadIndicatorStatus.DOWNLOAD_SUCCEED);
                break;
            case InstallEvent.AB_CHECK_FAILED /* 4104 */:
                refreshUI(DownloadControllerView.DownloadControllerStatus.INSTALL_FAILED, DownloadIndicatorView.DownloadIndicatorStatus.INSTALL_FAILED);
                break;
            case InstallEvent.AB_SPACE_VERIFY_FAILED /* 4105 */:
                refreshUI(DownloadControllerView.DownloadControllerStatus.TO_INSTALL, DownloadIndicatorView.DownloadIndicatorStatus.DOWNLOAD_SUCCEED);
                break;
            default:
                switch (eventId) {
                    case InstallEvent.BATTERY_NOT_ENOUGH /* 4111 */:
                        break;
                    case InstallEvent.SDCARD_NOT_MOUNTED /* 4112 */:
                        m10getMvpView().toast(this.mActivity.getString(R.string.error_sdcard_mounted));
                        refreshUI(DownloadControllerView.DownloadControllerStatus.TO_INSTALL, DownloadIndicatorView.DownloadIndicatorStatus.DOWNLOAD_SUCCEED);
                        break;
                    case InstallEvent.SDCARD_MOUNTED /* 4113 */:
                        refreshUI(DownloadControllerView.DownloadControllerStatus.INTEGRITY_CHECKING, DownloadIndicatorView.DownloadIndicatorStatus.NTEGRITY_CHECKING);
                        break;
                    case InstallEvent.CHECKING /* 4114 */:
                        refreshUI(DownloadControllerView.DownloadControllerStatus.INTEGRITY_CHECKING, DownloadIndicatorView.DownloadIndicatorStatus.NTEGRITY_CHECKING);
                        break;
                    case InstallEvent.CHECK_FAILED /* 4115 */:
                        refreshUI(DownloadControllerView.DownloadControllerStatus.FAILED, DownloadIndicatorView.DownloadIndicatorStatus.NTEGRITY_CHECK_FAILED);
                        break;
                    case InstallEvent.CHECK_SUCCEED /* 4116 */:
                        refreshUI(DownloadControllerView.DownloadControllerStatus.PREPARE_INSTALL, DownloadIndicatorView.DownloadIndicatorStatus.NTEGRITY_CHECK_SUCCEED);
                        break;
                    default:
                        switch (eventId) {
                            case InstallEvent.INSTALL_PREPARING /* 4126 */:
                                refreshUI(DownloadControllerView.DownloadControllerStatus.PREPARE_INSTALL, DownloadIndicatorView.DownloadIndicatorStatus.NTEGRITY_CHECKING);
                                break;
                            case InstallEvent.TEMPETATURE_NOT_SATISFY /* 4127 */:
                                if (installEvent.getInstallStatus() == 3) {
                                    CommonUtils.postToast(this.mContext, R.string.tempetature_high_install_pause_tips, 0);
                                } else if (installEvent.getInstallStatus() == 7) {
                                    CommonUtils.postToast(this.mContext, R.string.tempetature_high_verify_pause_tips, 0);
                                } else {
                                    CommonUtils.postToast(this.mContext, R.string.tempetature_high_cannot_install_tips, 0);
                                    refreshUI(DownloadControllerView.DownloadControllerStatus.TO_INSTALL, DownloadIndicatorView.DownloadIndicatorStatus.DOWNLOAD_SUCCEED);
                                }
                                break;
                        }
                        break;
                }
                refreshUI(DownloadControllerView.DownloadControllerStatus.TO_INSTALL, DownloadIndicatorView.DownloadIndicatorStatus.DOWNLOAD_SUCCEED);
                break;
        }
    }

    @RxBus.a(a = RxBus.RunningThreadType.mainThread)
    private void onClickEvent(ClickEvent clickEvent) {
        int eventId = clickEvent != null ? clickEvent.getEventId() : -1;
        LogUtils.i(TAG, "Get click event : " + eventId);
        if (eventId == 16) {
            AlertDialog alertDialog = this.mIncompatibleWarning;
            if (alertDialog == null || !alertDialog.isShowing()) {
                final String strPrepareAndroidUpgrageCheck = CommonUtils.prepareAndroidUpgrageCheck(this.mContext, this.mFotaUpdateInfo);
                if (!TextUtils.isEmpty(strPrepareAndroidUpgrageCheck)) {
                    LogUtils.d(TAG, "<--showIncompatibleWarning-->activity|install_night|" + CommonUtils.countTime(new CallBack() { // from class: com.bbk.updater.ui.UpdatePresenter.4
                        @Override // com.bbk.updater.config.CallBack
                        public boolean execute() {
                            List<AppInfo> incompatibleAppList = Configs.IncompatibleAppsConfig.getIncompatibleAppList(UpdatePresenter.this.mContext, strPrepareAndroidUpgrageCheck, SystemClock.elapsedRealtime());
                            if (incompatibleAppList == null || incompatibleAppList.size() <= 0) {
                                return false;
                            }
                            AlertDialog.Builder builder = new AlertDialog.Builder(UpdatePresenter.this.mContext, CommonUtils.getDialogThemeId());
                            builder.setCancelable(false);
                            final AlertDialog alertDialogCreate = builder.create();
                            ButtonClickInterface buttonClickInterface = new ButtonClickInterface() { // from class: com.bbk.updater.ui.UpdatePresenter.4.1
                                @Override // com.bbk.updater.ui.dialogcontent.panel.ButtonClickInterface
                                public void onButtonClick(int i) {
                                    RxBus.a().c(new DialogEvent(41, ConstantsUtils.DialogType.ANDROID_UPGRADE_INCOMPATIBLE_APPS_WARNING, "activity_install_night"));
                                    String version = UpdatePresenter.this.mFotaUpdateInfo != null ? UpdatePresenter.this.mFotaUpdateInfo.getVersion() : "";
                                    LogUtils.d(UpdatePresenter.TAG, "Click to positive:flagVersion=" + version);
                                    PrefsUtils.putString(UpdatePresenter.this.mContext, PrefsUtils.Other.KEY_UPDATE_CONTINUE_INCOMPATIBLE_WARNING_VERSION, version);
                                    UpdatePresenter.this.checkAndSetSmartInstall();
                                    AlertDialog alertDialog2 = alertDialogCreate;
                                    if (alertDialog2 == null || !alertDialog2.isShowing()) {
                                        return;
                                    }
                                    alertDialogCreate.dismiss();
                                }
                            };
                            UpdatePresenter.this.mIncompatibleWarning = alertDialogCreate;
                            PopDialogUtils.showIncompatibleWarning(UpdatePresenter.this.mContext, alertDialogCreate, incompatibleAppList, strPrepareAndroidUpgrageCheck, "activity_install_night", false, buttonClickInterface);
                            return true;
                        }

                        @Override // com.bbk.updater.config.CallBack
                        public void executeFinally(boolean z) {
                            if (z) {
                                return;
                            }
                            UpdatePresenter.this.checkAndSetSmartInstall();
                        }
                    }));
                    return;
                }
                checkAndSetSmartInstall();
                return;
            }
            return;
        }
        if (eventId == 27) {
            if (this.sDownloadInfoManager.d(this.mFotaDownloadStatus, this.mVgcDownloadStatus)) {
                this.sDownloadInfoManager.b();
                return;
            }
            return;
        }
        if (eventId == 18) {
            if (CommonUtils.isAppAvailable(this.mContext.getApplicationContext(), UpdateActivity.APP_UPGRADE_PACKAGE_NAME)) {
                Intent intent = new Intent("com.iqoo.moduletrafficupgrade.setting");
                intent.setPackage(UpdateActivity.APP_UPGRADE_PACKAGE_NAME);
                intent.putExtra("source", 1);
                JumpUtils.startActivitySafety(this.mContext, intent);
                return;
            }
            return;
        }
        if (eventId != 19) {
            switch (eventId) {
                case 0:
                    AlertDialog alertDialog2 = this.mIncompatibleWarning;
                    if (alertDialog2 == null || !alertDialog2.isShowing()) {
                        final String strPrepareAndroidUpgrageCheck2 = CommonUtils.prepareAndroidUpgrageCheck(this.mContext, this.mFotaUpdateInfo);
                        if (TextUtils.isEmpty(strPrepareAndroidUpgrageCheck2)) {
                            clickToDownload();
                        } else {
                            LogUtils.d(TAG, "<--showIncompatibleWarning-->activity|downloadand_install|" + CommonUtils.countTime(new CallBack() { // from class: com.bbk.updater.ui.UpdatePresenter.2
                                @Override // com.bbk.updater.config.CallBack
                                public boolean execute() {
                                    List<AppInfo> incompatibleAppList = Configs.IncompatibleAppsConfig.getIncompatibleAppList(UpdatePresenter.this.mContext, strPrepareAndroidUpgrageCheck2, SystemClock.elapsedRealtime());
                                    if (incompatibleAppList == null || incompatibleAppList.size() <= 0) {
                                        return false;
                                    }
                                    AlertDialog.Builder builder = new AlertDialog.Builder(UpdatePresenter.this.mContext, CommonUtils.getDialogThemeId());
                                    builder.setCancelable(false);
                                    final AlertDialog alertDialogCreate = builder.create();
                                    ButtonClickInterface buttonClickInterface = new ButtonClickInterface() { // from class: com.bbk.updater.ui.UpdatePresenter.2.1
                                        @Override // com.bbk.updater.ui.dialogcontent.panel.ButtonClickInterface
                                        public void onButtonClick(int i) {
                                            RxBus.a().c(new DialogEvent(41, ConstantsUtils.DialogType.ANDROID_UPGRADE_INCOMPATIBLE_APPS_WARNING, "activity_downloadand_install"));
                                            String version = UpdatePresenter.this.mFotaUpdateInfo != null ? UpdatePresenter.this.mFotaUpdateInfo.getVersion() : "";
                                            LogUtils.d(UpdatePresenter.TAG, "Click to positive:flagVersion=" + version);
                                            PrefsUtils.putString(UpdatePresenter.this.mContext, PrefsUtils.Other.KEY_UPDATE_CONTINUE_INCOMPATIBLE_WARNING_VERSION, version);
                                            UpdatePresenter.this.clickToDownload();
                                            AlertDialog alertDialog3 = alertDialogCreate;
                                            if (alertDialog3 == null || !alertDialog3.isShowing()) {
                                                return;
                                            }
                                            alertDialogCreate.dismiss();
                                        }
                                    };
                                    UpdatePresenter.this.mIncompatibleWarning = alertDialogCreate;
                                    PopDialogUtils.showIncompatibleWarning(UpdatePresenter.this.mContext, alertDialogCreate, incompatibleAppList, strPrepareAndroidUpgrageCheck2, "activity_downloadand_install", false, buttonClickInterface);
                                    return true;
                                }

                                @Override // com.bbk.updater.config.CallBack
                                public void executeFinally(boolean z) {
                                    if (z) {
                                        return;
                                    }
                                    UpdatePresenter.this.clickToDownload();
                                }
                            }));
                        }
                    }
                    break;
                case 1:
                    if (this.mFotaUpdateInfo != null) {
                        com.bbk.updater.download.a.a(this.mContext).c("ota_pacakge", this.mFotaUpdateInfo.getFileName());
                    }
                    if (this.mVgcUpdateInfo != null) {
                        com.bbk.updater.download.a.a(this.mContext).c("vgc_package", this.mVgcUpdateInfo.getFileName());
                    }
                    if (!CommonUtils.isBatterySatisfied(this.mContext, com.bbk.vgc.a.a.a().e(), com.bbk.vgc.a.a.a().e())) {
                        Context context = this.mContext;
                        CommonUtils.postToast(context, StringUtils.getResFormatString(context, R.string.low_battery_can_not_download, com.bbk.vgc.a.a.a().e() + "%%"), 0);
                    } else if (isReadyForDownload()) {
                        refreshUI(DownloadControllerView.DownloadControllerStatus.PREPARING_DOWNLOAD, DownloadIndicatorView.DownloadIndicatorStatus.DOWNLOAD_PREPARING);
                    } else {
                        refreshUI(DownloadControllerView.DownloadControllerStatus.TO_DOWNLOAD, DownloadIndicatorView.DownloadIndicatorStatus.TO_DOWNLOAD);
                    }
                    break;
                case 2:
                    if (!clickEvent.isBackGround()) {
                        if (this.mIsResumeDownload) {
                            resumeDownload();
                        } else {
                            startDownload();
                        }
                    }
                    break;
                case 3:
                    if (com.bbk.updater.install.a.a.a(this.mFotaUpdateInfo, (c) null) && !com.bbk.updater.install.a.a.b(this.mFotaUpdateInfo)) {
                        InstallEvent installEvent = new InstallEvent(InstallEvent.TEMPETATURE_NOT_SATISFY);
                        installEvent.setTempetatureNeeded(this.mFotaUpdateInfo.getInstallTempetatureNeed(1));
                        installEvent.setInstallStrategy(BaseFrameUtils.InstallStrategy.INSTALL_NOW_ACTIVITY.toString());
                        installEvent.setFotaVersion(this.mFotaUpdateInfo.getVersion());
                        RxBus.a().c(installEvent);
                    } else {
                        AlertDialog alertDialog3 = this.mIncompatibleWarning;
                        if (alertDialog3 == null || !alertDialog3.isShowing()) {
                            final String strPrepareAndroidUpgrageCheck3 = CommonUtils.prepareAndroidUpgrageCheck(this.mContext, this.mFotaUpdateInfo);
                            if (TextUtils.isEmpty(strPrepareAndroidUpgrageCheck3)) {
                                checkAndInstall(false);
                            } else {
                                LogUtils.d(TAG, "<--showIncompatibleWarning-->activity|install_now|" + CommonUtils.countTime(new CallBack() { // from class: com.bbk.updater.ui.UpdatePresenter.3
                                    @Override // com.bbk.updater.config.CallBack
                                    public boolean execute() {
                                        List<AppInfo> incompatibleAppList = Configs.IncompatibleAppsConfig.getIncompatibleAppList(UpdatePresenter.this.mContext, strPrepareAndroidUpgrageCheck3, SystemClock.elapsedRealtime());
                                        if (incompatibleAppList == null || incompatibleAppList.size() <= 0) {
                                            return false;
                                        }
                                        AlertDialog.Builder builder = new AlertDialog.Builder(UpdatePresenter.this.mContext, CommonUtils.getDialogThemeId());
                                        builder.setCancelable(false);
                                        final AlertDialog alertDialogCreate = builder.create();
                                        ButtonClickInterface buttonClickInterface = new ButtonClickInterface() { // from class: com.bbk.updater.ui.UpdatePresenter.3.1
                                            @Override // com.bbk.updater.ui.dialogcontent.panel.ButtonClickInterface
                                            public void onButtonClick(int i) {
                                                RxBus.a().c(new DialogEvent(41, ConstantsUtils.DialogType.ANDROID_UPGRADE_INCOMPATIBLE_APPS_WARNING, "activity_install_now"));
                                                String version = UpdatePresenter.this.mFotaUpdateInfo != null ? UpdatePresenter.this.mFotaUpdateInfo.getVersion() : "";
                                                LogUtils.d(UpdatePresenter.TAG, "Click to positive:flagVersion=" + version);
                                                PrefsUtils.putString(UpdatePresenter.this.mContext, PrefsUtils.Other.KEY_UPDATE_CONTINUE_INCOMPATIBLE_WARNING_VERSION, version);
                                                UpdatePresenter.this.checkAndInstall(false);
                                                AlertDialog alertDialog4 = alertDialogCreate;
                                                if (alertDialog4 == null || !alertDialog4.isShowing()) {
                                                    return;
                                                }
                                                alertDialogCreate.dismiss();
                                            }
                                        };
                                        UpdatePresenter.this.mIncompatibleWarning = alertDialogCreate;
                                        PopDialogUtils.showIncompatibleWarning(UpdatePresenter.this.mContext, alertDialogCreate, incompatibleAppList, strPrepareAndroidUpgrageCheck3, "activity_install_now", false, buttonClickInterface);
                                        return true;
                                    }

                                    @Override // com.bbk.updater.config.CallBack
                                    public void executeFinally(boolean z) {
                                        if (z) {
                                            return;
                                        }
                                        UpdatePresenter.this.checkAndInstall(false);
                                    }
                                }));
                            }
                        }
                    }
                    break;
                case 4:
                    if (this.sDownloadInfoManager.d(this.mFotaDownloadStatus, this.mVgcDownloadStatus)) {
                        m10getMvpView().showOneDialog(1010);
                    }
                    break;
                case 5:
                    if (!CommonUtils.isBatterySatisfied(this.mContext, com.bbk.vgc.a.a.a().e(), com.bbk.vgc.a.a.a().e())) {
                        Context context2 = this.mContext;
                        CommonUtils.postToast(context2, StringUtils.getResFormatString(context2, R.string.low_battery_can_not_download, com.bbk.vgc.a.a.a().e() + "%%"), 0);
                    } else {
                        networkNoticeBeforeDownload(true);
                    }
                    break;
                case 6:
                    if (!this.sDownloadInfoManager.n()) {
                        refreshUI(DownloadControllerView.DownloadControllerStatus.TO_DOWNLOAD, DownloadIndicatorView.DownloadIndicatorStatus.TO_DOWNLOAD);
                    }
                    JumpUtils.jumpToConnectNetwork(this.mContext);
                    break;
                case 7:
                    Intent intent2 = new Intent(this.mContext, (Class<?>) SettingsActivity.class);
                    intent2.putExtra(ConstantsUtils.BroadCastReceiverAction.EXTRA_START_UPDATE_ACTIVITY_TRIGGER, "UpdateActivtiy_click_set");
                    JumpUtils.startActivitySafety(this.mContext, intent2);
                    break;
                case 8:
                    m10getMvpView().finishJob();
                    break;
                case 9:
                    if ((!this.sDownloadInfoManager.n() && !this.sDownloadInfoManager.f("vgc_package")) || this.sDownloadInfoManager.p("ota_pacakge") || this.sDownloadInfoManager.p("vgc_package")) {
                        refreshUI(DownloadControllerView.DownloadControllerStatus.TO_DOWNLOAD, DownloadIndicatorView.DownloadIndicatorStatus.TO_DOWNLOAD);
                        break;
                    }
                    break;
                case 10:
                    Intent intent3 = new Intent(this.mContext, (Class<?>) LogActivity.class);
                    intent3.putExtra(ConstantsUtils.BroadCastReceiverAction.EXTRA_START_UPDATE_ACTIVITY_TRIGGER, "UpdateActivtiy_click_current");
                    JumpUtils.startActivitySafety(this.mContext, intent3);
                    break;
                default:
                    switch (eventId) {
                        case 100:
                            if (com.bbk.updater.install.a.a.a(this.mFotaUpdateInfo, (c) null) && !com.bbk.updater.install.a.a.a(this.mFotaUpdateInfo)) {
                                InstallEvent installEvent2 = new InstallEvent(InstallEvent.TEMPETATURE_NOT_SATISFY);
                                installEvent2.setTempetatureNeeded(this.mFotaUpdateInfo.getInstallTempetatureNeed(1));
                                installEvent2.setInstallStrategy("CONTINUE_INSTALL_ACTIVITY");
                                installEvent2.setFotaVersion(this.mFotaUpdateInfo.getVersion());
                                installEvent2.setInstallStatus(3);
                                RxBus.a().c(installEvent2);
                            } else {
                                com.bbk.updater.install.abinstall.b.a(this.mContext).a(false, false, false);
                            }
                            break;
                        case 101:
                            com.bbk.updater.install.a.a.a(this.mContext, BaseFrameUtils.UpdateMethod.UPDATE_NOW_ACTIVITY);
                            break;
                        case 102:
                            RxBus.a().c(new SmartInstallEvent(1));
                            break;
                        case 103:
                            if (com.bbk.updater.install.a.a.a(this.mFotaUpdateInfo, (c) null) && !com.bbk.updater.install.a.a.a(this.mFotaUpdateInfo)) {
                                InstallEvent installEvent3 = new InstallEvent(InstallEvent.TEMPETATURE_NOT_SATISFY);
                                installEvent3.setTempetatureNeeded(this.mFotaUpdateInfo.getInstallTempetatureNeed(1));
                                installEvent3.setInstallStrategy("CONTINUE_VERIFY_ACTIVITY");
                                installEvent3.setFotaVersion(this.mFotaUpdateInfo.getVersion());
                                installEvent3.setInstallStatus(7);
                                RxBus.a().c(installEvent3);
                            } else {
                                com.bbk.updater.install.abinstall.b.a(this.mContext).a(false, false, false);
                            }
                            break;
                    }
                    break;
            }
            return;
        }
        JumpUtils.jumpToTipsExplore(this.mContext);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clickToDownload() {
        if (!CommonUtils.isBatterySatisfied(this.mContext, com.bbk.vgc.a.a.a().e(), com.bbk.vgc.a.a.a().e())) {
            Context context = this.mContext;
            CommonUtils.postToast(context, StringUtils.getResFormatString(context, R.string.low_battery_can_not_download, com.bbk.vgc.a.a.a().e() + "%%"), 0);
        } else if (isReadyForDownload()) {
            refreshUI(DownloadControllerView.DownloadControllerStatus.PREPARING_DOWNLOAD, DownloadIndicatorView.DownloadIndicatorStatus.DOWNLOAD_PREPARING);
        } else {
            refreshUI(DownloadControllerView.DownloadControllerStatus.TO_DOWNLOAD, DownloadIndicatorView.DownloadIndicatorStatus.TO_DOWNLOAD);
        }
        PopDialogUtils.checkIfNeedPopAutoDownloadSwitchInduce(this.mContext.getApplicationContext());
    }

    /* JADX WARN: Removed duplicated region for block: B:23:0x0036  */
    @com.vivo.updaterbaseframe.rx.RxBus.a(a = com.vivo.updaterbaseframe.rx.RxBus.RunningThreadType.mainThread)
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void onSmartInstallEvent(com.bbk.updater.rx.event.SmartInstallEvent r6) {
        /*
            r5 = this;
            int r0 = r6.getEventId()
            r1 = 1
            if (r0 == r1) goto Lf
            r2 = 2
            if (r0 == r2) goto Lf
            r2 = 4
            if (r0 == r2) goto Lf
            goto L85
        Lf:
            boolean r6 = r6.isFromPrivacy()
            r0 = 0
            if (r6 == 0) goto L2d
            com.bbk.updater.bean.UpdateInfo r6 = r5.mFotaUpdateInfo
            if (r6 == 0) goto L36
            java.lang.String r6 = r6.getVersion()
            com.bbk.updater.bean.VgcUpdateInfo r2 = r5.mVgcUpdateInfo
            if (r2 == 0) goto L27
            java.lang.String r2 = r2.getVersion()
            goto L28
        L27:
            r2 = r0
        L28:
            java.lang.String r6 = com.bbk.updater.utils.VersionUtils.getVersionSplice(r6, r2)
            goto L50
        L2d:
            com.bbk.updater.bean.UpdateInfo r6 = r5.mFotaUpdateInfo
            if (r6 != 0) goto L38
            com.bbk.updater.bean.VgcUpdateInfo r6 = r5.mVgcUpdateInfo
            if (r6 == 0) goto L36
            goto L38
        L36:
            r6 = r0
            goto L50
        L38:
            com.bbk.updater.bean.UpdateInfo r6 = r5.mFotaUpdateInfo
            if (r6 == 0) goto L41
            java.lang.String r6 = r6.getVersion()
            goto L42
        L41:
            r6 = r0
        L42:
            com.bbk.updater.bean.VgcUpdateInfo r2 = r5.mVgcUpdateInfo
            if (r2 == 0) goto L4b
            java.lang.String r2 = r2.getVersion()
            goto L4c
        L4b:
            r2 = r0
        L4c:
            java.lang.String r6 = com.bbk.updater.utils.VersionUtils.getVersionSplice(r6, r2)
        L50:
            boolean r2 = android.text.TextUtils.isEmpty(r6)
            if (r2 != 0) goto L85
            android.content.Context r2 = r5.mContext
            boolean r2 = com.bbk.updater.utils.CommonUtils.isNightInstall(r2, r6)
            if (r2 == 0) goto L85
            com.bbk.updater.bean.UpdateInfo r2 = r5.mFotaUpdateInfo
            boolean r0 = com.bbk.updater.install.a.a.a(r2, r0)
            com.vivo.updaterbaseframe.b.b r2 = r5.m10getMvpView()
            com.bbk.updater.ui.UpdateView r2 = (com.bbk.updater.ui.UpdateView) r2
            boolean r3 = com.bbk.updater.install.a.a.a()
            r4 = 0
            if (r3 == 0) goto L75
            if (r0 != 0) goto L75
            r3 = r1
            goto L76
        L75:
            r3 = r4
        L76:
            if (r0 == 0) goto L81
            com.bbk.updater.bean.UpdateInfo r5 = r5.mFotaUpdateInfo
            boolean r5 = com.bbk.updater.install.a.a.b(r5)
            if (r5 != 0) goto L81
            goto L82
        L81:
            r1 = r4
        L82:
            r2.showNightInstallPlanTips(r6, r3, r1)
        L85:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.bbk.updater.ui.UpdatePresenter.onSmartInstallEvent(com.bbk.updater.rx.event.SmartInstallEvent):void");
    }

    @RxBus.a(a = RxBus.RunningThreadType.mainThread)
    private void onDialogEvent(DialogEvent dialogEvent) {
        int eventId = dialogEvent.getEventId();
        if (eventId == 17) {
            Settings.Global.putInt(this.mContext.getContentResolver(), ConstantsUtils.INTELLIGENT_INSTALLATION_KEY_IN_DATABASE, 1);
            CommonUtils.putIntIntoSettings(this.mContext, ConstantsUtils.AUTO_DOWNLOAD_KEY_IN_DATABASE, 11);
        } else {
            if (eventId != 18) {
                return;
            }
            Settings.Global.putInt(this.mContext.getContentResolver(), ConstantsUtils.INTELLIGENT_INSTALLATION_KEY_IN_DATABASE, 0);
        }
    }

    @RxBus.a(a = RxBus.RunningThreadType.mainThread)
    private void onDownloadEvent(DownloadEvent downloadEvent) {
        List<AppInfo> incompatibleAppList;
        int eventId = downloadEvent.getEventId();
        LogUtils.i(TAG, "Get download event : " + eventId);
        if (eventId == 1) {
            refreshUI(DownloadControllerView.DownloadControllerStatus.TO_DOWNLOAD, DownloadIndicatorView.DownloadIndicatorStatus.TO_DOWNLOAD);
            this.mDownloadProgress = downloadEvent.getProgress();
            m10getMvpView().refreshProgress(this.mDownloadProgress);
        } else {
            if (eventId != 2) {
                if (eventId != 4) {
                    if (eventId != 8) {
                        return;
                    }
                    fetchUpdateInfo();
                    m10getMvpView().changeToNoVersion();
                    return;
                }
                com.bbk.updater.download.a aVar = this.sDownloadInfoManager;
                if (aVar == null || !aVar.d(downloadEvent.getFotaStatus(), downloadEvent.getVgcStatus())) {
                    refreshUI(DownloadControllerView.DownloadControllerStatus.PREPARING_DOWNLOAD, DownloadIndicatorView.DownloadIndicatorStatus.DOWNLOAD_PREPARING);
                    return;
                }
                return;
            }
            this.mDownloadProgress = downloadEvent.getProgress();
            LogUtils.i(TAG, "Download progress : " + this.mDownloadProgress);
            m10getMvpView().refreshProgress(this.mDownloadProgress);
        }
        int fotaStatus = downloadEvent.getFotaStatus();
        int vgcStatus = downloadEvent.getVgcStatus();
        int resumeNet = downloadEvent.getResumeNet();
        LogUtils.i(TAG, "currentFotaStatus: " + fotaStatus + ", currentVgcStatus: " + vgcStatus + ", resumeNet: " + resumeNet);
        if (this.mDcStatus != DownloadControllerView.DownloadControllerStatus.RUNNING) {
            if (fotaStatus == 1) {
                this.mFotaDownloadStatus = -1;
            }
            if (vgcStatus == 1) {
                this.mFotaDownloadStatus = -1;
            }
        }
        if (fotaStatus == this.mFotaDownloadStatus && vgcStatus == this.mVgcDownloadStatus && (this.mResumeNet == resumeNet || resumeNet != 1 || !this.sDownloadInfoManager.d(fotaStatus, vgcStatus))) {
            return;
        }
        this.mFotaDownloadStatus = fotaStatus;
        this.mVgcDownloadStatus = vgcStatus;
        if (this.sDownloadInfoManager.f(fotaStatus, vgcStatus)) {
            refreshUI(DownloadControllerView.DownloadControllerStatus.FAILED, DownloadIndicatorView.DownloadIndicatorStatus.DOWNLOAD_FAILED);
            LogUtils.i(TAG, "Download status is mistaken! Update interface!");
            return;
        }
        if (this.sDownloadInfoManager.a(fotaStatus, vgcStatus)) {
            if (this.sDownloadInfoManager.t()) {
                refreshUI(DownloadControllerView.DownloadControllerStatus.FAILED, DownloadIndicatorView.DownloadIndicatorStatus.INSUFFICIENT_SPACE);
                LogUtils.i(TAG, "Space is insufficient! Update interface!");
                return;
            } else {
                refreshUI(DownloadControllerView.DownloadControllerStatus.PAUSED, DownloadIndicatorView.DownloadIndicatorStatus.DOWNLOAD_PAUSED);
                LogUtils.i(TAG, "Download status is paused! Update interface!");
                return;
            }
        }
        if (this.sDownloadInfoManager.c(fotaStatus, vgcStatus)) {
            refreshUI(DownloadControllerView.DownloadControllerStatus.FAILED, DownloadIndicatorView.DownloadIndicatorStatus.DOWNLOAD_WAITING_TO_RETRY);
            LogUtils.i(TAG, "Download status is waiting to retry! Update interface!");
            return;
        }
        if (this.sDownloadInfoManager.b(fotaStatus, vgcStatus)) {
            refreshUI(DownloadControllerView.DownloadControllerStatus.PREPARING_DOWNLOAD, DownloadIndicatorView.DownloadIndicatorStatus.DOWNLOAD_PREPARING);
            LogUtils.i(TAG, "Download status is preparing! Update interface!");
            return;
        }
        if (this.sDownloadInfoManager.d(fotaStatus, vgcStatus)) {
            this.mResumeNet = resumeNet;
            refreshUI(DownloadControllerView.DownloadControllerStatus.RUNNING, DownloadIndicatorView.DownloadIndicatorStatus.DOWNLOAD_RUNNING);
            LogUtils.i(TAG, "Download status is running! Update interface!");
            return;
        }
        if (this.sDownloadInfoManager.e(fotaStatus, vgcStatus) && this.mDownloadProgress == 1.0f) {
            String string = PrefsUtils.getString(this.mContext, "download_type", ConstantsUtils.DownloadType.AUTO_DOWNLOAD.toString());
            LogUtils.i(TAG, "downloadType:" + string);
            String strPrepareAndroidUpgrageCheck = CommonUtils.prepareAndroidUpgrageCheck(this.mContext, this.mFotaUpdateInfo);
            boolean z = false;
            if (!TextUtils.isEmpty(strPrepareAndroidUpgrageCheck) && (incompatibleAppList = Configs.IncompatibleAppsConfig.getIncompatibleAppList(this.mContext, strPrepareAndroidUpgrageCheck, false)) != null && incompatibleAppList.size() > 0) {
                z = true;
            }
            LogUtils.i(TAG, "Is Ard Ver Incompatible: " + z);
            if ((this.mFotaDownloadStatus != -1 || this.mVgcDownloadStatus != -1) && m10getMvpView().isResumeNow() && !ConstantsUtils.DownloadType.AUTO_DOWNLOAD.toString().equals(string) && !ConstantsUtils.DownloadType.DIALOG_BACKGROUND_DOWNLOAD.toString().equals(string) && !ConstantsUtils.DownloadType.NOTI_BACKGROUND_DOWNLOAD.toString().equals(string) && !ConstantsUtils.DownloadType.NOTI_RESUME_DOWNLOAD.toString().equals(string) && !CommonUtils.isCalling(this.mContext) && !com.bbk.vgc.a.a.a().f() && !z) {
                if (!com.bbk.updater.install.a.a.a(this.mFotaUpdateInfo, (c) null) || com.bbk.updater.install.a.a.b(this.mFotaUpdateInfo)) {
                    checkAndInstall(true);
                    return;
                }
                InstallEvent installEvent = new InstallEvent(InstallEvent.TEMPETATURE_NOT_SATISFY);
                installEvent.setTempetatureNeeded(this.mFotaUpdateInfo.getInstallTempetatureNeed(1));
                installEvent.setInstallStrategy(BaseFrameUtils.InstallStrategy.INSTALL_DIRECT_ACTIVITY.toString());
                installEvent.setFotaVersion(this.mFotaUpdateInfo.getVersion());
                RxBus.a().c(installEvent);
                return;
            }
            LogUtils.i(TAG, "Download status is succeed, load log! Update interface!");
            refreshUI(DownloadControllerView.DownloadControllerStatus.TO_INSTALL, DownloadIndicatorView.DownloadIndicatorStatus.DOWNLOAD_SUCCEED);
        }
    }

    @RxBus.a(a = RxBus.RunningThreadType.mainThread)
    private void onCheckEvent(CheckEvent checkEvent) {
        if (checkEvent == null || checkEvent.isBackGround()) {
            LogUtils.i(TAG, "check event from background download.");
            return;
        }
        int eventId = checkEvent.getEventId();
        LogUtils.i(TAG, "Get check event : " + eventId);
        if (eventId == 1) {
            m10getMvpView().setCheckIndicatorStatus(CheckIndicatorView.CheckIndicatorStatus.CHECKING);
            return;
        }
        if (eventId == 2) {
            m10getMvpView().setCheckIndicatorStatus(CheckIndicatorView.CheckIndicatorStatus.ERROR_SERVER_RESPONSE);
            return;
        }
        if (eventId == 4) {
            m10getMvpView().setCheckIndicatorStatus(CheckIndicatorView.CheckIndicatorStatus.ERROR_CONNECT_TO_SERVER);
            return;
        }
        if (eventId == 8) {
            m10getMvpView().changeToNoVersion();
            m10getMvpView().setCheckIndicatorStatus(CheckIndicatorView.CheckIndicatorStatus.UP_TO_DATE);
            return;
        }
        if (eventId == 16) {
            UpdateInfo fotaUpdateInfo = checkEvent.getFotaUpdateInfo();
            VgcUpdateInfo vgcUpdateInfo = checkEvent.getVgcUpdateInfo();
            int iCheckPackageAvailableType = CommonUtils.checkPackageAvailableType(fotaUpdateInfo, vgcUpdateInfo);
            if (iCheckPackageAvailableType == 1) {
                vgcUpdateInfo = null;
            } else if (iCheckPackageAvailableType == 2) {
                fotaUpdateInfo = null;
            }
            refreshUpdateInfo(fotaUpdateInfo, vgcUpdateInfo);
            refreshLogContent(this.mFotaUpdateInfo, this.mVgcUpdateInfo);
            refreshUI(DownloadControllerView.DownloadControllerStatus.TO_DOWNLOAD, DownloadIndicatorView.DownloadIndicatorStatus.TO_DOWNLOAD);
            return;
        }
        if (eventId == 64) {
            LogUtils.i(TAG, "CheckEvent: CHECKED_LOG_LOAD_SUCCEED");
            refreshUI(DownloadControllerView.DownloadControllerStatus.TO_INSTALL, DownloadIndicatorView.DownloadIndicatorStatus.DOWNLOAD_SUCCEED);
            com.bbk.updater.c.b.k(this.mContext.getApplicationContext());
            return;
        }
        if (eventId == 128 || eventId == 256) {
            refreshUI(DownloadControllerView.DownloadControllerStatus.TO_DOWNLOAD, DownloadIndicatorView.DownloadIndicatorStatus.CANNOT_CONNECT_SERVER);
            return;
        }
        if (eventId == 512) {
            networkNoticeBeforeDownload(false);
            return;
        }
        if (eventId != 4096) {
            if (eventId != 8192) {
                return;
            }
            m10getMvpView().changeToNoVersion();
            m10getMvpView().setCheckIndicatorStatus(CheckIndicatorView.CheckIndicatorStatus.NETWORK_DISABLED);
            return;
        }
        boolean zIsDataNetTypeCheck = checkEvent.isDataNetTypeCheck();
        LogUtils.i(TAG, "onCheckEvent isNetChecked = " + zIsDataNetTypeCheck);
        if (zIsDataNetTypeCheck) {
            if (com.bbk.vgc.a.a.a().c(this.mContext, 1) != 1 && CommonUtils.getNetworkConnectType(this.mContext).equals(ConstantsUtils.NetWorkType.MOBILE)) {
                m10getMvpView().showOneDialog(1006);
                return;
            } else {
                checkUpdateForDESysDTag();
                return;
            }
        }
        checkUpdateForDESysDTag();
    }

    @RxBus.a(a = RxBus.RunningThreadType.mainThread)
    private void onCotaStatusChangeEvent(CotaStatusChangeEvent cotaStatusChangeEvent) {
        LogUtils.i(TAG, "onCotaStatusChangeEvent, changeId = " + cotaStatusChangeEvent.getEventId());
        if (cotaStatusChangeEvent.getEventId() != 1) {
            return;
        }
        m10getMvpView().resetCotaView();
    }

    @RxBus.a(a = RxBus.RunningThreadType.mainThread)
    private void onTypeChangeEvent(TypeChangeEvent typeChangeEvent) {
        if (typeChangeEvent.getEventId() != 1) {
            return;
        }
        this.mUseDataCheckChanged = typeChangeEvent.getType() == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshUpdateInfo(UpdateInfo updateInfo, VgcUpdateInfo vgcUpdateInfo) {
        this.mFotaUpdateInfo = updateInfo;
        this.mVgcUpdateInfo = vgcUpdateInfo;
        if (updateInfo != null) {
            m10getMvpView().recordCrossVersion(this.mFotaUpdateInfo.getCrossVersion(), this.mFotaUpdateInfo.getStyleColor());
        }
        m10getMvpView().recordOSVersion(VersionUtils.getOsAlias());
        m10getMvpView().recordTotalSize(getTotalLength());
        UpdateInfo updateInfo2 = this.mFotaUpdateInfo;
        String version = updateInfo2 != null ? updateInfo2.getVersion() : null;
        UpdateView mvpView = m10getMvpView();
        VgcUpdateInfo vgcUpdateInfo2 = this.mVgcUpdateInfo;
        String version2 = vgcUpdateInfo2 != null ? vgcUpdateInfo2.getVersion() : null;
        VgcUpdateInfo vgcUpdateInfo3 = this.mVgcUpdateInfo;
        mvpView.recordVersion(VersionUtils.getVersionSplice(version, version2, vgcUpdateInfo3 != null ? vgcUpdateInfo3.getVersionShow() : null), version);
    }

    public void refreshLogContent(UpdateInfo updateInfo, VgcUpdateInfo vgcUpdateInfo) {
        m10getMvpView().showUpdateLog(WebHelper.getInstance(this.mContext).getLogHtmlPathAndCacheIt(updateInfo, vgcUpdateInfo));
        m10getMvpView().recordIsFullPackage(isNeedToShowBrokenTips(updateInfo, vgcUpdateInfo));
    }

    public long getTotalLength() {
        return CommonUtils.getTotalPackageLength(this.mFotaUpdateInfo, this.mVgcUpdateInfo).longValue();
    }

    private void fetchUpdateInfo() {
        LogUtils.i(TAG, "Fetch update info!");
        int i = AnonymousClass12.$SwitchMap$com$bbk$updater$utils$ConstantsUtils$NetWorkType[this.mPreNetworkType.ordinal()];
        if (i != 1) {
            if (i == 2) {
                m10getMvpView().showOneDialog(1001);
                m10getMvpView().setCheckIndicatorStatus(CheckIndicatorView.CheckIndicatorStatus.NETWORK_DISABLED);
                return;
            }
        } else if (com.bbk.vgc.a.a.a().a(this.mContext)) {
            m10getMvpView().showOneDialog(1013);
            m10getMvpView().setCheckIndicatorStatus(CheckIndicatorView.CheckIndicatorStatus.NETWORK_ROAMING_FORBID_UPDATE);
            return;
        }
        checkUpdate();
    }

    /* JADX WARN: Removed duplicated region for block: B:18:0x002d  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private boolean isNeedToShowBrokenTips(com.bbk.updater.bean.UpdateInfo r4, com.bbk.updater.bean.VgcUpdateInfo r5) {
        /*
            r3 = this;
            android.content.Context r3 = r3.mContext
            boolean r3 = com.bbk.updater.utils.CommonUtils.isSystemBroken(r3)
            r0 = 1
            if (r3 == 0) goto L2d
            if (r4 != 0) goto Ld
            if (r5 == 0) goto L2d
        Ld:
            r3 = 0
            int r1 = com.bbk.updater.utils.CommonUtils.checkPackageAvailableType(r4, r5)
            if (r1 == r0) goto L20
            r2 = 3
            if (r1 != r2) goto L18
            goto L20
        L18:
            r4 = 2
            if (r1 != r4) goto L24
            java.lang.String r3 = r5.getType()
            goto L24
        L20:
            java.lang.String r3 = r4.getType()
        L24:
            java.lang.String r4 = "full"
            boolean r3 = r4.equals(r3)
            if (r3 == 0) goto L2d
            goto L2e
        L2d:
            r0 = 0
        L2e:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "isNeedToShowBrokenTips "
            r3.append(r4)
            r3.append(r0)
            java.lang.String r3 = r3.toString()
            java.lang.String r4 = "Updater/UpdatePresenter"
            com.bbk.updater.utils.LogUtils.i(r4, r3)
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.bbk.updater.ui.UpdatePresenter.isNeedToShowBrokenTips(com.bbk.updater.bean.UpdateInfo, com.bbk.updater.bean.VgcUpdateInfo):boolean");
    }

    private void checkUpdate() {
        long j = PrefsUtils.getLong(this.mContext, PrefsUtils.Check.KEY_LAST_SHOW_OPEN_AUTO_UPGRADE_PACKAGE_TIME, -1L);
        boolean zCheckTimeBetweenOneAndSevenDay = j == -1 ? true : CommonUtils.checkTimeBetweenOneAndSevenDay(j);
        int iA = com.bbk.vgc.a.a.a().a(0);
        int iB = com.bbk.vgc.a.a.a().b(this.mContext, 1);
        boolean z = PrefsUtils.getBoolean(this.mContext, PrefsUtils.Check.KEY_OPEN_AUTO_CHECK_UPGRADE_PACKAGE_NO_MORE_PROMPT, false);
        if (iA == 1 && iB != 1 && !z && zCheckTimeBetweenOneAndSevenDay && !this.mShowedOpenAutoCheck) {
            this.mShowedOpenAutoCheck = true;
            m10getMvpView().showOneDialog(1005);
            return;
        }
        int iB2 = com.bbk.vgc.a.a.a().b(0);
        int iC = com.bbk.vgc.a.a.a().c(this.mContext, 1);
        if (iB2 == 1 && iC != 1 && CommonUtils.getNetworkConnectType(this.mContext).equals(ConstantsUtils.NetWorkType.MOBILE)) {
            m10getMvpView().showOneDialog(1006);
            return;
        }
        b bVar = new b(this.mContext.getApplicationContext());
        this.mCheckUpdateTask = bVar;
        bVar.a(BaseFrameUtils.CheckTrigeType.MANUL);
        this.mCheckUpdateTask.execute(new Void[0]);
    }

    private void checkUpdateForDESysDTag() {
        b bVar = new b(this.mContext.getApplicationContext());
        this.mCheckUpdateTask = bVar;
        bVar.execute(new Void[0]);
    }

    /* JADX WARN: Removed duplicated region for block: B:18:0x0057  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private boolean isReadyForDownload() {
        /*
            r6 = this;
            com.bbk.updater.bean.UpdateInfo r0 = r6.mFotaUpdateInfo
            r1 = 1
            r2 = 0
            if (r0 != 0) goto L1c
            com.bbk.updater.bean.VgcUpdateInfo r0 = r6.mVgcUpdateInfo
            if (r0 != 0) goto L1c
            android.content.Context r0 = r6.mContext
            r1 = 2131689663(0x7f0f00bf, float:1.9008348E38)
            com.bbk.updater.utils.CommonUtils.postToast(r0, r1, r2)
            com.vivo.updaterbaseframe.b.b r0 = r6.m10getMvpView()
            com.bbk.updater.ui.UpdateView r0 = (com.bbk.updater.ui.UpdateView) r0
            r0.finishJob()
            goto L3b
        L1c:
            android.content.Context r0 = r6.mContext
            com.bbk.updater.utils.ConstantsUtils$NetWorkType r0 = com.bbk.updater.utils.CommonUtils.getNetworkConnectType(r0)
            int[] r3 = com.bbk.updater.ui.UpdatePresenter.AnonymousClass12.$SwitchMap$com$bbk$updater$utils$ConstantsUtils$NetWorkType
            int r0 = r0.ordinal()
            r0 = r3[r0]
            if (r0 == r1) goto L3d
            r3 = 2
            if (r0 == r3) goto L30
            goto L55
        L30:
            com.vivo.updaterbaseframe.b.b r0 = r6.m10getMvpView()
            com.bbk.updater.ui.UpdateView r0 = (com.bbk.updater.ui.UpdateView) r0
            r1 = 1001(0x3e9, float:1.403E-42)
            r0.showOneDialog(r1)
        L3b:
            r1 = r2
            goto L55
        L3d:
            com.bbk.vgc.a.a r0 = com.bbk.vgc.a.a.a()
            android.content.Context r3 = r6.mContext
            boolean r0 = r0.a(r3)
            if (r0 == 0) goto L55
            com.vivo.updaterbaseframe.b.b r0 = r6.m10getMvpView()
            com.bbk.updater.ui.UpdateView r0 = (com.bbk.updater.ui.UpdateView) r0
            r1 = 1012(0x3f4, float:1.418E-42)
            r0.showOneDialog(r1)
            goto L3b
        L55:
            if (r1 == 0) goto L83
            boolean r0 = r6.mFromTrialVersion
            if (r0 == 0) goto L71
            java.lang.String r0 = "Updater/UpdatePresenter"
            java.lang.String r2 = "prepareDownloadTrialVersion"
            com.bbk.updater.utils.LogUtils.i(r0, r2)
            com.bbk.trialversion.b r0 = com.bbk.trialversion.b.a()
            android.content.Context r2 = r6.mContext
            com.bbk.updater.ui.UpdatePresenter$5 r3 = new com.bbk.updater.ui.UpdatePresenter$5
            r3.<init>()
            r0.a(r2, r3)
            goto L83
        L71:
            com.bbk.updater.download.a.b r0 = new com.bbk.updater.download.a.b
            android.content.Context r3 = r6.mContext
            com.bbk.updater.bean.UpdateInfo r4 = r6.mFotaUpdateInfo
            com.bbk.updater.bean.VgcUpdateInfo r5 = r6.mVgcUpdateInfo
            r0.<init>(r3, r4, r5)
            r6.mPrepareDownloadCheckTask = r0
            java.lang.Void[] r6 = new java.lang.Void[r2]
            r0.execute(r6)
        L83:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.bbk.updater.ui.UpdatePresenter.isReadyForDownload():boolean");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void networkNoticeBeforeDownload(boolean z) {
        this.mIsResumeDownload = z;
        int i = AnonymousClass12.$SwitchMap$com$bbk$updater$utils$ConstantsUtils$NetWorkType[CommonUtils.getNetworkConnectType(this.mContext).ordinal()];
        if (i != 1) {
            if (i == 2) {
                m10getMvpView().showOneDialog(1001);
                return;
            }
            if (i != 3 && i != 4) {
                LogUtils.i(TAG, "Network status can not be matched!");
                return;
            } else if (z) {
                resumeDownload();
                return;
            } else {
                startDownload();
                return;
            }
        }
        int iMobileNetworkDownloadCheck = CommonUtils.mobileNetworkDownloadCheck(this.mContext);
        LogUtils.i(TAG, "networkNoticeBeforeDownload, DIALOG_MOBILE_NETWORK checkType = " + iMobileNetworkDownloadCheck);
        if (iMobileNetworkDownloadCheck == 4) {
            m10getMvpView().showOneDialog(1012);
            return;
        }
        if (iMobileNetworkDownloadCheck == 3) {
            m10getMvpView().showOneDialog(1011);
            return;
        }
        if (iMobileNetworkDownloadCheck == 2) {
            m10getMvpView().showOneDialog(1009);
            return;
        }
        if (iMobileNetworkDownloadCheck == 1) {
            m10getMvpView().showOneDialog(1007);
        } else if (iMobileNetworkDownloadCheck == 0) {
            m10getMvpView().showOneDialog(1008);
        } else {
            m10getMvpView().showOneDialog(1002);
        }
    }

    private void resumeDownload() {
        com.bbk.updater.download.a aVarA = com.bbk.updater.download.a.a(this.mContext);
        UpdateInfo updateInfoV = com.bbk.updater.download.a.a(this.mContext).v();
        VgcUpdateInfo vgcUpdateInfoU = com.bbk.updater.download.a.a(this.mContext).u();
        if (CommonUtils.checkEnoughStorage(this.mContext, ((((updateInfoV != null ? updateInfoV.getFileLength() : 0L) - aVarA.h()) + (vgcUpdateInfoU != null ? vgcUpdateInfoU.getFileLength() : 0L)) - aVarA.i()) + (updateInfoV != null ? updateInfoV.getReservedStorage() : 0L), true)) {
            if (com.bbk.updater.download.a.a(this.mContext).o()) {
                com.bbk.updater.download.a.a(this.mContext).a("ota_pacakge", !CommonUtils.isNetworkWifi(this.mContext), true, true, ConstantsUtils.DownloadType.ACTIVITY_RESUME_DOWNLOAD.toString());
            }
            if (com.bbk.updater.download.a.a(this.mContext).p()) {
                com.bbk.updater.download.a.a(this.mContext).a("vgc_package", !CommonUtils.isNetworkWifi(this.mContext), true, true, ConstantsUtils.DownloadType.ACTIVITY_RESUME_DOWNLOAD.toString());
            }
            com.bbk.updater.download.a.a(this.mContext).c("ota_pacakge");
        }
    }

    private void startDownload() {
        com.bbk.updater.download.a aVar;
        boolean z;
        UpdateInfo updateInfo = this.mFotaUpdateInfo;
        long reservedStorage = updateInfo != null ? updateInfo.getReservedStorage() : 0L;
        Context context = this.mContext;
        long jLongValue = CommonUtils.getTotalPackageLength(this.mFotaUpdateInfo, this.mVgcUpdateInfo).longValue() + reservedStorage;
        boolean z2 = true;
        if (!CommonUtils.checkEnoughStorage(context, jLongValue, true)) {
            refreshUI(DownloadControllerView.DownloadControllerStatus.TO_DOWNLOAD, DownloadIndicatorView.DownloadIndicatorStatus.TO_DOWNLOAD);
            return;
        }
        if (!CommonUtils.isNetworkConnect(this.mContext) || (aVar = this.sDownloadInfoManager) == null) {
            return;
        }
        int iL = aVar.l();
        int iM = this.sDownloadInfoManager.m();
        boolean z3 = this.sDownloadInfoManager.a(iL) && !this.sDownloadInfoManager.d(iL);
        boolean z4 = this.sDownloadInfoManager.a(iM) && !this.sDownloadInfoManager.d(iM);
        UpdateInfo updateInfo2 = this.mFotaUpdateInfo;
        boolean z5 = updateInfo2 != null && com.vivo.updaterassistant.a.a(updateInfo2);
        ArrayList<com.vivo.updaterbaseframe.a.a> arrayList = new ArrayList<>();
        ArrayList<com.vivo.updaterbaseframe.a.a> arrayList2 = new ArrayList<>();
        if (this.mFotaUpdateInfo == null || z3 || this.sDownloadInfoManager.d(iL)) {
            if (z3) {
                com.vivo.updaterbaseframe.a.a aVar2 = new com.vivo.updaterbaseframe.a.a();
                aVar2.a(this.mFotaUpdateInfo);
                aVar2.a("ota_pacakge");
                aVar2.a(true);
                aVar2.a(!CommonUtils.isNetworkWifi(this.mContext) ? 0 : 2);
                aVar2.b(ConstantsUtils.DownloadType.NORMAL_MANUAL_DOWNLOAD.toString());
                arrayList2.add(aVar2);
            }
            z = false;
        } else {
            com.vivo.updaterbaseframe.a.a aVar3 = new com.vivo.updaterbaseframe.a.a();
            aVar3.a(this.mFotaUpdateInfo);
            aVar3.a("ota_pacakge");
            aVar3.a(true);
            aVar3.a(!CommonUtils.isNetworkWifi(this.mContext) ? 0 : 2);
            aVar3.b(ConstantsUtils.DownloadType.NORMAL_MANUAL_DOWNLOAD.toString());
            arrayList.add(aVar3);
            z = true;
        }
        if (this.mVgcUpdateInfo == null || z4 || this.sDownloadInfoManager.d(iM)) {
            if (z4) {
                com.vivo.updaterbaseframe.a.a aVar4 = new com.vivo.updaterbaseframe.a.a();
                aVar4.a(this.mVgcUpdateInfo);
                aVar4.a("vgc_package");
                aVar4.a(true);
                aVar4.a(CommonUtils.isNetworkWifi(this.mContext) ? 2 : 0);
                aVar4.b(ConstantsUtils.DownloadType.NORMAL_MANUAL_DOWNLOAD.toString());
                arrayList2.add(aVar4);
            }
            z2 = z;
        } else {
            com.vivo.updaterbaseframe.a.a aVar5 = new com.vivo.updaterbaseframe.a.a();
            aVar5.a(this.mVgcUpdateInfo);
            aVar5.a("vgc_package");
            aVar5.a(true);
            aVar5.a(CommonUtils.isNetworkWifi(this.mContext) ? 2 : 0);
            aVar5.b(ConstantsUtils.DownloadType.NORMAL_MANUAL_DOWNLOAD.toString());
            arrayList.add(aVar5);
        }
        if (arrayList.size() > 0 || arrayList2.size() > 0) {
            if (z5) {
                com.bbk.updater.download.a.a(this.mContext).a(com.vivo.updaterassistant.a.a(this.mFotaUpdateInfo, arrayList, CommonUtils.isNetworkWifi(this.mContext)), com.vivo.updaterassistant.a.a(this.mFotaUpdateInfo, arrayList2, CommonUtils.isNetworkWifi(this.mContext)));
            } else {
                com.bbk.updater.download.a.a(this.mContext).a(arrayList, arrayList2);
            }
        }
        if (arrayList2.size() > 0) {
            if (z3) {
                this.sDownloadInfoManager.c("ota_pacakge");
            }
            if (z4) {
                this.sDownloadInfoManager.c("vgc_package");
            }
        }
        if (z2) {
            return;
        }
        if (z3 || z4) {
            refreshUI(DownloadControllerView.DownloadControllerStatus.PREPARING_DOWNLOAD, DownloadIndicatorView.DownloadIndicatorStatus.DOWNLOAD_PREPARING);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkAndSetSmartInstall() {
        Observable.just(0).observeOn(Schedulers.io()).map(new Func1<Integer, List<PrivacyTerms>>() { // from class: com.bbk.updater.ui.UpdatePresenter.8
            @Override // rx.functions.Func1
            public List<PrivacyTerms> call(Integer num) {
                return PrivacyTermsHelper.getInstance().getNeedShowPrivacyTerms(UpdatePresenter.this.mContext);
            }
        }).observeOn(AndroidSchedulers.mainThread()).filter(new Func1<List<PrivacyTerms>, Boolean>() { // from class: com.bbk.updater.ui.UpdatePresenter.7
            @Override // rx.functions.Func1
            public Boolean call(List<PrivacyTerms> list) {
                if (list != null && list.size() > 0) {
                    return true;
                }
                RxBus.a().c(new SmartInstallEvent(1));
                return false;
            }
        }).subscribe((Subscriber) new Subscriber<List<PrivacyTerms>>() { // from class: com.bbk.updater.ui.UpdatePresenter.6
            @Override // rx.Observer
            public void onCompleted() {
                unsubscribe();
            }

            @Override // rx.Observer
            public void onError(Throwable th) {
                unsubscribe();
            }

            @Override // rx.Observer
            public void onNext(List<PrivacyTerms> list) {
                StrategyFactory.getInstance(UpdatePresenter.this.mContext).onPrivacyTermsChecked(list, 3);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkAndInstall(final boolean z) {
        CommonUtils.cancelAnyDialog(this.mContext);
        if (CommonUtils.isDemoUpdate()) {
            CommonUtils.startInstall(this.mContext, BaseFrameUtils.InstallStrategy.INSTALL_NOW_ACTIVITY.toString());
        } else {
            Observable.just(0).observeOn(Schedulers.io()).map(new Func1<Integer, List<PrivacyTerms>>() { // from class: com.bbk.updater.ui.UpdatePresenter.11
                @Override // rx.functions.Func1
                public List<PrivacyTerms> call(Integer num) {
                    return PrivacyTermsHelper.getInstance().getNeedShowPrivacyTerms(UpdatePresenter.this.mContext);
                }
            }).observeOn(AndroidSchedulers.mainThread()).filter(new Func1<List<PrivacyTerms>, Boolean>() { // from class: com.bbk.updater.ui.UpdatePresenter.10
                @Override // rx.functions.Func1
                public Boolean call(List<PrivacyTerms> list) {
                    if (list == null || list.size() <= 0) {
                        CommonUtils.startInstall(UpdatePresenter.this.mContext, BaseFrameUtils.InstallStrategy.INSTALL_NOW_ACTIVITY.toString());
                        return false;
                    }
                    return true;
                }
            }).subscribe((Subscriber) new Subscriber<List<PrivacyTerms>>() { // from class: com.bbk.updater.ui.UpdatePresenter.9
                @Override // rx.Observer
                public void onCompleted() {
                    unsubscribe();
                }

                @Override // rx.Observer
                public void onError(Throwable th) {
                    unsubscribe();
                }

                @Override // rx.Observer
                public void onNext(List<PrivacyTerms> list) {
                    if (z) {
                        StrategyFactory.getInstance(UpdatePresenter.this.mContext).onPrivacyTermsChecked(list, 1);
                    } else {
                        StrategyFactory.getInstance(UpdatePresenter.this.mContext).onPrivacyTermsChecked(list, 2);
                    }
                }
            });
        }
    }

    private int checkShowInfoType(UpdateInfo updateInfo, VgcUpdateInfo vgcUpdateInfo) {
        if (!VersionUtils.iSVgc()) {
            return (!this.sDownloadInfoManager.n() || updateInfo == null) ? 0 : 1;
        }
        int iL = this.sDownloadInfoManager.l();
        int iM = this.sDownloadInfoManager.m();
        boolean z = updateInfo != null && this.sDownloadInfoManager.a(iL);
        boolean z2 = vgcUpdateInfo != null && this.sDownloadInfoManager.a(iM);
        boolean z3 = updateInfo != null && this.sDownloadInfoManager.p("ota_pacakge");
        boolean z4 = vgcUpdateInfo != null && this.sDownloadInfoManager.p("vgc_package");
        LogUtils.d(TAG, "checkShowInfoType > isFotaDownloadStart : " + z + ", isVgcDownloadStart : " + z2);
        LogUtils.d(TAG, "checkShowInfoType > isFotaDownloadHang : " + z3 + ", isVgcDownloadHang : " + z4);
        if (!z && !z2) {
            return 0;
        }
        if (z && z2) {
            return 1;
        }
        if (z) {
            return z3 ? 2 : 1;
        }
        if (z2) {
            return z4 ? 3 : 1;
        }
        return 0;
    }

    private boolean checkIfDownloadSucceed(UpdateInfo updateInfo, VgcUpdateInfo vgcUpdateInfo, int i, int i2) {
        if (updateInfo != null && vgcUpdateInfo != null) {
            return this.sDownloadInfoManager.d(i) && this.sDownloadInfoManager.d(i2);
        }
        if (updateInfo != null) {
            return this.sDownloadInfoManager.d(i);
        }
        if (vgcUpdateInfo != null) {
            return this.sDownloadInfoManager.d(i2);
        }
        return false;
    }

    public void setFromTrialVersion(boolean z) {
        this.mFromTrialVersion = z;
    }

    public boolean getFromTrialVersion() {
        return this.mFromTrialVersion;
    }

    public void setFromNotice(boolean z) {
        this.mFromNotice = z;
    }
}
