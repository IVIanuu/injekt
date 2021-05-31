/*
 * Copyright 2021 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.ide.hints

import com.intellij.codeInsight.daemon.impl.*
import com.intellij.ide.ui.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.colors.*
import com.intellij.openapi.editor.impl.*
import com.intellij.openapi.editor.markup.*
import com.intellij.ui.paint.*
import com.intellij.util.ui.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.awt.*

data class ExpandableText(
  val collapsed: String,
  val expanded: String?
)

class TextPartsHintRenderer(text: String) : HintRenderer(text) {

  override fun calcWidthInPixels(inlay: Inlay<*>): Int {
    val metrics = getFontMetrics0(inlay.editor).metrics
    return metrics.stringWidth(text)
  }

  override fun paint(inlay: Inlay<*>, g: Graphics, r: Rectangle, textAttributes: TextAttributes) {
    val editor = inlay.editor
    if (editor !is EditorImpl) return

    val ascent = editor.ascent
    val descent = editor.descent
    val g2d = g as Graphics2D
    val attributes = getTextAttributes(editor)
    if (attributes != null) {
      val fontMetrics = getFontMetrics0(editor)
      val backgroundColor = attributes.backgroundColor
      if (backgroundColor != null) {
        val config = GraphicsUtil.setupAAPainting(g)
        GraphicsUtil.paintWithAlpha(g, BackgroundAlpha)
        g.setColor(backgroundColor)
        g.fillRect(r.x, r.y, r.width, r.height)
        config.restore()
      }
      val foregroundColor = attributes.foregroundColor
      if (foregroundColor != null) {
        val metrics = fontMetrics.metrics
        var xStart = r.x
        val yStart = r.y + Math.max(ascent, (r.height + metrics.ascent - metrics.descent) / 2)

        val width = metrics.stringWidth(text)

        val effectiveTextAttributes = attributes!!

        val backgroundColor = effectiveTextAttributes.backgroundColor
        if (backgroundColor != null) {
          val config = GraphicsUtil.setupAAPainting(g)
          GraphicsUtil.paintWithAlpha(g, BackgroundAlpha)
          g.setColor(backgroundColor)
          g.fillRect(xStart, r.y, width, r.height)
          config.restore()
        }

        val foregroundColor = effectiveTextAttributes.foregroundColor
        g.setColor(foregroundColor)

        g.setFont(editor.colorsScheme.getFont(editorFontTypeOf(effectiveTextAttributes.fontType)))

        val savedHint = g2d.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING)
        val savedClip = g.clip
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, AntialiasingType.getKeyForCurrentScope(false))
        g.clipRect(r.x, r.y, r.width, r.height)
        g.drawString(text, xStart, yStart)
        g.setClip(savedClip)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, savedHint)

        val effectColor = effectiveTextAttributes.effectColor
        val effectType = effectiveTextAttributes.effectType
        if (effectColor != null) {
          g.setColor(effectColor)
          val (x1, x2) = xStart to xStart + width
          val y = r.y + ascent
          val font = editor.colorsScheme.getFont(EditorFontType.PLAIN)

          when (effectType) {
            EffectType.LINE_UNDERSCORE -> EffectPainter.LINE_UNDERSCORE.paint(g2d, x1, y, x2 - x1, descent, font)
            EffectType.BOLD_LINE_UNDERSCORE -> EffectPainter.BOLD_LINE_UNDERSCORE.paint(g2d, x1, y, x2 - x1, descent, font)
            EffectType.STRIKEOUT -> EffectPainter.STRIKE_THROUGH.paint(g2d, x1, y, x2 - x1, editor.charHeight, font)
            EffectType.WAVE_UNDERSCORE -> EffectPainter.WAVE_UNDERSCORE.paint(g2d, x1, y, x2 - x1, descent, font)
            EffectType.BOLD_DOTTED_LINE -> EffectPainter.BOLD_DOTTED_UNDERSCORE.paint(g2d, x1, y, x2 - x1, descent, font)
          }
        }

        xStart += width
      }
    }
  }

  private fun editorFontTypeOf(fontType: Int) = when (fontType) {
    Font.BOLD -> EditorFontType.BOLD
    Font.ITALIC -> EditorFontType.ITALIC
    else -> EditorFontType.PLAIN
  }

  private fun getFontMetrics0(editor: Editor): Companion.MyFontMetrics {
    val font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
    return Companion.MyFontMetrics::class.java.constructors.first().newInstance(
      editor, font.family, font.size).cast()
  }
}

private const val BackgroundAlpha = 0.55F
