package com.mooc.libcommon.extention;

import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 已完成
 * 一个能够添加HeaderView,FooterView的PagedListAdapter。
 * 解决了添加HeaderView和FooterView时 RecyclerView定位不准确的问题
 *
 * @param <T>  Java Bean
 * @param <VH>
 */
public abstract class AbsPagedListAdapter<T, VH extends RecyclerView.ViewHolder> extends PagedListAdapter<T, VH> {
    // 存储header的集合
    private SparseArray<View> mHeaders = new SparseArray<>();
    // 存储footer的集合
    private SparseArray<View> mFooters = new SparseArray<>();
    private int BASE_ITEM_TYPE_HEADER = 100000;
    private int BASE_ITEM_TYPE_FOOTER = 200000;

    /***
     * @param diffCallback 是一个对pagedlist的item数据变更的时候进行对比的listner
     */
    protected AbsPagedListAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback) {
        super(diffCallback);
    }
    /***
     * 添加header
     * @param view
     */
    public void addHeaderView(View view) {
        //判断给View对象是否还没有处在mHeaders数组里面
        if (mHeaders.indexOfValue(view) < 0) {
            mHeaders.put(BASE_ITEM_TYPE_HEADER++, view);
            notifyDataSetChanged();
        }
    }

    /***
     * 添加footer
     * @param view
     */
    public void addFooterView(View view) {
        //判断给View对象是否还没有处在mFooters数组里面
        if (mFooters.indexOfValue(view) < 0) {
            mFooters.put(BASE_ITEM_TYPE_FOOTER++, view);
            notifyDataSetChanged();
        }
    }

    /***
     * 移除header
     * @param view
     */
    public void removeHeaderView(View view) {
        int index = mHeaders.indexOfValue(view);
        if (index < 0) return;
        mHeaders.removeAt(index);
        notifyDataSetChanged();
    }

    /***
     * 移除footer
     * @param view
     */
    public void removeFooterView(View view) {
        int index = mFooters.indexOfValue(view);
        if (index < 0) return;
        mFooters.removeAt(index);
        notifyDataSetChanged();
    }

    /***
     * 获取header的个数
     * @return
     */
    public int getHeaderCount() {
        return mHeaders.size();
    }

    /***
     * 获取footer的个数
     * @return
     */
    public int getFooterCount() {
        return mFooters.size();
    }

    /***
     * 获取pagelist要现实的item的个数
     * @return
     */
    @Override
    public int getItemCount() {
        int itemCount = super.getItemCount();
        return itemCount + mHeaders.size() + mFooters.size();
    }

    /***
     * 获取pagelist内显示的数据的个数
     * @return
     */
    public int getOriginalItemCount() {
        return getItemCount() - mHeaders.size() - mFooters.size();
    }

    /***
     * 获取pagelist显示的item的类型
     * @param position
     * @return
     */
    @Override
    public int getItemViewType(int position) {
        if (isHeaderPosition(position)) {
            //返回该position对应的headerview的  viewType
            return mHeaders.keyAt(position);
        }

        if (isFooterPosition(position)) {
            //footer类型的，需要计算一下它的position实际大小
            position = position - getOriginalItemCount() - mHeaders.size();
            return mFooters.keyAt(position);
        }
        position = position - mHeaders.size();
        return getItemViewType2(position);
    }

    protected int getItemViewType2(int position) {
        return 0;
    }

    /***
     * 当前位置是不是footer
     * @param position
     * @return
     */
    private boolean isFooterPosition(int position) {
        return position >= getOriginalItemCount() + mHeaders.size();
    }

    /***
     * 当前位置是不是header位置
     * @param position
     * @return
     */
    private boolean isHeaderPosition(int position) {
        return position < mHeaders.size();
    }

    /***
     * pagelist的holder创建的类，包括footer和header的创建
     * @param parent
     * @param viewType
     * @return
     */
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mHeaders.indexOfKey(viewType) >= 0) {
            View view = mHeaders.get(viewType);
            return (VH) new RecyclerView.ViewHolder(view) {
            };
        }

        if (mFooters.indexOfKey(viewType) >= 0) {
            View view = mFooters.get(viewType);
            return (VH) new RecyclerView.ViewHolder(view) {
            };
        }
        return onCreateViewHolder2(parent, viewType);
    }

    /***
     * pagelist的显示内容的holder的具体实现，由子类自行处理
     * @param parent
     * @param viewType
     * @return
     */
    protected abstract VH onCreateViewHolder2(ViewGroup parent, int viewType);

    /***
     * pagelist的holder中的视图和数据的绑定 ，包括header和footer和具体的内容区域的绑定
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        if (isHeaderPosition(position) || isFooterPosition(position))
            return;
        //列表中正常类型的itemView的 position 咱们需要减去添加headerView的个数
        position = position - mHeaders.size();
        onBindViewHolder2(holder, position);
    }

    /***
     * pagelist的holder中的具体内容的绑定，具体的实现逻辑由子类自己实现
     * @param holder
     * @param position
     */
    protected abstract void onBindViewHolder2(VH holder, int position);

    /***
     *
     * @param holder
     */
    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        if (!isHeaderPosition(holder.getAdapterPosition()) && !isFooterPosition(holder.getAdapterPosition())) {
            this.onViewAttachedToWindow2((VH) holder);
        }
    }

    public void onViewAttachedToWindow2(VH holder) {

    }

    /***
     * 当view从当前的window中移除时候的回调方法
     * @param holder
     */
    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        if (!isHeaderPosition(holder.getAdapterPosition()) && !isFooterPosition(holder.getAdapterPosition())) {
            this.onViewDetachedFromWindow2((VH) holder);
        }
    }

    /***
     * 实现类自己的view移除的时候的处理逻辑
     * @param holder
     */
    public void onViewDetachedFromWindow2(VH holder) {

    }

    /***
     * 注册一个观察者对象，adapter内部数据变的监听
     * @param observer
     */
    @Override
    public void registerAdapterDataObserver(@NonNull RecyclerView.AdapterDataObserver observer) {
        super.registerAdapterDataObserver(new AdapterDataObserverProxy(observer));
    }

    /****
     * AdapterDataObserver：一个观察者对象，观察adapter内部数据变化
     */
    //如果我们先添加了headerView,而后网络数据回来了再更新到列表上
    //由于Paging在计算列表上item的位置时 并不会顾及我们有没有添加headerView，就会出现列表定位的问题
    //实际上 RecyclerView#setAdapter方法，它会给Adapter注册了一个AdapterDataObserver
    //咱么可以代理registerAdapterDataObserver()传递进来的observer。在各个方法的实现中，把headerView的个数算上，再中转出去即可
    private class AdapterDataObserverProxy extends RecyclerView.AdapterDataObserver {
        private RecyclerView.AdapterDataObserver mObserver;

        public AdapterDataObserverProxy(RecyclerView.AdapterDataObserver observer) {
            mObserver = observer;
        }

        public void onChanged() {
            mObserver.onChanged();
        }

        public void onItemRangeChanged(int positionStart, int itemCount) {
            mObserver.onItemRangeChanged(positionStart + mHeaders.size(), itemCount);
        }

        public void onItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
            mObserver.onItemRangeChanged(positionStart + mHeaders.size(), itemCount, payload);
        }

        public void onItemRangeInserted(int positionStart, int itemCount) {
            mObserver.onItemRangeInserted(positionStart + mHeaders.size(), itemCount);
        }

        public void onItemRangeRemoved(int positionStart, int itemCount) {
            mObserver.onItemRangeRemoved(positionStart + mHeaders.size(), itemCount);
        }

        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            mObserver.onItemRangeMoved(fromPosition + mHeaders.size(), toPosition + mHeaders.size(), itemCount);
        }


    }
}
