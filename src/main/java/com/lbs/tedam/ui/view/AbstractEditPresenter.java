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

package com.lbs.tedam.ui.view;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.spring.events.EventBus.ViewEventBus;

import com.lbs.tedam.app.security.SecurityUtils;
import com.lbs.tedam.data.service.BaseService;
import com.lbs.tedam.data.service.GridPreferenceService;
import com.lbs.tedam.data.service.PropertyService;
import com.lbs.tedam.data.service.TedamUserService;
import com.lbs.tedam.exception.localized.LocalizedException;
import com.lbs.tedam.localization.TedamLocalizerWrapper;
import com.lbs.tedam.model.AbstractBaseEntity;
import com.lbs.tedam.model.GridPreference;
import com.lbs.tedam.model.Project;
import com.lbs.tedam.model.TedamUser;
import com.lbs.tedam.ui.components.ConfirmPopup;
import com.lbs.tedam.ui.components.basic.TedamButton;
import com.lbs.tedam.ui.components.basic.TedamDateField;
import com.lbs.tedam.ui.components.basic.TedamDateTimeField;
import com.lbs.tedam.ui.components.basic.TedamMenuBar;
import com.lbs.tedam.ui.components.basic.TedamPasswordField;
import com.lbs.tedam.ui.components.basic.TedamTextField;
import com.lbs.tedam.ui.components.combobox.TedamComboBox;
import com.lbs.tedam.ui.components.grid.TedamGrid;
import com.lbs.tedam.ui.navigation.NavigationManager;
import com.lbs.tedam.ui.util.Enums.UIParameter;
import com.lbs.tedam.ui.util.Enums.ViewMode;
import com.lbs.tedam.ui.util.TedamNotification;
import com.lbs.tedam.ui.util.TedamNotification.NotifyType;
import com.lbs.tedam.ui.util.TedamStatic;
import com.lbs.tedam.util.HasLogger;
import com.vaadin.data.BeanValidationBinder;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewBeforeLeaveEvent;
import com.vaadin.navigator.ViewLeaveAction;
import com.vaadin.ui.Component;
import com.vaadin.ui.Component.Focusable;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HasComponents;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;
import com.wcs.wcslib.vaadin.widget.multifileupload.ui.MultiFileUpload;

