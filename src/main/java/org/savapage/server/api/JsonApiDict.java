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
package org.savapage.server.api;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.savapage.core.SpException;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.server.api.request.ApiRequestHandler;
import org.savapage.server.api.request.ReqDbBackup;
import org.savapage.server.api.request.ReqGenerateUuid;
import org.savapage.server.api.request.ReqJobTicketDelete;
import org.savapage.server.api.request.ReqJobTicketPrint;
import org.savapage.server.api.request.ReqLogin;
import org.savapage.server.api.request.ReqOutboxClear;
import org.savapage.server.api.request.ReqOutboxDeleteJob;
import org.savapage.server.api.request.ReqOutboxExtend;
import org.savapage.server.api.request.ReqPosDepositQuickSearch;
import org.savapage.server.api.request.ReqPrintDelegationSet;
import org.savapage.server.api.request.ReqPrinterPrint;
import org.savapage.server.api.request.ReqPrinterQuickSearch;
import org.savapage.server.api.request.ReqQueueEnable;
import org.savapage.server.api.request.ReqQueueGet;
import org.savapage.server.api.request.ReqSharedAccountGet;
import org.savapage.server.api.request.ReqSharedAccountQuickSearch;
import org.savapage.server.api.request.ReqSharedAccountSet;
import org.savapage.server.api.request.ReqUserGroupGet;
import org.savapage.server.api.request.ReqUserGroupMemberQuickSearch;
import org.savapage.server.api.request.ReqUserGroupQuickSearch;
import org.savapage.server.api.request.ReqUserGroupSet;
import org.savapage.server.api.request.ReqUserGroupsAddRemove;
import org.savapage.server.api.request.ReqUserNotifyAccountChange;
import org.savapage.server.api.request.ReqUserQuickSearch;
import org.savapage.server.webapp.WebAppTypeEnum;

/**
 * A dedicated class for initializing the JSON API dictionary at the right time.
 *
 * @author Rijk Ravestein
 *
 */
public class JsonApiDict {

    public static final String PARM_WEBAPP_TYPE = "webAppType";
    public static final String PARM_REQ = "request";
    public static final String PARM_USER = "user";
    public static final String PARM_REQ_SUB = "request-sub";
    public static final String PARM_DATA = "data";

    public static final String REQ_ACCOUNT_VOUCHER_BATCH_CREATE =
            "account-voucher-batch-create";
    public static final String REQ_ACCOUNT_VOUCHER_BATCH_DELETE =
            "account-voucher-batch-delete";
    public static final String REQ_ACCOUNT_VOUCHER_BATCH_EXPIRE =
            "account-voucher-batch-expire";
    public static final String REQ_ACCOUNT_VOUCHER_BATCH_PRINT =
            "account-voucher-batch-print";
    public static final String REQ_ACCOUNT_VOUCHER_DELETE_EXPIRED =
            "account-voucher-delete-expired";
    public static final String REQ_ACCOUNT_VOUCHER_REDEEM =
            "account-voucher-redeem";

    public static final String REQ_CARD_IS_REGISTERED = "card-is-registered";
    public static final String REQ_CONFIG_GET_PROP = "config-get-prop";
    public static final String REQ_CONFIG_SET_PROPS = "config-set-props";
    public static final String REQ_CONSTANTS = "constants";
    public static final String REQ_DB_BACKUP = "db-backup";
    public static final String REQ_DEVICE_DELETE = "device-delete";

    public static final String REQ_DEVICE_NEW_CARD_READER =
            "device-new-card-reader";
    public static final String REQ_DEVICE_NEW_TERMINAL = "device-new-terminal";

    public static final String REQ_DEVICE_GET = "device-get";
    public static final String REQ_DEVICE_SET = "device-set";
    public static final String REQ_EXIT_EVENT_MONITOR = "exit-event-monitor";
    public static final String REQ_GCP_GET_DETAILS = "gcp-get-details";
    public static final String REQ_GCP_ONLINE = "gcp-online";
    public static final String REQ_GCP_REGISTER = "gcp-register";
    public static final String REQ_GCP_SET_DETAILS = "gcp-set-details";
    public static final String REQ_GCP_SET_NOTIFICATIONS =
            "gcp-set-notifications";
    public static final String REQ_GET_EVENT = "get-event";

    public static final String REQ_IMAP_TEST = "imap-test";
    public static final String REQ_IMAP_START = "imap-start";
    public static final String REQ_IMAP_STOP = "imap-stop";

    public static final String REQ_JOB_DELETE = "job-delete";
    public static final String REQ_JOB_EDIT = "job-edit";
    public static final String REQ_JOB_PAGES = "job-pages";
    public static final String REQ_INBOX_IS_VANILLA = "inbox-is-vanilla";
    public static final String REQ_JQPLOT = "jqplot";
    public static final String REQ_LANGUAGE = "language";

