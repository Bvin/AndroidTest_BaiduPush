package cn.bvin.app.test.push;

import java.sql.Timestamp;
import java.util.Calendar;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;

public class MainActivity extends Activity implements SendMsgAsyncTask.OnSendScuessListener{

	PushApplication app;
	Gson mGson;
	String curMsg;
	
	BroadcastReceiver commReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.hasExtra("onBind")) {
				Bundle bindData = intent.getBundleExtra("onBind");
				int errorCode =  bindData.getInt("errorCode");
				String bindString;
				if (errorCode==0) {
					bindString = "用户id："+ bindData.getString("userId")+"；频道Id:"+ bindData.getString("channelId");
				}else {
					bindString = "推送服务绑定失败, 错误码"+errorCode;
				}
				((TextView)findViewById(R.id.textView1)).append(bindString);
			}else if (intent.hasExtra("onMessage")) {
				String msgLine = "";
				try {
					Message msg = (Message) intent.getSerializableExtra("onMessage");
					String userNumber = "(No."+msg.getUser_id().substring(msg.getUser_id().length()-4)+")";
					Timestamp tt = new Timestamp(msg.getTime_samp());
					msgLine = "收到消息"+tt.getHours()+":"+tt.getMinutes()
							+"："+userNumber+msg.getMessage()+"\n";
					Log.e("onReceive", msgLine);
				} catch (Exception e) {
					msgLine = "收到消息"+intent.getStringExtra("onMessage")+"\n";
				}finally {
					((TextView)findViewById(R.id.textView2)).append(msgLine);
				}
				
			}else if (intent.hasExtra("onSetTags")) {
				String info = intent.getStringExtra("onSetTags");
				Log.e("onReceive", info);
				((TextView)findViewById(R.id.textView1)).append(info);
			}
		}
		
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		BackgoundService.luanch(getApplicationContext());
		registerMessageCommReceiver();
		setContentView(R.layout.activity_main);
		((TextView)findViewById(R.id.textView1)).setText("推送准备...\n");
		app = PushApplication.getInstance();
		mGson = app.getGson();
	}
	
	private void registerMessageCommReceiver() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(MessageReceiver.ACTION_COMMUNICATION);
		LocalBroadcastManager.getInstance(this).registerReceiver(commReceiver, intentFilter);
	}
	public void send(View v) {
		String userId = app.getUserId();
		String channelId = app.getChannelId();
		EditText etMessage = ((EditText)findViewById(R.id.etMsg));
		curMsg = etMessage.getText().toString();
		Message message = new Message(userId, channelId, System.currentTimeMillis(), curMsg, "");
		SendMsgAsyncTask task = new SendMsgAsyncTask(mGson.toJson(message), userId);
		task.setOnSendScuessListener(this);
		task.send();
		etMessage.setText("");
		InputMethodManager inputmanger = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputmanger.hideSoftInputFromWindow(etMessage.getWindowToken(), 0);
	}
	
	public void setTag(View v) {
		String userId = app.getUserId();
		SetTagTask task = new SetTagTask("TAG_GROUP", userId);
		task.setTags();
	}

	@Override
	public void sendScuess(String msg) {
		Calendar calendar= Calendar.getInstance();
		String time = calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE);
		((TextView)findViewById(R.id.textView2)).append("已发出"+time+"："+curMsg+"\n");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(commReceiver);
	}

	@Override
	public void onBackPressed() {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(android.R.drawable.ic_dialog_info);
		builder.setTitle("是否退出");
		builder.setPositiveButton("完全退出",
				new DialogInterface.OnClickListener() {
					@SuppressWarnings("deprecation")
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						finish();
						ActivityManager activityMgr= (ActivityManager) MainActivity.this.getSystemService(Context.ACTIVITY_SERVICE);
						activityMgr.restartPackage(MainActivity.this.getPackageName());
						activityMgr.killBackgroundProcesses(MainActivity.this.getPackageName());
						System.exit(0);
					}
				});
		builder.setNegativeButton("退居后台",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						MainActivity.super.onBackPressed();
					}
				});
		builder.show();
		
	}
	
	
}
