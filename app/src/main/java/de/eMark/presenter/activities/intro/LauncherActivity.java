package de.eMark.presenter.activities.intro;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import de.eMark.R;
import de.eMark.tools.animation.BRAnimator;
import de.eMark.tools.security.PostAuth;
import de.eMark.wallet.BRWalletManager;

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        PostAuth.instance.onCanaryCheck(this, false);
        BRWalletManager wallet = BRWalletManager.getInstance();
        if (wallet.noWallet(this)) {
            IntroActivity.open(this);
        } else {
            BRAnimator.startBreadActivity(this, true);
        }
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}
