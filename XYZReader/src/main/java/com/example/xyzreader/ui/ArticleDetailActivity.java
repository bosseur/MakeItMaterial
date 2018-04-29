package com.example.xyzreader.ui;


import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import java.util.List;
import java.util.Map;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
    implements LoaderManager.LoaderCallbacks<Cursor> {

  private Cursor mCursor;
  private long mStartId;

  private ViewPager mPager;
  private MyPagerAdapter mPagerAdapter;

//  private View mUpButton;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_article_detail);

    getSupportLoaderManager().initLoader(0, null, this);

    mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
    mPager = (ViewPager) findViewById(R.id.pager);
    mPager.setAdapter(mPagerAdapter);


    mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
      @Override
      public void onPageSelected(int position) {
        if (mCursor != null) {
          ArticleListActivity.currentPosition = position;
          mCursor.moveToPosition(position);

          Fragment fragment = mPagerAdapter.getItem(position);

          if (fragment.getView() != null ) {
            Toolbar toolbar = (Toolbar) fragment.getView().findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
              getSupportActionBar().setDisplayHomeAsUpEnabled(true);
              getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
          }
        }
      }


    });


    if (savedInstanceState == null) {
      if (getIntent() != null && getIntent().getData() != null) {
        mStartId = ItemsContract.Items.getItemId(getIntent().getData());
      }
    }

    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      prepareSharedElementTransition();
      postponeEnterTransition();
    }
  }

  /**
   * Prepares the shared element transition from and back to the grid fragment.
   */
  private void prepareSharedElementTransition() {


    // A similar mapping is set at the GridFragment with a setExitSharedElementCallback.
    setEnterSharedElementCallback(
        new SharedElementCallback() {
          @Override
          public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            // Locate the image view at the primary fragment (the ImageFragment that is currently
            // visible). To locate the fragment, call instantiateItem with the selection position.
            // At this stage, the method will simply return the fragment at the position and will
            // not create a new one.
            Fragment currentFragment = (Fragment) mPager.getAdapter()
                .instantiateItem(mPager, ArticleListActivity.currentPosition);
            View view = currentFragment.getView();
            if (view == null) {
              return;
            }

            // Map the first shared element name to the child ImageView.
            sharedElements.put(names.get(0), view.findViewById(R.id.photo));
          }
        });
  }


  @Override
  public boolean onSupportNavigateUp() {
    onBackPressed();
    return true;
  }

  @Override
  public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    return ArticleLoader.newAllArticlesInstance(this);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
    mCursor = cursor;
    mPagerAdapter.notifyDataSetChanged();

    // Select the start ID
    if (mStartId > 0) {
      mCursor.moveToFirst();
      // TODO: optimize
      while (!mCursor.isAfterLast()) {
        if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
          final int position = mCursor.getPosition();
          mPager.setCurrentItem(position, false);
          break;
        }
        mCursor.moveToNext();
      }
      mStartId = 0;
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> cursorLoader) {
    mCursor = null;
    mPagerAdapter.notifyDataSetChanged();
  }


  private class MyPagerAdapter extends android.support.v4.app.FragmentStatePagerAdapter {

    public MyPagerAdapter(android.support.v4.app.FragmentManager fm) {
      super(fm);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
      super.setPrimaryItem(container, position, object);
    }

    @Override
    public android.support.v4.app.Fragment getItem(int position) {
      mCursor.moveToPosition(position);
      return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
    }

    @Override
    public int getCount() {
      return (mCursor != null) ? mCursor.getCount() : 0;
    }

  }
}
