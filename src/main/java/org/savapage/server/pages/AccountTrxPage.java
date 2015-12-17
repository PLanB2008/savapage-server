/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.pages;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.AccountTrxDao;
import org.savapage.core.dao.AccountTrxDao.ListFilter;
import org.savapage.core.dao.helpers.AccountTrxPagerReq;
import org.savapage.core.dao.helpers.AccountTrxTypeEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.services.DocLogService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.ExternalSupplierEnum;
import org.savapage.core.services.helpers.ExternalSupplierStatusEnum;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.BitcoinUtil;
import org.savapage.core.util.CurrencyUtil;
import org.savapage.ext.payment.PaymentMethodEnum;
import org.savapage.server.SpSession;
import org.savapage.server.WebApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 */
public class AccountTrxPage extends AbstractListPage {

    private static final DocLogService DOC_LOG_SERVICE = ServiceContext
            .getServiceFactory().getDocLogService();

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(AccountTrxPage.class);

    /**
     * Maximum number of pages in the navigation bar. IMPORTANT: this must be an
     * ODD number.
     */
    private static final int MAX_PAGES_IN_NAVBAR = 5;

    private final class AccountTrxListView extends PropertyListView<AccountTrx> {

        private static final long serialVersionUID = 1L;

        final int balanceDecimals;
        final String bitcoinUrlPatternTrx;
        final String bitcoinUrlPatternAddr;

        /**
         *
         * @param id
         * @param entryList
         */
        public AccountTrxListView(final String id,
                final List<AccountTrx> entryList) {

            super(id, entryList);

            final ConfigManager cm = ConfigManager.instance();

            this.balanceDecimals = ConfigManager.getUserBalanceDecimals();

            this.bitcoinUrlPatternTrx =
                    cm.getConfigValue(Key.FINANCIAL_BITCOIN_USER_PAGE_URL_PATTERN_TRX);

            this.bitcoinUrlPatternAddr =
                    cm.getConfigValue(Key.FINANCIAL_BITCOIN_USER_PAGE_URL_PATTERN_ADDRESS);

        }

