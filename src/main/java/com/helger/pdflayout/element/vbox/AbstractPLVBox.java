/**
# * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.pdflayout.element.vbox;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;

import javax.annotation.CheckForSigned;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.string.ToStringGenerator;
import com.helger.pdflayout.PLDebug;
import com.helger.pdflayout.base.AbstractPLBlockElement;
import com.helger.pdflayout.base.AbstractPLRenderableObject;
import com.helger.pdflayout.base.IPLHasHorizontalAlignment;
import com.helger.pdflayout.base.IPLHasMargin;
import com.helger.pdflayout.base.IPLHasVerticalAlignment;
import com.helger.pdflayout.base.IPLRenderableObject;
import com.helger.pdflayout.base.IPLSplittableObject;
import com.helger.pdflayout.base.IPLVisitor;
import com.helger.pdflayout.base.PLElementWithSize;
import com.helger.pdflayout.base.PLSplitResult;
import com.helger.pdflayout.render.PageRenderContext;
import com.helger.pdflayout.render.PreparationContext;
import com.helger.pdflayout.spec.HeightSpec;
import com.helger.pdflayout.spec.SizeSpec;

/**
 * Vertical box - groups several rows.
 *
 * @author Philip Helger
 * @param <IMPLTYPE>
 *        Implementation type
 */
