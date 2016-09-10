package com.example.admin.recodedemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener, View.OnClickListener {
    private ImageButton mButton;
    private TextView mText;
    private MediaRecorder mediaRecorder;
    private Timer mTimer;
    private TimerTask mTimerTask;
    private File mFile;
    private String fileName;
    private final static int REQUEST_CODE_ASK_CALL_PHONE = 123;
    private TextView mTime;
    private MediaPlayer mPlayer;
    private Button soundButton;
    private TextView sountTime;
    private TimerTask mTimerTask2;
    private Boolean isPause = false;
    private int soundLength;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTimer = new Timer(true);
        if (Build.VERSION.SDK_INT >= 23) {
            int checkCallPhonePermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO);
            if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_ASK_CALL_PHONE);
                return;
            } else {
                init();
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mText.setText("正在录制");
                mButton.setBackgroundResource(R.mipmap.ht_unparse);
                fileName = mFile.getPath() + File.separator + createVoiceFileName();
                mediaRecorder = new MediaRecorder();
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                // 设置录制的声音的输出格式（必须在设置声音编码格式之前设置）
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
                // 设置声音编码的格式
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mediaRecorder.setOutputFile(fileName);
                try {
                    mediaRecorder.prepare();
                    mediaRecorder.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                final Timestamp start_time = new Timestamp(System.currentTimeMillis());
                final Handler mHandle = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        switch (msg.what) {
                            case 1:
                                Timestamp ts = new Timestamp(System.currentTimeMillis());
                                int duration = (int) ((ts.getTime() - start_time.getTime()) / 1000);
                                soundLength=duration;
                                mTime.setText(duration + "秒");
                                break;
                        }
                        super.handleMessage(msg);
                    }
                };
                mTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        Message message = new Message();
                        message.what = 1;
                        mHandle.sendMessage(message);
                    }
                };
                mTimer.schedule(mTimerTask, 1000, 1000);


                break;
            case MotionEvent.ACTION_UP:
                mTimerTask.cancel();
                mText.setText("录制结束");
                soundButton.setVisibility(View.VISIBLE);
                sountTime.setVisibility(View.VISIBLE);
                sountTime.setText(mTime.getText());
                mTime.setText(0 + "秒");
                mButton.setBackgroundResource(R.mipmap.ht_lued);
                mediaRecorder.stop();
                // 释放资源
                mediaRecorder.release();
                mediaRecorder = null;
                break;
        }
        return false;
    }

    private void init() {
        mButton = (ImageButton) findViewById(R.id.mButton);
        mText = (TextView) findViewById(R.id.mText);
        mTime = (TextView) findViewById(R.id.mTime);
        soundButton = (Button) findViewById(R.id.soundButton);
        sountTime = (TextView) findViewById(R.id.sountTime);
        mFile = new File(Environment.getExternalStorageDirectory(), "录音文件");
        if (!mFile.exists()) {
            mFile.mkdirs();
        }
        soundButton.setOnClickListener(this);
        mButton.setOnTouchListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_CALL_PHONE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    init();
                } else {
                    Toast.makeText(MainActivity.this, "请给予录音权限", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onDestroy() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            // 释放资源
            mediaRecorder.release();
            mediaRecorder = null;
        }
        super.onDestroy();
    }

    /*
        * 获取不同的文件名
        * */
    private String createVoiceFileName() {
        String fileName = "";
        Date date = new Date(System.currentTimeMillis());  //系统当前时间
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        fileName = getMyUUID() + "-" + dateFormat.format(date) + ".amr";
        return fileName;
    }

    /*
    * 获取唯一标识符
    * */
    private String getMyUUID() {
        final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        final String tmDevice, tmSerial, tmPhone, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
        String uniqueId = deviceUuid.toString();
        Log.d("debug", "uuid=" + uniqueId);
        return uniqueId;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.soundButton:
                if (mPlayer==null){
                    mPlayer = new MediaPlayer();
                    try {
                        mPlayer.setDataSource(fileName);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                final Timestamp tse=new Timestamp(System.currentTimeMillis());

                final Handler handler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        switch (msg.what) {
                            case 1:
                                Timestamp tss = new Timestamp(System.currentTimeMillis());
                                if(mPlayer!=null){
                                    sountTime.setText((int)((tss.getTime()-tse.getTime())/1000) + "秒");
                                }
                                break;
                        }
                        super.handleMessage(msg);
                    }
                };

                mTimerTask2 = new TimerTask() {
                    @Override
                    public void run() {
                        Message message = new Message();
                        message.what = 1;
                        handler.sendMessage(message);
                    }
                };
                mTimer.schedule(mTimerTask2, 1000, 1000);
                //播放结束
                mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if (mPlayer != null) {
                            soundButton.setText("播放结束");
                            mPlayer = null;
                            mTimerTask2.cancel();
                        }
                    }
                });
                if (!mPlayer.isPlaying() && !isPause) {
                    try {
                        mPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mPlayer.start();
                    soundButton.setText("正在播放");
                } else if (!isPause) {
                    mPlayer.pause();
                    soundButton.setText("暂停");
                    isPause = true;
                } else if (isPause) {
                    mPlayer.start();
                    isPause = false;
                    soundButton.setText("正在播放");
                }
                break;
        }
    }
}