    public static final String REQ_JOBTICKET_DELETE = "jobticket-delete";
    public static final String REQ_JOBTICKET_PRINT = "jobticket-print";

    public static final String REQ_PAPERCUT_TEST = "papercut-test";

    public static final String REQ_SMARTSCHOOL_TEST = "smartschool-test";
    public static final String REQ_SMARTSCHOOL_START = "smartschool-start";
    public static final String REQ_SMARTSCHOOL_START_SIMULATE =
            "smartschool-start-simulate";
    public static final String REQ_SMARTSCHOOL_STOP = "smartschool-stop";

    public static final String REQ_SMARTSCHOOL_PAPERCUT_STUDENT_COST_CSV =
            "smartschool-papercut-student-cost-csv";

    public static final String REQ_LETTERHEAD_ATTACH = "letterhead-attach";
    public static final String REQ_LETTERHEAD_DELETE = "letterhead-delete";
    public static final String REQ_LETTERHEAD_DETACH = "letterhead-detach";
    public static final String REQ_LETTERHEAD_GET = "letterhead-get";
    public static final String REQ_LETTERHEAD_LIST = "letterhead-list";
    public static final String REQ_LETTERHEAD_NEW = "letterhead-new";
    public static final String REQ_LETTERHEAD_SET = "letterhead-set";

    public static final String REQ_LOGIN = "login";

    public static final String REQ_LOGOUT = "logout";

    public static final String REQ_WEBAPP_UNLOAD = "webapp-unload";
    public static final String REQ_WEBAPP_CLOSE_SESSION =
            "webapp-close-session";

    public static final String REQ_MAIL_TEST = "mail-test";
    public static final String REQ_PAGOMETER_RESET = "pagometer-reset";

    public static final String REQ_OUTBOX_CLEAR = "outbox-clear";
    public static final String REQ_OUTBOX_DELETE_JOB = "outbox-delete-job";
    public static final String REQ_OUTBOX_EXTEND = "outbox-extend";

    public static final String REQ_PAGE_DELETE = "page-delete";
    public static final String REQ_PAGE_MOVE = "page-move";

    public static final String REQ_PAYMENT_GATEWAY_ONLINE =
            "payment-gateway-online";

    public static final String REQ_PDF = "pdf";
    public static final String REQ_PDF_GET_PROPERTIES = "pdf-get-properties";
    public static final String REQ_PDF_SET_PROPERTIES = "pdf-set-properties";

    public static final String REQ_PING = "ping";

    public static final String REQ_USER_CREDIT_TRANSFER =
            "user-credit-transfer";
    public static final String REQ_USER_MONEY_TRANSFER_REQUEST =
            "user-money-transfer-request";

    public static final String REQ_BITCOIN_WALLET_REFRESH =
            "bitcoin-wallet-refresh";

    public static final String REQ_POS_DEPOSIT = "pos-deposit";
    public static final String REQ_POS_RECEIPT_DOWNLOAD =
            "pos-receipt-download";
    public static final String REQ_POS_RECEIPT_DOWNLOAD_USER =
            "pos-receipt-download-user";

    /**
     * An administrator sending email to a POS Receipt.
     */
    public static final String REQ_POS_RECEIPT_SENDMAIL =
            "pos-receipt-sendmail";

    public static final String REQ_POS_DEPOSIT_QUICK_SEARCH =
            "pos-deposit-quick-search";

    public static final String REQ_PRINT_AUTH_CANCEL = "print-auth-cancel";
    public static final String REQ_PRINT_FAST_RENEW = "print-fast-renew";

    public static final String REQ_PRINT_DELEGATION_SET =
            "print-delegation-set";

    public static final String REQ_PRINTER_OPT_DOWNLOAD =
            "printer-opt-download";

    public static final String REQ_PRINTER_DETAIL = "printer-detail";
    public static final String REQ_PRINTER_GET = "printer-get";
    public static final String REQ_PRINTER_PRINT = "printer-print";
    public static final String REQ_PRINTER_RENAME = "printer-rename";
    public static final String REQ_PRINTER_QUICK_SEARCH =
            "printer-quick-search";

    public static final String REQ_PRINTER_SET = "printer-set";
    public static final String REQ_PRINTER_SET_MEDIA_COST =
            "printer-set-media-cost";
    public static final String REQ_PRINTER_SET_MEDIA_SOURCES =
            "printer-set-media-sources";

    public static final String REQ_PRINTER_SYNC = "printer-sync";

