package com.mooc.ppjoke.ui;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mooc.libcommon.extention.LiveDataBus;
import com.mooc.libcommon.global.AppGlobals;
import com.mooc.libnetwork.ApiResponse;
import com.mooc.libnetwork.ApiService;
import com.mooc.libnetwork.JsonCallback;
import com.mooc.ppjoke.model.Comment;
import com.mooc.ppjoke.model.Feed;
import com.mooc.ppjoke.model.TagList;
import com.mooc.ppjoke.model.User;
import com.mooc.ppjoke.ui.login.UserManager;
import com.mooc.ppjoke.ui.share.ShareDialog;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

/***
 * 帖子互动的Presenter
 */
public class InteractionPresenter {

    public static final String DATA_FROM_INTERACTION = "data_from_interaction";
    //点赞
    private static final String URL_TOGGLE_FEED_LIK = "/ugc/toggleFeedLike";
    //踩
    private static final String URL_TOGGLE_FEED_DISS = "/ugc/dissFeed";
    //分享
    private static final String URL_SHARE = "/ugc/increaseShareCount";
    //评论
    private static final String URL_TOGGLE_COMMENT_LIKE = "/ugc/toggleCommentLike";

    //给一个帖子点赞/取消点赞，它和给帖子点踩一踩是互斥的
    public static void toggleFeedLike(LifecycleOwner owner, Feed feed) {
        //
        if (!isLogin(owner, user -> toggleFeedLikeInternal(feed))) {
        } else {
            toggleFeedLikeInternal(feed);
        }
    }

    /***
     * 点在的具体实现
     * @param feed
     */
    private static void toggleFeedLikeInternal(Feed feed) {
        ApiService.get(URL_TOGGLE_FEED_LIK)
                .addParam("userId", UserManager.get().getUserId())
                .addParam("itemId", feed.itemId)
                .execute(new JsonCallback<JSONObject>() {
                    //点赞成功的回调
                    @Override
                    public void onSuccess(ApiResponse<JSONObject> response) {
                        if (response.body != null) {
                            boolean hasLiked = response.body.getBoolean("hasLiked").booleanValue();
                            feed.getUgc().setHasLiked(hasLiked);
                            LiveDataBus.get().with(DATA_FROM_INTERACTION).postValue(feed);
                        }
                    }

                    //点赞失败的回调
                    @Override
                    public void onError(ApiResponse<JSONObject> response) {
                        showToast(response.message);
                    }
                });
    }

    //给一个帖子点踩一踩/取消踩一踩,它和给帖子点赞是互斥的
    public static void toggleFeedDiss(LifecycleOwner owner, Feed feed) {
        if (!isLogin(owner, user -> toggleFeedDissInternal(feed))) {
        } else {
            toggleFeedDissInternal(feed);
        }
    }

    /***
     * 踩的具体实现
     * @param feed
     */
    private static void toggleFeedDissInternal(Feed feed) {
        ApiService.get(URL_TOGGLE_FEED_DISS).addParam("userId", UserManager.get().getUserId())
                .addParam("itemId", feed.itemId)
                .execute(new JsonCallback<JSONObject>() {
                    //踩成功的处理逻辑
                    @Override
                    public void onSuccess(ApiResponse<JSONObject> response) {
                        if (response.body != null) {
                            boolean hasLiked = response.body.getBoolean("hasLiked").booleanValue();
                            feed.getUgc().setHasdiss(hasLiked);
                        }
                    }

                    //踩失败的处理逻辑
                    @Override
                    public void onError(ApiResponse<JSONObject> response) {
                        showToast(response.message);
                    }
                });
    }

    /***
     *  打开分享面板
     */
    public static void openShare(Context context, Feed feed) {
        String url = "http://h5.aliyun.ppjoke.com/item/%s?timestamp=%s&user_id=%s";
        //分享的文本
        String shareContent = feed.feeds_text;
        //分享的内容url
        if (!TextUtils.isEmpty(feed.url)) {
            shareContent = feed.url;
            //
        } else if (!TextUtils.isEmpty(feed.cover)) {
            shareContent = feed.cover;
        }
        //分享的对话框
        ShareDialog shareDialog = new ShareDialog(context);
        shareDialog.setShareContent(shareContent);
        //点击分享的处理
        shareDialog.setShareItemClickListener(v -> ApiService.get(URL_SHARE)
                .addParam("itemId", feed.itemId)
                .execute(new JsonCallback<JSONObject>() {
                    //分享成功
                    @Override
                    public void onSuccess(ApiResponse<JSONObject> response) {
                        if (response.body != null) {
                            int count = response.body.getIntValue("count");
                            feed.getUgc().setShareCount(count);
                        }
                    }

                    //分享失败
                    @Override
                    public void onError(ApiResponse<JSONObject> response) {
                        showToast(response.message);
                    }
                }));
        //显示对话框
        shareDialog.show();
    }

