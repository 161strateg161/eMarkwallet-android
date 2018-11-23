package de.eMark.presenter.activities.intro;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import de.eMark.R;
import de.eMark.databinding.ActivityWriteDownBinding;
import de.eMark.presenter.activities.callbacks.ActivityWriteDownCallback;
import de.eMark.presenter.activities.util.BRActivity;
import de.eMark.tools.animation.BRAnimator;
import de.eMark.tools.security.AuthManager;
import de.eMark.tools.security.PostAuth;

public class WriteDownActivity extends BRActivity {

    public static void open(AppCompatActivity activity) {
        Intent intent = new Intent(activity, WriteDownActivity.class);
        activity.startActivity(intent);
    }

    private ActivityWriteDownCallback callback = () -> AuthManager.getInstance().authPrompt(
            WriteDownActivity.this, null,
            getString(R.string.VerifyPin_continueBody), new AuthType(AuthType.Type.POST_AUTH));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityWriteDownBinding binding = DataBindingUtil.setContentView(this,
                R.layout.activity_write_down);
        binding.setCallback(callback);
        setupToolbar();
        setToolbarTitle(R.string.SecurityCenter_paperKeyTitle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.home:
            case android.R.id.home:
                BRAnimator.startBreadActivity(WriteDownActivity.this, false);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        BRAnimator.startBreadActivity(WriteDownActivity.this, false);
    }

    @Override
    public void onComplete(AuthType authType) {
        switch(authType.type) {
            case LOGIN:
                break;
            case DIGI_ID:
                break;
            case POST_AUTH:
                PostAuth.instance.onPhraseCheckAuth(WriteDownActivity.this,false);
                break;
            default:
                super.onComplete(authType);
        }
    }

    @Override
    public void onCancel(AuthType authType) {

    }
}