package com.mooc.ppjoke.ui.detail;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.lifecycle.Observer;

import com.mooc.libcommon.dialog.LoadingDialog;
import com.mooc.libcommon.global.AppGlobals;
import com.mooc.libcommon.utils.FileUploadManager;
import com.mooc.libcommon.utils.FileUtils;
import com.mooc.libcommon.utils.PixUtils;
import com.mooc.libcommon.view.PPEditTextView;
import com.mooc.libcommon.view.ViewHelper;
import com.mooc.libnetwork.ApiResponse;
import com.mooc.libnetwork.ApiService;
import com.mooc.libnetwork.JsonCallback;
import com.mooc.ppjoke.R;
import com.mooc.ppjoke.databinding.LayoutCommentDialogBinding;
import com.mooc.ppjoke.model.Comment;
import com.mooc.ppjoke.ui.login.UserManager;
import com.mooc.ppjoke.ui.publish.CaptureActivity;

import java.util.concurrent.atomic.AtomicInteger;

/***
 * 评论的对话框
 */
public class CommentDialog extends AppCompatDialogFragment implements View.OnClickListener {
    //处理CommentDialog的布局控件的databinding对象
    private LayoutCommentDialogBinding mBinding;
    //评论帖子的唯一标识
    private long itemId;
    //添加评论的监听
    private commentAddListener mListener;
    //获取跳转到当前的对话框的item的key
    private static final String KEY_ITEM_ID = "key_item_id";
    //存储的文件路径
    private String filePath;
    //文件的宽和高
    private int width, height;
    //是否是视频
    private boolean isVideo;
    //封面的url
    private String coverUrl;
    //文件的url
    private String fileUrl;
    //加载对话框
    private LoadingDialog loadingDialog;

    /***
     * 创建一个对话框
     * @param itemId
     * @return
     */
    public static CommentDialog newInstance(long itemId) {
        Bundle args = new Bundle();
        args.putLong(KEY_ITEM_ID, itemId);
        CommentDialog fragment = new CommentDialog();
        fragment.setArguments(args);
        return fragment;
    }

    /***
     * 页面的控件的初始化和相关的监听的添加
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //设置对话框的宽度匹配屏幕的宽度
        Window window = getDialog().getWindow();
        window.setWindowAnimations(0);
        mBinding = LayoutCommentDialogBinding.inflate(inflater, ((ViewGroup) window.findViewById(android.R.id.content)), false);
        mBinding.commentVideo.setOnClickListener(this);
        mBinding.commentDelete.setOnClickListener(this);
        mBinding.commentSend.setOnClickListener(this);

        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);

        this.itemId = getArguments().getLong(KEY_ITEM_ID);

        ViewHelper.setViewOutline(mBinding.getRoot(), PixUtils.dp2px(10), ViewHelper.RADIUS_TOP);

        mBinding.getRoot().post(() -> showSoftInputMethod());

        dismissWhenPressBack();
        return mBinding.getRoot();
    }

    /****
     * 在对话框中监听返回按钮的操作
     */
    private void dismissWhenPressBack() {
        mBinding.inputView.setOnBackKeyEventListener(() -> {
            mBinding.inputView.postDelayed(() -> dismiss(), 200);
            return true;
        });
    }

    /***
     * 显示软键盘
     */
    private void showSoftInputMethod() {
        mBinding.inputView.setFocusable(true);
        mBinding.inputView.setFocusableInTouchMode(true);
        //请求获得焦点
        mBinding.inputView.requestFocus();
        InputMethodManager manager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        manager.showSoftInput(mBinding.inputView, 0);
    }

    /***
     * 对话框中的item的点击事件
     * @param v
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.comment_send) {
            //发布帖子
            publishComment();
        } else if (v.getId() == R.id.comment_video) {
            //录制
            CaptureActivity.startActivityForResult(getActivity());
            //点击叉的处理
        } else if (v.getId() == R.id.comment_delete) {
            filePath = null;
            isVideo = false;
            width = 0;
            height = 0;
            mBinding.commentCover.setImageDrawable(null);
            mBinding.commentExtLayout.setVisibility(View.GONE);
            mBinding.commentVideo.setEnabled(true);
            mBinding.commentVideo.setAlpha(255);
        }

    }

    /***
     * 录制界面返回的数据的处理
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CaptureActivity.REQ_CAPTURE && resultCode == Activity.RESULT_OK) {
            filePath = data.getStringExtra(CaptureActivity.RESULT_FILE_PATH);
            width = data.getIntExtra(CaptureActivity.RESULT_FILE_WIDTH, 0);
            height = data.getIntExtra(CaptureActivity.RESULT_FILE_HEIGHT, 0);
            isVideo = data.getBooleanExtra(CaptureActivity.RESULT_FILE_TYPE, false);

            if (!TextUtils.isEmpty(filePath)) {
                mBinding.commentExtLayout.setVisibility(View.VISIBLE);
                mBinding.commentCover.setImageUrl(filePath);
                if (isVideo) {
                    mBinding.commentIconVideo.setVisibility(View.VISIBLE);
                }
            }

            mBinding.commentVideo.setEnabled(false);
            mBinding.commentVideo.setAlpha(80);
        }
    }

    /***
     * 发布评论
     */
    private void publishComment() {
         //评论文本信息
        if (TextUtils.isEmpty(mBinding.inputView.getText())) {
            return;
        }

        if (isVideo && !TextUtils.isEmpty(filePath)) {
            FileUtils.generateVideoCover(filePath).observe(this, coverPath -> uploadFile(coverPath, filePath));
        } else if (!TextUtils.isEmpty(filePath)) {
            uploadFile(null, filePath);
        } else {
            publish();
        }
    }

