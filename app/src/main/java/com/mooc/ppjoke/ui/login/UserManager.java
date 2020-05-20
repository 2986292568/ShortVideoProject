package com.mooc.ppjoke.ui.login;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mooc.libcommon.extention.LiveDataBus;
import com.mooc.libcommon.global.AppGlobals;
import com.mooc.libnetwork.ApiResponse;
import com.mooc.libnetwork.ApiService;
import com.mooc.libnetwork.JsonCallback;
import com.mooc.libnetwork.cache.CacheManager;
import com.mooc.ppjoke.model.User;

/****
 * 已完成
 * 账户的管理类
 */
public class UserManager {
    //日志打印标示
    private static final String KEY_CACHE_USER = "cache_user";
    //用户管理类对象
    private static UserManager mUserManager = new UserManager();
    //保存用户的LiveData对象
    private MutableLiveData<User> userLiveData;
    //用户对象
    private User mUser;
    //获取用户管理对象的方法
    public static UserManager get() {
        return mUserManager;
    }

    private UserManager() {
        User cache = (User) CacheManager.getCache(KEY_CACHE_USER);
        if (cache != null && cache.expires_time > System.currentTimeMillis()) {
            mUser = cache;
        }
    }

    /***
     * 保存用户到缓存
     * @param user
     */
    public void save(User user) {
        mUser = user;
        CacheManager.save(KEY_CACHE_USER, user);
        if (getUserLiveData().hasObservers()) {
            getUserLiveData().postValue(user);
        }
    }

    /***
     * 登陆
     * @param context
     * @return
     */
    public LiveData<User> login(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
        return getUserLiveData();
    }

    /***
     * 判断用户是不是登陆状态
     * @return
     */
    public boolean isLogin() {
        return mUser == null ? false : mUser.expires_time > System.currentTimeMillis();
    }

    /***
     * 获取已经登陆的用户对象
     * @return
     */
    public User getUser() {
        return isLogin() ? mUser : null;
    }

    /***
     * 获取已经登陆的用户对象的id
     * @return
     */
    public long getUserId() {
        return isLogin() ? mUser.userId : 0;
    }

    /***
     * 用户的刷新处理
     * @return
     */
    public LiveData<User> refresh() {
        if (!isLogin()) {
            return login(AppGlobals.getApplication());
        }
        MutableLiveData<User> liveData = new MutableLiveData<>();
        ApiService.get("/user/query")
                .addParam("userId", getUserId())
                .execute(new JsonCallback<User>() {
                    @Override
                    public void onSuccess(ApiResponse<User> response) {
                        save(response.body);
                        liveData.postValue(getUser());
                    }

                    @SuppressLint("RestrictedApi")
                    @Override
                    public void onError(ApiResponse<User> response) {
                        ArchTaskExecutor.getMainThreadExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(AppGlobals.getApplication(), response.message, Toast.LENGTH_SHORT).show();
                            }
                        });
                        liveData.postValue(null);
                    }
                });
        return liveData;
    }

    /**
     * bugfix:  liveData默认情况下是支持黏性事件的，即之前已经发送了一条消息，当有新的observer注册进来的时候，也会把先前的消息发送给他，
     * <p>
     * 就造成了{@linkplain com.mooc.ppjoke.MainActivity#onNavigationItemSelected(MenuItem) }死循环
     * <p>
     * 那有两种解决方法
     * 1.我们在退出登录的时候，把livedata置为空，或者将其内的数据置为null
     * 2.利用我们改造的stickyLiveData来发送这个登录成功的事件
     * <p>
     * 我们选择第一种,把livedata置为空
     */
    public void logout() {
        CacheManager.delete(KEY_CACHE_USER, mUser);
        mUser = null;
        userLiveData = null;
    }

    /***
     * 获取一个MutableLiveData对象
     * @return
     */
    private MutableLiveData<User> getUserLiveData() {
        if (userLiveData == null) {
            userLiveData = new MutableLiveData<>();
        }
        return userLiveData;
    }
}
