package com.mooc.ppjoke.ui.publish;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.UseCase;
import androidx.camera.core.VideoCapture;
import androidx.camera.core.VideoCaptureConfig;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;

import com.mooc.ppjoke.R;
import com.mooc.ppjoke.databinding.ActivityLayoutCaptureBinding;
import com.mooc.ppjoke.view.RecordView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/***
 * 视频录制的界面
 */
public class CaptureActivity extends AppCompatActivity {
    public static final int REQ_CAPTURE = 10001;
    private ActivityLayoutCaptureBinding mBinding;
    private static final String[] PERMISSIONS = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};
    private static final int PERMISSION_CODE = 1000;
    private ArrayList<String> deniedPermission = new ArrayList<>();
    private CameraX.LensFacing mLensFacing = CameraX.LensFacing.BACK;
    private int rotation = Surface.ROTATION_0;
    private Size resolution = new Size(1280, 720);
    private Rational rational = new Rational(9, 16);
    private Preview preview;
    private ImageCapture imageCapture;
    private VideoCapture videoCapture;
    private TextureView textureView;
    private boolean takingPicture;
    private String outputFilePath;
    public static final String RESULT_FILE_PATH = "file_path";
    public static final String RESULT_FILE_WIDTH = "file_width";
    public static final String RESULT_FILE_HEIGHT = "file_height";
    public static final String RESULT_FILE_TYPE = "file_type";

    /***
     * 启动当前界面的方法
     * @param activity
     */
    public static void startActivityForResult(Activity activity) {
        Intent intent = new Intent(activity, CaptureActivity.class);
        activity.startActivityForResult(intent, REQ_CAPTURE);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_layout_capture);
        //权限申请
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_CODE);
        //开始录制
        mBinding.recordView.setOnRecordListener(new RecordView.onRecordListener() {
            //拍照
            @Override
            public void onClick() {
                takingPicture = true;
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), System.currentTimeMillis() + ".jpeg");
                mBinding.captureTips.setVisibility(View.INVISIBLE);
                imageCapture.takePicture(file, new ImageCapture.OnImageSavedListener() {
                    @Override
                    public void onImageSaved(@NonNull File file) {
                        onFileSaved(file);
                    }

                    @Override
                    public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {
                        showErrorToast(message);
                    }
                });
            }
            //录制视频
            @SuppressLint("RestrictedApi")
            @Override
            public void onLongClick() {
                takingPicture = false;
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), System.currentTimeMillis() + ".mp4");
                videoCapture.startRecording(file, new VideoCapture.OnVideoSavedListener() {
                    @Override
                    public void onVideoSaved(File file) {
                        onFileSaved(file);
                    }

                    @Override
                    public void onError(VideoCapture.UseCaseError useCaseError, String message, @Nullable Throwable cause) {
                        showErrorToast(message);
                    }
                });
            }

            @SuppressLint("RestrictedApi")
            @Override
            public void onFinish() {
                videoCapture.stopRecording();
            }
        });
    }

    /***
     * 展示error的toast
     * @param message
     */
    private void showErrorToast(@NonNull String message) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(CaptureActivity.this, message, Toast.LENGTH_SHORT).show();
        } else {
            runOnUiThread(() -> Toast.makeText(CaptureActivity.this, message, Toast.LENGTH_SHORT).show());
        }
    }

    /***
     * 启动视频预览的activity的返回结果处理
     * @param requestCode
     * @param resultCode 标识是否成功
     * @param data 返回的数据
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PreviewActivity.REQ_PREVIEW && resultCode == RESULT_OK) {
            Intent intent = new Intent();
            intent.putExtra(RESULT_FILE_PATH, outputFilePath);
            //当设备处于竖屏情况时，宽高的值 需要互换，横屏不需要
            intent.putExtra(RESULT_FILE_WIDTH, resolution.getHeight());
            intent.putExtra(RESULT_FILE_HEIGHT, resolution.getWidth());
            intent.putExtra(RESULT_FILE_TYPE, !takingPicture);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    /***
     * 保存文件
     * @param file
     */
    private void onFileSaved(File file) {
        outputFilePath = file.getAbsolutePath();
        String mimeType = takingPicture ? "image/jpeg" : "video/mp4";
        MediaScannerConnection.scanFile(this, new String[]{outputFilePath}, new String[]{mimeType}, null);
        PreviewActivity.startActivityForResult(this, outputFilePath, !takingPicture, "完成");
    }

    /***
     * 权限申请的回调方法
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            deniedPermission.clear();
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int result = grantResults[i];
                if (result != PackageManager.PERMISSION_GRANTED) {
                    deniedPermission.add(permission);
                }
            }

            if (deniedPermission.isEmpty()) {
                bindCameraX();
            } else {
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.capture_permission_message))
                        .setNegativeButton(getString(R.string.capture_permission_no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                CaptureActivity.this.finish();
                            }
                        })
                        .setPositiveButton(getString(R.string.capture_permission_ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String[] denied = new String[deniedPermission.size()];
                                ActivityCompat.requestPermissions(CaptureActivity.this, deniedPermission.toArray(denied), PERMISSION_CODE);
                            }
                        }).create().show();
            }
        }
    }

    /***
     *
     */
    @SuppressLint("RestrictedApi")
    private void bindCameraX() {
        CameraX.unbindAll();

        //查询一下当前要使用的设备摄像头(比如后置摄像头)是否存在
        boolean hasAvailableCameraId = false;
        try {
            //是否有后置摄像头
            hasAvailableCameraId = CameraX.hasCameraWithLensFacing(mLensFacing);
        } catch (CameraInfoUnavailableException e) {
            e.printStackTrace();
        }
        //无指定的摄像头的错误提示
        if (!hasAvailableCameraId) {
            showErrorToast("无可用的设备cameraId!,请检查设备的相机是否被占用");
            finish();
            return;
        }

        //查询一下是否存在可用的cameraId.形式如：后置："0"，前置："1"
        String cameraIdForLensFacing = null;
        try {
            cameraIdForLensFacing = CameraX.getCameraFactory().cameraIdForLensFacing(mLensFacing);
        } catch (CameraInfoUnavailableException e) {
            e.printStackTrace();
        }
        if (TextUtils.isEmpty(cameraIdForLensFacing)) {
            showErrorToast("无可用的设备cameraId!,请检查设备的相机是否被占用");
            finish();
            return;
        }
        // 创建预览的时候使用的CameraX使用的对象的配置信息
        PreviewConfig config = new PreviewConfig.Builder()
                //前后摄像头
                .setLensFacing(mLensFacing)
                //旋转角度
                .setTargetRotation(rotation)
                //分辨率
                .setTargetResolution(resolution)
                //宽高比
                .setTargetAspectRatio(rational)
                .build();
        //创建预览使用的对象
        preview = new Preview(config);
        //创建拍照时候使用的对象
        imageCapture = new ImageCapture(new ImageCaptureConfig.Builder()
                .setTargetAspectRatio(rational)
                .setTargetResolution(resolution)
                .setLensFacing(mLensFacing)
                .setTargetRotation(rotation).build());

        videoCapture = new VideoCapture(new VideoCaptureConfig.Builder()
                //设置旋转角度
                .setTargetRotation(rotation)
                //设置使用的摄像头
                .setLensFacing(mLensFacing)
                //设置分辨率
                .setTargetResolution(resolution)
                //设置宽高比
                .setTargetAspectRatio(rational)
                //视频帧率
                .setVideoFrameRate(25)
                //bit率
                .setBitRate(3 * 1024 * 1024).build());
        //设置视频流的预览的回调
        preview.setOnPreviewOutputUpdateListener(output -> {
            //预览的控件的初始化
            textureView = mBinding.textureView;
            //获取预览控件的父控件
            ViewGroup parent = (ViewGroup) textureView.getParent();
            parent.removeView(textureView);
            parent.addView(textureView, 0);
            //预览
            textureView.setSurfaceTexture(output.getSurfaceTexture());
        });

        //上面配置的都是我们期望的分辨率
        List<UseCase> newUseList = new ArrayList<>();
        newUseList.add(preview);
        newUseList.add(imageCapture);
        newUseList.add(videoCapture);
        //下面我们要查询一下 当前设备它所支持的分辨率有哪些，然后再更新一下 所配置的几个usecase
        Map<UseCase, Size> resolutions = CameraX.getSurfaceManager().getSuggestedResolutions(cameraIdForLensFacing, null, newUseList);
        Iterator<Map.Entry<UseCase, Size>> iterator = resolutions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UseCase, Size> next = iterator.next();
            UseCase useCase = next.getKey();
            Size value = next.getValue();
            Map<String, Size> update = new HashMap<>();
            update.put(cameraIdForLensFacing, value);
            useCase.updateSuggestedResolution(update);
        }
        CameraX.bindToLifecycle(this, preview, imageCapture, videoCapture);
    }

    @Override
    protected void onDestroy() {
        CameraX.unbindAll();
        super.onDestroy();
    }
}

