package com.arvingin.filezone;

/**
 * 请求权限结果反馈接口
 */
public interface RequestPermissionResultListener {
    /**
     * @param result true: 表示已授权
     */
    void onResult(boolean result);
}
