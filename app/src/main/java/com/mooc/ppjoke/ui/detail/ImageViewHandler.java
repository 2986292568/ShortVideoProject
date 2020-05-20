package com.mooc.ppjoke.ui.detail;

import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.mooc.ppjoke.R;
import com.mooc.ppjoke.databinding.ActivityFeedDetailTypeImageBinding;
import com.mooc.ppjoke.databinding.LayoutFeedDetailTypeImageHeaderBinding;
import com.mooc.ppjoke.model.Feed;
import com.mooc.ppjoke.view.PPImageView;

/***
 * 图片帖子的相关操作
 */
public class ImageViewHandler extends ViewHandler {
    // 帖子详情页的binding对象
    protected ActivityFeedDetailTypeImageBinding mImageBinding;
    //帖子详情的顶部
    protected LayoutFeedDetailTypeImageHeaderBinding mHeaderBinding;

    /***
     * 设置图片梯帖子的布局
     * @param activity
     */
    public ImageViewHandler(FragmentActivity activity) {
        super(activity);
        //初始化帖子详情对象的binding对象
        mImageBinding = DataBindingUtil.setContentView(activity, R.layout.activity_feed_detail_type_image);
        //帖子详情页底部互动布局的binding对象的初始化
        mInateractionBinding = mImageBinding.interactionLayout;
        mRecyclerView = mImageBinding.recyclerView;
        //返回按钮的监听
        mImageBinding.actionClose.setOnClickListener(v -> mActivity.finish());

    }

    /***
     * 绑定初始化数据
     * @param feed 帖子的详情内容
     */
    @Override
    public void bindInitData(Feed feed) {
        super.bindInitData(feed);
        mImageBinding.setFeed(mFeed);
        //初始化header的布局样式
        mHeaderBinding = LayoutFeedDetailTypeImageHeaderBinding.inflate(LayoutInflater.from(mActivity), mRecyclerView, false);
        mHeaderBinding.setFeed(mFeed);
        // 顶部头像的初始化
        PPImageView headerImage = mHeaderBinding.headerImage;
        headerImage.bindData(mFeed.width, mFeed.height, mFeed.width > mFeed.height ? 0 : 16, mFeed.cover);
        //添加一个header
        listAdapter.addHeaderView(mHeaderBinding.getRoot());
        //列表的滚动监听
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //设置标题栏的显示与隐藏
                boolean visible = mHeaderBinding.getRoot().getTop() <= -mImageBinding.titleLayout.getMeasuredHeight();
                mImageBinding.authorInfoLayout.getRoot().setVisibility(visible ? View.VISIBLE : View.GONE);
                mImageBinding.title.setVisibility(visible ? View.GONE : View.VISIBLE);

            }
        });
        handleEmpty(false);
    }
}
