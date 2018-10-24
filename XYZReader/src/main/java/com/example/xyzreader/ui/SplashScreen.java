package com.example.xyzreader.ui;

import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.xyzreader.R;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        ConstraintLayout base = findViewById(R.id.main_layout_splashscreen);


        startNextActivityWithReveal(base);
    }

    private void startNextActivityWithReveal(View view) {
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(this, view, "transition");
        int revealX = (int) (view.getX() + view.getWidth() / 2);
        int revealY = (int) (view.getY() + view.getHeight() / 2);

        Intent intent = new Intent(this, ArticleListActivity.class);
        intent.putExtra(getResources().getString(R.string.extra_reveal_x), revealX);
        intent.putExtra(getResources().getString(R.string.extra_reveal_y), revealY);

        ActivityCompat.startActivity(this, intent, options.toBundle());
        //TODO remove this
        finish();
    }
}
