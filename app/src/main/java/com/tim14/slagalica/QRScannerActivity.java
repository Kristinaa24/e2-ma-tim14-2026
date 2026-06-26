package com.tim14.slagalica;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;
import android.view.TextureView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class QRScannerActivity extends AppCompatActivity {

    public static final String EXTRA_QR_TEXT = "qr_text";

    private static final int REQUEST_CAMERA = 44;
    private static final int PREVIEW_WIDTH = 1280;
    private static final int PREVIEW_HEIGHT = 720;

    private TextureView cameraPreview;
    private TextView scannerStatus;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private final MultiFormatReader qrReader = new MultiFormatReader();
    private boolean resultDelivered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        cameraPreview = findViewById(R.id.qrCameraPreview);
        scannerStatus = findViewById(R.id.qrScannerStatus);
        findViewById(R.id.btnCloseQrScanner).setOnClickListener(v -> finish());

        cameraPreview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                openCameraWhenAllowed();
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (cameraPreview.isAvailable()) {
            openCameraWhenAllowed();
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void openCameraWhenAllowed() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
            return;
        }

        openCamera();
    }

    private void openCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            String cameraId = findBackCameraId(cameraManager);
            if (cameraId == null) {
                Toast.makeText(this, R.string.qr_camera_unavailable, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            imageReader = ImageReader.newInstance(
                    PREVIEW_WIDTH,
                    PREVIEW_HEIGHT,
                    android.graphics.ImageFormat.YUV_420_888,
                    2
            );
            imageReader.setOnImageAvailableListener(this::decodeLatestImage, backgroundHandler);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    startPreview();
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
                    runOnUiThread(() -> Toast.makeText(
                            QRScannerActivity.this,
                            R.string.qr_camera_unavailable,
                            Toast.LENGTH_SHORT
                    ).show());
                }
            }, backgroundHandler);
        } catch (CameraAccessException exception) {
            Toast.makeText(this, R.string.qr_camera_unavailable, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private String findBackCameraId(CameraManager cameraManager) throws CameraAccessException {
        for (String cameraId : cameraManager.getCameraIdList()) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                return cameraId;
            }
        }

        String[] cameraIds = cameraManager.getCameraIdList();
        return cameraIds.length > 0 ? cameraIds[0] : null;
    }

    private void startPreview() {
        if (cameraDevice == null || !cameraPreview.isAvailable() || imageReader == null) {
            return;
        }

        SurfaceTexture texture = cameraPreview.getSurfaceTexture();
        if (texture == null) {
            return;
        }

        texture.setDefaultBufferSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        Surface previewSurface = new Surface(texture);
        Surface readerSurface = imageReader.getSurface();

        try {
            CaptureRequest.Builder requestBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            requestBuilder.addTarget(previewSurface);
            requestBuilder.addTarget(readerSurface);

            cameraDevice.createCaptureSession(
                    Arrays.asList(previewSurface, readerSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            captureSession = session;
                            try {
                                requestBuilder.set(
                                        CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                );
                                session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler);
                            } catch (CameraAccessException ignored) {
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            runOnUiThread(() -> scannerStatus.setText(R.string.qr_camera_unavailable));
                        }
                    },
                    backgroundHandler
            );
        } catch (CameraAccessException exception) {
            scannerStatus.setText(R.string.qr_camera_unavailable);
        }
    }

    private void decodeLatestImage(ImageReader reader) {
        if (resultDelivered) {
            return;
        }

        Image image = reader.acquireLatestImage();
        if (image == null) {
            return;
        }

        try {
            byte[] yPlane = copyYPlane(image);
            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                    yPlane,
                    image.getWidth(),
                    image.getHeight(),
                    0,
                    0,
                    image.getWidth(),
                    image.getHeight(),
                    false
            );
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = qrReader.decodeWithState(bitmap);
            deliverResult(result.getText());
        } catch (NotFoundException ignored) {
            qrReader.reset();
        } catch (Exception ignored) {
            qrReader.reset();
        } finally {
            image.close();
        }
    }

    private byte[] copyYPlane(Image image) {
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        int width = image.getWidth();
        int height = image.getHeight();
        int rowStride = plane.getRowStride();
        byte[] data = new byte[width * height];

        for (int row = 0; row < height; row++) {
            buffer.position(row * rowStride);
            buffer.get(data, row * width, width);
        }

        return data;
    }

    private void deliverResult(String text) {
        if (resultDelivered) {
            return;
        }

        resultDelivered = true;
        Intent result = new Intent();
        result.putExtra(EXTRA_QR_TEXT, text);
        setResult(RESULT_OK, result);
        finish();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("QrScannerCamera");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread == null) {
            return;
        }

        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        backgroundThread = null;
        backgroundHandler = null;
    }

    private void closeCamera() {
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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
            return;
        }

        Toast.makeText(this, R.string.qr_camera_permission_denied, Toast.LENGTH_SHORT).show();
        finish();
    }
}
