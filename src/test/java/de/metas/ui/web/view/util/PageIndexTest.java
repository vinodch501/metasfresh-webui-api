package de.metas.ui.web.view.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

public class PageIndexTest
{
	@Test
	public void test_getPageContainingRow()
	{
		final int pageLength = 10;
		assertThat(PageIndex.getPageContainingRow(0, pageLength)).isEqualTo(PageIndex.ofFirstRowAndPageLength(0, pageLength));
		assertThat(PageIndex.getPageContainingRow(1, pageLength)).isEqualTo(PageIndex.ofFirstRowAndPageLength(0, pageLength));
		assertThat(PageIndex.getPageContainingRow(2, pageLength)).isEqualTo(PageIndex.ofFirstRowAndPageLength(0, pageLength));
		assertThat(PageIndex.getPageContainingRow(3, pageLength)).isEqualTo(PageIndex.ofFirstRowAndPageLength(0, pageLength));
		assertThat(PageIndex.getPageContainingRow(4, pageLength)).isEqualTo(PageIndex.ofFirstRowAndPageLength(0, pageLength));
		assertThat(PageIndex.getPageContainingRow(5, pageLength)).isEqualTo(PageIndex.ofFirstRowAndPageLength(0, pageLength));
		assertThat(PageIndex.getPageContainingRow(6, pageLength)).isEqualTo(PageIndex.ofFirstRowAndPageLength(0, pageLength));
		assertThat(PageIndex.getPageContainingRow(7, pageLength)).isEqualTo(PageIndex.ofFirstRowAndPageLength(0, pageLength));
		assertThat(PageIndex.getPageContainingRow(8, pageLength)).isEqualTo(PageIndex.ofFirstRowAndPageLength(0, pageLength));
		assertThat(PageIndex.getPageContainingRow(9, pageLength)).isEqualTo(PageIndex.ofFirstRowAndPageLength(0, pageLength));
		//
		assertThat(PageIndex.getPageContainingRow(10, pageLength)).isEqualTo(PageIndex.ofFirstRowAndPageLength(10, pageLength));
		assertThat(PageIndex.getPageContainingRow(11, pageLength)).isEqualTo(PageIndex.ofFirstRowAndPageLength(10, pageLength));
		assertThat(PageIndex.getPageContainingRow(12, pageLength)).isEqualTo(PageIndex.ofFirstRowAndPageLength(10, pageLength));
		assertThat(PageIndex.getPageContainingRow(13, pageLength)).isEqualTo(PageIndex.ofFirstRowAndPageLength(10, pageLength));
		assertThat(PageIndex.getPageContainingRow(14, pageLength)).isEqualTo(PageIndex.ofFirstRowAndPageLength(10, pageLength));
		assertThat(PageIndex.getPageContainingRow(15, pageLength)).isEqualTo(PageIndex.ofFirstRowAndPageLength(10, pageLength));
		assertThat(PageIndex.getPageContainingRow(16, pageLength)).isEqualTo(PageIndex.ofFirstRowAndPageLength(10, pageLength));
		assertThat(PageIndex.getPageContainingRow(17, pageLength)).isEqualTo(PageIndex.ofFirstRowAndPageLength(10, pageLength));
		assertThat(PageIndex.getPageContainingRow(18, pageLength)).isEqualTo(PageIndex.ofFirstRowAndPageLength(10, pageLength));
		assertThat(PageIndex.getPageContainingRow(19, pageLength)).isEqualTo(PageIndex.ofFirstRowAndPageLength(10, pageLength));
	}

	@Test
	public void test_getLastRowIndex()
	{
		assertThat(PageIndex.ofFirstRowAndPageLength(0, 10).getLastRowIndex()).isEqualTo(9);
		assertThat(PageIndex.ofFirstRowAndPageLength(10, 10).getLastRowIndex()).isEqualTo(19);
	}

