package com.example.serviecepractice;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by xiecy on 2018/01/18.
 */

public class DownloadTask extends AsyncTask<String,Integer,Integer> {

    public final static int SUCCESS=0;
    public final static int FAILED=1;
    public final static int PAUSED=2;
    public final static int CANCELED=3;

    private DownloadListener listener;
    private boolean isCanceled=false;
    private boolean isPaused=false;
    private int lastProgress;

    public DownloadTask(DownloadListener listener) {
        this.listener = listener;
    }

    @Override
    protected Integer doInBackground(String... params) {
        InputStream inputStream=null;
        RandomAccessFile savedFile=null;//文件内容访问类
        File file=null;
        try {
            long downloadLength=0;
            String downloadUrl= params[0];
            String fileName=downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            String directory= Environment.
                    getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            file =new File(directory+fileName);
            if(file.exists()){
                downloadLength=file.length();
            }
            long contentLength=getContentLength(downloadUrl);
            if (contentLength==0){
                return FAILED;
            }else if (contentLength==downloadLength){
                return SUCCESS;
            }
            OkHttpClient client=new OkHttpClient();
            Request request=new Request.Builder()
                    .addHeader("RANGE","bytes="+downloadLength+"-")
                    .url(downloadUrl).build();
            Response response=client.newCall(request).execute();
            if (response!=null){
                inputStream=response.body().byteStream();
                savedFile=new RandomAccessFile(file,"rw");
                savedFile.seek(downloadLength);//设置文件偏移量
                byte[] b=new byte[1024];
                int total=0;
                int len;
                //=-1时读取完毕
                while ((len=inputStream.read(b))!=-1){
                    if (isCanceled){
                        return CANCELED;
                    }else if (isPaused){
                        return PAUSED;
                    }else {
                        total+=len;
                        savedFile.write(b,0,len);
                        //计算下载百分比
                        int progress=(int)((total+downloadLength)*100/contentLength);
                        publishProgress(progress);
                    }
                }
                response.body().close();
                return SUCCESS;


            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if (inputStream!=null){
                    inputStream.close();
                }
                if (savedFile!=null){
                    savedFile.close();
                }
                if (isCanceled&&file!=null){
                    file.delete();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return FAILED;

    }

    @Override
    protected void onPostExecute(Integer status) {
        switch (status){
            case SUCCESS:
                listener.onSuccess();
                break;
            case FAILED:
                listener.onFailed();
                break;
            case CANCELED:
                listener.onCanceled();
                break;
            case PAUSED:
                listener.onPaused();
                break;
                default:
                    break;
        }
    }

    public void pauseDownload(){
        isPaused=true;
    }

    public void cancelDownload(){
        isCanceled=true;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress=values[0];
        if (progress>lastProgress){
            listener.onProgress(progress);
            lastProgress=progress;
        }
    }

    private long getContentLength(String downloadUrl) throws IOException{
        OkHttpClient client=new OkHttpClient();
        Request request=new Request.Builder().url(downloadUrl).build();
        Response response=client.newCall(request).execute();
        if (response!=null&&response.isSuccessful()){
            long contentLength=response.body().contentLength();
            response.close();
            return contentLength;
        }
        return 0;
    }


}
