package com.bytedance.tools.codelocator.importopt;

import com.bytedance.tools.codelocator.model.CodeLocatorUserConfig;
import com.bytedance.tools.codelocator.utils.Mob;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.changes.ui.BooleanCommitOption;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinMetaHandler;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class DeleteImportCheckinHandler extends CheckinHandler implements CheckinMetaHandler {

    protected final Project myProject;

    private final CheckinProjectPanel myPanel;

    public DeleteImportCheckinHandler(final Project project, final CheckinProjectPanel panel) {
        myProject = project;
        myPanel = panel;
        if (CodeLocatorUserConfig.loadConfig().isAutoFormatCode()) {
            VcsConfiguration.getInstance(project).REFORMAT_BEFORE_PROJECT_COMMIT = true;
        }
    }

    @Override
    public void runCheckinHandlers(@NotNull Runnable finishAction) {
        checkNeedDeleteUselessImport(finishAction);
    }

    private void checkNeedDeleteUselessImport(@NotNull Runnable finishAction) {
        if (!CodeLocatorUserConfig.loadConfig().isDeleteUselessImport()) {
            finishAction.run();
            return;
        }
        final Collection<VirtualFile> files = myPanel.getVirtualFiles();
        DeleteImportManager.INSTANCE.setCount(0);
        if (files != null) {
            for (VirtualFile f : files) {
                final PsiFile file = PsiManager.getInstance(myProject).findFile(f);
                if (file != null) {
                    DeleteImportManager.INSTANCE.deleteUnusedImports(file, myProject);
                }
            }
        }
        final int count = DeleteImportManager.INSTANCE.getCount();
        if (count != 0) {
            Mob.mob(Mob.Action.EXEC, Mob.Button.REMOVE_IMPORT + ", fileCount: " + count);
        }
        FileDocumentManager.getInstance().saveAllDocuments();
        finishAction.run();
    }

    @Override
    @Nullable
    public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
        return new BooleanCommitOption(myPanel, "Delete Unused Imports", true,
            () -> CodeLocatorUserConfig.loadConfig().isDeleteUselessImport(),
            value -> {
                CodeLocatorUserConfig.loadConfig().setDeleteUselessImport(value);
                CodeLocatorUserConfig.updateConfig(CodeLocatorUserConfig.loadConfig());
            });
    }

}
