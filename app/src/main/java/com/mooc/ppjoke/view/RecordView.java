package com.mooc.ppjoke.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.Nullable;

import com.mooc.libcommon.utils.PixUtils;
import com.mooc.ppjoke.R;

/****
 * 视频录制的自定义view
 */
public class RecordView extends View implements View.OnLongClickListener, View.OnClickListener {

    private static final int PROGRESS_INTERVAL = 100;
    //绘制白色小圆圈的画笔
    private final Paint fillPaint;
    //绘制外部的录制进度的画笔
    private final Paint progressPaint;
    //进度的最大值
    private int progressMaxValue;
    //绘制小圆点的半径
    private final int radius;
    //外侧进度圆环的宽度
    private final int progressWidth;
    //进度圆环的颜色
    private final int progressColor;
    //填充色
    private final int fillColor;
    //绘制的最大持续时间
    private final int maxDuration;
    private int progressValue;
    //是否在录制的boolean
    private boolean isRecording;
    //录制的开始时间
    private long startRecordTime;
    //录制的回调监听
    private onRecordListener mListener;

    public RecordView(Context context) {
        this(context, null);
    }

    public RecordView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecordView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RecordView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        //获取自定义属性
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RecordView, defStyleAttr, defStyleRes);
        radius = typedArray.getDimensionPixelOffset(R.styleable.RecordView_radius, 0);
        progressWidth = typedArray.getDimensionPixelOffset(R.styleable.RecordView_progress_width, PixUtils.dp2px(3));
        progressColor = typedArray.getColor(R.styleable.RecordView_progress_color, Color.RED);
        fillColor = typedArray.getColor(R.styleable.RecordView_fill_color, Color.WHITE);
        maxDuration = typedArray.getInteger(R.styleable.RecordView_duration, 10);
        setMaxDuration(maxDuration);
        typedArray.recycle();

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(fillColor);
        fillPaint.setStyle(Paint.Style.FILL);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(progressColor);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(progressWidth);

        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                progressValue++;
                //在非UI线程进行视图的刷新
                postInvalidate();
                if (progressValue <= progressMaxValue) {
                    sendEmptyMessageDelayed(0, PROGRESS_INTERVAL);
                } else {
                    finishRecord();
                }
            }
        };
        // 手指按下开始录制   手指松开停止录制
        setOnTouchListener((v, event) -> {
            //按下的处理
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isRecording = true;
                startRecordTime = System.currentTimeMillis();
                handler.sendEmptyMessage(0);
            //抬起的处理
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                long now = System.currentTimeMillis();
                //  getLongPressTimeout按下的持续时间
                if (now - startRecordTime > ViewConfiguration.getLongPressTimeout()) {
                    finishRecord();
                }
                //停止发送消息
                handler.removeCallbacksAndMessages(null);
                isRecording = false;
                startRecordTime = 0;
                progressValue = 0;
                postInvalidate();
            }
            return false;
        });
        setOnClickListener(this);
        setOnLongClickListener(this);
    }


    private void finishRecord() {
        if (mListener != null) {
            mListener.onFinish();
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (isRecording) {

            canvas.drawCircle(width / 2, height / 2, width / 2, fillPaint);

            int left = progressWidth / 2;
            int top = progressWidth / 2;
            int right = width - progressWidth / 2;
            int bottom = height - progressWidth / 2;
            float sweepAngle = (progressValue * 1.0f / progressMaxValue) * 360;
            canvas.drawArc(left, top, right, bottom, -90, sweepAngle, false, progressPaint);
        } else {
            canvas.drawCircle(width / 2, height / 2, radius, fillPaint);
        }
    }

    public void setMaxDuration(int maxDuration) {
        this.progressMaxValue = maxDuration * 1000 / PROGRESS_INTERVAL;
    }

    public void setOnRecordListener(onRecordListener listener) {

        mListener = listener;
    }

    @Override
    public boolean onLongClick(View v) {
        if (mListener != null) {
            mListener.onLongClick();
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        if (mListener != null) {
            mListener.onClick();
        }
    }

    public interface onRecordListener {
        void onClick();

        void onLongClick();

        void onFinish();
    }
}


