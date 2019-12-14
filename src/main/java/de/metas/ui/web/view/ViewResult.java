package de.metas.ui.web.view;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import de.metas.i18n.ITranslatableString;
import de.metas.ui.web.document.filter.DocumentFilter;
import de.metas.ui.web.view.util.PageIndex;
import de.metas.ui.web.window.datatypes.DocumentId;
import de.metas.ui.web.window.model.DocumentQueryOrderBy;
import de.metas.util.Check;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2016 metas GmbH
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

@Immutable
@ToString
public final class ViewResult
{
	/**
	 * Creates a view result having given loaded page
	 */
	public static ViewResult ofViewAndPage(
			@NonNull final IView view,
			@NonNull final PageIndex pageIndex,
			final List<DocumentQueryOrderBy> orderBys,
			final List<? extends IViewRow> page)
	{
		return builder()
				.view(view)
				.pageIndex(pageIndex)
				.orderBys(orderBys)
				.rows(page)
				.build();
	}

	/**
	 * Creates a view result without any loaded page.
	 */
	public static ViewResult ofView(final IView view)
	{
		return new ViewResult(view);
	}

	//
	// View info
	@Getter
	private final ViewId viewId;
	@Getter
	private final ViewProfileId profileId;
	@Getter
	private final ViewId parentViewId;
	private final ITranslatableString viewDescription;
	@Getter
	private final ViewHeaderProperties viewHeaderProperties;
	@Getter
	private final long size;
	@Getter
	private final int queryLimit;
	@Getter
	private final boolean queryLimitHit;

	@Getter
	private final ImmutableList<DocumentFilter> stickyFilters;
	@Getter
	private final ImmutableList<DocumentFilter> filters;
	@Getter
	private final ImmutableList<DocumentQueryOrderBy> orderBys;

	//
	// Page info
	@Nullable
	private final PageIndex pageIndex;
	private final ImmutableList<DocumentId> rowIds;
	private final ImmutableList<IViewRow> page;
	@Getter
	private final ImmutableMap<String, ViewResultColumn> columnInfosByFieldName;

	/**
	 * +
	 * View and loaded page constructor
	 */
	@Builder
	private ViewResult(
			@NonNull final IView view,
			@NonNull final PageIndex pageIndex,
			@NonNull final Integer pageLength,
			@NonNull final List<DocumentQueryOrderBy> orderBys,
			@Nullable final List<DocumentId> rowIds,
			@Nullable final List<? extends IViewRow> rows,
			@Nullable final List<ViewResultColumn> columnInfos)
	{
		this.viewId = view.getViewId();
		this.profileId = view.getProfileId();
		this.parentViewId = view.getParentViewId();
		this.viewDescription = view.getDescription();
		this.viewHeaderProperties = view.getHeaderProperties() != null ? view.getHeaderProperties() : ViewHeaderProperties.EMPTY;
		this.size = view.size();
		this.queryLimit = view.getQueryLimit();
		this.queryLimitHit = view.isQueryLimitHit();

		stickyFilters = ImmutableList.copyOf(view.getStickyFilters());
		filters = ImmutableList.copyOf(view.getFilters());
		this.orderBys = ImmutableList.copyOf(orderBys);

		//
		// Page
		if (rowIds == null && rows == null)
		{
			throw new IllegalArgumentException("rowIds or rows shall not be null");
		}
		this.pageIndex = pageIndex;
		this.rowIds = rowIds != null ? ImmutableList.copyOf(rowIds) : null;
		this.page = rows != null ? ImmutableList.copyOf(rows) : null;
		this.columnInfosByFieldName = columnInfos != null ? Maps.uniqueIndex(columnInfos, ViewResultColumn::getFieldName)
				: ImmutableMap.of();
	}

	/** View (WITHOUT loaded page) constructor */
	private ViewResult(final IView view)
	{
		this.viewId = view.getViewId();
		this.profileId = view.getProfileId();
		this.parentViewId = view.getParentViewId();
		this.viewDescription = view.getDescription();
		this.viewHeaderProperties = view.getHeaderProperties() != null ? view.getHeaderProperties() : ViewHeaderProperties.EMPTY;
		this.size = view.size();
		this.queryLimit = view.getQueryLimit();
		this.queryLimitHit = view.isQueryLimitHit();

		stickyFilters = ImmutableList.copyOf(view.getStickyFilters());
		filters = ImmutableList.copyOf(view.getFilters());
		orderBys = ImmutableList.copyOf(view.getDefaultOrderBys());

		//
		// Page
		pageIndex = null;
		rowIds = null;
		page = null;
		columnInfosByFieldName = ImmutableMap.of();
	}

	public String getViewDescription(final String adLanguage)
	{
		if (viewDescription == null)
		{
			return null;
		}
		final String viewDescriptionStr = viewDescription.translate(adLanguage);
		return !Check.isEmpty(viewDescriptionStr, true) ? viewDescriptionStr : null;
	}

	public int getFirstRow()
	{
		return pageIndex != null ? pageIndex.getFirstRow() : 0;
	}

	public int getPageLength()
	{
		return pageIndex != null ? pageIndex.getPageLength() : 0;
	}

	public boolean isPageLoaded()
	{
		return page != null;
	}

	public List<DocumentId> getRowIds()
	{
		if (rowIds != null)
		{
			return rowIds;
		}
		else
		{
			return getPage().stream().map(IViewRow::getId).collect(ImmutableList.toImmutableList());
		}
	}

	public boolean isEmpty()
	{
		return getPage().isEmpty();
	}

	/**
	 * @return loaded page
	 * @throws IllegalStateException if the page is not loaded, see {@link #isPageLoaded()}
	 */
	public List<IViewRow> getPage()
	{
		if (page == null)
		{
			throw new IllegalStateException("page not loaded for " + this);
		}
		return page;
	}
}