    public static final String REQ_QUEUE_ENABLE = "queue-enable";
    public static final String REQ_QUEUE_GET = "queue-get";
    public static final String REQ_QUEUE_SET = "queue-set";

    public static final String REQ_SHARED_ACCOUNT_GET = "shared-account-get";
    public static final String REQ_SHARED_ACCOUNT_SET = "shared-account-set";
    public static final String REQ_SHARED_ACCOUNT_QUICK_SEARCH =
            "shared-account-quick-search";

    public static final String REQ_REPORT = "report";
    public static final String REQ_REPORT_USER = "report-user";

    public static final String REQ_RESET_JMX_PASSWORD = "reset-jmx-pw";
    public static final String REQ_RESET_ADMIN_PASSWORD = "reset-admin-pw";
    public static final String REQ_RESET_USER_PASSWORD = "reset-user-pw";
    public static final String REQ_RESET_USER_PIN = "reset-user-pin";
    public static final String REQ_SEND = "send";
    public static final String REQ_USER_DELETE = "user-delete";
    public static final String REQ_USER_GET = "user-get";
    public static final String REQ_USER_GET_STATS = "user-get-stats";
    public static final String REQ_USER_LAZY_ECOPRINT = "user-lazy-ecoprint";
    public static final String REQ_USER_NOTIFY_ACCOUNT_CHANGE =
            "user-notify-account-change";
    public static final String REQ_USER_QUICK_SEARCH = "user-quick-search";
    public static final String REQ_USER_SET = "user-set";
    public static final String REQ_USER_SOURCE_GROUPS = "user-source-groups";
    public static final String REQ_USER_SYNC = "user-sync";

    public static final String REQ_USERGROUPS_ADD_REMOVE =
            "usergroups-add-remove";

    public static final String REQ_USERGROUP_GET = "usergroup-get";

    public static final String REQ_USERGROUP_SET = "usergroup-set";

    public static final String REQ_USERGROUP_QUICK_SEARCH =
            "usergroup-quick-search";

    public static final String REQ_USERGROUP_MEMBER_QUICK_SEARCH =
            "usergroup-member-quick-search";

    public static final String REQ_GENERATE_UUID = "generate-uuid";

    /**
     */
    public enum DbClaim {
        /**
         * NO {@link ConfigManager#dbClaim()} or
         * {@link ConfigManager#dbClaimExclusive()} is needed.
         */
        NONE,

        /**
         * {@link ConfigManager#dbClaim()} is needed.
         */
        READ,

        /**
         * {@link ConfigManager#dbClaimExclusive()} is needed.
         */
        EXCLUSIVE
    }

    /**
     *
     */
    public enum LetterheadLock {
        NONE, READ, UPDATE
    }

    /**
     *
     */
    public enum AuthReq {
        /**
         * No authentication required.
         */
        NONE,

        /**
         * User authentication required.
         */
        USER,

        /**
         * Admin authentication required.
         */
        ADMIN
    }

    /**
     *
     */
    public enum DbAccess {
        NO, YES, USER_LOCK
    }

    /**
     *
     */
    private class Req {

        final AuthReq authReq;
        final DbAccess dbAccess;
        final DbClaim dbClaim;
        final Class<? extends ApiRequestHandler> handler;
        final EnumSet<ACLRoleEnum> aclRolesRequired;

        /**
         * @deprecated
         * @param authReq
         * @param dbClaim
         * @param dbAccess
         */
        @Deprecated
        private Req(final AuthReq authReq, final DbClaim dbClaim,
                final DbAccess dbAccess) {
            this.dbAccess = dbAccess;
            this.dbClaim = dbClaim;
            this.authReq = authReq;
            this.handler = null;
            this.aclRolesRequired = null;
        }

        private Req(final AuthReq authReq,
                final Class<? extends ApiRequestHandler> handler,
                final DbClaim dbClaim, final DbAccess dbAccess) {
            this.dbAccess = dbAccess;
            this.dbClaim = dbClaim;
            this.authReq = authReq;
            this.handler = handler;
            this.aclRolesRequired = null;
        }

        private Req(final AuthReq authReq,
                final Class<? extends ApiRequestHandler> handler,
                final DbClaim dbClaim, final DbAccess dbAccess,
                final EnumSet<ACLRoleEnum> aclRolesRequired) {
            this.dbAccess = dbAccess;
            this.dbClaim = dbClaim;
            this.authReq = authReq;
            this.handler = handler;
            this.aclRolesRequired = aclRolesRequired;
        }

    };

    /**
     *
     */
    private final Map<String, Req> dict = new HashMap<>();

