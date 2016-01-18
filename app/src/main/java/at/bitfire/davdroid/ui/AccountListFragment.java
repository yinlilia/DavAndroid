/*
 * Copyright © 2013 – 2016 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.davdroid.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;
import at.bitfire.davdroid.syncadapter.ServiceDB;
import at.bitfire.davdroid.syncadapter.ServiceDB.*;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;

public class AccountListFragment extends ListFragment implements OnAccountsUpdateListener, LoaderManager.LoaderCallbacks<List<AccountListFragment.AccountInfo>>, AdapterView.OnItemClickListener {

    protected AccountManager accountManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setListAdapter(new AccountListAdapter(getContext()));

        accountManager = AccountManager.get(getContext());
        accountManager.addOnAccountsUpdatedListener(this, null, false);

        return inflater.inflate(R.layout.account_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getLoaderManager().initLoader(0, getArguments(), this);

        ListView list = getListView();
        list.setOnItemClickListener(this);
        list.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
    }

    @Override
    public void onDestroyView() {
        accountManager.removeOnAccountsUpdatedListener(this);
        super.onDestroyView();
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AccountInfo info = (AccountInfo)getListAdapter().getItem(position);

        Intent intent = new Intent(getContext(), AccountActivity.class);
        intent.putExtra(AccountActivity.EXTRA_ACCOUNT_NAME, info.account.name);
        startActivity(intent);
    }


    @Override
    public void onAccountsUpdated(Account[] accounts) {
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public Loader<List<AccountInfo>> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(getContext());
    }

    @Override
    public void onLoadFinished(Loader<List<AccountInfo>> loader, List<AccountInfo> accounts) {
        AccountListAdapter adapter = (AccountListAdapter)getListAdapter();
        if (adapter != null) {
            adapter.clear();
            adapter.addAll(accounts);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<AccountInfo>> loader) {
    }


    @RequiredArgsConstructor
    public static class AccountInfo {
        final Account account;
        boolean hasCardDAV, hasCalDAV;
    }


    static class AccountListAdapter extends ArrayAdapter<AccountInfo> {

        public AccountListAdapter(Context context) {
            super(context, R.layout.account_list_item);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            if (v == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                v = inflater.inflate(R.layout.account_list_item, parent, false);
            }

            AccountInfo info = getItem(position);

            TextView tv = (TextView)v.findViewById(R.id.account_name);
            tv.setText(info.account.name);

            tv = (TextView)v.findViewById(R.id.carddav);
            tv.setVisibility(info.hasCardDAV ? View.VISIBLE : View.GONE);

            tv = (TextView)v.findViewById(R.id.caldav);
            tv.setVisibility(info.hasCalDAV ? View.VISIBLE : View.GONE);

            return v;
        }

    }

    static class AccountLoader extends AsyncTaskLoader<List<AccountInfo>> {
        final AccountManager accountManager;
        final OpenHelper dbHelper;

        public AccountLoader(Context context) {
            super(context);
            accountManager = AccountManager.get(context);
            dbHelper = new OpenHelper(context);
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Override
        public List<AccountInfo> loadInBackground() {
            List<AccountInfo> accounts = new LinkedList<>();
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();

                for (Account account : accountManager.getAccountsByType(Constants.ACCOUNT_TYPE)) {
                    AccountInfo info = new AccountInfo(account);

                    // query services of this account
                    @Cleanup Cursor cursor = db.query(
                            Services._TABLE,
                            new String[] { Services.SERVICE },
                            Services.ACCOUNT_NAME + "=?", new String[] { account.name },
                            null, null, null);
                    while (cursor.moveToNext()) {
                        String service = cursor.getString(0);
                        if (Services.SERVICE_CARDDAV.equals(service))
                            info.hasCardDAV = true;
                        if (Services.SERVICE_CALDAV.equals(service))
                            info.hasCalDAV = true;
                    }

                    accounts.add(info);
                }
            } finally {
                dbHelper.close();
            }
            return accounts;
        }
    }

}