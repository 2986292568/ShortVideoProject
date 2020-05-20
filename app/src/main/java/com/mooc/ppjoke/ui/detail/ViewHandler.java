package com.mooc.ppjoke.ui.detail;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.paging.ItemKeyedDataSource;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mooc.libcommon.utils.PixUtils;
import com.mooc.libcommon.view.EmptyView;
import com.mooc.ppjoke.R;
import com.mooc.ppjoke.databinding.LayoutFeedDetailBottomInateractionBinding;
import com.mooc.ppjoke.model.Comment;
import com.mooc.ppjoke.model.Feed;
import com.mooc.ppjoke.ui.MutableItemKeyedDataSource;

/***
 * 图文详情页的相同的功能都在此处开发
 */
public abstract class ViewHandler {
    //保存评论数据列表的viewmodel
    private final FeedDetailViewModel viewModel;
    //加载布局使用的context对象
    protected FragmentActivity mActivity;
    //内部显示的帖子数据
    protected Feed mFeed;
    //显示帖子信息的列表
    protected RecyclerView mRecyclerView;
    //处理底部布局的viewbinding对象
    protected LayoutFeedDetailBottomInateractionBinding mInateractionBinding;
    //帖子评论区的适配器
    protected FeedCommentAdapter listAdapter;
    //评论内容的对话框
    private CommentDialog commentDialog;

    public ViewHandler(FragmentActivity activity) {
        mActivity = activity;
        //初始化viewmodel
        viewModel = ViewModelProviders.of(activity).get(FeedDetailViewModel.class);
    }

    /***
     * 初始化帖子数据同时进行绑定
     * @param feed
     */
    @CallSuper
    public void bindInitData(Feed feed) {
        //底部的viewbinding设置lifeowner对象
        mInateractionBinding.setOwner(mActivity);
        mFeed = feed;
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setItemAnimator(null);
        listAdapter = new FeedCommentAdapter(mActivity) {
            @Override
            public void onCurrentListChanged(@Nullable PagedList<Comment> previousList, @Nullable PagedList<Comment> currentList) {
                boolean empty = currentList.size() <= 0;
                handleEmpty(!empty);
            }
        };
        mRecyclerView.setAdapter(listAdapter);
        viewModel.setItemId(mFeed.itemId);
        //监听帖子数据的变化
        viewModel.getPageData().observe(mActivity, comments -> {
            //提交到recycleview显示数据
            listAdapter.submitList(comments);
            handleEmpty(comments.size() > 0);
        });
        //评论按钮的点击事件
        mInateractionBinding.inputView.setOnClickListener(v -> showCommentDialog());
    }

    /***
     * 展示评论对话框
     */
    private void showCommentDialog() {
        if (commentDialog == null) {
            commentDialog = CommentDialog.newInstance(mFeed.itemId);
        }
        //评论成功的回调
        commentDialog.setCommentAddListener(comment -> {
            handleEmpty(true);
            listAdapter.addAndRefreshList(comment);
        });
        commentDialog.show(mActivity.getSupportFragmentManager(), "comment_dialog");
    }

    private EmptyView mEmptyView;

    public void handleEmpty(boolean hasData) {
        if (hasData) {
            if (mEmptyView != null) {
                listAdapter.removeHeaderView(mEmptyView);
            }
        } else {
            if (mEmptyView == null) {
                mEmptyView = new EmptyView(mActivity);
                RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.topMargin = PixUtils.dp2px(40);
                mEmptyView.setLayoutParams(layoutParams);
                mEmptyView.setTitle(mActivity.getString(R.string.feed_comment_empty));
            }
            listAdapter.addHeaderView(mEmptyView);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (commentDialog != null && commentDialog.isAdded()) {
            commentDialog.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void onPause() {

    }

    public void onResume() {

    }

    public void onBackPressed() {

    }
}
