package com.mooc.ppjoke.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import com.mooc.ppjoke.model.Feed;

/***
 * 帖子详情页的处理逻辑
 */
public class FeedDetailActivity extends AppCompatActivity {
    private static final String KEY_FEED = "key_feed";
    public static final String KEY_CATEGORY = "key_category";
    //评论列表 和底部的互动的功能的实现
    private ViewHandler viewHandler = null;

    /**
     * 从别的界面启动详情页
     * @param context  跳转的上下文
     * @param item     跳转到详情页传递的数据
     * @param category
     */
    public static void startFeedDetailActivity(Context context, Feed item, String category) {
        Intent intent = new Intent(context, FeedDetailActivity.class);
        intent.putExtra(KEY_FEED, item);
        intent.putExtra(KEY_CATEGORY, category);
        context.startActivity(intent);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //读取传递过来的帖子对象
        Feed feed = (Feed) getIntent().getSerializableExtra(KEY_FEED);
        if (feed == null) {
            finish();
            return;
        }
        //判断帖子的类型 获取操作帖子的对象
        if (feed.itemType == Feed.TYPE_IMAGE_TEXT) {
            //操作图文帖子
            viewHandler = new ImageViewHandler(this);
        } else {
            //操作视频帖子
            viewHandler = new VideoViewHandler(this);
        }
        //绑定初始化数据
        viewHandler.bindInitData(feed);
    }

    /***
     * 启动视频录制activty返回之后的处理
     * @param requestCode  请求码
     * @param resultCode   结果码
     * @param data   返回的数据
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (viewHandler != null) {
            //具体的操作委托给对应的viewHandler来实现
            viewHandler.onActivityResult(requestCode, resultCode, data);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (viewHandler != null) {
            viewHandler.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewHandler != null) {
            viewHandler.onResume();
        }
    }

    @Override
    public void onBackPressed() {
        if (viewHandler != null) {
            viewHandler.onBackPressed();
        }
        super.onBackPressed();
    }
}
