/*
 * Copyright (C) 2024 LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.spoofing

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.UserInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView

import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.android.internal.util.lunaris.HideGmsUtils

import org.lineageos.lineageparts.R

class HideGms : Fragment(R.layout.hide_gms_layout) {

    private lateinit var activityManager: ActivityManager
    private lateinit var packageManager: PackageManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppListAdapter
    private lateinit var packageList: List<PackageInfo>
    private lateinit var userManager: UserManager
    private lateinit var userInfos: List<UserInfo>

    private var searchText = ""
    private var customFilter: ((PackageInfo) -> Boolean)? = null
    private var comparator: ((PackageInfo, PackageInfo) -> Int)? = null
    private var hideGmsUtils: HideGmsUtils = HideGmsUtils()
    private var showSystem = false
    private var optionsMenu: Menu? = null

    override fun onStart() {
        super.onStart()
        updateOptionsMenu()
        val host = activity
        host?.invalidateOptionsMenu()
    }

    @SuppressLint("QueryPermissionsNeeded")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        requireActivity().setTitle(R.string.hide_gms_title)
        activityManager = requireContext().getSystemService(ActivityManager::class.java)
        packageManager = requireContext().packageManager
        packageList = packageManager.getInstalledPackages(PackageManager.MATCH_ANY_USER)
        userManager = UserManager.get(requireContext())
        userInfos = userManager.getUsers()
        for (info in userInfos) {
            hideGmsUtils.setApps(requireContext(), info.id)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = AppListAdapter()
        recyclerView = view.findViewById<RecyclerView>(R.id.apps_list).also {
            it.layoutManager = LinearLayoutManager(context)
            it.adapter = adapter
        }
        refreshList()
    }

    /**
     * @return an initial list of packages that should appear as selected.
     */
    private fun getInitialCheckedList(): List<String> {
        val flattenedString = Settings.Secure.getString(
            requireContext().contentResolver, Settings.Secure.HIDE_GMS
        )
        return flattenedString?.takeIf {
            it.isNotBlank()
        }?.split(",")?.toList() ?: emptyList()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val activity = activity ?: return
        optionsMenu = menu
        inflater.inflate(R.menu.hide_gms_menu, menu)

        menu.findItem(R.id.show_system).setVisible(showSystem)
        menu.findItem(R.id.hide_system).setVisible(!showSystem)

        val searchMenuItem = menu.findItem(R.id.search) as MenuItem
        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                ViewCompat.setNestedScrollingEnabled(recyclerView, false)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                ViewCompat.setNestedScrollingEnabled(recyclerView, true)
                return true
            }
        })
        val searchView = searchMenuItem.actionView as SearchView
        searchView.queryHint = getString(R.string.search)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String) = false

            override fun onQueryTextChange(newText: String): Boolean {
                searchText = newText
                refreshList()
                return true
            }
        })

        updateOptionsMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val i = item.itemId
        if (i == R.id.show_system || i == R.id.hide_system) {
            showSystem = !showSystem
            refreshList()
        }
        updateOptionsMenu()
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        updateOptionsMenu()
    }

    override fun onDestroyOptionsMenu() {
        optionsMenu = null
    }

    private fun updateOptionsMenu() {
        val menu = optionsMenu ?: return
        menu.findItem(R.id.show_system).setVisible(!showSystem)
        menu.findItem(R.id.hide_system).setVisible(showSystem)
    }

    /**
     * Called when user selects an item.
     *
     * @param packageName the package name of the selected app.
     * @param isChecked whether the item is now checked.
     */
    private fun onListUpdate(packageName: String, isChecked: Boolean) {
        if (packageName.isBlank()) return
        for (info in userInfos) {
            if (isChecked) {
                hideGmsUtils.addApp(requireContext(), packageName, info.id)
            } else {
                hideGmsUtils.removeApp(requireContext(), packageName, info.id)
            }
        }
        try {
            activityManager.forceStopPackage(packageName)
        } catch (ignored: Exception) {
        }
    }

    private fun refreshList() {
        var list = packageList.filter {
            if (!showSystem) {
                !(it.applicationInfo?.isSystemApp() ?: true)
                && !(it.applicationInfo?.packageName?.contains("android.settings") ?: true)
            } else {
                !(it.applicationInfo?.packageName?.contains("android.settings") ?: true)
                && !(it.applicationInfo?.isResourceOverlay() ?: true)
            }
        }.filter {
            getLabel(it).contains(searchText, true)
        }
        list = customFilter?.let { customFilter ->
            list.filter {
                customFilter(it)
            }
        } ?: list
        list = comparator?.let {
            list.sortedWith(it)
        } ?: list.sortedWith { a, b ->
            getLabel(a).compareTo(getLabel(b))
        }
        if (::adapter.isInitialized) adapter.submitList(list.map { appInfoFromPackageInfo(it) })
    }

    private fun appInfoFromPackageInfo(packageInfo: PackageInfo) =
        AppInfo(
            packageInfo.packageName,
            getLabel(packageInfo),
            packageInfo.applicationInfo?.loadIcon(packageManager) ?: context?.getDrawable(R.drawable.ic_launcher_foreground)!!
        )

    private fun getLabel(packageInfo: PackageInfo) =
        packageInfo.applicationInfo?.loadLabel(packageManager)?.toString() ?: packageInfo.packageName

    private inner class AppListAdapter : ListAdapter<AppInfo, AppListViewHolder>(itemCallback) {
        private val selectedIndices = mutableSetOf<Int>()
        private var initialList = getInitialCheckedList().toMutableList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            AppListViewHolder(layoutInflater.inflate(
                R.layout.hide_gms_list_item, parent, false))

        override fun onBindViewHolder(holder: AppListViewHolder, position: Int) {
            getItem(position).let {
                holder.label.text = it.label
                holder.packageName.text = it.packageName
                holder.icon.setImageDrawable(it.icon)
                holder.itemView.setOnClickListener { _ ->
                    if (selectedIndices.contains(position)) {
                        selectedIndices.remove(position)
                        onListUpdate(holder.packageName.text.toString(), false)
                    } else {
                        selectedIndices.add(position)
                        onListUpdate(holder.packageName.text.toString(), true)
                    }
                    notifyItemChanged(position)
                }
                if (initialList.contains(it.packageName)) {
                    initialList.remove(it.packageName)
                    selectedIndices.add(position)
                }
                holder.checkBox.isChecked = selectedIndices.contains(position)
            }
        }

        override fun submitList(list: List<AppInfo>?) {
            initialList = getInitialCheckedList().toMutableList()
            selectedIndices.clear()
            super.submitList(list)
        }
    }

    private class AppListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.icon)
        val label: TextView = itemView.findViewById(R.id.label)
        val packageName: TextView = itemView.findViewById(R.id.packageName)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
    }

    private data class AppInfo(
        val packageName: String,
        val label: String,
        val icon: Drawable,
    )

    companion object {
        private val itemCallback = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(oldInfo: AppInfo, newInfo: AppInfo) =
                oldInfo.packageName == newInfo.packageName

            override fun areContentsTheSame(oldInfo: AppInfo, newInfo: AppInfo) =
                oldInfo == newInfo
        }
    }
}
