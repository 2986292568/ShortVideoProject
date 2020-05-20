package com.mooc.libnetwork.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/***
 * 缓存管理类
 */
public class CacheManager {

    /***
     * 反序列,把二进制数据转换成java object对象
     */
    private static Object toObject(byte[] data) {
        ByteArrayInputStream bais = null;
        ObjectInputStream ois = null;
        try {
            bais = new ByteArrayInputStream(data);
            ois = new ObjectInputStream(bais);
            return ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bais != null) {
                    bais.close();
                }
                if (ois != null) {
                    ois.close();
                }
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }
        return null;
    }

    /***
     * 序列化存储数据需要转换成二进制
     */
    private static <T> byte[] toByteArray(T body) {
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        try {
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(body);
            oos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.close();
                }
                if (oos != null) {
                    oos.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new byte[0];
    }

    /***
     * 删除数据缓存  在数据库中删除
     * @param key  删除的key
     * @param body 删除的具体数据
     * @param <T>
     */
    public static <T> void delete(String key, T body) {
        Cache cache = new Cache();
        cache.key = key;
        cache.data = toByteArray(body);
        CacheDatabase.get().getCache().delete(cache);
    }

    /***
     * 保存数据
     * @param key
     * @param body
     * @param <T>
     */
    public static <T> void save(String key, T body) {
        Cache cache = new Cache();
        cache.key = key;
        cache.data = toByteArray(body);
        CacheDatabase.get().getCache().save(cache);
    }

    /***
     * 获取缓存数据
     * @param key 获取缓存数据的key
     * @return
     */
    public static Object getCache(String key) {
        Cache cache = CacheDatabase.get().getCache().getCache(key);
        if (cache != null && cache.data != null) {
            return toObject(cache.data);
        }
        return null;
    }
}
