package com.textilechat.ingenious.textilechat.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.pixplicity.easyprefs.library.Prefs;
import com.textilechat.ingenious.textilechat.Adapters.chat_adapter;
import com.textilechat.ingenious.textilechat.R;
import com.textilechat.ingenious.textilechat.Utils.Endpoints;
import com.textilechat.ingenious.textilechat.Utils.Utils;
import com.textilechat.ingenious.textilechat.classes.Animation;
import com.textilechat.ingenious.textilechat.classes.JSONParser;
import com.textilechat.ingenious.textilechat.classes.chat_messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.textilechat.ingenious.textilechat.classes.serialize_msg_class;

import org.json.JSONException;
import org.json.JSONObject;

import cn.pedant.SweetAlert.SweetAlertDialog;
import cz.msebera.android.httpclient.Header;
import es.dmoral.toasty.Toasty;

public class Chat_Activity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private List<chat_messages> chat_message_list;
    private chat_adapter chat_adapters;
    String sending_msg;
    private EditText edit_message;
    private Button btn_send;
    final String id = Prefs.getString("user_id", "0");

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Chat");
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView=(RecyclerView) findViewById(R.id.recycler_view);
        edit_message=findViewById(R.id.edit_msg);
        btn_send=findViewById(R.id.btn_send);


        if(Utils.isOnline(Chat_Activity.this))
        {
            try
            {
                requestData(Endpoints.ip_server);
            }
            catch (Exception ex) {
                new SweetAlertDialog(Chat_Activity.this, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Oops...")
                        .setContentText("Some thing went wrong!")
                        .show();
            }
        } else
        {
            new SweetAlertDialog(Chat_Activity.this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Oops...")
                    .setContentText("Internet Not Found!")
                    .show();
        }

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                serialize_msg_class msg_class=(serialize_msg_class) intent.getSerializableExtra("msg");
                chat_messages coming=new chat_messages();

                coming.setIds(msg_class.getU_id().toString());
                coming.setUser_name(msg_class.getU_name().toString());
                coming.setMessages(msg_class.getMassege().toString());
                coming.setTimestamp(msg_class.getCreated_at().toString());

//                if(chat_message_list.size() > 1 ){
                    chat_message_list.add(coming);
                    chat_adapters.notifyDataSetChanged();
//                }else{
//                    chat_message_list = new ArrayList<>();
//                    chat_message_list.add(coming);
//                    chat_adapters = new chat_adapter(Chat_Activity.this, chat_message_list);
//                    LinearLayoutManager layoutManager = new LinearLayoutManager(Chat_Activity.this);
//                    layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
//                    recyclerView.setLayoutManager(layoutManager);
//                    recyclerView.setAdapter(chat_adapters);
//                    if (chat_adapters.getItemCount() > 1) {
//                        recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, chat_adapters.getItemCount() - 1);
//                    }
//                }

                if (chat_adapters.getItemCount() > 1) {
                    recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, chat_adapters.getItemCount() - 1);
                }
            }
        };





        //sending msg
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sending_msg=edit_message.getText().toString();
                if(!sending_msg.isEmpty()){
                    if(getIntent().getStringExtra("id_name").equals("category")) {

                        sending_chat_to_server(sending_msg,getIntent().getStringExtra("c_id")+"","0");
                        hideSoftKeyboard(Chat_Activity.this);

                    }else if(getIntent().getStringExtra("id_name").equals("sub_category")) {

                        sending_chat_to_server(sending_msg,getIntent().getStringExtra("c_id")+"",getIntent().getStringExtra("s_id")+"");
                        hideSoftKeyboard(Chat_Activity.this);
                    }
                }else{
                    Toast.makeText(Chat_Activity.this,"Insert text to send",Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter("chat"));
    }

    //getting chats from server one time
    public void requestData(String uri) {

        StringRequest request = new StringRequest(Request.Method.POST, uri, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if (response.contains("null")) {

                    chat_message_list = new ArrayList<>();
                    chat_adapters = new chat_adapter(Chat_Activity.this, chat_message_list);
                    LinearLayoutManager layoutManager = new LinearLayoutManager(Chat_Activity.this);
                    layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
                    recyclerView.setLayoutManager(layoutManager);
                    recyclerView.setAdapter(chat_adapters);
                } else {
                    try {
                        chat_message_list = JSONParser.parse_chatmessages(response);
                        chat_adapters = new chat_adapter(Chat_Activity.this, chat_message_list);
                        LinearLayoutManager layoutManager = new LinearLayoutManager(Chat_Activity.this);
                        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
                        recyclerView.setLayoutManager(layoutManager);
                        recyclerView.setAdapter(chat_adapters);
                        if (chat_adapters.getItemCount() > 1) {
                            recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, chat_adapters.getItemCount() - 1);
                        }
                    }catch (Exception e){

                    }
                }


            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        new SweetAlertDialog(Chat_Activity.this, SweetAlertDialog.ERROR_TYPE)
                                .setTitleText("Oops...")
                                .setContentText("Some thing went wrong!")
                                .show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                if(getIntent().getStringExtra("id_name").equals("category")) {

                    params.put("req_key", "retrive_category_masseges");
                    params.put("c_id", getIntent().getStringExtra("c_id")+"");

                }else if(getIntent().getStringExtra("id_name").equals("sub_category")) {

                    params.put("req_key", "retrive_sub_category_masseges");
                    params.put("c_id", getIntent().getStringExtra("c_id")+"");
                    params.put("sc_id", getIntent().getStringExtra("s_id")+"");

                }
                return params;
            }
        };
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }


    // Declear the Registration Function
    private void sending_chat_to_server(String message,String cat_id, String sc_id)
    {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("req_key","add_messges");
        params.put("cat_id",cat_id);
        params.put("sub_cat_id",sc_id);
        params.put("user_id",id);
        params.put("message",message);
        client.post(Endpoints.ip_server, params, new AsyncHttpResponseHandler()
        {
            @Override
            public void onStart()
            {
                super.onStart();
//                pd = Utils.showProgress(Chat_Activity.this);
//                pd.show();
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String response = Utils.getResponse(responseBody);
                if(response.equals("null")) {
                    //Toasty.warning(Chat_Activity.this, "Response is null", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                String  response  = Utils.getResponse(responseBody);
                if(response.equals("null")) {
                    Toasty.warning(Chat_Activity.this, "Unable to Connect Server", Toast.LENGTH_SHORT).show();

                }else {

                    Log.d("response",response);
                }
            }

            @Override
            public void onFinish() {
                super.onFinish();
//                pd.dismiss();
                edit_message.setText("");
            }
        });
    }



    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    public void finish() {
        super.finish();
        Animation.swipeLeft(this);
    }

}
