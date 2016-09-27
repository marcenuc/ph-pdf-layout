/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.pdflayout.element.table;

import java.awt.Color;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.pdflayout.base.AbstractPLRenderableObject;
import com.helger.pdflayout.base.IPLSplittableObject;
import com.helger.pdflayout.base.IPLVisitor;
import com.helger.pdflayout.base.PLSplitResult;
import com.helger.pdflayout.element.hbox.PLHBox;
import com.helger.pdflayout.element.hbox.PLHBoxColumn;
import com.helger.pdflayout.render.PageRenderContext;
import com.helger.pdflayout.render.PreparationContext;
import com.helger.pdflayout.spec.SizeSpec;
import com.helger.pdflayout.spec.WidthSpec;

public class PLTableRow extends AbstractPLRenderableObject <PLTableRow> implements IPLSplittableObject <PLTableRow>
{
  private final PLHBox m_aRow = new PLHBox ().setVertSplittable (true);

  public PLTableRow ()
  {}

  public void addCell (@Nonnull final PLTableCell aCell, @Nonnull final WidthSpec aWidth)
  {
    m_aRow.addColumn (aCell, aWidth);
  }

  @Nullable
  public PLTableCell getCellAtIndex (final int nIndex)
  {
    final PLHBoxColumn aColumn = m_aRow.getColumnAtIndex (nIndex);
    return aColumn == null ? null : (PLTableCell) aColumn.getElement ();
  }

  public void forEachCell (@Nonnull final Consumer <? super PLTableCell> aConsumer)
  {
    m_aRow.forEachColumn (x -> aConsumer.accept ((PLTableCell) x.getElement ()));
  }

  public void forEachCell (@Nonnull final ObjIntConsumer <? super PLTableCell> aConsumer)
  {
    m_aRow.forEachColumn ( (x, idx) -> aConsumer.accept ((PLTableCell) x.getElement (), idx));
  }

  @Nonnull
  public PLTableRow setFillColor (@Nonnull final Color aFillColor)
  {
    forEachCell (x -> x.setFillColor (aFillColor));
    return this;
  }

  @Override
  public void visit (@Nonnull final IPLVisitor aVisitor) throws IOException
  {
    super.visit (aVisitor);
    m_aRow.visit (aVisitor);
  }

  @Override
  protected SizeSpec onPrepare (final PreparationContext aCtx)
  {
    return m_aRow.prepare (aCtx);
  }

  public boolean isVertSplittable ()
  {
    return m_aRow.isVertSplittable ();
  }

  @Nullable
  public PLSplitResult splitElementVert (final float fAvailableWidth, final float fAvailableHeight)
  {
    return m_aRow.splitElementVert (fAvailableWidth, fAvailableHeight);
  }

  @Override
  protected void onRender (final PageRenderContext aCtx) throws IOException
  {
    m_aRow.render (aCtx);
  }
}
