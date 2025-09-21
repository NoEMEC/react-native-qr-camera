package com.noemec.qrcamera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

public class NoemecQRScannerView extends FrameLayout {
  private static final String TAG = "NoemecQRScanner";

  private TextureView textureView;
  private CameraDevice cameraDevice;
  private CameraCaptureSession captureSession;
  private CaptureRequest.Builder previewRequestBuilder;
  private Handler backgroundHandler;
  private HandlerThread backgroundThread;

  private boolean active = true;
  private boolean torchOn = false;

  private String currentCameraId;
  private Size previewSize;
  private ImageReader imageReader;

  private long lastDecodeMs = 0L;
  private static final long DECODE_THROTTLE_MS = 250L;

  public NoemecQRScannerView(Context context) {
    super(context);
    init(context);
  }

  public NoemecQRScannerView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  private void init(Context context) {
    textureView = new TextureView(context);
    addView(textureView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    textureView.setSurfaceTextureListener(surfaceTextureListener);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (active) {
  if (textureView.isAvailable()) startCamera();
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    stopCamera();
  }

  public void setActive(boolean active) {
    this.active = active;
    if (active) startCamera(); else stopCamera();
  }

  public void setTorchOn(boolean torchOn) {
    this.torchOn = torchOn;
    updateTorch();
  }

  private void startBackgroundThread() {
    backgroundThread = new HandlerThread("NoemecQRBackground");
    backgroundThread.start();
    backgroundHandler = new Handler(backgroundThread.getLooper());
  }

  private void stopBackgroundThread() {
    if (backgroundThread != null) {
      backgroundThread.quitSafely();
      try {
        backgroundThread.join();
      } catch (InterruptedException e) {
        // ignore
      }
      backgroundThread = null;
      backgroundHandler = null;
    }
  }

  private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
      if (!active) return;
      if (cameraDevice == null) {
        startCamera();
      } else if (captureSession == null) {
        createPreviewSession();
      }
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {}

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) { return true; }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) { /* no-op */ }
  };

  private void startCamera() {
    if (!active) return;
  if (!textureView.isAvailable()) return;

    if (cameraDevice != null) {
      if (captureSession == null) createPreviewSession();
      return;
    }

    Context context = getContext();
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      Log.w(TAG, "Camera permission not granted");
      return;
    }

    startBackgroundThread();
    CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    try {
      currentCameraId = getBackCameraId(manager);
      computePreviewSize(manager, currentCameraId);
      manager.openCamera(currentCameraId, stateCallback, backgroundHandler);
    } catch (Exception e) {
      Log.e(TAG, "openCamera error", e);
    }
  }

  private String getBackCameraId(CameraManager manager) throws CameraAccessException {
    for (String id : manager.getCameraIdList()) {
      CameraCharacteristics ch = manager.getCameraCharacteristics(id);
      Integer facing = ch.get(CameraCharacteristics.LENS_FACING);
      if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) return id;
    }
    return manager.getCameraIdList()[0];
  }

  private void stopCamera() {
    try {
      if (captureSession != null) {
        captureSession.close();
        captureSession = null;
      }
      if (cameraDevice != null) {
        cameraDevice.close();
        cameraDevice = null;
      }
      if (imageReader != null) {
        imageReader.close();
        imageReader = null;
      }
    } finally {
      stopBackgroundThread();
    }
  }

  private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
    @Override
    public void onOpened(@NonNull CameraDevice camera) {
      cameraDevice = camera;
      createPreviewSession();
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice camera) {
      camera.close();
      cameraDevice = null;
    }

    @Override
    public void onError(@NonNull CameraDevice camera, int error) {
      camera.close();
      cameraDevice = null;
    }
  };

  private void createPreviewSession() {
    try {
      SurfaceTexture texture = textureView.getSurfaceTexture();
      if (texture == null || cameraDevice == null) return;

      int width;
      int height;
      if (previewSize != null) {
        width = previewSize.getWidth();
        height = previewSize.getHeight();
      } else {
        width = textureView.getWidth() > 0 ? textureView.getWidth() : 1280;
        height = textureView.getHeight() > 0 ? textureView.getHeight() : 720;
      }
      texture.setDefaultBufferSize(width, height);
      Surface previewSurface = new Surface(texture);

      if (imageReader == null) {
        imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2);
        imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler);
      }
      Surface imageSurface = imageReader.getSurface();

      previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
      previewRequestBuilder.addTarget(previewSurface);
      previewRequestBuilder.addTarget(imageSurface);
      previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
      previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);

      List<Surface> surfaces = new ArrayList<>();
      surfaces.add(previewSurface);
      surfaces.add(imageSurface);

      cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
          if (cameraDevice == null) return;
          captureSession = session;
          try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            updateTorch();
            CaptureRequest previewRequest = previewRequestBuilder.build();
            captureSession.setRepeatingRequest(previewRequest, null, backgroundHandler);
          } catch (CameraAccessException e) {
            Log.e(TAG, "start preview error", e);
          }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
          Log.e(TAG, "configureFailed");
        }
      }, backgroundHandler);
    } catch (Exception e) {
      Log.e(TAG, "createPreviewSession", e);
    }
  }

  private void computePreviewSize(CameraManager manager, String cameraId) throws CameraAccessException {
    CameraCharacteristics ch = manager.getCameraCharacteristics(cameraId);
    StreamConfigurationMap map = ch.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
    if (map == null) return;
    Size[] sizes = map.getOutputSizes(SurfaceTexture.class);
    if (sizes == null || sizes.length == 0) return;
    int vw = Math.max(textureView.getWidth(), 1);
    int vh = Math.max(textureView.getHeight(), 1);
    previewSize = chooseOptimalSize(sizes, vw, vh);
  }

  private Size chooseOptimalSize(Size[] choices, int width, int height) {
    Size best = choices[0];
    for (Size s : choices) {
      boolean bigEnough = s.getWidth() >= width && s.getHeight() >= height;
      boolean smallerThanBest = s.getWidth() * s.getHeight() < best.getWidth() * best.getHeight();
      if (bigEnough && smallerThanBest) best = s;
    }
    return best;
  }

  private void updateTorch() {
    if (previewRequestBuilder == null || captureSession == null) return;
    try {
      previewRequestBuilder.set(CaptureRequest.FLASH_MODE, torchOn ? CaptureRequest.FLASH_MODE_TORCH : CaptureRequest.FLASH_MODE_OFF);
      captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);
    } catch (CameraAccessException e) {
      Log.e(TAG, "updateTorch", e);
    }
  }

  private final ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
    @Override
    public void onImageAvailable(ImageReader reader) {
      long now = System.currentTimeMillis();
      if (now - lastDecodeMs < DECODE_THROTTLE_MS) {
        Image skip = reader.acquireLatestImage();
        if (skip != null) skip.close();
        return;
      }
      Image image = reader.acquireLatestImage();
      if (image == null) return;
      try {
        String decoded = decodeImage(image);
        if (decoded != null && !decoded.isEmpty()) {
          lastDecodeMs = now;
          emitCode(decoded);
        }
      } catch (Exception e) {
        // ignore decode errors
      } finally {
        image.close();
      }
    }
  };

  private String decodeImage(Image image) {
    if (image.getFormat() != ImageFormat.YUV_420_888) return null;
    Image.Plane[] planes = image.getPlanes();
    if (planes == null || planes.length < 3) return null;
    int width = image.getWidth();
    int height = image.getHeight();
    byte[] nv21 = yuv420ToNV21(planes, width, height);
    if (nv21 == null) return null;
    try {
      LuminanceSource source = new com.google.zxing.PlanarYUVLuminanceSource(
        nv21, width, height, 0, 0, width, height, false
      );
      BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
      Hashtable<DecodeHintType, Object> hints = new Hashtable<>();
      hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
      hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));
      Result result = new QRCodeReader().decode(bitmap, hints);
      return result.getText();
    } catch (Exception e) {
      return null;
    }
  }

  private static byte[] yuv420ToNV21(Image.Plane[] planes, int width, int height) {
    byte[] out = new byte[width * height * 3 / 2];
    int offset = 0;

    ByteBuffer yBuf = planes[0].getBuffer();
    int yRowStride = planes[0].getRowStride();
    int yPixelStride = planes[0].getPixelStride();
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        out[offset++] = yBuf.get(y * yRowStride + x * yPixelStride);
      }
    }

    ByteBuffer uBuf = planes[1].getBuffer();
    ByteBuffer vBuf = planes[2].getBuffer();
    int uvRowStride = planes[1].getRowStride();
    int uvPixelStride = planes[1].getPixelStride();
    for (int y = 0; y < height / 2; y++) {
      for (int x = 0; x < width / 2; x++) {
        int u = uBuf.get(y * uvRowStride + x * uvPixelStride) & 0xFF;
        int v = vBuf.get(y * uvRowStride + x * uvPixelStride) & 0xFF;
        out[offset++] = (byte) v;
        out[offset++] = (byte) u;
      }
    }

    return out;
  }

  private void emitCode(String value) {
    ReactContext reactContext = (ReactContext) getContext();
    com.facebook.react.bridge.WritableMap map = Arguments.createMap();
    map.putString("value", value);
    reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), "onCodeScanned", map);
  }
}
