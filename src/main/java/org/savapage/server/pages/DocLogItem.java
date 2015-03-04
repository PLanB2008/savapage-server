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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.crypto.CryptoUser;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.PrintOutDao;
import org.savapage.core.dao.helpers.DocLogPagerReq;
import org.savapage.core.dao.helpers.DocLogProtocolEnum;
import org.savapage.core.dao.helpers.PrintInDeniedReasonEnum;
import org.savapage.core.dao.helpers.PrintModeEnum;
import org.savapage.core.dao.helpers.ReservedIppQueueEnum;
import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.DocIn;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.DocOut;
import org.savapage.core.jpa.PdfOut;
import org.savapage.core.jpa.PrintIn;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.services.QueueService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.NumberUtil;
import org.savapage.server.WebApp;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class DocLogItem {

    private String userId;
    private String userName;

    private String header;
    private DocLogDao.Type docType;
    private Date createdDate;
    private String title;
    private String comment;
    private String deliveryProtocol;

    private BigDecimal cost;

    private String humanReadableByteCount;
    private int totalPages;
    private int totalSheets;
    private PrintModeEnum printMode;
    private int copies;
    private Boolean drmRestricted;

    private String signature;
    private String destination;
    private Boolean letterhead;
    private Boolean duplex;
    private Boolean grayscale;

    private Integer jobId;
    private IppJobStateEnum jobState;
    private Boolean encrypted;
    private String paperSize;

    private Boolean printInPrinted;
    private PrintInDeniedReasonEnum printInDeniedReason;

    private String author;
    private String subject;
    private String keywords;
    private Boolean userPw;
    private Boolean ownerPw;

    private List<AccountTrx> transactions;

    public List<AccountTrx> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<AccountTrx> transactions) {
        this.transactions = transactions;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public DocLogDao.Type getDocType() {
        return docType;
    }

    public void setDocType(DocLogDao.Type docType) {
        this.docType = docType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getHumanReadableByteCount() {
        return humanReadableByteCount;
    }

    public void setHumanReadableByteCount(String humanReadableByteCount) {
        this.humanReadableByteCount = humanReadableByteCount;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    /**
     *
     *
     */
    public static abstract class AbstractQuery {

        protected abstract String getExtraWhere(DocLogPagerReq req);

        protected abstract String getExtraJoin();

        protected abstract void setExtraParms(final Query query,
                DocLogPagerReq req);

        /**
         *
         * @param em
         * @param count
         * @param userId
         * @param req
         * @return
         */
        private String getSelectString(final EntityManager em,
                final boolean count, final Long userId, DocLogPagerReq req) {

            String jpql = getSelectCommon(count);

            /*
             *
             */
            final String join = getExtraJoin();
            if (join != null) {
                jpql += " " + join;
            }

            /*
             *
             */
            final String whereCommon = getWhereCommon(userId, req);
            if (whereCommon != null) {
                jpql += " WHERE " + whereCommon;
            }

            /*
             *
             */
            final String where = getExtraWhere(req);
            if (where != null) {
                if (whereCommon == null) {
                    jpql += " WHERE ";
                } else {
                    jpql += " AND ";
                }
                jpql += where;
            }

            return jpql;
        }

        /**
         *
         * @param em
         * @param userId
         * @param req
         * @return
         */
        public long filteredCount(final EntityManager em, final Long userId,
                DocLogPagerReq req) {

            final String jpql = getSelectString(em, true, userId, req);

            Query query = em.createQuery(jpql);

            final Date dayFrom = req.getSelect().dateFrom();
            final Date dayTo = req.getSelect().dateTo();
            final String titleText = req.getSelect().getDocName();

            setParmsCommon(query, userId, dayFrom, dayTo, titleText);
            setExtraParms(query, req);

            Number countResult = (Number) query.getSingleResult();
            return countResult.longValue();
        }

        /**
         *
         * @param orderBy
         * @return
         */
        protected abstract String getOrderByField(
                final DocLogDao.FieldEnum orderBy);

        /**
         *
         * @param em
         * @param userId
         * @param req
         * @return
         */
        @SuppressWarnings("unchecked")
        public List<DocLogItem> getListChunk(EntityManager em,
                final Long userId, final DocLogPagerReq req) {

            final PrintOutDao printOutDAO = ServiceContext
                    .getDaoContext().getPrintOutDao();

            String jpql = getSelectString(em, false, userId, req);

            /*
             *
             */
            final DocLogDao.FieldEnum orderBy = req.getSort().getSortField();
            final boolean sortAscending = req.getSort().getAscending();

            String orderField = null;

            switch (orderBy) {
            case DOC_NAME:
                orderField = "D.title";
                break;
            case CREATE_DATE:
                orderField = "D.createdDate";
                break;
            default:
                orderField = getOrderByField(orderBy);
                break;
            }

            if (orderField != null) {

                jpql += " ORDER BY " + orderField;

                if (!sortAscending) {
                    jpql += " DESC";
                }

                jpql += ", D.id DESC";
            }

            final Query query = em.createQuery(jpql);

            final Date dayFrom = req.getSelect().dateFrom();
            final Date dayTo = req.getSelect().dateTo();
            final String titleText = req.getSelect().getDocName();

            setParmsCommon(query, userId, dayFrom, dayTo, titleText);
            setExtraParms(query, req);

            Integer startPosition = req.calcStartPosition();
            Integer maxResults = req.getMaxResults();

            if (startPosition != null) {
                query.setFirstResult(startPosition);
            }
            if (maxResults != null) {
                query.setMaxResults(maxResults);
            }

            final QueueService queueService =
                    ServiceContext.getServiceFactory().getQueueService();

            final List<DocLogItem> list = new ArrayList<>();

            for (final DocLog docLog : ((List<DocLog>) query.getResultList())) {

                DocLogItem log = new DocLogItem();

                log.setUserId(docLog.getUser().getUserId());
                log.setUserName(docLog.getUser().getFullName());
                log.setTitle(docLog.getTitle());
                log.setComment(docLog.getLogComment());
                log.setDeliveryProtocol(docLog.getDeliveryProtocol());
                log.setDrmRestricted(docLog.getDrmRestricted());
                log.setCreatedDate(docLog.getCreatedDate());
                log.setCost(docLog.getCost());

                if (docLog.getNumberOfPages() == null) {
                    log.setTotalPages(0);
                } else {
                    log.setTotalPages(docLog.getNumberOfPages());
                }

                if (docLog.getTransactions() == null) {
                    log.setTransactions(new ArrayList<AccountTrx>());
                } else {
                    log.setTransactions(docLog.getTransactions());
                }

                log.setHumanReadableByteCount(NumberUtil
                        .humanReadableByteCount(docLog.getNumberOfBytes(), true));

                final DocIn docIn = docLog.getDocIn();
                final DocOut docOut = docLog.getDocOut();

                log.setTotalSheets(0);
                log.setCopies(1);

                if (docIn != null) {

                    final PrintIn printIn = docIn.getPrintIn();
                    log.setDocType(DocLogDao.Type.IN);

                    if (printIn != null) {

                        final DocLogProtocolEnum protocol =
                                DocLogProtocolEnum.asEnum(docLog
                                        .getDeliveryProtocol());

                        ReservedIppQueueEnum reservedQueue =
                                queueService.getReservedQueue(printIn
                                        .getQueue().getUrlPath());

                        if (reservedQueue == null) {
                            reservedQueue = ReservedIppQueueEnum.IPP_PRINT;
                        }

                        if (reservedQueue == ReservedIppQueueEnum.IPP_PRINT) {

                            log.setHeader(
                            // reservedQueue.getUiText() + " " +
                            WebApp.MOUNT_PATH_PRINTERS + "/"
                                    + printIn.getQueue().getUrlPath());

                        } else {
                            log.setHeader(reservedQueue.getUiText());
                        }

                        log.setPaperSize(printIn.getPaperSize());

                        log.setPrintInPrinted(printIn.getPrinted());
                        log.setPrintInDeniedReason(PrintInDeniedReasonEnum
                                .parseDbValue(printIn.getDeniedReason()));

                    } else {
                        log.setHeader("???");
                    }

                } else if (docOut != null) {

                    log.setHeader(docOut.getDestination());
                    log.setSignature(docOut.getSignature());
                    log.setDestination(docOut.getDestination());

                    log.setLetterhead(docOut.getLetterhead() != null
                            && docOut.getLetterhead().booleanValue());

                    final PrintOut printOut = docOut.getPrintOut();
                    final PdfOut pdfOut = docOut.getPdfOut();

                    if (printOut != null) {

                        log.setDocType(DocLogDao.Type.PRINT);
                        log.setHeader(printOut.getPrinter().getDisplayName());

                        log.setDuplex(printOut.getDuplex());
                        log.setGrayscale(printOut.getGrayscale());

                        log.setPaperSize(printOut.getPaperSize());
                        log.setJobId(printOut.getCupsJobId());
                        log.setJobState(printOutDAO.getIppJobState(printOut));
                        log.setTotalSheets(printOut.getNumberOfSheets());
                        log.setCopies(printOut.getNumberOfCopies());

                        log.setPrintMode(PrintModeEnum.valueOf(printOut
                                .getPrintMode()));

                    } else if (pdfOut != null) {

                        log.setDocType(DocLogDao.Type.PDF);
                        log.setHeader("PDF");

                        log.setAuthor(pdfOut.getAuthor());
                        log.setSubject(pdfOut.getSubject());
                        log.setKeywords(pdfOut.getKeywords());
                        log.setEncrypted(pdfOut.getEncrypted());
                        log.setOwnerPw(StringUtils.isNotBlank(pdfOut
                                .getPasswordOwner()));
                        log.setUserPw(StringUtils.isNotBlank(pdfOut
                                .getPasswordUser()));
                    }
                }

                list.add(log);
            }
            return list;

        }

        /**
         *
         */
        protected final String getSelectCommon(final boolean count) {

            String jpql = "SELECT ";

            if (count) {
                jpql += "COUNT(D.id)";
            } else {
                jpql += "D";
            }

            jpql += " FROM DocLog D JOIN D.user U";

            return jpql;
        }

        /**
         *
         * @param userId
         * @param req
         * @return
         */
        protected final String getWhereCommon(final Long userId,
                final DocLogPagerReq req) {

            final Date dayFrom = req.getSelect().dateFrom();
            final Date dayTo = req.getSelect().dateTo();
            final String titleText = req.getSelect().getDocName();

            int nWhere = 0;
            String where = "";

            if (userId != null) {
                if (nWhere > 0) {
                    where += " AND ";
                }
                nWhere++;
                where += "U.id = :userId";
            }

            if (dayFrom != null) {
                if (nWhere > 0) {
                    where += " AND ";
                }
                nWhere++;
                where += "D.createdDay >= :dayFrom";
            }

            if (dayTo != null) {
                if (nWhere > 0) {
                    where += " AND ";
                }
                nWhere++;
                where += "D.createdDay <= :dayTo";
            }

            if (titleText != null) {
                if (nWhere > 0) {
                    where += " AND ";
                }
                nWhere++;
                where += "lower(D.title) like :titleText";
            }

            if (nWhere == 0) {
                where = null;
            }

            return where;
        }

        /**
         *
         * @param query
         * @param userId
         * @param dayFrom
         * @param dayTo
         * @param titleText
         */
        protected final void setParmsCommon(final Query query,
                final Long userId, final Date dayFrom, final Date dayTo,
                final String titleText) {

            if (userId != null) {
                query.setParameter("userId", userId);
            }
            if (dayFrom != null) {
                query.setParameter("dayFrom", dayFrom);
            }
            if (dayTo != null) {
                query.setParameter("dayTo", dayTo);
            }
            if (titleText != null) {
                query.setParameter("titleText", "%" + titleText.toLowerCase()
                        + "%");
            }
        }

    };

    /**
     *
     *
     */
    private static class QAll extends AbstractQuery {

        @Override
        public final String getExtraJoin() {
            return null;
        }

        @Override
        public final String getExtraWhere(final DocLogPagerReq req) {
            return null;
        }

        @Override
        protected void setExtraParms(Query query, DocLogPagerReq req) {
            // no code intended
        }

        @Override
        protected String getOrderByField(DocLogDao.FieldEnum orderBy) {
            return null;
        }
    };

    /**
     *
     */
    private static class QIn extends AbstractQuery {

        @Override
        protected String getExtraWhere(DocLogPagerReq req) {
            String jpql = null;
            Long id = req.getSelect().getQueueId();
            if (id != null && id > 0) {
                jpql = "Q.id = :queue_id";
            }
            return jpql;
        }

        @Override
        protected String getExtraJoin() {
            return "JOIN D.docIn I JOIN I.printIn P JOIN P.queue Q";
        }

        @Override
        protected void setExtraParms(Query query, DocLogPagerReq req) {
            Long id = req.getSelect().getQueueId();
            if (id != null && id > 0) {
                query.setParameter("queue_id", id);
            }
        }

        @Override
        protected String getOrderByField(DocLogDao.FieldEnum orderBy) {

            if (orderBy == DocLogDao.FieldEnum.QUEUE) {
                return "Q.urlPath";
            } else {
                return null;
            }
        }
    }

    /**
     *
     */
    private static class QOut extends AbstractQuery {

        @Override
        protected String getExtraWhere(DocLogPagerReq req) {

            final String selSignature = req.getSelect().getSignature();
            final String selDestination = req.getSelect().getDestination();
            final Boolean selLetterhead = req.getSelect().getLetterhead();

            int nWhere = 0;
            String where = "";

            if (selSignature != null) {
                if (nWhere > 0) {
                    where += " AND ";
                }
                nWhere++;
                where += "lower(O.signature) like :signature";
            }

            if (selDestination != null) {
                if (nWhere > 0) {
                    where += " AND ";
                }
                nWhere++;
                where += "lower(O.destination) like :destination";
            }

            if (selLetterhead != null) {
                if (nWhere > 0) {
                    where += " AND ";
                }
                nWhere++;
                where += "O.letterhead = :letterhead";
            }

            if (nWhere == 0) {
                where = null;
            }

            return where;
        }

        @Override
        protected String getExtraJoin() {
            return "JOIN D.docOut O";
        }

        @Override
        protected void setExtraParms(Query query, DocLogPagerReq req) {

            final String selSignature = req.getSelect().getSignature();
            final String selDestination = req.getSelect().getDestination();
            final Boolean selLetterhead = req.getSelect().getLetterhead();

            if (selSignature != null) {
                query.setParameter("signature",
                        "%" + selSignature.toLowerCase() + "%");
            }
            if (selDestination != null) {
                query.setParameter("destination",
                        "%" + selDestination.toLowerCase() + "%");
            }
            if (selLetterhead != null) {
                query.setParameter("letterhead", selLetterhead);
            }
        }

        @Override
        protected String getOrderByField(DocLogDao.FieldEnum orderBy) {
            return null;
        }
    }

    /**
     *
     */
    private static class QPdf extends QOut {
        @Override
        protected String getExtraWhere(DocLogPagerReq req) {

            int nWhere = 0;
            String where = super.getExtraWhere(req);

            if (where == null) {
                where = "";
            } else {
                nWhere++;
            }

            final String selAuthor = req.getSelect().getAuthor();
            final String selSubject = req.getSelect().getSubject();
            final String selKeywords = req.getSelect().getKeywords();
            final String selUserpw = req.getSelect().getUserPw();
            final String selOwnerpw = req.getSelect().getOwnerPw();
            final Boolean selEncrypted = req.getSelect().getEncrypted();

            if (selAuthor != null) {
                if (nWhere > 0) {
                    where += " AND ";
                }
                nWhere++;
                where += "lower(F.author) like :author";
            }
            if (selSubject != null) {
                if (nWhere > 0) {
                    where += " AND ";
                }
                nWhere++;
                where += "lower(F.subject) like :subject";
            }
            if (selKeywords != null) {
                if (nWhere > 0) {
                    where += " AND ";
                }
                nWhere++;
                where += "lower(F.keywords) like :keywords";
            }
            if (selUserpw != null) {
                if (nWhere > 0) {
                    where += " AND ";
                }
                nWhere++;
                where += "F.passwordUser = :userpw";
            }
            if (selOwnerpw != null) {
                if (nWhere > 0) {
                    where += " AND ";
                }
                nWhere++;
                where += "F.passwordOwner = :ownerpw";
            }
            if (selEncrypted != null) {
                if (nWhere > 0) {
                    where += " AND ";
                }
                nWhere++;
                where += "F.encrypted = :encrypted";
            }

            if (nWhere == 0) {
                where = null;
            }
            return where;
        }

        @Override
        protected String getExtraJoin() {
            return "JOIN D.docOut O JOIN O.pdfOut F";
        }

        @Override
        protected void setExtraParms(Query query, DocLogPagerReq req) {

            super.setExtraParms(query, req);

            final String selAuthor = req.getSelect().getAuthor();
            final String selSubject = req.getSelect().getSubject();
            final String selKeywords = req.getSelect().getKeywords();
            final String selUserpw = req.getSelect().getUserPw();
            final String selOwnerpw = req.getSelect().getOwnerPw();
            final Boolean selEncrypted = req.getSelect().getEncrypted();

            if (selAuthor != null) {
                query.setParameter("author", "%" + selAuthor.toLowerCase()
                        + "%");
            }
            if (selSubject != null) {
                query.setParameter("subject", "%" + selSubject.toLowerCase()
                        + "%");
            }
            if (selKeywords != null) {
                query.setParameter("keywords", "%" + selKeywords.toLowerCase()
                        + "%");
            }
            if (selUserpw != null) {
                query.setParameter("userpw", CryptoUser.encrypt(selUserpw));
            }
            if (selOwnerpw != null) {
                query.setParameter("ownerpw", CryptoUser.encrypt(selOwnerpw));
            }
            if (selEncrypted != null) {
                query.setParameter("encrypted", selEncrypted);
            }
        }

        @Override
        protected String getOrderByField(DocLogDao.FieldEnum orderBy) {
            return null;
        }
    }

    /**
     *
     */
    private static class QPrint extends QOut {

        @Override
        protected String getExtraWhere(DocLogPagerReq req) {

            int nWhere = 0;
            String where = super.getExtraWhere(req);

            if (where == null) {
                where = "";
            } else {
                nWhere++;
            }

            Long selId = req.getSelect().getPrinterId();
            final Boolean selDuplex = req.getSelect().getDuplex();
            final DocLogDao.JobState selState =
                    req.getSelect().getPrintOutState();

            if (selId != null && selId > 0) {
                if (nWhere > 0) {
                    where += " AND ";
                }
                nWhere++;
                where += "S.id = :printer_id";
            }

            if (selDuplex != null) {
                if (nWhere > 0) {
                    where += " AND ";
                }
                nWhere++;
                where += "P.duplex = :duplex";
            }

            if (selState == DocLogDao.JobState.ACTIVE) {
                if (nWhere > 0) {
                    where += " AND ";
                }
                nWhere++;
                where +=
                        "P.cupsJobState < "
                                + IppJobStateEnum.IPP_JOB_STOPPED.asInt();
            } else if (selState == DocLogDao.JobState.COMPLETED) {
                if (nWhere > 0) {
                    where += " AND ";
                }
                nWhere++;
                where +=
                        "P.cupsJobState > "
                                + IppJobStateEnum.IPP_JOB_PROCESSING.asInt();
            }

            if (nWhere == 0) {
                where = null;
            }
            return where;
        }

        @Override
        protected String getExtraJoin() {
            return "JOIN D.docOut O JOIN O.printOut P JOIN P.printer S";
        }

        @Override
        protected void setExtraParms(Query query, DocLogPagerReq req) {

            super.setExtraParms(query, req);

            Long selId = req.getSelect().getPrinterId();
            final Boolean selDuplex = req.getSelect().getDuplex();

            if (selId != null && selId > 0) {
                query.setParameter("printer_id", selId);
            }

            if (selDuplex != null) {
                query.setParameter("duplex", selDuplex);
            }

        }

        @Override
        protected String getOrderByField(DocLogDao.FieldEnum orderBy) {
            if (orderBy == DocLogDao.FieldEnum.PRINTER) {
                return "S.displayName";
            } else {
                return null;
            }
        }

    }

    /**
     *
     */
    private DocLogItem() {

    }

    /**
     *
     * @param em
     * @param userId
     * @param req
     * @return
     */
    public static AbstractQuery createQuery(final DocLogDao.Type docType) {

        switch (docType) {
        case ALL:
            return new QAll();
        case IN:
            return new QIn();
        case OUT:
            return new QOut();
        case PDF:
            return new QPdf();
        case PRINT:
            return new QPrint();
        default:
            throw new SpException("unknown doctype [" + docType + "]");
        }
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Boolean getLetterhead() {
        return letterhead;
    }

    public void setLetterhead(Boolean letterhead) {
        this.letterhead = letterhead;
    }

    public Boolean getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    public Boolean getDuplex() {
        return duplex;
    }

    public void setDuplex(Boolean duplex) {
        this.duplex = duplex;
    }

    public Boolean getGrayscale() {
        return grayscale;
    }

    public void setGrayscale(Boolean grayscale) {
        this.grayscale = grayscale;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public Boolean getUserPw() {
        return userPw;
    }

    public void setUserPw(Boolean userPw) {
        this.userPw = userPw;
    }

    public Boolean getOwnerPw() {
        return ownerPw;
    }

    public void setOwnerPw(Boolean ownerPw) {
        this.ownerPw = ownerPw;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPaperSize() {
        return paperSize;
    }

    public void setPaperSize(String paperSize) {
        this.paperSize = paperSize;
    }

    public Integer getJobId() {
        return jobId;
    }

    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }

    public IppJobStateEnum getJobState() {
        return jobState;
    }

    public void setJobState(IppJobStateEnum jobState) {
        this.jobState = jobState;
    }

    public Boolean getDrmRestricted() {
        return drmRestricted;
    }

    public void setDrmRestricted(Boolean drmRestricted) {
        this.drmRestricted = drmRestricted;
    }

    public Boolean getPrintInPrinted() {
        return printInPrinted;
    }

    public void setPrintInPrinted(Boolean printInPrinted) {
        this.printInPrinted = printInPrinted;
    }

    public PrintInDeniedReasonEnum getPrintInDeniedReason() {
        return printInDeniedReason;
    }

    public void setPrintInDeniedReason(
            PrintInDeniedReasonEnum printInDeniedReason) {
        this.printInDeniedReason = printInDeniedReason;
    }

    public String getDeliveryProtocol() {
        return deliveryProtocol;
    }

    public void setDeliveryProtocol(String deliveryProtocol) {
        this.deliveryProtocol = deliveryProtocol;
    }

    public int getTotalSheets() {
        return totalSheets;
    }

    public void setTotalSheets(int totalSheets) {
        this.totalSheets = totalSheets;
    }

    public int getCopies() {
        return copies;
    }

    public void setCopies(int copies) {
        this.copies = copies;
    }

    public PrintModeEnum getPrintMode() {
        return printMode;
    }

    public void setPrintMode(PrintModeEnum printMode) {
        this.printMode = printMode;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

}