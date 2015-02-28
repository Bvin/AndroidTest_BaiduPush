package cn.bvin.app.test.push;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Style;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.baidu.frontia.api.FrontiaPushMessageReceiver;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class MessageReceiver extends FrontiaPushMessageReceiver{

	public static final String ACTION_COMMUNICATION = "ACTION_COMMUNICATION";
	
	private List<Map<String, String>> msgList = new ArrayList<Map<String, String>>();
	
	Gson gson;
	
	/**
	 * 调用PushManager.startWork后，sdk将对pushserver发起绑定请求，
	 * 这个过程是异步的。绑定请求的结果通过onBind返回。 如果您需要用单播推送，
	 * 需要把这里获取的channelid和user id上传到应用server中，
	 * 再调用server接口用channel id和user id给单个手机或者用户推送。
	 */
	@Override
	public void onBind(Context context, int errorCode, String appid, String userId, String channelId, String requestId) {
		 String responseString = "onBind errorCode=" + errorCode + " appid="
	                + appid + " userId=" + userId + " channelId=" + channelId
	                + " requestId=" + requestId;
		PushApplication app =  PushApplication.getInstance();
		gson = app.getGson();
		app.setUserId(userId);
		app.setChannelId(channelId);
		Log.e("MessageReceiver#onBind", responseString);
		//sendData(context, "onBind","用户id："+ userId+"；频道Id:"+channelId);
		sendOnBind(context,errorCode, userId, channelId);
	}

	private void sendOnBind(Context context, int errorCode,String userId, String channelId) {
		Intent intent = new Intent(ACTION_COMMUNICATION);
		Bundle bindData = new Bundle();
		bindData.putInt("errorCode", errorCode);
		bindData.putString("userId", userId);
		bindData.putString("channelId", channelId);
		intent.putExtra("onBind", bindData);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}
	
	@Override
	public void onMessage(Context arg0, String message, String customContentString) {
		String messageString = "透传消息 message=\"" + message
                + "\" customContentString=" + customContentString;
		Log.e("MessageReceiver#onMessage", messageString);
		
		Gson mGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
				.create();
		try {
			Message msg = mGson.fromJson(message, Message.class);
			Log.e("MessageReceiver#onMessage.uid", msg.getUser_id()+"<=>"+PushApplication.getInstance().getUserId());
			if (!msg.getUser_id().equals(PushApplication.getInstance().getUserId())) {
				deliverMessage(arg0, "onMessage",msg);
				notif(arg0, msg);
			}
			
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
			sendData(arg0, "onMessage","收到消息："+message+"\n");
			Toast.makeText(arg0, "百度后台消息："+message, Toast.LENGTH_SHORT).show();
		}
		
	}
	
	Style newInboxStyle(String userNumber,List<Map<String, String>> msgList) {
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(userNumber);
        List<String> peopleMsgs = new ArrayList<String>();
        for (Map<String, String> map : msgList) {
        	for (Map.Entry<String, String> entry: map.entrySet()) {
				if (entry.getKey().equals(userNumber)) {
					peopleMsgs.add(entry.getValue());
					inboxStyle.addLine(entry.getValue());
					break;
				}
			}
		}
        inboxStyle.setSummaryText(peopleMsgs.get(peopleMsgs.size()-1));
        return inboxStyle;
    }

	private void notif(Context context,Message msg ) {
		long uid = Long.parseLong(msg.getUser_id());
		String userNumber = "No."+msg.getUser_id().substring(msg.getUser_id().length()-4);
		Map<String, String> map = new HashMap<String, String>();
		map.put(msg.getUser_id(), msg.getMessage());
		msgList.add(map);
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
		mBuilder.setDefaults(NotificationCompat.DEFAULT_ALL);
		mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
		mBuilder.setTicker("您有新的消息哦！");
		mBuilder.setSmallIcon(R.drawable.ic_launcher);
		mBuilder.setContentTitle(userNumber+"发来消息");
		if (msgList.size()>1) {
			mBuilder.setStyle(newInboxStyle(userNumber, msgList));
		}else {
			mBuilder.setContentText(msg.getMessage());
		}
		NotificationManager mNotifyMgr = 
		        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		// Builds the notification and issues it.
		mNotifyMgr.notify((int) uid, mBuilder.build());
		//Toast.makeText(arg0, msg.getMessage(), Toast.LENGTH_SHORT).show();
	}
	
	private void sendData(Context context,String key,String value) {
		Intent intent = new Intent(ACTION_COMMUNICATION);
		intent.putExtra(key, value);
		context.getApplicationContext().sendBroadcast(intent);
	}
	
	private void deliverMessage(Context context,String key,Message msg) {
		Intent intent = new Intent(ACTION_COMMUNICATION);
		intent.putExtra(key, msg);
		context.getApplicationContext().sendBroadcast(intent);
	}
	
	
	@Override
	public void onDelTags(Context arg0, int arg1, List<String> arg2, List<String> arg3, String arg4) {
		
	}

	@Override
	public void onListTags(Context arg0, int arg1, List<String> arg2, String arg3) {
		
	}

	@Override
	public void onNotificationClicked(Context arg0, String arg1, String arg2, String arg3) {
		
	}

	@Override
	public void onSetTags(Context arg0, int arg1, List<String> arg2, List<String> arg3, String arg4) {
		StringBuilder sb = new StringBuilder();
		if (arg1==0) {
			sb.append("设置成功的tag:");
			for (String string : arg2) {
				sb.append(string).append(";");
			}
		} else {
			sb.append("设置失败的tag:");
			for (String string : arg3) {
				sb.append(string).append(";");
			}
		}
		sendData(arg0, "onSetTags",sb.toString());
	}

	@Override
	public void onUnbind(Context arg0, int arg1, String arg2) {
		
	}

}
