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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.EnumUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.PerformanceLogger;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.ServiceEntryPoint;
import org.savapage.server.SpSession;
import org.savapage.server.WebAppParmEnum;
import org.savapage.server.api.UserAgentHelper;
import org.savapage.server.api.request.ApiRequestHelper;
import org.savapage.server.webapp.WebAppTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract page for all pages.
 * <p>
 * TODO: All helper methods should be moved to {@link MarkupHelper}.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractPage extends WebPage
        implements ServiceEntryPoint {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    protected static final String POST_PARM_WEBAPPTYPE = "webAppType";

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractPage.class);

    private final DateFormat dfLongDate = DateFormat
            .getDateInstance(DateFormat.LONG, getSession().getLocale());

    private final DateFormat dfDateTime = DateFormat.getDateTimeInstance(
            DateFormat.LONG, DateFormat.LONG, getSession().getLocale());

    private final DateFormat dfShortDateTime = DateFormat.getDateTimeInstance(
            DateFormat.SHORT, DateFormat.SHORT, getSession().getLocale());

    private final NumberFormat fmNumber =
            NumberFormat.getInstance(getSession().getLocale());

    private final DateFormat dfShortDate = DateFormat
            .getDateInstance(DateFormat.SHORT, getSession().getLocale());

    private final DateFormat dfMediumDate = DateFormat
            .getDateInstance(DateFormat.MEDIUM, getSession().getLocale());

    private boolean serviceContextOpened = false;

    /**
     *
     */
    private Date perfStartTime;

    /**
     *
     * @param parameters
     */
    protected AbstractPage() {
        super();
    }

    /**
     *
     * @param parameters
     */
    protected AbstractPage(final PageParameters parameters) {
        super(parameters);
    }

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
     * @return The web App type.
     */
    protected final WebAppTypeEnum
            getWebAppTypeEnum(final PageParameters parameters) {
        return EnumUtils.getEnum(WebAppTypeEnum.class,
                parameters.get(WebAppParmEnum.SP_APP.parm()).toString());
    }

    /**
     * Opens the {@link ServiceContext} with the {@link Locale} of the session.
     * <p>
     * When needed, this method MUST be called in the <b>constructor</b> of the
     * actual Page implementation.
     * </p>
     * <p>
     * Note: The {@link #onAfterRender()} method is overloaded to
     * COMMIT/ROLLBACK the {@link DaoContext} and to CLOSE the
     * {@link ServiceContext}.
     * </p>
     * <p>
     * Note: The {@link #onBeforeRender()} method is overloaded to handle any
     * {@link Exception} from super#{@link #onAfterRender()} to ROLLBACK the
     * {@link DaoContext} and to CLOSE the {@link ServiceContext}.
     * </p>
     */
    private void openServiceContext() {
        ServiceContext.open();
        serviceContextOpened = true;
        ServiceContext.setLocale(getSession().getLocale());
    }

    @Override
    protected final void onBeforeRender() {

        perfStartTime = PerformanceLogger.startTime();

        openServiceContext();

        try {

            super.onBeforeRender();

        } catch (Exception e) {

            LOGGER.error(e.getMessage(), e);

            if (serviceContextOpened) {
                ServiceContext.getDaoContext().rollback();
                ServiceContext.close();
            }

            throw e;
        }
    }

    @Override
    protected final void onAfterRender() {
        try {
            super.onAfterRender();
            if (serviceContextOpened) {
                ServiceContext.getDaoContext().commit();
            }
        } catch (Exception e) {

            LOGGER.error(e.getMessage(), e);

            if (serviceContextOpened) {
                ServiceContext.getDaoContext().rollback();
            }

            throw e;

        } finally {
            if (serviceContextOpened) {
                ServiceContext.close();
            }
        }

        if (PerformanceLogger.isEnabled()) {
            PerformanceLogger.log(this.getClass(), "onAfterRender",
                    perfStartTime, getSessionWebAppType().toString());
        }
    }

    /**
     * Gets the authenticated {@link WebAppTypeEnum} from the session.
     *
     * @return The {@link WebAppTypeEnum}.
     */
    protected final WebAppTypeEnum getSessionWebAppType() {

        WebAppTypeEnum webAppType = WebAppTypeEnum.UNDEFINED;

        final SpSession session = SpSession.get();

        if (getSession() != null) {
            webAppType = session.getWebAppType();
        }

        return webAppType;
    }

    /**
     * @return {@code true} when Google Sign-In is enabled (for client device).
     */
    protected final boolean isGoogleSignInEnabled() {
        return ApiRequestHelper.isGoogleSignInEnabled(getSessionWebAppType(),
                this.getClientIpAddr());
    }

    /**
     * @return {@code true} when login is restricted to local methods, i.e.
     *         Google Sign-In is inactive.
     */
    protected final boolean isRestrictedToLocalLogin() {
        return this.getParmValue(this.getPageParameters(), true,
                WebAppParmEnum.SP_LOGIN_LOCAL.parm()) != null;
    }

    /**
     * @deprecated Gets as localized string of a Number. The locale of the
     *             current session is used.
     *             <p>
     *             Use {@link MarkupHelper#localizedNumber(long)}.
     *             </p>
     *
     * @param number
     * @return The localized string.
     */
    @Deprecated
    protected final String localizedNumber(final long number) {
        return fmNumber.format(number);
    }

    /**
     * Gets as localized (long) date string of a Date. The locale of the current
     * session is used.
     *
     * @param date
     *            The date.
     * @return The localized date string.
     */
    protected final String localizedDate(final Date date) {
        return dfLongDate.format(date);
    }

    /**
     * Gets as localized (short) date string of a Date. The locale of the
     * current session is used.
     *
     * @param date
     *            The date.
     * @return The localized date string.
     */
    protected final String localizedShortDate(final Date date) {
        return dfShortDate.format(date);
    }

    /**
     * Gets as localized (medium) date string of a Date. The locale of the
     * current session is used.
     *
     * @param date
     *            The date.
     * @return The localized date string.
     */
    protected final String localizedMediumDate(final Date date) {
        return dfMediumDate.format(date);
    }

    /**
     * Gets as localized date/time string of a Date. The locale of the current
     * session is used.
     *
     * @param date
     *            The date.
     * @return The localized date/time string.
     */
    protected final String localizedDateTime(final Date date) {
        return dfDateTime.format(date);
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
     * Gets the POST-ed parameter value.
     *
     * @param parm
     *            Parameter name.
     * @return {@code null} when parameter is not present.
     */
    protected final String getParmValue(final String parm) {
        return getRequestCycle().getRequest().getPostParameters()
                .getParameterValue(parm).toString();
    }

    /**
     * Gets the POST-ed parameter boolean value.
     *
     * @param parm
     *            Parameter name.
     * @param defaultValue
     *            The dfaut value.
     * @return The boolean (default) value.
     */
    protected final boolean getParmBoolean(final String parm,
            final boolean defaultValue) {
        return getRequestCycle().getRequest().getPostParameters()
                .getParameterValue(parm).toBoolean(defaultValue);
    }

    /**
     *
     * @param getParms
     *            The {@link PageParameters}.
     * @param isGetAction
     *            {@code true} when a GET parameter.
     * @param parm
     *            The parameter name.
     * @return {@code null} when parameter is not present.
     */
    protected final String getParmValue(final PageParameters getParms,
            final boolean isGetAction, final String parm) {
        if (isGetAction) {
            return getParms.get(parm).toString();
        }
        return getParmValue(parm);
    }

    protected DateFormat getDfShortDate() {
        return dfShortDate;
    }

    /**
     * Gets the IP address of the client.
     *
     * @return
     */
    protected String getClientIpAddr() {
        return ((ServletWebRequest) RequestCycle.get().getRequest())
                .getContainerRequest().getRemoteAddr();
    }

    /**
     *
     * @param isVisible
     * @param id
     * @param val
     * @return
     */
    protected final Label createVisibleLabel(final boolean isVisible,
            final String id, final String val) {

        Label label = new Label(id, val) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isVisible;
            }
        };
        return label;
    }

    /**
     *
     * @param isVisible
     * @param id
     * @param val
     * @param cssClass
     * @return
     */
    protected final Label createVisibleLabel(final boolean isVisible,
            final String id, final String val, final String cssClass) {

        Label label = createVisibleLabel(isVisible, id, val);

        if (isVisible && cssClass != null) {
            label.add(new AttributeModifier("class", cssClass));
        }

        return label;
    }

    /**
     *
     * @param isVisible
     * @param id
     * @param val
     */
    protected final void addVisible(final boolean isVisible, final String id,
            final String val) {
        addVisible(isVisible, id, val, null);
    }

    /**
     *
     * @param isVisible
     * @param id
     * @param val
     * @param cssClass
     *            Can be {@code null}.
     */
    protected final void addVisible(final boolean isVisible, final String id,
            final String val, final String cssClass) {
        add(createVisibleLabel(isVisible, id, val, cssClass));
    }

    /**
     * @return The {@link UserAgentHelper}.
     */
    protected UserAgentHelper createUserAgentHelper() {
        final HttpServletRequest request =
                (HttpServletRequest) getRequestCycle().getRequest()
                        .getContainerRequest();
        return new UserAgentHelper(request);
    }

    /**
     * Adds a label/input text.
     *
     * @param wicketId
     *            The {@code wicket:id} of the {@code <input>} part.
     * @param attrIdFor
     *            The value of the HTML 'id' attribute of the {@code <input>}
     *            part, and the 'for' attribute of the {@code <label>} part.
     * @param attrValue
     *            The value of the HTML 'value' attribute of this radio button.
     */
    protected void labelledText(final String wicketId, final String attrIdFor,
            final String attrValue) {
        Label labelWrk = new Label(wicketId);
        labelWrk.add(new AttributeModifier("id", attrIdFor));
        labelWrk.add(new AttributeModifier("value", attrValue));
        add(labelWrk);
        tagLabel(wicketId + "-label", wicketId, attrIdFor);
    }

    /**
     * @deprecated Adds a checkbox.
     *             <p>
     *             Use {@link MarkupHelper#addCheckbox(String, String, boolean)}
     *             .
     *             </p>
     *
     * @param wicketId
     *            The {@code wicket:id} of the {@code <input>} part.
     *
     * @param htmlId
     *            The HTML 'id' of the {@code <input>} part.
     * @param checked
     *            {@code true} if the checkbox must be checked.
     */
    @Deprecated
    protected void tagCheckbox(final String wicketId, final String htmlId,
            final boolean checked) {
        Label labelWrk = new Label(wicketId);
        labelWrk.add(new AttributeModifier("id", htmlId));
        if (checked) {
            labelWrk.add(new AttributeModifier("checked", "checked"));
        }
        add(labelWrk);
    }

    /**
     * @deprecated Adds a label.
     *             <p>
     *             Use {@link MarkupHelper#tagLabel(String, String, String)}.
     *             </p>
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param localizerKey
     *            The localizer key of the label text as used in
     *            {@link #getLocalizer()}.
     * @param attrFor
     *            The value of the HTML 'for' attribute.
     */
    @Deprecated
    protected void tagLabel(final String wicketId, final String localizerKey,
            final String attrFor) {
        Label labelWrk = new Label(wicketId,
                getLocalizer().getString(localizerKey, this));
        labelWrk.add(new AttributeModifier("for", attrFor));
        add(labelWrk);
    }

    /**
     *
     * @param wicketIdBase
     *            The base {@code wicket:id} for the radio group items.
     * @param wicketIdSuffix
     *            The {@code wicket:id} suffix for this item.
     * @param attrName
     *            The value of the HTML 'name' attribute of this radio button.
     * @param attrValue
     *            The value of the HTML 'value' attribute of this radio button.
     * @param checked
     */
    protected void labelledRadio(final String wicketIdBase,
            final String wicketIdSuffix, final String attrName,
            final String attrValue, final boolean checked) {

        final String attrId = attrName + wicketIdSuffix;
        tagRadio(wicketIdBase + wicketIdSuffix, attrName, attrId, attrValue,
                checked);
        /*
         *
         */
        Label labelWrk = new Label(wicketIdBase + wicketIdSuffix + "-label",
                getLocalizer().getString(wicketIdBase + wicketIdSuffix, this));
        labelWrk.add(new AttributeModifier("for", attrId));
        add(labelWrk);
    }

    /**
     *
     * @param wicketId
     *            The {@code wicket:id} for this radio group item.
     * @param attrName
     *            The value of the HTML 'name' attribute of this radio button.
     * @param attrId
     *            The value of the HTML 'id' attribute of this radio button.
     * @param attrValue
     *            The value of the HTML 'value' attribute of this radio button.
     * @param checked
     */
    protected void tagRadio(final String wicketId, final String attrName,
            final String attrId, final String attrValue,
            final boolean checked) {
        Label labelWrk = new Label(wicketId);
        labelWrk.add(new AttributeModifier("name", attrName));
        labelWrk.add(new AttributeModifier("id", attrId));
        labelWrk.add(new AttributeModifier("value", attrValue));
        if (checked) {
            labelWrk.add(new AttributeModifier("checked", "checked"));
        }
        add(labelWrk);
    }

    /**
     * @deprecated Adds input of type text.
     *             <p>
     *             Use {@link MarkupHelper#addTextInput(String, String)}.
     *             </p>
     *
     * @param wicketId
     * @param value
     */
    @Deprecated
    protected final void addTextInput(final String wicketId,
            final String value) {

        Label labelWrk = new Label(wicketId);
        labelWrk.add(new AttributeModifier("value", value));
        add(labelWrk);
    }

}
