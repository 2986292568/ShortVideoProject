package com.mooc.ppjoke.utils;

import android.content.res.AssetManager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.mooc.libcommon.global.AppGlobals;
import com.mooc.ppjoke.model.BottomBar;
import com.mooc.ppjoke.model.Destination;
import com.mooc.ppjoke.model.SofaTab;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class AppConfig {

     // 一个存储页面节点destination的数据集合
    private static HashMap<String, Destination> sDestConfig;
    // 底部导航对象
    private static BottomBar sBottomBar;
    // 沙发页对象
    private static SofaTab sSofaTab, sFindTabConfig;

    /***
     * 解析destination.json文件 初始化 sDestConfig
     * @return
     */
    public static HashMap<String, Destination> getDestConfig() {
        if (sDestConfig == null) {
            String content = parseFile("destination.json");
            sDestConfig = JSON.parseObject(content, new TypeReference<HashMap<String, Destination>>() {
            });
        }
        return sDestConfig;
    }

    /****
     * 解析 main_tabs_config.json文件 初始化 sBottomBar
     * @return
     */
    public static BottomBar getBottomBarConfig() {
        if (sBottomBar == null) {
            String content = parseFile("main_tabs_config.json");
            sBottomBar = JSON.parseObject(content, BottomBar.class);
        }
        return sBottomBar;
    }

    /***
     * 解析 sofa_tabs_config.json文件 初始化 sSofaTab
     * @return
     */
    public static SofaTab getSofaTabConfig() {
        if (sSofaTab == null) {
            String content = parseFile("sofa_tabs_config.json");
            sSofaTab = JSON.parseObject(content, SofaTab.class);
            Collections.sort(sSofaTab.tabs, new Comparator<SofaTab.Tabs>() {
                @Override
                public int compare(SofaTab.Tabs o1, SofaTab.Tabs o2) {
                    return o1.index < o2.index ? -1 : 1;
                }
            });
        }
        return sSofaTab;
    }
    /***
     * 解析 find_tabs_config.json文件 初始化 sFindTabConfig
     * @return
     */
    public static SofaTab getFindTabConfig() {
        if (sFindTabConfig == null) {
            String content = parseFile("find_tabs_config.json");
            sFindTabConfig = JSON.parseObject(content, SofaTab.class);
            Collections.sort(sFindTabConfig.tabs, new Comparator<SofaTab.Tabs>() {
                @Override
                public int compare(SofaTab.Tabs o1, SofaTab.Tabs o2) {
                    return o1.index < o2.index ? -1 : 1;
                }
            });
        }
        return sFindTabConfig;
    }

    /****
     * 解析文件
     * @param fileName  解析文件的文件名
     * @return 返回解析之后的字符串
     */
    private static String parseFile(String fileName) {
        AssetManager assets = AppGlobals.getApplication().getAssets();
        InputStream is = null;
        BufferedReader br = null;
        StringBuilder builder = new StringBuilder();
        try {
            is = assets.open(fileName);
            br = new BufferedReader(new InputStreamReader(is));
            String line = null;
            while ((line = br.readLine()) != null) {
                builder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (br != null) {
                    br.close();
                }
            } catch (Exception e) {

            }
        }

        return builder.toString();
    }
}
