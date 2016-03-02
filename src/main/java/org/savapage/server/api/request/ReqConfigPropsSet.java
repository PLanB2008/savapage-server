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
import java.text.ParseException;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.job.SpJobScheduler;
import org.savapage.core.jpa.PrinterGroup;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.ext.smartschool.SmartschoolPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates one or more configuration properties.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqConfigPropsSet extends ApiRequestMixin {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ReqConfigPropsSet.class);

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        String msgKey = "msg-config-props-applied";

        final JsonNode list;

        try {
            list = new ObjectMapper().readTree(getParmValue("dto"));
        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }

        final ConfigManager cm = ConfigManager.instance();

        final Iterator<String> iter = list.getFieldNames();

        boolean isSmartSchoolUpdate = false;

        boolean isValid = true;
        int nJobsRescheduled = 0;
        int nValid = 0;

        while (iter.hasNext() && isValid) {

            final String key = iter.next();

            String value = list.get(key).getTextValue();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(key + " = " + value);
            }

            final Key configKey = cm.getConfigKey(key);

            /*
             * If this value is Locale formatted, we MUST revert to locale
             * independent format.
             */
            if (cm.isConfigBigDecimal(configKey)) {
                try {
                    value = BigDecimalUtil.toPlainString(value,
                            this.getLocale(), true);
                } catch (ParseException e) {
                    throw new IllegalStateException(e.getMessage());
                }
            }

            /*
             *
             */
            if (!customConfigPropValidate(configKey, value)) {
                return;
            }

            final IConfigProp.ValidationResult res =
                    cm.validate(configKey, value);
            isValid = res.isValid();

            if (isValid) {

                boolean preValue = false;

                switch (configKey) {

                case SYS_DEFAULT_LOCALE:
                    ConfigManager.setDefaultLocale(value);
                    break;

                case SCHEDULE_HOURLY:
                    SpJobScheduler.instance().scheduleJobs(configKey);
                    nJobsRescheduled++;
                    break;
                case SCHEDULE_DAILY:
                    SpJobScheduler.instance().scheduleJobs(configKey);
                    nJobsRescheduled++;
                    break;
                case SCHEDULE_WEEKLY:
                    SpJobScheduler.instance().scheduleJobs(configKey);
                    nJobsRescheduled++;
                    break;
                case SCHEDULE_MONTHLY:
                    SpJobScheduler.instance().scheduleJobs(configKey);
                    nJobsRescheduled++;
                    break;
                case SCHEDULE_DAILY_MAINT:
                    SpJobScheduler.instance().scheduleJobs(configKey);
                    nJobsRescheduled++;
                    break;
                case PRINT_IMAP_ENABLE:
                    preValue = cm.isConfigValue(configKey);
                    break;
                default:
                    break;
                }

                /*
                 * TODO: This updates the cache while database is not committed
                 * yet! When database transaction is rollback back the cache is
                 * dirty.
                 */
                cm.updateConfigKey(configKey, value, requestingUser);

                nValid++;

                if (configKey == Key.PRINT_IMAP_ENABLE && preValue
                        && !cm.isConfigValue(configKey)) {
                    if (SpJobScheduler.interruptImapListener()) {
                        msgKey = "msg-config-props-applied-mail-print-stopped";
                    }

                } else if (configKey == Key.SMARTSCHOOL_1_ENABLE
                        || configKey == Key.SMARTSCHOOL_2_ENABLE) {
                    isSmartSchoolUpdate = true;
                }

            } else {
                setApiResult(ApiResultCodeEnum.ERROR, "msg-config-props-error",
                        value);
            }

        } // end-while

        if (nValid > 0) {
            ConfigManager.instance().calcRunnable();
        }

        if (isValid) {

            if (nJobsRescheduled > 0) {
                msgKey = "msg-config-props-applied-rescheduled";
            } else if (isSmartSchoolUpdate
                    && !ConfigManager.isSmartSchoolPrintActiveAndEnabled()
                    && SmartschoolPrinter.isOnline()) {
                if (SpJobScheduler.interruptSmartSchoolPoller()) {
                    msgKey = "msg-config-props-applied-smartschool-stopped";
                }
            }

            setApiResult(ApiResultCodeEnum.OK, msgKey);
        }

    }

    /**
     * Custom validates a {@link IConfigProp.Key} value.
     *
     * @param key
     *            The key of the configuration item.
     * @param value
     *            The value of the configuration item.
     * @return {@code null} when NO validation error, or the userData object
     *         filled with the error message when an error is encountered..
     */
    private boolean customConfigPropValidate(Key key, String value) {

        if (key == Key.PROXY_PRINT_NON_SECURE_PRINTER_GROUP
                && StringUtils.isNotBlank(value)) {

            final PrinterGroup jpaPrinterGroup = ServiceContext.getDaoContext()
                    .getPrinterGroupDao().findByName(value);

            if (jpaPrinterGroup == null) {
                setApiResult(ApiResultCodeEnum.ERROR,
                        "msg-device-printer-group-not-found", value);
                return false;
            }
        }

        if (key == Key.SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER
                || key == Key.SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER_DUPLEX
                || key == Key.SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER_GRAYSCALE
                || key == Key.SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER_GRAYSCALE_DUPLEX
                || key == Key.SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER
                || key == Key.SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER_DUPLEX
                || key == Key.SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER_GRAYSCALE
                || key == Key.SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER_GRAYSCALE_DUPLEX) {

            if (StringUtils.isNotBlank(value)) {

                final PrinterDao printerDao =
                        ServiceContext.getDaoContext().getPrinterDao();

                if (printerDao.findByName(value) == null) {
                    setApiResult(ApiResultCodeEnum.ERROR,
                            "msg-printer-not-found", value);
                    return false;
                }
            }
        }

        return true;
    }

}