public abstract class AbstractEditPresenter<T extends AbstractBaseEntity, S extends BaseService<T, Integer>, P extends AbstractEditPresenter<T, S, P, V>, V extends AbstractEditView<T, S, P, V>>
        implements Serializable, HasLogger, TedamLocalizerWrapper {

    /**
     * long serialVersionUID
     */
    private static final long serialVersionUID = 1L;
    private transient final S service;
    private final TedamUserService userService;
    private final PropertyService propertyService;
    private final NavigationManager navigationManager;
    private final BeanFactory beanFactory;
    private final Class<T> entityType;
    private transient V view;
    private BeanValidationBinder<T> binder;
    private ViewEventBus viewEventBus;
    private boolean hasChanges;

	@Autowired
	private GridPreferenceService gridPreferenceService;

    protected AbstractEditPresenter(ViewEventBus viewEventBus, NavigationManager navigationManager, S service, Class<T> entityType, BeanFactory beanFactory,
                                    TedamUserService userService, PropertyService propertyService) {
        this.service = service;
        this.navigationManager = navigationManager;
        this.entityType = entityType;
        this.beanFactory = beanFactory;
        this.viewEventBus = viewEventBus;
        this.userService = userService;
        this.propertyService = propertyService;
        createBinder();
    }

    public void subscribeToEventBus() {
        viewEventBus.subscribe(this);
    }

    public void destroy() {
        viewEventBus.unsubscribe(this);
    }

    public void beforeLeavingView(ViewBeforeLeaveEvent event) {
        destroy();
        runWithConfirmation(event::navigate, () -> {
        });
    }

    private void runWithConfirmation(Runnable onConfirmation, Runnable onCancel) {
        if (hasUnsavedChanges()) {
            ConfirmPopup confirmPopup = beanFactory.getBean(ConfirmPopup.class);
            confirmPopup.showLeaveViewConfirmDialog(view, onConfirmation, onCancel);
        } else {
            onConfirmation.run();
        }
    }

    protected void createBinder() {
        binder = new BeanValidationBinder<>(getEntityType());
        binder.setRequiredConfigurator(null);
        binder.addValueChangeListener(e -> hasChanges = true);
    }

    protected BeanValidationBinder<T> getBinder() {
        return binder;
    }

    public PropertyService getPropertyService() {
        return propertyService;
    }

    protected Class<T> getEntityType() {
        return entityType;
    }

    protected boolean hasUnsavedChanges() {
        return hasChanges;
    }

    public void setHasChanges(boolean hasChanges) {
        this.hasChanges = hasChanges;
    }

    protected S getService() {
        return service;
    }

    public ViewEventBus getViewEventBus() {
        return viewEventBus;
    }

    public V getView() {
        return view;
    }

    public void setView(V view) {
        this.view = view;
        view.bindFormFields(getBinder());
        view.getAccordion().setSelectedTab(0);
    }

    protected abstract void enterView(Map<UIParameter, Object> parameters) throws LocalizedException;

    protected abstract Class<? extends View> getGridView();

    protected void delete(T item) throws LocalizedException {
        getService().delete(item);
    }

    protected T save(T item) throws LocalizedException {
        if (!item.isNew()) {
            item.setDateUpdated(LocalDateTime.now());
            item.setUpdatedUser(SecurityUtils.getCurrentUser(userService).getTedamUser().getUserName());
        } else {
            item.setDateCreated(LocalDateTime.now());
            item.setCreatedUser(SecurityUtils.getCurrentUser(userService).getTedamUser().getUserName());
        }
        return getService().save(item);
    }

    protected void refreshView(T item, ViewMode mode) throws LocalizedException {
        getView().setViewMode(mode);
        setItem(item);
        setHasChanges(false);
    }

    public T getItem() {
        return binder.getBean();
    }

    protected void setItem(T item) throws LocalizedException {
        binder.setBean(item);
    }

    public void backPressed() {
        if (getView().getViewMode().equals(ViewMode.EDIT)) {
            beforeLeavingView(new ViewBeforeLeaveEvent(getNavigationManager(), new ViewLeaveAction() {

                /** long serialVersionUID */
                private static final long serialVersionUID = 1L;

                @Override
                public void run() {
                    try {
                        enterView(TedamStatic.getUIParameterMap(getBinder().getBean().getId(), ViewMode.VIEW));
                        setHasChanges(false);
                    } catch (LocalizedException e) {
                        getLogger().error(e.getLocalizedMessage(), e);
                    }
                }
            }));
        } else {
            getNavigationManager().navigateTo(getGridView());
        }
    }

    public void okPressed() throws LocalizedException {
        try {
            T item = getBinder().getBean();

            if (focusFirstErrorField()) {
                return;
            }
            if (getView().getViewMode() == ViewMode.NEW) {
                item = save(item);
                if (item != null) {
                    // Navigate to edit view so URL is updated correctly
                    getNavigationManager().updateViewParameter("" + item.getId());
                    enterView(TedamStatic.getUIParameterMap(item.getId(), ViewMode.VIEW));
					getView().showSuccessfulSave();
                }
            } else if (getView().getViewMode() == ViewMode.EDIT) {
                item.setDateUpdated(LocalDateTime.now());
                item.setUpdatedUser(SecurityUtils.getCurrentUser(getUserService()).getTedamUser().getUserName());
                item = save(item);
                if (item != null) {
                    enterView(TedamStatic.getUIParameterMap(item.getId(), ViewMode.VIEW));
					getView().showSuccessfulUpdate();
                }
            } else if (getView().getViewMode().equals(ViewMode.VIEW)) {
                enterView(TedamStatic.getUIParameterMap(item.getId(), ViewMode.EDIT));
            }
        } catch (DataIntegrityViolationException e) {
            getView().showDataIntegrityException();
        }
    }

    public boolean focusFirstErrorField() {
        Optional<Object> firstErrorField = getView().validate().findFirst();
        if (firstErrorField.isPresent()) {
            String emptyField = ((Component) firstErrorField.get()).getCaption();
            TedamNotification.showNotification(emptyField + getLocaleValue("view.jobedit.messages.emptyField"), NotifyType.ERROR);
            ((Focusable) firstErrorField.get()).focus();
            return true;
        }
        return false;
    }

    public NavigationManager getNavigationManager() {
        return navigationManager;
    }

    public TedamUserService getUserService() {
        return userService;
    }

    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public void organizeComponents(HasComponents components, boolean isReadOnly) {
        for (Component component : components) {
            if (component instanceof TabSheet) {
                TabSheet tabSheet = (TabSheet) component;
                organizeComponents(tabSheet, isReadOnly);
            }
            if (component instanceof HorizontalLayout) {
                HorizontalLayout hLayout = (HorizontalLayout) component;
                organizeComponents(hLayout, isReadOnly);
            }
            if (component instanceof VerticalLayout) {
                VerticalLayout vLayout = (VerticalLayout) component;
                organizeComponents(vLayout, isReadOnly);
            }
            if (component instanceof CssLayout) {
                CssLayout cssLayout = (CssLayout) component;
                organizeComponents(cssLayout, isReadOnly);
            }
            if (component instanceof TedamGrid<?>) {
                TedamGrid<?> tedamGrid = (TedamGrid<?>) component;
                if (isReadOnly) {
                    tedamGrid.setSelectionMode(SelectionMode.NONE);
                } else {
                    tedamGrid.setSelectionMode(tedamGrid.getSelectionMode());
                }
                if (tedamGrid.getRUDMenuColumn() != null) {
                    tedamGrid.getRUDMenuColumn().setHidden(isReadOnly);
                    displayTestRunsOperationsColumn(tedamGrid);
                }

            }
            if (component instanceof TedamButton) {
                TedamButton tedamButton = (TedamButton) component;
                if (!tedamButton.getId().equals("general.button.cancel") && !tedamButton.getId().equals("general.button.save")) {
                    tedamButton.setEnabled(!isReadOnly);
                }
            }
            if (component instanceof TedamMenuBar) {
                TedamMenuBar menuBar = (TedamMenuBar) component;
                menuBar.setEnabled(!isReadOnly);
            }
            if (component instanceof TedamComboBox) {
                TedamComboBox<?> comboBox = (TedamComboBox<?>) component;
                comboBox.setReadOnly(isReadOnly);
            }
            if (component instanceof TedamTextField) {
                TedamTextField textField = (TedamTextField) component;
                textField.setReadOnly(isReadOnly);
            }
            if (component instanceof TedamPasswordField) {
                TedamPasswordField passwordField = (TedamPasswordField) component;
                passwordField.setReadOnly(isReadOnly);
            }
            if (component instanceof TedamDateTimeField) {
                TedamDateTimeField tedamDateTimeField = (TedamDateTimeField) component;
                tedamDateTimeField.setReadOnly(isReadOnly);
            }
            if (component instanceof TedamDateField) {
                TedamDateField tedamDateField = (TedamDateField) component;
                tedamDateField.setReadOnly(isReadOnly);
            }
            if (component instanceof Upload) {
                ((Upload) component).setEnabled(!isReadOnly);
            }
            if (component instanceof MultiFileUpload) {
                ((MultiFileUpload) component).setEnabled(isReadOnly);
            }
        }
    }

    public void setGridEditorAttributes(Component component, boolean isEnabled) {
        TedamGrid<?> tedamGrid = (TedamGrid<?>) component;
        if (tedamGrid.getEditor().isOpen()) {
            tedamGrid.getEditor().cancel();
        }
        tedamGrid.getButtonDown().setEnabled(isEnabled);
        tedamGrid.getButtonUp().setEnabled(isEnabled);
        tedamGrid.getButtonCopyRows().setEnabled(isEnabled);
        tedamGrid.getEditor().setEnabled(isEnabled);
    }

    protected void displayTestRunsOperationsColumn(TedamGrid<?> tedamGrid) {
    }

    protected void isAuthorized(T entity) throws LocalizedException {
        boolean isAuthorized = checkUserProjectAuthority(entity);
        if (!isAuthorized) {
            getView().showNotAuthorized();
            return;
        }
    }

    private boolean checkUserProjectAuthority(T entity) throws LocalizedException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        TedamUser user = userService.findByUserName(auth.getName());
        List<Project> projectList = user.getProjects();
        Project currentProject = null;
        boolean authorized = false;
        currentProject = getProjectByEntity(entity);
        for (Project project : projectList) {
            if (project.getId().equals(currentProject.getId())) {
                authorized = true;
            }
        }
        return authorized;
    }

    protected Project getProjectByEntity(T entity) {
        return null;
    }

	protected void getTitleForHeader() {
	}

	private List<GridPreference> findGridPreference(List<TedamGrid<?>> gridList) throws LocalizedException {
		Integer userId = SecurityUtils.getUser().getId();
		Integer projectId = SecurityUtils.getUserSessionProject().getId();
		String viewId = this.getClass().getName();
		List<GridPreference> gridPreferenceList = new ArrayList<>();
		for (TedamGrid<?> grid : gridList) {
			GridPreference gridPreference = gridPreferenceService.findByUserIdAndProjectIdAndViewIdAndGridId(userId,
					projectId, viewId, grid.getId());
			if (gridPreference != null) {
				gridPreferenceList.add(gridPreference);
			}
		}
		return gridPreferenceList;
	}

	protected void saveGridPreference(List<TedamGrid<?>> gridList) {
		try {
			List<GridPreference> gridPreferenceList = findGridPreference(gridList);
			if (gridPreferenceList.isEmpty()) {
				for (TedamGrid<?> grid : gridList) {
					GridPreference gridPreference = grid.saveGridPreferenceByGrid(grid);
					gridPreference.setUserId(SecurityUtils.getUser().getId());
					gridPreference.setProjectId(SecurityUtils.getUserSessionProject().getId());
					gridPreference.setViewId(this.getClass().getName());
					gridPreferenceList.add(gridPreference);
				}
			} else {
				for (GridPreference gridPreference : gridPreferenceList) {
					for (TedamGrid<?> grid : gridList) {
						if (grid.getId().equals(gridPreference.getGridId())) {
							gridPreference = grid.saveGridPreference(gridPreference);
							break;
						}
					}
				}
			}
			gridPreferenceService.save(gridPreferenceList);
		} catch (LocalizedException e) {
		}
	}

	protected void laodGridPreference(List<TedamGrid<?>> gridList) {
		try {
			for (GridPreference gridPreference : findGridPreference(gridList)) {
				for (TedamGrid<?> grid : gridList) {
					if (grid.getId().equals(gridPreference.getGridId())) {
						grid.loadGridPreference(gridPreference);
						break;
					}
				}
			}
		} catch (LocalizedException e) {
		}
	}

}
