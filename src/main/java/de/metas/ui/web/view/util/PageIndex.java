package de.metas.ui.web.view.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;

import de.metas.util.Check;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2018 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

@Value
public class PageIndex
{
	public static PageIndex ofFirstRowAndPageLength(final int firstRowZeroBased, final int pageLength)
	{
		return new PageIndex(firstRowZeroBased, pageLength);
	}

	public static PageIndex getPageContainingRow(final int rowIndex, final int pageLength)
	{
		Check.assume(rowIndex >= 0, "rowIndex >= 0");
		Check.assume(pageLength > 0, "pageLength > 0");

		final int page = rowIndex / pageLength;
		final int firstRow = page * pageLength;

		return ofFirstRowAndPageLength(firstRow, pageLength);
	}

	public static PageIndex firstPage(final int pageLength)
	{
		return ofFirstRowAndPageLength(0, pageLength);
	}

	public static PageIndex all()
	{
		return ALL;
	}

	private static final PageIndex ALL = new PageIndex(0, Integer.MAX_VALUE);

	/** Page's first row (zero based index) */
	int firstRow;
	int pageLength;

	private PageIndex(final int firstRow, final int pageLength)
	{
		Check.assume(firstRow >= 0, "firstRow >= 0");
		Check.assume(pageLength > 0, "pageLength > 0");

		this.firstRow = firstRow;
		this.pageLength = pageLength;
	}

	public int getLastRowIndex()
	{
		return firstRow + pageLength - 1;
	}

	@VisibleForTesting
	Iterator<PageIndex> iterator(final int maxRows)
	{
		return new PageIndexIterator(this, maxRows);
	}

	private Spliterator<PageIndex> spliterator(final int maxRows)
	{
		return Spliterators.spliteratorUnknownSize(
				iterator(maxRows),
				Spliterator.ORDERED | Spliterator.IMMUTABLE);
	}

	public <T> Stream<T> iterateAndStream(
			final int maxRows,
			@NonNull final Function<PageIndex, Stream<T>> pageFetcher)
	{
		return StreamSupport.stream(spliterator(maxRows), false)
				.flatMap(pageFetcher);
	}

	@ToString
	private static final class PageIndexIterator implements Iterator<PageIndex>
	{
		private final int maxRows;
		private PageIndex nextPage;

		public PageIndexIterator(@NonNull final PageIndex firstPage, final int maxRows)
		{
			Check.assumeGreaterThanZero(maxRows, "maxRows");

			this.maxRows = maxRows;
			nextPage = adjustFirstPageIfNeeded(firstPage, maxRows);
		}

		private static PageIndex adjustFirstPageIfNeeded(final PageIndex firstPage, final int maxRows)
		{
			final int lastRow = firstPage.getLastRowIndex();
			final int lastRowAllowed = maxRows - 1;
			if (lastRow <= lastRowAllowed)
			{
				return firstPage;
			}
			else
			{
				final int firstRow = firstPage.getFirstRow();
				final int pageLengthAdjusted = lastRowAllowed - firstRow + 1;
				if (pageLengthAdjusted > 0)
				{
					return ofFirstRowAndPageLength(firstRow, pageLengthAdjusted);
				}
				else
				{
					return null;
				}
			}
		}

		@Override
		public boolean hasNext()
		{
			return nextPage != null;
		}

		@Override
		public PageIndex next()
		{
			if (nextPage == null)
			{
				throw new NoSuchElementException();
			}

			final PageIndex currentPage = nextPage;
			nextPage = computeNextPageOrNull(nextPage, maxRows);

			return currentPage;
		}

		private static PageIndex computeNextPageOrNull(final PageIndex page, final int maxRows)
		{
			final int lastRowAllowed = maxRows - 1;

			final int pageLastRow = page.getLastRowIndex();
			final int pageLength = page.getPageLength();

			final int nextPageFirstRow = pageLastRow + 1;
			final int nextPageLastRow = nextPageFirstRow + pageLength - 1;

			if (nextPageLastRow <= lastRowAllowed)
			{
				return ofFirstRowAndPageLength(nextPageFirstRow, pageLength);
			}
			else
			{
				final int nextPageLength = lastRowAllowed - nextPageFirstRow + 1;
				if (nextPageLength > 0)
				{
					return PageIndex.ofFirstRowAndPageLength(nextPageFirstRow, nextPageLength);
				}
				else
				{
					return null;
				}
			}
		}
	}
}
