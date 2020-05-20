package com.mooc.libnetwork;

/**
 * 网络请求结果的回调
 *
 */
public abstract class JsonCallback<T> {
    /***
     * 网络请求成功的回调
     * @param response
     */
    public void onSuccess(ApiResponse<T> response) {

    }

    /***
     * 网络请求出错的回调
     * @param response
     */
    public void onError(ApiResponse<T> response) {

    }

    /***
     * 缓存成功的回调
     * @param response
     */
    public void onCacheSuccess(ApiResponse<T> response) {

    }
}
