package com.example.serviecepractice;

/**
 * Created by xiecy on 2018/01/18.
 */

public interface DownloadListener {

    void onProgress(int progress);

    void onSuccess();

    void onFailed();

    void onPaused();

    void onCanceled();
}
