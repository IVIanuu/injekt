package com.ivianuu.injekt.ide.hints

import com.intellij.codeInsight.daemon.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.*

var injectionHintsEnabled = false

class ShowInjectionHintsAction : ToggleAction(
  "Toggle injection hints",
  "Show hints for injected parameters",
  /* icon = */ null
) {
  override fun isSelected(event: AnActionEvent): Boolean = injectionHintsEnabled

  override fun setSelected(event: AnActionEvent, value: Boolean) {
    injectionHintsEnabled = value
    ProjectManager.getInstance().openProjects.forEach {
      DaemonCodeAnalyzer.getInstance(event.project).restart()
    }
  }
}
