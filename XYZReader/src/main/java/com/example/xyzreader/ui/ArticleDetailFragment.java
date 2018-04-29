package com.example.xyzreader.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.NavUtils;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends android.support.v4.app.Fragment implements
    android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor> {
  private static final String TAG = "ArticleDetailFragment";

  public static final String ARG_ITEM_ID = "item_id";
  private static final float PARALLAX_FACTOR = 1.25f;

  private Cursor mCursor;
  private long mItemId;
  private View mRootView;

  private Toolbar mToolbar;
  private CollapsingToolbarLayout mCollapsingToolbar;

  private View mPhotoContainerView;
  private ImageView mPhotoView;
  private int mScrollY;
  private boolean mIsCard = false;

  private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
  // Use default locale format
  private SimpleDateFormat outputFormat = new SimpleDateFormat();
  // Most time functions can only handle 1902 - 2037
  private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public ArticleDetailFragment() {
  }

  public static ArticleDetailFragment newInstance(long itemId) {
    Bundle arguments = new Bundle();
    arguments.putLong(ARG_ITEM_ID, itemId);
    ArticleDetailFragment fragment = new ArticleDetailFragment();
    fragment.setArguments(arguments);
    return fragment;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      // Respond to the action bar's Up/Home button
      case android.R.id.home:
        getActivity().onBackPressed();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getArguments().containsKey(ARG_ITEM_ID)) {
      mItemId = getArguments().getLong(ARG_ITEM_ID);
    }

    mIsCard = getResources().getBoolean(R.bool.detail_is_card);

    setHasOptionsMenu(true);
  }

  public ArticleDetailActivity getActivityCast() {
    return (ArticleDetailActivity) getActivity();
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
    // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
    // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
    // we do this in onActivityCreated.
    getLoaderManager().initLoader(0, null, this);
  }


  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

    mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
    mPhotoContainerView = mRootView.findViewById(R.id.photo_container);
    mToolbar = (android.support.v7.widget.Toolbar) mRootView.findViewById(R.id.toolbar);

    mCollapsingToolbar = (CollapsingToolbarLayout) mRootView.findViewById(R.id.collapse_toolbar);

    mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
            .setType("text/plain")
            .setText("Some sample text")
            .getIntent(), getString(R.string.action_share)));
      }
    });

    bindViews();
    return mRootView;
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

  private void bindViews() {
    if (mRootView == null) {
      return;
    }

    TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);
    TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
    bylineView.setMovementMethod(new LinkMovementMethod());
    TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);

    if (mCursor != null) {
      mRootView.setAlpha(0);
      mRootView.setVisibility(View.VISIBLE);
      mRootView.animate().alpha(1);
      String titleArticle = mCursor.getString(ArticleLoader.Query.TITLE);
      titleView.setText(titleArticle);
      mToolbar.setTitle(titleArticle);
      AppCompatActivity activity = (AppCompatActivity) getActivity();
      activity.setSupportActionBar(mToolbar);
      if (activity.getSupportActionBar() != null) {
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
      }

      Date publishedDate = parsePublishedDate();
      if (!publishedDate.before(START_OF_EPOCH.getTime())) {
        bylineView.setText(Html.fromHtml(
            DateUtils.getRelativeTimeSpanString(
                publishedDate.getTime(),
                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_ALL).toString()
                + " by <font color='#ffffff'>"
                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                + "</font>"));

      } else {
        // If date is before 1902, just show the string
        bylineView.setText(Html.fromHtml(
            outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                + "</font>"));

      }
      String articleText = mCursor.getString(ArticleLoader.Query.BODY);
      // For better performance
      bodyView.setText(Html.fromHtml(
          articleText.replaceAll("(\r\n|\n)", "<br />")));
      ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
          .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
              Bitmap bitmap = imageContainer.getBitmap();
              if (bitmap != null) {
                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                  mPhotoView.setTransitionName("POSITION_" + mItemId);
                  try {
                    getActivity().startPostponedEnterTransition();
                  } catch (Exception ex) {
                    Log.d(TAG, ex.getMessage());
                  }
                }
              }
            }

            @Override
            public void onErrorResponse(VolleyError volleyError) {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getActivity().startPostponedEnterTransition();
              }
            }
          });
    } else {
      mToolbar.setTitle(" ");
      mRootView.setVisibility(View.GONE);
      titleView.setText("N/A");
      bylineView.setText("N/A");
      bodyView.setText("N/A");
    }
  }

  @Override
  public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
    if (!isAdded()) {
      if (cursor != null) {
        cursor.close();
      }
      return;
    }

    mCursor = cursor;
    if (mCursor != null && !mCursor.moveToFirst()) {
      Log.e(TAG, "Error reading item detail cursor");
      mCursor.close();
      mCursor = null;
    }

    bindViews();
  }

  @Override
  public void onLoaderReset(Loader<Cursor> cursorLoader) {
    mCursor = null;
    bindViews();
  }

}
