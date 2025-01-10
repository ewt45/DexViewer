package org.buildsmali.viewer

import android.animation.LayoutTransition
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.children
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.checkbox.MaterialCheckBox.STATE_CHECKED
import com.google.android.material.checkbox.MaterialCheckBox.STATE_INDETERMINATE
import com.google.android.material.checkbox.MaterialCheckBox.STATE_UNCHECKED
import org.buildsmali.viewer.dex.SmaliPackageData
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.dexbacked.DexBackedClassDef
import org.jf.dexlib2.dexbacked.DexBackedDexFile
import org.jf.dexlib2.iface.MultiDexContainer
import java.io.File
import java.io.IOException
import java.util.Stack
import java.util.stream.Collectors

class ViewActivity : AppCompatActivity() {
    private val smaliData = SmaliPackageData("L", "")

    //选中的类
    private val checkedClasses = HashSet<DexBackedClassDef>()

    //勾选/取消父包时, 会检查子包状态。如果出发子包状态变化，子包又会去检查父包形成循环。此时检查这个flag,
    // 如果为true,则不修改父包状态
    private var isCheckingSubContent = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()

        //TODO 每次进入页面都会刷新。以后也可以改成手动刷新？
        if (!Environment.isExternalStorageManager()) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        } else {
            try {
                readSmali()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 显示可提取的类
     */
    @Throws(IOException::class)
    private fun readSmali() {
        val providerPkg = "org.buildsmali.provider"
        val apkPath = getProviderApkPath(providerPkg)
        if (apkPath == null) {
            findViewById<TextView>(R.id.text).text = "未找到待解析的apk: $providerPkg"
            return
        }

        // DexBackedDexFile dexFile = DexFileFactory.loadDexFile(new File(apkPath), null);
        val container = DexFileFactory.loadDexContainer(File(apkPath), null)

        //读取dex,将smali类存入smaliData中
        container.dexEntryNames.flatMap { name: String ->
            getClassesFromDex(
                container,
                name
            )
        }.sorted().fold(smaliData) { acc, def ->
            addClassDefToList(def)
            acc
        }
            //.forEach { def -> addClassDefToList(def) }

        //显示
        val linear = findViewById<LinearLayout>(R.id.linear_list)
        removeStateListenerInItemsTree(linear)
        linear.removeAllViews()
        checkedClasses.clear()
        displayDataList(linear, smaliData)


        //全选
        findViewById<CheckBox>(R.id.check_all).setOnCheckedChangeListener { _: CompoundButton?, check: Boolean ->
            linear.children.forEach { child: View ->
                findCheckBoxOfPkg(child as ViewGroup)?.isChecked = check
            }
        }

        //保存路径
        val dstPath = getPreferences(MODE_PRIVATE).getString("DST_PATH", "Android/测试生成.dex")!!
        findViewById<EditText>(R.id.edit_path).setText(dstPath)


        //写入新的dex，参考DexPool.writeTo
        findViewById<View>(R.id.btn_export).setOnClickListener { v: View ->
            AlertDialog.Builder(v.context)
                .setMessage(checkedClasses.stream()
                    .sorted()
                    .map { obj: DexBackedClassDef -> obj.type }
                    .collect(Collectors.joining("\n")))
                .show()
        }

        //        ((TextView) findViewById(R.id.text)).setText(apkPath);
    }

    /**
     * 给定一个包，创建此包下的直接子包和直接子类的视图
     */
    private fun displayDataList(root: LinearLayout, pkgData: SmaliPackageData) {
        root.layoutTransition = LayoutTransition()
        //新建包的时候，底下附带一个容器，用于包内的子包和子类的显隐
        val clsIcon = AppCompatResources.getDrawable(this, R.drawable.ic_class)
        val pkgIcon = AppCompatResources.getDrawable(this, R.drawable.ic_folder)
        val currAllClasses = pkgData.allSubClasses
        val currPkgCheck = findCheckBoxOfPkg(root.parent as ViewGroup)
        //子包
        pkgData.subPackages.forEach { (name: String, pkg: SmaliPackageData) ->
            val container = layoutInflater.inflate(R.layout.item_smali_pkg_or_class, root, false)
            val tv = container.findViewById<TextView>(R.id.text)
            val image = container.findViewById<ImageView>(R.id.icon)
            val check = container.findViewById<MaterialCheckBox>(R.id.check)
            val subFrame = container.findViewById<LinearLayout>(R.id.sub_frame)
            val subAllClasses = pkg.allSubClasses

            tv.text = name
            image.setImageDrawable(pkgIcon)

            // 点击时折叠或展开
            tv.setOnClickListener {
                if (subFrame.childCount != 0) {
                    removeStateListenerInItemsTree(subFrame)
                    subFrame.removeAllViews()
                } else {
                    displayDataList(subFrame, pkg)
                }
            }

            //勾选
            setPkgCheckedState(subAllClasses, check)
            check.isChecked = checkedClasses.containsAll(subAllClasses)
            check.addOnCheckedStateChangedListener { _: MaterialCheckBox?, state: Int ->
                when (state) {
                    STATE_CHECKED -> checkedClasses.addAll(subAllClasses)
                    STATE_UNCHECKED -> checkedClasses.removeAll(subAllClasses.toSet())
                    //用户点击不会变成半勾选，肯定是子包在修改父包状态，就不用再检查子包状态了
                    STATE_INDETERMINATE -> {
                        currPkgCheck?.checkedState = STATE_INDETERMINATE
                        return@addOnCheckedStateChangedListener
                    }
                }
                //父包变化后检查子包勾选状态
                isCheckingSubContent = true
                subFrame.children.forEach {
                    findCheckBoxOfPkg(it as ViewGroup)?.checkedState = state
                }
                isCheckingSubContent = false

                //子包变化后检查父包勾选状态
                setPkgCheckedState(currAllClasses, currPkgCheck)
            }


            container.tag = pkg
            root.addView(container)

            // 自动展开test包
            if (pkg.fullPkgName == "Ltest/") {
                tv.performClick()
            }
        }

        //子类
        pkgData.classes.forEach { (name: String, cls: DexBackedClassDef) ->
            val container = layoutInflater.inflate(R.layout.item_smali_pkg_or_class, root, false)
            val tv = container.findViewById<TextView>(R.id.text)
            val image = container.findViewById<ImageView>(R.id.icon)
            val check = container.findViewById<MaterialCheckBox>(R.id.check)

            tv.text = name
            image.setImageDrawable(clsIcon)

            // 点击时选中
            tv.setOnClickListener { check.performClick() }
            check.isChecked = checkedClasses.contains(cls)
            check.setOnCheckedChangeListener { _: CompoundButton?, checked: Boolean ->
                if (checked) checkedClasses.add(cls)
                else checkedClasses.remove(cls)
                //子类变化后检查父包勾选状态
                if (!isCheckingSubContent) {
                    setPkgCheckedState(currAllClasses, currPkgCheck)
                }
            }

            // 长按时菜单
            val popupMenu = PopupMenu(this, tv)
            popupMenu.menu.add("选中该类及其内部类").setOnMenuItemClickListener {
                val nextState = !check.isChecked
                check.isChecked = nextState
                //从子类开始，将和自己同名且带$的一并勾选/取消勾选
                val offset = pkgData.subPackages.size
                for (i in 0 until pkgData.classes.size) {
                    val broContainer = root.getChildAt(i + offset) as ViewGroup
                    val broCls = broContainer.tag as DexBackedClassDef
                    // 找到外层类名
                    val prefix = cls.type.split("\\$".toRegex())[0].replace(";", "")
                    // 待比较类名（不包含分号）
                    val broType = broCls.type.replace(";", "")
                    // 如果当前选择的是内部类，注意外部类也要被勾选。
                    if (broType == prefix || broType.startsWith("$prefix\$")) {
                        findCheckBoxOfPkg(broContainer)?.isChecked = nextState
                    }
                }
                true
            }
            tv.setOnLongClickListener {
                popupMenu.show()
                true
            }

            container.tag = cls
            root.addView(container)
        }
    }

    /**
     * 在移除包含checkbox的视图时调用（checkbox添加了状态监听器）
     * 遍历移除列表中的每一项中的check的state监听器。每一项可能有子列表，也会遍历
     * @param root 从此视图的子视图开始寻找checkbox
     */
    private fun removeStateListenerInItemsTree(root: LinearLayout) {
        val listViews = Stack<ViewGroup>()
        listViews.push(root)
        while (!listViews.empty()) {
            val listView = listViews.pop()
            listView.children.forEach { child ->
                findCheckBoxOfPkg(child as ViewGroup)?.clearOnCheckedStateChangedListeners()
                findSubFrameOfPkg(listView)?.let { listViews.push(it) }
            }
        }
    }

    /**
     * 给定一个包的全部子类和其对应的复选框，根据选中情况 设置复选框状态
     */
    private fun setPkgCheckedState(
        allSubClasses: List<DexBackedClassDef>,
        check: MaterialCheckBox?
    ) {
        if (check == null) return
        var count = 0
        for (cls in allSubClasses) if (checkedClasses.contains(cls)) count++

        check.checkedState =
            if (count > 0 && count == allSubClasses.size) STATE_CHECKED
            else if (count > 0) STATE_INDETERMINATE
            else STATE_UNCHECKED
    }

    /**
     * 给定一个包对应的item布局，在其中找到checkbox并返回
     */
    private fun findCheckBoxOfPkg(group: ViewGroup): MaterialCheckBox? {
        for (child in group.children) if (child is MaterialCheckBox) return child
        return null
    }

    /**
     * 给定一个包对应的item布局，在其中找到subFrame并返回
     */
    private fun findSubFrameOfPkg(group: ViewGroup): LinearLayout? {
        for (child in group.children) if (child.id == R.id.sub_frame) return child as LinearLayout
        return null
    }

    /**
     * 获取 对应名称的dex中的全部类
     */
    private fun getClassesFromDex(
        container: MultiDexContainer<out DexBackedDexFile>,
        name: String
    ): Set<DexBackedClassDef> {
        try {
            return container.getEntry(name)!!.dexFile!!.classes
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    /**
     * 将一个类添加到SmaliPackageData中
     */
    private fun addClassDefToList(def: DexBackedClassDef) {
        //Ltest/Test5;
        val type = def.type
        val splits = type.substring(1, type.length - 1).split("/".toRegex())
        var currPkg = smaliData
        for (i in splits.indices)
            when (i) {
                splits.size - 1 -> currPkg.addClassDef(splits[i], def) // 将类添加到对应包下
                else -> currPkg = currPkg.getSubPackage(splits[i]) // 寻找包名
            }
    }

    /**
     * 获取 provider的apk的本地路径。
     *
     * @return 返回apk本地路径。找不到时返回null
     */
    private fun getProviderApkPath(pkg: String): String? {
        try {
            //需要在manifest指定对应包名的query,才能获取到info
            return packageManager.getApplicationInfo(pkg, 0).sourceDir
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return null // 如果包名不存在，返回 null
        }
    }
}