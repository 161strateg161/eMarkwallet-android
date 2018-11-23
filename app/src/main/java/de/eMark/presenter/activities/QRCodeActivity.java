package de.eMark.presenter.activities;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import de.eMark.R;
import de.eMark.databinding.ActivityQrCodeBinding;
import de.eMark.presenter.activities.callbacks.ActivityQRCodeCallback;
import de.eMark.presenter.activities.util.BRActivity;
import de.eMark.tools.animation.BRAnimator;
import de.eMark.tools.qrcode.QRUtils;

public class QRCodeActivity extends BRActivity {

    private static final String QR_IMAGE_URL = "QRCodeActivity:QrImageUrl";
    private ActivityQrCodeBinding binding;

    private ActivityQRCodeCallback callback = () -> supportFinishAfterTransition();

    public static void show(AppCompatActivity activity, View view, String qrUrl) {
        Intent intent = new Intent(activity, QRCodeActivity.class);
        intent.putExtra(QR_IMAGE_URL, qrUrl);
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(activity, view, "qr_image");
        activity.startActivity(intent, options.toBundle());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportPostponeEnterTransition();
        binding = DataBindingUtil.setContentView(this,
                R.layout.activity_qr_code);
        binding.setCallback(callback);
        populateQRImage();
        supportStartPostponedEnterTransition();
        new Handler().postDelayed(() -> {
            ObjectAnimator colorFade = BRAnimator.animateBackgroundDim(binding.background, false, null);
            colorFade.setStartDelay(350);
            colorFade.setDuration(500);
            colorFade.start();
        }, 250);
    }

    private void populateQRImage() {
        QRUtils.generateQR(this, getIntent().getStringExtra(QR_IMAGE_URL),  binding.qrImage);
    }
}
