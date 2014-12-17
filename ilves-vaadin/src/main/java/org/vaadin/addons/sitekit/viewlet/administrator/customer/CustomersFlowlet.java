/**
 * Copyright 2013 Tommi S.E. Laukkanen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vaadin.addons.sitekit.viewlet.administrator.customer;

import com.vaadin.data.util.filter.Compare;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import org.vaadin.addons.lazyquerycontainer.EntityContainer;
import org.vaadin.addons.sitekit.flow.AbstractFlowlet;
import org.vaadin.addons.sitekit.grid.FieldDescriptor;
import org.vaadin.addons.sitekit.grid.FilterDescriptor;
import org.vaadin.addons.sitekit.grid.FormattingTable;
import org.vaadin.addons.sitekit.grid.Grid;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.model.Customer;
import org.vaadin.addons.sitekit.model.PostalAddress;
import org.vaadin.addons.sitekit.security.SecurityService;
import org.vaadin.addons.sitekit.site.SiteFields;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Customer list flow.
 *
 * @author Tommi S.E. Laukkanen
 */
public final class CustomersFlowlet extends AbstractFlowlet {

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** The entity container. */
    private EntityContainer<Customer> entityContainer;
    /** The customer grid. */
    private Grid entityGrid;

    @Override
    public String getFlowletKey() {
        return "customers";
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void initialize() {
        final List<FieldDescriptor> fieldDefinitions = SiteFields.getFieldDescriptors(Customer.class);

        final List<FilterDescriptor> filterDefinitions = new ArrayList<FilterDescriptor>();
        //filterDefinitions.add(new FilterDescriptor("companyName", "companyName", "Company Name", new TextField(), 101, "=", String.class, ""));
        //filterDefinitions.add(new FilterDescriptor("lastName", "lastName", "Last Name", new TextField(), 101, "=", String.class, ""));

        final EntityManager entityManager = getSite().getSiteContext().getObject(EntityManager.class);
        entityContainer = new EntityContainer<Customer>(entityManager, true, false, false, Customer.class, 1000, new String[] { "companyName",
                "lastName", "firstName" }, new boolean[] { true, true, true }, "customerId");

        for (final FieldDescriptor fieldDefinition : fieldDefinitions) {
            entityContainer.addContainerProperty(fieldDefinition.getId(), fieldDefinition.getValueType(), fieldDefinition.getDefaultValue(),
                    fieldDefinition.isReadOnly(), fieldDefinition.isSortable());
        }

        final GridLayout gridLayout = new GridLayout(1, 2);
        gridLayout.setSizeFull();
        gridLayout.setMargin(false);
        gridLayout.setSpacing(true);
        gridLayout.setRowExpandRatio(1, 1f);
        setViewContent(gridLayout);

        final HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setSizeUndefined();
        gridLayout.addComponent(buttonLayout, 0, 0);

        final Table table = new FormattingTable();
        entityGrid = new Grid(table, entityContainer);
        entityGrid.setFields(fieldDefinitions);
        entityGrid.setFilters(filterDefinitions);
        //entityGrid.setFixedWhereCriteria("e.owner.companyId=:companyId");

        table.setColumnCollapsed("created", true);
        table.setColumnCollapsed("modified", true);
        table.setColumnCollapsed("company", true);
        gridLayout.addComponent(entityGrid, 0, 1);

        final Button addButton = new Button("Add");
        addButton.setIcon(getSite().getIcon("button-icon-add"));
        buttonLayout.addComponent(addButton);

        addButton.addClickListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                final Customer customer = new Customer();
                customer.setCreated(new Date());
                customer.setModified(customer.getCreated());
                customer.setInvoicingAddress(new PostalAddress());
                customer.setDeliveryAddress(new PostalAddress());
                customer.setOwner((Company) getSite().getSiteContext().getObject(Company.class));
                final CustomerFlowlet customerView = getFlow().forward(CustomerFlowlet.class);
                customerView.edit(customer, true);
            }
        });

        final Button editButton = new Button("Edit");
        editButton.setIcon(getSite().getIcon("button-icon-edit"));
        buttonLayout.addComponent(editButton);
        editButton.addClickListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                if (entityGrid.getSelectedItemId() == null) {
                    return;
                }
                final Customer entity = entityContainer.getEntity(entityGrid.getSelectedItemId());
                final CustomerFlowlet customerView = getFlow().forward(CustomerFlowlet.class);
                customerView.edit(entity, false);
            }
        });

        final Button removeButton = new Button("Remove");
        removeButton.setIcon(getSite().getIcon("button-icon-remove"));
        buttonLayout.addComponent(removeButton);
        removeButton.addClickListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                if (entityGrid.getSelectedItemId() == null) {
                    return;
                }
                final Customer entity = entityContainer.getEntity(entityGrid.getSelectedItemId());
                SecurityService.removeCustomer(getSite().getSiteContext(), entity);
                entityContainer.refresh();
            }
        });

    }

    @Override
    public void enter() {

        final Company company = getSite().getSiteContext().getObject(Company.class);
        //entityGrid.getFixedWhereParameters().put("companyId", company.getCompanyId());
        entityContainer.removeDefaultFilters();
        entityContainer.addDefaultFilter(new Compare.Equal("owner.companyId", company.getCompanyId()));

        entityGrid.refresh();
    }

}
