package com.mooc.ppjoke.utils;

import android.content.ComponentName;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.ActivityNavigator;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.NavGraphNavigator;
import androidx.navigation.NavigatorProvider;
import androidx.navigation.fragment.FragmentNavigator;

import com.mooc.libcommon.global.AppGlobals;
import com.mooc.ppjoke.navigator.FixFragmentNavigator;
import com.mooc.ppjoke.model.Destination;

import java.util.HashMap;
import java.util.Iterator;
//已完成
/***
 *
 *        (实现导航的控制操作)                         NavController  -> 进行具体的导航到指定的destination的操作
 *
 *
 *
 *
 *                                                  NavigatorProvider ->存储的是一系列要导航的destination对应的navigator
 *
 *                                                              ||
 *        (他们相当于导航的数据源)                                 ||  二者之间是一一对应的
 *                                                              ||
 *
 *                                                   NavGraph -> 存储的是一系列的要导航的destination集合
 *
 *
 *
 *
 *
 *       (具体的导航的处理机制的实现)                  NavGraphNavigator   获取第一个要导航到的界面
 *

 */
public class NavGraphBuilder {
    public static void build(FragmentActivity activity, FragmentManager childFragmentManager, NavController controller, int containerId) {
        //是一个存储 Navigator的集合
        NavigatorProvider provider = controller.getNavigatorProvider();
        //NavGraphNavigator也是页面路由导航器的一种，只不过他比较特殊。
        //它只为默认的展示页提供导航服务,但真正的跳转还是交给对应的navigator来完成的
        NavGraph navGraph = new NavGraph(new NavGraphNavigator(provider));
        //FragmentNavigator fragmentNavigator = provider.getNavigator(FragmentNavigator.class);
        //fragment的导航此处使用我们定制的FixFragmentNavigator，底部Tab切换时 使用hide()/show(),而不是replace()
        FixFragmentNavigator fragmentNavigator = new FixFragmentNavigator(activity, childFragmentManager, containerId);
        provider.addNavigator(fragmentNavigator);
        //getNavigator()检索已经注册的 navigator
        ActivityNavigator activityNavigator = provider.getNavigator(ActivityNavigator.class);

        HashMap<String, Destination> destConfig = AppConfig.getDestConfig();
        Iterator<Destination> iterator = destConfig.values().iterator();
        //解析destination
        while (iterator.hasNext()) {
            Destination node = iterator.next();
            if (node.isFragment) {
                FixFragmentNavigator.Destination destination = fragmentNavigator.createDestination();
                destination.setId(node.id);
                destination.setClassName(node.className);
                destination.addDeepLink(node.pageUrl);
                navGraph.addDestination(destination);
            } else {
                ActivityNavigator.Destination destination = activityNavigator.createDestination();
                destination.setId(node.id);
                destination.setComponentName(new ComponentName(AppGlobals.getApplication().getPackageName(), node.className));
                destination.addDeepLink(node.pageUrl);
                navGraph.addDestination(destination);
            }

            //给APP页面导航结果图 设置一个默认的展示页的id
            if (node.asStarter) {
                navGraph.setStartDestination(node.id);
            }
        }
        //将 NavGraph和 NavController相互关联
        controller.setGraph(navGraph);
    }
}
