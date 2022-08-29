package com.example.accball;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainActivity extends AppCompatActivity implements SensorEventListener, SurfaceHolder.Callback{

    SensorManager mSensorManager;
    Sensor mAccSensor;

    SurfaceHolder mHolder;
    int mSurfaceWidth;  //サーフェスビューの幅
    int mSurfaceHeight; //サーフェスビューの高さ

    static final float RADIUS = 150.0f; //ボールを描画する時の半径を表す定数
    static final int DIA = (int)RADIUS * 2; //ボールの直径を表す定数
    static final float COEF = 1000.0f; //ボールの移動量を調整するための係数

    float mBallX; //ボールの現在のX軸座標（最初は画面中央）
    float mBallY; //ボールの現在のY軸座標（最初は画面中央）
    float mVX; //ボールのX軸方向への加速度
    float mVY; //ボールのY軸方向への加速度
    long mT0; //前回センサーから加速度を取得した値

    Bitmap mBallBitmap; //ボールの画像

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //スマホのスクリーンを縦表示に固定
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //加速度センサーの取得
        mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        mHolder = surfaceView.getHolder();
        mHolder.addCallback(this);//サーフェスビューが変更・破棄された時のイベントリスナーを登録

        //サーフェスビューを透明にする
        mHolder.setFormat(PixelFormat.TRANSLUCENT);
        surfaceView.setZOrderOnTop(true);

        //ボールの画像を用意する
        Bitmap ball = BitmapFactory.decodeResource(getResources(), R.drawable.ball);
        mBallBitmap = Bitmap.createScaledBitmap(ball, DIA, DIA, false);
    }

    //加速度センサーの値に変化があった時に呼ばれる
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){

            float X = -sensorEvent.values[0]; //X軸方向のボールの加速度
            float Y = sensorEvent.values[1]; //Y軸方向のボールの加速度

            //時間tを求める
            if(mT0 == 0){
                mT0 = sensorEvent.timestamp; //1回目の加速度の取得（センサーの値を受け取った時間）
                return;
            }
            float t = sensorEvent.timestamp - mT0; //経過時間 = 現在の時間-前回の時間
            mT0 = sensorEvent.timestamp;
            t = t / 1000000000.0f; //ナノ秒(ns)を秒(s)に単位変換

            //移動距離を求める(d = Vot + 1/2at^2)
            float dX = (mVX * t) + (X * t * t / 2.0f); //X軸方向のボールの移動距離
            float dY = (mVY * t) + (Y * t * t / 2.0f); //Y軸方向のボールの移動距離

            //移動距離から、現在のボールの位置を更新
            mBallX = mBallX + dX * COEF;
            mBallY =mBallY + dY * COEF;

            //現在のボールの移動速度を更新（等加速度運動：V =　Vo + at）
            mVX = mVX + X * t;
            mVY = mVY + Y * t;

            //ボールが画面外に出ないようにする処理
            //X軸方向
            if(mBallX - RADIUS < 0 && mVX < 0){ //ボールの端で跳ね返る挙動
                mVX = -mVX / 3.0f; //速度反転により壁で跳ね返る挙動 + 減速する挙動
                mBallX = RADIUS;
            }else if(mBallX + RADIUS > mSurfaceWidth && mVX > 0){
                mVX = -mVX / 3.0f;
                mBallX = mSurfaceWidth - RADIUS;
            }
            //Y軸方向
            if(mBallY - RADIUS < 0 && mVY < 0){ //ボールの端で跳ね返る挙動
                mVY = -mVY / 3.0f; //速度反転により壁で跳ね返る挙動 + 減速する挙動
                mBallY = RADIUS;
            }else if(mBallY + RADIUS > mSurfaceHeight && mVY > 0){
                mVY = -mVY / 3.0f;
                mBallY = mSurfaceHeight - RADIUS;
            }

            //加速度から算出したボールの現在位置で、ボールをキャンバスに描画し直す
            drawCanvas();
        }


    }

    private void drawCanvas() {
        //画面にボールを表示する処理
        Canvas c = mHolder.lockCanvas(); //キャンバスの取得
        c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); //前回の描画をクリアする
        Paint paint = new Paint();
        c.drawBitmap(mBallBitmap, mBallX-RADIUS, mBallY -RADIUS, paint);
        //画面への反映とサーフェスのアンロックを行う
        mHolder.unlockCanvasAndPost(c);
    }


    //加速度センサーの精度が変更された時に呼ばれる
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    //画面が表示された時に呼ばれるメソッド
    @Override
    protected void onResume() {
        super.onResume();

    }

    //画面が閉じられた時に呼ばれるメソッド
    @Override
    protected void onPause() {
        super.onPause();

    }

    //サーフェスが作成された時に呼ばれる
    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        //(自身, 監視を行いたいセンサー, センサーの更新頻度)
        mSensorManager.registerListener(this, mAccSensor, SensorManager.SENSOR_DELAY_GAME);
    }
    //サーフェスに変更があった時に呼ばれる
    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        mSurfaceWidth = i1;
        mSurfaceHeight = i2;

    }

    //サーフェスが削除された時に呼ばれる
    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        mSensorManager.unregisterListener(this);
    }
}