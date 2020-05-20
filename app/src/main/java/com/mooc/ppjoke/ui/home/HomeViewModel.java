package com.mooc.ppjoke.ui.home;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.ItemKeyedDataSource;
import androidx.paging.PagedList;

import com.alibaba.fastjson.TypeReference;
import com.mooc.libnetwork.ApiResponse;
import com.mooc.libnetwork.ApiService;
import com.mooc.libnetwork.JsonCallback;
import com.mooc.libnetwork.Request;
import com.mooc.ppjoke.ui.AbsViewModel;
import com.mooc.ppjoke.model.Feed;
import com.mooc.ppjoke.ui.MutablePageKeyedDataSource;
import com.mooc.ppjoke.ui.login.UserManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/***
 * 已完成
 */
public class HomeViewModel extends AbsViewModel<Feed> {
    //是否缓存
    private volatile boolean witchCache = true;
    //缓存的PagedList数据
    private MutableLiveData<PagedList<Feed>> cacheLiveData = new MutableLiveData<>();
    //是否进行加载更多
    private AtomicBoolean loadAfter = new AtomicBoolean(false);
    //当前的显示的数据的类型
    private String mFeedType;

    /***
     * 创建DataSource的方法
     * @return 返回DataSource
     */
    @Override
    public DataSource createDataSource() {
        return new FeedDataSource();
    }

    /***
     * 获取获取的PagedList数据
     * @return
     */
    public MutableLiveData<PagedList<Feed>> getCacheLiveData() {
        return cacheLiveData;
    }

    /***
     * 设置当前的数据的类型
     * @param feedType
     */
    public void setFeedType(String feedType) {
        mFeedType = feedType;
    }

    /***
     * 创建DataSource的具体实现的方法
     */
    class FeedDataSource extends ItemKeyedDataSource<Integer, Feed> {
        //初始化pagedlist的时候调用的方法对数据进行加载
        @Override
        public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull LoadInitialCallback<Feed> callback) {
            Log.e("homeviewmodel", "loadInitial: ");
            //先加载缓存  在加载网络数据 网络数据成功之后更新缓存
            loadData(0, params.requestedLoadSize, callback);
             witchCache = false;
        }

        //对加载的数据进行分页的逻辑 因为加载的数据很多 在屏幕中只显示pagedlist个，当滑动的以后对数据分页 显示
        @Override
        public void loadAfter(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Feed> callback) {
            //向后加载分页数据的
            Log.e("homeviewmodel", "loadAfter: ");
            loadData(params.key, params.requestedLoadSize, callback);
        }

        //向前加载数据
        @Override
        public void loadBefore(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Feed> callback) {
            callback.onResult(Collections.emptyList());
            //能够向前加载数据的
        }

        /**
         * 获取给定的item的 key  通过这个key进行后续的分页的数据的加载
         */
        @NonNull
        @Override
        public Integer getKey(@NonNull Feed item) {
            return item.id;
        }
    }

    /***
     * 发起网络请求加载数据
     * @param key  通过key是否大于0判断是不是分页数据
     * @param count 每页加载的数据量
     * @param callback 加载数据的状态的回调
     */
    private void loadData(int key, int count, ItemKeyedDataSource.LoadCallback<Feed> callback) {
         // key>0 触发分页的逻辑
        if (key > 0) {
            loadAfter.set(true);
    }
        //发起网络请求获取帖子列表数据
        Request request = ApiService.get("/feeds/queryHotFeedsList")
                .addParam("feedType", mFeedType)
                .addParam("userId", UserManager.get().getUserId())
                .addParam("feedId", key)
                .addParam("pageCount", count)
                .responseType(new TypeReference<ArrayList<Feed>>() {
                }.getType());
        //判断是否加载缓存
        if (witchCache) {
            request.cacheStrategy(Request.CACHE_ONLY);
            request.execute(new JsonCallback<List<Feed>>() {
                @Override
                public void onCacheSuccess(ApiResponse<List<Feed>> response) {
                    Log.e("loadData", "onCacheSuccess: ");
                    //创建一个MutablePageKeyedDataSource对象
                    MutablePageKeyedDataSource dataSource = new MutablePageKeyedDataSource<Feed>();
                    dataSource.data.addAll(response.body);
                    //将网络情的数据转化为pagedlist
                    PagedList pagedList = dataSource.buildNewPagedList(config);
                    //更改livedata数据
                    cacheLiveData.postValue(pagedList);
                }
            });
        }
        try {
            //
            //  进行网路请求的设置的缓存策略的指定
            Request netRequest = witchCache ? request.clone() : request;
            //  下拉刷新，上拉分页  上拉分页不需要更新缓存  下拉刷新更新缓存
            netRequest.cacheStrategy(key == 0 ? Request.NET_CACHE : Request.NET_ONLY);
            ApiResponse<List<Feed>> response = netRequest.execute();
            List<Feed> data = response.body == null ? Collections.emptyList() : response.body;
            //从DataSource加载数据
            callback.onResult(data);
            //此次分页的操作执行完成之后
            if (key > 0) {
                //通过BoundaryPageData发送数据 告诉UI层 是否应该主动关闭上拉加载分页的动画
                ((MutableLiveData) getBoundaryPageData()).postValue(data.size() > 0);
                //设置是否进行分页为false
                loadAfter.set(false);
            }
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        Log.e("loadData", "loadData: key:" + key);

    }

    /****
     * 上拉加载分页之后的处理逻辑
     * @param id  加载数据时候的参数id
     * @param callback  加载之后的回调处理
     */
    @SuppressLint("RestrictedApi")
    public void loadAfter(int id, ItemKeyedDataSource.LoadCallback<Feed> callback) {
        if (loadAfter.get()) {
            // 传递一个空的数据   之后重新加载网络数据  重新构建pagelist实现数据的显示
            callback.onResult(Collections.emptyList());
            return;
        }
        //下拉加载分页的逻辑触发
        ArchTaskExecutor.getIOThreadExecutor().execute(() -> loadData(id, config.pageSize, callback));
    }
}