	@Nested
	public class pagesIterator
	{
		@Test
		public void startAt10_pageOf10_iterateUntil5_Expect_Empty()
		{
			final PageIndex page = PageIndex.ofFirstRowAndPageLength(10, 10);
			assertThat(page.iterator(5))
					.isEmpty();
		}

		@Test
		public void startAt10_pageOf10_iterateUntil9_Expect_Empty()
		{
			final PageIndex page = PageIndex.ofFirstRowAndPageLength(10, 10);
			assertThat(page.iterator(9))
					.isEmpty();
		}

		@Test
		public void startAt10_pageOf10_iterateUntil10_Expect_Empty()
		{
			final PageIndex page = PageIndex.ofFirstRowAndPageLength(10, 10);
			assertThat(page.iterator(10))
					.isEmpty();
		}

		@Test
		public void startAt10_pageOf10_iterateUntil11_Expect_OnePageWithOneElement()
		{
			final PageIndex page = PageIndex.ofFirstRowAndPageLength(10, 10);
			assertThat(page.iterator(11))
					.containsExactly(
							PageIndex.ofFirstRowAndPageLength(10, 1));
		}

		@Test
		public void startAt10_pageOf10_iterateUntil15()
		{
			final PageIndex page = PageIndex.ofFirstRowAndPageLength(10, 10);
			assertThat(page.iterator(15))
					.containsExactly(
							PageIndex.ofFirstRowAndPageLength(10, 5));
		}

		@Test
		public void startAt10_pageOf10_iterateUntil27()
		{
			final PageIndex page = PageIndex.ofFirstRowAndPageLength(10, 10);
			assertThat(page.iterator(27))
					.containsExactly(
							PageIndex.ofFirstRowAndPageLength(10, 10),
							PageIndex.ofFirstRowAndPageLength(20, 7));
		}
	}

	@Nested
	public class iterateAndStream_using_pageFetcher
	{
		private Stream<Integer> toRowIndices(final PageIndex page)
		{
			return IntStream.range(page.getFirstRow(), page.getLastRowIndex() + 1)
					.boxed();
		}

		@Test
		public void startAt10_pageOf10_iterateUntil5_Expect_Empty()
		{
			final PageIndex page = PageIndex.ofFirstRowAndPageLength(10, 10);
			assertThat(page.iterateAndStream(5, this::toRowIndices))
					.isEmpty();
		}

		@Test
		public void startAt10_pageOf10_iterateUntil9_Expect_Empty()
		{
			final PageIndex page = PageIndex.ofFirstRowAndPageLength(10, 10);
			assertThat(page.iterateAndStream(9, this::toRowIndices))
					.isEmpty();
		}

		@Test
		public void startAt10_pageOf10_iterateUntil10_Expect_Empty()
		{
			final PageIndex page = PageIndex.ofFirstRowAndPageLength(10, 10);
			assertThat(page.iterateAndStream(10, this::toRowIndices))
					.isEmpty();
		}

		@Test
		public void startAt10_pageOf10_iterateUntil11_Expect_OnePageWithOneElement()
		{
			final PageIndex page = PageIndex.ofFirstRowAndPageLength(10, 10);
			assertThat(page.iterateAndStream(11, this::toRowIndices))
					.containsExactly(10);
		}

		@Test
		public void startAt10_pageOf10_iterateUntil15()
		{
			final PageIndex page = PageIndex.ofFirstRowAndPageLength(10, 10);
			assertThat(page.iterateAndStream(15, this::toRowIndices))
					.containsExactly(10, 11, 12, 13, 14);
		}

		@Test
		public void startAt10_pageOf10_iterateUntil27()
		{
			final PageIndex page = PageIndex.ofFirstRowAndPageLength(10, 10);
			assertThat(page.iterateAndStream(27, this::toRowIndices))
					.containsExactly(
							10, 11, 12, 13, 14, 15, 16, 17, 18, 19, // first page (complete)
							20, 21, 22, 23, 24, 25, 26 // second page (partial)
					);
		}
	}

}
