package de.eMark;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.multidex.MultiDexApplication;
import android.support.v7.app.AppCompatDelegate;

import com.crashlytics.android.Crashlytics;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.facebook.soloader.SoLoader;
import com.google.zxing.client.android.PreferencesActivity;

import java.util.concurrent.TimeUnit;

import de.eMark.presenter.activities.DisabledActivity;
import de.eMark.presenter.activities.LoginActivity;
import de.eMark.tools.animation.BRAnimator;
import de.eMark.tools.manager.BRSharedPrefs;
import de.eMark.tools.security.BRKeyStore;
import de.eMark.tools.util.BRConstants;
import de.eMark.wallet.BRWalletManager;
import io.fabric.sdk.android.Fabric;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/22/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class eMark extends MultiDexApplication implements
        Application.ActivityLifecycleCallbacks {
    public static final String HOST = "deutsche-emark.org";
    public static final String FEE_URL = "https://api.deutsche-emark.org/fee-kb-v2.php";
    private static final long SYNC_PERIOD = TimeUnit.HOURS.toMillis(24);

    private static eMark application;

    public static eMark getContext() {
        return application;
    }

    public boolean isSuspended() {
        return activeActivity == null;
    }

    // TODO: Unfortunately there's some part of the app that use Activity context and need
    // TODO: to be reworked accordingly; see getContext usages. This will not leak activity context
    // TODO: as the reference is removed in the lifecycle callback onPause invokation
    // activities
    private Activity activeActivity;

    public Activity getActivity() {
        return activeActivity;
    }

    static {
        System.loadLibrary(BRConstants.NATIVE_LIB_NAME);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SoLoader.init(this, false);
        Fabric.with(this, new Crashlytics());
        application = this;
        activeActivity = null;
        registerActivityLifecycleCallbacks(this);
        JobManager.create(this).addJobCreator(new SyncBlockchainJobCreator());

        //This is for legacy users that have the boolean set to true, Vibrate permission has been
        // removed
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean(PreferencesActivity.KEY_VIBRATE, false)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(PreferencesActivity.KEY_VIBRATE, false);
            editor.commit();
        }
    }


    //////////////////////////////////////////////////////////////////////////////////
    //////////// Implementation of ActivityLifecycleCallbacks interface //////////////
    //////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onActivityCreated(Activity anActivity, Bundle aBundle) {
    }

    @Override
    public void onActivityStarted(Activity anActivity) {
    }

    /**
     * This called is used to ensure that no matter what activity is being started or resumed
     * the application will go back to the login screen if the timeout period has been exceeded
     */
    @Override
    public void onActivityResumed(Activity anActivity) {
        activeActivity = anActivity;
        if (!(activeActivity instanceof DisabledActivity)
                && !(activeActivity instanceof LoginActivity)) {
            // lock wallet if 3 minutes passed
            long suspendedTime = BRSharedPrefs.getSuspendTime(anActivity);
            if (suspendedTime != 0 && (System.currentTimeMillis() - suspendedTime >= 180 * 1000)) {
                if (!BRKeyStore.getPinCode(activeActivity).isEmpty()) {
                    BRAnimator.startBreadActivity(activeActivity, true);
                }
            }
        }
        BRSharedPrefs.putSuspendTime(anActivity, 0);
    }

    @Override
    public void onActivityStopped(Activity anActivity) {
    }

    @Override
    public void onActivityDestroyed(Activity anActivity) {
    }

    @Override
    public void onActivityPaused(Activity anActivity) {
        activeActivity = null;
        BRSharedPrefs.putSuspendTime(anActivity, System.currentTimeMillis());
    }

    @Override
    public void onActivitySaveInstanceState(Activity anActivity, Bundle aBundle) {
    }

    public class SyncBlockchainJobCreator implements JobCreator {

        @Override
        @Nullable
        public Job create(@NonNull String tag) {
            switch (tag) {
                case SyncBlockchainJob.TAG:
                    return new SyncBlockchainJob();
                default:
                    return null;
            }
        }
    }

    public static class SyncBlockchainJob extends Job {

        public static final String TAG = "sync_blockchain_job";

        @Override
        @NonNull
        protected Result onRunJob(Params params) {
            BRWalletManager.getInstance().init();
            return Result.SUCCESS;
        }

        public static void scheduleJob() {
            JobManager.instance().cancelAll();
            new JobRequest.Builder(SyncBlockchainJob.TAG)
                    .setPeriodic(SYNC_PERIOD).setRequiredNetworkType(
                    JobRequest.NetworkType.UNMETERED).setRequiresCharging(true)
                    .build()
                    .schedule();
        }
    }
}