    /***
     *  给一个帖子的评论点赞/取消点赞
     */
    public static void toggleCommentLike(LifecycleOwner owner, Comment comment) {
        if (!isLogin(owner, new Observer<User>() {
            @Override
            public void onChanged(User user) {
                toggleCommentLikeInternal(comment);
            }
        })) {
        } else {
            toggleCommentLikeInternal(comment);
        }
    }

    /****
     * 给一条帖子的评论点赞的处理
     * @param comment
     */
    private static void toggleCommentLikeInternal(Comment comment) {

        ApiService.get(URL_TOGGLE_COMMENT_LIKE)
                .addParam("commentId", comment.commentId)
                .addParam("userId", UserManager.get().getUserId())
                .execute(new JsonCallback<JSONObject>() {
                    @Override
                    public void onSuccess(ApiResponse<JSONObject> response) {
                        if (response.body != null) {
                            boolean hasLiked = response.body.getBooleanValue("hasLiked");
                            comment.getUgc().setHasLiked(hasLiked);
                        }
                    }

                    @Override
                    public void onError(ApiResponse<JSONObject> response) {
                        showToast(response.message);
                    }
                });
    }

    /**
     * 收藏/取消收藏一个帖子
     */
    public static void toggleFeedFavorite(LifecycleOwner owner, Feed feed) {
        if (!isLogin(owner, user -> toggleFeedFavorite(feed))) {
        } else {
            toggleFeedFavorite(feed);
        }
    }

    /***
     * 收藏一条帖子的具体实现
     * @param feed
     */
    private static void toggleFeedFavorite(Feed feed) {
        ApiService.get("/ugc/toggleFavorite")
                .addParam("itemId", feed.itemId)
                .addParam("userId", UserManager.get().getUserId())
                .execute(new JsonCallback<JSONObject>() {
                    @Override
                    public void onSuccess(ApiResponse<JSONObject> response) {
                        if (response.body != null) {
                            boolean hasFavorite = response.body.getBooleanValue("hasFavorite");
                            feed.getUgc().setHasFavorite(hasFavorite);
                            LiveDataBus.get().with(DATA_FROM_INTERACTION)
                                    .postValue(feed);
                        }
                    }

                    @Override
                    public void onError(ApiResponse<JSONObject> response) {
                        showToast(response.message);
                    }
                });
    }


    /***
     *  收藏/取消收藏一个帖子
     */
    public static void toggleFollowUser(LifecycleOwner owner, Feed feed) {
        if (!isLogin(owner, user -> toggleFollowUser(feed))) {
        } else {
            toggleFollowUser(feed);
        }
    }

    /***
     * 关注用户的具体实现
     * @param feed
     */
    private static void toggleFollowUser(Feed feed) {
        ApiService.get("/ugc/toggleUserFollow")
                .addParam("followUserId", UserManager.get().getUserId())
                .addParam("userId", feed.author.userId)
                .execute(new JsonCallback<JSONObject>() {
                    @Override
                    public void onSuccess(ApiResponse<JSONObject> response) {
                        if (response.body != null) {
                            boolean hasFollow = response.body.getBooleanValue("hasLiked");
                            feed.getAuthor().setHasFollow(hasFollow);
                            LiveDataBus.get().with(DATA_FROM_INTERACTION)
                                    .postValue(feed);
                        }
                    }

                    @Override
                    public void onError(ApiResponse<JSONObject> response) {
                        showToast(response.message);
                    }
                });
    }

    /***
     * 删除一条帖子
     * @param context  帖子的内容
     * @param itemId   贴在的itemid
     * @return 返回一个包含删除内容的LiveData对象
     */
    public static LiveData<Boolean> deleteFeed(Context context, long itemId) {
        MutableLiveData<Boolean> liveData = new MutableLiveData<>();
        new AlertDialog.Builder(context)
                .setNegativeButton("删除", (dialog, which) -> {
                    dialog.dismiss();
                    deleteFeedInternal(liveData, itemId);
                }).setPositiveButton("取消", (dialog, which) -> dialog.dismiss()).setMessage("确定要删除这条评论吗？").create().show();
        return liveData;
    }

