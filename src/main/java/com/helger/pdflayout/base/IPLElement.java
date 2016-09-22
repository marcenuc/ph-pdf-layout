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
package com.helger.pdflayout.base;

/**
 * Base interface for renderable objects having a margin, a border and a
 * padding<br>
 * Each object is self-responsible for handling its margin, border and padding!
 *
 * @author Philip Helger
 * @param <IMPLTYPE>
 *        Implementation type
 */
public interface IPLElement <IMPLTYPE extends IPLElement <IMPLTYPE>> extends
                            IPLRenderableObject <IMPLTYPE>,
                            IPLHasMarginBorderPadding <IMPLTYPE>,
                            IPLHasFillColor <IMPLTYPE>
{
  /* empty */
}