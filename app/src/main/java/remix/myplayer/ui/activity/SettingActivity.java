package remix.myplayer.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.umeng.analytics.MobclickAgent;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.bmob.v3.update.BmobUpdateAgent;
import remix.myplayer.R;
import remix.myplayer.db.DBOpenHelper;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.dialog.ColorChooseDialog;
import remix.myplayer.ui.dialog.FolderChooserDialog;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DiskCache;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;

/**
 * @ClassName SettingActivity
 * @Description 设置界面
 * @Author Xiaoborui
 * @Date 2016/8/23 13:51
 */
public class SettingActivity extends ToolbarActivity implements FolderChooserDialog.FolderCallback{
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.setting_color_src)
    ImageView mColorSrc;
    @BindView(R.id.setting_lrc_path)
    TextView mLrcPath;
    @BindView(R.id.setting_clear_text)
    TextView mCache;
    //是否需要刷新
    private boolean mNeedRefresh = false;
    //是否从主题颜色选择对话框返回
    private boolean mFromColorChoose = false;
    //缓存大小
    private long mCacheSize = 0;
    private final int RECREATE = 100;
    private final int CACHESIZE = 101;
    private final int CLEARFINISH = 102;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == RECREATE)
                recreate();
            if(msg.what == CACHESIZE){
                mCache.setText(getString(R.string.cache_szie,1.0 * mCacheSize / 1024 / 1024));
            }
            if(msg.what == CLEARFINISH){
                ToastUtil.show(SettingActivity.this,"清除成功");
                mCache.setText("0MB");
                mLrcPath.setText(R.string.default_lrc_path);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_setting);
        ButterKnife.bind(this);
        setUpToolbar(mToolbar,"设置");

        //读取重启aitivity之前的数据
        if(savedInstanceState != null){
            mNeedRefresh = savedInstanceState.getBoolean("needRefresh");
            mFromColorChoose = savedInstanceState.getBoolean("fromColorChoose");
        }

        if(!SPUtil.getValue(this,"Setting","LrcPath","").equals("")) {
            mLrcPath.setText(getString(R.string.lrc_tip,SPUtil.getValue(this,"Setting","LrcPath","")));
        }
        //初始化箭头颜色
        final int arrowColor = ThemeStore.getAccentColor();
        ((GradientDrawable)mColorSrc.getDrawable()).setColor(arrowColor);
        ButterKnife.apply( new ImageView[]{findView(R.id.setting_eq_arrow),findView(R.id.setting_feedback_arrow),
                findView(R.id.setting_about_arrow),findView(R.id.setting_update_arrow)},
                new ButterKnife.Action<ImageView>(){
                    @Override
                    public void apply(@NonNull ImageView view, int index) {
                        Theme.TintDrawable(view,view.getBackground(),arrowColor);
                    }
        });

        //分根线颜色
        ButterKnife.apply(new View[]{findView(R.id.setting_divider_1),findView(R.id.setting_divider_2),findView(R.id.setting_divider_3)},
                new ButterKnife.Action<View>() {
                    @Override
                    public void apply(@NonNull View view, int index) {
                        view.setBackgroundColor(ThemeStore.getDividerColor());
                    }
                });

        //初始化点击效果
        final TypedArray ta = getTheme().obtainStyledAttributes(new int[]{R.attr.background_common});
        try {
            ButterKnife.apply(new View[]{findView(R.id.setting_filter_container),findView(R.id.setting_lrc_container),findView(R.id.setting_color_container),
                            findView(R.id.setting_notify_container),findView(R.id.setting_eq_container),findView(R.id.setting_feedback_container),
                            findView(R.id.setting_about_container),findView(R.id.setting_update_container),findView(R.id.setting_clear_container)},
                    new ButterKnife.Action<View>() {
                        @Override
                        public void apply(@NonNull View view, int index) {
                            if(ta != null)
                                view.setBackground(ta.getDrawable(0));
//                            Drawable defaultDrawable = Theme.getShape(GradientDrawable.RECTANGLE, Color.TRANSPARENT);
//                            Drawable selectDrawable = Theme.getShape(GradientDrawable.RECTANGLE,ThemeStore.getSelectColor());
//                            view.setBackground(Theme.getPressDrawable(defaultDrawable,selectDrawable,ThemeStore.getRippleColor(),null,selectDrawable));
                        }
                    });
        } finally {
            ta.recycle();
        }

        //计算缓存大小
        new Thread(){
            @Override
            public void run() {
                mCacheSize = 0;
                mCacheSize += CommonUtil.getFolderSize(DiskCache.getDiskCacheDir(SettingActivity.this,"lrc"));
                mCacheSize += CommonUtil.getFolderSize(DiskCache.getDiskCacheDir(SettingActivity.this,"thumbnail"));
                mCacheSize += CommonUtil.getFolderSize(getCacheDir());
                mCacheSize += CommonUtil.getFolderSize(getFilesDir());
                mHandler.sendEmptyMessage(CACHESIZE);
            }
        }.start();
    }

    public void onResume() {
        MobclickAgent.onPageStart(SettingActivity.class.getSimpleName());
        super.onResume();
    }
    public void onPause() {
        MobclickAgent.onPageEnd(SettingActivity.class.getSimpleName());
        super.onPause();
    }


    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("needRefresh",mNeedRefresh);
        setResult(Activity.RESULT_OK,intent);
        finish();
    }

    @Override
    protected void onClickNavigation() {
        onBackPressed();
    }

    @Override
    public void onFolderSelection(@NonNull FolderChooserDialog dialog, @NonNull File folder) {
        boolean success = SPUtil.putValue(this,"Setting","LrcPath",folder.getAbsolutePath());
        ToastUtil.show(this, success ? R.string.setting_success : R.string.setting_error, Toast.LENGTH_SHORT);
        mLrcPath.setText(getString(R.string.lrc_tip,SPUtil.getValue(this,"Setting","LrcPath","")));
    }

    @OnClick ({R.id.setting_filter_container,R.id.setting_color_container,R.id.setting_notify_container,
            R.id.setting_feedback_container,R.id.setting_about_container, R.id.setting_update_container,
            R.id.setting_eq_container,R.id.setting_lrc_container,R.id.setting_clear_container})
    public void onClick(View v){
        switch (v.getId()){
            //文件过滤
            case R.id.setting_filter_container:
                startActivity(new Intent(SettingActivity.this,ScanActivity.class));
                break;
            //歌词扫描路径
            case R.id.setting_lrc_container:
                new FolderChooserDialog.Builder(this)
                        .chooseButton(R.string.choose_folder)
                        .allowNewFolder(true,R.string.new_folder)
                        .show();
                break;
            //选择主色调
            case R.id.setting_color_container:
                startActivityForResult(new Intent(SettingActivity.this, ColorChooseDialog.class),0);
                break;
            //通知栏底色
            case R.id.setting_notify_container:
                try {
                    MobclickAgent.onEvent(this,"NotifyColor");
                    new MaterialDialog.Builder(this)
                            .title("通知栏底色")
                            .titleColorAttr(R.attr.text_color_primary)
                            .positiveText("选择")
                            .positiveColorAttr(R.attr.text_color_primary)
                            .buttonRippleColorAttr(R.attr.ripple_color)
                            .items(new String[]{getString(R.string.use_system_color),getString(R.string.use_black_color)})
                            .itemsCallbackSingleChoice(SPUtil.getValue(SettingActivity.this,"Setting","IsSystemColor",true) ? 0 : 1,
                                    new MaterialDialog.ListCallbackSingleChoice() {
                                @Override
                                public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                    SPUtil.putValue(SettingActivity.this,"Setting","IsSystemColor",which == 0);
                                    sendBroadcast(new Intent(Constants.NOTIFY));
                                    return true;
                                }
                            })
                            .backgroundColorAttr(R.attr.background_color_3)
                            .itemsColorAttr(R.attr.text_color_primary)
                            .show();
                } catch (Exception e){
                    e.printStackTrace();
                }
                break;
            //音效设置
            case R.id.setting_eq_container:
                MobclickAgent.onEvent(this,"EQ");
                Intent audioEffectIntent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                audioEffectIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, MusicService.getMediaPlayer().getAudioSessionId());
                if(CommonUtil.isIntentAvailable(this,audioEffectIntent)){
                    startActivityForResult(audioEffectIntent, 0);
                } else {
                    startActivity(new Intent(this,EQActivity.class));
                }
                break;
            //意见与反馈
            case R.id.setting_feedback_container:
                startActivity(new Intent(SettingActivity.this,FeedBakActivity.class));
                break;
            //关于我们
            case R.id.setting_about_container:
                startActivity(new Intent(SettingActivity.this,AboutActivity.class));
                break;
            //检查更新
            case R.id.setting_update_container:
                MobclickAgent.onEvent(this,"CheckUpdate");
                BmobUpdateAgent.forceUpdate(this);
                break;
            //清除缓存
            case R.id.setting_clear_container:
                new MaterialDialog.Builder(SettingActivity.this)
                        .content(R.string.confirm_clear_cache)
                        .positiveText(R.string.confirm)
                        .negativeText(R.string.cancel)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                new Thread(){
                                    @Override
                                    public void run() {
                                        //清除歌词，封面等缓存
                                        CommonUtil.deleteFilesByDirectory(DiskCache.getDiskCacheDir(SettingActivity.this,"lrc"));
                                        CommonUtil.deleteFilesByDirectory(DiskCache.getDiskCacheDir(SettingActivity.this,"thumbnail"));
                                        //清除配置文件、数据库等缓存
                                        CommonUtil.deleteFilesByDirectory(getCacheDir());
                                        CommonUtil.deleteFilesByDirectory(getFilesDir());
                                        SPUtil.deleteFile(SettingActivity.this,"Setting");
                                        deleteDatabase(DBOpenHelper.DBNAME);
                                        //清除fresco缓存
                                        Fresco.getImagePipeline().clearCaches();
                                        mHandler.sendEmptyMessage(CLEARFINISH);
                                    }
                                }.start();
                            }
                        })
                        .backgroundColorAttr(R.attr.background_color_3)
                        .positiveColorAttr(R.attr.text_color_primary)
                        .negativeColorAttr(R.attr.text_color_primary)
                        .contentColorAttr(R.attr.text_color_primary)
                        .show();
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("needRefresh",mNeedRefresh);
        outState.putBoolean("fromColorChoose",mFromColorChoose);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 0 && data != null){
            mNeedRefresh = data.getBooleanExtra("needRefresh",false);
            mFromColorChoose = data.getBooleanExtra("fromColorChoose",false);
            if(mNeedRefresh){
                mHandler.sendEmptyMessage(RECREATE);
//                if(mFromColorChoose)
//                    mModeSwitch.setChecked(false);
            }

        }
    }

}
