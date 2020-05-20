package com.mooc.ppjoke.exoplayer;

import android.util.Pair;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 列表视频自动播放 检测逻辑
 */
public class PageListPlayDetector {

    //收集一个个的能够进行视频播放的 对象，面向接口
    private List<IPlayTarget> mTargets = new ArrayList<>();
    //展示播放的视频item的控件
    private RecyclerView mRecyclerView;
    //正在播放的那个
    private IPlayTarget playingTarget;
    //添加要播放的对象
    public void addTarget(IPlayTarget target) {
        mTargets.add(target);
    }
    //删除要播放的对象
    public void removeTarget(IPlayTarget target) {
        mTargets.remove(target);
    }

    /***
     * 构造器
     * @param owner  声明周期管理对象
     * @param recyclerView  展示数据的列表
     */
    public PageListPlayDetector(LifecycleOwner owner, RecyclerView recyclerView) {
        //初始化显示数据的列表控件
        mRecyclerView = recyclerView;
        //监听数组的声明周期
        owner.getLifecycle().addObserver(new LifecycleEventObserver() {
            //数组声明周期变化就会回调到这个方法中
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                // onDestory()进行一系列的清理工作
                if (event == Lifecycle.Event.ON_DESTROY) {
                    playingTarget = null;
                    mTargets.clear();
                    mRecyclerView.removeCallbacks(delayAutoPlay);
                    recyclerView.removeOnScrollListener(scrollListener);
                    owner.getLifecycle().removeObserver(this);
                }
            }
        });
        //注册一个观察者 观察recycleview中的数据的变化
        recyclerView.getAdapter().registerAdapterDataObserver(mDataObserver);
        // 滚动监听
        recyclerView.addOnScrollListener(scrollListener);

    }

    /***
     * recycleview的滚动监听对象
     */
    RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        // recycleview的滚动状态变化的时候的回调
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            // 判断当滚动停止的时候进行自动播放
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                autoPlay();
            }
        }

        //recycleview滚动的时候的处理逻辑
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (dx == 0 && dy == 0) {
                //时序问题。当执行了AdapterDataObserver#onItemRangeInserted  可能还没有被布局到RecyclerView上。
                //所以此时 recyclerView.getChildCount()还是等于0的。
                //等childView 被布局到RecyclerView上之后，会执行onScrolled（）方法
                //并且此时 dx,dy都等于0
                postAutoPlay();
            } else {
                //如果有正在播放的,且滑动时被划出了屏幕 则 停止他
                if (playingTarget != null && playingTarget.isPlaying() && !isTargetInBounds(playingTarget)) {
                    playingTarget.inActive();
                }
            }
        }
    };

    /***
     * 加入到消息队列延迟播放
     */
    private void postAutoPlay() {
        mRecyclerView.post(delayAutoPlay);
    }

    Runnable delayAutoPlay = () -> autoPlay();
    /***
     * 监听数据 当有数据加载到recycleview中的时候就会回调到这个方法中实现自动播放
     */
    private final RecyclerView.AdapterDataObserver mDataObserver = new RecyclerView.AdapterDataObserver() {
        //有数据插入的时候的回调方法
        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            postAutoPlay();
        }
    };

    /***
     * target的自动播放的逻辑
     */
    private void autoPlay() {
        //判断你不是已经有视频播放的item了
        if (mTargets.size() <= 0 || mRecyclerView.getChildCount() <= 0) {
            return;
        }
        //对当前正在播放的target的判断和处理
        if (playingTarget != null && playingTarget.isPlaying() && isTargetInBounds(playingTarget)) {
            return;
        }
        // 一个保存正在播放的tager对象
        IPlayTarget activeTarget = null;
        for (IPlayTarget target : mTargets) {
            //判断是不是符合播放的条件
            boolean inBounds = isTargetInBounds(target);
            if (inBounds) {
                //符合条件 则对活跃的target进行初始化
                activeTarget = target;
                break;
            }
        }

        if (activeTarget != null) {
            //停止正在播放的target
            if (playingTarget != null) {
                playingTarget.inActive();
            }
            //播放符合条件的活跃的target
            playingTarget = activeTarget;
            activeTarget.onActive();
        }
    }

    /**
     * 检测 IPlayTarget 所在的 viewGroup 是否至少还有一半的大小在屏幕内
     *
     * @param target
     * @return
     */
    private boolean isTargetInBounds(IPlayTarget target) {
        ViewGroup owner = target.getOwner();
        ensureRecyclerViewLocation();
        if (!owner.isShown() || !owner.isAttachedToWindow()) {
            return false;
        }

        int[] location = new int[2];
        owner.getLocationOnScreen(location);

        int center = location[1] + owner.getHeight() / 2;

        //承载视频播放画面的ViewGroup它需要至少一半的大小 在RecyclerView上下范围内
        return center >= rvLocation.first && center <= rvLocation.second;
    }

    /***
     * 存储位置信息的对象
     */
    private Pair<Integer, Integer> rvLocation = null;

    /***
     * 获取recycleview在屏幕中的位置
     * @return
     */
    private Pair<Integer, Integer> ensureRecyclerViewLocation() {
        if (rvLocation == null) {
            int[] location = new int[2];
            mRecyclerView.getLocationOnScreen(location);

            int top = location[1];
            int bottom = top + mRecyclerView.getHeight();

            rvLocation = new Pair(top, bottom);
        }
        return rvLocation;
    }

    public void onPause() {
        if (playingTarget != null) {
            playingTarget.inActive();
        }
    }

    public void onResume() {
        if (playingTarget != null) {
            playingTarget.onActive();
        }
    }
}
