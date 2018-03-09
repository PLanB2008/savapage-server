/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
package org.savapage.server.pages.user;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.ipp.IppSyntaxException;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.jpa.Device;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.api.request.ApiRequestHelper;
import org.savapage.server.helpers.CssClassEnum;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.CommunityStatusFooterPanel;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.session.SpSession;
import org.savapage.server.webprint.WebPrintHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class Main extends AbstractUserPage {

    private static final long serialVersionUID = 1L;

    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static final String CSS_CLASS_MAIN_ACTIONS = "main_actions";

    private static final String CSS_CLASS_MAIN_ACTIONS_BASE =
            "main_action_base";

    private static final String CSS_CLASS_MAIN_ACTION_OUTBOX =
            CSS_CLASS_MAIN_ACTIONS_BASE + " sp-btn-show-outbox";

    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static enum NavButtonEnum {
        ABOUT, BROWSE, UPLOAD, PDF, LETTERHEAD, SORT, PRINT, TICKET,
        TICKET_QUEUE, HELP
    }

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static class NavBarItem {

        private final String itemCssClass;
        private final String imgCssClass;
        private final String buttonHtmlId;
        private final String buttonText;

        public NavBarItem(final String itemCssClass, final String imgCssClass,
                final String buttonHtmlId, final String buttonText) {
            this.imgCssClass = imgCssClass;
            this.itemCssClass = itemCssClass;
            this.buttonHtmlId = buttonHtmlId;
            this.buttonText = buttonText;
        }

    }

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private class NavBarRow extends PropertyListView<NavBarItem> {

        /**
         *
         * @param id
         *            The Wicket id.
         * @param list
         *            The list.
         */
        NavBarRow(final String id, final List<NavBarItem> list) {
            super(id, list);
        }

        /**
        *
        */
        private static final long serialVersionUID = 1L;

        @Override
        protected void populateItem(final ListItem<NavBarItem> listItem) {

            final NavBarItem navBarItem = listItem.getModelObject();

            final WebMarkupContainer contItem = new WebMarkupContainer("item");

            contItem.add(new AttributeModifier(MarkupHelper.ATTR_CLASS,
                    navBarItem.itemCssClass));

            //
            final WebMarkupContainer contButton =
                    new WebMarkupContainer("button");

            contButton
                    .add(new AttributeAppender(MarkupHelper.ATTR_CLASS,
                            String.format(" %s", navBarItem.imgCssClass)))
                    .add(new AttributeModifier(MarkupHelper.ATTR_ID,
                            navBarItem.buttonHtmlId));

            contButton.add(new Label("button-text", navBarItem.buttonText));

            //
            contItem.add(contButton);

            listItem.add(contItem);
        }
    }

    /**
     *
     * @param parameters
     *            The page parameters.
     */
    public Main(final PageParameters parameters) {

        super(parameters);

        final ConfigManager cm = ConfigManager.instance();
        final String urlHelp = cm.getConfigValue(Key.WEBAPP_USER_HELP_URL);
        final boolean hasHelpURL = StringUtils.isNotBlank(urlHelp)
                && cm.isConfigValue(Key.WEBAPP_USER_HELP_URL_ENABLE);

        final org.savapage.core.jpa.User user = SpSession.get().getUser();

        final Set<NavButtonEnum> buttonPrivileged = getNavButtonPriv(user);

        final Set<NavButtonEnum> buttonSubstCandidates = new HashSet<>();

        buttonSubstCandidates.add(NavButtonEnum.BROWSE);
        buttonSubstCandidates.add(NavButtonEnum.ABOUT);

        if (hasHelpURL) {
            buttonSubstCandidates.add(NavButtonEnum.HELP);
        }

        final boolean isUpload =
                WebPrintHelper.isWebPrintEnabled(getClientIpAddr());

        if (isUpload) {
            buttonSubstCandidates.add(NavButtonEnum.UPLOAD);
        }

        if (buttonPrivileged.contains(NavButtonEnum.TICKET)) {
            buttonSubstCandidates.add(NavButtonEnum.TICKET_QUEUE);
        }

        this.populateNavBar(buttonPrivileged, buttonSubstCandidates);

        // final boolean isPrintDelegate = user != null &&
        // ACCESS_CONTROL_SERVICE
        // .hasAccess(user, ACLRoleEnum.PRINT_DELEGATE);
        addVisible(false, "button-print-delegation", "-");

        add(new CommunityStatusFooterPanel("community-status-footer-panel",
                false));

        final MarkupHelper helper = new MarkupHelper(this);

        addVisible(cm.isConfigValue(Key.WEBAPP_USER_GDPR_ENABLE),
                "btn-txt-gdpr", "GDPR");

        // Mini-buttons in footer/header bar
        addVisible(
                isUpload && buttonSubstCandidates
                        .contains(NavButtonEnum.UPLOAD),
                "button-upload", localized(HtmlButtonEnum.UPLOAD));

        addVisible(buttonSubstCandidates.contains(NavButtonEnum.ABOUT),
                "button-mini-about", localized(HtmlButtonEnum.ABOUT));

        helper.addAppendLabelAttr("btn-about-popup-menu",
                HtmlButtonEnum.ABOUT.uiText(getLocale()),
                MarkupHelper.ATTR_CLASS, CssClassEnum.SP_BTN_ABOUT.clazz());

        // Action pop-up in sort view.
        this.add(MarkupHelper.createEncloseLabel("main-arr-action-pdf",
                localized("button-pdf"),
                buttonPrivileged.contains(NavButtonEnum.PDF)));

        this.add(MarkupHelper.createEncloseLabel("main-arr-action-print",
                localized(HtmlButtonEnum.PRINT),
                buttonPrivileged.contains(NavButtonEnum.PRINT)));

        this.add(MarkupHelper.createEncloseLabel("main-arr-action-ticket",
                localized("button-ticket"),
                !buttonPrivileged.contains(NavButtonEnum.PRINT)
                        && buttonPrivileged.contains(NavButtonEnum.TICKET)));

        // Fixed buttons in sort view.
        helper.addButton("button-back", HtmlButtonEnum.BACK);
        helper.addButton("button-delete", HtmlButtonEnum.DELETE);

        //
        final String userId;
        final String userName;

        if (user == null) {
            userId = "";
            userName = "";
        } else {
            userName = StringUtils.defaultString(user.getFullName());
            userId = user.getUserId();
        }

        final boolean showUserBalance =
                ACCESS_CONTROL_SERVICE.hasPermission(user,
                        ACLOidEnum.U_FINANCIAL, ACLPermissionEnum.READER);

        helper.encloseLabel("mini-user-balance", "", showUserBalance);

        //
        final Label name = MarkupHelper.createEncloseLabel("mini-user-name",
                userName, true);

        final boolean showUserDetails =
                ACCESS_CONTROL_SERVICE.hasAccess(user, ACLOidEnum.U_USER);

        if (showUserDetails) {
            MarkupHelper.appendLabelAttr(name, MarkupHelper.ATTR_CLASS,
                    "sp-button-user-details");
        }
        helper.encloseLabel("sparkline-user-pie", "", showUserDetails);

        add(name.add(new AttributeModifier("title", userId)));

        //
        helper.encloseLabel("mini-sys-maintenance",
                NounEnum.MAINTENANCE.uiText(getLocale()),
                ConfigManager.isSysMaintenance());

        if (hasHelpURL) {

            final Label btn = helper.encloseLabel("button-mini-help",
                    HtmlButtonEnum.HELP.uiText(getLocale()), true);

            MarkupHelper.modifyLabelAttr(btn, MarkupHelper.ATTR_HREF, urlHelp);

            if (!buttonSubstCandidates.contains(NavButtonEnum.HELP)) {
                // Hide: href is used by the substitute main button.
                MarkupHelper.modifyLabelAttr(btn, MarkupHelper.ATTR_STYLE,
                        "display:none;");
            }

        } else {
            helper.discloseLabel("button-mini-help");
        }
    }

    /**
     * Gets the buttons that must be present on the main navigation bar.
     *
     * @param user
     *            The user.
     * @return The privileged navigation buttons.
     */
    private Set<NavButtonEnum>
            getNavButtonPriv(final org.savapage.core.jpa.User user) {

        final Set<NavButtonEnum> set = new HashSet<>();

        NavButtonEnum navButtonWlk;

        //
        navButtonWlk = NavButtonEnum.PDF;

        final Integer inboxPriv = ACCESS_CONTROL_SERVICE.getPrivileges(user,
                ACLOidEnum.U_INBOX);

        if (inboxPriv == null
                || ACLPermissionEnum.DOWNLOAD.isPresent(inboxPriv.intValue())
                || ACLPermissionEnum.SEND.isPresent(inboxPriv.intValue())) {
            set.add(navButtonWlk);
        }

        //

        if (ACCESS_CONTROL_SERVICE.isAuthorized(user,
                ACLRoleEnum.PRINT_CREATOR)) {

            final Device terminal =
                    ApiRequestHelper.getHostTerminal(this.getClientIpAddr());

            try {

                final NavButtonEnum navButtonPrint;

                if (ServiceContext.getServiceFactory().getProxyPrintService()
                        .areJobTicketPrintersOnly(terminal, user.getUserId())) {
                    navButtonPrint = NavButtonEnum.TICKET;
                } else {
                    navButtonPrint = NavButtonEnum.PRINT;
                }

                set.add(navButtonPrint);

            } catch (IppConnectException | IppSyntaxException e) {
                LOGGER.error(e.getMessage());
            }

        } else if (ACCESS_CONTROL_SERVICE.isAuthorized(user,
                ACLRoleEnum.JOB_TICKET_CREATOR)) {
            set.add(NavButtonEnum.TICKET);
        }

        //
        navButtonWlk = NavButtonEnum.SORT;

        if (inboxPriv == null
                || ACLPermissionEnum.EDITOR.isPresent(inboxPriv.intValue())) {
            set.add(navButtonWlk);
        }

        //
        navButtonWlk = NavButtonEnum.LETTERHEAD;
        if (ACCESS_CONTROL_SERVICE.hasAccess(user,
                ACLOidEnum.U_LETTERHEAD)) {
            set.add(navButtonWlk);
        }

        //
        return set;
    }

    /**
     * Uses a {@link NavBarItem} by removing it from the set of candidates.
     *
     * @param buttonCandidates
     *            The set of candidates.
     * @return The {@link NavBarItem} used.
     */
    private NavBarItem
            useButtonCandidate(final Set<NavButtonEnum> buttonCandidates) {

        NavButtonEnum candidate = null;
        NavBarItem item = null;

        /*
         * Order is important!
         */

        // #1
        if (item == null) {
            candidate = NavButtonEnum.UPLOAD;
            if (buttonCandidates.contains(candidate)) {
                item = new NavBarItem(CSS_CLASS_MAIN_ACTIONS_BASE,
                        "ui-icon-main-upload", "button-mini-upload",
                        localized(HtmlButtonEnum.UPLOAD));
            }
        }

        // #2
        if (item == null) {
            candidate = NavButtonEnum.TICKET_QUEUE;
            if (buttonCandidates.contains(candidate)) {
                item = new NavBarItem(CSS_CLASS_MAIN_ACTION_OUTBOX,
                        "ui-icon-main-hold-jobs", "button-main-outbox",
                        NounEnum.QUEUE.uiText(getLocale()));
            }
        }

        // #3
        if (item == null) {
            candidate = NavButtonEnum.HELP;
            if (buttonCandidates.contains(candidate)) {
                item = new NavBarItem(CSS_CLASS_MAIN_ACTIONS_BASE,
                        "ui-icon-main-help", "button-main-help",
                        localized(HtmlButtonEnum.HELP));
            }
        }

        // #4
        if (item == null) {
            candidate = NavButtonEnum.ABOUT;
            if (buttonCandidates.contains(candidate)) {
                item = new NavBarItem(CSS_CLASS_MAIN_ACTIONS_BASE,
                        String.format("%s %s", "ui-icon-main-about",
                                CssClassEnum.SP_BTN_ABOUT.clazz()),
                        "button-about", localized(HtmlButtonEnum.ABOUT));
            }
        }

        // #5
        if (item == null) {
            candidate = NavButtonEnum.BROWSE;
            if (buttonCandidates.contains(candidate)) {
                item = new NavBarItem(CSS_CLASS_MAIN_ACTIONS,
                        "ui-icon-main-browse", "button-browser",
                        localized(HtmlButtonEnum.BROWSE));
            }
        }

        if (item != null) {
            buttonCandidates.remove(candidate);
        }

        return item;
    }

    /**
     * Populates the central navigation bar.
     *
     * @param buttonPrivileged
     * @param buttonCandidates
     *            The substitute buttons.
     */
    private void populateNavBar(final Set<NavButtonEnum> buttonPrivileged,
            final Set<NavButtonEnum> buttonCandidates) {

        List<NavBarItem> items = new ArrayList<>();

        NavBarItem itemWlk;

        // ----------------------------------------------------------
        // Row 1
        // ----------------------------------------------------------

        // ------------
        // PDF
        // ------------
        if (buttonPrivileged.contains(NavButtonEnum.PDF)) {
            itemWlk = new NavBarItem(CSS_CLASS_MAIN_ACTIONS,
                    "ui-icon-main-pdf-properties", "button-main-pdf-properties",
                    localized("button-pdf"));
        } else {
            itemWlk = useButtonCandidate(buttonCandidates);
        }

        if (itemWlk != null) {
            items.add(itemWlk);
        }

        // --------------------------
        // Print or Ticket (or none)
        // --------------------------
        if (buttonPrivileged.contains(NavButtonEnum.PRINT)) {

            items.add(new NavBarItem(CSS_CLASS_MAIN_ACTIONS,
                    "ui-icon-main-print", "button-main-print",
                    localized(HtmlButtonEnum.PRINT)));

        } else if (buttonPrivileged.contains(NavButtonEnum.TICKET)) {

            final String cssClass;

            if (ConfigManager.instance()
                    .isConfigValue(Key.JOBTICKET_COPIER_ENABLE)) {
                cssClass = CSS_CLASS_MAIN_ACTIONS_BASE;
            } else {
                cssClass = CSS_CLASS_MAIN_ACTIONS;
            }

            items.add(new NavBarItem(cssClass, "ui-icon-main-jobticket",
                    "button-main-print", localized("button-ticket")));
        }

        // ------------
        // Letterhead
        // ------------
        if (buttonPrivileged.contains(NavButtonEnum.LETTERHEAD)) {
            itemWlk = new NavBarItem(CSS_CLASS_MAIN_ACTIONS_BASE,
                    "ui-icon-main-letterhead", "button-main-letterhead",
                    localized("button-letterhead"));
        } else {
            itemWlk = useButtonCandidate(buttonCandidates);
        }

        if (itemWlk != null) {
            items.add(itemWlk);
        }

        // ------------
        // Delete
        // ------------
        items.add(new NavBarItem(CSS_CLASS_MAIN_ACTIONS, "ui-icon-main-clear",
                "button-main-clear", localized(HtmlButtonEnum.DELETE)));

        //
        add(new NavBarRow("main-navbar-row-top", items));

        // ----------------------------------------------------------
        // Row 2
        // ----------------------------------------------------------
        items = new ArrayList<>();

        items.add(new NavBarItem(CSS_CLASS_MAIN_ACTIONS_BASE,
                "ui-icon-main-logout", "button-logout",
                localized(HtmlButtonEnum.LOGOUT)));

        items.add(new NavBarItem(CSS_CLASS_MAIN_ACTIONS_BASE,
                "ui-icon-main-refresh", "button-main-refresh",
                localized(HtmlButtonEnum.REFRESH)));

        items.add(new NavBarItem(CSS_CLASS_MAIN_ACTIONS_BASE,
                "ui-icon-main-doclog", "button-main-doclog",
                localized("button-doclog")));

        if (buttonPrivileged.contains(NavButtonEnum.SORT)) {
            itemWlk = new NavBarItem(CSS_CLASS_MAIN_ACTIONS,
                    "ui-icon-main-arr-edit", "main-arr-edit",
                    localized(HtmlButtonEnum.SORT));
        } else {
            itemWlk = useButtonCandidate(buttonCandidates);
        }

        if (itemWlk != null) {
            items.add(itemWlk);
        }

        add(new NavBarRow("main-navbar-row-bottom", items));
    }

}
