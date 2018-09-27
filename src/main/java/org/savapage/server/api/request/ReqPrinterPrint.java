/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.api.request;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.codehaus.jackson.JsonProcessingException;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.DeviceDao;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.enums.DeviceTypeEnum;
import org.savapage.core.dao.enums.PrintModeEnum;
import org.savapage.core.dao.enums.ProxyPrintAuthModeEnum;
import org.savapage.core.dao.helpers.JsonPrintDelegation;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.IppMediaSourceCostDto;
import org.savapage.core.dto.PrintDelegationDto;
import org.savapage.core.i18n.JobTicketNounEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.imaging.EcoPrintPdfTaskPendingException;
import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.inbox.InboxInfoDto.InboxJob;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.pdf.PdfPageRotateHelper;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.print.proxy.ProxyPrintAuthManager;
import org.savapage.core.print.proxy.ProxyPrintException;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.print.proxy.ProxyPrintJobChunk;
import org.savapage.core.print.proxy.ProxyPrintJobChunkInfo;
import org.savapage.core.print.proxy.ProxyPrintJobChunkRange;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.AccountTrxInfo;
import org.savapage.core.services.helpers.AccountTrxInfoSet;
import org.savapage.core.services.helpers.InboxSelectScopeEnum;
import org.savapage.core.services.helpers.PageRangeException;
import org.savapage.core.services.helpers.PageScalingEnum;
import org.savapage.core.services.helpers.PrinterAttrLookup;
import org.savapage.core.services.helpers.ProxyPrintCostDto;
import org.savapage.core.services.helpers.ProxyPrintCostParms;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.DateUtil;
import org.savapage.ext.papercut.PaperCutServerProxy;
import org.savapage.ext.papercut.job.PaperCutPrintMonitorJob;
import org.savapage.server.api.JsonApiDict;
import org.savapage.server.pages.user.Print;
import org.savapage.server.session.SpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Handles a Proxy Print request.
 *
 * IMPORTANT: The printing transaction MUST be guarded by
 * {@link ConfigManager#readPrintOutLock()}. This is managed by the caller of
 * this request via {@link JsonApiDict#getPrintOutLockNeeded(String)}.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqPrinterPrint extends ApiRequestMixin {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ReqPrinterPrint.class);

    /** */
    public enum JobTicketTypeEnum {
        /** */
        PRINT, COPY
    }

    /**
     *
     * @author Rijk Ravestein
     *
     */
    @JsonInclude(Include.NON_NULL)
    private static class DtoReq extends AbstractDto {

        private String user;
        private String printer;
        private String readerName;
        private String jobName;
        private Integer jobIndex;
        private Boolean landscapeView;
        private PageScalingEnum pageScaling;
        private Integer copies;
        private String ranges;
        private Boolean collate;
        private Boolean removeGraphics;
        private Boolean ecoprint;
        private InboxSelectScopeEnum clearScope;
        private Boolean separateDocs;
        private Boolean jobTicket;
        private JobTicketTypeEnum jobTicketType;
        private String jobTicketTag;
        private Integer jobTicketCopyPages;
        private Long jobTicketDate;
        private Integer jobTicketHrs;
        private Integer jobTicketMin;
        private String jobTicketRemark;
        private Map<String, String> options;
        private PrintDelegationDto delegation;
        private Long accountId;

        @SuppressWarnings("unused")
        public String getUser() {
            return user;
        }

        @SuppressWarnings("unused")
        public void setUser(String user) {
            this.user = user;
        }

        public String getPrinter() {
            return printer;
        }

        @SuppressWarnings("unused")
        public void setPrinter(String printer) {
            this.printer = printer;
        }

        public String getReaderName() {
            return readerName;
        }

        @SuppressWarnings("unused")
        public void setReaderName(String readerName) {
            this.readerName = readerName;
        }

        public String getJobName() {
            return jobName;
        }

        @SuppressWarnings("unused")
        public void setJobName(String jobName) {
            this.jobName = jobName;
        }

        /**
         * @return zero or greater for a specific job, LT zero when all jobs.
         */
        public Integer getJobIndex() {
            return jobIndex;
        }

        @SuppressWarnings("unused")
        public void setJobIndex(Integer jobIndex) {
            this.jobIndex = jobIndex;
        }

        public Boolean getLandscapeView() {
            return landscapeView;
        }

        public void setLandscapeView(Boolean landscapeView) {
            this.landscapeView = landscapeView;
        }

        public PageScalingEnum getPageScaling() {
            return pageScaling;
        }

        @SuppressWarnings("unused")
        public void setPageScaling(PageScalingEnum pageScaling) {
            this.pageScaling = pageScaling;
        }

        public Integer getCopies() {
            return copies;
        }

        @SuppressWarnings("unused")
        public void setCopies(Integer copies) {
            this.copies = copies;
        }

        public String getRanges() {
            return ranges;
        }

        @SuppressWarnings("unused")
        public void setRanges(String ranges) {
            this.ranges = ranges;
        }

        public Boolean getCollate() {
            return collate;
        }

        @SuppressWarnings("unused")
        public void setCollate(Boolean collate) {
            this.collate = collate;
        }

        public Boolean getRemoveGraphics() {
            return removeGraphics;
        }

        @SuppressWarnings("unused")
        public void setRemoveGraphics(Boolean removeGraphics) {
            this.removeGraphics = removeGraphics;
        }

        public Boolean getEcoprint() {
            return ecoprint;
        }

        @SuppressWarnings("unused")
        public void setEcoprint(Boolean ecoprint) {
            this.ecoprint = ecoprint;
        }

        public InboxSelectScopeEnum getClearScope() {
            return clearScope;
        }

        @SuppressWarnings("unused")
        public void setClearScope(InboxSelectScopeEnum clearScope) {
            this.clearScope = clearScope;
        }

        public Boolean getSeparateDocs() {
            return separateDocs;
        }

        @SuppressWarnings("unused")
        public void setSeparateDocs(Boolean separateDocs) {
            this.separateDocs = separateDocs;
        }

        public Boolean getJobTicket() {
            return jobTicket;
        }

        @SuppressWarnings("unused")
        public void setJobTicket(Boolean jobTicket) {
            this.jobTicket = jobTicket;
        }

        public JobTicketTypeEnum getJobTicketType() {
            return jobTicketType;
        }

        @SuppressWarnings("unused")
        public void setJobTicketType(JobTicketTypeEnum jobTicketType) {
            this.jobTicketType = jobTicketType;
        }

        public String getJobTicketTag() {
            return jobTicketTag;
        }

        @SuppressWarnings("unused")
        public void setJobTicketTag(String jobTicketTag) {
            this.jobTicketTag = jobTicketTag;
        }

        public Integer getJobTicketCopyPages() {
            return jobTicketCopyPages;
        }

        @SuppressWarnings("unused")
        public void setJobTicketCopyPages(Integer jobTicketCopyPages) {
            this.jobTicketCopyPages = jobTicketCopyPages;
        }

        public Long getJobTicketDate() {
            return jobTicketDate;
        }

        @SuppressWarnings("unused")
        public void setJobTicketDate(Long jobTicketDate) {
            this.jobTicketDate = jobTicketDate;
        }

        public Integer getJobTicketHrs() {
            return jobTicketHrs;
        }

        @SuppressWarnings("unused")
        public void setJobTicketHrs(Integer jobTicketHrs) {
            this.jobTicketHrs = jobTicketHrs;
        }

        public Integer getJobTicketMin() {
            return jobTicketMin;
        }

        @SuppressWarnings("unused")
        public void setJobTicketMin(Integer jobTicketMin) {
            this.jobTicketMin = jobTicketMin;
        }

        public String getJobTicketRemark() {
            return jobTicketRemark;
        }

        @SuppressWarnings("unused")
        public void setJobTicketRemark(String jobTicketRemark) {
            this.jobTicketRemark = jobTicketRemark;
        }

        public Map<String, String> getOptions() {
            return options;
        }

        @SuppressWarnings("unused")
        public void setOptions(Map<String, String> options) {
            this.options = options;
        }

        public PrintDelegationDto getDelegation() {
            return delegation;
        }

        @SuppressWarnings("unused")
        public void setDelegation(PrintDelegationDto delegation) {
            this.delegation = delegation;
        }

        public Long getAccountId() {
            return accountId;
        }

        @SuppressWarnings("unused")
        public void setAccountId(Long accountId) {
            this.accountId = accountId;
        }

        @JsonIgnore
        public boolean isDelegatedPrint() {
            return this.delegation != null
                    && (!this.delegation.getGroups().isEmpty()
                            || !this.delegation.getUsers().isEmpty()
                            || !this.delegation.getCopies().isEmpty());
        }
    }

    /**
     * Trims and optionally sanitizes the print job name.
     *
     * @param cm
     *            The config manager.
     * @param jobName
     *            The print job name.
     * @return Trimmed and sanitized job name.
     */
    private static String sanitizeIppJobName(final ConfigManager cm,
            final String jobName) {

        if (cm.isConfigValue(Key.IPP_JOB_NAME_SPACE_TO_UNDERSCORE_ENABLE)) {
            return jobName.trim().replaceAll("\\s+", "_");
        }
        return jobName.trim();
    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws JsonProcessingException, IOException, ParseException {

        final DtoReq dtoReq =
                DtoReq.create(DtoReq.class, this.getParmValueDto());

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(DtoReq.prettyPrint(this.getParmValueDto()));
        }

        // Validate
        if (!validateCommon(dtoReq)) {
            return;
        }

        final boolean isJobTicket = BooleanUtils.isTrue(dtoReq.getJobTicket());

        final ConfigManager cm = ConfigManager.instance();

        // First action.
        dtoReq.setJobName(sanitizeIppJobName(cm, dtoReq.getJobName()));

        // INVARIANT
        if (isJobTicket && cm.isConfigValue(Key.JOBTICKET_TAGS_ENABLE)
                && cm.isConfigValue(Key.JOBTICKET_TAGS_REQUIRED)
                && StringUtils.isBlank(dtoReq.getJobTicketTag())) {

            setApiResult(ApiResultCodeEnum.INFO,
                    "msg-print-select-option-required",
                    JobTicketNounEnum.TAG.uiText(getLocale()));
            return;
        }

        // INVARIANT
        if (dtoReq.getAccountId() != null && dtoReq.getAccountId()
                .equals(Print.OPTION_VALUE_SELECT_PROMPT)) {

            setApiResult(ApiResultCodeEnum.INFO,
                    "msg-print-select-option-required",
                    PrintOutNounEnum.ACCOUNT.uiText(getLocale()));
            return;
        }

        final boolean isCopyJobTicket = isJobTicket
                && dtoReq.getJobTicketType() == JobTicketTypeEnum.COPY;

        // Validate
        if (isCopyJobTicket) {
            if (!validateJobTicketCopy(dtoReq)) {
                return;
            }
        } else {
            if (!validateProxyPrintFiltering(dtoReq)) {
                return;
            }
        }

        /*
         * If/how to clear the inbox.
         */
        final InboxSelectScopeEnum clearScope;

        if (isCopyJobTicket) {

            clearScope = InboxSelectScopeEnum.NONE;

        } else if (cm.isConfigValue(
                Key.WEBAPP_USER_PROXY_PRINT_CLEAR_INBOX_ENABLE)) {
            /*
             * Overrule scope with system setting.
             */
            clearScope = cm.getConfigEnum(InboxSelectScopeEnum.class,
                    Key.WEBAPP_USER_PROXY_PRINT_CLEAR_INBOX_SCOPE);

            if (clearScope == null) {
                throw new IllegalStateException("Invalid clear scope.");
            }

        } else if (dtoReq.getClearScope() == null) {
            clearScope = InboxSelectScopeEnum.NONE;
        } else {
            clearScope = dtoReq.getClearScope();
        }

        /*
         * Printer access?
         */
        final DeviceDao deviceDao =
                ServiceContext.getDaoContext().getDeviceDao();

        final Printer printer;

        try {
            printer = PROXY_PRINT_SERVICE.getValidateProxyPrinterAccess(
                    lockedUser, dtoReq.getPrinter(),
                    ServiceContext.getTransactionDate());

        } catch (ProxyPrintException e) {
            setApiResultText(ApiResultCodeEnum.ERROR, e.getMessage());
            return;
        }

        //
        final InboxInfoDto jobs;

        final boolean isPrintAllDocuments = dtoReq.getJobIndex().intValue() < 0;
        final boolean isPrintAllPages;

        int nPagesPrinted;
        String ranges;

        if (isCopyJobTicket) {

            jobs = null;
            nPagesPrinted = dtoReq.getJobTicketCopyPages().intValue();
            ranges = null;
            isPrintAllPages = true;

        } else {

            jobs = INBOX_SERVICE.getInboxInfo(requestingUser);
            ranges = dtoReq.getRanges().trim();
            isPrintAllPages = ranges.isEmpty();

            final StringBuilder sortedRangesOut = new StringBuilder();

            try {
                nPagesPrinted = INBOX_SERVICE.calcPagesInRanges(jobs,
                        dtoReq.getJobIndex().intValue(), ranges,
                        sortedRangesOut);

            } catch (PageRangeException e) {
                setApiResultText(ApiResultCodeEnum.ERROR,
                        e.getMessage(getLocale()));
                return;
            }

            /*
             * This gives the SORTED ranges as string: CUPS cannot handle ranges
             * like '7-8,5,2' but needs '2,5,7-8'
             */
            ranges = sortedRangesOut.toString();
        }

        /*
         * INVARIANT: when NOT a job ticket the total number of printed pages
         * MUST be within limits. When Job Ticket printer is present, this
         * option is prompted to the user.
         */
        if (!isJobTicket) {

            final int totPages = dtoReq.getCopies().intValue() * nPagesPrinted;

            final Integer maxPages =
                    cm.getConfigInteger(Key.PROXY_PRINT_MAX_PAGES);

            if (maxPages != null && totPages > maxPages.intValue()) {

                if (PROXY_PRINT_SERVICE.isJobTicketPrinterPresent()) {
                    setApiResult(ApiResultCodeEnum.WARN,
                            "msg-print-exceeds-jobticket-pagelimit",
                            String.valueOf(totPages), String.valueOf(maxPages));
                } else {
                    setApiResult(ApiResultCodeEnum.ERROR,
                            "msg-print-exceeds-pagelimit",
                            String.valueOf(totPages), String.valueOf(maxPages));
                }
                return;
            }
        }

        /*
         * Vanilla jobs and job separation?
         */
        final boolean isSeparateVanillaJobsCandidate =
                isPrintAllDocuments && isPrintAllPages && jobs != null
                        && INBOX_SERVICE.isInboxVanilla(jobs);

        final boolean doSeparateVanillaJobs;

        if (isSeparateVanillaJobsCandidate) {
            if (isJobTicket) {
                doSeparateVanillaJobs =
                        BooleanUtils.isTrue(dtoReq.getSeparateDocs());
            } else {
                if (cm.isConfigValue(
                        Key.WEBAPP_USER_PROXY_PRINT_SEPARATE_ENABLE)
                        && dtoReq.getSeparateDocs() != null) {
                    doSeparateVanillaJobs =
                            dtoReq.getSeparateDocs().booleanValue();
                } else {
                    doSeparateVanillaJobs = cm.isConfigValue(
                            Key.WEBAPP_USER_PROXY_PRINT_SEPARATE);
                }
            }
        } else {
            doSeparateVanillaJobs = false;
        }

        final boolean doChunkVanillaJobs;
        final Integer iVanillaJob;

        if (doSeparateVanillaJobs) {
            iVanillaJob = null;
            doChunkVanillaJobs = true;
        } else if (isPrintAllDocuments) {
            iVanillaJob = null;
            doChunkVanillaJobs = false;
        } else {
            iVanillaJob = dtoReq.getJobIndex();
            doChunkVanillaJobs = true;
        }

        /*
         * Create the proxy print request, and chunk it.
         */
        final ProxyPrintInboxReq printReq = new ProxyPrintInboxReq(iVanillaJob);

        printReq.setCollate(dtoReq.getCollate());
        printReq.setJobName(dtoReq.getJobName());
        printReq.setPageRanges(ranges);
        printReq.setNumberOfCopies(dtoReq.getCopies());
        printReq.setNumberOfPages(nPagesPrinted);
        printReq.setOptionValues(dtoReq.getOptions());
        printReq.setPrinterName(dtoReq.getPrinter());
        printReq.setRemoveGraphics(dtoReq.getRemoveGraphics());
        printReq.setEcoPrintShadow(dtoReq.getEcoprint());
        printReq.setLocale(this.getLocale());
        printReq.setIdUser(lockedUser.getId());
        printReq.setClearScope(clearScope);

        printReq.setConvertToGrayscale(!isJobTicket && printReq.isGrayscale()
                && PROXY_PRINT_SERVICE.isColorPrinter(dtoReq.getPrinter())
                && PRINTER_SERVICE.isClientSideMonochrome(printer));

        printReq.setLocalBooklet(!isJobTicket
                && ProxyPrintInboxReq.isBooklet(dtoReq.getOptions())
                && PRINTER_SERVICE.isClientSideBooklet(printer));

        final boolean isDelegatedPrint = applyPrintDelegation(dtoReq, printReq);
        final boolean isSharedAccountPrint;

        if (isDelegatedPrint) {
            isSharedAccountPrint = false;
        } else {
            isSharedAccountPrint = applySharedAccount(dtoReq, printReq);
        }

        if (isCopyJobTicket) {

            if (!validateAddCopyJobRequestOpts(printer, printReq)) {
                return;
            }

        } else {

            try {
                PROXY_PRINT_SERVICE.chunkProxyPrintRequest(lockedUser, printReq,
                        dtoReq.getPageScaling(), doChunkVanillaJobs,
                        iVanillaJob);
            } catch (ProxyPrintException e) {
                if (e.hasLogFileMessage()) {
                    LOGGER.warn(e.getLogFileMessage());
                }
                setApiResultText(ApiResultCodeEnum.WARN, e.getMessage());
                return;
            }

            /*
             * INVARIANT: User expected page orientation MUST match actual
             * orientation.
             */
            if (!validatePdfOrientation(dtoReq, printReq.getJobChunkInfo())) {
                return;
            }
        }

        /*
         * Non-secure Proxy Print, integrated with PaperCut?
         */
        final boolean isNonSecureProxyPrint = dtoReq.getReaderName() == null;

        final boolean isExtPaperCutPrint =
                isNonSecureProxyPrint && PAPERCUT_SERVICE
                        .isMonitorPaperCutPrintStatus(printer.getPrinterName(),
                                isDelegatedPrint || isSharedAccountPrint);

        if (isExtPaperCutPrint) {

            final List<String> usersNotFound = checkPaperCutUsers(
                    requestingUser, printReq.getAccountTrxInfoSet());

            if (!usersNotFound.isEmpty()) {

                final StringBuilder users = new StringBuilder();

                for (final String id : usersNotFound) {
                    users.append(id).append(' ');
                }
                setApiResult(ApiResultCodeEnum.ERROR,
                        "msg-print-users-missing-in-papercut",
                        users.toString().trim());
                return;
            }
        }

        /*
         * Calculate the printing cost.
         */
        final JsonProxyPrinter proxyPrinter =
                PROXY_PRINT_SERVICE.getCachedPrinter(printReq.getPrinterName());

        /*
         * INVARIANT:
         */
        if (!validateOptions(proxyPrinter, printReq.getOptionValues())) {
            return;
        }

        final String currencySymbol = SpSession.getAppCurrencySymbol();

        final ProxyPrintCostDto costResult;

        try {
            /*
             * NOTE: For JobTickets the SavaPage calculated costs are leading.
             */
            if (isExtPaperCutPrint && !isJobTicket) {
                /*
                 * No need to calculate the cost since it is taken from PaperCut
                 * after PaperCut reports that job is printed successfully.
                 */
                costResult = new ProxyPrintCostDto();
            } else {
                /*
                 * Set the common parameters for all print job chunks, and
                 * calculate the cost.
                 */
                final ProxyPrintCostParms costParms =
                        printReq.createProxyPrintCostParms(proxyPrinter);

                costResult = ACCOUNTING_SERVICE.calcProxyPrintCost(
                        ServiceContext.getLocale(), currencySymbol, lockedUser,
                        printer, costParms, printReq.getJobChunkInfo());
            }
        } catch (ProxyPrintException e) {
            this.setApiResultText(ApiResultCodeEnum.WARN, e.getMessage());
            return;
        }

        /*
         * INVARIANT:
         */
        if (!validatePruneIppOptions(proxyPrinter, printReq.getOptionValues(),
                costResult, isJobTicket)) {
            return;
        }

        printReq.setCostResult(costResult);

        /*
         * Copy Job Ticket?
         */
        if (isCopyJobTicket) {
            this.onJobTicketCopy(lockedUser, dtoReq, printReq, currencySymbol);
            return;
        }

        /*
         * Print Job Ticket?
         */
        if (isJobTicket) {
            this.onJobTicketPrint(lockedUser, dtoReq, printReq, currencySymbol);
            return;
        }

        try {
            /*
             * Direct Proxy Print integrated with PaperCut?
             */
            if (isExtPaperCutPrint) {
                this.onExtPaperCutPrint(lockedUser, dtoReq, printReq,
                        currencySymbol);
                return;
            }
            /*
             * Direct Proxy Print?
             */
            if (isNonSecureProxyPrint) {
                this.onNonSecurePrint(lockedUser, dtoReq, printReq,
                        currencySymbol);
                return;
            }
        } catch (IppConnectException e) {
            setApiResult(ApiResultCodeEnum.WARN,
                    "msg-print-service-unavailable");
            return;
        }

        /*
         * Proxy Print Authentication is needed (secure printing).
         */
        final Device device = deviceDao.findByName(dtoReq.getReaderName());

        /*
         * INVARIANT: Device MUST exits.
         */
        if (device == null) {
            throw new SpException(
                    "Reader Device [" + dtoReq.getReaderName() + "] NOT found");
        }
        /*
         * INVARIANT: Device MUST be enabled.
         */
        if (device.getDisabled()) {
            throw new SpException(
                    "Device [" + dtoReq.getReaderName() + "] is disabled");
        }

        /*
         * INVARIANT: Device MUST be a reader.
         */
        if (!deviceDao.isCardReader(device)) {
            throw new SpException("Device [" + dtoReq.getReaderName()
                    + "] is NOT a Card Reader");
        }

        /*
         * INVARIANT: Reader MUST have Printer restriction.
         */
        if (!deviceDao.hasPrinterRestriction(device)) {
            throw new SpException("Reader [" + dtoReq.getReaderName()
                    + "] does not have associated Printer(s).");
        }

        /*
         * INVARIANT: Reader MUST have Printer restriction.
         */
        if (device.getPrinter() == null) {

            if (!PRINTER_SERVICE.checkDeviceSecurity(printer,
                    DeviceTypeEnum.CARD_READER, device)) {

                throw new SpException("Reader [" + dtoReq.getReaderName()
                        + "] does not have associated Printer(s).");
            }
        }

        /*
         * Hold Print?
         */
        final ProxyPrintAuthModeEnum authModeEnum =
                DEVICE_SERVICE.getProxyPrintAuthMode(device.getId());

        if (authModeEnum.isHoldRelease()) {
            this.onHoldPrint(lockedUser, dtoReq, printReq, currencySymbol);
            return;
        }

        /*
         * Secure WebApp Proxy Print.
         */
        printReq.setPrintMode(PrintModeEnum.AUTH);
        printReq.setStatus(ProxyPrintInboxReq.Status.NEEDS_AUTH);

        if (ProxyPrintAuthManager.submitRequest(dtoReq.getPrinter(),
                device.getHostname(), printReq)) {
            onSecurePrint(printReq, currencySymbol);
        } else {
            setApiResult(ApiResultCodeEnum.WARN, "msg-print-auth-pending");
        }
    }

    /**
     * Adds and validates dummy options to the proxy print request for a Copy
     * Job.
     *
     * @param printer
     *            The printer.
     * @param printReq
     *            The request.
     * @return {@code false} when validation error (and API result is set).
     */
    private boolean validateAddCopyJobRequestOpts(final Printer printer,
            final ProxyPrintInboxReq printReq) {

        final PrinterAttrLookup printerAttrLookup =
                new PrinterAttrLookup(printer);

        final IppMediaSourceCostDto mediaSourceCost =
                printerAttrLookup.get(new PrinterDao.MediaSourceAttr(
                        printReq.getMediaSourceOption()));

        if (mediaSourceCost == null || mediaSourceCost.getMedia() == null) {
            setApiResult(ApiResultCodeEnum.ERROR,
                    "msg-copyjob-media-source-missing");
            return false;
        }

        printReq.setMediaSourceOption(mediaSourceCost.getSource());
        printReq.setMediaOption(mediaSourceCost.getMedia().getMedia());

        printReq.setJobChunkInfo(ProxyPrintJobChunkInfo.createCopyJobChunk(
                mediaSourceCost, printReq.getNumberOfPages()));

        return true;
    }

    /**
     * Applies print delegation info into the print request.
     *
     * @param dtoReq
     *            The incoming request.
     * @param printReq
     *            The print request.
     * @return {@code true} when delegated print is applied.
     */
    private static boolean applyPrintDelegation(final DtoReq dtoReq,
            final ProxyPrintInboxReq printReq) {

        final boolean isDelegatedPrint = dtoReq.isDelegatedPrint();

        if (isDelegatedPrint) {

            if (!ConfigManager.instance()
                    .isConfigValue(Key.PROXY_PRINT_DELEGATE_ENABLE)) {
                throw new SpException("Delegated Print is disabled.");
            }

            final JsonPrintDelegation jsonDelegation =
                    JsonPrintDelegation.create(dtoReq.getDelegation());

            final AccountTrxInfoSet infoSet = PRINT_DELEGATION_SERVICE
                    .createAccountTrxInfoSet(jsonDelegation);

            printReq.setNumberOfCopies(infoSet.getUnitTotal());
            printReq.setAccountTrxInfoSet(infoSet);
        }
        return isDelegatedPrint;
    }

    /**
     * Applies charge to shared account into the print request.
     *
     * @param dtoReq
     *            The incoming request.
     * @param printReq
     *            The print request.
     * @return {@code true} when shared account is applied.
     */
    private static boolean applySharedAccount(final DtoReq dtoReq,
            final ProxyPrintInboxReq printReq) {

        if (dtoReq.getAccountId() == null || dtoReq.getAccountId()
                .equals(Print.OPTION_VALUE_SELECT_PERSONAL_ACCOUNT)) {
            return false;
        }

        final Account account = ACCOUNT_DAO.findById(dtoReq.getAccountId());

        if (account == null) {
            throw new IllegalStateException(String.format(
                    "Shared account %d not found", dtoReq.getAccountId()));
        }

        final AccountTrxInfoSet infoSet =
                new AccountTrxInfoSet(dtoReq.getCopies().intValue());
        printReq.setAccountTrxInfoSet(infoSet);

        final List<AccountTrxInfo> trxList = new ArrayList<>();
        infoSet.setAccountTrxInfoList(trxList);

        final AccountTrxInfo trx = new AccountTrxInfo();
        trxList.add(trx);

        trx.setWeight(dtoReq.getCopies());
        trx.setWeightUnit(Integer.valueOf(1));

        trx.setAccount(account);

        return true;
    }

    /**
     *
     * @param printReq
     * @param currencySymbol
     */
    private void onSecurePrint(final ProxyPrintInboxReq printReq,
            final String currencySymbol) {

        final String localizedCost;

        try {
            localizedCost = localizedPrinterCost(
                    printReq.getCostResult().getCostTotal(), null);
        } catch (ParseException e) {
            throw new SpException(e.getMessage());
        }

        final Map<String, Object> data = this.getUserData();

        if (StringUtils.isNotBlank(localizedCost)) {
            data.put("formattedCost", localizedCost);

            if (StringUtils.isNotBlank(localizedCost)) {
                data.put("currencySymbol", currencySymbol);
            }
        }
        /*
         * Signal NEEDS_AUTH
         */
        data.put("requestStatus", printReq.getStatus().toString());
        data.put("printAuthExpirySecs", ConfigManager.instance()
                .getConfigInt(Key.PROXY_PRINT_DIRECT_EXPIRY_SECS));

        setApiResultOk();
    }

    /**
     * Validates input for all cases.
     *
     * @param dtoReq
     *            The user request.
     * @return {@code true} when input is valid.
     */
    private boolean validateCommon(final DtoReq dtoReq) {

        final int copies = dtoReq.getCopies().intValue();

        //
        if (copies <= 0) {
            setApiResultText(ApiResultCodeEnum.ERROR,
                    "Invalid number of copies.");
            return false;
        }

        final Map<String, String> options = dtoReq.getOptions();

        if (options == null) {
            return true;
        }

        String optValWlk = null;

        optValWlk = options.get(
                IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_BOOKLET);

        if (copies > 1 && !StringUtils
                .defaultString(optValWlk,
                        IppKeyword.ORG_SAVAPAGE_ATTR_FINISHINGS_BOOKLET_NONE)
                .equals(IppKeyword.ORG_SAVAPAGE_ATTR_FINISHINGS_BOOKLET_NONE)) {

            if (BooleanUtils.isFalse(dtoReq.getCollate())) {
                setApiResult(ApiResultCodeEnum.WARN,
                        "msg-print-booklet-collate-mismatch");
                return false;
            }
        }

        return true;
    }

    /**
     * Validates input for Copy Job ticket.
     *
     * @param dtoReq
     *            The user request.
     * @return {@code true} when input is valid.
     */
    private boolean validateJobTicketCopy(final DtoReq dtoReq) {

        if (StringUtils.isBlank(dtoReq.getJobName())) {
            setApiResult(ApiResultCodeEnum.ERROR, "msg-copyjob-title-missing");
        } else if (dtoReq.getJobTicketCopyPages() == null
                || dtoReq.getJobTicketCopyPages().intValue() < 1) {
            setApiResult(ApiResultCodeEnum.ERROR, "msg-copyjob-pages-invalid");
        } else {
            return true;
        }
        return false;
    }

    /**
     * Validates proxy print filtering.
     *
     * @param dtoReq
     *            The user request.
     * @return {@code true} when input is valid.
     */
    private boolean validateProxyPrintFiltering(final DtoReq dtoReq) {
        /*
         * INVARIANT: Only one filter allowed for proxy print.
         */
        if (dtoReq.getRemoveGraphics() != null && dtoReq.getRemoveGraphics()
                && dtoReq.getEcoprint() != null && dtoReq.getEcoprint()) {
            setApiResult(ApiResultCodeEnum.INFO,
                    "msg-select-single-pdf-filter");
            return false;
        }
        return true;
    }

    /**
     * Validates expected PDF page orientation when punch or staple is
     * requested.
     *
     * @param dtoReq
     *            The user request.
     * @param chunkInfo
     *            The {@link ProxyPrintJobChunkInfo}.
     * @return {@code true} when input is valid.
     */
    private boolean validatePdfOrientation(final DtoReq dtoReq,
            final ProxyPrintJobChunkInfo chunkInfo) {

        if (dtoReq.getLandscapeView() == null || !ConfigManager.instance()
                .isConfigValue(Key.WEBAPP_NUMBER_UP_PREVIEW_ENABLE)) {
            return true;
        }

        final Map<String, String> options = dtoReq.getOptions();

        if (options == null) {
            return true;
        }

        final boolean isFinishing;

        String optValWlk = null;

        optValWlk = options.get(
                IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_STAPLE);

        if (StringUtils
                .defaultString(optValWlk,
                        IppKeyword.ORG_SAVAPAGE_ATTR_FINISHINGS_STAPLE_NONE)
                .equals(IppKeyword.ORG_SAVAPAGE_ATTR_FINISHINGS_STAPLE_NONE)) {

            optValWlk = options.get(
                    IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_PUNCH);

            isFinishing = !StringUtils
                    .defaultString(optValWlk,
                            IppKeyword.ORG_SAVAPAGE_ATTR_FINISHINGS_PUNCH_NONE)
                    .equals(IppKeyword.ORG_SAVAPAGE_ATTR_FINISHINGS_PUNCH_NONE);
        } else {
            isFinishing = true;
        }

        if (!isFinishing) {
            return true;
        }

        // Traverse print chunks
        for (final ProxyPrintJobChunk chunk : chunkInfo.getChunks()) {

            // Get the first range.
            final ProxyPrintJobChunkRange chunkRange = chunk.getRanges().get(0);

            final InboxInfoDto filteredInbox = chunkInfo.getFilteredInboxInfo();

            // Find job
            for (int i = 0; i < filteredInbox.getJobs().size(); i++) {

                if (i != chunkRange.getJob()) {
                    continue;
                }

                final InboxJob job = filteredInbox.getJobs().get(i);

                final File pdfFile = Paths
                        .get(ConfigManager.getUserHomeDir(dtoReq.getUser()),
                                job.getFile())
                        .toFile();

                final boolean expectedLandscape =
                        dtoReq.getLandscapeView().booleanValue();

                final int firstPage;
                if (chunkRange.pageBegin == null) {
                    firstPage = 1;
                } else {
                    firstPage = chunkRange.pageBegin.intValue();
                }

                final boolean seenLandscape;
                try {
                    seenLandscape = PdfPageRotateHelper.isSeenAsLandscape(
                            pdfFile, firstPage,
                            Integer.valueOf(job.getRotate()));
                } catch (IOException | NumberFormatException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }

                if (expectedLandscape == seenLandscape) {
                    continue;
                }

                final PrintOutNounEnum orientationAct;
                final PrintOutNounEnum orientationExp;
                if (seenLandscape) {
                    orientationAct = PrintOutNounEnum.LANDSCAPE;
                    orientationExp = PrintOutNounEnum.PORTRAIT;
                } else {
                    orientationAct = PrintOutNounEnum.PORTRAIT;
                    orientationExp = PrintOutNounEnum.LANDSCAPE;
                }

                setApiResult(ApiResultCodeEnum.WARN,
                        "msg-print-orientation-mismatch",
                        orientationExp.uiText(getLocale()),
                        orientationAct.uiText(getLocale()));
                return false;
            }
        }

        return true;
    }

    /**
     * Validates IPP choices constraint rules. When not valid,
     * {@link #setApiResultText(ApiResultCodeEnum, String)} is called with
     * {@link ApiResultCodeEnum#WARN}.
     *
     * @param proxyPrinter
     *            The proxy printer.
     * @param ippOptions
     *            The IPP attribute key/choices.
     * @param costResult
     *            The calculated cost result.
     * @param isJobTicket
     *            {@code true} when these are Job Ticket options.
     * @return {@code true} when choices are valid.
     */
    private boolean validateOptions(final JsonProxyPrinter proxyPrinter,
            final Map<String, String> ippOptions) {

        final String msg = PROXY_PRINT_SERVICE
                .validateContraintsMsg(proxyPrinter, ippOptions, getLocale());
        if (msg != null) {
            setApiResultText(ApiResultCodeEnum.WARN, msg);
            return false;
        }
        return true;
    }

    /**
     * Validates and prunes IPP Media choices according to the list of cost
     * rules. When not valid,
     * {@link #setApiResultText(ApiResultCodeEnum, String)} is called with
     * {@link ApiResultCodeEnum#WARN}.
     *
     * @param proxyPrinter
     *            The proxy printer.
     * @param ippOptions
     *            The IPP attribute key/choices.
     * @param costResult
     *            The calculated cost result.
     * @param isJobTicket
     *            {@code true} when these are Job Ticket options.
     * @return {@code true} when choices are valid.
     */
    private boolean validatePruneIppOptions(final JsonProxyPrinter proxyPrinter,
            final Map<String, String> ippOptions,
            final ProxyPrintCostDto costResult, final boolean isJobTicket) {

        final String msg = PROXY_PRINT_SERVICE
                .validateCustomCostRules(proxyPrinter, ippOptions, getLocale());

        if (msg != null) {
            setApiResultText(ApiResultCodeEnum.WARN, msg);
            return false;
        }

        /*
         * Prune irrelevant media-* options: i.e. all paper related media-*
         * options.
         */
        if (!isJobTicket && proxyPrinter.hasCustomCostRulesMedia()
                && !StringUtils
                        .defaultString(ippOptions
                                .get(IppDictJobTemplateAttr.ATTR_MEDIA_TYPE))
                        .equals(IppKeyword.MEDIA_TYPE_PAPER)) {

            final String[] attrToRemove =
                    IppDictJobTemplateAttr.JOBTICKET_ATTR_MEDIA_TYPE_PAPER;

            for (final String ippKey : attrToRemove) {
                ippOptions.remove(ippKey);
            }
        }

        return true;
    }

    /**
     * Gets the localized string for a BigDecimal.
     *
     * @param decimal
     *            The {@link BigDecimal}.
     * @param currencySymbol
     *            {@code null} when not available.
     * @return The localized string.
     * @throws ParseException
     *             When decimal parsing fails.
     */
    private String localizedPrinterCost(final BigDecimal decimal,
            final String currencySymbol) throws ParseException {

        BigDecimal value = decimal;

        if (value == null) {
            value = BigDecimal.ZERO;
        }

        String cost = BigDecimalUtil.localize(value,
                ConfigManager.getPrinterCostDecimals(), this.getLocale(), true);

        if (StringUtils.isBlank(currencySymbol)) {
            return cost;
        }
        return currencySymbol + cost;
    }

    /**
     * Calculates the Job Ticket delivery date.
     *
     * @param dtoReq
     *            The {@link DtoReq}.
     * @return The calculated Job Ticket delivery date.
     */
    private Date calcJobTicketDeliveryDate(final DtoReq dtoReq) {

        final ConfigManager cm = ConfigManager.instance();

        final boolean isJobTicketDateTime =
                cm.isConfigValue(Key.JOBTICKET_DELIVERY_DATETIME_ENABLE);

        Date deliveryDate;

        if (dtoReq.getJobTicketDate() == null) {
            if (isJobTicketDateTime) {
                deliveryDate = new Date();
            } else {
                deliveryDate = null;
            }
        } else {
            deliveryDate = new Date(dtoReq.getJobTicketDate().longValue());
        }

        if (isJobTicketDateTime) {

            int minutes = 0;

            if (dtoReq.getJobTicketHrs() != null) {
                minutes += dtoReq.getJobTicketHrs().intValue()
                        * DateUtil.MINUTES_IN_HOUR;
            }
            if (dtoReq.getJobTicketMin() != null) {
                minutes += dtoReq.getJobTicketMin().intValue();
            }
            if (minutes == 0) {
                minutes =
                        cm.getConfigInt(Key.JOBTICKET_DELIVERY_DAY_MINUTES, 0);
            }

            deliveryDate = DateUtils.addMinutes(
                    DateUtils.truncate(deliveryDate, Calendar.DAY_OF_MONTH),
                    minutes);
        }
        return deliveryDate;
    }

    /**
     * Handles a Job Ticket of type {@link JobTicketTypeEnum#COPY}.
     *
     * @param lockedUser
     *            The locked {@link User} instance.
     * @param dtoReq
     *            The {@link DtoReq}.
     * @param printReq
     *            The print request.
     * @param currencySymbol
     *            The currency symbol.
     */
    private void onJobTicketCopy(final User lockedUser, final DtoReq dtoReq,
            final ProxyPrintInboxReq printReq, final String currencySymbol) {

        printReq.setPrintMode(PrintModeEnum.TICKET_C);
        printReq.setComment(dtoReq.getJobTicketRemark());

        JOBTICKET_SERVICE.createCopyJob(lockedUser, printReq,
                calcJobTicketDeliveryDate(dtoReq), dtoReq.getJobTicketTag());

        setApiResultMsg(dtoReq, printReq);

        ApiRequestHelper.addUserStats(this.getUserData(), lockedUser,
                this.getLocale(), currencySymbol);
    }

    /**
     * Handles a Job Ticket of type {@link JobTicketTypeEnum#PRINT}.
     *
     * @param lockedUser
     *            The locked {@link User} instance.
     * @param dtoReq
     *            The {@link DtoReq}.
     * @param printReq
     *            The print request.
     * @param currencySymbol
     *            The currency symbol.
     */
    private void onJobTicketPrint(final User lockedUser, final DtoReq dtoReq,
            final ProxyPrintInboxReq printReq, final String currencySymbol) {

        printReq.setPrintMode(PrintModeEnum.TICKET);
        printReq.setComment(dtoReq.getJobTicketRemark());

        final Date deliveryDate = calcJobTicketDeliveryDate(dtoReq);

        try {
            JOBTICKET_SERVICE.proxyPrintInbox(lockedUser, printReq,
                    deliveryDate, dtoReq.getJobTicketTag());

        } catch (EcoPrintPdfTaskPendingException e) {
            setApiResult(ApiResultCodeEnum.INFO, "msg-ecoprint-pending");
            return;
        }

        /*
         * Since the job is preserved in the outbox we clear the inbox.
         */
        printReq.setClearedObjects(
                PROXY_PRINT_SERVICE.clearInbox(lockedUser, printReq));

        setApiResultMsg(dtoReq, printReq);

        ApiRequestHelper.addUserStats(this.getUserData(), lockedUser,
                this.getLocale(), currencySymbol);
    }

    /**
     *
     * @param lockedUser
     *            The locked {@link User} instance, can be {@code null}.
     * @param dtoReq
     *            The {@link DtoReq}.
     * @param printReq
     *            The print request.
     * @param currencySymbol
     *            The currency symbol.
     */
    private void onHoldPrint(final User lockedUser, final DtoReq dtoReq,
            final ProxyPrintInboxReq printReq, final String currencySymbol) {

        printReq.setPrintMode(PrintModeEnum.HOLD);

        try {
            OUTBOX_SERVICE.proxyPrintInbox(lockedUser, printReq);
        } catch (EcoPrintPdfTaskPendingException e) {
            setApiResult(ApiResultCodeEnum.INFO, "msg-ecoprint-pending");
            return;
        }

        /*
         * Since the job is preserved in the outbox we clear the inbox.
         */
        printReq.setClearedObjects(
                PROXY_PRINT_SERVICE.clearInbox(lockedUser, printReq));

        setApiResultMsg(dtoReq, printReq);
        ApiRequestHelper.addUserStats(this.getUserData(), lockedUser,
                this.getLocale(), currencySymbol);
    }

    /**
     *
     * @param lockedUser
     *            The locked {@link User} instance, can be {@code null}.
     * @param dtoReq
     *            The {@link DtoReq}.
     * @param printReq
     *            The print request.
     * @param currencySymbol
     *            The currency symbol.
     * @throws IppConnectException
     */
    private void onNonSecurePrint(final User lockedUser, final DtoReq dtoReq,
            final ProxyPrintInboxReq printReq, final String currencySymbol)
            throws IppConnectException {

        printReq.setPrintMode(PrintModeEnum.PUSH);

        try {
            PROXY_PRINT_SERVICE.proxyPrintInbox(lockedUser, printReq);
        } catch (EcoPrintPdfTaskPendingException e) {
            setApiResult(ApiResultCodeEnum.INFO, "msg-ecoprint-pending");
            return;
        }

        setApiResultMsg(dtoReq, printReq);

        if (printReq.getStatus() == ProxyPrintInboxReq.Status.PRINTED) {

            ApiRequestHelper.addUserStats(this.getUserData(), lockedUser,
                    this.getLocale(), currencySymbol);
        }
    }

    /**
     * Proxy Prints to a PaperCut managed printer.
     * <p>
     * The PaperCut status is monitored by {@link PaperCutPrintMonitorJob}.
     * </p>
     *
     * @param lockedUser
     *            The locked {@link User} instance, can be {@code null}.
     * @param dtoReq
     *            The {@link DtoReq}.
     * @param printReq
     * @param currencySymbol
     * @throws IppConnectException
     */
    private void onExtPaperCutPrint(final User lockedUser, final DtoReq dtoReq,
            final ProxyPrintInboxReq printReq, final String currencySymbol)
            throws IppConnectException {

        PAPERCUT_SERVICE.prepareForExtPaperCut(printReq, null, null);

        try {
            PROXY_PRINT_SERVICE.proxyPrintInbox(lockedUser, printReq);

        } catch (EcoPrintPdfTaskPendingException e) {
            setApiResult(ApiResultCodeEnum.INFO, "msg-ecoprint-pending");
            return;
        }

        setApiResultMsg(dtoReq, printReq);

        if (printReq.getStatus() == ProxyPrintInboxReq.Status.PRINTED) {

            ApiRequestHelper.addUserStats(this.getUserData(), lockedUser,
                    this.getLocale(), currencySymbol);
        }
    }

    /**
     * Sets the JSON {@code result} and {@code requestStatus} of a Proxy Print
     * Request on parameter {@code out}.
     *
     * @param out
     *            The Map to put the {@code result} on.
     * @param printReq
     *            The Proxy Print Request.
     * @return the {@code out} parameter.
     */
    public static Map<String, Object> setApiResultMsg(
            final Map<String, Object> out, final ProxyPrintInboxReq printReq) {

        final ApiResultCodeEnum code;
        switch (printReq.getStatus()) {
        case ERROR_PRINTER_NOT_FOUND:
            code = ApiResultCodeEnum.ERROR;
            break;
        case PRINTED:
            code = ApiResultCodeEnum.OK;
            break;
        case WAITING_FOR_RELEASE:
            code = ApiResultCodeEnum.OK;
            break;
        default:
            code = ApiResultCodeEnum.WARN;
        }

        out.put("requestStatus", printReq.getStatus().toString());

        return createApiResult(out, code, printReq.getUserMsgKey(),
                printReq.getUserMsg());
    }

    /**
     * Sets the JSON {@code result}, {@code clearDelegate} indicator, and
     * {@code requestStatus} of a Proxy Print Request on parameter {@code out}.
     *
     * @param dtoReq
     *            The incoming request.
     * @param printReq
     *            The Proxy Print Request.
     */
    private void setApiResultMsg(final DtoReq dtoReq,
            final ProxyPrintInboxReq printReq) {

        setApiResultMsg(this.getUserData(), printReq);

        this.getUserData().put("clearDelegate",
                dtoReq.isDelegatedPrint()
                        && ConfigManager.instance().isConfigValue(
                                Key.WEBAPP_USER_PROXY_PRINT_CLEAR_DELEGATE));
    }

    /**
     * Checks if users exist in PaperCut.
     *
     * @param requestingUser
     *            The ID of the requesting user.
     * @param infoSet
     *            The {@link AccountTrxInfoSet} containing the user account
     *            information.
     * @return A list with IDs of users not found.
     */
    private List<String> checkPaperCutUsers(final String requestingUser,
            final AccountTrxInfoSet infoSet) {

        final List<String> usersNotFound = new ArrayList<>();

        final PaperCutServerProxy serverProxy =
                PaperCutServerProxy.create(ConfigManager.instance(), false);

        if (PAPERCUT_SERVICE.findUser(serverProxy, requestingUser) == null) {
            usersNotFound.add(requestingUser);
        }

        if (infoSet != null) {
            for (final AccountTrxInfo info : infoSet.getAccountTrxInfoList()) {
                final AccountTypeEnum accountType =
                        EnumUtils.getEnum(AccountTypeEnum.class,
                                info.getAccount().getAccountType());

                if (accountType == AccountTypeEnum.GROUP
                        || accountType == AccountTypeEnum.SHARED) {
                    continue;
                }

                final String userId = info.getAccount().getNameLower();

                if (PAPERCUT_SERVICE.findUser(serverProxy, userId) == null) {
                    usersNotFound.add(userId);
                }
            }
        }
        return usersNotFound;
    }

}