    /***
     * 上传视频
     * @param coverPath  视频的封面
     * @param filePath  视频的文件路径
     */
    @SuppressLint("RestrictedApi")
    private void uploadFile(String coverPath, String filePath) {
        //AtomicInteger, CountDownLatch, CyclicBarrier
        showLoadingDialog();
        AtomicInteger count = new AtomicInteger(1);
        //封面文件上传
        if (!TextUtils.isEmpty(coverPath)) {
            count.set(2);

            // ArchTaskExecutor  jetpack的线程池组件
            ArchTaskExecutor.getIOThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    int remain = count.decrementAndGet();
                    coverUrl = FileUploadManager.upload(coverPath);
                    if (remain <= 0) {
                        if (!TextUtils.isEmpty(fileUrl) && !TextUtils.isEmpty(coverUrl)) {
                            publish();
                        } else {
                            dismissLoadingDialog();
                            showToast(getString(R.string.file_upload_failed));
                        }
                    }
                }
            });
        }
        //具体的文件的上传
        ArchTaskExecutor.getIOThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                int remain = count.decrementAndGet();
                fileUrl = FileUploadManager.upload(filePath);
                if (remain <= 0) {
                    if (!TextUtils.isEmpty(fileUrl) || !TextUtils.isEmpty(coverPath) && !TextUtils.isEmpty(coverUrl)) {
                        publish();
                    } else {
                        dismissLoadingDialog();
                        showToast(getString(R.string.file_upload_failed));
                    }
                }
            }
        });

    }

    /***
     * 添加评论的请求发起
     */
    private void publish() {
        String commentText = mBinding.inputView.getText().toString();
        ApiService.post("/comment/addComment")
                .addParam("userId", UserManager.get().getUserId())
                .addParam("itemId", itemId)
                .addParam("commentText", commentText)
                .addParam("image_url", isVideo ? coverUrl : fileUrl)
                .addParam("video_url", isVideo ? fileUrl : null)
                .addParam("width", width)
                .addParam("height", height)
                .execute(new JsonCallback<Comment>() {
                    @Override
                    public void onSuccess(ApiResponse<Comment> response) {
                        onCommentSuccess(response.body);
                        dismissLoadingDialog();
                    }

                    @Override
                    public void onError(ApiResponse<Comment> response) {
                        showToast("评论失败:" + response.message);
                        dismissLoadingDialog();
                    }
                });
    }

    /***
     * 显示加载的对话框
     */
    private void showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = new LoadingDialog(getContext());
            loadingDialog.setLoadingText(getString(R.string.upload_text));
            loadingDialog.setCanceledOnTouchOutside(false);
            loadingDialog.setCancelable(false);
        }
        if (!loadingDialog.isShowing()) {
            loadingDialog.show();
        }
    }

    /***
     * 隐藏加载对话框
     */
    @SuppressLint("RestrictedApi")
    private void dismissLoadingDialog() {
        if (loadingDialog != null) {
            //dismissLoadingDialog  的调用可能会出现在异步线程调用
            if (Looper.myLooper() == Looper.getMainLooper()) {
                ArchTaskExecutor.getMainThreadExecutor().execute(() -> {
                    if (loadingDialog != null && loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                });
            } else if (loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
        }
    }

    /***
     * 评论成功的处理
     * @param body
     */
    @SuppressLint("RestrictedApi")
    private void onCommentSuccess(Comment body) {
        showToast("评论发布成功");
        ArchTaskExecutor.getMainThreadExecutor().execute(() -> {
            if (mListener != null) {
                mListener.onAddComment(body);
            }
            dismiss();
        });
    }

    @Override
    public void dismiss() {
        super.dismiss();
        dismissLoadingDialog();
        filePath = null;
        fileUrl = null;
        coverUrl = null;
        isVideo = false;
        width = 0;
        height = 0;
    }

    @SuppressLint("RestrictedApi")
    private void showToast(String s) {
        //showToast几个可能会出现在异步线程调用
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(AppGlobals.getApplication(), s, Toast.LENGTH_SHORT).show();
        } else {
            ArchTaskExecutor.getMainThreadExecutor().execute(() -> Toast.makeText(AppGlobals.getApplication(), s, Toast.LENGTH_SHORT).show());
        }
    }

    /***
     * 评论添加监听
     */
    public interface commentAddListener {
        void onAddComment(Comment comment);
    }

    /***
     * 设置评论监听
     * @param listener
     */
    public void setCommentAddListener(commentAddListener listener) {

        mListener = listener;
    }
}
