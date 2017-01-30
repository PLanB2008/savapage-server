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

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.dao.enums.PrintInDeniedReasonEnum;
import org.savapage.core.dao.enums.PrintModeEnum;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.CurrencyUtil;
import org.savapage.server.WebApp;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class DocLogItemPanel extends Panel {

    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    private final NumberFormat fmNumber =
            NumberFormat.getInstance(getSession().getLocale());

    private final DateFormat dfShortDateTime = DateFormat.getDateTimeInstance(
            DateFormat.SHORT, DateFormat.SHORT, getSession().getLocale());

    private final DateFormat dfShortTime = DateFormat
            .getTimeInstance(DateFormat.SHORT, getSession().getLocale());

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Number of currency decimals to display.
     */
    private final int currencyDecimals;

    //
    private final boolean showDocLogCost;

    /**
     *
     * @param id
     * @param model
     */
    public DocLogItemPanel(String id, IModel<DocLogItem> model,
            final boolean showFinancialData) {

        super(id, model);

        this.currencyDecimals = ConfigManager.getUserBalanceDecimals();
        this.showDocLogCost = showFinancialData;
    }

    /**
     *
     * @param model
     */
    public void populate(final IModel<DocLogItem> model) {

        final DocLogItem obj = model.getObject();

        String cssClass = null;

        final Map<String, String> mapVisible = new HashMap<>();

        for (final String attr : new String[] { "title", "log-comment",
                "printin-pie", "pdfout-pie", "printout-pie", "printoutMode",
                "signature", "destination", "letterhead", "author", "subject",
                "keywords", "drm", "userpw", "ownerpw", "duplex", "singlex",
                "color", "grayscale", "papersize", "cost-currency", "cost",
                "account-trx", "job-id", "job-state", "job-completed-date",
                "print-in-denied-reason-hyphen", "print-in-denied-reason",
                "collateCopies", "ecoPrint", "removeGraphics", "extSupplier",
                "punch", "staple", "fold", "booklet", "jobticket-media",
                "jobticket-copy", "jobticket-finishing-ext", "landscape" }) {
            mapVisible.put(attr, null);
        }

        MarkupHelper helper = new MarkupHelper(this);

        //
        final String extSupplierImgUrl;

        final boolean isExtSupplier = obj.getExtSupplier() != null;

        if (isExtSupplier) {
            mapVisible.put("extSupplier", obj.getExtSupplier().getUiText());
            extSupplierImgUrl =
                    WebApp.getExtSupplierEnumImgUrl(obj.getExtSupplier());
        } else {
            extSupplierImgUrl = null;
        }

        if (StringUtils.isBlank(extSupplierImgUrl)) {
            helper.discloseLabel("extSupplierImg");
        } else {
            helper.encloseLabel("extSupplierImg", "", true)
                    .add(new AttributeModifier("src", extSupplierImgUrl));
            // disable for now ... (how to check thirdparty?)
            final String thirdPartyUrl = null;
            // WebApp.getThirdPartyEnumImgUrl(ThirdPartyEnum.PAPERCUT);
            if (StringUtils.isBlank(thirdPartyUrl)) {
                helper.discloseLabel("thirdPartyImg");
            } else {
                helper.encloseLabel("thirdPartyImg", "", true)
                        .add(new AttributeModifier("src", thirdPartyUrl));
            }
        }

        //
        final ExternalSupplierStatusEnum extSupplierStatus =
                obj.getExtSupplierStatus();

        if (extSupplierStatus == null) {
            helper.discloseLabel("extStatus");
        } else {
            addVisible(true, "extStatus", extSupplierStatus.uiText(getLocale()),
                    MarkupHelper.getCssTxtClass(extSupplierStatus));
        }

        //
        String cssJobState = null;

        //
        if (StringUtils.isNotBlank(obj.getComment())) {
            mapVisible.put("log-comment", obj.getComment());
        }

        // Account Transactions
        if (!obj.getTransactions().isEmpty()) {

            final StringBuilder builder = new StringBuilder();

            int totWeightDelegators = 0;

            for (final AccountTrx trx : obj.getTransactions()) {

                final Account account = trx.getAccount();

                final AccountTypeEnum accountType =
                        AccountTypeEnum.valueOf(account.getAccountType());

                if (accountType != AccountTypeEnum.SHARED
                        && accountType != AccountTypeEnum.GROUP) {
                    totWeightDelegators +=
                            trx.getTransactionWeight().intValue();
                    continue;
                }

                final Account accountParent = account.getParent();

                builder.append(" • ");

                if (accountParent != null) {
                    builder.append(accountParent.getName()).append('\\');
                }

                builder.append(account.getName());

                if (trx.getAmount().compareTo(BigDecimal.ZERO) != 0) {
                    builder.append(" ")
                            .append(CurrencyUtil.getCurrencySymbol(
                                    trx.getCurrencyCode(), getLocale()))
                            .append(" ")
                            .append(localizedDecimal(trx.getAmount()));
                }

                builder.append(" (").append(trx.getTransactionWeight())
                        .append(')');
            }

            if (isExtSupplier && totWeightDelegators > 0) {
                builder.append(" • ");
                builder.append(localized("delegators")).append(" (")
                        .append(totWeightDelegators).append(")");
            }

            mapVisible.put("account-trx", builder.toString());
        }

        //
        if (obj.getDocType() == DocLogDao.Type.IN) {

            cssClass = MarkupHelper.CSS_PRINT_IN_QUEUE;

            if (obj.getPaperSize() != null) {
                mapVisible.put("papersize", obj.getPaperSize().toUpperCase());
            }

            if (obj.getDrmRestricted()) {
                mapVisible.put("drm", "DRM");
            }

            if (!obj.getPrintInPrinted()) {

                mapVisible.put("print-in-denied-reason-hyphen", "-");

                PrintInDeniedReasonEnum deniedReason =
                        obj.getPrintInDeniedReason();

                String key = "print-in-denied-reason-unknown";
                if (deniedReason != null
                        && deniedReason.equals(PrintInDeniedReasonEnum.DRM)) {
                    key = "print-in-denied-reason-drm";
                }

                mapVisible.put("print-in-denied-reason", localized(key));

            } else {
                mapVisible.put("printin-pie",
                        String.valueOf(obj.getTotalPages()));
            }

        } else {

            /*
             * Not for now...
             */
            // mapVisible.put("signature", obj.getSignature());

            if (obj.getLetterhead()) {
                mapVisible.put("letterhead", "LH");
            }

            if (obj.getDocType() == DocLogDao.Type.PDF) {

                cssClass = MarkupHelper.CSS_PRINT_OUT_PDF;

                mapVisible.put("destination", obj.getDestination());

                mapVisible.put("author", obj.getAuthor());
                mapVisible.put("subject", obj.getSubject());
                mapVisible.put("keywords", obj.getKeywords());

                if (obj.getDrmRestricted()) {
                    mapVisible.put("drm", "DRM");
                }
                if (obj.getUserPw()) {
                    mapVisible.put("userpw", "U");
                }
                if (obj.getOwnerPw()) {
                    mapVisible.put("ownerpw", "O");
                }

                mapVisible.put("pdfout-pie",
                        String.valueOf(obj.getTotalPages()));

            } else {

                final String ticketNumber;
                if (obj.getPrintMode() == PrintModeEnum.TICKET
                        || obj.getPrintMode() == PrintModeEnum.TICKET_C
                        || obj.getPrintMode() == PrintModeEnum.TICKET_E) {
                    ticketNumber = obj.getExtId();
                } else {
                    ticketNumber = null;
                }

                mapVisible.put("printoutMode", String
                        .format("%s %s", obj.getPrintMode().uiText(getLocale()),
                                StringUtils.defaultString(ticketNumber))
                        .trim());

                cssClass = MarkupHelper.CSS_PRINT_OUT_PRINTER;

                if (obj.getDuplex()) {
                    mapVisible.put("duplex", localized("duplex"));
                } else {
                    mapVisible.put("singlex", localized("singlex"));
                }

                if (obj.getGrayscale()) {
                    mapVisible.put("grayscale", localized("grayscale"));
                } else {
                    mapVisible.put("color", localized("color"));
                }

                if (obj.isFinishingPunch()) {
                    mapVisible.put("punch", localized("punch"));
                }
                if (obj.isFinishingStaple()) {
                    mapVisible.put("staple", localized("staple"));
                }
                if (obj.isFinishingFold()) {
                    mapVisible.put("fold", localized("fold"));
                }
                if (obj.isFinishingBooklet()) {
                    mapVisible.put("booklet", localized("booklet"));
                }

                mapVisible.put("papersize", obj.getPaperSize().toUpperCase());

                if (obj.getJobId() != null
                        && !obj.getJobId().equals(Integer.valueOf(0))) {
                    mapVisible.put("job-id", obj.getJobId().toString());
                }

                if (this.showDocLogCost
                        && obj.getCost().compareTo(BigDecimal.ZERO) != 0) {
                    mapVisible.put("cost-currency",
                            CurrencyUtil.getCurrencySymbol(
                                    obj.getCurrencyCode(),
                                    getSession().getLocale()));
                    mapVisible.put("cost", localizedDecimal(obj.getCost()));
                }

                String sfx = null;

                switch (obj.getJobState()) {
                case IPP_JOB_ABORTED:
                    sfx = "aborted";
                    break;
                case IPP_JOB_CANCELED:
                    sfx = "canceled";
                    break;
                case IPP_JOB_COMPLETED:
                    sfx = "completed";
                    break;
                case IPP_JOB_HELD:
                    sfx = "held";
                    break;
                case IPP_JOB_PENDING:
                    sfx = "pending";
                    break;
                case IPP_JOB_PROCESSING:
                    sfx = "processing";
                    break;
                case IPP_JOB_STOPPED:
                    sfx = "stopped";
                    break;
                }

                cssJobState = MarkupHelper.getCssTxtClass(obj.getJobState());

                if (sfx != null) {
                    mapVisible.put("job-state", localized("job-state-" + sfx));
                    if (obj.getCompletedDate() != null) {
                        mapVisible.put("job-completed-date",
                                localizedShortTime(obj.getCompletedDate()));
                    }
                }

                final String sparklineData =
                        String.format("%d,%d", obj.getTotalSheets(),
                                obj.getTotalPages() * obj.getCopies()
                                        - obj.getTotalSheets());

                mapVisible.put("printout-pie", sparklineData);
            }
        }

        Label labelWlk = new Label("header");
        labelWlk.add(new AttributeModifier("class", cssClass));
        add(labelWlk);

        //
        add(new Label("user-name", obj.getUserId()));
        //
        add(new Label("dateCreated",
                localizedShortDateTime(obj.getCreatedDate())));

        //
        String title = null;
        if (ConfigManager.instance()
                .isConfigValue(Key.WEBAPP_DOCLOG_SHOW_DOC_TITLE)) {
            title = obj.getTitle();
        }
        mapVisible.put("title", title);

        /*
         * Totals
         */
        final StringBuilder totals = new StringBuilder();

        //
        String key = null;

        int total = obj.getTotalPages();
        int copies = obj.getCopies();

        //
        totals.append(localizedNumber(total));
        key = (total == 1) ? "page" : "pages";
        totals.append(" ").append(localized(key));

        // n-up
        if (obj.getNumberUp() != null && obj.getNumberUp().intValue() > 1) {
            totals.append(", ").append(localized("n-up", obj.getNumberUp()));
        }

        //
        if (copies > 1) {
            totals.append(", ").append(copies).append(" ");
            if (obj.getCollateCopies() != null) {
                if (obj.getCollateCopies()) {
                    key = "collated";
                } else {
                    key = "uncollated";
                }
                totals.append(localized(key)).append(" ");
            }

            totals.append(localized("copies"));
        }

        //
        total = obj.getTotalSheets();
        if (total > 0) {
            key = (total == 1) ? "sheet" : "sheets";
            totals.append(" (").append(total).append(" ").append(localized(key))
                    .append(")");
        }

        //
        if (obj.getHumanReadableByteCount() != null) {
            totals.append(", ").append(obj.getHumanReadableByteCount());
        }

        add(new Label("totals", totals.toString()));

        if (obj.getEcoPrint() != null && obj.getEcoPrint()) {
            mapVisible.put("ecoPrint", "EcoPrint");
        }

        if (obj.getRemoveGraphics() != null && obj.getRemoveGraphics()) {
            mapVisible.put("removeGraphics", localized("graphics-removed"));
        }

        mapVisible.put("jobticket-media",
                PROXYPRINT_SERVICE.getJobTicketOptionsUiText(getLocale(),
                        IppDictJobTemplateAttr.JOBTICKET_ATTR_MEDIA,
                        obj.getIppOptionMap()));

        mapVisible.put("jobticket-copy",
                PROXYPRINT_SERVICE.getJobTicketOptionsUiText(getLocale(),
                        IppDictJobTemplateAttr.JOBTICKET_ATTR_COPY,
                        obj.getIppOptionMap()));

        mapVisible.put("jobticket-finishings-ext",
                PROXYPRINT_SERVICE.getJobTicketOptionsUiText(getLocale(),
                        IppDictJobTemplateAttr.JOBTICKET_ATTR_FINISHINGS_EXT,
                        obj.getIppOptionMap()));

        //
        if (obj.getIppOptionMap() != null
                && obj.getIppOptionMap().isLandscapeJob()) {
            mapVisible.put("landscape", localized("landscape"));
        }

        /*
         * Hide/Show
         */
        for (final Map.Entry<String, String> entry : mapVisible.entrySet()) {

            if (entry.getValue() == null) {
                entry.setValue("");
            }

            String cssClassWlk = null;

            if (entry.getKey().equals("job-state")) {
                cssClassWlk = cssJobState;
            }

            addVisible(StringUtils.isNotBlank(entry.getValue()), entry.getKey(),
                    entry.getValue(), cssClassWlk);
        }

    }

    /**
     *
     * @param isVisible
     * @param id
     * @param val
     */
    private final void addVisible(final boolean isVisible, final String id,
            final String val, final String cssClass) {

        Label label = new Label(id, val) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isVisible;
            }
        };

        if (isVisible && cssClass != null) {
            label.add(new AttributeModifier("class", cssClass));
        }

        add(label);
    }

    /**
     * Gets the localized string for a BigDecimal.
     *
     * @param vlaue
     *            The {@link BigDecimal}.
     * @return The localized string.
     */
    protected final String localizedDecimal(final BigDecimal value) {
        try {
            return BigDecimalUtil.localize(value, this.currencyDecimals,
                    getSession().getLocale(), true);
        } catch (ParseException e) {
            throw new SpException(e);
        }
    }

    /**
     * Gets as localized string of a Number. The locale of the current session
     * is used.
     *
     * @param number
     * @return The localized string.
     */
    protected final String localizedNumber(final long number) {
        return fmNumber.format(number);
    }

    /**
     * Gets as localized short date/time string of a Date. The locale of the
     * current session is used.
     *
     * @param date
     *            The date.
     * @return The localized short date/time string.
     */
    protected final String localizedShortDateTime(final Date date) {
        return dfShortDateTime.format(date);
    }

    /**
     * Gets as localized short time string of a Date. The locale of the current
     * session is used.
     *
     * @param date
     *            The date.
     * @return The localized short time string.
     */
    protected final String localizedShortTime(final Date date) {
        return dfShortTime.format(date);
    }

    /**
     * Gives the localized string for a key.
     *
     * @param key
     *            The key from the XML resource file
     * @return The localized string.
     */
    protected final String localized(final String key) {
        return getLocalizer().getString(key, this);
    }

    /**
     * Localizes and format a string with placeholder arguments.
     *
     * @param key
     *            The key from the XML resource file
     * @param objects
     *            The values to fill the placeholders
     * @return The localized string.
     */
    protected final String localized(final String key,
            final Object... objects) {
        return MessageFormat.format(getLocalizer().getString(key, this),
                objects);
    }

}
