/*
 * Copyright 2014-2019 Logo Business Solutions
 * (a.k.a. LOGO YAZILIM SAN. VE TIC. A.S)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.lbs.tedam.ui.components.window.teststep.griddelete;

import com.lbs.tedam.data.service.PropertyService;
import com.lbs.tedam.exception.localized.LocalizedException;
import com.lbs.tedam.generator.steptype.GeneratorFactory;
import com.lbs.tedam.generator.steptype.GridDeleteGenerator;
import com.lbs.tedam.model.DTO.GridCell;
import com.lbs.tedam.ui.TedamFaceEvents.TestStepTypeParameterPreparedEvent;
import com.lbs.tedam.ui.components.CustomExceptions.TedamWindowNotAbleToOpenException;
import com.lbs.tedam.ui.components.basic.TedamWindow;
import com.lbs.tedam.ui.components.combobox.TedamGridTagComboBox;
import com.lbs.tedam.ui.util.Enums.UIParameter;
import com.lbs.tedam.ui.util.Enums.WindowSize;
import com.lbs.tedam.ui.util.TedamNotification;
import com.lbs.tedam.ui.util.TedamNotification.NotifyType;
import com.lbs.tedam.util.EnumsV2.TestStepType;
import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus.ViewEventBus;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringComponent
@ViewScope
public class WindowTestStepTypeGridDelete extends TedamWindow {

    private static final long serialVersionUID = 1L;

    private BeanFactory beanFactory;
    private GridDeleteWindowPresenter gridDeleteWindowPresenter;
    private TedamGridTagComboBox tedamGridTagComboBox;
    private TedamDynamicGridDelete dynamicGrid;

    @Autowired
    public WindowTestStepTypeGridDelete(ViewEventBus viewEventBus, GridDeleteWindowPresenter presenter, BeanFactory beanFactory, TedamDynamicGridDelete dynamicGrid,
                                        TedamGridTagComboBox tedamGridTagComboBox, PropertyService propertyService) {
        super(WindowSize.BIG, viewEventBus, propertyService);
        this.gridDeleteWindowPresenter = presenter;
        this.beanFactory = beanFactory;
        this.dynamicGrid = dynamicGrid;
        this.tedamGridTagComboBox = tedamGridTagComboBox;
    }

    @PostConstruct
    private void initView() {
        gridDeleteWindowPresenter.init(this);
    }

    @Override
    protected Component buildContent() throws LocalizedException {
        initTedamGridComboBox();

        addSection(getLocaleValue("view.viewedit.section.values"), tedamGridTagComboBox, dynamicGrid);
        gridDeleteWindowPresenter.fillComponentsWithValues();
        return getMainLayout();
    }

    private void initTedamGridComboBox() {
        tedamGridTagComboBox.addValueChangeListener(new ValueChangeListener<String>() {

            /** long serialVersionUID */
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(ValueChangeEvent<String> event) {
                if (event.getValue() != null) {
                    try {
                        gridDeleteWindowPresenter.onGridTagChanged(event.getValue());
                    } catch (LocalizedException e) {
                        logError(e);
                    }
                }
            }
        });
    }

    @Override
    public void publishCloseSuccessEvent() {
        getEventBus().publish(this, new TestStepTypeParameterPreparedEvent(gridDeleteWindowPresenter.getTestStep()));
    }

    private GridDeleteGenerator getGenerator() {
        GridDeleteGenerator generator = (GridDeleteGenerator) GeneratorFactory.getGenerator(TestStepType.GRID_DELETE, beanFactory);
        generator.setGridTag(tedamGridTagComboBox.getValue());
        generator.setRowIndexes(getRowIndexes(dynamicGrid.getSelectedComponent()));
        gridDeleteWindowPresenter.getTestStep().setGenerator(generator);

        return generator;
    }

    private List<Integer> getRowIndexes(List<GridCell> selectedGridRows) {
        List<Integer> selectedRowIndexList = new ArrayList<>();
        for (GridCell gridCell : selectedGridRows) {
            selectedRowIndexList.add(gridCell.getRowIndex());
        }
        return selectedRowIndexList;
    }

    @Override
    protected String getHeader() {
        return getLocaleValue("window.WindowTestStepTypeGridDelete.header");
    }

    @Override
    public void open(Map<UIParameter, Object> parameters) throws TedamWindowNotAbleToOpenException, LocalizedException {
        UI.getCurrent().addWindow(this);
        center();
        setModal(true);
        focus();
        gridDeleteWindowPresenter.enterWindow(parameters);
        initWindow();
    }

    public TedamGridTagComboBox getTedamGridTagComboBox() {
        return tedamGridTagComboBox;
    }

    public TedamDynamicGridDelete getDynamicGrid() {
        return dynamicGrid;
    }

    public void fillDynamicGrid(String gridTag) throws LocalizedException {
        dynamicGrid.resetGrid();
        List<List<GridCell>> gridItems = gridDeleteWindowPresenter.getGridItems(gridTag);
        if (gridItems.size() > 0) {
            dynamicGrid.initData(gridItems.get(0));
            dynamicGrid.setItems(gridItems);
        }
    }

    @Override
    protected boolean readyToClose() {
        if (!getGenerator().validate()) {
            TedamNotification.showNotification(getLocaleValue("window.readytoclose.teststeptype"), NotifyType.ERROR);
            return false;
        }
        return true;
    }

    @Override
    protected void windowClose() {

    }

}