    /**
     * @deprecated
     * @param key
     * @param dbClaim
     * @param dbUsage
     * @param authReq
     */
    @Deprecated
    private void put(final String key, AuthReq authReq, DbClaim dbClaim,
            DbAccess dbUsage) {
        dict.put(key, new Req(authReq, dbClaim, dbUsage));
    }

    /**
     *
     * @param key
     * @param dbClaim
     * @param handler
     * @param dbUsage
     * @param authReq
     */
    private void put(final String key,
            final Class<? extends ApiRequestHandler> handler, AuthReq authReq,
            DbClaim dbClaim, DbAccess dbUsage) {
        dict.put(key, new Req(authReq, handler, dbClaim, dbUsage));
    }

    /**
     * @deprecated Puts a user's {@link Req} in the dictionary.
     *
     * @param key
     * @param dbClaim
     * @param dbAccess
     */
    @Deprecated
    private void usr(final String key, DbClaim dbClaim, DbAccess dbAccess) {
        dict.put(key, new Req(AuthReq.USER, dbClaim, dbAccess));
    }

    /**
     * Puts a user's {@link Req} in the dictionary.
     *
     * @param key
     * @param handler
     * @param dbClaim
     * @param dbAccess
     */
    private void usr(final String key,
            final Class<? extends ApiRequestHandler> handler, DbClaim dbClaim,
            DbAccess dbAccess) {
        dict.put(key, new Req(AuthReq.USER, handler, dbClaim, dbAccess));
    }

    /**
     * Puts a user's {@link Req} in the dictionary.
     *
     * @param key
     * @param handler
     * @param dbClaim
     * @param dbAccess
     */
    private void acl(final String key,
            final Class<? extends ApiRequestHandler> handler, DbClaim dbClaim,
            DbAccess dbAccess, final EnumSet<ACLRoleEnum> aclRolesRequired) {
        dict.put(key, new Req(AuthReq.USER, handler, dbClaim, dbAccess,
                aclRolesRequired));
    }

    /**
     * @deprecated
     * @param key
     * @param dbClaim
     * @param dbAccess
     * @param aclRolesRequired
     */
    @Deprecated
    private void acl(final String key, DbClaim dbClaim, DbAccess dbAccess,
            final EnumSet<ACLRoleEnum> aclRolesRequired) {
        dict.put(key, new Req(AuthReq.USER, null, dbClaim, dbAccess,
                aclRolesRequired));
    }

    /**
     * @deprecated Puts a administrator's {@link Req} in the dictionary.
     *
     * @param key
     * @param dbClaim
     * @param dbAccess
     */
    @Deprecated
    private void adm(final String key, DbClaim dbClaim, DbAccess dbAccess) {
        dict.put(key, new Req(AuthReq.ADMIN, dbClaim, dbAccess));
    }

    /**
     *
     * @param key
     * @param handler
     * @param dbClaim
     * @param dbAccess
     */
    private void adm(final String key,
            final Class<? extends ApiRequestHandler> handler, DbClaim dbClaim,
            DbAccess dbAccess) {
        dict.put(key, new Req(AuthReq.ADMIN, handler, dbClaim, dbAccess));
    }

    private void non(final String key) {
        dict.put(key, new Req(AuthReq.NONE, DbClaim.NONE, DbAccess.NO));
    }

    private void non(final String key,
            final Class<? extends ApiRequestHandler> handler) {
        dict.put(key,
                new Req(AuthReq.NONE, handler, DbClaim.NONE, DbAccess.NO));
    }

    /**
     * Checks which DbClaim the request needs.
     *
     * @param request
     *            The id string of the request.
     * @return The database claim needed.
     */
    public DbClaim getDbClaimNeeded(final String request) {
        return dict.get(request).dbClaim;
    }

    public void lock(final LetterheadLock lock) {
        if (lock == JsonApiDict.LetterheadLock.READ) {
            ReadWriteLockEnum.LETTERHEAD_STORE.setReadLock(true);
        } else if (lock == JsonApiDict.LetterheadLock.UPDATE) {
            ReadWriteLockEnum.LETTERHEAD_STORE.setWriteLock(true);
        }
    }

