package com.mooc.libnetwork;

/***
 * 网络请求返回的信息对象
 * @param <T>
 */
public class ApiResponse<T> {
    //是否成功
    public boolean success;
    //状态码
    public int status;
    //网络请求的信息
    public String message;
    //请求体
    public T body;
}
