/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
 * Authors: Rijk Ravestein.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.api.request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.savapage.core.dao.UserGroupDao;
import org.savapage.core.dao.UserGroupMemberDao;
import org.savapage.core.dao.enums.ReservedUserGroupEnum;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.QuickSearchFilterDto;
import org.savapage.core.dto.QuickSearchItemDto;
import org.savapage.core.dto.QuickSearchUserGroupItemDto;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.services.ServiceContext;

/**
 * Proxy Printers Quicksearch.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUserGroupQuickSearch extends ApiRequestMixin {

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static class DtoRsp extends AbstractDto {

        private List<QuickSearchItemDto> items;

        public List<QuickSearchItemDto> getItems() {
            return items;
        }

        public void setItems(List<QuickSearchItemDto> items) {
            this.items = items;
        }

    }

    @Override
    protected void
            onRequest(final String requestingUser, final User lockedUser)
                    throws IOException {

        final UserGroupMemberDao groupMemberDao =
                ServiceContext.getDaoContext().getUserGroupMemberDao();

        final QuickSearchFilterDto dto =
                AbstractDto.create(QuickSearchFilterDto.class,
                        this.getParmValue("dto"));

        final List<QuickSearchItemDto> items = new ArrayList<>();

        //
        final UserGroupDao.ListFilter filter = new UserGroupDao.ListFilter();

        filter.setContainingText(dto.getFilter());

        /*
         * Since we locally filter out reserved groups we need to retrieve more
         * results.
         */
        final int maxResult =
                dto.getMaxResults().intValue()
                        + ReservedUserGroupEnum.values().length;

        final UserGroupDao userGroupDao =
                ServiceContext.getDaoContext().getUserGroupDao();

        final List<UserGroup> userGroupList =
                userGroupDao.getListChunk(filter, null,
                        Integer.valueOf(maxResult), UserGroupDao.Field.NAME,
                        true);

        final UserGroupMemberDao.GroupFilter groupFilter =
                new UserGroupMemberDao.GroupFilter();

        int i = 0;

        for (final UserGroup group : userGroupList) {

            final ReservedUserGroupEnum reservedGroup =
                    ReservedUserGroupEnum.fromDbName(group.getGroupName());

            if (reservedGroup != null) {
                continue;
            }

            final QuickSearchUserGroupItemDto itemWlk =
                    new QuickSearchUserGroupItemDto();

            itemWlk.setKey(group.getId());
            itemWlk.setText(group.getGroupName());

            groupFilter.setGroupId(group.getId());
            itemWlk.setUserCount(groupMemberDao.getUserCount(groupFilter));

            items.add(itemWlk);

            if (++i < dto.getMaxResults()) {
                continue;
            }
            break;
        }

        //
        final DtoRsp rsp = new DtoRsp();
        rsp.setItems(items);

        setResponse(rsp);
        setApiResultOk();
    }

}