    /***
     * 删除贴在的网络请求
     * @param liveData
     * @param itemId
     */
    private static void deleteFeedInternal(MutableLiveData<Boolean> liveData, long itemId) {
        ApiService.get("/feeds/deleteFeed")
                .addParam("itemId", itemId)
                .execute(new JsonCallback<JSONObject>() {
                    @Override
                    public void onSuccess(ApiResponse<JSONObject> response) {
                        if (response.body != null) {
                            boolean success = response.body.getBoolean("result");
                            liveData.postValue(success);
                            showToast("删除成功");
                        }
                    }

                    @Override
                    public void onError(ApiResponse<JSONObject> response) {
                        showToast(response.message);
                    }
                });
    }

    /***
     * 删除某个帖子的一个评论
     *   @param context  帖子的内容
     *   @param itemId   贴在的itemid
     *   @return 返回一个包含删除帖子评论结果的LiveData对象
     */

    public static LiveData<Boolean> deleteFeedComment(Context context, long itemId, long commentId) {
        MutableLiveData<Boolean> liveData = new MutableLiveData<>();
        new AlertDialog.Builder(context)
                .setNegativeButton("删除", (dialog, which) -> {
                    dialog.dismiss();
                    deleteFeedCommentInternal(liveData, itemId, commentId);
                }).setPositiveButton("取消", (dialog, which) -> dialog.dismiss()).setMessage("确定要删除这条评论吗？").create().show();
        return liveData;
    }

    /***
     * 删除帖子的评论
     * @param liveData
     * @param itemId
     * @param commentId
     */
    private static void deleteFeedCommentInternal(LiveData liveData, long itemId, long commentId) {
        ApiService.get("/comment/deleteComment")
                .addParam("userId", UserManager.get().getUserId())
                .addParam("commentId", commentId)
                .addParam("itemId", itemId)
                .execute(new JsonCallback<JSONObject>() {
                    @Override
                    public void onSuccess(ApiResponse<JSONObject> response) {
                        if (response.body != null) {
                            boolean result = response.body.getBooleanValue("result");
                            ((MutableLiveData) liveData).postValue(result);
                            showToast("评论删除成功");
                        }
                    }

                    @Override
                    public void onError(ApiResponse<JSONObject> response) {
                        showToast(response.message);
                    }
                });
    }


    /***
     * 关注/取消关注一个帖子标签
     */

    public static void toggleTagLike(LifecycleOwner owner, TagList tagList) {
        if (!isLogin(owner, new Observer<User>() {
            @Override
            public void onChanged(User user) {
                toggleTagLikeInternal(tagList);
            }
        })) ;
        else {
            toggleTagLikeInternal(tagList);
        }
    }

    /***
     * 帖子的关注状态的处理
     * @param tagList
     */
    private static void toggleTagLikeInternal(TagList tagList) {
        ApiService.get("/tag/toggleTagFollow")
                .addParam("tagId", tagList.tagId)
                .addParam("userId", UserManager.get().getUserId())
                .execute(new JsonCallback<JSONObject>() {
                    @Override
                    public void onSuccess(ApiResponse<JSONObject> response) {
                        if (response.body != null) {
                            Boolean follow = response.body.getBoolean("hasFollow");
                            tagList.setHasFollow(follow);
                        }
                    }

                    @Override
                    public void onError(ApiResponse<JSONObject> response) {
                        showToast(response.message);
                    }
                });
    }

    /***
     * Toast提示
     * @param message
     */
    @SuppressLint("RestrictedApi")
    private static void showToast(String message) {
        ArchTaskExecutor.getMainThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(AppGlobals.getApplication(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /***
     * 判断当前是不是已经登陆的状态
     * 对登陆成功返回的LiveData数据进行观察，在onChange方法中处理user  同时remove掉LiveData的观察者对象
     * @param owner
     * @param observer
     * @return
     */
    private static boolean isLogin(LifecycleOwner owner, Observer<User> observer) {
        if (UserManager.get().isLogin()) {
            return true;
        } else {
            LiveData<User> liveData = UserManager.get().login(AppGlobals.getApplication());
            if (owner == null) {
                liveData.observeForever(loginObserver(observer, liveData));
            } else {
                liveData.observe(owner, loginObserver(observer, liveData));
            }
            return false;
        }
    }

    /***
     * 监听登陆的结果的观察者 在 onChange()方法中对登陆成功的LiveData数据进行处理
     * @param observer  观察者对象
     * @param liveData  登陆回来的数据
     * @return
     */
    @NotNull
    private static Observer<User> loginObserver(Observer<User> observer, LiveData<User> liveData) {
        return new Observer<User>() {
            @Override
            public void onChanged(User user) {
                liveData.removeObserver(this);
                //登陆成功的处理
                if (user != null && observer != null) {
                    observer.onChanged(user);
                }
            }
        };
    }

}
