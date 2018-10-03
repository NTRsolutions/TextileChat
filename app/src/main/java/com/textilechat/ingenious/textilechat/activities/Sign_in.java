package com.textilechat.ingenious.textilechat.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.textilechat.ingenious.textilechat.R;
import com.textilechat.ingenious.textilechat.classes.Animation;

public class Sign_in extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
    }

    @Override
    public void finish() {
        super.finish();
        Animation.slideRight(Sign_in.this);
    }
}