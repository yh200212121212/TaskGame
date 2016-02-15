/*
 * Copyright (C) 2015 Federico Iosue (federico.iosue@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.fred.taskgame.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.games.Games;
import com.google.android.gms.games.quest.Quests;

import net.fred.taskgame.R;
import net.fred.taskgame.activity.CategoryActivity;
import net.fred.taskgame.activity.MainActivity;
import net.fred.taskgame.activity.SettingsActivity;
import net.fred.taskgame.model.Category;
import net.fred.taskgame.model.NavigationItem;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.model.adapters.NavDrawerAdapter;
import net.fred.taskgame.model.adapters.NavDrawerCategoryAdapter;
import net.fred.taskgame.model.listeners.OnPermissionRequestedListener;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.DbHelper;
import net.fred.taskgame.utils.Navigation;
import net.fred.taskgame.utils.PermissionsHelper;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.utils.ThrottledFlowContentObserver;
import net.fred.taskgame.utils.UiUtils;
import net.fred.taskgame.view.NonScrollableListView;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;


/**
 * Fragment used for managing interactions for and presentation of a navigation
 * drawer. See the <a href=
 * "https://developer.android.com/design/patterns/navigation-drawer.html#Interaction"
 * > design guidelines</a> for a complete explanation of the behaviors
 * implemented here.
 */
public class NavigationDrawerFragment extends Fragment {

    private static final int REQUEST_CODE_CATEGORY = 2;

    String[] mNavigationArray;
    TypedArray mNavigationIconsArray;
    TypedArray mNavigationIconsSelectedArray;
    private NonScrollableListView mDrawerList;
    private NonScrollableListView mDrawerCategoriesList;
    private View mCategoriesListHeader;
    private View mSettingsView, mSettingsViewCat;
    private MainActivity mActivity;
    private TextView mCurrentPoints;

    private ThrottledFlowContentObserver mContentObserver = new ThrottledFlowContentObserver(100) {
        @Override
        public void onChangeThrottled() {
            init();
        }
    };

