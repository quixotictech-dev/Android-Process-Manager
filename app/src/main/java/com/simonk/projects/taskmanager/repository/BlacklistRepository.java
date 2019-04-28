package com.simonk.projects.taskmanager.repository;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.room.Database;

import com.simonk.projects.taskmanager.database.DatabaseManager;
import com.simonk.projects.taskmanager.database.entity.BlacklistEntity;
import com.simonk.projects.taskmanager.entity.AppInfo;
import com.simonk.projects.taskmanager.entity.ProcessInfo;

import java.util.ArrayList;
import java.util.List;

public class BlacklistRepository {

    public List<AppInfo> getAllInstalledApplicationsInfo(Context context, boolean notSystem) {
        PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> installedApplications = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        List<AppInfo> appInfoList = new ArrayList<>();
        for (ApplicationInfo info : installedApplications) {
            AppInfo appInfo = new AppInfo();
            appInfo.setText(packageManager.getApplicationLabel(info).toString());
            appInfo.setImage(packageManager.getApplicationIcon(info));
            appInfo.setPpackage(info.processName);
            appInfo.setEnabled(info.enabled);
            appInfo.setInBlacklist(false);
            boolean isSystem = (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            if (isSystem && notSystem) {
                continue;
            }
            appInfo.setIsSystem(isSystem);

            if (appInfo.getPpackage().contains(context.getPackageName())) {
                continue;
            }

            appInfoList.add(appInfo);
        }

        return appInfoList;
    }

    public LiveData<List<AppInfo>> getAllBlacklistApplicationInfo(Context context) {
        LiveData<List<BlacklistEntity>> blacklistEntitiesData = DatabaseManager.loadBlacklistEntities(context);
        return Transformations.map(blacklistEntitiesData, blacklistEntities -> {
            List<AppInfo> appInfoList = new ArrayList<>();
            PackageManager packageManager = context.getPackageManager();
            for (BlacklistEntity entity : blacklistEntities) {
                ApplicationInfo info = null;
                try {
                    info = packageManager.getApplicationInfo(entity.apppackage, PackageManager.GET_META_DATA);
                } catch (PackageManager.NameNotFoundException exception) {
                    continue;
                }
                AppInfo appInfo = new AppInfo();
                appInfo.setText(packageManager.getApplicationLabel(info).toString());
                appInfo.setImage(packageManager.getApplicationIcon(info));
                appInfo.setPpackage(info.processName);
                appInfo.setEnabled(info.enabled);
                appInfo.setIsSystem((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
                appInfo.setInBlacklist(true);

                appInfoList.add(appInfo);
            }

            return appInfoList;
        });
    }

    public void insertAppInBlacklist(Context context, AppInfo appInfo) {
        BlacklistEntity blacklistEntity = new BlacklistEntity();
        blacklistEntity.apppackage = appInfo.getPpackage();
        DatabaseManager.insertBlacklistEntity(context, blacklistEntity);
    }

    public void deleteAppFromBlackList(Context context, AppInfo appInfo) {
        BlacklistEntity blacklistEntity = new BlacklistEntity();
        blacklistEntity.apppackage = appInfo.getPpackage();
        DatabaseManager.deleteBlacklistEntityByPackage(context, blacklistEntity.apppackage);
    }
}
