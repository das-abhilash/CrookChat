package com.crookchat.materialcamera;

import android.app.Fragment;
import android.support.annotation.NonNull;

import com.crookchat.materialcamera.internal.BaseCaptureActivity;
import com.crookchat.materialcamera.internal.CameraFragment;

public class CaptureActivity extends BaseCaptureActivity {

  @Override
  @NonNull
  public Fragment getFragment() {
    return CameraFragment.newInstance();
  }
}
