package com.bytedance.tools.codelocator.tinypng.dialog;

import com.bytedance.tools.codelocator.listener.OnClickListener;
import com.bytedance.tools.codelocator.utils.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class TinyImageDialog extends JDialog {

    private JPanel mContentPanel;

    private JScrollPane mScrollPanel;

    private JSplitPane mSplitPanel;

    private JTree mFileTree;

    private JButton mBtnSave;

    private JButton mBtnCancel;

    private JButton mBtnProcess;

    private JComponent mImageBefore;

    private JComponent mImageAfter;

    private JLabel mDetailsBefore;

    private JLabel mDetailsAfter;

    private JLabel mTitleBefore;

    private JLabel mTitleAfter;

    private JLabel mDetailsTotal;

    private JPanel mToolbar;

    private List<VirtualFile> mImageFiles;

    private List<VirtualFile> mRootFiles;

    private Project mProject;

    private boolean mInCompressProgress;

    boolean mIsAutoPopup;

    ImageSelectListener mImageSelectListener = null;

    private HashMap<VirtualFile, File> mCompressedFileMap;

    private final String mProcessKey;

    private List<FileTreeNode> mImageFileNodes;

    private long mTotalSaveSize;

    public TinyImageDialog(Project project,
                           List<VirtualFile> files,
                           List<VirtualFile> roots,
                           boolean isAutoPop,
                           HashMap<VirtualFile, File> compressMap,
                           String processKey) {
        mInCompressProgress = false;
        mIsAutoPopup = isAutoPop;
        mImageFiles = files;
        mRootFiles = roots;
        mProject = project;
        this.mProcessKey = (processKey == null ? getProjectImageStoreKey(project) : processKey);
        mCompressedFileMap = compressMap;

        setTitle(ResUtils.getString("tiny_png_dialog_title"));
        setContentPane(mContentPanel);
        getRootPane().setDefaultButton(mBtnProcess);

        mBtnProcess.addActionListener(new ProcessActionListener(this));
        mBtnSave.addActionListener(new SaveActionListener(this));

        final CancelActionListener cancelActionListener = new CancelActionListener(this);
        mBtnCancel.addActionListener(cancelActionListener);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });
        mContentPanel.registerKeyboardAction(cancelActionListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        JComponentUtils.supportCommandW(mContentPanel, new OnClickListener() {
            @Override
            public void onClick() {
                hide();
            }
        });
        configureUI();
    }

    public String getProcessKey() {
        return mProcessKey;
    }

    public long getTotalSaveSize() {
        return mTotalSaveSize;
    }

    private List<FileTreeNode> getAllNodes(FileTreeNode root) {
        List<FileTreeNode> nodes = new LinkedList();
        Enumeration enumeration = root.children();
        while (enumeration.hasMoreElements()) {
            FileTreeNode node = (FileTreeNode) enumeration.nextElement();
            if (!node.isLeaf()) {
                nodes.addAll(getAllNodes(node));
            } else {
                nodes.add(node);
            }
        }
        return nodes;
    }

    public static String getProjectImageStoreKey(Project project) {
        final String projectFilePath = project.getName() + "_" + System.currentTimeMillis();
        return projectFilePath;
    }

    public void setDialogSize(JFrame frame) {
        this.setMinimumSize(new Dimension(frame.getWidth() * 3 / 5, frame.getHeight() * 3 / 5));
        mSplitPanel.setDividerLocation(getMinimumSize().width * 2 / 5);
        this.setLocationRelativeTo(frame);
        this.pack();
    }

    private void configureUI() {
        mSplitPanel.setBackground(UIUtil.getPanelBackground());
        mSplitPanel.setUI(new BasicSplitPaneUI() {
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    private final int dashHeight = 30;

                    private Color background = UIUtil.getPanelBackground();

                    private Color dashes = UIUtil.getSeparatorColor();

                    public void setBorder(Border b) {
                    }

                    @Override
                    public void paint(Graphics g) {
                        g.setColor(background);
                        g.fillRect(0, 0, getSize().width, getSize().height);

                        final int top = (getSize().height - dashHeight) / 2;
                        g.setColor(dashes);
                        g.drawLine(4, top, 4, top + dashHeight);
                        g.drawLine(7, top, 7, top + dashHeight);
                        super.paint(g);
                    }
                };
            }
        });

        mSplitPanel.setBorder(null);

        mTitleBefore.setForeground(JBColor.green.darker());
        mTitleAfter.setForeground(JBColor.red.darker());

        mTitleBefore.setText(ResUtils.getString("tiny_png_dialog_before_compress"));
        mTitleAfter.setText(ResUtils.getString("tiny_png_dialog_after_compress"));

        mBtnSave.setText(ResUtils.getString("save"));
        mBtnCancel.setText(ResUtils.getString("cancel"));
        mBtnProcess.setText(ResUtils.getString("tiny_png_dialog_compress"));

        mDetailsAfter.setFont(new Font(mDetailsAfter.getFont().getName(), mDetailsAfter.getFont().getStyle(), 14));
        mDetailsBefore.setFont(new Font(mDetailsBefore.getFont().getName(), mDetailsBefore.getFont().getStyle(), 14));
        mDetailsTotal.setFont(new Font(mDetailsTotal.getFont().getName(), mDetailsTotal.getFont().getStyle(), 14));

        JComponentUtils.setSize(mDetailsTotal, 200, 18);
        JComponentUtils.setSize(mDetailsBefore, 200, 18);
        JComponentUtils.setSize(mDetailsAfter, 200, 18);

        mImageFileNodes = getAllNodes((FileTreeNode) getFileTree().getModel().getRoot());

        if (mCompressedFileMap != null && mCompressedFileMap.size() > 0) {
            for (FileTreeNode node : mImageFileNodes) {
                final VirtualFile virtualFile = node.getVirtualFile();
                if (mCompressedFileMap.containsKey(virtualFile)) {
                    node.setCompressedImageFile(mCompressedFileMap.remove(virtualFile));
                }
                if (mCompressedFileMap.isEmpty()) {
                    break;
                }
            }
            onCompressFinish();
        }
    }

    public void onCompressFinish() {
        mInCompressProgress = false;
        mBtnProcess.setText(ResUtils.getString("tiny_png_dialog_compress"));
        rootPane.setDefaultButton(mBtnSave);
        mBtnSave.setEnabled(true);
        mBtnProcess.setEnabled(true);
        mBtnCancel.setText(ResUtils.getString("cancel"));
        long totalBytes = 0;
        mTotalSaveSize = 0;
        for (FileTreeNode node : mImageFileNodes) {
            if (!node.isChecked() || node.getVirtualFile() == null) {
                continue;
            }
            totalBytes += node.getVirtualFile().getLength();
            if (node.getCompressedImageFile() != null && node.getVirtualFile().getLength() > node.getCompressedImageFile().length()) {
                mTotalSaveSize += (node.getVirtualFile().getLength() - node.getCompressedImageFile().length());
            }
        }
        float compress = (totalBytes == 0L ? 0 : (mTotalSaveSize * 100f / totalBytes));
        String savedSizeStr = StringUtils.getFileSize(mTotalSaveSize, false);
        mDetailsTotal.setText(String.format("Total compress: %.1f%% / Saved: %s", compress, savedSizeStr));
    }

    public JTree getFileTree() {
        return mFileTree;
    }

    public Project getProject() {
        return mProject;
    }

    public JImage getImageBefore() {
        return (JImage) mImageBefore;
    }

    public JImage getImageAfter() {
        return (JImage) mImageAfter;
    }

    public JLabel getDetailsBefore() {
        return mDetailsBefore;
    }

    public JLabel getDetailsAfter() {
        return mDetailsAfter;
    }

    public JButton getBtnSave() {
        return mBtnSave;
    }

    public List<FileTreeNode> getImageFileNodes() {
        return mImageFileNodes;
    }

    public JButton getBtnCancel() {
        return mBtnCancel;
    }

    public JButton getBtnProcess() {
        return mBtnProcess;
    }

    public void setCompressInProgress(boolean value) {
        mInCompressProgress = value;
    }

    public boolean getCompressInProgress() {
        return mInCompressProgress;
    }

    private void onClose() {
        if (mInCompressProgress) {
            mInCompressProgress = false;
        }
        if (mIsAutoPopup) {
            Mob.mob(Mob.Action.CLICK, "tiny_auto_pop_cancel");
        }
        dispose();
    }

    private void createUIComponents() {
        UIUtil.removeScrollBorder(mScrollPanel);
        mImageAfter = new JImage();
        mImageBefore = new JImage();
        mFileTree = new CheckboxTree(new FileCellRenderer(mProject), buildTree());
        mFileTree.setRootVisible(false);
        mFileTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        TreeUtil.expandAll(mFileTree);
        mImageSelectListener = new ImageSelectListener(this);
        mFileTree.addTreeSelectionListener(mImageSelectListener);
        configureToolbar();
        mImageSelectListener.valueChanged(null);
    }

    private void configureToolbar() {
        mToolbar = new JPanel();
        JComponentUtils.setSize(mToolbar, 0, 0);
    }

    private FileTreeNode buildTree() {
        FileTreeNode root = new FileTreeNode();
        for (VirtualFile file : mImageFiles) {
            getParent(root, file).add(new FileTreeNode(file));
        }
        return root;
    }

    private FileTreeNode getParent(FileTreeNode root, VirtualFile file) {
        if (mRootFiles.contains(file)) {
            return root;
        }

        LinkedList<VirtualFile> path = new LinkedList<>();
        while (!mRootFiles.contains(file)) {
            file = file.getParent();
            path.addFirst(file);
        }

        FileTreeNode parent = root;
        for (VirtualFile pathElement : path) {
            FileTreeNode node = findNodeByUserObject(parent, pathElement);
            if (node == null) {
                node = new FileTreeNode(pathElement);
                parent.add(node);
            }
            parent = node;
        }
        return parent;
    }

    @Nullable
    private FileTreeNode findNodeByUserObject(FileTreeNode root, Object userObject) {
        Enumeration enumeration = root.children();
        while (enumeration.hasMoreElements()) {
            FileTreeNode node = (FileTreeNode) enumeration.nextElement();
            if (node.getUserObject() == userObject) {
                return node;
            }
            if (userObject == null || node.getUserObject() == null) {
                continue;
            }
            if (((VirtualFile) userObject).getUrl().equals(((VirtualFile) node.getUserObject()).getPresentableUrl())) {
                return node;
            }
        }
        return null;
    }

    @Override
    public void dispose() {
        super.dispose();
        FileUtils.deleteFile(new File(FileUtils.sCodelocatorImageFileDirPath, getProcessKey()));
    }

}
