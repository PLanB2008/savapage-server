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
package org.savapage.server.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.PrinterGroup;
import org.savapage.core.jpa.PrinterGroupMember;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class JobTicketPrintAddIn extends AbstractAuthPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * .
     */
    private static final JobTicketService JOBTICKET_SERVICE =
            ServiceContext.getServiceFactory().getJobTicketService();

    /**
     * .
     */
    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /**
     *
     */
    private static class RedirectPrinter {

        private Long id;
        private String name;
        private boolean preferred;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isPreferred() {
            return preferred;
        }

        public void setPreferred(boolean preferred) {
            this.preferred = preferred;
        }

    }

    /**
     * .
     */
    private static class RedirectPrinterListView
            extends PropertyListView<RedirectPrinter> {

        /**
         * .
         */
        private static final long serialVersionUID = 1L;

        /**
         *
         * @param id
         *            The wicket id.
         * @param list
         *            The item list.
         */
        RedirectPrinterListView(final String id,
                final List<RedirectPrinter> list) {

            super(id, list);
        }

        /**
         * Transforms a vanilla text to an HTMl string with non-breaking hyphens
         * and spaces.
         *
         * @param vanilla
         *            The vanilla text.
         * @return The HTML string.
         */
        private String asHtml(final String vanilla) {
            return StringUtils.replace(
                    StringUtils.replace(vanilla, "-", "&#8209;"), " ",
                    "&nbsp;");
        }

        @Override
        protected void populateItem(final ListItem<RedirectPrinter> item) {

            final RedirectPrinter printer = item.getModelObject();

            final String id = UUID.randomUUID().toString();

            Label labelWlk;

            //
            labelWlk = new Label("label", asHtml(printer.getName()));
            labelWlk.setEscapeModelStrings(false);
            MarkupHelper.modifyLabelAttr(labelWlk, "for", id);
            item.add(labelWlk);

            //
            labelWlk = new Label("input", "");
            MarkupHelper.modifyLabelAttr(labelWlk, "id", id);
            MarkupHelper.modifyLabelAttr(labelWlk, "value",
                    printer.getId().toString());
            if (printer.isPreferred()) {
                MarkupHelper.modifyLabelAttr(labelWlk, "checked", "checked");
            }
            item.add(labelWlk);
        }

    }

    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     * @param parameters
     *            The {@link PageParameters}.
     */
    public JobTicketPrintAddIn(final PageParameters parameters) {

        super(parameters);

        final String jobFileName = this.getParmValue("jobFileName");

        if (StringUtils.isBlank(jobFileName)) {
            setResponsePage(new MessageContent(AppLogLevelEnum.ERROR,
                    "\"jobFileName\" parameter missing"));
        }

        final OutboxJobDto job;

        try {
            job = JOBTICKET_SERVICE.getTicket(jobFileName);
        } catch (Exception e) {
            setResponsePage(
                    new MessageContent(AppLogLevelEnum.ERROR, e.getMessage()));
            return;
        }

        if (job == null) {
            setResponsePage(new MessageContent(AppLogLevelEnum.WARN,
                    localized("msg-jobticket-not-found")));
            return;
        }

        final MarkupHelper helper = new MarkupHelper(this);

        //
        helper.addModifyLabelAttr("btn-print", localized("button-print"),
                MarkupHelper.ATTR_DATA_SAVAPAGE, jobFileName);

        //
        final List<RedirectPrinter> printerList = this.getRedirectPrinters(job);

        add(new RedirectPrinterListView("printer-radio", printerList));
    }

    /**
     *
     * @param jobFileName
     * @return
     */
    private List<RedirectPrinter> getRedirectPrinters(final OutboxJobDto job) {

        final List<RedirectPrinter> printerList = new ArrayList<>();

        final String groupName = ConfigManager.instance()
                .getConfigValue(Key.JOBTICKET_PROXY_PRINTER_GROUP);

        final PrinterGroup printerGroup = ServiceContext.getDaoContext()
                .getPrinterGroupDao().findByName(groupName);

        if (printerGroup == null) {
            return printerList;
        }

        final boolean colorJob = job.isColorJob();
        final boolean duplexJob = job.isDuplexJob();

        int iPreferred = -1;
        int iPrinter = 0;

        for (final PrinterGroupMember member : printerGroup.getMembers()) {

            final Printer printer = member.getPrinter();

            final JsonProxyPrinter cupsPrinter = PROXYPRINT_SERVICE
                    .getCachedPrinter(printer.getPrinterName());

            if (cupsPrinter == null) {
                throw new IllegalStateException(
                        String.format("Printer [%s] not found in cache.",
                                printer.getPrinterName()));
            }

            /*
             * Check compatibility.
             */
            if (duplexJob && !cupsPrinter.getDuplexDevice()) {
                continue;
            }

            final boolean colorPrinter = cupsPrinter.getColorDevice();

            if (colorJob && !colorPrinter) {
                continue;
            }

            if (iPreferred < 0) {
                if (colorJob) {
                    if (colorPrinter) {
                        iPreferred = iPrinter;
                    }
                } else if (!colorPrinter) {
                    iPreferred = iPrinter;
                }
            }

            final RedirectPrinter redirectPrinter = new RedirectPrinter();
            printerList.add(redirectPrinter);

            redirectPrinter.setId(printer.getId());
            redirectPrinter.setName(printer.getDisplayName());

            iPrinter++;
        }

        if (!printerList.isEmpty() && iPreferred >= 0) {
            printerList.get(iPreferred).setPreferred(true);
        }
        //
        return printerList;
    }
}
