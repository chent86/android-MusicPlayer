package com.example.ct.musicplayer;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import rx.Observable;
import rx.Subscriber;

public class MusicService extends Service{
    MediaPlayer mediaPlayer;
    public String path = "";
    public final IBinder binder = new MyBinder();
    public class MyBinder extends Binder {
        protected boolean onTransact(int code, Parcel data, Parcel reply,
                                     int flags) throws RemoteException {
            if(code == 0) {
                reply.writeString(MusicService.this.path);
            } else if(code == 1) {
                if(MusicService.this.mediaPlayer.isPlaying()) {
                    reply.writeInt(1);
                } else {
                    reply.writeInt(0);
                }
            } else if(code == 2) {
                reply.writeInt(MusicService.this.mediaPlayer.getDuration());
            } else if(code == 3) {
                reply.writeInt(MusicService.this.mediaPlayer.getCurrentPosition());
            } else if(code == 4) {
                MusicService.this.mediaPlayer.seekTo(data.readInt());
            } else if(code == 5) {
                MusicService.this.start();
            } else if(code == 6) {
                String s = data.readString();  // 只能读一次
                MusicService.this.change_source(s);
                MusicService.this.path = s;
            } else if(code == 7) {
                MusicService.this.mediaPlayer.seekTo(0);
                MusicService.this.mediaPlayer.pause();
            }
            return super.onTransact(code, data, reply, flags);
        }
    }
    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        try{
            path = CopyMusic("山高水长.mp3");
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();  // 没有prepare会报wrong state错误
//            AssetManager am = this.getAssets();
//            AssetFileDescriptor afd = am.openFd("山高水长.mp3");  // 直接读取assets中的文件
//            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),afd.getLength());
//            mediaPlayer.prepare();

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
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
        File dir = new File("data/data/com.example.ct.musicplayer/Song");
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