    /**
     * @param claim
     */
    public void lock(final DbClaim claim) {
        if (claim == JsonApiDict.DbClaim.READ) {
            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);
        } else if (claim == JsonApiDict.DbClaim.EXCLUSIVE) {
            ReadWriteLockEnum.DATABASE_READONLY.setWriteLock(true);
        }
    }

    public void unlock(final LetterheadLock lock) {
        if (lock == JsonApiDict.LetterheadLock.READ) {
            ReadWriteLockEnum.LETTERHEAD_STORE.setReadLock(false);
        } else if (lock == JsonApiDict.LetterheadLock.UPDATE) {
            ReadWriteLockEnum.LETTERHEAD_STORE.setWriteLock(false);
        }
    }

    /**
     * @param claim
     */
    public void unlock(final DbClaim claim) {
        if (claim == JsonApiDict.DbClaim.READ) {
            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
        } else if (claim == JsonApiDict.DbClaim.EXCLUSIVE) {
            ReadWriteLockEnum.DATABASE_READONLY.setWriteLock(false);
        }
    }

    /**
     * Checks which Public Letterhead lock is needed for a request.
     * <p>
     * For convenience, we are conservative and assume that request that use
     * letterheads use <b>public</b> letterheads.
     * </p>
     * <p>
     * If a user is an administrator this rules out some possibilities too.
     * </p>
     *
     * @param request
     *            The id string of the request.
     * @param isAdmin
     *            {@code true} if e requesting is an administrator.
     * @return The {@link LetterheadLock} needed.
     */
    public LetterheadLock getLetterheadLockNeeded(final String request,
            final boolean isAdmin) {

        /*
         * @param isPublicContext {@code true} if the request involves a public
         * letterhead.
         */
        boolean isPublicContext = true;

        switch (request) {

        case REQ_LETTERHEAD_ATTACH:
        case REQ_LETTERHEAD_LIST:
        case REQ_LETTERHEAD_GET:
            return LetterheadLock.READ;

        case REQ_LETTERHEAD_DELETE:
        case REQ_LETTERHEAD_SET:
            if (!isAdmin) {
                return LetterheadLock.NONE;
            } else if (isPublicContext) {
                return LetterheadLock.UPDATE;
            }
            return LetterheadLock.NONE;

        case REQ_PDF:
        case REQ_PRINTER_PRINT:
        case REQ_SEND:
            if (isPublicContext) {
                return LetterheadLock.READ;
            }
            return LetterheadLock.NONE;

        case REQ_LETTERHEAD_DETACH:
        case REQ_LETTERHEAD_NEW:
        default:
            return LetterheadLock.NONE;
        }

    }

    /**
     * Is request about downloading a file?
     *
     * @param request
     * @return
     */
    public static boolean isDownloadRequest(final String request) {

        switch (request) {

        case REQ_PDF:
        case REQ_REPORT:
        case REQ_REPORT_USER:
        case REQ_ACCOUNT_VOUCHER_BATCH_PRINT:
        case REQ_POS_RECEIPT_DOWNLOAD:
        case REQ_POS_RECEIPT_DOWNLOAD_USER:
        case REQ_PRINTER_OPT_DOWNLOAD:
        case REQ_SMARTSCHOOL_PAPERCUT_STUDENT_COST_CSV:
            return true;
        default:
            return false;
        }
    }

    /**
     * Checks if the <b>requesting</b> user needs to be locked because of access
     * to database or user file system (safe-pages).
     *
     * @param request
     *            The id string of the request.
     * @return {@code true} if requesting user needs to be locked for this
     *         request.
     */
    public boolean isUserLockNeeded(final String request) {
        return dict.get(request).dbAccess == DbAccess.USER_LOCK;
    }

    /**
     *
     * @param request
     * @return
     */
    public boolean isDbAccessNeeded(final String request) {
        return dict.get(request).dbAccess != DbAccess.NO;
    }

    /**
     * Checks if request is valid.
     *
     * @param request
     *            The request.
     * @return {@code false} is the request is invalid (unknown).
     */
    public boolean isValidRequest(final String request) {
        return dict.get(request) != null;
    }

    /**
     * Checks if the request needs an authenticated user.
     *
     * @param request
     *            The id string of the request.
     * @return {@code true} if the request needs user (or admin) authorization.
     */
    public boolean isAuthenticationNeeded(final String request) {
        return dict.get(request).authReq != AuthReq.NONE;
    }

    /**
     * Checks if the request needs an authenticated administrator.
     *
     * @param request
     *            The id string of the request.
     * @return {@code true} if the request needs an authenticated administrator.
     */
    public boolean isAdminAuthenticationNeeded(final String request) {
        return dict.get(request).authReq == AuthReq.ADMIN;
    }

    /**
     * Checks if a Web App is authorized to execute the request.
     *
     * @param request
     *            The id string of the request.
     * @param webAppType
     *            The {@link WebAppTypeEnum}.
     * @return {@code true} when Web App is authorized.
     */
    public boolean isWebAppAuthorized(final String request,
            final WebAppTypeEnum webAppType) {

        final Req req = dict.get(request);

        if (req.aclRolesRequired == null) {
            return true;
        }

        final Iterator<ACLRoleEnum> iter = req.aclRolesRequired.iterator();

        boolean allowed = false;

        while (!allowed && iter.hasNext()) {

            final ACLRoleEnum role = iter.next();

            switch (role) {
            case JOB_TICKET_CREATOR:
                allowed = webAppType == WebAppTypeEnum.USER;
                break;
            case JOB_TICKET_OPERATOR:
                allowed = EnumSet
                        .of(WebAppTypeEnum.ADMIN, WebAppTypeEnum.JOB_TICKETS)
                        .contains(webAppType);
                break;
            case PRINT_DELEGATE:
                allowed = webAppType == WebAppTypeEnum.USER;
                break;
            case PRINT_DELEGATOR:
                allowed = webAppType == WebAppTypeEnum.USER;
                break;
            case WEB_CASHIER:
                allowed = EnumSet.of(WebAppTypeEnum.ADMIN, WebAppTypeEnum.POS)
                        .contains(webAppType);
                break;
            default:
                throw new IllegalArgumentException(String.format(
                        "%s.%s is not supported",
                        role.getClass().getSimpleName(), role.toString()));
            }
        }
        return allowed;
    }

    /**
     *
     */
    public JsonApiDict() {

        adm(REQ_ACCOUNT_VOUCHER_BATCH_CREATE, DbClaim.READ, DbAccess.YES);
        adm(REQ_ACCOUNT_VOUCHER_BATCH_DELETE, DbClaim.READ, DbAccess.YES);
        adm(REQ_ACCOUNT_VOUCHER_BATCH_EXPIRE, DbClaim.READ, DbAccess.YES);
        adm(REQ_ACCOUNT_VOUCHER_BATCH_PRINT, DbClaim.READ, DbAccess.YES);
        adm(REQ_ACCOUNT_VOUCHER_DELETE_EXPIRED, DbClaim.READ, DbAccess.YES);

        usr(REQ_ACCOUNT_VOUCHER_REDEEM, DbClaim.READ, DbAccess.USER_LOCK);

        put(REQ_CARD_IS_REGISTERED, AuthReq.NONE, DbClaim.READ, DbAccess.YES);
        adm(REQ_CONFIG_GET_PROP, DbClaim.READ, DbAccess.YES);
        adm(REQ_CONFIG_SET_PROPS, DbClaim.READ, DbAccess.YES);
        put(REQ_CONSTANTS, AuthReq.NONE, DbClaim.READ, DbAccess.YES);

        adm(REQ_DB_BACKUP, ReqDbBackup.class, DbClaim.NONE, DbAccess.NO);

        adm(REQ_DEVICE_DELETE, DbClaim.READ, DbAccess.YES);

        adm(REQ_DEVICE_NEW_CARD_READER, DbClaim.NONE, DbAccess.NO);
        adm(REQ_DEVICE_NEW_TERMINAL, DbClaim.NONE, DbAccess.NO);

        adm(REQ_DEVICE_GET, DbClaim.NONE, DbAccess.YES);
        adm(REQ_DEVICE_SET, DbClaim.READ, DbAccess.YES);

        usr(REQ_EXIT_EVENT_MONITOR, DbClaim.NONE, DbAccess.NO);
        adm(REQ_GCP_GET_DETAILS, DbClaim.READ, DbAccess.YES);
        adm(REQ_GCP_ONLINE, DbClaim.READ, DbAccess.YES);
        adm(REQ_GCP_REGISTER, DbClaim.READ, DbAccess.YES);
        adm(REQ_GCP_SET_DETAILS, DbClaim.READ, DbAccess.YES);
        adm(REQ_GCP_SET_NOTIFICATIONS, DbClaim.READ, DbAccess.YES);
        usr(REQ_GET_EVENT, DbClaim.NONE, DbAccess.USER_LOCK);

        adm(REQ_IMAP_TEST, DbClaim.NONE, DbAccess.NO);
        adm(REQ_IMAP_START, DbClaim.NONE, DbAccess.NO);
        adm(REQ_IMAP_STOP, DbClaim.NONE, DbAccess.NO);

        adm(REQ_PAPERCUT_TEST, DbClaim.NONE, DbAccess.NO);

        adm(REQ_SMARTSCHOOL_TEST, DbClaim.NONE, DbAccess.NO);
        adm(REQ_SMARTSCHOOL_START, DbClaim.NONE, DbAccess.NO);
        adm(REQ_SMARTSCHOOL_START_SIMULATE, DbClaim.NONE, DbAccess.NO);
        adm(REQ_SMARTSCHOOL_STOP, DbClaim.NONE, DbAccess.NO);
        adm(REQ_SMARTSCHOOL_PAPERCUT_STUDENT_COST_CSV, DbClaim.NONE,
                DbAccess.NO);

        usr(REQ_JOB_DELETE, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_JOB_EDIT, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_JOB_PAGES, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_INBOX_IS_VANILLA, DbClaim.NONE, DbAccess.USER_LOCK);

        acl(REQ_JOBTICKET_DELETE, ReqJobTicketDelete.class, DbClaim.READ,
                DbAccess.YES, EnumSet.of(ACLRoleEnum.JOB_TICKET_OPERATOR));

        acl(REQ_JOBTICKET_PRINT, ReqJobTicketPrint.class, DbClaim.READ,
                DbAccess.YES, EnumSet.of(ACLRoleEnum.JOB_TICKET_OPERATOR));

        usr(REQ_JQPLOT, DbClaim.NONE, DbAccess.YES);
        non(REQ_LANGUAGE);
        usr(REQ_LETTERHEAD_ATTACH, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_LETTERHEAD_DELETE, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_LETTERHEAD_DETACH, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_LETTERHEAD_LIST, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_LETTERHEAD_NEW, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_LETTERHEAD_GET, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_LETTERHEAD_SET, DbClaim.NONE, DbAccess.USER_LOCK);

        put(REQ_LOGIN, ReqLogin.class, AuthReq.NONE, DbClaim.READ,
                DbAccess.YES);

        usr(REQ_LOGOUT, DbClaim.NONE, DbAccess.NO);
        usr(REQ_WEBAPP_UNLOAD, DbClaim.NONE, DbAccess.NO);
        put(REQ_WEBAPP_CLOSE_SESSION, AuthReq.NONE, DbClaim.NONE, DbAccess.NO);

        adm(REQ_MAIL_TEST, DbClaim.NONE, DbAccess.NO);

        usr(REQ_OUTBOX_CLEAR, ReqOutboxClear.class, DbClaim.NONE,
                DbAccess.USER_LOCK);
        usr(REQ_OUTBOX_DELETE_JOB, ReqOutboxDeleteJob.class, DbClaim.NONE,
                DbAccess.USER_LOCK);
        usr(REQ_OUTBOX_EXTEND, ReqOutboxExtend.class, DbClaim.NONE,
                DbAccess.USER_LOCK);

        adm(REQ_PAGOMETER_RESET, DbClaim.EXCLUSIVE, DbAccess.YES);

        usr(REQ_PAGE_DELETE, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_PAGE_MOVE, DbClaim.NONE, DbAccess.USER_LOCK);

        usr(REQ_PDF, DbClaim.READ, DbAccess.USER_LOCK);
        usr(REQ_PDF_GET_PROPERTIES, DbClaim.NONE, DbAccess.YES);
        usr(REQ_PDF_SET_PROPERTIES, DbClaim.READ, DbAccess.YES);

        non(REQ_PING);
        non(REQ_GENERATE_UUID, ReqGenerateUuid.class);

        usr(REQ_USER_CREDIT_TRANSFER, DbClaim.READ, DbAccess.YES);
        usr(REQ_USER_MONEY_TRANSFER_REQUEST, DbClaim.READ, DbAccess.YES);

        adm(REQ_BITCOIN_WALLET_REFRESH, DbClaim.NONE, DbAccess.NO);

        acl(REQ_POS_DEPOSIT, DbClaim.NONE, DbAccess.YES,
                EnumSet.of(ACLRoleEnum.WEB_CASHIER));

        acl(REQ_POS_RECEIPT_DOWNLOAD, DbClaim.READ, DbAccess.YES,
                EnumSet.of(ACLRoleEnum.WEB_CASHIER));

        usr(REQ_POS_RECEIPT_DOWNLOAD_USER, DbClaim.READ, DbAccess.YES);

        acl(REQ_POS_RECEIPT_SENDMAIL, DbClaim.READ, DbAccess.YES,
                EnumSet.of(ACLRoleEnum.WEB_CASHIER));

        acl(REQ_POS_DEPOSIT_QUICK_SEARCH, ReqPosDepositQuickSearch.class,
                DbClaim.READ, DbAccess.YES,
                EnumSet.of(ACLRoleEnum.WEB_CASHIER));

        adm(REQ_PAYMENT_GATEWAY_ONLINE, DbClaim.NONE, DbAccess.NO);

        usr(REQ_PRINT_AUTH_CANCEL, DbClaim.NONE, DbAccess.NO);
        usr(REQ_PRINT_FAST_RENEW, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_PRINTER_DETAIL, DbClaim.NONE, DbAccess.YES);
        adm(REQ_PRINTER_GET, DbClaim.NONE, DbAccess.YES);
        adm(REQ_PRINTER_OPT_DOWNLOAD, DbClaim.NONE, DbAccess.YES);
        usr(REQ_PRINTER_PRINT, ReqPrinterPrint.class, DbClaim.READ,
                DbAccess.USER_LOCK);

        usr(REQ_PRINTER_QUICK_SEARCH, ReqPrinterQuickSearch.class, DbClaim.READ,
                DbAccess.YES);

        adm(REQ_PRINTER_SET, DbClaim.READ, DbAccess.YES);
        adm(REQ_PRINTER_SET_MEDIA_COST, DbClaim.READ, DbAccess.YES);
        adm(REQ_PRINTER_SET_MEDIA_SOURCES, DbClaim.READ, DbAccess.YES);
        adm(REQ_PRINTER_RENAME, DbClaim.READ, DbAccess.YES);
        adm(REQ_PRINTER_SYNC, DbClaim.READ, DbAccess.YES);

        adm(REQ_QUEUE_GET, ReqQueueGet.class, DbClaim.NONE, DbAccess.YES);
        adm(REQ_QUEUE_SET, DbClaim.READ, DbAccess.YES);
        adm(REQ_QUEUE_ENABLE, ReqQueueEnable.class, DbClaim.READ, DbAccess.YES);

        adm(REQ_SHARED_ACCOUNT_GET, ReqSharedAccountGet.class, DbClaim.NONE,
                DbAccess.YES);
        adm(REQ_SHARED_ACCOUNT_SET, ReqSharedAccountSet.class, DbClaim.READ,
                DbAccess.YES);

        usr(REQ_SHARED_ACCOUNT_QUICK_SEARCH, ReqSharedAccountQuickSearch.class,
                DbClaim.READ, DbAccess.YES);

        adm(REQ_REPORT, DbClaim.READ, DbAccess.YES);
        usr(REQ_REPORT_USER, DbClaim.READ, DbAccess.YES);

        adm(REQ_RESET_ADMIN_PASSWORD, DbClaim.NONE, DbAccess.NO);
        adm(REQ_RESET_JMX_PASSWORD, DbClaim.NONE, DbAccess.NO);

        usr(REQ_RESET_USER_PASSWORD, DbClaim.READ, DbAccess.USER_LOCK);
        usr(REQ_RESET_USER_PIN, DbClaim.READ, DbAccess.USER_LOCK);
        usr(REQ_SEND, DbClaim.READ, DbAccess.USER_LOCK);
        usr(REQ_USER_LAZY_ECOPRINT, DbClaim.NONE, DbAccess.USER_LOCK);

        adm(REQ_USER_DELETE, DbClaim.READ, DbAccess.YES);
        adm(REQ_USER_GET, DbClaim.READ, DbAccess.YES);

        usr(REQ_USER_GET_STATS, DbClaim.READ, DbAccess.YES);

        acl(REQ_USER_NOTIFY_ACCOUNT_CHANGE, ReqUserNotifyAccountChange.class,
                DbClaim.READ, DbAccess.YES,
                EnumSet.of(ACLRoleEnum.WEB_CASHIER));

        acl(REQ_USER_QUICK_SEARCH, ReqUserQuickSearch.class, DbClaim.READ,
                DbAccess.YES, EnumSet.of(ACLRoleEnum.WEB_CASHIER));

        adm(REQ_USER_SET, DbClaim.READ, DbAccess.YES);
        adm(REQ_USER_SOURCE_GROUPS, DbClaim.NONE, DbAccess.NO);
        adm(REQ_USER_SYNC, DbClaim.NONE, DbAccess.NO);

        adm(REQ_USERGROUPS_ADD_REMOVE, ReqUserGroupsAddRemove.class,
                DbClaim.READ, DbAccess.YES);

        adm(REQ_USERGROUP_GET, ReqUserGroupGet.class, DbClaim.READ,
                DbAccess.YES);
        adm(REQ_USERGROUP_SET, ReqUserGroupSet.class, DbClaim.READ,
                DbAccess.YES);

        usr(REQ_USERGROUP_QUICK_SEARCH, ReqUserGroupQuickSearch.class,
                DbClaim.READ, DbAccess.YES);

        usr(REQ_USERGROUP_MEMBER_QUICK_SEARCH,
                ReqUserGroupMemberQuickSearch.class, DbClaim.READ,
                DbAccess.YES);

        usr(REQ_PRINT_DELEGATION_SET, ReqPrintDelegationSet.class, DbClaim.READ,
                DbAccess.YES);

    }

    /**
     * Creates a request handler.
     *
     * @param request
     *            The request id.
     * @return The {@link ApiRequestHandler}.
     */
    public ApiRequestHandler createApiRequest(final String request) {

        final Req req = this.dict.get(request);

        if (req.handler == null) {
            return null;
        }

        try {
            return req.handler.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new SpException(e.getMessage(), e);
        }
    }
}
