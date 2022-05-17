package com.bytedance.tools.codelocator.importopt

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import org.jetbrains.kotlin.idea.inspections.KotlinUnusedImportInspection
import org.jetbrains.kotlin.psi.KtFile

object DeleteImportManager {

    var count = 0

    fun removeUnusedKotlinImports(currentFile: KtFile, project: Project) {
        WriteCommandAction.runWriteCommandAction(project) {
            val importList = currentFile.importList
            val analyzeRes = KotlinUnusedImportInspection.analyzeImports(currentFile)
            val unusedImports = analyzeRes?.unusedImports ?: return@runWriteCommandAction
            count++
            importList?.imports?.forEach {
                if (it in unusedImports) {
                    it.delete()
                }
            }
        }
    }

    fun removeUnusedJavaImports(file: PsiJavaFile, project: Project) {
        WriteCommandAction.runWriteCommandAction(project) {
            val javaCodeStyleManager = JavaCodeStyleManager.getInstance(project)
            val findRedundantImports = javaCodeStyleManager.findRedundantImports(file)
            if (findRedundantImports.isNullOrEmpty()) {
                return@runWriteCommandAction
            }
            count++
            javaCodeStyleManager.removeRedundantImports(file)
        }
    }

    fun deleteUnusedImports(file: PsiFile, project: Project) {
        when (file) {
            is PsiJavaFile -> {
                removeUnusedJavaImports(file, project)
            }
            is KtFile -> {
                removeUnusedKotlinImports(file, project)
            }
        }
    }
}