package de.metas.ui.web.window.datatypes.json;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.adempiere.ad.expression.api.LogicExpressionResult;
import org.adempiere.util.GuavaCollectors;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import de.metas.ui.web.view.IDocumentView;
import de.metas.ui.web.view.json.JSONDocumentViewLayout;
import de.metas.ui.web.window.WindowConstants;
import de.metas.ui.web.window.datatypes.DocumentPath;
import de.metas.ui.web.window.descriptor.DetailId;
import de.metas.ui.web.window.exceptions.InvalidDocumentPathException;
import de.metas.ui.web.window.model.Document;
import de.metas.ui.web.window.model.DocumentChanges;
import de.metas.ui.web.window.model.DocumentSaveStatus;
import de.metas.ui.web.window.model.DocumentValidStatus;
import de.metas.ui.web.window.model.IDocumentChangesCollector;
import de.metas.ui.web.window.model.IIncludedDocumentsCollection;
import io.swagger.annotations.ApiModel;

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

/**
 * JSON format:
 * <code>
 * [ { field:someFieldName }, {...} ]
 * </code>
 *
 * @author metas-dev <dev@metasfresh.com>
 *
 */
@ApiModel("document")
@SuppressWarnings("serial")
// @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE) // cannot use it because of "otherProperties"
public final class JSONDocument implements Serializable
{
	public static final JSONDocument ofDocument(final Document document, final JSONOptions jsonOpts)
	{
		final JSONDocument jsonDocument = new JSONDocument(document.getDocumentPath());

		//
		// Fields
		{
			final List<JSONDocumentField> jsonFields = new ArrayList<>();

			// Add pseudo "ID" field first
			jsonFields.add(0, JSONDocumentField.idField(document.getDocumentIdAsJson()));

			// Append the other fields
			document.getFieldViews()
					.stream()
					.filter(jsonOpts.documentFieldFilter())
					.map(JSONDocumentField::ofDocumentField)
					.forEach(jsonFields::add);

			jsonDocument.setFields(jsonFields);
		}

		//
		// Valid Status
		final DocumentValidStatus documentValidStatus = document.getValidStatus();
		if (documentValidStatus != null)
		{
			jsonDocument.setValidStatus(documentValidStatus);
		}

		//
		// Save Status
		final DocumentSaveStatus documentSaveStatus = document.getSaveStatus();
		if (documentSaveStatus != null)
		{
			jsonDocument.setSaveStatus(documentSaveStatus);
		}

		//
		// Included tabs info
		document.getIncludedDocumentsCollections()
				.stream()
				.map(JSONIncludedTabInfo::new)
				.forEach(jsonDocument::setIncludedTabInfo);

		//
		// Set debugging info
		if (WindowConstants.isProtocolDebugging())
		{
			jsonDocument.putDebugProperty("tablename", document.getEntityDescriptor().getTableNameOrNull());
			jsonDocument.putDebugProperty(JSONOptions.DEBUG_ATTRNAME, jsonOpts.toString());
			jsonDocument.putDebugProperty("fields-count", jsonDocument.getFieldsCount());
		}

		return jsonDocument;
	}

	/**
	 * @param documents
	 * @param includeFieldsList
	 * @return list of {@link JSONDocument}s
	 */
	public static List<JSONDocument> ofDocumentsList(final Collection<Document> documents, final JSONOptions jsonOpts)
	{
		return documents.stream()
				.map(document -> ofDocument(document, jsonOpts))
				.collect(Collectors.toList());
	}

	public static List<JSONDocument> ofEvents(final IDocumentChangesCollector documentChangesCollector, final JSONOptions jsonOpts)
	{
		return documentChangesCollector.streamOrderedDocumentChanges()
				.map(documentChanges -> ofEventOrNull(documentChanges, jsonOpts))
				.filter(jsonDocument -> jsonDocument != null)
				.collect(ImmutableList.toImmutableList());
	}

