/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.davdroid.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import at.bitfire.davdroid.App;
import at.bitfire.davdroid.BuildConfig;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;
import at.bitfire.davdroid.resource.LocalTaskList;

public class StartupDialogFragment extends DialogFragment {
    public static final String
            PREF_HINT_GOOGLE_PLAY_ACCOUNTS_REMOVED = "hint_google_play_accounts_removed",
            PREF_HINT_OPENTASKS_NOT_INSTALLED = "hint_opentasks_not_installed";

    private static final String ARGS_MODE = "mode";

    enum Mode {
        DEVELOPMENT_VERSION,
        FDROID_DONATE,
        GOOGLE_PLAY_ACCOUNTS_REMOVED,
        OPENTASKS_NOT_INSTALLED
    }

    public static StartupDialogFragment[] getStartupDialogs(Context context) {
        List<StartupDialogFragment> dialogs = new LinkedList<>();

        if (BuildConfig.VERSION_NAME.contains("-alpha") || BuildConfig.VERSION_NAME.contains("-beta"))
            dialogs.add(StartupDialogFragment.instantiate(Mode.DEVELOPMENT_VERSION));
        else {
            // store-specific information
            final String installedFrom = installedFrom(context);
            if (installedFrom == null || installedFrom.startsWith("org.fdroid"))
                dialogs.add(StartupDialogFragment.instantiate(Mode.FDROID_DONATE));

            else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP &&    // only on Android <5
                    "com.android.vending".equals(installedFrom) &&              // only when installed from Play Store
                    App.getPreferences().getBoolean(PREF_HINT_GOOGLE_PLAY_ACCOUNTS_REMOVED, true))      // and only when "Don't show again" hasn't been clicked yet
                dialogs.add(StartupDialogFragment.instantiate(Mode.GOOGLE_PLAY_ACCOUNTS_REMOVED));
        }

        // OpenTasks information
        if (!LocalTaskList.tasksProviderAvailable(context.getContentResolver()) &&
                App.getPreferences().getBoolean(PREF_HINT_OPENTASKS_NOT_INSTALLED, true))
            dialogs.add(StartupDialogFragment.instantiate(Mode.OPENTASKS_NOT_INSTALLED));

        Collections.reverse(dialogs);
        return dialogs.toArray(new StartupDialogFragment[dialogs.size()]);
    }

    public static StartupDialogFragment instantiate(Mode mode) {
        StartupDialogFragment frag = new StartupDialogFragment();
        Bundle args = new Bundle(1);
        args.putString(ARGS_MODE, mode.name());
        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(false);

        Mode mode = Mode.valueOf(getArguments().getString(ARGS_MODE));
        switch (mode) {
            case DEVELOPMENT_VERSION:
                return new AlertDialog.Builder(getActivity())
                        .setIcon(R.drawable.ic_launcher)
                        .setTitle(R.string.startup_development_version)
                        .setMessage(R.string.startup_development_version_message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setNeutralButton(R.string.startup_development_version_show_forums, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Constants.webUri.buildUpon().appendEncodedPath("forums/").build()));
                            }
                        })
                        .create();

            case FDROID_DONATE:
                return new AlertDialog.Builder(getActivity())
                        .setIcon(R.drawable.ic_launcher)
                        .setTitle(R.string.startup_donate)
                        .setMessage(R.string.startup_donate_message)
                        .setPositiveButton(R.string.startup_donate_now, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Constants.webUri.buildUpon().appendEncodedPath("donate/").build()));
                            }
                        })
                        .setNegativeButton(R.string.startup_donate_later, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .create();

            case GOOGLE_PLAY_ACCOUNTS_REMOVED:
                Drawable icon = null;
                try {
                    icon = getContext().getPackageManager().getApplicationIcon("com.android.vending").getCurrent();
                } catch (PackageManager.NameNotFoundException e) {
                    App.log.log(Level.WARNING, "Can't load Play Store icon", e);
                }
                return new AlertDialog.Builder(getActivity())
                        .setIcon(icon)
                        .setTitle(R.string.startup_google_play_accounts_removed)
                        .setMessage(R.string.startup_google_play_accounts_removed_message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setNeutralButton(R.string.startup_google_play_accounts_removed_more_info, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Constants.webUri.buildUpon().appendEncodedPath("faq/").build());
                                getContext().startActivity(intent);
                            }
                        })
                        .setNegativeButton(R.string.startup_dont_show_again, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                App.getPreferences().edit().putBoolean(PREF_HINT_GOOGLE_PLAY_ACCOUNTS_REMOVED, false).commit();
                            }
                        })
                        .create();

            case OPENTASKS_NOT_INSTALLED:
                return new AlertDialog.Builder(getActivity())
                        .setIcon(R.drawable.ic_alarm_on_dark)
                        .setTitle(R.string.startup_opentasks_not_installed)
                        .setMessage(R.string.startup_opentasks_not_installed_message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setNeutralButton(R.string.startup_opentasks_not_installed_install, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=org.dmfs.tasks"));
                                getContext().startActivity(intent);
                            }
                        })
                        .setNegativeButton(R.string.startup_dont_show_again, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                App.getPreferences().edit().putBoolean(PREF_HINT_OPENTASKS_NOT_INSTALLED, false).commit();
                            }
                        })
                        .create();
        }

        throw new IllegalArgumentException(/* illegal mode argument */);
    }

    private static String installedFrom(Context context) {
        try {
            return context.getPackageManager().getInstallerPackageName(context.getPackageName());
        } catch(IllegalArgumentException e) {
            return null;
        }
    }

}