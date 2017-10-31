package de.metas.ui.web.address;

import static org.adempiere.model.InterfaceWrapperHelper.load;
import static org.adempiere.model.InterfaceWrapperHelper.newInstance;
import static org.adempiere.model.InterfaceWrapperHelper.save;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.adempiere.ad.trx.api.ITrxManager;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Services;
import org.compiere.model.I_C_Region;
import org.compiere.util.CCache;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.metas.adempiere.model.I_C_Location;
import de.metas.adempiere.model.I_C_Postal;
import de.metas.adempiere.service.ILocationBL;
import de.metas.logging.LogManager;
import de.metas.ui.web.window.datatypes.DocumentId;
import de.metas.ui.web.window.datatypes.DocumentType;
import de.metas.ui.web.window.datatypes.LookupValue;
import de.metas.ui.web.window.datatypes.LookupValue.IntegerLookupValue;
import de.metas.ui.web.window.datatypes.json.JSONDocumentChangedEvent;
import de.metas.ui.web.window.descriptor.DocumentEntityDescriptor;
import de.metas.ui.web.window.exceptions.DocumentNotFoundException;
import de.metas.ui.web.window.model.Document;
import de.metas.ui.web.window.model.Document.CopyMode;
import de.metas.ui.web.window.model.IDocumentChangesCollector;
import de.metas.ui.web.window.model.IDocumentChangesCollector.ReasonSupplier;
import de.metas.ui.web.window.model.NullDocumentChangesCollector;

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

@Component
public class AddressRepository
{
	//
	// services
	private static final Logger logger = LogManager.getLogger(AddressRepository.class);
	@Autowired
	private AddressDescriptorFactory descriptorsFactory;

	//
	private final AtomicInteger nextAddressDocId = new AtomicInteger(1);
	private final CCache<DocumentId, Document> id2addressDoc = CCache.newLRUCache("AddressDocuments", 50, 0);

	private static final String VERSION_DEFAULT = "0";
	private static final ReasonSupplier REASON_ProcessAddressDocumentChanges = () -> "process Address document changes";

	public Document createNewFrom(final int fromC_Location_ID, final IDocumentChangesCollector changesCollector)
	{
		final DocumentEntityDescriptor entityDescriptor = descriptorsFactory.getAddressDescriptor()
				.getEntityDescriptor();

		final Document addressDoc = Document.builder(entityDescriptor)
				.initializeAsNewDocument(nextAddressDocId::getAndIncrement, VERSION_DEFAULT)
				.setChangesCollector(changesCollector)
				.build();

		final I_C_Location fromLocation = fromC_Location_ID <= 0 ? null : load(fromC_Location_ID, I_C_Location.class);
		if (fromLocation != null)
		{
			final IAddressModel address = InterfaceWrapperHelper.create(addressDoc, IAddressModel.class);
			updateAddressModelFromLocationRecord(address, fromLocation);
		}

		addressDoc.checkAndGetValidStatus();

		logger.trace("Created from C_Location_ID={}: {}", fromC_Location_ID, addressDoc);

		putAddressDocument(addressDoc);

		return addressDoc;
	}

	public AddressLayout getLayout()
	{
		return descriptorsFactory.getAddressDescriptor().getLayout();
	}

	private final void putAddressDocument(final Document addressDoc)
	{
		final Document addressDocReadonly = addressDoc.copy(CopyMode.CheckInReadonly, NullDocumentChangesCollector.instance);
		id2addressDoc.put(addressDoc.getDocumentId(), addressDocReadonly);

		logger.trace("Added to repository: {}", addressDocReadonly);
	}

	private final void removeAddressDocumentById(final DocumentId addressDocId)
	{
		final Document addressDocRemoved = id2addressDoc.remove(addressDocId);

		logger.trace("Removed from repository by ID={}: {}", addressDocId, addressDocRemoved);
	}

	private Document getInnerAddressDocument(final DocumentId addressDocId)
	{
		final Document addressDoc = id2addressDoc.get(addressDocId);
		if (addressDoc == null)
		{
			throw new DocumentNotFoundException(DocumentType.Address, AddressDescriptor.DocumentTypeId, addressDocId);
		}
		return addressDoc;
	}

	public Document getAddressDocumentForReading(final int addressDocIdInt)
	{
		final DocumentId addressDocId = DocumentId.of(addressDocIdInt);
		return getInnerAddressDocument(addressDocId).copy(CopyMode.CheckInReadonly, NullDocumentChangesCollector.instance);
	}

	private Document getAddressDocumentForWriting(final DocumentId addressDocId, final IDocumentChangesCollector changesCollector)
	{
		return getInnerAddressDocument(addressDocId).copy(CopyMode.CheckOutWritable, changesCollector);
	}