	private static JSONDocument ofEventOrNull(final DocumentChanges documentChangedEvents, final JSONOptions jsonOpts)
	{
		if (documentChangedEvents.isEmpty())
		{
			return null;
		}

		final JSONDocument jsonDocument = new JSONDocument(documentChangedEvents.getDocumentPath());

		// If the document was deleted, we just need to export that flag. All the other changes are not relevant.
		if(documentChangedEvents.isDeleted())
		{
			jsonDocument.setDeleted();
			return jsonDocument;
		}

		//
		// Fields
		{
			final List<JSONDocumentField> jsonFields = new ArrayList<>();
			documentChangedEvents.getFieldChangesList()
					.stream()
					.filter(jsonOpts.documentFieldChangeFilter())
					.forEach((field) -> {
						// Add the pseudo-field "ID" first
						if (field.isKey())
						{
							jsonFields.add(0, JSONDocumentField.idField(field.getValueAsJsonObject()));
						}

						// Append the other fields
						jsonFields.add(JSONDocumentField.ofDocumentFieldChangedEvent(field));
					});
			jsonDocument.setFields(jsonFields);
		}

		//
		// Valid status
		final DocumentValidStatus documentValidStatus = documentChangedEvents.getDocumentValidStatus();
		if (documentValidStatus != null)
		{
			jsonDocument.setValidStatus(documentValidStatus);
		}

		//
		// Save status
		final DocumentSaveStatus documentSaveStatus = documentChangedEvents.getDocumentSaveStatus();
		if (documentSaveStatus != null)
		{
			jsonDocument.setSaveStatus(documentSaveStatus);
		}

		//
		// Included tabs info
		documentChangedEvents.getIncludedDetailInfos()
				.stream()
				.map(JSONIncludedTabInfo::new)
				.forEach(jsonDocument::setIncludedTabInfo);

		return jsonDocument;
	}

	public static List<JSONDocument> ofDocumentViewList(final List<IDocumentView> documentViews)
	{
		return documentViews.stream()
				.map(JSONDocument::ofDocumentView)
				.collect(Collectors.toList());
	}

	public static JSONDocument ofDocumentView(final IDocumentView documentView)
	{
		final JSONDocument jsonDocument = new JSONDocument(documentView.getDocumentPath());

		//
		// Fields
		{
			final List<JSONDocumentField> jsonFields = new ArrayList<>();

			// Add pseudo "ID" field first
			final String idFieldName = documentView.getIdFieldNameOrNull();
			if (idFieldName != null)
			{
				final Object id = documentView.getDocumentId().toJson();
				jsonFields.add(0, JSONDocumentField.idField(id));
			}

			// Append the other fields
			documentView.getFieldNameAndJsonValues()
					.entrySet()
					.stream()
					.map(e -> JSONDocumentField.ofNameAndValue(e.getKey(), e.getValue()))
					.forEach(jsonFields::add);

			jsonDocument.setFields(jsonFields);

			//
			// Document view record specific attributes
			if (documentView.isProcessed())
			{
				jsonDocument.putOtherProperty("processed", true);
			}
			if (documentView.hasAttributes())
			{
				jsonDocument.putOtherProperty(JSONDocumentViewLayout.PROPERTY_supportAttributes, true);
			}
			if (documentView.getType() != null)
			{
				jsonDocument.putOtherProperty("type", documentView.getType().getName());
			}
		}

		//
		// Included documents if any
		{
			final List<? extends IDocumentView> includedDocuments = documentView.getIncludedDocuments();
			if (!includedDocuments.isEmpty())
			{
				final List<JSONDocument> jsonIncludedDocuments = includedDocuments
						.stream()
						.map(JSONDocument::ofDocumentView)
						.collect(GuavaCollectors.toImmutableList());
				jsonDocument.setIncludedDocuments(jsonIncludedDocuments);
			}
		}

		return jsonDocument;
	}

	@JsonProperty("id")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final String id;

	@JsonProperty("tabid")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final String tabid;

	@JsonProperty("rowId")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final String rowId;

	@JsonProperty("validStatus")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private DocumentValidStatus validStatus;

	@JsonProperty("saveStatus")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private DocumentSaveStatus saveStatus;
	
	@JsonProperty("deleted")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Boolean deleted;

	@JsonProperty("fields")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@JsonSerialize(using = JsonMapAsValuesListSerializer.class)
	private Map<String, JSONDocumentField> fieldsByName;

	@JsonProperty("includedTabsInfo")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@JsonSerialize(using = JsonMapAsValuesListSerializer.class)
	private Map<String, JSONIncludedTabInfo> includedTabsInfo;

	@JsonProperty("includedDocuments")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<JSONDocument> includedDocuments;

	private final Map<String, Object> otherProperties = new LinkedHashMap<>();

	public JSONDocument(final DocumentPath documentPath)
	{
		super();

		if (documentPath == null)
		{
			id = null;
			tabid = null;
			rowId = null;
		}
		else if (documentPath.isRootDocument())
		{
			id = documentPath.getDocumentId().toJson();
			tabid = null;
			rowId = null;
		}
		else if (documentPath.isSingleIncludedDocument())
		{
			id = documentPath.getDocumentId().toJson();
			final DetailId detailId = documentPath.getDetailId();
			tabid = DetailId.toJson(detailId);
			rowId = documentPath.getSingleRowId().toJson();
		}
		else
		{
			// shall not happen
			throw new InvalidDocumentPathException(documentPath, "only root path and single included document path are allowed");
		}
	}

