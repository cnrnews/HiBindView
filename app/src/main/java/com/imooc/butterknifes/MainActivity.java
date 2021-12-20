package com.imooc.butterknifes;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import android.widget.TextView;
import com.imooc.annotations.BindView;
import com.imooc.butterknife.ButterKnife;
import com.imooc.butterknife.Unbinder;

public class MainActivity extends AppCompatActivity{

    @BindView(R.id.tv_login)
    TextView mLoginTextView;
    private Unbinder unbinder;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        unbinder = ButterKnife.bind(this);

//        mLoginTextView.setText("ddd");
    }
    @Override
    protected void onDestroy() {
        unbinder.unbind();
        super.onDestroy();
    }
}