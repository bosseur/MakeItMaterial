package com.example.xyzreader.ui;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

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

  private SimpleDateFormat dateFormat;
  // Use default locale format
  private SimpleDateFormat outputFormat = new SimpleDateFormat();
  // Most time functions can only handle 1902 - 2037
  private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

  public static int currentPosition;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_article_list);

    dateFormat = new SimpleDateFormat(getString(R.string.date_format));

    mToolbar = (Toolbar) findViewById(R.id.toolbar);

    mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

    mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
    getSupportLoaderManager().initLoader(0, null, this);

    if (savedInstanceState == null) {
      refresh();
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      prepareTransitions();
      postponeEnterTransition();
    }
  }

  /**
   * Prepares the shared element transition to the pager fragment, as well as the other transitions
   * that affect the flow.
   */
  private void prepareTransitions() {


    // A similar mapping is set at the ImagePagerFragment with a setEnterSharedElementCallback.
    setExitSharedElementCallback(
        new SharedElementCallback() {
          @Override
          public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            // Locate the ViewHolder for the clicked position.
            RecyclerView.ViewHolder selectedViewHolder = mRecyclerView
                .findViewHolderForAdapterPosition(ArticleListActivity.currentPosition);
            if (selectedViewHolder == null || selectedViewHolder.itemView == null) {
              return;
            }

            // Map the first shared element name to the child ImageView.
            sharedElements
                .put(names.get(0), selectedViewHolder.itemView.findViewById(R.id.thumbnail));
          }
        });
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
    int columnCount = getResources().getInteger(R.integer.list_column_count);
    StaggeredGridLayoutManager sglm =
        new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
    mRecyclerView.scrollToPosition(ArticleListActivity.currentPosition);
    mRecyclerView.setLayoutManager(sglm);
    Snackbar.make(mSwipeRefreshLayout, getString(R.string.done_loading),
        Snackbar.LENGTH_SHORT).show();
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    mRecyclerView.setAdapter(null);
  }

  private class Adapter extends RecyclerView.Adapter<ViewHolder> implements View.OnClickListener {
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
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
      final ViewHolder vh = new ViewHolder(view);
      view.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            vh.thumbnailView.setTransitionName("POSITION_" + getItemId(vh.getAdapterPosition()));
          }
          ArticleListActivity.currentPosition = vh.getAdapterPosition();
          Intent intent = new Intent(Intent.ACTION_VIEW,
              ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())));
          if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            Bundle bundle = ActivityOptions
                .makeSceneTransitionAnimation(ArticleListActivity.this,
                    vh.thumbnailView,
                    vh.thumbnailView.getTransitionName())
                .toBundle();
            startActivity(intent, bundle);
            return;
          }
          startActivity(intent);
        }
      });
      return vh;
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
    public void onBindViewHolder(ViewHolder holder, int position) {
      mCursor.moveToPosition(position);
      holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
      Date publishedDate = parsePublishedDate();
      if (!publishedDate.before(START_OF_EPOCH.getTime())) {

        holder.subtitleView.setText(Html.fromHtml(
            DateUtils.getRelativeTimeSpanString(
                publishedDate.getTime(),
                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_ALL).toString()
                + "<br/>" + " by "
                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
      } else {
        holder.subtitleView.setText(Html.fromHtml(
            outputFormat.format(publishedDate)
                + "<br/>" + " by "
                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
      }
      holder.thumbnailView.setImageUrl(
          mCursor.getString(ArticleLoader.Query.THUMB_URL),
          ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
      holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));

      if (ArticleListActivity.currentPosition != position) {
        return;
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        ArticleListActivity.this.startPostponedEnterTransition();
      }
    }

    @Override
    public int getItemCount() {
      return mCursor.getCount();
    }

    @Override
    public void onClick(View v) {

    }
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    public DynamicHeightNetworkImageView thumbnailView;
    public TextView titleView;
    public TextView subtitleView;

    public ViewHolder(View view) {
      super(view);
      thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
      titleView = (TextView) view.findViewById(R.id.article_title);
      subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
    }
  }

}