	@JsonCreator
	private JSONDocument(
			@JsonProperty("id") final String id //
			, @JsonProperty("tabid") final String tabid //
			, @JsonProperty("rowId") final String rowId //
			, @JsonProperty("valid-status") final String validStatus //
			, @JsonProperty("save-status") final String saveStatus //
			, @JsonProperty("fields") final List<JSONDocumentField> fields //
			)
	{
		super();
		this.id = id;
		this.tabid = tabid;
		this.rowId = rowId;
		setFields(fields);
	}

	public String getId()
	{
		return id;
	}

	public String getTabid()
	{
		return tabid;
	}

	public String getRowId()
	{
		return rowId;
	}

	private void setValidStatus(final DocumentValidStatus validStatus)
	{
		this.validStatus = validStatus;
	}

	public DocumentValidStatus getValidStatus()
	{
		return validStatus;
	}

	private void setSaveStatus(final DocumentSaveStatus documentSaveStatus)
	{
		saveStatus = documentSaveStatus;
	}

	public DocumentSaveStatus getSaveStatus()
	{
		return saveStatus;
	}
	
	public void setDeleted()
	{
		this.deleted = Boolean.TRUE;
	}

	@JsonAnyGetter
	public Map<String, Object> getOtherProperties()
	{
		return otherProperties;
	}

	@JsonAnySetter
	public void putOtherProperty(final String name, final Object jsonValue)
	{
		otherProperties.put(name, jsonValue);
	}

	public JSONDocument putDebugProperty(final String name, final Object jsonValue)
	{
		otherProperties.put("debug-" + name, jsonValue);
		return this;
	}

	public void setFields(final Collection<JSONDocumentField> fields)
	{
		fieldsByName = fields == null ? null : Maps.uniqueIndex(fields, (field) -> field.getField());
	}

	@JsonIgnore
	public int getFieldsCount()
	{
		return fieldsByName == null ? 0 : fieldsByName.size();
	}

	private void setIncludedTabInfo(final JSONIncludedTabInfo tabInfo)
	{
		if (includedTabsInfo == null)
		{
			includedTabsInfo = new HashMap<>();
		}
		includedTabsInfo.put(tabInfo.tabid, tabInfo);
	}

	private void setIncludedDocuments(final List<JSONDocument> includedDocuments)
	{
		this.includedDocuments = includedDocuments;
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
	private static final class JSONIncludedTabInfo
	{
		@JsonProperty("tabid")
		private final String tabid;

		@JsonProperty("stale")
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private final Boolean stale;

		@JsonProperty("allowCreateNew")
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private final Boolean allowCreateNew;
		@JsonProperty("allowCreateNewReason")
		@JsonInclude(JsonInclude.Include.NON_EMPTY)
		private final String allowCreateNewReason;

		@JsonProperty("allowDelete")
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private final Boolean allowDelete;
		@JsonProperty("allowDeleteReason")
		@JsonInclude(JsonInclude.Include.NON_EMPTY)
		private final String allowDeleteReason;

		private JSONIncludedTabInfo(final IIncludedDocumentsCollection includedDocumentsCollection)
		{
			tabid = DetailId.toJson(includedDocumentsCollection.getDetailId());

			final boolean stale = includedDocumentsCollection.isStale();
			this.stale = stale ? Boolean.TRUE : null;

			final LogicExpressionResult allowCreateNew = includedDocumentsCollection.getAllowCreateNewDocument();
			if (allowCreateNew != null)
			{
				this.allowCreateNew = allowCreateNew.booleanValue();
				allowCreateNewReason = allowCreateNew.getName();
			}
			else
			{
				this.allowCreateNew = null;
				allowCreateNewReason = null;
			}

			final LogicExpressionResult allowDelete = includedDocumentsCollection.getAllowDeleteDocument();
			if (allowDelete != null)
			{
				this.allowDelete = allowDelete.booleanValue();
				allowDeleteReason = allowDelete.getName();
			}
			else
			{
				this.allowDelete = null;
				allowDeleteReason = null;
			}
		}

		private JSONIncludedTabInfo(final DocumentChanges.IncludedDetailInfo includedDetailInfo)
		{
			tabid = DetailId.toJson(includedDetailInfo.getDetailId());

			final boolean stale = includedDetailInfo.isStale();
			this.stale = stale ? Boolean.TRUE : null;

			final LogicExpressionResult allowCreateNew = includedDetailInfo.getAllowNew();
			if (allowCreateNew != null)
			{
				this.allowCreateNew = allowCreateNew.booleanValue();
				allowCreateNewReason = allowCreateNew.getName();
			}
			else
			{
				this.allowCreateNew = null;
				allowCreateNewReason = null;
			}

			final LogicExpressionResult allowDelete = includedDetailInfo.getAllowDelete();
			if (allowDelete != null)
			{
				this.allowDelete = allowDelete.booleanValue();
				allowDeleteReason = allowDelete.getName();
			}
			else
			{
				this.allowDelete = null;
				allowDeleteReason = null;
			}
		}
	}
}