        @Override
        protected void populateItem(final ListItem<AccountTrx> item) {

            final AccountTrx accountTrx = item.getModelObject();
            final DocLog docLog = accountTrx.getDocLog();

            final PrintOut printOut;

            if (docLog != null && docLog.getDocOut() != null) {
                printOut = docLog.getDocOut().getPrintOut();
            } else {
                printOut = null;
            }

            //
            item.add(new Label("trxDate", localizedShortDateTime(accountTrx
                    .getTransactionDate())));
            //
            item.add(new Label("trxActor", accountTrx.getTransactedBy()));

            //
            final String currencySymbol =
                    String.format("%s ", CurrencyUtil.getCurrencySymbol(
                            accountTrx.getCurrencyCode(), getSession()
                                    .getLocale()));

            final String amount;
            final String balance;

            try {
                amount =
                        BigDecimalUtil.localize(accountTrx.getAmount(),
                                this.balanceDecimals, getSession().getLocale(),
                                currencySymbol, true);

                balance =
                        BigDecimalUtil.localize(accountTrx.getBalance(),
                                this.balanceDecimals, getSession().getLocale(),
                                currencySymbol, true);

            } catch (ParseException e) {
                throw new SpException(e);
            }

            Label labelWrk;

            labelWrk = new Label("amount", amount);
            if (accountTrx.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                labelWrk.add(new AttributeModifier("class",
                        MarkupHelper.CSS_AMOUNT_MIN));
            }
            item.add(labelWrk);

            labelWrk = new Label("balance", balance);
            if (accountTrx.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                labelWrk.add(new AttributeModifier("class",
                        MarkupHelper.CSS_AMOUNT_MIN));
            }
            item.add(labelWrk);

            //
            PaymentMethodEnum extPaymentMethod = null;

            if (accountTrx.getExtMethod() != null) {
                try {
                    extPaymentMethod =
                            PaymentMethodEnum
                                    .valueOf(accountTrx.getExtMethod());
                } catch (Exception e) {
                    // noop
                }
            }

            //
            //
            final boolean showDocLogTitle =
                    ConfigManager.instance().isConfigValue(
                            Key.WEBAPP_DOCLOG_SHOW_DOC_TITLE);

            String printOutInfo = null;
            String comment = accountTrx.getComment();
            String imageSrc = null;

            //
            String msgKey;

            String key;

            final AccountTrxTypeEnum trxType =
                    AccountTrxTypeEnum.valueOf(accountTrx.getTrxType());

            switch (trxType) {

            case ADJUST:
                key = "type-adjust";
                break;

            case DEPOSIT:
                key = "type-deposit";
                break;

            case GATEWAY:

                if (extPaymentMethod != null) {
                    imageSrc =
                            WebApp.getPaymentMethodImgUrl(extPaymentMethod,
                                    false);
                }

                key = "type-gateway";
                break;

            case TRANSFER:
                key = "type-transfer";
                break;

            case VOUCHER:
                key = "type-voucher";
                break;

            case PRINT_IN:

                key = "type-print-in";

                final StringBuilder cmt = new StringBuilder();

                if (showDocLogTitle) {
                    cmt.append("(").append(docLog.getTitle()).append(")");
                }

                if (docLog.getNumberOfPages() == 1) {
                    msgKey = "printed-pages-one";
                } else {
                    msgKey = "printed-pages-multiple";
                }

                printOutInfo =
                        localized(msgKey, docLog.getNumberOfPages().toString());

                comment = cmt.toString();

                break;

            case PRINT_OUT:

                if (printOut == null) {
                    throw new SpException("TrxType [" + trxType
                            + "] : unhandled cost source");
                }

                key = "type-print-out";

                final int totalPages =
                        printOut.getNumberOfCopies().intValue()
                                * docLog.getNumberOfPages().intValue();

                if (totalPages == 1) {
                    msgKey = "printed-pages-one";
                } else {
                    msgKey = "printed-pages-multiple";
                }

                printOutInfo = localized(msgKey, String.valueOf(totalPages));

                comment = printOut.getPrinter().getDisplayName();

                if (showDocLogTitle
                        && StringUtils.isNotBlank(docLog.getTitle())) {
                    comment += " (" + docLog.getTitle() + ")";
                }

                break;

            default:
                throw new SpException("TrxType [" + trxType
                        + "] unknown: not handled");
            }

            final boolean isExtBitcoin =
                    StringUtils.isNotBlank(accountTrx.getExtCurrencyCode())
                            && accountTrx.getExtCurrencyCode().equals(
                                    CurrencyUtil.BITCOIN_CURRENCY_CODE);

            // External supplier
            final ExternalSupplierEnum extSupplierEnum =
                    DOC_LOG_SERVICE.getExtSupplier(docLog);

            final String extSupplier;

            if (extSupplierEnum == null) {
                extSupplier = null;
            } else {
                extSupplier = extSupplierEnum.getUiText();
                imageSrc = WebApp.getExtSupplierEnumImgUrl(extSupplierEnum);
            }

            // External supplier status
            final ExternalSupplierStatusEnum extSupplierStatus =
                    DOC_LOG_SERVICE.getExtSupplierStatus(docLog);

            //
            final MarkupHelper helper = new MarkupHelper(item);

            if (StringUtils.isBlank(imageSrc)) {
                helper.discloseLabel("trxImage");
            } else {
                labelWrk =
                        MarkupHelper.createEncloseLabel("trxImage", "", true);
                labelWrk.add(new AttributeModifier("src", imageSrc));
                item.add(labelWrk);
            }

            //
            final boolean isGatewayPaymentMethodInHeader =
                    trxType == AccountTrxTypeEnum.GATEWAY
                            && extPaymentMethod != null;

            if (isGatewayPaymentMethodInHeader) {
                item.add(new Label("trxType", extPaymentMethod.toString()
                        .toLowerCase()));
            } else {
                item.add(new Label("trxType", localized(key)));
            }

            //
            helper.encloseLabel("extSupplier", extSupplier, extSupplier != null);

            if (extSupplierStatus == null) {
                helper.discloseLabel("extStatus");
            } else {
                helper.encloseLabel("extStatus",
                        extSupplierStatus.uiText(getLocale()), true).add(
                        new AttributeAppender("class", MarkupHelper
                                .getCssTxtClass(extSupplierStatus)));
            }

            //
            final boolean isVisible = accountTrx.getPosPurchase() != null;

            if (isVisible) {

                helper.encloseLabel("receiptNumber", accountTrx
                        .getPosPurchase().getReceiptNumber(), isVisible);

                if (StringUtils.isNotBlank(accountTrx.getPosPurchase()
                        .getPaymentType())) {
                    helper.encloseLabel("paymentMethod", accountTrx
                            .getPosPurchase().getPaymentType(), isVisible);
                } else {
                    helper.discloseLabel("paymentMethod");
                }

                if (trxType == AccountTrxTypeEnum.DEPOSIT) {

                    helper.encloseLabel("downloadReceipt",
                            localized("button-receipt"), isVisible);

                    labelWrk = (Label) item.get("downloadReceipt");
                    labelWrk.add(new AttributeModifier("data-savapage",
                            accountTrx.getId().toString()));
                }

            } else {
                helper.discloseLabel("receiptNumber");
                helper.encloseLabel(
                        "paymentMethod",
                        accountTrx.getExtMethod(),
                        !isGatewayPaymentMethodInHeader
                                && StringUtils.isNotBlank(accountTrx
                                        .getExtMethod()));
            }

            if (isExtBitcoin) {

                helper.discloseLabel("bitcoinAddress"); // not now
                helper.discloseLabel("paymentMethodAddress");

                if (StringUtils.isBlank(this.bitcoinUrlPatternTrx)) {

                    helper.discloseLabel("bitcoinTrxHash");

                    helper.encloseLabel("extId", accountTrx.getExtId(), true);

                } else {
                    helper.discloseLabel("extId");

                    helper.addModifyLabelAttr("bitcoinTrxHash",
                            StringUtils.abbreviateMiddle(accountTrx.getExtId(),
                                    "...", 13), "href", MessageFormat.format(
                                    this.bitcoinUrlPatternTrx,
                                    accountTrx.getExtId()));

                }

            } else {
                helper.encloseLabel("paymentMethodAddress", accountTrx
                        .getExtMethodAddress(), StringUtils
                        .isNotBlank(accountTrx.getExtMethodAddress()));

                helper.encloseLabel("extId", accountTrx.getExtId(),
                        StringUtils.isNotBlank(accountTrx.getExtId()));

                helper.discloseLabel("bitcoinAddress");
                helper.discloseLabel("bitcoinTrxHash");
            }

            if (!isVisible || trxType != AccountTrxTypeEnum.DEPOSIT) {
                helper.discloseLabel("downloadReceipt");
            }

            //
            item.add(createVisibleLabel(StringUtils.isNotBlank(comment),
                    "comment", comment));
            //
            item.add(createVisibleLabel(StringUtils.isNotBlank(printOutInfo),
                    "printOut", printOutInfo));

            if (trxType == AccountTrxTypeEnum.GATEWAY
                    && accountTrx.getExtAmount() != null) {

                final StringBuilder ext = new StringBuilder();

                try {

                    final boolean isPending =
                            accountTrx.getAmount().compareTo(BigDecimal.ZERO) == 0;

                    ext.append(
                            CurrencyUtil.getCurrencySymbol(
                                    accountTrx.getExtCurrencyCode(),
                                    getSession().getLocale())).append(" ");

                    if (isExtBitcoin) {
                        ext.append(BigDecimalUtil.localize(accountTrx
                                .getExtAmount(), BitcoinUtil.BTC_DECIMALS,
                                getSession().getLocale(), true));
                    } else {
                        ext.append(BigDecimalUtil.localize(
                                accountTrx.getExtAmount(),
                                this.balanceDecimals, getSession().getLocale(),
                                "", true));
                    }

                    if (accountTrx.getExtFee() != null
                            && accountTrx.getExtFee()
                                    .compareTo(BigDecimal.ZERO) != 0) {

                        ext.append(" -/- ");

                        if (isExtBitcoin) {
                            ext.append(BigDecimalUtil.localize(accountTrx
                                    .getExtFee(), BitcoinUtil.BTC_DECIMALS,
                                    getSession().getLocale(), true));
                        } else {
                            ext.append(BigDecimalUtil.localize(accountTrx
                                    .getExtFee(), this.balanceDecimals,
                                    getSession().getLocale(), "", true));
                        }
                    }

                    if (isPending) {
                        ext.append(" Confirmations (")
                                .append(accountTrx.getExtConfirmations())
                                .append(")");
                    }

                } catch (ParseException e) {
                    throw new SpException(e);
                }

                helper.encloseLabel("extAmount", ext.toString(), true);
            } else {
                helper.discloseLabel("extAmount");
            }

            helper.encloseLabel("extDetails", accountTrx.getExtDetails(),
                    StringUtils.isNotBlank(accountTrx.getExtDetails()));
        }
    }

