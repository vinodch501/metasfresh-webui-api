package de.metas.ui.web.view;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.adempiere.exceptions.DBException;

import de.metas.ui.web.document.filter.DocumentFilter;
import de.metas.ui.web.document.filter.provider.DocumentFilterDescriptorsProvider;
import de.metas.ui.web.document.filter.sql.SqlDocumentFilterConverterContext;
import de.metas.ui.web.view.util.PageIndex;
import de.metas.ui.web.window.datatypes.DocumentId;
import de.metas.ui.web.window.datatypes.DocumentIdsSelection;
import de.metas.ui.web.window.descriptor.DocumentFieldWidgetType;
import de.metas.ui.web.window.model.DocumentQueryOrderBy;
import de.metas.ui.web.window.model.sql.SqlOptions;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2017 metas GmbH
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

/**
 * View data repository.
 * This repository is responsible for fetching {@link IViewRow} or even their models.
 * 
 * @author metas-dev <dev@metasfresh.com>
 *
 */
public interface IViewDataRepository
{
	String getTableName();

	String getSqlWhereClause(ViewId viewId, List<DocumentFilter> filters, DocumentIdsSelection rowIds, SqlOptions sqlOpts);

	Map<String, DocumentFieldWidgetType> getWidgetTypesByFieldName();

	DocumentFilterDescriptorsProvider getViewFilterDescriptors();

	IViewRow retrieveById(ViewEvaluationCtx viewEvalCtx, ViewId viewId, DocumentId rowId);

	List<IViewRow> retrievePage(ViewEvaluationCtx viewEvalCtx, ViewRowIdsOrderedSelection orderedSelection, PageIndex pageIndex) throws DBException;

	List<DocumentId> retrieveRowIdsByPage(ViewEvaluationCtx viewEvalCtx, ViewRowIdsOrderedSelection orderedSelection, PageIndex pageIndex);

	<T> List<T> retrieveModelsByIds(ViewId viewId, DocumentIdsSelection rowIds, Class<T> modelClass);

	ViewRowIdsOrderedSelection createOrderedSelectionFromSelection(final ViewEvaluationCtx viewEvalCtx, ViewRowIdsOrderedSelection fromSelection, List<DocumentQueryOrderBy> orderBys);

	void deleteSelection(ViewId viewId);

	void scheduleDeleteSelections(Set<String> viewIds);

	ViewRowIdsOrderedSelection createOrderedSelection(ViewEvaluationCtx viewEvalCtx, ViewId viewId, List<DocumentFilter> filters, boolean applySecurityRestrictions, SqlDocumentFilterConverterContext context);

	ViewRowIdsOrderedSelection removeRowIdsNotMatchingFilters(ViewRowIdsOrderedSelection selection, List<DocumentFilter> filters, Set<DocumentId> rowIds);
}
