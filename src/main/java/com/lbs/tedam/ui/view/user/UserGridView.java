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

package com.lbs.tedam.ui.view.user;

import com.lbs.tedam.data.service.TedamUserService;
import com.lbs.tedam.model.TedamUser;
import com.lbs.tedam.ui.components.grid.GridColumns;
import com.lbs.tedam.ui.components.grid.GridColumns.GridColumn;
import com.lbs.tedam.ui.components.grid.RUDOperations;
import com.lbs.tedam.ui.components.grid.TedamGridConfig;
import com.lbs.tedam.ui.view.AbstractGridView;
import com.lbs.tedam.ui.view.user.edit.UserEditView;
import com.lbs.tedam.util.EnumsV2.TedamUserRole;
import com.vaadin.navigator.View;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Grid.SelectionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Secured(TedamUserRole.Constants.ADMIN)
@SpringView
public class UserGridView extends AbstractGridView<TedamUser, TedamUserService, UserGridPresenter, UserGridView> {

    private static final long serialVersionUID = 1L;

    private TedamGridConfig<TedamUser> config = new TedamGridConfig<TedamUser>() {

        @Override
        public List<GridColumn> getColumnList() {
            return GridColumns.GridColumn.USER_COLUMNS;
        }

        @Override
        public Class<TedamUser> getBeanType() {
            return TedamUser.class;
        }

        @Override
        public List<RUDOperations> getRUDOperations() {
            List<RUDOperations> operations = new ArrayList<RUDOperations>();
            operations.add(RUDOperations.DELETE);
            operations.add(RUDOperations.VIEW);
            return operations;
        }

    };

    @Autowired
    public UserGridView(UserGridPresenter presenter) {
        super(presenter, SelectionMode.MULTI);
    }

    @PostConstruct
    private void init() {
        getPresenter().setView(this);
        setHeader(getLocaleValue("usersGridLabel"));
    }

    @Override
    protected TedamGridConfig<TedamUser> getTedamGridConfig() {
        return config;
    }

    @Override
    protected Class<? extends View> getEditView() {
        return UserEditView.class;
    }

    @Override
    public void buildGridColumnDescription() {
        // TODO Auto-generated method stub

    }

}