    @Override
    protected final boolean needMembership() {
        return isAdminRoleContext();
    }

    /**
     *
     */
    public AccountTrxPage() {

        if (isAuthErrorHandled()) {
            return;
        }

        //
        final String data = getParmValue(POST_PARM_DATA);
        final AccountTrxPagerReq req = AccountTrxPagerReq.read(data);

        //
        final ListFilter filter = new ListFilter();

        // filter.setAccountType(AccountTypeEnum.USER); // for now ...

        filter.setTrxType(req.getSelect().getTrxType());
        filter.setDateFrom(req.getSelect().dateFrom());
        filter.setDateTo(req.getSelect().dateTo());
        filter.setContainingCommentText(req.getSelect().getContainingText());

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("trxType [" + filter.getTrxType() + "] dateFrom ["
                    + filter.getDateFrom() + "] dateTo [" + filter.getDateTo()
                    + "] text [" + filter.getContainingCommentText() + "]");
        }

        Long userId = null;
        Long accountId = null;

        /*
         * isAdminWebAppContext() sometimes returns null. Why !?
         */
        final boolean adminWebApp = isAdminRoleContext();

        if (adminWebApp) {

            userId = req.getSelect().getUserId();
            accountId = req.getSelect().getAccountId();

        } else {
            /*
             * If we are called in a User WebApp context we ALWAYS use the user
             * of the current session.
             */
            userId = SpSession.get().getUser().getId();
        }

        filter.setUserId(userId);
        filter.setAccountId(accountId);

        //
        final AccountTrxDao accountTrxDao =
                ServiceContext.getDaoContext().getAccountTrxDao();

        final long logCount = accountTrxDao.getListCount(filter);

        /*
         * Display the requested page.
         */
        final List<AccountTrx> entryList =
                accountTrxDao.getListChunk(filter, req.calcStartPosition(), req
                        .getMaxResults(), req.getSort().getField(), req
                        .getSort().getAscending());

        add(new AccountTrxListView("log-entry-view", entryList));

        /*
         * Display the navigation bars and write the response.
         */
        createNavBarResponse(req, logCount, MAX_PAGES_IN_NAVBAR,
                "sp-accounttrx-page", new String[] { "nav-bar-1", "nav-bar-2" });
    }
}
