package com.example.xyzreader.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.udafil.dhruvamsharma.loadinglib.KeepLoading;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ArticleListActivity.class.toString();
    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    private static final int VIEW_TYPE_NORMAL = 1;
    private static final int VIEW_TYPE_FULL_SIZE = 2;

    public DynamicHeightNetworkImageView thumbnail;


    View rootLayout;

    private int revealX;
    private int revealY;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        rootLayout = findViewById(R.id.root_layout);
        mRecyclerView = findViewById(R.id.recycler_view);
        getLoaderManager().initLoader(0, null, this);

        Intent intent = getIntent();

        if(savedInstanceState == null && intent.hasExtra(getResources().getString(R.string.extra_reveal_x))
                && intent.hasExtra(getResources()
                .getString(R.string.extra_reveal_y)) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                startActivityWithRevealAnimation(intent);

        } else {
            rootLayout.setVisibility(View.VISIBLE);
        }

        if (savedInstanceState == null) {
            refresh();
        }
    }

    private void startActivityWithRevealAnimation(Intent intent) {

        rootLayout.setVisibility(View.INVISIBLE);

        revealX = intent.getIntExtra(getResources().getString(R.string.extra_reveal_x), 0);
        revealY = intent.getIntExtra(getResources().getString(R.string.extra_reveal_y), 0);


        ViewTreeObserver viewTreeObserver = rootLayout.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    revealActivity(revealX, revealY);
                    rootLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        }

    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);

                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = 1;
        GridLayoutManager sglm = new GridLayoutManager(getApplicationContext(), columnCount);

        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolderFullSize> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolderFullSize onCreateViewHolder(ViewGroup parent, int viewType) {

            LayoutInflater inflater = getLayoutInflater();
            final ViewHolderFullSize viewHolder;

            View view = inflater.inflate(R.layout.list_item_article_full_size, parent, false);
            viewHolder = new ViewHolderFullSize( view );

            clickHandler(view, viewHolder);

            return viewHolder;
        }

        private Date parsePublishedDate() {
            try {
                String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();
            }
        }

        @Override
        public void onBindViewHolder(final ViewHolderFullSize holder, int position) {
            mCursor.moveToPosition(position);



                ViewHolderFullSize viewHolderNormal = (ViewHolderFullSize) holder;
                viewHolderNormal.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
                Date publishedDate = parsePublishedDate();
                if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                    viewHolderNormal.subtitleView.setText(Html.fromHtml(
                            DateUtils.getRelativeTimeSpanString(
                                    publishedDate.getTime(),
                                    System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                    DateUtils.FORMAT_ABBREV_ALL).toString()
                                    + "<br/>" + " by "
                                    + mCursor.getString(ArticleLoader.Query.AUTHOR)));
                } else {
                    viewHolderNormal.subtitleView.setText(Html.fromHtml(
                            outputFormat.format(publishedDate)
                                    + "<br/>" + " by "
                                    + mCursor.getString(ArticleLoader.Query.AUTHOR)));
                }


//                Glide.with(ArticleListActivity.this)
//                        .load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
//                        .into(viewHolderNormal.thumbnailView);

                viewHolderNormal.thumbnailView.setImageUrl(
                        mCursor.getString(ArticleLoader.Query.THUMB_URL),
                        ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
                viewHolderNormal.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));




        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }

        @Override
        public int getItemViewType(int position) {

            if((position + 1) % 3 == 0) {
                return VIEW_TYPE_FULL_SIZE;
            } else
                return VIEW_TYPE_NORMAL;

        }

        private void clickHandler(View view, final RecyclerView.ViewHolder viewHolder) {

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(viewHolder.getAdapterPosition())));

                    startActivity( intent);
                }
            });

        }
    }

    public class ViewHolderNormal extends RecyclerView.ViewHolder {
        public ConstraintLayout mainBackground;
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolderNormal(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail_full_size);
            titleView = (TextView) view.findViewById(R.id.article_title_full_size);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle_full_size);
            mainBackground = (ConstraintLayout) view.findViewById(R.id.main_layout_full_size);


        }
    }

    public class ViewHolderFullSize extends RecyclerView.ViewHolder {
        public ConstraintLayout mainBackground;
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolderFullSize(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail_full_size);
            titleView = (TextView) view.findViewById(R.id.article_title_full_size);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle_full_size);
            mainBackground = (ConstraintLayout) view.findViewById(R.id.main_layout);


        }
    }




    protected void revealActivity(int x, int y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            float finalRadius = (float) (Math.max(rootLayout.getWidth(), rootLayout.getHeight()) * 1.1);

            // create the animator for this view (the start radius is zero)
            Animator circularReveal = ViewAnimationUtils.createCircularReveal(rootLayout, x, y, 0, finalRadius);
            circularReveal.setDuration(400);
            circularReveal.setInterpolator(new AccelerateInterpolator());

            // make the view visible and start the animation
            rootLayout.setVisibility(View.VISIBLE);
            circularReveal.start();
        } else {
            finish();
        }
    }

}
