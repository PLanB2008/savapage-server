/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
 * Author: Rijk Ravestein.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.pages;

import java.util.Iterator;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.IppQueueDao;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dao.helpers.DocLogPagerReq;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.QueueService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.DocLogScopeEnum;
import org.savapage.server.session.SpSession;
import org.savapage.server.webapp.WebAppTypeEnum;

/**
 *
 * @author Rijk Ravestein
 */
public class DocLogBase extends AbstractAuthPage {

    private static final long serialVersionUID = 1L;

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /** */
    private static final QueueService QUEUE_SERVICE =
            ServiceContext.getServiceFactory().getQueueService();

    /** */
    private static final ConfigManager CONFIG_MNGR = ConfigManager.instance();

    /**
     *
     */
    public DocLogBase(final PageParameters parameters) {

        super(parameters);

        if (isAuthErrorHandled()) {
            return;
        }

        final WebAppTypeEnum webAppType = this.getSessionWebAppType();

        /**
         * If this page is displayed in the Admin WebApp context, we check the
         * admin authentication (including the need for a valid Membership).
         */
        if (webAppType == WebAppTypeEnum.ADMIN) {
            checkAdminAuthorization();
        }

        if (webAppType == WebAppTypeEnum.JOBTICKETS) {

            final User user = SpSession.get().getUser();

            if (user == null || !ACCESS_CONTROL_SERVICE.hasAccess(user,
                    ACLRoleEnum.JOB_TICKET_OPERATOR)) {

                this.setResponsePage(NotAuthorized.class);
                setAuthErrorHandled(true);
            }

        }

        handlePage(webAppType);
    }

