package io.digibyte.presenter.activities;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.platform.tools.BRBitId;

import io.digibyte.R;
import io.digibyte.databinding.ActivityPinBinding;
import io.digibyte.presenter.activities.callbacks.LoginActivityCallback;
import io.digibyte.presenter.activities.models.PinActivityModel;
import io.digibyte.presenter.activities.util.BRActivity;
import io.digibyte.presenter.interfaces.BRAuthCompletion;
import io.digibyte.tools.animation.BRAnimator;
import io.digibyte.tools.animation.SpringAnimator;
import io.digibyte.tools.security.AuthManager;
import io.digibyte.tools.security.BitcoinUrlHandler;
import io.digibyte.wallet.BRWalletManager;

public class LoginActivity extends BRActivity {
    private static final String TAG = LoginActivity.class.getName();
    ActivityPinBinding binding;
    private StringBuilder pin = new StringBuilder();
    private boolean inputAllowed = true;

    private LoginActivityCallback callback = () -> showFingerprintDialog();

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_pin);
        binding.setData(new PinActivityModel());
        binding.setCallback(callback);
        binding.brkeyboard.addOnInsertListener(key -> handleClick(key));
        binding.brkeyboard.setShowDot(false);
        binding.brkeyboard.setDeleteImage(getDrawable(R.drawable.ic_delete_white));
        if (!processDeepLink(getIntent()) &&
                AuthManager.isFingerPrintAvailableAndSetup(this)) {
            showFingerprintDialog();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processDeepLink(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDots();

        inputAllowed = true;
        BRWalletManager.getInstance().smartInit(this, BRWalletManager.SmartInitType.LoginActivity);
    }

    private final boolean processDeepLink(@Nullable final Intent intent) {
        Uri data = intent.getData();
        if (data != null && BRBitId.isBitId(data.toString())) {
            BRBitId.signAndRespond(this, data.toString(), true);
            return true;
        } else if (data != null && BitcoinUrlHandler.isBitcoinUrl(data.toString())) {
            BRAnimator.showSendFragment(this, data.toString());
            return true;
        }
        return false;
    }

    private final void handleClick(String key) {
        if (!inputAllowed) {
            Log.e(TAG, "handleClick: input not allowed");
            return;
        }
        if (key == null) {
            Log.e(TAG, "handleClick: key is null! ");
            return;
        }

        if (key.isEmpty()) {
            handleDeleteClick();
        } else if (Character.isDigit(key.charAt(0))) {
            handleDigitClick(Integer.parseInt(key.substring(0, 1)));
        } else {
            Log.e(TAG, "handleClick: oops: " + key);
        }
    }

    private void showFingerprintDialog() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AuthManager.getInstance().authPrompt(LoginActivity.this, "", "",
                    new BRAuthCompletion() {
                        @Override
                        public void onComplete() {
                            unlockWallet();
                        }

                        @Override
                        public void onCancel() {

                        }
                    });
        }, 500);
    }


    private final void handleDigitClick(Integer dig) {
        if (pin.length() < 6) {
            pin.append(dig);
        }
        updateDots();
    }

    private final void handleDeleteClick() {
        if (pin.length() > 0) {
            pin.deleteCharAt(pin.length() - 1);
        }
        updateDots();
    }

    private final void unlockWallet() {
        BRAnimator.startBreadActivity(this, false);
    }

    private final void showFailedToUnlock() {
        SpringAnimator.failShakeAnimation(LoginActivity.this, binding.pinLayout);
        pin = new StringBuilder("");
        new Handler().postDelayed(() -> {
            inputAllowed = true;
            updateDots();
        }, 1000);
    }

    private final void updateDots() {
        AuthManager.getInstance().updateDots(pin.toString(), binding.dot1,
                binding.dot2, binding.dot3, binding.dot4,
                binding.dot5, binding.dot6, () -> {
                    inputAllowed = false;
                    if (AuthManager.getInstance().checkAuth(pin.toString(),
                            LoginActivity.this)) {
                        AuthManager.getInstance().authSuccess(LoginActivity.this);
                        unlockWallet();
                    } else {
                        AuthManager.getInstance().authFail(LoginActivity.this);
                        showFailedToUnlock();
                    }
                });
    }
}