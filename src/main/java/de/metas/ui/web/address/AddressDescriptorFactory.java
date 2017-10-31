package de.metas.ui.web.address;

import org.adempiere.ad.callout.api.ICalloutField;
import org.adempiere.ad.expression.api.ILogicExpression;
import org.adempiere.ad.expression.api.impl.LogicExpressionCompiler;
import org.adempiere.service.ISysConfigBL;
import org.adempiere.util.Services;
import org.compiere.model.I_C_Country;
import org.compiere.util.CCache;
import org.springframework.stereotype.Component;

import de.metas.adempiere.model.I_C_Location;
import de.metas.i18n.IMsgBL;
import de.metas.ui.web.window.datatypes.DocumentType;
import de.metas.ui.web.window.datatypes.LookupValue.IntegerLookupValue;
import de.metas.ui.web.window.descriptor.DocumentEntityDataBindingDescriptor;
import de.metas.ui.web.window.descriptor.DocumentEntityDataBindingDescriptor.DocumentEntityDataBindingDescriptorBuilder;
import de.metas.ui.web.window.descriptor.DocumentEntityDescriptor;
import de.metas.ui.web.window.descriptor.DocumentFieldDescriptor;
import de.metas.ui.web.window.descriptor.DocumentFieldDescriptor.Characteristic;
import de.metas.ui.web.window.descriptor.DocumentFieldWidgetType;
import de.metas.ui.web.window.descriptor.DocumentLayoutElementDescriptor;
import de.metas.ui.web.window.descriptor.DocumentLayoutElementFieldDescriptor;
import de.metas.ui.web.window.model.DocumentsRepository;

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
public class AddressDescriptorFactory
{
	private final CCache<Integer, AddressDescriptor> cache = CCache.newLRUCache("AddressDescriptor", 1, 0);

	private static final String SYSCONFIG_UsePostalLookup = "de.metas.ui.web.address.UsePostalLookup";

	public AddressDescriptor getAddressDescriptor()
	{
		final int key = 0; // some dummy key
		return cache.getOrLoad(key, () -> createAddressDescriptor());
	}

	private static boolean isUsePostalLookup()
	{
		final boolean defaultWhenNotFound = false; // don't use postal lookup by default
		return Services.get(ISysConfigBL.class).getBooleanValue(SYSCONFIG_UsePostalLookup, defaultWhenNotFound);
	}

	private AddressDescriptor createAddressDescriptor()
	{
		final DocumentEntityDescriptor entityDescriptor = createAddressEntityDescriptor();
		final AddressLayout layout = createLayout(entityDescriptor);
		return AddressDescriptor.of(entityDescriptor, layout);
	}

	private DocumentEntityDescriptor createAddressEntityDescriptor()
	{
		final DocumentEntityDescriptor.Builder addressDescriptor = DocumentEntityDescriptor.builder()
				.setDocumentType(DocumentType.Address, AddressDescriptor.DocumentTypeId) // we have only one descriptor for all addresses
				.setCaption(Services.get(IMsgBL.class).getTranslatableMsgText(I_C_Location.COLUMNNAME_C_Location_ID))
				.setDataBinding(new AddressDataBindingDescriptorBuilder())
				.disableDefaultTableCallouts()
		//
		;

		//
		// Address1 ... Address4 fields
		addressDescriptor.addField(buildFieldDescriptor(IAddressModel.COLUMNNAME_Address1)
				.setWidgetType(DocumentFieldWidgetType.Text));
		//
		addressDescriptor.addField(buildFieldDescriptor(IAddressModel.COLUMNNAME_Address2)
				.setWidgetType(DocumentFieldWidgetType.Text));
		//
		addressDescriptor.addField(buildFieldDescriptor(IAddressModel.COLUMNNAME_Address3)
				.setWidgetType(DocumentFieldWidgetType.Text));
		//
		addressDescriptor.addField(buildFieldDescriptor(IAddressModel.COLUMNNAME_Address4)
				.setWidgetType(DocumentFieldWidgetType.Text));

		//
		// Country
		final AddressCountryLookupDescriptor countryLookup = AddressCountryLookupDescriptor.newInstance();
		addressDescriptor.addField(buildFieldDescriptor(IAddressModel.COLUMNNAME_C_Country_ID)
				.setWidgetType(DocumentFieldWidgetType.Lookup)
				.setValueClass(IntegerLookupValue.class)
				.setMandatoryLogic(true)
				.setLookupDescriptorProvider(countryLookup)
				.addCallout(AddressCallout::onC_Country_ID));
		addressDescriptor.addField(buildFieldDescriptor(IAddressModel.COLUMNNAME_HasRegion)
				.setWidgetType(DocumentFieldWidgetType.YesNo)
				.removeCharacteristic(Characteristic.PublicField)); // internal field (not displayed!)
		addressDescriptor.addField(buildFieldDescriptor(IAddressModel.COLUMNNAME_IsPostcodeLookup)
				.setWidgetType(DocumentFieldWidgetType.YesNo)
				.removeCharacteristic(Characteristic.PublicField)); // internal field (not displayed!)

		//
		// Postal lookup
		final ILogicExpression isPostalLookup = LogicExpressionCompiler.instance.compile("@" + IAddressModel.COLUMNNAME_IsPostcodeLookup + "/N@=Y");
		final AddressPostalLookupDescriptor postalLookup = AddressPostalLookupDescriptor.ofCountryLookup(countryLookup);
		addressDescriptor.addField(buildFieldDescriptor(IAddressModel.COLUMNNAME_C_Postal_ID)
				.setWidgetType(DocumentFieldWidgetType.Lookup)
				.setValueClass(IntegerLookupValue.class)
				.setDisplayLogic(isPostalLookup)
				.setLookupDescriptorProvider(postalLookup));

		//
		// Postal, City, Region
		final ILogicExpression isNotPostalLookup = isPostalLookup.negate();
		addressDescriptor.addField(buildFieldDescriptor(IAddressModel.COLUMNNAME_Postal)
				.setWidgetType(DocumentFieldWidgetType.Text)
				.setDisplayLogic(isNotPostalLookup));
		//
		addressDescriptor.addField(buildFieldDescriptor(IAddressModel.COLUMNNAME_City)
				.setWidgetType(DocumentFieldWidgetType.Text)
				.setDisplayLogic(isNotPostalLookup));
		//
		final ILogicExpression hasRegions = LogicExpressionCompiler.instance.compile("@" + IAddressModel.COLUMNNAME_HasRegion + "/N@=Y");
		addressDescriptor.addField(buildFieldDescriptor(IAddressModel.COLUMNNAME_C_Region_ID)
				.setWidgetType(DocumentFieldWidgetType.Lookup)
				.setValueClass(IntegerLookupValue.class)
				.setDisplayLogic(isNotPostalLookup.and(hasRegions))
				.setLookupDescriptorProvider(AddressRegionLookupDescriptor.newInstance()));

		//
		// Build it and return
		return addressDescriptor.build();
	}

