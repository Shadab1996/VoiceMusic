package com.team3amk.voicemusic;

/**
 * Created by Funkies PC on 06-May-17.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Bhupendra on 26-04-2017.
 */

public class SMSReciever extends BroadcastReceiver {
    private OnSmsReceivedListener listener = null;
    public void setOnSmsReceivedListener(Context context) {
        this.listener = (OnSmsReceivedListener) context;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        try{
            Bundle bundle=intent.getExtras();
            SmsMessage[ ] msg=null;
            String sender="";
            // String str="SMS From ";
            String str="";
            if(bundle!=null)
            {
                Object[ ]pdus=(Object[ ])bundle.get("pdus");
                msg=new SmsMessage[pdus.length];
                for (int i=0; i<msg.length; i++)
                {
                    msg[i]=SmsMessage.createFromPdu((byte[ ])pdus[i]);
                    if(i==0)
                    {
                        sender=msg[i].getOriginatingAddress();
                        //str+=sender + ":";
                    }
                    str+=msg[i].getMessageBody().toString();
                }
                //  Toast.makeText(context, "By SMS Receiver " + str, Toast.LENGTH_LONG).show();
                if (listener != null) {
                    listener.onSmsReceived(sender, str);
                }
                this.clearAbortBroadcast();
                Log.d("SMS Receiver " ,str);

            }
        }
        catch(Exception e)
        {
            Toast.makeText(context, "Exception in SMS Broadcast " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }
    public interface OnSmsReceivedListener {
        public void onSmsReceived(String sender, String message);
    }
}
