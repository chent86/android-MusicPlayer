package com.example.ct.musicplayer;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    MusicService ms;
    Handler mHandler;
    Runnable mRunnable;
    boolean has_started = false;
    private SimpleDateFormat time = new SimpleDateFormat("mm:ss");
    boolean start = false;
    ObjectAnimator animator;
    String path;
    Intent intent;
    boolean update=false;
    private ServiceConnection sc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ms = ((MusicService.MyBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };


    public static boolean isServiceRunning(Context context,String serviceName) {
        // 校验服务是否还存在
        ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(100);
        for (ActivityManager.RunningServiceInfo info : services) {
            // 得到所有正在运行的服务的名称
            String name = info.service.getClassName();
            System.out.println(info.service.getClassName());
            if (serviceName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //  rotateAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        // 类似的RotateAnimation不方便控制动画的暂停（停在某个角度）
        CircleImageView img = findViewById(R.id.musicImg);
        animator = ObjectAnimator.ofFloat(img, "rotation", 0f, 360f);
        animator.setDuration(8000);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(-1);

        intent = new Intent(this, MusicService.class);
        if(!isServiceRunning(this, "com.example.ct.musicplayer.MusicService")) {
            startService(intent);
            bindService(intent, sc, 0);
        }
        else{
            bindService(intent, sc, 0);
            update = false;
        }
        final SeekBar seekBar = findViewById(R.id.progress);
        final TextView end = findViewById(R.id.end);
        final TextView current = findViewById(R.id.start);
        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                mHandler.postDelayed(mRunnable, 1000);
                if (ms != null) {
                    if(!update && !ms.path.equals("")) {
                        if(ms.mediaPlayer.isPlaying() && !update) {
                            animator.start();
                            start = true;
                            has_started = true;
                            ImageButton ib = findViewById(R.id.play);
                            ib.setBackgroundResource(R.drawable.pause);
                        }
                        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                        mmr.setDataSource(ms.path);
                        String title =  mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                        String author = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                        byte[] d = mmr.getEmbeddedPicture();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(d, 0, d.length);
                        TextView title_text = findViewById(R.id.title);
                        TextView author_text = findViewById(R.id.author);
                        CircleImageView music_img = findViewById(R.id.musicImg);
                        title_text.setText(title);
                        author_text.setText(author);
                        music_img.setImageBitmap(bitmap);
                        update = true;
                    }
                    seekBar.setMax(ms.mediaPlayer.getDuration());
                    seekBar.setProgress(ms.mediaPlayer.getCurrentPosition());
                    current.setText(time.format(ms.mediaPlayer.getCurrentPosition()));
                    end.setText(time.format(ms.mediaPlayer.getDuration()));
                    if(ms.mediaPlayer.getCurrentPosition() >= ms.mediaPlayer.getDuration()) {  // 播放结束
                        seekBar.setProgress(0);
                        animator.end();
                        ImageButton ib = findViewById(R.id.play);
                        ib.setBackgroundResource(R.drawable.play);
                        current.setText("00:00");
                        start = false;
                        has_started = false;
                    }
                }
            }
        };
        mHandler.post(mRunnable);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    ms.mediaPlayer.seekTo(seekBar.getProgress());  // 如果没有fromUser判断，则会不断执行，造成卡顿
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    public void stop(View view) {    // 结束播放
        ImageButton ib = findViewById(R.id.play);
        ms.stop();
        SeekBar seekBar = findViewById(R.id.progress);
        seekBar.setProgress(0);
        TextView current = findViewById(R.id.start);
        current.setText("00:00");
        start = false;
        ib.setBackgroundResource(R.drawable.play);
        animator.end();
        has_started = false;
    }

    public void start(View view) {   // 开始，暂停
        ms.start();  // 由Service进行判断当前是否在播放
        ImageButton ib = findViewById(R.id.play);
        if (!start) {
            start = true;
            ib.setBackgroundResource(R.drawable.pause);
            if (has_started)  // 尝试使用进度条的位置判断歌曲是否已经开始，然而可能歌曲还没开始，用户拖动了进度条，此时不应该使用animator.resume(),所以还是选择使用本地变量来存储状态
                animator.resume();
            else
                animator.start();
        } else {
            start = false;
            ib.setBackgroundResource(R.drawable.play);
            animator.pause();
        }
        has_started = true;
    }

    public void back(View view) {   // 退出
        mHandler.removeCallbacks(mRunnable);
        unbindService(sc);
        try {
            MainActivity.this.finish();
            stopService(intent); //  停止Service
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 1);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if ("file".equalsIgnoreCase(uri.getScheme())){//使用第三方应用打开
                path = uri.getPath();
                return;
            }
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {//4.4以后
                path = getPath(this, uri);
            } else {//4.4以下下系统调用方法
                path = getRealPathFromURI(uri);
            }
            View t= null;
            stop(t);  // 停止播放
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(path);
            String title =  mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String author = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            byte[] d = mmr.getEmbeddedPicture();
            Bitmap bitmap = BitmapFactory.decodeByteArray(d, 0, d.length);
            TextView title_text = findViewById(R.id.title);
            TextView author_text = findViewById(R.id.author);
            CircleImageView music_img = findViewById(R.id.musicImg);
            title_text.setText(title);
            author_text.setText(author);
            music_img.setImageBitmap(bitmap);
            ms.change_source(path);
            has_started = false;
            ms.path = path;
        }
    }

    public String getRealPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if(null!=cursor&&cursor.moveToFirst()){;
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
            cursor.close();
        }
        return res;
    }

    @SuppressLint("NewApi")
    public String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public String getDataColumn(Context context, Uri uri, String selection,
                                String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

}