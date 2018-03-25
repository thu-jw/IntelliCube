package com.jw.activities;

import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jw.AnimCubeLib.AnimCube;


public class GameActivity extends AppCompatActivity implements AnimCube.OnCubeModelUpdatedListener, AnimCube.OnCubeAnimationFinishedListener{

    public static final String ANIM_CUBE_SAVE_STATE_BUNDLE_ID = "animCube";
    private static final String TAG = "AnimCubeActivity";
    private boolean anim_available = false;
    private boolean isRestorable = false;
    private AnimCube animCube;
    private Bundle state;

    private TextView message;
    private TextView timer_view;
    private Toolbar toolbar;

    private Timer timer = new Timer(this, 1);
    public boolean isPlaying = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
        initView();
        animCube = (AnimCube) findViewById(R.id.animcube);
        animCube.setOnCubeModelUpdatedListener(this);
        animCube.setOnAnimationFinishedListener(this);
        animCube.setParent(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.game_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.start:
                anim_available = true;
                break;
            case R.id.stop:
                anim_available = false;
                animCube.stopAnimation();
                break;
            case R.id.reset:
                message.setText("");
                animCube.resetToInitialState();
                break;
            case R.id.scramble:
                setMessage(animCube.scramble());
                break;
            case R.id.save_state:
                state = animCube.saveState();
                isRestorable = true;
                break;
            case R.id.restore_state:
                if (!isRestorable){
                    Toast.makeText(GameActivity.this,this.getResources().getString(R.string.no_valid_state), Toast.LENGTH_LONG).show();
                }
                else {
                    animCube.restoreState(state);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState ");
        outState.putBundle(ANIM_CUBE_SAVE_STATE_BUNDLE_ID, animCube.saveState());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState");
        animCube.restoreState(savedInstanceState.getBundle(ANIM_CUBE_SAVE_STATE_BUNDLE_ID));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        animCube.cleanUpResources();
        Log.d(TAG, "onDestroy: finish");
    }

    @Override
    public void onCubeModelUpdate(int[][] newCubeModel) {
        Log.d(TAG, "Cube model updated!");
        printCubeModel(newCubeModel);
    }

    void printCubeModel(int[][] cube) {
        Log.d(TAG, "Cube model:");
        StringBuilder stringBuilder = new StringBuilder("");
        for (int i = 0; i < cube.length; i++) {
            stringBuilder.append("\n");
            stringBuilder.append(i);
            stringBuilder.append(":\n");
            for (int j = 0; j < cube[i].length; j++) {
                stringBuilder.append(" ");
                stringBuilder.append(cube[i][j]);
                if ((j + 1) % 3 == 0) {
                    stringBuilder.append("\n");
                } else {
                    stringBuilder.append(", ");
                }
            }
        }
        Log.d(TAG, stringBuilder.toString());
    }

    @Override
    public void onAnimationFinished() {
        Log.d(TAG, "Cube AnimationFinished!");
    }


    private void initData(){
        AnimCube.isRotatable = true;
        anim_available = true;
        return;
    }

    private void initView() {
        setContentView(R.layout.activity_game);
        message = (TextView) findViewById(R.id.scramble);
        timer_view = (TextView)findViewById(R.id.timer);
        message.setMovementMethod(ScrollingMovementMethod.getInstance());
        toolbar = findViewById(R.id.cubeToolBar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }


    private void setMessage(String m){
        if (!anim_available)
            return;
        message.append(m);
        int offset=message.getLineCount()*message.getLineHeight();
        if(offset>message.getHeight()){
            message.scrollTo(0,offset-message.getHeight());
        }
    }

    public void setTimer(String t){
        if (!isPlaying)
            return;
        timer_view.setText(t);
    }

    public void startTiming(){
        isPlaying = true;
        Log.e("tag", "start");
        timer.count();
        timer.timeStart = System.currentTimeMillis();
    }

    public void endTiming(){
        ShowFinishedSolveDialog();
        Log.e("tag", "endTiming");
        isPlaying = false;
    }

    public void ShowFinishedSolveDialog() {
        ButtonDialogFragment buttonDialogFragment = new ButtonDialogFragment();
        buttonDialogFragment.show("复原成功！", "用时: " + timer_view.getText(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
//                Toast.makeText(GameActivity.this, "点击了确定 " + which, Toast.LENGTH_SHORT).show();
                timer_view.setText("");
                message.setText("");
            }
        }, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
//                Toast.makeText(GameActivity.this, "点击了取消 " + which, Toast.LENGTH_SHORT).show();
                timer_view.setText("");
                message.setText("");
            }
        }, getFragmentManager());
    }
}
