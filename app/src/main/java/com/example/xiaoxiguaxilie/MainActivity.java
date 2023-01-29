package com.example.xiaoxiguaxilie;


import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.example.xiaoxiguaxilie.bean.BuyerNum;
import com.example.xiaoxiguaxilie.service.KeepAliveService;
import com.example.xiaoxiguaxilie.util.CipherUtils;
import com.example.xiaoxiguaxilie.util.HttpClient;
import com.example.xiaoxiguaxilie.util.NotificationSetUtil;
import com.example.xiaoxiguaxilie.util.UpdateApk;
import com.example.xiaoxiguaxilie.util.WindowPermissionCheck;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


/**
 * 佣金支持卡小数点
 * 停止接单取消所有网络请求
 * 远程公告、频率等
 * try catch
 * 多买号情况下，不选择买号接单的问题
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private EditText etUname,etPaw;
    private TextView tvStart,tvStop,tvLog,tvBrow,tvGetTitle,tvTitle;
    private Handler mHandler;
    private String tbId;
    private int tbIndex;
    /*
    接单成功音乐提示播放次数（3次）
    播放的次数是count+1次
     */
    private int count;
    private SharedPreferences userInfo;
    private int minPl;
    private String cookie;
    private List<BuyerNum> buyerNumList;
    private boolean isAuth = false;
    private AlertDialog alertDialog2;
    private String[] tbNameArr;
    private Dialog dialog;
    private static String LOGIN_URL = "";
    private static String BROW_OPEN = "";


    //小西瓜
    private long version = 827;
    private String todayCount;
    private String theWeekCount;
    private String theMonthCount;



    private static final String LOGIN = "/tradingtreasure/user/login.do";
    private static final String GET_TB_INFO = "/tradingtreasure/webmobile/apprentice/buyerAccountListBrief.do";
    private static final String SETTING_TB = "/tradingtreasure/webmobile/apprentice/toggleBuyerAccount.do";
    private static final String GET_TASK = "/tradingtreasure//webmobile/apprentice/getBrushHandTaskV2.do";
    private static final String LQ_TASK = "/tradingtreasure/webmobile/apprentice/snatchATask.do";
    private static final String GET_SHOP_DETAIL = "/tradingtreasure/webmobile/apprentice/receive_task.do";
    private static final String CHECK_TASK = "/tradingtreasure//webmobile/apprentice/toBuy.do";




    /**
     * 需要更改的地方：
     * 1、MainActivity
     * 2、build.gradle配置文件
     * 3、AndroidMainfest.xml文件
     * 4、Update文件
     * 5、KeepAlive文件
     */
    private static final String PT_NAME = "xiaoXiGua";
    private static final String TITLE = "小西瓜助手";
    private static final String SUCCESS_TI_SHI = "小西瓜接单成功";
    private static final String CHANNELID = "xiaoxiguaSuccess";
    private static int ICON = R.mipmap.xiaoxigua;
    private static final int JIE_DAN_SUCCESS = R.raw.xxg_success;
    private static final int JIE_DAN_FAIL = R.raw.xxg_fail;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去掉标题栏
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(this, KeepAliveService.class);
        startService(intent);//启动保活服务
        ignoreBatteryOptimization();//忽略电池优化
        if(!checkFloatPermission(this)){
            //权限请求方法
            requestSettingCanDrawOverlays();
        }
        initView();
    }


    private void initView(){
        //检查更新
        UpdateApk.update(MainActivity.this);
        //是否开启通知权限
        openNotification();
        //是否开启悬浮窗权限
        WindowPermissionCheck.checkPermission(this);
        //获取平台地址
        getPtAddress();
        mHandler = new Handler();
        tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText(TITLE);
        tvBrow = findViewById(R.id.tv_brow);
        etUname = findViewById(R.id.et_username);
        etPaw = findViewById(R.id.et_password);
        tvStart = findViewById(R.id.tv_start);
        tvStop = findViewById(R.id.tv_stop);
        tvLog = findViewById(R.id.tv_log);
        getUserInfo();//读取用户信息
        //设置textView为可滚动方式
        tvLog.setMovementMethod(ScrollingMovementMethod.getInstance());
        tvLog.setTextIsSelectable(true);
        tvStart.setOnClickListener(this);
        tvStop.setOnClickListener(this);
        tvBrow.setOnClickListener(this);
        tvGetTitle = findViewById(R.id.tv_getTitle);
        tvGetTitle.setOnClickListener(this);
        tvLog.setText("多绑号多接单~"+"\n");
        buyerNumList = new ArrayList<>();
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.tv_start:

                version = 827;
                version += getDayCount();

                cookie = "";
                tbId = null;
                /*
                先清除掉之前的Handler中的Runnable，不然会和之前的任务一起执行多个
                 */
                mHandler.removeCallbacksAndMessages(null);
                if(LOGIN_URL == ""){
                    tvLog.setText("获取最新网址中,请3秒后重试...");
                }else {
                    userLogin(etUname.getText().toString().trim(),etPaw.getText().toString().trim(),"login");
                }
                break;
            case R.id.tv_stop:
                stop();
                break;
            case R.id.tv_brow:
                version = 827;
                version += getDayCount();
                browOpen();
                break;
            case R.id.tv_getTitle:
                if(LOGIN_URL == ""){
                    tvLog.setText("获取最新网址中,请3秒后重试...");
                }else {
                    version = 827;
                    version += getDayCount();

                    cookie = "";
                    userLogin(etUname.getText().toString().trim(),etPaw.getText().toString().trim(),"getShopTitle");
                }
                break;
        }
    }


    /**
     * 弹窗公告
     */
    public void announcementDialog(String[] lesson){

        dialog = new AlertDialog
                .Builder(this)
                .setTitle("公告")
                .setCancelable(false) //触摸窗口边界以外是否关闭窗口，设置 false
                .setPositiveButton("我知道了", null)
                //.setMessage("")
                .setItems(lesson,null)
                .create();
        dialog.show();
    }


    private void browOpen(){
        if(BROW_OPEN == "") {
            tvLog.setText("获取最新网址中,请3秒后重试...");
        }
        Uri uri = Uri.parse(BROW_OPEN+version);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }


    /**
     * 重写activity的onKeyDown方法，点击返回键后不销毁activity
     * 可参考：https://blog.csdn.net/qq_36713816/article/details/71511860
     * 另外一种解决办法：重写onBackPressed方法，里面不加任务内容，屏蔽返回按钮
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }




    /**
     * 用户登录
     * @param username
     * @param password
     */
    private void userLogin(String username, String password,String mark){

        tvLog.setText(new SimpleDateFormat("HH:mm:ss").format(new Date()) + ": 正在登陆中..."+"\n");

        HttpClient.getInstance().get(LOGIN, LOGIN_URL)
                .params("qrCodeId", version)
                .params("user_account", username)
                .params("user_password", CipherUtils.md5(password))
                .params("user_type", "0")
                .params("fingerprint", "3177720876")
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            /**
                             * {"msg":"系统繁忙，请稍后再试","code":"4022","success":"false"}
                             * {"code":"200","count":0,"msg":"登录成功","success":"true","value":"0"}
                             */
                            JSONObject obj = JSONObject.parseObject(response.body());
                            System.out.println(obj);
                            //登录成功
                            if("true".equals(obj.getString("success"))){
                                saveUserInfo(username,password);
                                sendLog(obj.getString("msg"));
                                List<String> list = response.headers().values("Set-Cookie");
                                for (String str : list) {
                                    if(str.contains("JSESSIONID") || str.contains("user_")){
                                        cookie += str.substring(0, str.indexOf(";")) + "; ";
                                    }
                                }
                                cookie += "ic="+version;
                                if("login".equals(mark)){
                                    getTbInfo();
                                }else {
                                    getShopTitle();
                                }
                                return;
                            }
                            sendLog(obj.getString("msg"));
                        }catch (Exception e){
                            sendLog("登录："+e.getMessage());
                        }
                    }
                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        sendLog("登录ERR："+response.getException());
                    }
                });
    }




    /**
     * 获取淘宝账号
     */
    private void getTbInfo() {
        HttpClient.getInstance().get(GET_TB_INFO, LOGIN_URL)
                .params("sortType", "0")
                .params("page", "0")
                .params("limit", "20")
                .headers("Cookie",cookie)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            JSONObject o = JSONObject.parseObject(response.body());
                            System.out.println(o);
                            /**
                             * {"code":"4022","msg":"系统繁忙，请稍后再试","success":"false"}
                             */
                            if("true".equals(o.getString("success"))){
                                //获取绑定淘宝号信息
                                JSONArray jsonArray = o.getJSONObject("data").getJSONArray("accountList");
                                buyerNumList.clear();
                                for (int i = 0; i < jsonArray.size(); i++) {
                                    JSONObject tbInfo = jsonArray.getJSONObject(i);
                                    /**
                                     * 预测0是淘宝。代考察
                                     */
                                    if("0".equals(tbInfo.getString("account_type"))){
                                        String tbName = tbInfo.getString("account_number");
                                        /**
                                         * status
                                         * 0未审核
                                         * 1审核通过
                                         * 2未通过
                                         */
                                        String tbStatus = tbInfo.getString("state");
                                        if("1".equals(tbStatus)){
                                            String tbId = tbInfo.getString("buyer_account_id");
                                            buyerNumList.add(new BuyerNum(tbId,tbName));
                                        }else{
                                            sendLog(tbName+"："+tbInfo.getString("remarks"));
                                        }

                                    }
                                }
                                if(buyerNumList.size() == 0){
                                    sendLog("无可用的接单账号");
                                    return;
                                }
                                sendLog("获取到"+buyerNumList.size()+"个可用接单号");
                                tbNameArr = new String[buyerNumList.size()+1];
                                tbNameArr[0] = "自动切换买号";
                                for (int i = 0; i < buyerNumList.size(); i++){
                                    tbNameArr[i+1] = buyerNumList.get(i).getName();
                                }
                                showSingleAlertDialog();
                            }else if("4022".equals(o.getString("code"))){
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        getTbInfo();
                                    }
                                }, 3000);
                            } else {
                                sendLog(o.getString("msg"));
                            }
                        }catch (Exception e){
                            sendLog("淘宝号:"+e.getMessage());
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        sendLog("getTbInfo Err："+response.getException());
                    }
                });
    }


    public void showSingleAlertDialog(){

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("请选择接单淘宝号");
        alertBuilder.setCancelable(false); //触摸窗口边界以外是否关闭窗口，设置 false
        alertBuilder.setSingleChoiceItems( tbNameArr, -1, new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(DialogInterface arg0, int index) {
                if("自动切换买号".equals(tbNameArr[index])){
                    isAuth = true;
                    sendLog("将使用 "+tbNameArr[index]+" 进行接单");
                }else {
                    isAuth = false;
                    //根据选择的淘宝名获取淘宝id
                    List<BuyerNum> buyerNum = buyerNumList.stream().
                            filter(p -> p.getName().equals(tbNameArr[index])).collect(Collectors.toList());
                    tbId = buyerNum.get(0).getId();
                    sendLog("将使用 "+buyerNum.get(0).getName()+" 进行接单");
                }
            }
        });
        alertBuilder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                //TODO 业务逻辑代码
                if(!isAuth && tbId == null){
                    sendLog("未选择接单账号");
                    return;
                }
                start();
                // 关闭提示框
                alertDialog2.dismiss();
            }
        });
        alertDialog2 = alertBuilder.create();
        alertDialog2.show();
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    public void start(){
        if(isAuth){
            tbIndex = 0;
            tbId = buyerNumList.get(tbIndex).getId();
            tbIndex++;  //++的目的是，如果3个买号都是正常的，则会获取第二个买号
        }
        setAccount();
    }



    private long getDayCount(){
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String nowdayTime = dateFormat.format(new Date());
        String date = "2022-09-09";
        return getDaySub(date,nowdayTime);
    }



    private void setAccount() {
        HttpClient.getInstance().post(SETTING_TB, LOGIN_URL)
                .params("buyerAccountId",tbId)
                .headers("Cookie",cookie)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        /**
                         * 应该不会出现：{"code":"206","count":0,"msg":"未通过审核，无法切换为默认账号，请联系客服！","success":"false"}
                         * {"code":"203","count":0,"msg":"已经是默认账号了","success":"false"}
                         * {"code":"4022","msg":"系统繁忙，请稍后再试","success":"false"}
                         * {"code":"200","count":0,"msg":"切换成功！","success":"true"}
                         */
                        JSONObject o = JSONObject.parseObject(response.body());
                        System.out.println(o);
                        if("203".equals(o.getString("code"))){
                            sendLog(o.getString("msg"));
                            checkTask();
                            return;
                        }
                        if("4022".equals(o.getString("code"))){
                            sendLog("设置默认："+o.getString("msg"));
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    setAccount();
                                }
                            }, 3000);
                            return;
                        }
                        if(!"true".equals(o.getString("success"))){
                            sendLog(o.getString("msg"));
                            playMusic(JIE_DAN_FAIL,3000,0);
                            return;
                        }
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                checkTask();
                            }
                        }, 3000);

                    }
                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                    }
                });
    }


    private void checkTask() {
        HttpClient.getInstance().get(CHECK_TASK, LOGIN_URL)
                .params("ut", "apprentice")
                .headers("Cookie",cookie)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            JSONObject o = JSONObject.parseObject(response.body());
                            System.out.println(o);
                            if("true".equals(o.getString("success"))){
                                JSONObject j = o.getJSONObject("data");
                                if(j.containsKey("tid")){
                                    sendLog("已存在任务,请先完成在接单");
                                    playMusic(JIE_DAN_FAIL,3000,0);
                                }else {
                                    getTask();
                                }
                            }else {
                                sendLog(o.getString("msg"));
                            }
                        }catch (Exception e){

                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                    }
                });
    }


    private void getShopTitle() {
        HttpClient.getInstance().get(CHECK_TASK, LOGIN_URL)
                .params("ut", "apprentice")
                .headers("Cookie",cookie)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            JSONObject o = JSONObject.parseObject(response.body());
                            System.out.println(o);
                            if("true".equals(o.getString("success"))){
                                JSONObject j = o.getJSONObject("data");
                                if(j.containsKey("tid")){
                                    getShopDetail(j.getString("tid"));
                                }else {
                                    sendLog("暂无任务,请先领取任务");
                                }
                            }else {
                                sendLog(o.getString("msg"));
                            }
                        }catch (Exception e){

                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                    }
                });
    }




    private void getTask(){
        HttpClient.getInstance().post(GET_TASK, LOGIN_URL)
                .isSpliceUrl(true)  //是否强制将params的参数拼接到url后面
                .upJson("{}")
                .params("ut","apprentice")
                .headers("Cookie",cookie)
                .execute(new StringCallback() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            /**
                             * {"msg":"人太多了，请等等再试","code":"4021","success":"false"}
                             * {"count":0,"code":"206","msg":"您有超过1单未评价任务,完成后再来试试吧！","success":"false","data":null,"value":null}
                             * {"code":"200","count":0,"msg":"成功占有订单！","success":"true","data":"10051592","value":null}
                             */
                            if(302 == response.code()){
                                sendLog("请重新登录");
                                playMusic(JIE_DAN_FAIL,3000,0);
                                return;
                            }
                            JSONObject obj = JSONObject.parseObject(response.body());
                            System.out.println(obj);
                            if("true".equals(obj.getString("success"))){
                                sendLog("接单成功");
                                playMusic(JIE_DAN_SUCCESS,3000,2);
                                lqTask(obj.getString("data"));
                            } else {
                                if("206".equals(obj.getString("code"))){
                                    playMusic(JIE_DAN_FAIL,3000,0);
                                    sendLog(obj.getString("msg"));
                                    return;
                                }
                                if("刷单过于频繁，超过次数限制！".equals(obj.getString("msg"))){
                                    List<BuyerNum> buyerNum = buyerNumList.stream().
                                            filter(p -> p.getId().equals(tbId)).collect(Collectors.toList());
                                    sendLog(buyerNum.get(0).getName()+" 日/周/月已接满，请明天在试");
                                }else {
                                    sendLog(obj.getString("msg"));
                                }
                                jieDan();
                            }
                        }catch (Exception e){
                            sendLog("acceptV2："+e.getMessage());
                        }
                    }
                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        sendLog("acceptV2 Err："+response.getException());
                    }
                });
    }





    private void lqTask(String orderId){

        HttpClient.getInstance().post(LQ_TASK+"?ut=apprentice", LOGIN_URL)
                .params("transaction_record_id",orderId)
                .headers("Cookie",cookie)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            /**
                             *
                             * {"code":"200","count":0,"msg":"成功","success":"true"}
                             */
                            JSONObject obj = JSONObject.parseObject(response.body());
                            System.out.println(obj);
                            if("true".equals(obj.getString("success"))){
                                getShopDetail(orderId);
                                return;
                            }
                            sendLog(obj.getString("msg"));
                        }catch (Exception e){
                            sendLog("acceptV2："+e.getMessage());
                        }
                    }
                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                    }
                });
    }



    private void getShopDetail(String orderId) {
        HttpClient.getInstance().get(GET_SHOP_DETAIL, LOGIN_URL)
                .params("tid", orderId)
                .headers("Cookie",cookie)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            JSONObject o = JSONObject.parseObject(response.body());
                            System.out.println(o);
                            if("true".equals(o.getString("success"))){
                                JSONObject j = o.getJSONObject("data").getJSONObject("task").getJSONObject("order").getJSONObject("commodity");
                                JSONObject k = o.getJSONObject("data").getJSONObject("task").getJSONObject("order");
                                sendLog2("-------------------------------");
                                sendLog2("商品关键词："+j.getString("keyword"));
                                sendLog2("-------------------------------");
                                sendLog2("商品淘口令："+j.getString("validate_url"));
                                sendLog2("-------------------------------");
                                sendLog2("店铺名："+k.getString("seller_id"));
                            }else {
                                sendLog(o.getString("msg"));
                            }
                        }catch (Exception e){

                        }
                    }
                });
    }



    /**
     * 走到这里说明一定没接到任务，不然就是判断逻辑有问题
     */
    public void jieDan(){
        if(isAuth){
            if(buyerNumList.size() != 1){
                if (tbIndex < buyerNumList.size()) {
                    tbId = buyerNumList.get(tbIndex).getId();
                } else {
                    tbIndex = 0;
                    tbId = buyerNumList.get(tbIndex).getId();
                }
                tbIndex++;

                HttpClient.getInstance().post(SETTING_TB, LOGIN_URL)
                        .params("buyerAccountId",tbId)
                        .headers("Cookie",cookie)
                        .execute(new StringCallback() {
                            @Override
                            public void onSuccess(Response<String> response) {
                                JSONObject o = JSONObject.parseObject(response.body());
                                System.out.println(o);
                                /**
                                 * 应该不会出现：{"code":"206","count":0,"msg":"未通过审核，无法切换为默认账号，请联系客服！","success":"false"}
                                 * {"code":"203","count":0,"msg":"已经是默认账号了","success":"false"}
                                 * {"code":"4022","msg":"系统繁忙，请稍后再试","success":"false"}
                                 * {"code":"200","count":0,"msg":"切换成功！","success":"true"}
                                 */
                                if("4022".equals(o.getString("code"))){
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            setAccount();
                                        }
                                    }, minPl);
                                    return;
                                }
                                if(!"true".equals(o.getString("success"))){
                                    sendLog(o.getString("msg"));
                                    playMusic(JIE_DAN_FAIL,3000,0);
                                    return;
                                }
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        checkTask();
                                    }
                                }, minPl);
                            }
                            @Override
                            public void onError(Response<String> response) {
                                super.onError(response);
                                sendLog("jieDan出错啦~"+response.getException());
                            }
                        });
            }
        }else {
            mHandler.postDelayed(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void run() {
                    checkTask();
                }
            }, minPl);
        }
    }






    /**
     * 停止接单
     */
    public void stop(){
        OkGo.getInstance().cancelAll();
        //Handler中已经提供了一个removeCallbacksAndMessages去清除Message和Runnable
        mHandler.removeCallbacksAndMessages(null);
        sendLog("已停止接单");
    }



    public void getPtAddress(){

        HttpClient.getInstance().get("/ptVersion/checkUpdate","http://47.94.255.103")
                .params("ptName",PT_NAME)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            JSONObject ptAddrObj = JSONObject.parseObject(response.body());
                            if(ptAddrObj == null){
                                Toast.makeText(MainActivity.this, "没有配置此平台更新信息！", Toast.LENGTH_LONG).show();
                                return;
                            }
                            LOGIN_URL = ptAddrObj.getString("ptUrl");
                            BROW_OPEN = ptAddrObj.getString("openUrl");
                            minPl = Integer.parseInt(ptAddrObj.getString("pinLv"));
                            String[] jieDan = ptAddrObj.getString("apkVersion").split(",");
                            todayCount = jieDan[0];
                            theWeekCount = jieDan[1];
                            theMonthCount = jieDan[2];

                            //公告弹窗
                            String[] gongGao = ptAddrObj.getString("ptAnnoun").split(";");
                            announcementDialog(gongGao);
                        }catch (Exception e){
                            sendLog("获取网址："+e.getMessage());
                        }

                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        sendLog("服务器出现问题啦~");
                    }
                });
    }


    /**
     * 接单成功后通知铃声
     * @param voiceResId 音频文件
     * @param milliseconds 需要震动的毫秒数
     */
    private void playMusic(int voiceResId, long milliseconds,int total){

        count = total;//不然会循环播放

        //播放语音
        MediaPlayer player = MediaPlayer.create(MainActivity.this, voiceResId);
        player.start();
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                //播放完成事件
                if(count != 0){
                    player.start();
                }
                count --;
            }
        });

        //震动
        Vibrator vib = (Vibrator) this.getSystemService(Service.VIBRATOR_SERVICE);
        //延迟的毫秒数
        vib.vibrate(milliseconds);
    }



    /**
     * 日志更新
     * @param log
     */
    public void sendLog(String log){
        scrollToTvLog();
        if(tvLog.getLineCount() > 40){
            tvLog.setText("");
        }
        tvLog.append(new SimpleDateFormat("HH:mm:ss").format(new Date()) + ": "+log+"\n");
    }

    public void sendLog2(String log){
        scrollToTvLog();
        tvLog.append(new SimpleDateFormat("HH:mm:ss").format(new Date()) + ": "+log+"\n");
    }




    public long getDaySub(String beginDateStr,String endDateStr) {

        long day = 0;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date beginDate;
        Date endDate;
        try {
            beginDate = format.parse(beginDateStr);
            endDate = format.parse(endDateStr);
            day = (endDate.getTime()-beginDate.getTime())/(24*60*60*1000);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        System.out.println("day:"+day);

        return day;
    }




    /**
     * 忽略电池优化
     */

    public void ignoreBatteryOptimization() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        boolean hasIgnored = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            hasIgnored = powerManager.isIgnoringBatteryOptimizations(getPackageName());
            //  判断当前APP是否有加入电池优化的白名单，如果没有，弹出加入电池优化的白名单的设置对话框。
            if(!hasIgnored) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:"+getPackageName()));
                startActivity(intent);
            }
        }
    }


    private void openNotification(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //判断是否需要开启通知栏功能
            NotificationSetUtil.OpenNotificationSetting(this);
        }
    }



    //权限打开
    private void requestSettingCanDrawOverlays() {
        int sdkInt = Build.VERSION.SDK_INT;
        if (sdkInt >= Build.VERSION_CODES.O) {//8.0以上
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivityForResult(intent, 1);
        } else if (sdkInt >= Build.VERSION_CODES.M) {//6.0-8.0
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 1);
        } else {//4.4-6.0以下
            //无需处理了
        }
    }




    //判断是否开启悬浮窗权限   context可以用你的Activity.或者tiis
    public static boolean checkFloatPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            return true;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            try {
                Class cls = Class.forName("android.content.Context");
                Field declaredField = cls.getDeclaredField("APP_OPS_SERVICE");
                declaredField.setAccessible(true);
                Object obj = declaredField.get(cls);
                if (!(obj instanceof String)) {
                    return false;
                }
                String str2 = (String) obj;
                obj = cls.getMethod("getSystemService", String.class).invoke(context, str2);
                cls = Class.forName("android.app.AppOpsManager");
                Field declaredField2 = cls.getDeclaredField("MODE_ALLOWED");
                declaredField2.setAccessible(true);
                Method checkOp = cls.getMethod("checkOp", Integer.TYPE, Integer.TYPE, String.class);
                int result = (Integer) checkOp.invoke(obj, 24, Binder.getCallingUid(), context.getPackageName());
                return result == declaredField2.getInt(cls);
            } catch (Exception e) {
                return false;
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AppOpsManager appOpsMgr = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
                if (appOpsMgr == null)
                    return false;
                int mode = appOpsMgr.checkOpNoThrow("android:system_alert_window", android.os.Process.myUid(), context
                        .getPackageName());
                return mode == AppOpsManager.MODE_ALLOWED || mode == AppOpsManager.MODE_IGNORED;
            } else {
                return Settings.canDrawOverlays(context);
            }
        }
    }




    /**
     * 保存用户信息
     */
    private void saveUserInfo(String username,String password){
        userInfo = getSharedPreferences("userData", MODE_PRIVATE);
        SharedPreferences.Editor editor = userInfo.edit();//获取Editor
        //得到Editor后，写入需要保存的数据
        editor.putString("username",username);
        editor.putString("password", password);
        editor.commit();//提交修改

    }



    /**
     * 接单成功执行逻辑
     */
    protected void receiveSuccess(String bj,String yj){
        //前台通知的id名，任意
        String channelId = CHANNELID;
        //前台通知的名称，任意
        String channelName = "接单成功状态栏通知";
        //发送通知的等级，此处为高，根据业务情况而定
        int importance = NotificationManager.IMPORTANCE_HIGH;

        // 2. 获取系统的通知管理器
        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        // 3. 创建NotificationChannel(这里传入的channelId要和创建的通知channelId一致，才能为指定通知建立通知渠道)
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(channelId,channelName, importance);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(channel);
        }
        //点击通知时可进入的Activity
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,notificationIntent,0);
        // 1. 创建一个通知(必须设置channelId)
        @SuppressLint("WrongConstant") Notification notification = new NotificationCompat.Builder(this,channelId)
                .setContentTitle(SUCCESS_TI_SHI)
                .setContentText("本金:"+bj+"  佣金:"+yj)
                .setSmallIcon(ICON)
                .setContentIntent(pendingIntent)//点击通知进入Activity
                .setPriority(NotificationCompat.PRIORITY_MAX) //设置通知的优先级为最大
                .setCategory(Notification.CATEGORY_TRANSPORT) //设置通知类别
                .setVisibility(Notification.VISIBILITY_PUBLIC)  //控制锁定屏幕中通知的可见详情级别
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),ICON))   //设置大图标
                .build();

        // 4. 发送通知
        notificationManager.notify(2, notification);
    }


    public void onResume() {
        super.onResume();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //移除标记为id的通知 (只是针对当前Context下的所有Notification)
        notificationManager.cancel(2);
        //移除所有通知
        //notificationManager.cancelAll();

    }




    /**
     * 读取用户信息
     */
    private void getUserInfo(){
        userInfo = getSharedPreferences("userData", MODE_PRIVATE);
        String username = userInfo.getString("username", null);//读取username
        String passwrod = userInfo.getString("password", null);//读取password
        if(username!=null && passwrod!=null){
            etUname.setText(username);
            etPaw.setText(passwrod);
        }
    }


    public void scrollToTvLog(){
        int tvHeight = tvLog.getHeight();
        int tvHeight2 = getTextViewHeight(tvLog);
        if(tvHeight2>tvHeight){
            tvLog.scrollTo(0,tvHeight2-tvLog.getHeight());
        }
    }

    private int getTextViewHeight(TextView textView) {
        Layout layout = textView.getLayout();
        int desired = layout.getLineTop(textView.getLineCount());
        int padding = textView.getCompoundPaddingTop() +
                textView.getCompoundPaddingBottom();
        return desired + padding;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //关闭弹窗，不然会 报错（虽然不影响使用）
        dialog.dismiss();

    }
}