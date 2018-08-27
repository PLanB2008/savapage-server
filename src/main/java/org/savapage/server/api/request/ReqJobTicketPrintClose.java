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

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.msg.UserMsgIndicator;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.ServiceContext;
import org.savapage.ext.notification.JobTicketCancelEvent;
import org.savapage.ext.notification.JobTicketCloseEvent;
import org.savapage.ext.notification.JobTicketEvent;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqJobTicketPrintClose extends ApiRequestMixin {

    /**
     * .
     */
    private static final JobTicketService JOBTICKET_SERVICE =
            ServiceContext.getServiceFactory().getJobTicketService();

    /**
     *
     * The request.
     *
     */
    private static class DtoReq extends AbstractDto {

        private String jobFileName;

        public String getJobFileName() {
            return jobFileName;
        }

        @SuppressWarnings("unused")
        public void setJobFileName(String jobFileName) {
            this.jobFileName = jobFileName;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq =
                DtoReq.create(DtoReq.class, this.getParmValueDto());

        final OutboxJobDto dto =
                JOBTICKET_SERVICE.closeTicketPrint(dtoReq.getJobFileName());

        final String msgKey;

        if (dto == null) {
            msgKey = "msg-outbox-jobticket-print-close-none";
        } else {
            msgKey = "msg-outbox-jobticket-print-close";
            notifyUser(requestingUser, dto);
        }

        this.setApiResult(ApiResultCodeEnum.OK, msgKey);
    }

    /**
     *
     * @param requestingUser
     * @param dto
     * @throws IOException
     *             When IO error.
     */
    private void notifyUser(final String requestingUser, final OutboxJobDto dto)
            throws IOException {

        final Long userKey = dto.getUserId();
        final IppJobStateEnum jobState = dto.getIppJobState();

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();
        final User user = userDao.findById(userKey);

        if (UserMsgIndicator.isSafePagesDirPresent(user.getUserId())) {

            final UserMsgIndicator.Msg userMsgInd;

            if (Boolean
                    .valueOf(jobState == IppJobStateEnum.IPP_JOB_COMPLETED)) {
                userMsgInd = UserMsgIndicator.Msg.JOBTICKET_SETTLED_PRINT;
            } else {
                userMsgInd = UserMsgIndicator.Msg.JOBTICKET_DENIED;
            }

            UserMsgIndicator.write(user.getUserId(),
                    ServiceContext.getTransactionDate(), userMsgInd, null);
        }

        //
        sendEmailNotification(requestingUser, user, dto, jobState);

        //
        final JobTicketEvent event;

        if (jobState == IppJobStateEnum.IPP_JOB_COMPLETED) {
            event = new JobTicketCloseEvent();
        } else {
            event = new JobTicketCancelEvent();
        }

        final User userOperator =
                userDao.findActiveUserByUserId(requestingUser);

        event.setDocumentName(dto.getJobName());
        event.setOperator(requestingUser);

        if (userOperator == null
                || StringUtils.isBlank(userOperator.getFullName())) {
            event.setOperatorName(requestingUser);
        } else {
            event.setOperatorName(userOperator.getFullName());
        }

        event.setCreator(user.getUserId());
        event.setCreatorName(user.getFullName());

        event.setTicketNumber(dto.getTicketNumber());
        event.setLocale(getLocale());

        if (jobState == IppJobStateEnum.IPP_JOB_COMPLETED) {
            getNotificationListener()
                    .onJobTicketEvent((JobTicketCloseEvent) event);
        } else {
            getNotificationListener()
                    .onJobTicketEvent((JobTicketCancelEvent) event);
        }
    }

    /**
     *
     * @param requestingUser
     * @param user
     * @param dto
     * @param ippState
     * @return
     */
    private String sendEmailNotification(final String requestingUser,
            final User user, final OutboxJobDto dto,
            final IppJobStateEnum ippState) {

        /*
         * INVARIANT: Print must be completed.
         */
        if (ippState != IppJobStateEnum.IPP_JOB_COMPLETED) {
            return null;
        }
        /*
         * INVARIANT: Notification must be enabled.
         */
        if (!ConfigManager.instance()
                .isConfigValue(Key.JOBTICKET_NOTIFY_EMAIL_COMPLETED_ENABLE)) {
            return null;
        }

        return JOBTICKET_SERVICE.notifyTicketCompletedByEmail(dto,
                requestingUser, user, ConfigManager.getDefaultLocale());
    }

}
