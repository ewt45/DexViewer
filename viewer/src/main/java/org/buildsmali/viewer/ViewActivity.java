package org.buildsmali.viewer;

import static com.google.android.material.checkbox.MaterialCheckBox.STATE_CHECKED;
import static com.google.android.material.checkbox.MaterialCheckBox.STATE_INDETERMINATE;
import static com.google.android.material.checkbox.MaterialCheckBox.STATE_UNCHECKED;

import android.animation.LayoutTransition;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.checkbox.MaterialCheckBox;

import org.buildsmali.viewer.dex.SmaliPackageData;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.MultiDexContainer;
import org.jf.dexlib2.writer.io.FileDataStore;
import org.jf.dexlib2.writer.pool.DexPool;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

public class ViewActivity extends AppCompatActivity {
    private final SmaliPackageData smaliData = new SmaliPackageData("L", "");
    //选中的类
    private final HashSet<DexBackedClassDef> checkedClasses = new HashSet<>();
    //勾选/取消父包时, 会检查子包状态。如果出发子包状态变化，子包又会去检查父包形成循环。此时检查这个flag,
    // 如果为true,则不修改父包状态
    private boolean isCheckingSubContent = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //TODO 每次进入页面都会刷新。以后也可以改成手动刷新？
        if (!Environment.isExternalStorageManager()) {
            startActivity(new Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
        } else {
            try {
                readSmali();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 显示可提取的包名
     */
    private void readSmali() throws IOException {
        String providerPkg = "org.buildsmali.provider";
        String apkPath = getProviderApkPath(providerPkg);
        if (apkPath == null) {
            ((TextView) findViewById(R.id.text)).setText("未找到待解析的apk: " + providerPkg);
            return;
        }

//        DexBackedDexFile dexFile = DexFileFactory.loadDexFile(new File(apkPath), null);
        MultiDexContainer<? extends DexBackedDexFile> container =
                DexFileFactory.loadDexContainer(new File(apkPath), null);

        //读取dex,将smali类存入smaliData中
        container.getDexEntryNames().stream()
                .flatMap(name -> getClassesFromDex(container, name).stream())
//                .filter(cls -> cls.getType().startsWith("Ltest/"))
                .sorted()
                .forEach(this::addClassDefToList);

        //显示
        LinearLayout linear = findViewById(R.id.linear_list);
        removeStateListenerInItemsTree(linear);
        linear.removeAllViews();
        checkedClasses.clear();
        displayDataList(linear, smaliData);


        //全选
        ((CheckBox) findViewById(R.id.check_all)).setOnCheckedChangeListener((btn, check) -> {
            for (int i = 0; i < linear.getChildCount(); i++) {
                ((CheckBox) linear.getChildAt(i)).setChecked(check);
            }
        });

        //保存路径
        String dstPath = getPreferences(MODE_PRIVATE)
                .getString("DST_PATH", "Android/测试生成.dex");
        EditText editPath = findViewById(R.id.edit_path);
        editPath.setText(dstPath);


        //写入新的dex，参考DexPool.writeTo
        findViewById(R.id.btn_export).setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setMessage(checkedClasses.stream()
                            .sorted()
                            .map(DexBackedClassDef::getType)
                            .collect(Collectors.joining("\n")))
                    .show();
//            try {
//                DexPool dexPool = new DexPool(container.getEntry("classes.dex").getDexFile().getOpcodes());
//                for (int i = 0; i < linear.getChildCount(); i++) {
//                    CheckBox check = (CheckBox) linear.getChildAt(i);
//                    if (check.isChecked())
//                        dexPool.internClass((ClassDef) check.getTag());
//                }
////        selClsList.forEach(dexPool::internClass);
//                String currDst = editPath.getText().toString();
//                getPreferences(MODE_PRIVATE).edit().putString("DST_PATH", currDst).apply();
//                File dstFile = new File(Environment.getExternalStorageDirectory(), currDst);
//                dexPool.writeTo(new FileDataStore(dstFile));
//
//                Toast.makeText(this, "导出成功", Toast.LENGTH_SHORT).show();
//            } catch (Exception e) {
//                e.printStackTrace();
//                Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show();
//
//            }
        });

//        ((TextView) findViewById(R.id.text)).setText(apkPath);
    }

    /**
     * 给定一个包，创建此包下的直接子包和直接子类的视图
     */
    private void displayDataList(LinearLayout root, SmaliPackageData pkgData) {
        root.setLayoutTransition(new LayoutTransition());
        //新建包的时候，底下附带一个容器，用于包内的子包和子类的显隐
        Drawable clsIcon = AppCompatResources.getDrawable(this, R.drawable.ic_class);
        Drawable pkgIcon = AppCompatResources.getDrawable(this, R.drawable.ic_folder);
        List<DexBackedClassDef> currAllClasses = pkgData.getAllClasses();
        MaterialCheckBox currPkgCheck = findCheckBoxOfPkg((ViewGroup) root.getParent());
        //子包
        pkgData.getSubPackages().forEach((String name, SmaliPackageData pkg) -> {
            View container = getLayoutInflater().inflate(R.layout.item_smali_pkg_or_class, root, false);
            TextView tv = container.findViewById(R.id.text);
            ImageView image = container.findViewById(R.id.icon);
            MaterialCheckBox check = container.findViewById(R.id.check);
            LinearLayout subFrame = container.findViewById(R.id.sub_frame);
            List<DexBackedClassDef> subAllClasses = pkg.getAllClasses();

            tv.setText(name);
            image.setImageDrawable(pkgIcon);

            // 点击时折叠或展开
            tv.setOnClickListener(v -> {
                if (subFrame.getChildCount() != 0) {
                    removeStateListenerInItemsTree(subFrame);
                    subFrame.removeAllViews();
                } else {
                    displayDataList(subFrame, pkg);
                }
            });

            //勾选
            setPkgCheckedState(subAllClasses, check);
            check.setChecked(checkedClasses.containsAll(subAllClasses));
            check.addOnCheckedStateChangedListener((v, state) -> {
                if (state == STATE_CHECKED)
                    checkedClasses.addAll(subAllClasses);
                else if (state == STATE_UNCHECKED)
                    subAllClasses.forEach(checkedClasses::remove);
                else {
                    //用户点击不会变成半勾选，肯定是子包在修改父包状态，就不用再检查子包状态了
                    if (currPkgCheck != null)
                        currPkgCheck.setCheckedState(STATE_INDETERMINATE);
                    return;
                }

                //父包变化后检查子包勾选状态
                isCheckingSubContent = true;
                for (int i=0; i<subFrame.getChildCount(); i++) {
                    MaterialCheckBox subCheck = findCheckBoxOfPkg((ViewGroup) subFrame.getChildAt(i));
                    if (subCheck != null) subCheck.setCheckedState(state);
                }
                isCheckingSubContent = false;

                //子包变化后检查父包勾选状态
                setPkgCheckedState(currAllClasses, currPkgCheck);
            });


            container.setTag(pkg);
            root.addView(container);

            // 自动展开test包
            if (pkg.getFullPkgName().equals("Ltest/")) {
                tv.performClick();
            }
        });

        //子类
        pkgData.getClasses().forEach((String name, DexBackedClassDef cls) -> {
            View container = getLayoutInflater().inflate(R.layout.item_smali_pkg_or_class, root, false);
            TextView tv = container.findViewById(R.id.text);
            ImageView image = container.findViewById(R.id.icon);
            MaterialCheckBox check = container.findViewById(R.id.check);

            tv.setText(name);
            image.setImageDrawable(clsIcon);

            // 点击时选中
            tv.setOnClickListener(v -> check.performClick());
            check.setChecked(checkedClasses.contains(cls));
            check.setOnCheckedChangeListener((v, checked) -> {
                if (checked)
                    checkedClasses.add(cls);
                else
                    checkedClasses.remove(cls);

                //子类变化后检查父包勾选状态
                if (!isCheckingSubContent) {
                    setPkgCheckedState(currAllClasses, currPkgCheck);
                }
            });

            // 长按时菜单
            PopupMenu popupMenu = new PopupMenu(this, tv);
            popupMenu.getMenu().add("选中该类及其内部类").setOnMenuItemClickListener(item -> {
                boolean nextState = !check.isChecked();
                check.setChecked(nextState);
                //从子类开始，将和自己同名且带$的一并勾选/取消勾选
                int offset = pkgData.getSubPackages().size();
                for (int i = 0; i < pkgData.getClasses().size(); i++) {
                    ViewGroup broContainer = (ViewGroup) root.getChildAt(i + offset);
                    DexBackedClassDef broCls = (DexBackedClassDef) broContainer.getTag();
                    // 找到外层类名
                    String prefix = cls.getType().split("\\$")[0].replace(";","");
                    // 待比较类名（不包含分号）
                    String broType = broCls.getType().replace(";", "");
                    // 如果当前选择的是内部类，注意外部类也要被勾选。
                    if (broType.equals(prefix) || broType.startsWith(prefix + "$")) {
                        MaterialCheckBox broCheck = findCheckBoxOfPkg(broContainer);
                        if (broCheck != null) broCheck.setChecked(nextState);
                    }
                }
                return true;
            });
            tv.setOnLongClickListener(v -> {
                popupMenu.show();
                return true;
            });

            container.setTag(cls);
            root.addView(container);
        });
    }

    /**
     * 在移除包含checkbox的视图时调用（checkbox添加了状态监听器）
     * 遍历移除列表中的每一项中的check的state监听器。每一项可能有子列表，也会遍历
     */
    private void removeStateListenerInItemsTree(LinearLayout root) {
        Stack<ViewGroup> listViews = new Stack<>();
        listViews.push(root);
        while (!listViews.empty()) {
            ViewGroup listView = listViews.pop();
            for (int i = 0; i < listView.getChildCount(); i++) {
                MaterialCheckBox check = findCheckBoxOfPkg(listView);
                if (check != null) check.clearOnCheckedStateChangedListeners();
                LinearLayout subFrame = findSubFrameOfPkg(listView);
                if (subFrame != null) listViews.push(subFrame);
            }
        }
    }

    /**
     * 给定一个包的全部子类和其对应的复选框，根据选中情况 设置复选框状态
     */
    private void setPkgCheckedState(
            List<DexBackedClassDef> allSubClasses,
            @Nullable MaterialCheckBox check
    ) {
        if (check == null)
            return;
        int count = 0;
        int checkedState;
        for (DexBackedClassDef cls : allSubClasses)
            if (checkedClasses.contains(cls))
                count ++;

        if (count > 0 && count == allSubClasses.size())
            checkedState = STATE_CHECKED;
        else if (count > 0)
            checkedState = STATE_INDETERMINATE;
        else
            checkedState = STATE_UNCHECKED;
        check.setCheckedState(checkedState);
    }

    /**
     * 给定一个包对应的item布局，在其中找到checkbox并返回
     */
    @Nullable
    private MaterialCheckBox findCheckBoxOfPkg(ViewGroup group) {
        for (int i = 0; i<group.getChildCount(); i++) {
            if (group.getChildAt(i) instanceof MaterialCheckBox)
                return (MaterialCheckBox) group.getChildAt(i);
        }
        return null;
    }

    /**
     * 给定一个包对应的item布局，在其中找到subFrame并返回
     */
    @Nullable
    private LinearLayout findSubFrameOfPkg(ViewGroup group) {
        for (int i = 0; i<group.getChildCount(); i++) {
            View view = group.getChildAt(i);
            if (view.getId() == R.id.sub_frame)
                return (LinearLayout) view;
        }
        return null;
    }

    /**
     * 获取 对应名称的dex中的全部类
     */
    private Set<? extends DexBackedClassDef> getClassesFromDex(MultiDexContainer<? extends DexBackedDexFile> container, String name) {
        try {
            return Objects.requireNonNull(container.getEntry(name)).getDexFile().getClasses();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将一个类添加到SmaliPackageData中
     */
    private void addClassDefToList(DexBackedClassDef def) {
        //Ltest/Test5;
        String type = def.getType();
        String[] splits = type.substring(1, type.length() - 1).split("/");
        SmaliPackageData currPkg = smaliData;
        for (int i = 0; i < splits.length; i++) {
            if (i != splits.length - 1) {
                currPkg = currPkg.getSubPackage(splits[i]); // 寻找包名
            } else {
                currPkg.addClassDef(splits[i], def); // 将类添加到对应包下
            }
        }
    }

    /**
     * 获取 provider的apk的本地路径。
     *
     * @return 返回apk本地路径。找不到时返回null
     */
    @Nullable
    public String getProviderApkPath(String pkg) {
        try {
            //需要在manifest指定对应包名的query,才能获取到info
            return getPackageManager().getApplicationInfo(pkg, 0).sourceDir;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null; // 如果包名不存在，返回 null
        }
    }
}