public abstract class AbstractPLVBox <IMPLTYPE extends AbstractPLVBox <IMPLTYPE>> extends
                                     AbstractPLRenderableObject <IMPLTYPE> implements IPLSplittableObject <IMPLTYPE>
{
  public static final boolean DEFAULT_FULL_WIDTH = true;
  private static final Logger s_aLogger = LoggerFactory.getLogger (AbstractPLVBox.class);

  private final ICommonsList <PLVBoxRow> m_aRows = new CommonsArrayList<> ();
  private boolean m_bVertSplittable = DEFAULT_VERT_SPLITTABLE;
  private int m_nHeaderRowCount = 0;
  private boolean m_bFullWidth = DEFAULT_FULL_WIDTH;

  // Status vars
  /** prepared row size (with outline of contained element) */
  private SizeSpec [] m_aPreparedRowSize;
  /** prepared element size (without outline) */
  private SizeSpec [] m_aPreparedElementSize;

  public AbstractPLVBox ()
  {}

  @Nonnull
  @OverridingMethodsMustInvokeSuper
  public IMPLTYPE setBasicDataFrom (@Nonnull final AbstractPLVBox <?> aSource)
  {
    super.setBasicDataFrom (aSource);
    setVertSplittable (aSource.m_bVertSplittable);
    setHeaderRowCount (aSource.m_nHeaderRowCount);
    return thisAsT ();
  }

  /**
   * @return The number of rows. Always &ge; 0.
   */
  @Nonnegative
  public int getRowCount ()
  {
    return m_aRows.size ();
  }

  /**
   * @return All rows. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <PLVBoxRow> getAllRows ()
  {
    return m_aRows.getClone ();
  }

  /**
   * @return All rows. Never <code>null</code>.
   */
  @Nonnull
  public Iterable <PLVBoxRow> getRows ()
  {
    return m_aRows;
  }

  public void forEachRow (@Nonnull final Consumer <? super PLVBoxRow> aConsumer)
  {
    m_aRows.forEach (aConsumer);
  }

  public void forEachRow (@Nonnull final ObjIntConsumer <? super PLVBoxRow> aConsumer)
  {
    m_aRows.forEach (aConsumer);
  }

  /**
   * Get the row at the specified index.
   *
   * @param nIndex
   *        The index to use. Should be &ge; 0.
   * @return <code>null</code> if an invalid index was provided.
   */
  @Nullable
  public PLVBoxRow getRowAtIndex (@Nonnegative final int nIndex)
  {
    return m_aRows.getAtIndex (nIndex);
  }

  /**
   * @return The first row or <code>null</code> if no row is present.
   */
  @Nullable
  public PLVBoxRow getFirstRow ()
  {
    return m_aRows.getFirst ();
  }

  /**
   * @return The last row or <code>null</code> if no row is present.
   */
  @Nullable
  public PLVBoxRow getLastRow ()
  {
    return m_aRows.getLast ();
  }

  /**
   * Get the element in the row at the specified index.
   *
   * @param nIndex
   *        The index to use. Should be &ge; 0.
   * @return <code>null</code> if an invalid index was provided.
   */
  @Nullable
  public IPLRenderableObject <?> getRowElementAtIndex (@Nonnegative final int nIndex)
  {
    final PLVBoxRow aRow = getRowAtIndex (nIndex);
    return aRow == null ? null : aRow.getElement ();
  }

  /**
   * @return The element in the first row or <code>null</code> if no row is
   *         present.
   */
  @Nullable
  public IPLRenderableObject <?> getFirstRowElement ()
  {
    final PLVBoxRow aRow = getFirstRow ();
    return aRow == null ? null : aRow.getElement ();
  }

  /**
   * @return The element in the last row or <code>null</code> if no row is
   *         present.
   */
  @Nullable
  public IPLRenderableObject <?> getLastRowElement ()
  {
    final PLVBoxRow aRow = getLastRow ();
    return aRow == null ? null : aRow.getElement ();
  }

  @Nonnull
  private PLVBoxRow _addAndReturnRow (@CheckForSigned final int nIndex,
                                      @Nonnull final IPLRenderableObject <?> aElement,
                                      @Nonnull final HeightSpec aHeight)
  {
    final PLVBoxRow aItem = new PLVBoxRow (aElement, aHeight);
    if (nIndex < 0 || nIndex >= m_aRows.size ())
      m_aRows.add (aItem);
    else
      m_aRows.add (nIndex, aItem);
    return aItem;
  }

  /**
   * Add a row to this VBox using auto height.
   *
   * @param aElement
   *        The row to be added. May not be <code>null</code>.
   * @return the created row
   */
  @Nonnull
  public PLVBoxRow addAndReturnRow (@Nonnull final IPLRenderableObject <?> aElement)
  {
    return addAndReturnRow (aElement, HeightSpec.auto ());
  }

  /**
   * Add a row to this VBox.
   *
   * @param aElement
   *        The row to be added. May not be <code>null</code>.
   * @param aHeight
   *        The height specification to use. May not be <code>null</code>.
   * @return the created row
   */
  @Nonnull
  public PLVBoxRow addAndReturnRow (@Nonnull final IPLRenderableObject <?> aElement, @Nonnull final HeightSpec aHeight)
  {
    internalCheckNotPrepared ();
    return _addAndReturnRow (-1, aElement, aHeight);
  }

  /**
   * Add a row to this VBox using auto height.
   *
   * @param aElement
   *        The row to be added. May not be <code>null</code>.
   * @return this
   */
  @Nonnull
  public IMPLTYPE addRow (@Nonnull final IPLRenderableObject <?> aElement)
  {
    return addRow (aElement, HeightSpec.auto ());
  }

  /**
   * Add a row to this VBox.
   *
   * @param aElement
   *        The row to be added. May not be <code>null</code>.
   * @param aHeight
   *        The height specification to use. May not be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public IMPLTYPE addRow (@Nonnull final IPLRenderableObject <?> aElement, @Nonnull final HeightSpec aHeight)
  {
    addAndReturnRow (aElement, aHeight);
    return thisAsT ();
  }

  /**
   * Add a row to this VBox.
   *
   * @param nIndex
   *        The index where the row should be added. Must be &ge; 0.
   * @param aElement
   *        The row to be added. May not be <code>null</code>.
   * @param aHeight
   *        The height specification to use. May not be <code>null</code>.
   * @return the created row. Never <code>null</code>.
   */
  @Nonnull
  public PLVBoxRow addAndReturnRow (@Nonnegative final int nIndex,
                                    @Nonnull final IPLRenderableObject <?> aElement,
                                    @Nonnull final HeightSpec aHeight)
  {
    ValueEnforcer.isGE0 (nIndex, "Index");
    internalCheckNotPrepared ();
    return _addAndReturnRow (nIndex, aElement, aHeight);
  }

  /**
   * Add a row to this VBox.
   *
   * @param nIndex
   *        The index where the row should be added. Must be &ge; 0.
   * @param aElement
   *        The row to be added. May not be <code>null</code>.
   * @param aHeight
   *        The height specification to use. May not be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public IMPLTYPE addRow (@Nonnegative final int nIndex,
                          @Nonnull final IPLRenderableObject <?> aElement,
                          @Nonnull final HeightSpec aHeight)
  {
    addAndReturnRow (nIndex, aElement, aHeight);
    return thisAsT ();
  }

  /**
   * Remove the row with the specified index.
   *
   * @param nIndex
   *        The index to be removed.
   * @return this for chaining
   */
  @Nonnull
  public IMPLTYPE removeRow (@Nonnegative final int nIndex)
  {
    ValueEnforcer.isGE0 (nIndex, "Index");
    internalCheckNotPrepared ();
    m_aRows.remove (nIndex);
    return thisAsT ();
  }

  public boolean isVertSplittable ()
  {
    return m_bVertSplittable;
  }

  @Nonnull
  public IMPLTYPE setVertSplittable (final boolean bVertSplittable)
  {
    m_bVertSplittable = bVertSplittable;
    return thisAsT ();
  }

  /**
   * @return The number of header rows. By default 0. Always &ge; 0.
   */
  @Nonnegative
  public int getHeaderRowCount ()
  {
    return m_nHeaderRowCount;
  }

  /**
   * Set the number of header rows in this table. Header rows get repeated on
   * every page upon rendering.
   *
   * @param nHeaderRowCount
   *        The number of header rows, to be repeated by page. Must be &ge; 0.
   * @return this
   */
  @Nonnull
  public IMPLTYPE setHeaderRowCount (@Nonnegative final int nHeaderRowCount)
  {
    ValueEnforcer.isGE0 (nHeaderRowCount, "HeaderRowCount");

    m_nHeaderRowCount = nHeaderRowCount;
    return thisAsT ();
  }

  public boolean containsAnySplittableElement ()
  {
    return m_aRows.containsAny (x -> x.getElement ().isVertSplittable ());
  }

  public boolean isFullWidth ()
  {
    return m_bFullWidth;
  }

  @Nonnull
  public IMPLTYPE setFullWidth (final boolean bFullWidth)
  {
    m_bFullWidth = bFullWidth;
    return thisAsT ();
  }

  @Override
  public void visit (@Nonnull final IPLVisitor aVisitor) throws IOException
  {
    for (final PLVBoxRow aRow : m_aRows)
      aRow.getElement ().visit (aVisitor);
  }

  @Override
  @OverridingMethodsMustInvokeSuper
  protected SizeSpec onPrepare (@Nonnull final PreparationContext aCtx)
  {
    m_aPreparedRowSize = new SizeSpec [m_aRows.size ()];
    m_aPreparedElementSize = new SizeSpec [m_aRows.size ()];

    final float fAvailableWidth = aCtx.getAvailableWidth ();
    final float fAvailableHeight = aCtx.getAvailableHeight ();
    float fMaxContentWidth = 0;
    float fMaxRowWidth = 0;
    float fUsedHeightFull = 0;

    int nStarRows = 0;
    int nAutoRows = 0;
    for (final PLVBoxRow aRow : m_aRows)
      switch (aRow.getHeight ().getType ())
      {
        case STAR:
          ++nStarRows;
          break;
        case AUTO:
          ++nAutoRows;
          break;
      }

    int nIndex = 0;
    float fRestHeight = fAvailableHeight;
    // 1. prepare all non-star height items
    for (final PLVBoxRow aRow : m_aRows)
    {
      if (aRow.getHeight ().isAbsolute ())
      {
        final IPLRenderableObject <?> aElement = aRow.getElement ();

        // Height of this row
        final float fRowHeight = aRow.getHeight ().getEffectiveValue (fAvailableHeight);

        // Prepare child element
        final SizeSpec aElementPreparedSize = aElement.prepare (new PreparationContext (aCtx.getGlobalContext (),
                                                                                        fAvailableWidth,
                                                                                        fRowHeight));

        // Update used width
        // Effective content width of this element
        fMaxContentWidth = Math.max (fMaxContentWidth, aElementPreparedSize.getWidth ());
        final float fRowWidth = aElementPreparedSize.getWidth () + aElement.getOutlineXSum ();
        fMaxRowWidth = Math.max (fMaxRowWidth, fRowWidth);

        // Update used height
        fUsedHeightFull += fRowHeight;
        fRestHeight -= fRowHeight;

        // Without padding and margin
        m_aPreparedRowSize[nIndex] = new SizeSpec (m_bFullWidth ? fAvailableWidth : fRowWidth, fRowHeight);
        m_aPreparedElementSize[nIndex] = aElementPreparedSize;
      }
      ++nIndex;
    }

    // 2. prepare all auto widths items
    nIndex = 0;
    for (final PLVBoxRow aRow : m_aRows)
    {
      if (aRow.getHeight ().isAuto ())
      {
        final IPLRenderableObject <?> aElement = aRow.getElement ();

        // Height of this row
        final float fAvailableRowHeight = fRestHeight;

        // Prepare child element
        final SizeSpec aElementPreparedSize = aElement.prepare (new PreparationContext (aCtx.getGlobalContext (),
                                                                                        fAvailableWidth,
                                                                                        fAvailableRowHeight));

        // Use the used size of the element as the column width
        final float fRowHeight = aElementPreparedSize.getHeight () + aElement.getOutlineYSum ();

        // Update used width
        // Effective content width of this element
        fMaxContentWidth = Math.max (fMaxContentWidth, aElementPreparedSize.getWidth ());
        final float fRowWidth = aElementPreparedSize.getWidth () + aElement.getOutlineXSum ();
        fMaxRowWidth = Math.max (fMaxRowWidth, fRowWidth);

        // Update used height
        fUsedHeightFull += fRowHeight;

        // Without padding and margin
        m_aPreparedRowSize[nIndex] = new SizeSpec (m_bFullWidth ? fAvailableWidth : fRowWidth, fRowHeight);
        m_aPreparedElementSize[nIndex] = aElementPreparedSize;
      }
      ++nIndex;
    }

    // 3. prepare all star widths items
    fRestHeight = fAvailableHeight - fUsedHeightFull;
    nIndex = 0;
    for (final PLVBoxRow aRow : m_aRows)
    {
      if (aRow.getHeight ().isStar ())
      {
        final IPLRenderableObject <?> aElement = aRow.getElement ();

        // Height of this row
        final float fRowHeight = fRestHeight / nStarRows;

        // Prepare child element
        final SizeSpec aElementPreparedSize = aElement.prepare (new PreparationContext (aCtx.getGlobalContext (),
                                                                                        fAvailableWidth,
                                                                                        fRowHeight));

        // Update used width
        // Effective content width of this element
        fMaxContentWidth = Math.max (fMaxContentWidth, aElementPreparedSize.getWidth ());
        final float fRowWidth = aElementPreparedSize.getWidth () + aElement.getOutlineXSum ();
        fMaxRowWidth = Math.max (fMaxRowWidth, fRowWidth);

        // Update used height
        fUsedHeightFull += fRowHeight;
        // Don't change rest-height!

        // Without padding and margin
        m_aPreparedRowSize[nIndex] = new SizeSpec (m_bFullWidth ? fAvailableWidth : fRowWidth, fRowHeight);
        m_aPreparedElementSize[nIndex] = aElementPreparedSize;
      }
      ++nIndex;
    }

    // Apply alignment
    {
      nIndex = 0;
      for (final PLVBoxRow aRow : m_aRows)
      {
        final IPLRenderableObject <?> aElement = aRow.getElement ();
        if (aElement instanceof IPLHasMargin <?>)
        {
          if (aElement instanceof IPLHasVerticalAlignment <?>)
          {
            final float fMarginTop = ((IPLHasVerticalAlignment <?>) aElement).getIndentY (m_aPreparedRowSize[nIndex].getHeight (),
                                                                                          m_aPreparedElementSize[nIndex].getHeight ());
            ((IPLHasMargin <?>) aElement).addMarginTop (fMarginTop);
          }
          if (aElement instanceof IPLHasHorizontalAlignment <?>)
          {
            final float fMarginLeft = ((IPLHasHorizontalAlignment <?>) aElement).getIndentX (m_bFullWidth ? fAvailableWidth
                                                                                                          : fMaxContentWidth,
                                                                                             m_bFullWidth ? m_aPreparedElementSize[nIndex].getWidth ()
                                                                                                          : m_aPreparedRowSize[nIndex].getWidth ());
            ((IPLHasMargin <?>) aElement).addMarginLeft (fMarginLeft);
          }
        }
        ++nIndex;
      }
    }

    // Set min size for block elements
    {
      nIndex = 0;
      for (final PLVBoxRow aRow : m_aRows)
      {
        final IPLRenderableObject <?> aElement = aRow.getElement ();
        if (aElement instanceof AbstractPLBlockElement <?>)
        {
          final AbstractPLBlockElement <?> aRealElement = (AbstractPLBlockElement <?>) aElement;
          // Set minimum column width and height as prepared width
          aRealElement.setMinSize (m_bFullWidth ? fAvailableWidth : fMaxContentWidth,
                                   m_aPreparedRowSize[nIndex].getHeight () - aElement.getOutlineYSum ());
        }
        ++nIndex;
      }
    }

    // Small consistency check (with rounding included)
    if (GlobalDebug.isDebugMode ())
    {
      if (fMaxRowWidth - fAvailableWidth > 0.01)
        s_aLogger.warn (getDebugID () +
                        " uses more width (" +
                        fMaxRowWidth +
                        ") than available (" +
                        fAvailableWidth +
                        ")!");
      if (fUsedHeightFull - fAvailableHeight > 0.01 && !isVertSplittable ())
        s_aLogger.warn (getDebugID () +
                        " uses more height (" +
                        fUsedHeightFull +
                        ") than available (" +
                        fAvailableHeight +
                        ")!");
    }

    return new SizeSpec (fMaxRowWidth, fUsedHeightFull);
  }

  @Nullable
  public PLSplitResult splitElementVert (final float fAvailableWidth, final float fAvailableHeight)
  {
    if (fAvailableHeight <= 0)
      return null;

    if (!containsAnySplittableElement ())
    {
      // Splitting makes no sense
      if (PLDebug.isDebugSplit ())
        PLDebug.debugSplit (this, "Cannot split because no splittable elements are contained");
      return null;
    }

    // Create resulting VBoxes - the first one is not splittable again!
    final AbstractPLVBox <?> aVBox1 = new PLVBox ().setBasicDataFrom (this)
                                                   .setID (getID () + "-1")
                                                   .setVertSplittable (false);
    final AbstractPLVBox <?> aVBox2 = new PLVBox ().setBasicDataFrom (this)
                                                   .setID (getID () + "-2")
                                                   .setVertSplittable (true);

    final int nTotalRows = getRowCount ();
    final ICommonsList <SizeSpec> aVBox1RowSize = new CommonsArrayList<> (nTotalRows);
    final ICommonsList <SizeSpec> aVBox1ElementSize = new CommonsArrayList<> (nTotalRows);
    float fUsedVBox1RowHeight = 0;

    // Copy all header rows
    for (int nRow = 0; nRow < m_nHeaderRowCount; ++nRow)
    {
      final IPLRenderableObject <?> aHeaderRowElement = getRowElementAtIndex (nRow);
      aVBox1.addRow (aHeaderRowElement);
      aVBox2.addRow (aHeaderRowElement);

      fUsedVBox1RowHeight += m_aPreparedRowSize[nRow].getHeight ();
      aVBox1RowSize.add (m_aPreparedRowSize[nRow]);
      aVBox1ElementSize.add (m_aPreparedElementSize[nRow]);
    }

    // The height and width after header are identical
    final ICommonsList <SizeSpec> aVBox2RowSize = new CommonsArrayList<> (aVBox1RowSize);
    final ICommonsList <SizeSpec> aVBox2ElementSize = new CommonsArrayList<> (aVBox1ElementSize);
    float fUsedVBox2RowHeight = fUsedVBox1RowHeight;

    // Copy all content rows
    boolean bOnVBox1 = true;

    for (int nRow = m_nHeaderRowCount; nRow < nTotalRows; ++nRow)
    {
      final IPLRenderableObject <?> aRowElement = getRowElementAtIndex (nRow);
      final float fRowHeight = m_aPreparedRowSize[nRow].getHeight ();

      if (bOnVBox1)
      {
        if (fUsedVBox1RowHeight + fRowHeight <= fAvailableHeight)
        {
          // Row fits in first VBox without a change
          aVBox1.addRow (aRowElement);
          fUsedVBox1RowHeight += fRowHeight;
          // Use data as is
          aVBox1RowSize.add (m_aPreparedRowSize[nRow]);
          aVBox1ElementSize.add (m_aPreparedElementSize[nRow]);
        }
        else
        {
          // Row does not fit - check if it can be splitted
          bOnVBox1 = false;
          // try to split the row
          boolean bSplittedRow = false;
          if (aRowElement.isVertSplittable ())
          {
            final float fSplitWidth = m_aPreparedElementSize[nRow].getWidth ();
            final float fSplitHeight = fAvailableHeight - fUsedVBox1RowHeight - aRowElement.getOutlineYSum ();
            if (PLDebug.isDebugSplit ())
              PLDebug.debugSplit (this,
                                  "Trying to split " +
                                        aRowElement.getDebugID () +
                                        " into pieces for split size " +
                                        PLDebug.getWH (fSplitWidth, fSplitHeight));

            // Try to split the element contained in the row
            final PLSplitResult aSplitResult = aRowElement.getAsSplittable ().splitElementVert (fSplitWidth,
                                                                                                fSplitHeight);
            if (aSplitResult != null)
            {
              final IPLRenderableObject <?> aVBox1RowElement = aSplitResult.getFirstElement ().getElement ();
              aVBox1.addRow (aVBox1RowElement);
              fUsedVBox1RowHeight += aSplitResult.getFirstElement ().getHeightFull ();
              aVBox1RowSize.add (aSplitResult.getFirstElement ().getSizeFull ());
              aVBox1ElementSize.add (aSplitResult.getFirstElement ().getSize ());

              final IPLRenderableObject <?> aVBox2RowElement = aSplitResult.getSecondElement ().getElement ();
              aVBox2.addRow (aVBox2RowElement);
              fUsedVBox2RowHeight += aSplitResult.getSecondElement ().getHeightFull ();
              aVBox2RowSize.add (aSplitResult.getSecondElement ().getSizeFull ());
              aVBox2ElementSize.add (aSplitResult.getSecondElement ().getSize ());

              if (PLDebug.isDebugSplit ())
                PLDebug.debugSplit (this,
                                    "Split row element " +
                                          aRowElement.getDebugID () +
                                          " (Row " +
                                          nRow +
                                          ") into pieces: " +
                                          aVBox1RowElement.getDebugID () +
                                          " (" +
                                          aSplitResult.getFirstElement ().getWidth () +
                                          "+" +
                                          aVBox1RowElement.getOutlineXSum () +
                                          " & " +
                                          aSplitResult.getFirstElement ().getHeight () +
                                          "+" +
                                          aVBox1RowElement.getOutlineYSum () +
                                          ") and " +
                                          aVBox2RowElement.getDebugID () +
                                          " (" +
                                          aSplitResult.getSecondElement ().getWidth () +
                                          "+" +
                                          aVBox2RowElement.getOutlineXSum () +
                                          " & " +
                                          aSplitResult.getSecondElement ().getHeight () +
                                          "+" +
                                          aVBox2RowElement.getOutlineYSum () +
                                          ")");
              bSplittedRow = true;
            }
            else
            {
              if (PLDebug.isDebugSplit ())
                PLDebug.debugSplit (this,
                                    "Failed to split row element " +
                                          aRowElement.getDebugID () +
                                          " (Row " +
                                          nRow +
                                          ") into pieces");
            }
          }

          if (!bSplittedRow)
          {
            // just add the full row to the second VBox since the row does not
            // fit on first page
            aVBox2.addRow (aRowElement);
            fUsedVBox2RowHeight += fRowHeight;
            aVBox2RowSize.add (m_aPreparedRowSize[nRow]);
            aVBox2ElementSize.add (m_aPreparedElementSize[nRow]);
          }
        }
      }
      else
      {
        // We're already on VBox 2 - add all elements, since VBox2 may be split
        // again later!
        aVBox2.addRow (aRowElement);
        fUsedVBox2RowHeight += fRowHeight;
        aVBox2RowSize.add (m_aPreparedRowSize[nRow]);
        aVBox2ElementSize.add (m_aPreparedElementSize[nRow]);
      }
    }

    if (aVBox1.getRowCount () == m_nHeaderRowCount)
    {
      // Splitting makes no sense!
      if (PLDebug.isDebugSplit ())
        PLDebug.debugSplit (this, "Splitting makes no sense, because VBox 1 would be empty");
      return null;
    }

    if (aVBox2.getRowCount () == m_nHeaderRowCount)
    {
      // Splitting makes no sense!
      if (PLDebug.isDebugSplit ())
        PLDebug.debugSplit (this, "Splitting makes no sense, because VBox 2 would be empty");
      return null;
    }

    // Excluding padding/margin
    aVBox1.internalMarkAsPrepared (new SizeSpec (fAvailableWidth, fUsedVBox1RowHeight));
    aVBox1.m_aPreparedRowSize = ArrayHelper.newArray (aVBox1RowSize, SizeSpec.class);
    aVBox1.m_aPreparedElementSize = ArrayHelper.newArray (aVBox1ElementSize, SizeSpec.class);

    aVBox2.internalMarkAsPrepared (new SizeSpec (fAvailableWidth, fUsedVBox2RowHeight));
    aVBox2.m_aPreparedRowSize = ArrayHelper.newArray (aVBox2RowSize, SizeSpec.class);
    aVBox2.m_aPreparedElementSize = ArrayHelper.newArray (aVBox2ElementSize, SizeSpec.class);

    return new PLSplitResult (new PLElementWithSize (aVBox1, new SizeSpec (fAvailableWidth, fUsedVBox1RowHeight)),
                              new PLElementWithSize (aVBox2, new SizeSpec (fAvailableWidth, fUsedVBox2RowHeight)));
  }

  @Override
  protected void onRender (@Nonnull final PageRenderContext aCtx) throws IOException
  {
    final float fCurX = aCtx.getStartLeft () + getOutlineLeft ();
    float fCurY = aCtx.getStartTop () - getOutlineTop ();

    int nIndex = 0;
    for (final PLVBoxRow aRow : m_aRows)
    {
      final IPLRenderableObject <?> aElement = aRow.getElement ();
      final float fRowWidth = m_aPreparedRowSize[nIndex].getWidth ();
      final float fRowHeight = m_aPreparedRowSize[nIndex].getHeight ();

      // Perform contained element after border
      final PageRenderContext aRowElementCtx = new PageRenderContext (aCtx, fCurX, fCurY, fRowWidth, fRowHeight);
      aElement.render (aRowElementCtx);

      // Update Y-pos
      fCurY -= fRowHeight;
      ++nIndex;
    }
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ())
                            .append ("Rows", m_aRows)
                            .appendIfNotNull ("PreparedRowSize", m_aPreparedRowSize)
                            .appendIfNotNull ("PreparedElementSize", m_aPreparedElementSize)
                            .toString ();
  }
}