    private SharedPreferences.OnSharedPreferenceChangeListener mCurrentPointsObserver = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (mCurrentPoints != null && PrefUtils.PREF_CURRENT_POINTS.equals(key)) {
                mCurrentPoints.setText(String.valueOf(PrefUtils.getLong(PrefUtils.PREF_CURRENT_POINTS, 0)));
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(false);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // registers for callbacks from the specified tables
        mContentObserver.registerForContentChanges(inflater.getContext(), Task.class);
        mContentObserver.registerForContentChanges(inflater.getContext(), Category.class);

        PrefUtils.registerOnPrefChangeListener(mCurrentPointsObserver);

        View view = inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onDestroyView() {
        mContentObserver.unregisterForContentChanges(getView().getContext());
        PrefUtils.unregisterOnPrefChangeListener(mCurrentPointsObserver);

        super.onDestroyView();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = (MainActivity) getActivity();

        init();
    }

    /**
     * Initialization of compatibility navigation drawer
     */
    private void init() {

        mCurrentPoints = (TextView) getView().findViewById(R.id.currentPoints);
        mCurrentPoints.setText(String.valueOf(PrefUtils.getLong(PrefUtils.PREF_CURRENT_POINTS, 0)));

        buildMainMenu();
        buildCategoriesMenu();
    }

    private void buildCategoriesMenu() {
        // Retrieves data to fill tags list
        List<Category> categories = DbHelper.getCategories();

        mDrawerCategoriesList = (NonScrollableListView) getView().findViewById(R.id.drawer_tag_list);

        // Inflater used for header and footer
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        // Inflation of header view
        if (mCategoriesListHeader == null) {
            mCategoriesListHeader = inflater.inflate(R.layout.drawer_category_list_header, null);
        }

        // Inflation of Settings view
        if (mSettingsView == null) {
            mSettingsView = ((ViewStub) getActivity().findViewById(R.id.settings_placeholder)).inflate();
        }
        mSettingsView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(settingsIntent);
            }
        });

        if (mSettingsViewCat == null) {
            mSettingsViewCat = inflater.inflate(R.layout.drawer_category_list_footer, null);
        }
        mSettingsViewCat.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(settingsIntent);
            }
        });

        if (mDrawerCategoriesList.getAdapter() == null) {
            mDrawerCategoriesList.addFooterView(mSettingsViewCat);
        }
        if (categories.size() == 0) {
            mCategoriesListHeader.setVisibility(View.GONE);
            mSettingsViewCat.setVisibility(View.GONE);
            mSettingsView.setVisibility(View.VISIBLE);
        } else if (categories.size() > 0) {
            mSettingsView.setVisibility(View.GONE);
            mCategoriesListHeader.setVisibility(View.VISIBLE);
            mSettingsViewCat.setVisibility(View.VISIBLE);
        }

        mDrawerCategoriesList.setAdapter(new NavDrawerCategoryAdapter(mActivity, categories));

        // Sets click events
        mDrawerCategoriesList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

                // Commits pending deletion or archiviation
                mActivity.commitPending();

                Object item = mDrawerCategoriesList.getAdapter().getItem(position);
                // Ensuring that clicked item is not the ListView header
                if (item != null) {
                    Category tag = (Category) item;
                    selectNavigationItem(mDrawerCategoriesList, position);
                    mActivity.updateNavigation(String.valueOf(tag.id));
                    mDrawerCategoriesList.setItemChecked(position, true);
                    if (mDrawerList != null)
                        mDrawerList.setItemChecked(0, false); // Forces redraw
                    mActivity.initTasksList(mActivity.getIntent());
                }
            }
        });

        // Sets long click events
        mDrawerCategoriesList.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View view, int position, long arg3) {
                if (mDrawerCategoriesList.getAdapter() != null) {
                    Object item = mDrawerCategoriesList.getAdapter().getItem(position);
                    // Ensuring that clicked item is not the ListView header
                    if (item != null) {
                        editCategory((Category) item);
                    }
                } else {
                    UiUtils.showMessage(getActivity(), R.string.category_deleted);
                }
                return true;
            }
        });

        mDrawerCategoriesList.justifyListViewHeightBasedOnChildren();
    }


    private void buildMainMenu() {
        // Sets the adapter for the MAIN navigation list view
        mDrawerList = (NonScrollableListView) getView().findViewById(R.id.drawer_nav_list);
        mNavigationArray = getResources().getStringArray(R.array.navigation_list);
        mNavigationIconsArray = getResources().obtainTypedArray(R.array.navigation_list_icons);
        mNavigationIconsSelectedArray = getResources().obtainTypedArray(R.array.navigation_list_icons_selected);

        final List<NavigationItem> items = new ArrayList<>();
        for (int i = 0; i < mNavigationArray.length; i++) {
            if (!checkSkippableItem(i)) {
                NavigationItem item = new NavigationItem(i, mNavigationArray[i], mNavigationIconsArray.getResourceId(i,
                        0), mNavigationIconsSelectedArray.getResourceId(i, 0));
                items.add(item);
            }
        }
        mDrawerList.setAdapter(new NavDrawerAdapter(mActivity, items));

        // Sets click events
        mDrawerList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                mActivity.commitPending();

                String navigation = getResources().getStringArray(R.array.navigation_list_codes)[items.get(position).getArrayIndex()];
                selectNavigationItem(mDrawerList, position);
                mActivity.updateNavigation(navigation);
                mDrawerList.setItemChecked(position, true);
                if (mDrawerCategoriesList != null)
                    mDrawerCategoriesList.setItemChecked(0, false); // Called to force redraw
                // Reset intent
                mActivity.getIntent().setAction(Intent.ACTION_MAIN);

                // Call method to update tasks list
                mActivity.initTasksList(mActivity.getIntent());
            }
        });

        mDrawerList.justifyListViewHeightBasedOnChildren();
    }


    private boolean checkSkippableItem(int i) {
        boolean skippable = false;
        boolean dynamicMenu = PrefUtils.getBoolean(PrefUtils.PREF_DYNAMIC_MENU, true);
        switch (i) {
            case Navigation.TRASH:
                if (DbHelper.getTasksTrashed().size() == 0 && dynamicMenu)
                    skippable = true;
                break;
        }
        return skippable;
    }


    /**
     * Swaps fragments in the main content view
     */
    private void selectNavigationItem(ListView list, int position) {
        Object itemSelected = list.getItemAtPosition(position);
        if (itemSelected.getClass().isAssignableFrom(NavigationItem.class)) {
            mActivity.getSupportActionBar().setTitle(((NavigationItem) itemSelected).getText());
        } else { // Is a category
            mActivity.getSupportActionBar().setTitle(((Category) itemSelected).name);
        }
        // Navigation drawer is closed after a while to avoid lag
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mActivity.getDrawerLayout().closeDrawer(GravityCompat.START);
            }
        }, 200);
    }

    /**
     * Categories addition and editing
     */
    public void editCategory(Category category) {
        Intent categoryIntent = new Intent(getActivity(), CategoryActivity.class);
        categoryIntent.putExtra(Constants.INTENT_CATEGORY, Parcels.wrap(category));
        startActivityForResult(categoryIntent, REQUEST_CODE_CATEGORY);
    }

    private MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }


    @Override
    public void onActivityResult(int requestCode, final int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
            case REQUEST_CODE_CATEGORY:
                // Dialog retarded to give time to activity's views of being
                // completely initialized
                // The dialog style is chosen depending on result code
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        UiUtils.showMessage(getActivity(), R.string.category_saved);
                        break;
                    case Activity.RESULT_FIRST_USER:
                        UiUtils.showMessage(getActivity(), R.string.category_deleted);
                        break;
                    default:
                        break;
                }

                break;

            default:
                break;
        }
    }

    @OnClick(R.id.loginBtn)
    public void onLoginBtnClicked(View v) {
        PermissionsHelper.requestPermission(getActivity(), Manifest.permission.GET_ACCOUNTS,
                R.string.permission_get_account, new OnPermissionRequestedListener() {
                    @Override
                    public void onPermissionGranted() {
                        getMainActivity().beginUserInitiatedSignIn();
                    }
                });
    }

    @OnClick(R.id.questsBtn)
    public void onQuestsBtnClicked(View v) {
        try {
            startActivityForResult(Games.Quests.getQuestsIntent(getMainActivity().getApiClient(), Quests.SELECT_ALL_QUESTS), 0);
        } catch (Exception ignored) {
        }
    }

    @OnClick(R.id.leaderboardBtn)
    public void onLeaderboardBtnClicked(View v) {
        try {
            startActivityForResult(Games.Leaderboards.getLeaderboardIntent(getMainActivity().getApiClient(), Constants.LEADERBOARD_ID), 0);
        } catch (Exception ignored) {
        }
    }
}