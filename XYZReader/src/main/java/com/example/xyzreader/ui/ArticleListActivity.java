package com.example.xyzreader.ui;

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
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

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
public class ArticleListActivity extends ActionBarActivity implements
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);


        final View toolbarContainerView = findViewById(R.id.toolbar_container);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
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
        int columnCount = 2;
        GridLayoutManager sglm = new GridLayoutManager(getApplicationContext(), columnCount);
        sglm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if ((position+1) %3 == 0) {
                    return 1;
                } else
                    return 1;
            }
        });
        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
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
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            LayoutInflater inflater = getLayoutInflater();
            final RecyclerView.ViewHolder viewHolder;

            if(viewType == VIEW_TYPE_NORMAL) {

                View view = inflater.inflate(R.layout.list_item_article, parent, false);
                viewHolder = new ViewHolderNormal( view );

                clickHandler(view, viewHolder);

            } else {

                View view = inflater.inflate(R.layout.list_item_article_full_size, parent, false);
                viewHolder = new ViewHolderFullSize( view );

                clickHandler(view, viewHolder);
            }



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
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            mCursor.moveToPosition(position);

            if(holder.getItemViewType() == VIEW_TYPE_NORMAL) {

                ViewHolderNormal viewHolderNormal = (ViewHolderNormal) holder;
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
                viewHolderNormal.thumbnailView.setImageUrl(
                        mCursor.getString(ArticleLoader.Query.THUMB_URL),
                        ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
                viewHolderNormal.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));


            } else {

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
                viewHolderNormal.thumbnailView.setImageUrl(
                        mCursor.getString(ArticleLoader.Query.THUMB_URL),
                        ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
                viewHolderNormal.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));

            }


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
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
            mainBackground = (ConstraintLayout) view.findViewById(R.id.main_layout);


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

}
