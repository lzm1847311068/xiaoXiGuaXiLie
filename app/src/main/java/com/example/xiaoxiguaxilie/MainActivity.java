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
 * ????????????????????????
 * ????????????????????????????????????
 * ????????????????????????
 * try catch
 * ???????????????????????????????????????????????????
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private EditText etUname,etPaw;
    private TextView tvStart,tvStop,tvLog,tvBrow,tvGetTitle,tvTitle;
    private Handler mHandler;
    private String tbId;
    private int tbIndex;
    /*
    ???????????????????????????????????????3??????
    ??????????????????count+1???
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


    //?????????
    private long version = 826;
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
     * ????????????????????????
     * 1???MainActivity
     * 2???build.gradle????????????
     * 3???AndroidMainfest.xml??????
     * 4???Update??????
     * 5???KeepAlive??????
     */
    private static final String PT_NAME = "xiaoXiGua";
    private static final String TITLE = "???????????????";
    private static final String SUCCESS_TI_SHI = "?????????????????????";
    private static final String CHANNELID = "xiaoxiguaSuccess";
    private static int ICON = R.mipmap.xiaoxigua;
    private static final int JIE_DAN_SUCCESS = R.raw.xxg_success;
    private static final int JIE_DAN_FAIL = R.raw.xxg_fail;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //???????????????
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(this, KeepAliveService.class);
        startService(intent);//??????????????????
        ignoreBatteryOptimization();//??????????????????
        if(!checkFloatPermission(this)){
            //??????????????????
            requestSettingCanDrawOverlays();
        }
        initView();
    }


    private void initView(){
        //????????????
        UpdateApk.update(MainActivity.this);
        //????????????????????????
        openNotification();
        //???????????????????????????
        WindowPermissionCheck.checkPermission(this);
        //??????????????????
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
        getUserInfo();//??????????????????
        //??????textView??????????????????
        tvLog.setMovementMethod(ScrollingMovementMethod.getInstance());
        tvLog.setTextIsSelectable(true);
        tvStart.setOnClickListener(this);
        tvStop.setOnClickListener(this);
        tvBrow.setOnClickListener(this);
        tvGetTitle = findViewById(R.id.tv_getTitle);
        tvGetTitle.setOnClickListener(this);
        tvLog.setText("??????????????????~"+"\n");
        buyerNumList = new ArrayList<>();
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.tv_start:

                version = 826;
                version += getDayCount();

                cookie = "";
                tbId = null;
                /*
                ?????????????????????Handler??????Runnable????????????????????????????????????????????????
                 */
                mHandler.removeCallbacksAndMessages(null);
                if(LOGIN_URL == ""){
                    tvLog.setText("?????????????????????,???3????????????...");
                }else {
                    userLogin(etUname.getText().toString().trim(),etPaw.getText().toString().trim(),"login");
                }
                break;
            case R.id.tv_stop:
                stop();
                break;
            case R.id.tv_brow:
                version = 826;
                version += getDayCount();
                browOpen();
                break;
            case R.id.tv_getTitle:
                if(LOGIN_URL == ""){
                    tvLog.setText("?????????????????????,???3????????????...");
                }else {
                    version = 826;
                    version += getDayCount();

                    cookie = "";
                    userLogin(etUname.getText().toString().trim(),etPaw.getText().toString().trim(),"getShopTitle");
                }
                break;
        }
    }


    /**
     * ????????????
     */
    public void announcementDialog(String[] lesson){

        dialog = new AlertDialog
                .Builder(this)
                .setTitle("??????")
                .setCancelable(false) //??????????????????????????????????????????????????? false
                .setPositiveButton("????????????", null)
                //.setMessage("")
                .setItems(lesson,null)
                .create();
        dialog.show();
    }


    private void browOpen(){
        if(BROW_OPEN == "") {
            tvLog.setText("?????????????????????,???3????????????...");
        }
        Uri uri = Uri.parse(BROW_OPEN+version);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }


    /**
     * ??????activity???onKeyDown????????????????????????????????????activity
     * ????????????https://blog.csdn.net/qq_36713816/article/details/71511860
     * ?????????????????????????????????onBackPressed??????????????????????????????????????????????????????
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
     * ????????????
     * @param username
     * @param password
     */
    private void userLogin(String username, String password,String mark){

        tvLog.setText(new SimpleDateFormat("HH:mm:ss").format(new Date()) + ": ???????????????..."+"\n");

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
                             * {"msg":"??????????????????????????????","code":"4022","success":"false"}
                             * {"code":"200","count":0,"msg":"????????????","success":"true","value":"0"}
                             */
                            JSONObject obj = JSONObject.parseObject(response.body());
                            System.out.println(obj);
                            //????????????
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
                            sendLog("?????????"+e.getMessage());
                        }
                    }
                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        sendLog("??????ERR???"+response.getException());
                    }
                });
    }




    /**
     * ??????????????????
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
                             * {"code":"4022","msg":"??????????????????????????????","success":"false"}
                             */
                            if("true".equals(o.getString("success"))){
                                //???????????????????????????
                                JSONArray jsonArray = o.getJSONObject("data").getJSONArray("accountList");
                                buyerNumList.clear();
                                for (int i = 0; i < jsonArray.size(); i++) {
                                    JSONObject tbInfo = jsonArray.getJSONObject(i);
                                    /**
                                     * ??????0?????????????????????
                                     */
                                    if("0".equals(tbInfo.getString("account_type"))){
                                        String tbName = tbInfo.getString("account_number");
                                        /**
                                         * status
                                         * 0?????????
                                         * 1????????????
                                         * 2?????????
                                         */
                                        String tbStatus = tbInfo.getString("state");
                                        if("1".equals(tbStatus)){
                                            String tbId = tbInfo.getString("buyer_account_id");
                                            buyerNumList.add(new BuyerNum(tbId,tbName));
                                        }else{
                                            sendLog(tbName+"???"+tbInfo.getString("remarks"));
                                        }

                                    }
                                }
                                if(buyerNumList.size() == 0){
                                    sendLog("????????????????????????");
                                    return;
                                }
                                sendLog("?????????"+buyerNumList.size()+"??????????????????");
                                tbNameArr = new String[buyerNumList.size()+1];
                                tbNameArr[0] = "??????????????????";
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
                            sendLog("?????????:"+e.getMessage());
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        sendLog("getTbInfo Err???"+response.getException());
                    }
                });
    }


    public void showSingleAlertDialog(){

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("????????????????????????");
        alertBuilder.setCancelable(false); //??????????????????????????????????????????????????? false
        alertBuilder.setSingleChoiceItems( tbNameArr, -1, new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(DialogInterface arg0, int index) {
                if("??????????????????".equals(tbNameArr[index])){
                    isAuth = true;
                    sendLog("????????? "+tbNameArr[index]+" ????????????");
                }else {
                    isAuth = false;
                    //????????????????????????????????????id
                    List<BuyerNum> buyerNum = buyerNumList.stream().
                            filter(p -> p.getName().equals(tbNameArr[index])).collect(Collectors.toList());
                    tbId = buyerNum.get(0).getId();
                    sendLog("????????? "+buyerNum.get(0).getName()+" ????????????");
                }
            }
        });
        alertBuilder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                //TODO ??????????????????
                if(!isAuth && tbId == null){
                    sendLog("?????????????????????");
                    return;
                }
                start();
                // ???????????????
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
            tbIndex++;  //++?????????????????????3??????????????????????????????????????????????????????
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
                         * ?????????????????????{"code":"206","count":0,"msg":"??????????????????????????????????????????????????????????????????","success":"false"}
                         * {"code":"203","count":0,"msg":"????????????????????????","success":"false"}
                         * {"code":"4022","msg":"??????????????????????????????","success":"false"}
                         * {"code":"200","count":0,"msg":"???????????????","success":"true"}
                         */
                        JSONObject o = JSONObject.parseObject(response.body());
                        System.out.println(o);
                        if("203".equals(o.getString("code"))){
                            sendLog(o.getString("msg"));
                            checkTask();
                            return;
                        }
                        if("4022".equals(o.getString("code"))){
                            sendLog("???????????????"+o.getString("msg"));
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
                                    sendLog("???????????????,?????????????????????");
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
                                    sendLog("????????????,??????????????????");
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
                .isSpliceUrl(true)  //???????????????params??????????????????url??????
                .upJson("{}")
                .params("ut","apprentice")
                .headers("Cookie",cookie)
                .execute(new StringCallback() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            /**
                             * {"msg":"??????????????????????????????","code":"4021","success":"false"}
                             * {"count":0,"code":"206","msg":"????????????1??????????????????,???????????????????????????","success":"false","data":null,"value":null}
                             * {"code":"200","count":0,"msg":"?????????????????????","success":"true","data":"10051592","value":null}
                             */
                            if(302 == response.code()){
                                sendLog("???????????????");
                                playMusic(JIE_DAN_FAIL,3000,0);
                                return;
                            }
                            JSONObject obj = JSONObject.parseObject(response.body());
                            System.out.println(obj);
                            if("true".equals(obj.getString("success"))){
                                sendLog("????????????");
                                playMusic(JIE_DAN_SUCCESS,3000,2);
                                lqTask(obj.getString("data"));
                            } else {
                                if("206".equals(obj.getString("code"))){
                                    playMusic(JIE_DAN_FAIL,3000,0);
                                    sendLog(obj.getString("msg"));
                                    return;
                                }
                                if("??????????????????????????????????????????".equals(obj.getString("msg"))){
                                    List<BuyerNum> buyerNum = buyerNumList.stream().
                                            filter(p -> p.getId().equals(tbId)).collect(Collectors.toList());
                                    sendLog(buyerNum.get(0).getName()+" ???/???/??????????????????????????????");
                                }else {
                                    sendLog(obj.getString("msg"));
                                }
                                jieDan();
                            }
                        }catch (Exception e){
                            sendLog("acceptV2???"+e.getMessage());
                        }
                    }
                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        sendLog("acceptV2 Err???"+response.getException());
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
                             * {"code":"200","count":0,"msg":"??????","success":"true"}
                             */
                            JSONObject obj = JSONObject.parseObject(response.body());
                            System.out.println(obj);
                            if("true".equals(obj.getString("success"))){
                                getShopDetail(orderId);
                                return;
                            }
                            sendLog(obj.getString("msg"));
                        }catch (Exception e){
                            sendLog("acceptV2???"+e.getMessage());
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
                                sendLog2("??????????????????"+j.getString("keyword"));
                                sendLog2("-------------------------------");
                                sendLog2("??????????????????"+j.getString("validate_url"));
                                sendLog2("-------------------------------");
                                sendLog2("????????????"+k.getString("seller_id"));
                            }else {
                                sendLog(o.getString("msg"));
                            }
                        }catch (Exception e){

                        }
                    }
                });
    }



    /**
     * ???????????????????????????????????????????????????????????????????????????
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
                                 * ?????????????????????{"code":"206","count":0,"msg":"??????????????????????????????????????????????????????????????????","success":"false"}
                                 * {"code":"203","count":0,"msg":"????????????????????????","success":"false"}
                                 * {"code":"4022","msg":"??????????????????????????????","success":"false"}
                                 * {"code":"200","count":0,"msg":"???????????????","success":"true"}
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
                                sendLog("jieDan?????????~"+response.getException());
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
     * ????????????
     */
    public void stop(){
        OkGo.getInstance().cancelAll();
        //Handler????????????????????????removeCallbacksAndMessages?????????Message???Runnable
        mHandler.removeCallbacksAndMessages(null);
        sendLog("???????????????");
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
                                Toast.makeText(MainActivity.this, "????????????????????????????????????", Toast.LENGTH_LONG).show();
                                return;
                            }
                            LOGIN_URL = ptAddrObj.getString("ptUrl");
                            BROW_OPEN = ptAddrObj.getString("openUrl");
                            minPl = Integer.parseInt(ptAddrObj.getString("pinLv"));
                            String[] jieDan = ptAddrObj.getString("apkVersion").split(",");
                            todayCount = jieDan[0];
                            theWeekCount = jieDan[1];
                            theMonthCount = jieDan[2];

                            //????????????
                            String[] gongGao = ptAddrObj.getString("ptAnnoun").split(";");
                            announcementDialog(gongGao);
                        }catch (Exception e){
                            sendLog("???????????????"+e.getMessage());
                        }

                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        sendLog("????????????????????????~");
                    }
                });
    }


    /**
     * ???????????????????????????
     * @param voiceResId ????????????
     * @param milliseconds ????????????????????????
     */
    private void playMusic(int voiceResId, long milliseconds,int total){

        count = total;//?????????????????????

        //????????????
        MediaPlayer player = MediaPlayer.create(MainActivity.this, voiceResId);
        player.start();
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                //??????????????????
                if(count != 0){
                    player.start();
                }
                count --;
            }
        });

        //??????
        Vibrator vib = (Vibrator) this.getSystemService(Service.VIBRATOR_SERVICE);
        //??????????????????
        vib.vibrate(milliseconds);
    }



    /**
     * ????????????
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
     * ??????????????????
     */

    public void ignoreBatteryOptimization() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        boolean hasIgnored = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            hasIgnored = powerManager.isIgnoringBatteryOptimizations(getPackageName());
            //  ????????????APP??????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            if(!hasIgnored) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:"+getPackageName()));
                startActivity(intent);
            }
        }
    }


    private void openNotification(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //???????????????????????????????????????
            NotificationSetUtil.OpenNotificationSetting(this);
        }
    }



    //????????????
    private void requestSettingCanDrawOverlays() {
        int sdkInt = Build.VERSION.SDK_INT;
        if (sdkInt >= Build.VERSION_CODES.O) {//8.0??????
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivityForResult(intent, 1);
        } else if (sdkInt >= Build.VERSION_CODES.M) {//6.0-8.0
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 1);
        } else {//4.4-6.0??????
            //???????????????
        }
    }




    //?????????????????????????????????   context???????????????Activity.??????tiis
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
     * ??????????????????
     */
    private void saveUserInfo(String username,String password){
        userInfo = getSharedPreferences("userData", MODE_PRIVATE);
        SharedPreferences.Editor editor = userInfo.edit();//??????Editor
        //??????Editor?????????????????????????????????
        editor.putString("username",username);
        editor.putString("password", password);
        editor.commit();//????????????

    }



    /**
     * ????????????????????????
     */
    protected void receiveSuccess(String bj,String yj){
        //???????????????id????????????
        String channelId = CHANNELID;
        //??????????????????????????????
        String channelName = "???????????????????????????";
        //???????????????????????????????????????????????????????????????
        int importance = NotificationManager.IMPORTANCE_HIGH;

        // 2. ??????????????????????????????
        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        // 3. ??????NotificationChannel(???????????????channelId?????????????????????channelId????????????????????????????????????????????????)
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(channelId,channelName, importance);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(channel);
        }
        //???????????????????????????Activity
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,notificationIntent,0);
        // 1. ??????????????????(????????????channelId)
        @SuppressLint("WrongConstant") Notification notification = new NotificationCompat.Builder(this,channelId)
                .setContentTitle(SUCCESS_TI_SHI)
                .setContentText("??????:"+bj+"  ??????:"+yj)
                .setSmallIcon(ICON)
                .setContentIntent(pendingIntent)//??????????????????Activity
                .setPriority(NotificationCompat.PRIORITY_MAX) //?????????????????????????????????
                .setCategory(Notification.CATEGORY_TRANSPORT) //??????????????????
                .setVisibility(Notification.VISIBILITY_PUBLIC)  //????????????????????????????????????????????????
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),ICON))   //???????????????
                .build();

        // 4. ????????????
        notificationManager.notify(2, notification);
    }


    public void onResume() {
        super.onResume();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //???????????????id????????? (??????????????????Context????????????Notification)
        notificationManager.cancel(2);
        //??????????????????
        //notificationManager.cancelAll();

    }




    /**
     * ??????????????????
     */
    private void getUserInfo(){
        userInfo = getSharedPreferences("userData", MODE_PRIVATE);
        String username = userInfo.getString("username", null);//??????username
        String passwrod = userInfo.getString("password", null);//??????password
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
        //???????????????????????? ?????????????????????????????????
        dialog.dismiss();

    }
}