	private DocumentFieldDescriptor.Builder buildFieldDescriptor(final String columnName)
	{
		return DocumentFieldDescriptor.builder(columnName)
				.setCaption(Services.get(IMsgBL.class).getTranslatableMsgText(columnName))
				.setLookupDescriptorProvider_None()
				//
				.setReadonlyLogic(false)
				.setDisplayLogic(true)
				.setMandatoryLogic(false)
				//
				.addCharacteristic(Characteristic.PublicField)
				.setDataBinding(AddressFieldBinding.ofColumnName(columnName));

	}

	private static AddressLayout createLayout(final DocumentEntityDescriptor addressDescriptor)
	{
		final AddressLayout.Builder layout = AddressLayout.builder();

		addressDescriptor.getFields()
				.stream()
				.filter(fieldDescriptor -> fieldDescriptor.hasCharacteristic(Characteristic.PublicField))
				.map(fieldDescriptor -> createLayoutElement(fieldDescriptor))
				.forEach(layoutElement -> layout.addElement(layoutElement));

		return layout.build();
	}

	private static DocumentLayoutElementDescriptor.Builder createLayoutElement(final DocumentFieldDescriptor fieldDescriptor)
	{
		return DocumentLayoutElementDescriptor.builder()
				.setCaption(fieldDescriptor.getCaption())
				.setWidgetType(fieldDescriptor.getWidgetType())
				.addField(DocumentLayoutElementFieldDescriptor.builder(fieldDescriptor.getFieldName())
						.setLookupSource(fieldDescriptor.getLookupSourceType())
						.setPublicField(true)
						.setSupportZoomInto(fieldDescriptor.isSupportZoomInto()));
	}

	private static class AddressDataBindingDescriptorBuilder implements DocumentEntityDataBindingDescriptorBuilder
	{
		private final DocumentEntityDataBindingDescriptor dataBinding = new DocumentEntityDataBindingDescriptor()
		{
			@Override
			public DocumentsRepository getDocumentsRepository()
			{
				throw new IllegalStateException("No repository available for " + this);
			}
		};

		private AddressDataBindingDescriptorBuilder()
		{
			super();
		}

		@Override
		public DocumentEntityDataBindingDescriptor getOrBuild()
		{
			return dataBinding;
		}
	}

	private static final class AddressCallout
	{
		private static final void onC_Country_ID(final ICalloutField calloutField)
		{
			final IAddressModel location = calloutField.getModel(IAddressModel.class);
			final I_C_Country country = location.getC_Country();

			final boolean hasRegions = country != null && country.isHasRegion();
			location.setHasRegion(hasRegions);

			final boolean postalLookup = country != null && country.isPostcodeLookup() && isUsePostalLookup();
			location.setPostcodeLookup(postalLookup);
		}

	}
}