    /**
     *
     * @param webAppType
     *            The Web App Type this page is part of.
     */
    private void handlePage(final WebAppTypeEnum webAppType) {

        final String data = getParmValue(POST_PARM_DATA);

        final DocLogPagerReq req = DocLogPagerReq.read(data);

        final Long userId;
        Long accountId = null;

        final boolean userNameVisible;
        final boolean accountNameVisible;

        if (webAppType == WebAppTypeEnum.ADMIN) {

            userId = req.getSelect().getUserId();
            userNameVisible = (userId != null);

            accountId = req.getSelect().getAccountId();
            accountNameVisible = (accountId != null);

        } else if (webAppType == WebAppTypeEnum.JOBTICKETS) {

            userId = null;
            userNameVisible = false;
            accountNameVisible = false;

        } else {
            /*
             * If we are called in a User WebApp context we ALWAYS use the user
             * of the current session.
             */
            userId = SpSession.get().getUser().getId();
            userNameVisible = false;
            accountNameVisible = false;
        }

        //
        String userName = null;
        String accountName = null;

        if (userNameVisible) {
            final User user = ServiceContext.getDaoContext().getUserDao()
                    .findById(userId);
            userName = user.getUserId();
        } else if (accountNameVisible) {
            final Account account = ServiceContext.getDaoContext()
                    .getAccountDao().findById(accountId);
            accountName = account.getName();
        }

        //
        Label hiddenLabel = new Label("hidden-user-id");
        String hiddenValue = "";

        if (userId != null) {
            hiddenValue = userId.toString();
        }
        hiddenLabel.add(new AttributeModifier("value", hiddenValue));
        add(hiddenLabel);

        //
        hiddenLabel = new Label("hidden-account-id");
        hiddenValue = "";

        if (accountId != null) {
            hiddenValue = accountId.toString();
        }
        hiddenLabel.add(new AttributeModifier("value", hiddenValue));
        add(hiddenLabel);

        //
        final MarkupHelper helper = new MarkupHelper(this);

        //
        final boolean btnVisiblePdf;
        final boolean btnVisiblePrint;
        final boolean btnVisibleTicket;
        final boolean visibleLetterhead;

        DocLogScopeEnum defaultScope = DocLogScopeEnum.ALL;

        final List<Printer> printerList = getPrinterList(webAppType);

        if (webAppType == WebAppTypeEnum.ADMIN) {

            btnVisiblePdf = true;
            btnVisiblePrint = true;
            btnVisibleTicket = true;
            visibleLetterhead = true;

        } else if (webAppType == WebAppTypeEnum.JOBTICKETS) {

            defaultScope = DocLogScopeEnum.TICKET;

            btnVisiblePdf = false;
            btnVisiblePrint = false;
            btnVisibleTicket = true;
            visibleLetterhead = false;

        } else {

            final User user = SpSession.get().getUser();

            final List<ACLPermissionEnum> permissions = ACCESS_CONTROL_SERVICE
                    .getUserPermission(user, ACLOidEnum.U_INBOX);

            visibleLetterhead = user != null
                    && (permissions == null || ACCESS_CONTROL_SERVICE
                            .hasUserAccess(user, ACLOidEnum.U_LETTERHEAD));

            btnVisiblePdf = user != null && (permissions == null
                    || ACCESS_CONTROL_SERVICE.hasUserPermission(permissions,
                            ACLPermissionEnum.DOWNLOAD)
                    || ACCESS_CONTROL_SERVICE.hasUserPermission(permissions,
                            ACLPermissionEnum.SEND));

            btnVisiblePrint = !printerList.isEmpty() && user != null
                    && ACCESS_CONTROL_SERVICE.hasAccess(user,
                            ACLRoleEnum.PRINT_CREATOR);

            btnVisibleTicket = user != null && ACCESS_CONTROL_SERVICE
                    .hasAccess(user, ACLRoleEnum.JOB_TICKET_CREATOR);

            final List<DocLogScopeEnum> typeDefaultOrder =
                    CONFIG_MNGR.getConfigEnumList(DocLogScopeEnum.class,
                            Key.WEBAPP_USER_DOCLOG_SELECT_TYPE_DEFAULT_ORDER);

            DocLogScopeEnum scopeSecondChoice = null;
            for (final DocLogScopeEnum scope : typeDefaultOrder) {
                if ((scope == DocLogScopeEnum.PDF && btnVisiblePdf)
                        || (scope == DocLogScopeEnum.PRINT && btnVisiblePrint)
                        || (scope == DocLogScopeEnum.TICKET
                                && btnVisibleTicket)) {
                    defaultScope = scope;
                    break;
                }
                if (scopeSecondChoice == null && (scope == DocLogScopeEnum.IN
                        || scope == DocLogScopeEnum.OUT)) {
                    scopeSecondChoice = scope;
                }
            }

            if (defaultScope == DocLogScopeEnum.ALL
                    && scopeSecondChoice != null) {
                defaultScope = scopeSecondChoice;
            }
        }

        Label selectTypeToCheck = helper.addModifyLabelAttr("select-type-all",
                MarkupHelper.ATTR_VALUE, DocLogDao.Type.ALL.toString());

        Label labelWlk;

        labelWlk = helper.addModifyLabelAttr("select-type-in",
                MarkupHelper.ATTR_VALUE, DocLogDao.Type.IN.toString());
        if (defaultScope == DocLogScopeEnum.IN) {
            selectTypeToCheck = labelWlk;
        }

        labelWlk = helper.addModifyLabelAttr("select-type-out",
                MarkupHelper.ATTR_VALUE, DocLogDao.Type.OUT.toString());
        if (defaultScope == DocLogScopeEnum.OUT) {
            selectTypeToCheck = labelWlk;
        }

        if (btnVisiblePdf) {
            labelWlk = helper.addModifyLabelAttr("select-type-pdf",
                    MarkupHelper.ATTR_VALUE, DocLogDao.Type.PDF.toString());
            if (defaultScope == DocLogScopeEnum.PDF) {
                selectTypeToCheck = labelWlk;
            }
        } else {
            helper.discloseLabel("select-type-pdf");
        }

        if (btnVisiblePrint) {
            labelWlk = helper.addModifyLabelAttr("select-type-print",
                    MarkupHelper.ATTR_VALUE, DocLogDao.Type.PRINT.toString());
            if (defaultScope == DocLogScopeEnum.PRINT) {
                selectTypeToCheck = labelWlk;
            }
        } else {
            helper.discloseLabel("select-type-print");
        }

        if (btnVisibleTicket) {
            labelWlk = helper.addModifyLabelAttr("select-type-ticket",
                    MarkupHelper.ATTR_VALUE, DocLogDao.Type.TICKET.toString());
            if (defaultScope == DocLogScopeEnum.TICKET) {
                selectTypeToCheck = labelWlk;
            }
        } else {
            helper.discloseLabel("select-type-ticket");
        }

        MarkupHelper.modifyLabelAttr(selectTypeToCheck,
                MarkupHelper.ATTR_CHECKED, MarkupHelper.ATTR_CHECKED);

        //
        helper.encloseLabel("prompt-letterhead", localized("prompt-letterhead"),
                visibleLetterhead);

        //
        helper.encloseLabel("select-and-sort-user", userName, userNameVisible);
        helper.encloseLabel("select-and-sort-account", accountName,
                accountNameVisible);

        /*
         * Option list: Queues
         */
        add(new PropertyListView<IppQueue>("option-list-queues",
                getQueueList(webAppType)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<IppQueue> item) {
                final IppQueue queue = item.getModel().getObject();
                final Label label = new Label("option-queue",
                        String.format("/%s", queue.getUrlPath()));
                label.add(new AttributeModifier("value", queue.getId()));
                item.add(label);
            }

        });