	public void processAddressDocumentChanges(final int addressDocIdInt, final List<JSONDocumentChangedEvent> events, final IDocumentChangesCollector changesCollector)
	{
		final DocumentId addressDocId = DocumentId.of(addressDocIdInt);
		final Document addressDoc = getAddressDocumentForWriting(addressDocId, changesCollector);
		addressDoc.processValueChanges(events, REASON_ProcessAddressDocumentChanges);

		Services.get(ITrxManager.class)
				.getCurrentTrxListenerManagerOrAutoCommit()
				.onAfterCommit(() -> putAddressDocument(addressDoc));
	}

	public LookupValue complete(final int addressDocIdInt)
	{
		final DocumentId addressDocId = DocumentId.of(addressDocIdInt);
		final Document addressDoc = getAddressDocumentForWriting(addressDocId, NullDocumentChangesCollector.instance);
		final IAddressModel address = InterfaceWrapperHelper.create(addressDoc, IAddressModel.class);

		final I_C_Location locationRecord = newInstance(I_C_Location.class);
		updateLocationRecordFromAddressModel(locationRecord, address);
		save(locationRecord);

		Services.get(ITrxManager.class)
				.getCurrentTrxListenerManagerOrAutoCommit()
				.onAfterCommit(() -> removeAddressDocumentById(addressDocId));

		final String locationStr = Services.get(ILocationBL.class).mkAddress(locationRecord);
		return IntegerLookupValue.of(locationRecord.getC_Location_ID(), locationStr);
	}

	/**
	 * note to developer: keep in sync with {@link #updateAddressModelFromLocationRecord(IAddressModel, I_C_Location)}
	 * 
	 * @param locationRecord
	 * @param address
	 */
	private static final void updateLocationRecordFromAddressModel(final I_C_Location locationRecord, final IAddressModel address)
	{
		locationRecord.setAddress1(address.getAddress1());
		locationRecord.setAddress2(address.getAddress2());
		locationRecord.setAddress3(address.getAddress3());
		locationRecord.setAddress4(address.getAddress4());

		locationRecord.setC_Country_ID(address.getC_Country_ID());

		locationRecord.setC_Region_ID(address.getC_Region_ID());
		final I_C_Region region = locationRecord.getC_Region();
		locationRecord.setRegionName(region != null ? region.getName() : null);

		final int postalId = address.isPostcodeLookup() && address.getC_Postal_ID() > 0 ? address.getC_Postal_ID() : -1;
		locationRecord.setC_Postal_ID(address.getC_Postal_ID());
		updateLocationRecordFromPostalId(locationRecord);

		//
		// No postal look: set data from fields
		if (postalId <= 0)
		{
			locationRecord.setPostal(address.getPostal());
			locationRecord.setCity(address.getCity());
		}
	}

	private static final void updateLocationRecordFromPostalId(final I_C_Location locationRecord)
	{
		final I_C_Postal postalRecord = locationRecord.getC_Postal();
		if (postalRecord != null)
		{
			locationRecord.setPostal(postalRecord.getPostal());
			locationRecord.setPostal_Add(postalRecord.getPostal_Add());
			locationRecord.setC_City_ID(postalRecord.getC_City_ID());
			locationRecord.setCity(postalRecord.getCity());

			locationRecord.setC_Country_ID(postalRecord.getC_Country_ID());

			locationRecord.setC_Region_ID(postalRecord.getC_Region_ID());
			locationRecord.setRegionName(postalRecord.getRegionName());
		}
		else
		{
			locationRecord.setPostal(null);
			locationRecord.setPostal_Add(null);
			locationRecord.setC_City_ID(-1);
			locationRecord.setCity(null);

			// locationRecord.setC_Country_ID(null);

			locationRecord.setC_Region_ID(-1);
			locationRecord.setRegionName(null);
		}
	}

	/**
	 * note to developers: keep in sync with {@link #updateLocationRecordFromAddressModel(I_C_Location, IAddressModel)}
	 */
	private static final void updateAddressModelFromLocationRecord(final IAddressModel address, final I_C_Location locationRecord)
	{
		address.setAddress1(locationRecord.getAddress1());
		address.setAddress2(locationRecord.getAddress2());
		address.setAddress3(locationRecord.getAddress3());
		address.setAddress4(locationRecord.getAddress3());
		address.setC_Country_ID(locationRecord.getC_Country_ID());
		address.setC_Postal_ID(locationRecord.getC_Postal_ID());
		address.setPostal(locationRecord.getPostal());
		address.setCity(locationRecord.getCity());
		address.setC_Region_ID(locationRecord.getC_Region_ID());
	}

}
