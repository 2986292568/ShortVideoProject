package com.mooc.ppjoke.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.paging.ItemKeyedDataSource;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;

import com.mooc.libnavannotation.FragmentDestination;
import com.mooc.ppjoke.exoplayer.PageListPlayDetector;
import com.mooc.ppjoke.exoplayer.PageListPlayManager;
import com.mooc.ppjoke.model.Feed;
import com.mooc.ppjoke.ui.AbsListFragment;
import com.mooc.ppjoke.ui.MutablePageKeyedDataSource;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

import java.util.List;

/***
 * 已完成
 */
@FragmentDestination(pageUrl = "main/tabs/home", asStarter = true)
public class HomeFragment extends AbsListFragment<Feed, HomeViewModel> {
    //视频控制对象
    private PageListPlayDetector playDetector;
    //帖子的类型标识
    private String feedType;
    //是否应该暂停播放
    private boolean shouldPause = true;
    /***
     * 创建一个包含feedType参数的HomeFragment
     * @param feedType
     * @return
     */
    public static HomeFragment newInstance(String feedType) {
        Bundle args = new Bundle();
        args.putString("feedType", feedType);
        HomeFragment fragment = new HomeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /***
     * 当View创建的时候回调的方法
     * @param view
     * @param savedInstanceState
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        //对缓存的数据进行观察在数据发生变化的时候通过adapter进行UI的更新
        mViewModel.getCacheLiveData().observe(this, feeds -> submitList(feeds));
        //初始化视频控制器对象
        playDetector = new PageListPlayDetector(this, mRecyclerView);
        //此处的mViewModel的类型是HomeViewModle
        mViewModel.setFeedType(feedType);
    }

    /***
     * 创建pagelistadapter的方法
     * @return
     */
    @Override
    public PagedListAdapter getAdapter() {
        //通过feedtype的类型显示对应的内容
        feedType = getArguments() == null ? "all" : getArguments().getString("feedType");
        return new FeedAdapter(getContext(), feedType) {
            //当item划入屏幕的处理
            @Override
            public void onViewAttachedToWindow2(@NonNull ViewHolder holder) {
                if (holder.isVideoItem()) {
                    playDetector.addTarget(holder.getListPlayerView());
                }
            }
            //当item划出屏幕的时候的处理
            @Override
            public void onViewDetachedFromWindow2(@NonNull ViewHolder holder) {
                playDetector.removeTarget(holder.getListPlayerView());
            }
           // 判断是否要暂停播放
            @Override
            public void onStartFeedDetailActivity(Feed feed) {
                boolean isVideo = feed.itemType == Feed.TYPE_VIDEO;
                shouldPause = !isVideo;
            }
            //当前的pagedlist数据发生改变的时候的回调方法
            @Override
            public void onCurrentListChanged(@Nullable PagedList<Feed> previousList, @Nullable PagedList<Feed> currentList) {
                //这个方法是在我们每提交一次 pagelist对象到adapter 就会触发一次
                //每调用一次 adpater.submitlist
                if (previousList != null && currentList != null) {
                    if (!currentList.containsAll(previousList)) {
                        mRecyclerView.scrollToPosition(0);
                    }
                }
            }
        };
    }

    /****
     * 上拉加载更多的时候的回调方法
     * @param refreshLayout 上拉分页控件
     */
    @Override
    public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
        //获取当前的adapter对应的pagelist数据
        final PagedList<Feed> currentList = adapter.getCurrentList();
        //上拉加载没有数据数据的时候显示emptyview
        if (currentList == null || currentList.size() <= 0) {
            finishRefresh(false);
            return;
        }
        //获取最后一个item的id信息
        Feed feed = currentList.get(adapter.getItemCount() - 1);
        //进行下拉分页的逻辑处理
        mViewModel.loadAfter(feed.id, new ItemKeyedDataSource.LoadCallback<Feed>() {
            //  data ：获取为pagedlist设置empty数据之后重新加载的数据
            @Override
            public void onResult(@NonNull List<Feed> data) {
                PagedList.Config config = currentList.getConfig();
                if (data != null && data.size() > 0) {
                    //这里 咱们手动接管 分页数据加载的时候 使用MutableItemKeyedDataSource也是可以的。
                    //由于当且仅当 paging不再帮我们分页的时候，我们才会接管。所以 就不需要ViewModel中创建的DataSource继续工作了，所以使用
                    //MutablePageKeyedDataSource也是可以的
                    MutablePageKeyedDataSource dataSource = new MutablePageKeyedDataSource();
                    //这里要把列表上已经显示的先添加到dataSource.data中
                    //而后把本次分页回来的数据再添加到dataSource.data中
                    dataSource.data.addAll(currentList);
                    dataSource.data.addAll(data);
                    PagedList pagedList = dataSource.buildNewPagedList(config);
                    //更新下拉刷新的数据
                    submitList(pagedList);
                }
            }
        });
    }

    /***
     * 下拉刷新的回调方法
     * @param refreshLayout
     */
    @Override
    public void onRefresh(@NonNull RefreshLayout refreshLayout) {
        //invalidate 之后Paging会重新创建一个DataSource 重新调用它的loadInitial方法加载初始化数据
        //详情见：LivePagedListBuilder#compute方法
        mViewModel.getDataSource().invalidate();
    }

    /***
     * 底部导航栏切换的时候调用的方法
     * @param hidden
     */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            playDetector.onPause();
        } else {
            playDetector.onResume();
        }
    }

    @Override
    public void onPause() {
        //如果是跳转到详情页,咱们就不需要 暂停视频播放了
        //如果是前后台切换 或者去别的页面了 都是需要暂停视频播放的
        if (shouldPause) {
            playDetector.onPause();
        }
        Log.e("homefragment", "onPause: feedtype:" + feedType);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        shouldPause = true;
        //由于沙发Tab的几个子页面 复用了HomeFragment。
        //我们需要判断下 当前页面 它是否有ParentFragment.
        //当且仅当 它和它的ParentFragment均可见的时候，才能恢复视频播放
        if (getParentFragment() != null) {
            //isVisible()判断fragment是否可见
            if (getParentFragment().isVisible() && isVisible()) {
                Log.e("homefragment", "onResume: feedtype:" + feedType);
                playDetector.onResume();
            }
        } else {
            if (isVisible()) {
                Log.e("homefragment", "onResume: feedtype:" + feedType);
                playDetector.onResume();
            }
        }
    }


    @Override
    public void onDestroy() {
        //记得销毁
        PageListPlayManager.release(feedType);
        super.onDestroy();
    }
}