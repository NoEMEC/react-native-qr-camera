package com.noemec.qrcamera;

import androidx.annotation.NonNull;

import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.common.MapBuilder;

import java.util.Map;

public class NoemecQRScannerViewManager extends SimpleViewManager<NoemecQRScannerView> {
  public static final String REACT_CLASS = "NoemecQRScannerView";

  @NonNull
  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @NonNull
  @Override
  protected NoemecQRScannerView createViewInstance(@NonNull ThemedReactContext reactContext) {
    return new NoemecQRScannerView(reactContext);
  }

  @ReactProp(name = "active", defaultBoolean = true)
  public void setActive(NoemecQRScannerView view, boolean active) {
    view.setActive(active);
  }

  @ReactProp(name = "torchOn", defaultBoolean = false)
  public void setTorchOn(NoemecQRScannerView view, boolean torchOn) {
    view.setTorchOn(torchOn);
  }

  @Override
  public Map getExportedCustomDirectEventTypeConstants() {
    return MapBuilder.builder()
      .put("onCodeScanned", MapBuilder.of("registrationName", "onCodeScanned"))
      .build();
  }
}