        /*
         * Option list: Printers
         */
        add(new PropertyListView<Printer>("option-list-printers", printerList) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<Printer> item) {
                final Printer printer = item.getModel().getObject();
                final Label label =
                        new Label("option-printer", printer.getDisplayName());
                label.add(new AttributeModifier("value", printer.getId()));
                item.add(label);
            }

        });

        /*
         *
         */
        if (req.getSelect() != null) {

            final Long printerId = req.getSelect().getPrinterId();
            if (printerId != null) {

            }

            final Long queueId = req.getSelect().getQueueId();
            if (queueId != null) {

            }
        }

    }

    /**
     * Gets the Queue list.
     *
     * @param webAppType
     *            The Web App Type.
     * @return The list.
     */
    private static List<IppQueue>
            getQueueList(final WebAppTypeEnum webAppType) {

        final IppQueueDao dao = ServiceContext.getDaoContext().getIppQueueDao();

        final IppQueueDao.ListFilter filter = new IppQueueDao.ListFilter();

        if (webAppType == WebAppTypeEnum.USER) {
            filter.setDeleted(Boolean.FALSE);
            filter.setDisabled(Boolean.FALSE);
        }

        final List<IppQueue> list = dao.getListChunk(filter, null, null,
                IppQueueDao.Field.URL_PATH, true);

        if (webAppType == WebAppTypeEnum.USER) {
            final Iterator<IppQueue> iter = list.iterator();
            while (iter.hasNext()) {
                if (!QUEUE_SERVICE.isActiveQueue(iter.next())) {
                    iter.remove();
                }
            }
        }
        return list;
    }

    /**
     * Gets the Printer list.
     *
     * @param webAppType
     *            The Web App Type.
     * @return The list.
     */
    private static List<Printer>
            getPrinterList(final WebAppTypeEnum webAppType) {

        final PrinterDao dao = ServiceContext.getDaoContext().getPrinterDao();

        final PrinterDao.ListFilter filter = new PrinterDao.ListFilter();

        filter.setJobTicket(Boolean.FALSE);

        if (webAppType == WebAppTypeEnum.USER) {
            filter.setDeleted(Boolean.FALSE);
            filter.setDisabled(Boolean.FALSE);
            filter.setInternal(Boolean.FALSE);
        }

        return dao.getListChunk(filter, null, null,
                PrinterDao.Field.DISPLAY_NAME, true);
    }

    @Override
    protected boolean needMembership() {
        // return isAdminRoleContext();
        return false;
    }
}
