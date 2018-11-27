package com.example.ct.musicplayer;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MusicService extends Service{
    MediaPlayer mediaPlayer;
    public final IBinder binder = new MyBinder();
    public class MyBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        mediaPlayer = new MediaPlayer();
        try{
//            mediaPlayer.setDataSource(CopyMusic("山高水长.mp3"));
            AssetManager am = this.getAssets();
            AssetFileDescriptor afd = am.openFd("山高水长.mp3");  // 直接读取assets中的文件
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),afd.getLength());
            mediaPlayer.prepare();

        } catch(IOException e) {
            e.printStackTrace();
        }
        return binder;  // 需要返回binder
    }

    public void start() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            try {
                mediaPlayer.prepare();
                mediaPlayer.seekTo(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void change_source(String path) {
        try{
            mediaPlayer.reset();             // 如果不reset会出错
            mediaPlayer.setDataSource(path); // 需要获得权限，manifest + 手动给权限
            mediaPlayer.prepare();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public String  CopyMusic(String MusicName) throws IOException {  // 将assets目录下的文件拷贝到SD卡中
        File dir = new File("data/data/com.example.ct.musicplayer/"+MusicName);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdir();
        }
        File file = new File(dir, MusicName);
        InputStream inputStream = null;
        OutputStream outputStream =null;
        if (!file.exists()) {
            try {
                file.createNewFile();
                inputStream = this.getClass().getClassLoader().getResourceAsStream("assets/" + MusicName);
                outputStream = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int len ;
                while ((len = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer,0,len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        }
        return file.getPath();
    }
}


