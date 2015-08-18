/*! SavaPage jQuery Mobile Admin Web App | (c) 2011-2015 Datraverse B.V. | GNU Affero General Public License */

/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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

/*
 * NOTE: the *! comment blocks are part of the compressed version.
 */

/*
 * SavaPage jQuery Mobile Admin Web App
 */
( function($, window, document, JSON, _ns) {"use strict";

		/*jslint browser: true*/
		/*global $, jQuery, alert*/

		/**
		 * Constructor
		 */
		_ns.Controller = function(_i18n, _model, _view, _api, _cometd) {

			var _this = this
			//
			, _util = _ns.Utils, i18nRefresh
			//
			, _handleLoginResult, _saveConfigProps
			//
			, _fillConfigPropsYN, _fillConfigPropsText, _fillConfigPropsRadio
			//
			, _userSync, _updateGcpState
			//
			;

			/**
			 *
			 */
			_fillConfigPropsYN = function(props, propNames) {
				propNames.forEach(function(name) {
					props[name] = _view.isCheckedYN($('#' + name.replace(/\./g, '\\.')));
				});
			};

			/**
			 *
			 */
			_fillConfigPropsRadio = function(props, propNames) {
				propNames.forEach(function(name) {
					props[name] = _view.getRadioValue(name);
				});
			};

			/**
			 *
			 */
			_fillConfigPropsText = function(props, propNames) {
				propNames.forEach(function(name) {
					props[name] = $('#' + name.replace(/\./g, '\\.')).val();
				});
			};

			/**
			 *
			 */
			i18nRefresh = function(i18nNew) {
				if (i18nNew && i18nNew.i18n) {
					_i18n.refresh(i18nNew.i18n);
					$.mobile.pageLoadErrorMessage = _i18n.string('msg-page-load-error');
				} else {
					_view.message('i18n initialization failed.');
				}
			};

			/**
			 *
			 */
			_handleLoginResult = function(authMode, data) {

				_model.user.loggedIn = (data.result.code <= '2');

				if (_model.user.loggedIn) {

					_model.startSession();

					_model.user.key_id = data.key_id;
					_model.user.id = data.id;

					_model.language = data.language;
					_model.country = data.country;

					_model.setAuthToken(data.id, data.authtoken, _model.language, _model.country);

					/*
					 * This is the token used for CometD authentication.
					 * See: Java org.savapage.server.cometd.BayeuxAuthenticator
					 */
					_model.user.cometdToken = data.cometdToken;

					_model.user.admin = data.admin;
					_model.user.role = data.role;
					_model.user.mail = data.mail;
					_model.user.mailDefault = data.mail;

					/*
					 * Used to display a login info/warning popup message at a
					 * later time.
					 */
					_model.user.logonApiResult = (data.result.code > '0') ? data.result : null;

					if (_view.pages.admin.isLoaded) {
						_view.pages.admin.initView();
						_view.pages.admin.show();
					} else {
						_view.pages.admin.load().show();
					}

					_cometd.start(_model.user.cometdToken);

					if (_model.maxIdleSeconds) {
						_ns.monitorUserIdle(_model.maxIdleSeconds, _view.pages.admin.onLogout);
					}

				} else if (_view.activePage().attr('id') === 'page-login') {
					_view.pages.login.notifyLoginFailed(authMode, data.result.txt);
				} else {
					_view.pages.login.loadShowAsync();
				}

			};
			/*
			 *
			 */
			_saveConfigProps = function(props) {
				var res = _api.call({
					request : 'config-set-props',
					props : JSON.stringify(props)
				});
				_view.showApiMsg(res);
				return (res.result.code === '0');
			};
			/*
			 *
			 */
			this.init = function() {

				var res, language, country
				//
				, authModeRequest = _util.getUrlParam('login');

				/*
				 *
				 */
				_model.initAuth();

				res = _api.call({
					request : 'constants',
					authMode : authModeRequest
				});

				/*
				 * FIX for Opera to prevent endless reloads when server is down:
				 * check the return code. Display a basic message (English is all
				 * we
				 * have), no fancy stuff (cause jQuery might not be working).
				 */
				if (res.result.code !== '0') {
					_view.message('connection to server is lost');
					return;
				}

				_model.locale = res.locale;
				_model.maxIdleSeconds = res.maxIdleSeconds;
				_model.intUserIdPfx = res.intUserIdPfx;

				// NOTE: authCardSelfAssoc is DISABLED
				_view.pages.login.setAuthMode(res.authName, res.authId, res.authCardLocal, res.authCardIp, res.authModeDefault, res.authCardPinReq, null, res.cardLocalMaxMsecs, res.cardAssocMaxSecs);

				// Configures CometD (without starting it)
				_cometd.configure(res.cometdMaxNetworkDelay);

				_i18n.initValues(res.i18n_values);

				language = _util.getUrlParam('language');
				if (!language) {
					language = _model.authToken.language || '';
				}

				country = _util.getUrlParam('country');
				if (!country) {
					country = _model.authToken.country || '';
				}

				res = _api.call({
					request : 'language',
					language : language,
					country : country
				});

				i18nRefresh(res);

				_view.initI18n(res.language);

				//
				// Call-back: api
				//
				_api.onExpired(function() {
					if (_view.activePage().attr('id') === 'page-admin') {
						_view.showExpiredDialog();
					} else {
						// deferred handling
						_model.sessionExpired = true;
					}
				});
				// Call-back: api
				_api.onDisconnected(function() {
					_model.user.loggedIn = false;
					_view.changePage($('#page-login'));
				});

			};

			/**
			 *
			 */
			this.login = function(authMode, authId, authPw, authToken) {

				_model.user.loggedIn = false;

				$('#overlay-curtain').show();

				_api.callAsync({
					request : 'login',
					authMode : authMode,
					authId : authId,
					authPw : authPw,
					authToken : authToken,
					role : 'admin'
				}, function(data) {
					_handleLoginResult(authMode, data);
				}, function() {
					$('#overlay-curtain').hide();
				}, function(data) {
					// Do NOT use _view.showApiMsg(data), cause it spoils the
					// focus() in case of Card Swipe.
					_view.message(data.result.txt);
				});
			};

			/*
			 *
			 */
			_view.onDisconnected = function() {
				_model.user.loggedIn = false;
				_view.pages.login.loadShowAsync();
			};

			/**
			 * Callbacks: page LANGUAGE
			 */
			_view.pages.language.onSelectLocale(function(lang, country) {
				/*
				 * This call sets the locale for the current session and returns
				 * strings needed for off-line mode.
				 */
				var res = _api.call({
					request : 'language',
					language : lang,
					country : country
				});

				if (res.result.code === "0") {

					_model.setLanguage(lang);
					_model.setCountry(country);

					i18nRefresh(res);

					/*
					 * By restarting, the newly localized login page is displayed
					 */
					_ns.restartWebApp();
				}
			});

			/**
			 * Callbacks: page User Password Reset
			 */
			_view.pages.userPwReset.onSelectReset(function(password) {
				var res = _api.call({
					request : 'reset-user-pw',
					iuser : _model.editUser.userName,
					password : password
				});
				_view.showApiMsg(res);
				if (res.result.code === '0') {
					_view.changePage($('#page-admin'));
				}
			});

			/**
			 * Callbacks: page LOGIN
			 */
			_view.pages.login.onShow(function() {
				_model.user.loggedIn = false;
				_cometd.stop();
			});

			_view.pages.login.onLanguage(function() {
				_view.pages.language.loadShowAsync();
			});

			_view.pages.login.onLogin(function(mode, id, pw) {
				_this.login(mode, id, pw);
			});

			/**
			 * Callbacks: page ADMIN
			 */
			_view.pages.admin.onDisconnected = function() {
				_model.user.loggedIn = false;
				_view.changePage($('#page-login'));
				_view.showApiMsg(_api.simulateDisconnectError());
			};

			_ns.PanelCommon.onDisconnected = _view.pages.admin.onDisconnected;

			_view.pages.admin.onLogout = function() {
				/*
				 * NOTE: This is the same solution as in the User WebApp.
				 * See remarks over there.
				 */

				/*
				 * Prevent that BACK button shows private data when disconnected.
				 * Mantis #108
				 */
				_model.startSession();

				$('#page-admin').empty();

				var res = _api.call({
					request : 'logout',
					authToken : _model.authToken.token
				});
				if (res.result.code !== '0') {
					_view.message(res.result.txt);
					_model.setAuthToken(null, null);
				}
				_ns.restartWebApp();
			};

			_view.pages.admin.onApplyCliAppAuth = function() {
				var props = {};
				_fillConfigPropsText(props, ['cliapp.auth.admin-passkey']);
				_fillConfigPropsYN(props, ['cliapp.auth.trust-user-account', 'cliapp.auth.trust-webapp-user-auth']);
				_saveConfigProps(props);
			};

			_view.pages.admin.onApplyAuth = function(method) {
				var props = {
					"auth.method" : method
				};
				if (method === 'ldap') {
					props['ldap.schema.type'] = _view.getRadioValue('ldap.schema.type');
					_fillConfigPropsText(props, ['auth.ldap.host', 'auth.ldap.port', 'auth.ldap.basedn', 'auth.ldap.admin-dn', 'auth.ldap.admin-password', 'ldap.schema.user-id-number-field', 'ldap.schema.user-card-number-field']);
					_fillConfigPropsYN(props, ['auth.ldap.use-ssl']);
					_fillConfigPropsRadio(props, ['ldap.user-card-number.first-byte', 'ldap.user-card-number.format']);
				}
				_saveConfigProps(props);
			};

			_view.pages.admin.onUserSourceGroupApply = function() {
				var val = $("#user-source\\.group").val();
				_saveConfigProps({
					'user-source.group' : val
				});
				$('#user-source-group-txt').html(val);
				_view.pages.admin.onUserSourceGroupCancel();
			};

			_view.pages.admin.onUserSourceGroupAll = function() {
				_saveConfigProps({
					'user-source.group' : ''
				});
				$('#user-source-group-txt').html($('#user-source-group-all').text());
				_view.pages.admin.onUserSourceGroupCancel();
			};

			_view.pages.admin.onUserSourceGroupCancel = function() {
				$('.user-source-group-display').show();
				$('.user-source-group-edit').hide();
			};

			_view.pages.admin.onUserSourceGroupEdit = function() {

				var res, html;

				$('.user-source-group-display').hide();
				$('.user-source-group-edit').show();

				res = _api.call({
					request : 'user-source-groups'
				});

				if (res.result.code !== '0') {
					_view.showApiMsg(res);
				} else {
					html = '';
					$.each(res.groups, function(key, val) {
						html += '<option value="' + val + '">' + val + '</option>';
					});
					$('#user-source\\.group').empty().append(html);
					$('#user-source\\.group').selectmenu("refresh");
				}
			};

			_view.pages.admin.onUserSyncApply = function() {
				var props = {};
				_fillConfigPropsYN(props, ['user-source.update-user-details', 'schedule.auto-sync.user']);
				_saveConfigProps(props);
			};

			_userSync = function(test) {
				$('#user-sync-messages').html('');
				_view.showApiMsg(_api.call({
					request : 'user-sync',
					test : test,
					'delete-users' : _view.isCheckedYN($("#sync-delete-users"))
				}));
				_view.checkCb('#sync-delete-users', false);
			};

			_view.pages.admin.onUserSyncNow = function() {
				_userSync('N');
			};

			_view.pages.admin.onUserSyncTest = function() {
				_userSync('Y');
			};

			_view.pages.admin.onApplySmtp = function(security) {
				var props = {};
				props['mail.smtp.security'] = security;
				_fillConfigPropsText(props, ['mail.smtp.host', 'mail.smtp.port', 'mail.smtp.username', 'mail.smtp.password']);
				_saveConfigProps(props);

			};

			_view.pages.admin.onApplyMail = function() {
				var props = {};
				_fillConfigPropsText(props, ['mail.from.address', 'mail.from.name', 'mail.reply.to.address', 'mail.reply.to.name']);
				_saveConfigProps(props);
			};

			_view.pages.admin.onVoucherDeleteBatch = function(batch) {
				_view.showApiMsg(_api.call({
					request : "account-voucher-batch-delete",
					batch : batch
				}));
				_view.pages.admin.refreshAccountVouchers();
			};

			_view.pages.admin.onVoucherExpireBatch = function(batch) {
				_view.showApiMsg(_api.call({
					request : "account-voucher-batch-expire",
					batch : batch
				}));
				_view.pages.admin.refreshAccountVouchers();
			};

			_view.pages.admin.onDownload = function(request, data, requestSub) {
				_api.download(request, data, requestSub);
			};

			_view.pages.admin.onVoucherDeleteExpired = function() {
				_view.showApiMsg(_api.call({
					request : "account-voucher-delete-expired"
				}));
				_view.pages.admin.refreshAccountVouchers();
			};

			_view.pages.admin.onTestMail = function() {
				_view.showApiMsg(_api.call({
					request : 'mail-test',
					mailto : $('#mail-test-address').val()
				}));
			};

			_view.pages.admin.onApplyInternalUsers = function(enabled) {
				var props = {};
				_fillConfigPropsYN(props, ['internal-users.enable', 'internal-users.user-can-change-password']);
				_saveConfigProps(props);
			};

			_view.pages.admin.onFlipswitchWebPrint = function(enabled) {
				var props = {};
				props['web-print.enable'] = enabled ? 'Y' : 'N';
				_saveConfigProps(props);
			};

			_view.pages.admin.onApplyWebPrint = function(enabled) {
				var props = {};
				props['web-print.enable'] = enabled ? 'Y' : 'N';
				if (enabled) {
					_fillConfigPropsText(props, ['web-print.max-file-mb', 'web-print.limit-ip-addresses']);
				}
				_saveConfigProps(props);
			};

			_view.pages.admin.onApplySmartSchool = function(enable1, enable2) {
				var props = {};

				props['smartschool.1.enable'] = enable1 ? 'Y' : 'N';
				props['smartschool.2.enable'] = enable2 ? 'Y' : 'N';

				_fillConfigPropsYN(props, ['smartschool.user.insert.lazy-print']);

				if (enable1) {
					_fillConfigPropsText(props, ['smartschool.1.soap.print.endpoint.url', 'smartschool.1.soap.print.endpoint.password', 'smartschool.1.soap.print.proxy-printer']);
				}
				if (enable2) {
					_fillConfigPropsText(props, ['smartschool.2.soap.print.endpoint.url', 'smartschool.2.soap.print.endpoint.password', 'smartschool.2.soap.print.proxy-printer']);
				}
				_saveConfigProps(props);
			};

			_view.pages.admin.onApplySmartSchoolPaperCut = function(enable) {
				var props = {};

				props['smartschool.papercut.enable'] = enable ? 'Y' : 'N';

				if (enable) {
					_fillConfigPropsText(props, ['papercut.server.host', 'papercut.server.port', 'papercut.webservices.auth-token', 'papercut.db.jdbc-url', 'papercut.db.user', 'papercut.db.password']);
				}
				_saveConfigProps(props);
			};

			_view.pages.admin.onApplyGcpEnable = function(_panel, enabled) {

				var res = _api.call({
					request : 'gcp-set-details',
					enabled : enabled,
					clientId : $('#gcp-client-id').val(),
					clientSecret : $('#gcp-client-secret').val(),
					printerName : $('#gcp-printer-name').val()
				});

				_view.showApiMsg(res);

				if (res.result.code === '0') {
					_updateGcpState(_panel, res);
				}
			};

			_view.pages.admin.onApplyGcpNotification = function() {
				var res = _api.call({
					request : 'gcp-set-notifications',
					enabled : _view.isCbChecked($('#gcp-mail-after-cancel-enable')),
					emailSubject : $('#gcp-mail-after-cancel-subject').val(),
					emailBody : $('#gcp-mail-after-cancel-body').val()
				});
				_view.showApiMsg(res);
				return false;
			};

			_updateGcpState = function(_panel, apiRes) {
				$('#gcp-printer-state-display').html(apiRes.displayState);
				$('#gcp-printer-state').text(apiRes.state);
				$('#gcp-summary').table('refresh');
				_model.gcp.state = apiRes.state;
				_panel.Options.onGcpRefresh(_panel.Options);
			};

			_view.pages.admin.onApplyGcpOnline = function(_panel, online) {
				var res = _api.call({
					request : 'gcp-online',
					online : online
				});

				_view.showApiMsg(res);

				if (res.result.code === '0') {
					_updateGcpState(_panel, res);
				}

				return false;
			};

			_view.pages.admin.onRefreshGcp = function(_panel) {

				var res = _api.call({
					request : 'gcp-get-details'
				});

				if (res.result.code === '0') {

					$('#gcp-client-id').val(res.clientId);
					$('#gcp-client-secret').val(res.clientSecret);

					$('#gcp-printer-name').val(res.printerName);
					$('#gcp-summary-printer-name').html(res.printerName);

					$('#gcp-printer-owner').html(res.ownerId);

					_updateGcpState(_panel, res);

				} else {
					_view.showApiMsg(res);
				}
			};

			_view.pages.admin.onRegisterGcp = function() {

				var res = _api.call({
					request : 'gcp-register',
					clientId : $('#gcp-client-id').val(),
					clientSecret : $('#gcp-client-secret').val(),
					printerName : $('#gcp-printer-name').val()
				});

				if (res.result.code === '0') {
					window.open(res.complete_invite_url, '', 'top=400,left=400,width=800,height=400,modal=yes,location=0,menubar=0,status=0,titlebar=0,toolbar=0');
				} else {
					_view.showApiMsg(res);
				}
			};

			_view.pages.admin.onApplyProxyPrint = function() {

				var props = {};

				_fillConfigPropsYN(props, ['proxy-print.non-secure', 'webapp.user.proxy-print.clear-inbox']);

				if (props['proxy-print.non-secure'] === 'Y') {
					_fillConfigPropsText(props, ['proxy-print.non-secure-printer-group']);
				}

				_fillConfigPropsText(props, ['proxy-print.fast-expiry-mins', 'proxy-print.hold-expiry-mins', 'proxy-print.direct-expiry-secs', 'webapp.user.proxy-print.max-copies']);

				_saveConfigProps(props);
			};

			_view.pages.admin.onApplyFinancialGeneral = function() {
				var props = {};
				_fillConfigPropsText(props, ["financial.global.credit-limit", "financial.printer.cost-decimals", "financial.user.balance-decimals"]);
				_saveConfigProps(props);
			};

			_view.pages.admin.onChangeFinancialCurrencyCode = function() {
				var props = {};
				_fillConfigPropsText(props, ["financial.global.currency-code"]);
				_saveConfigProps(props);
			};

			_view.pages.admin.onApplyFinancialPos = function() {
				var props = {}
				//
				;
				_fillConfigPropsText(props, ["financial.pos.payment-methods", "financial.pos.receipt-header"]);
				_saveConfigProps(props);
			};

			_view.pages.admin.onApplyFinancialVouchers = function(cardFontFamily) {
				var props = {
					"financial.voucher.card-font-family" : cardFontFamily
				};
				_fillConfigPropsYN(props, ['financial.user.vouchers.enable']);
				_fillConfigPropsText(props, ["financial.voucher.card-header", "financial.voucher.card-footer"]);
				_saveConfigProps(props);
			};

			_view.pages.admin.onApplyFinancialUserTransfers = function() {
				var props = {};
				_fillConfigPropsYN(props, ['financial.user.transfers.enable', 'financial.user.transfers.enable-comments']);
				_saveConfigProps(props);
			};

			_view.pages.admin.onApplyImap = function(enabled, security) {
				var props = {};
				props['print.imap.enable'] = enabled ? 'Y' : 'N';
				if (enabled) {
					props['print.imap.security'] = security;
					_fillConfigPropsText(props, ['print.imap.host', 'print.imap.port', 'print.imap.user', 'print.imap.password', 'print.imap.folder.inbox', 'print.imap.folder.trash', 'print.imap.max-file-mb', 'print.imap.max-files']);
				}
				_saveConfigProps(props);
			};

			_view.pages.admin.onFlatRequest = function(request) {
				var res = _api.call({
					request : request
				});
				_view.showApiMsg(res);
				return res;
			};

			_view.pages.admin.onPaymentGatewayOnline = function (bitcoin, online) {
				_view.showApiMsg(_api.call({
					request : "payment-gateway-online",
					bitcoin : bitcoin,
					online : online
				}));
			}; 

			_view.pages.admin.onApplyUserCreate = function() {
				var props = {};
				_fillConfigPropsYN(props, ['user.insert.lazy-login', 'user.insert.lazy-print']);
				_saveConfigProps(props);
			};

			_view.pages.admin.onApplyUserAuthModeLocal = function() {
				var props = {};
				_fillConfigPropsYN(props, ['auth-mode.name', 'auth-mode.name.show', 'auth-mode.id', 'auth-mode.id.show', 'auth-mode.id.is-masked', 'auth-mode.id.pin-required', 'auth-mode.card-local', 'auth-mode.card-local.show', 'auth-mode.card.pin-required', 'auth-mode.card.self-association', 'user.can-change-pin', 'webapp.user.auth.trust-cliapp-auth']);
				_fillConfigPropsRadio(props, ['auth-mode-default', 'card.number.format', 'card.number.first-byte']);
				_saveConfigProps(props);
			};

			_view.pages.admin.onApplyBackupAuto = function() {
				var props = {};
				_fillConfigPropsYN(props, ['system.backup.enable-automatic', 'delete.doc-log', 'delete.app-log', 'delete.account-trx-log']);
				_fillConfigPropsText(props, ['system.backup.days-to-keep', 'delete.doc-log.days', 'delete.app-log.days', 'delete.account-trx-log.days']);
				_saveConfigProps(props);
			};

			_view.pages.admin.onBitcoinWalletRefresh = function() {
				_view.showApiMsg(_api.call({
					request : 'bitcoin-wallet-refresh'
				}));
			};

			_view.pages.admin.onPagometerReset = function() {
				var scope = []
				//
				, sfxs = ['dashboard', 'queues', 'printers', 'users']
				//
				, i = 0;

				$.each(sfxs, function(key, sfx) {
					var val = '#pagometer-reset-' + sfx;
					if ($(val).is(':checked')) {
						scope[i] = $(val).val();
						_view.checkCb(val, false);
						i = i + 1;
					}
				});
				if (i > 0) {
					_view.showApiMsg(_api.call({
						request : 'pagometer-reset',
						scope : JSON.stringify(scope)
					}));
				}
			};

			_view.pages.admin.onApplyLocale = function() {
				var props = {};
				_fillConfigPropsText(props, ['system.default-locale']);
				_saveConfigProps(props);
			};

			_view.pages.admin.onApplyPaperSize = function() {
				_saveConfigProps({
					'system.default-papersize' : _view.getRadioValue('system.default-papersize')
				});
			};

			_view.pages.admin.onApplyReportsPdfFontFamily = function(fontFamily) {
				_saveConfigProps({
					'reports.pdf.font-family' : fontFamily
				});
			};

			_view.pages.admin.onApplyConverters = function() {
				var props = {};
				_fillConfigPropsYN(props, ['doc.convert.libreoffice-enabled', 'doc.convert.xpstopdf-enabled']);
				_saveConfigProps(props);
			};

			_view.pages.admin.onApplyPrintIn = function() {
				var props = {};
				_fillConfigPropsYN(props, ['print-in.allow-encrypted-pdf']);
				_saveConfigProps(props);
			};

			_view.pages.admin.onCreateUser = function() {
				_model.editUser = {
					dbId : null,
					userName : _model.intUserIdPfx,
					fullName : '',
					email : '',
					emailOther : [],
					admin : false,
					person : true,
					disabled : false,
					internal : true,
					password : '',
					accounting : {}
				};
				_view.pages.user.loadShowAsync();
			};

			_view.pages.admin.onEditUser = function(uid) {
				var res = _api.call({
					request : 'user-get',
					s_user : uid
				});
				if (res.result.code === '0') {
					_model.editUser = res.userDto;
					_view.pages.user.loadShowAsync(function() {
						$('#user-userid-txt').html(_model.editUser.userName);
					});
				} else {
					_view.showApiMsg(res);
				}
			};

			/**
			 *
			 */
			_view.pages.user.onSaveUser = function() {

				var res, emailOther = [], accounting = {};

				//
				$.each($('#user-email-other').val().split("\n"), function(key, val) {
					var address = val.trim();
					if (address.length > 0) {
						emailOther.push({
							address : address
						});
					}
				});
				_model.editUser.emailOther = emailOther;

				//
				accounting.balance = $('#user-account-balance').val();
				accounting.creditLimit = _view.getRadioValue("user-account-credit-limit-type");
				accounting.creditLimitAmount = $('#user-account-credit-limit-amount').val();

				_model.editUser.accounting = accounting;

				//
				if (!_model.editUser.dbId) {
					if (!_view.checkPwMatch($('#user-user-pw'), $('#user-user-pw-confirm'))) {
						return;
					}
					_model.editUser.userName = $('#user-userid').val();
					_model.editUser.password = $('#user-user-pw').val();
				}

				_model.editUser.email = $('#user-email').val();
				_model.editUser.fullName = $('#user-fullname').val();
				_model.editUser.admin = $('#user-isadmin').is(':checked');
				_model.editUser.person = $('#user-isperson').is(':checked');
				_model.editUser.disabled = $('#user-disabled').is(':checked');
				_model.editUser.card = $('#user-card-number').val();
				_model.editUser.id = $('#user-id-number').val();
				_model.editUser.pin = $('#user-pin').val();

				res = _api.call({
					request : 'user-set',
					userDto : JSON.stringify(_model.editUser)
				});

				_view.showApiMsg(res);

				/*
				 * Refresh, so any change is displayed
				 */
				if (res.result.code === '0') {

					if (_model.editUser.dbId) {
						if (_api.call({
							request : "user-notify-account-change",
							dto : JSON.stringify({
								key : _model.editUser.dbId
							})
						}).result.code !== '0') {
							_view.showApiMsg(res);
						}
					}

					/*
					 * Close dialog. Do NOT use $('#button-cancel-user').click();
					 */
					_view.changePage($('#page-admin'));
					_view.pages.admin.refreshUsers();
				}
			};

			_view.pages.user.onDeleteUser = function() {

				var res = _api.call({
					request : 'user-delete',
					id : _model.editUser.dbId,
					userid : _model.editUser.userName
				});
				_view.showApiMsg(res);

				/*
				 * Close dialog. Do NOT use $('#button-cancel-user').click();
				 */
				_view.changePage($('#page-admin'));

				/*
				 * Refresh, so any change is displayed
				 */
				if (res.result.code === '0') {
					_view.pages.admin.refreshUsers();
				}
			};

			_view.pages.admin.onEditConfigProp = function(name) {

				var res = _api.call({
					request : 'config-get-prop',
					name : name
				});
				if (res.result.code === '0') {
					_model.editConfigProp = res.j_prop;
					_view.pages.configProp.loadShowAsync(function() {
						$('#config-prop-name').html(_model.editConfigProp.name);
					});
				} else {
					_view.showApiMsg(res);
				}
			};

			_view.pages.voucherCreate.onCreateBatchExit = function() {
				_view.pages.admin.refreshAccountVouchers();
			};

			_view.pages.voucherCreate.onCreateBatch = function(dto, design) {

				var res = _api.call({
					request : "account-voucher-batch-create",
					dto : JSON.stringify(dto)
				});

				_view.showApiMsg(res);

				if (res.result.code === '0') {
					// VoucherBatchPrintDto
					_api.download("account-voucher-batch-print", null, JSON.stringify({
						batchId : dto.batchId,
						design : design
					}));
				}
			};

			_view.pages.configProp.onSave = function() {

				var prop = {}, ok
				//
				, sel = (_model.editConfigProp.multiline ? '#config-prop-value-multiline' : '#config-prop-value')
				//
				;

				_model.editConfigProp.value = $(sel).val();

				prop[(_model.editConfigProp.name)] = _model.editConfigProp.value;
				ok = _saveConfigProps(prop);

				/*
				 * Refresh, so any change is displayed
				 */
				if (ok) {
					/*
					 * Close dialog. Do NOT use
					 * $('#button-cancel-configprop').click();
					 */
					_view.changePage($('#page-admin'));
					_view.pages.admin.refreshConfigProps();
				}
			};

			_view.pages.admin.onCreateQueue = function() {
				_model.editQueue = {
					id : null,
					urlpath : 'untitled',
					ipallowed : '',
					trusted : true,
					disabled : false,
					deleted : false
				};
				_view.pages.queue.loadShowAsync(function() {
					$('#title-queue').html(_model.editQueue.urlpath);
				});
			};

			_view.pages.admin.onEditQueue = function(id) {
				var res = _api.call({
					request : 'queue-get',
					id : id
				});
				if (res && res.result.code === '0') {
					_model.editQueue = res.j_queue;
					_view.pages.queue.loadShowAsync(function() {
						$('#title-queue').html(_model.editQueue.urlpath);
					});
				} else {
					_view.showApiMsg(res);
				}
			};

			_view.pages.queue.onSaveQueue = function() {

				_model.editQueue.urlpath = $('#queue-url-path').val();
				_model.editQueue.ipallowed = $('#queue-ip-allowed').val();
				_model.editQueue.trusted = $('#queue-trusted').is(':checked');
				_model.editQueue.disabled = $('#queue-disabled').is(':checked');
				_model.editQueue.deleted = $('#queue-deleted').is(':checked');

				var res = _api.call({
					request : 'queue-set',
					j_queue : JSON.stringify(_model.editQueue)
				});

				_view.showApiMsg(res);

				/*
				 * Refresh, so any change is displayed
				 */
				if (res.result.code === '0') {
					/*
					 * Close dialog. Do NOT use
					 * $('#button-cancel-queue').click();
					 */
					_view.changePage($('#page-admin'));
					_view.pages.admin.refreshQueues();
				}
			};

			//-------------------------------------------------------
			_view.pages.admin.onEditDevice = function(id) {
				var res = _api.call({
					request : 'device-get',
					id : id
				});
				if (res && res.result.code === '0') {
					_model.editDevice = res.j_device;
					_view.pages.device.loadShowAsync(function() {
						$('#title-device').html(_model.editDevice.deviceName);
					});
				} else {
					_view.showApiMsg(res);
				}
			};

			_view.pages.device.onDeleteDevice = function(device) {
				var res = _api.call({
					request : 'device-delete',
					id : device.id,
					deviceName : device.deviceName
				});

				_view.showApiMsg(res);

				/*
				 * Refresh, so any change is displayed
				 */
				if (res.result.code === '0') {
					/*
					 * Close dialog. Do NOT use
					 * $('#button-cancel-device').click();
					 */
					_view.changePage($('#page-admin'));
					_view.pages.admin.refreshDevices();
				}
			};

			_view.pages.device.onSaveDevice = function(device) {
				var res = _api.call({
					request : 'device-set',
					j_device : JSON.stringify(device)
				});

				_view.showApiMsg(res);

				/*
				 * Refresh, so any change is displayed
				 */
				if (res.result.code === '0') {
					/*
					 * Close dialog. Do NOT use
					 * $('#button-cancel-device').click();
					 */
					_view.changePage($('#page-admin'));
					_view.pages.admin.refreshDevices();
				}
			};

			_view.pages.admin.onCreateDeviceTerminal = function() {

				var res = _api.call({
					request : 'device-new-terminal'
				});

				if (res && res.result.code === '0') {

					_model.editDevice = res.j_device;

					_view.pages.device.loadShowAsync(function() {
						$('#title-device').html(_model.editDevice.deviceName);
					});

				} else {
					_view.showApiMsg(res);
				}

			};

			_view.pages.admin.onCreateDeviceCardReader = function() {
				var res = _api.call({
					request : 'device-new-card-reader'
				});

				if (res && res.result.code === '0') {

					_model.editDevice = res.j_device;

					_view.pages.device.loadShowAsync(function() {
						$('#title-device').html(_model.editDevice.deviceName);
					});

				} else {
					_view.showApiMsg(res);
				}
			};

			//-------------------------------------------------------
			_view.pages.admin.onPrinterSynchr = function(id) {
				_view.showApiMsg(_api.call({
					request : 'printer-sync'
				}));
				$('#button-printers-apply').click();
			};

			_view.pages.admin.onEditPrinter = function(id) {
				var res = _api.call({
					request : 'printer-get',
					id : id
				});
				if (res && res.result.code === '0') {
					_model.editPrinter = res.j_printer;
					_view.pages.printer.loadShowAsync(function() {
						$('#sp-printer-page-name').html(_model.editPrinter.printerName);
					});
				} else {
					_view.showApiMsg(res);
				}
			};

			_view.pages.printer.onSavePrinter = function() {

				_model.editPrinter.displayName = $('#printer-displayname').val();
				_model.editPrinter.location = $('#printer-location').val();
				_model.editPrinter.printerGroups = $('#printer-printergroups').val();

				_model.editPrinter.disabled = $('#printer-disabled').is(':checked');
				_model.editPrinter.deleted = $('#printer-deleted').is(':checked');

				var res = _api.call({
					request : 'printer-set',
					j_printer : JSON.stringify(_model.editPrinter)
				});

				_view.showApiMsg(res);

				if (res.result.code === '0') {
					/*
					 * Close dialog. Do NOT simulate a click event like this:
					 * $('#button-cancel-printer').click();
					 * ... since it executes async (?), i.e. the
					 * refreshPrinters()
					 * is performed while the edit dialog is still in view: this
					 * makes the rendering of the 'html' sparklines fail.
					 */
					_view.changePage($('#page-admin'));
					/*
					 * Refresh, so any change is displayed
					 */
					_view.pages.admin.refreshPrinters();
				}
			};

			/**
			 *
			 */
			_view.pages.printer.onSavePrinterMediaSources = function() {

				var res
				//
				, SOURCE_AUTO = 'auto', SOURCE_MANUAL = 'manual'
				// org.savapage.dto.ProxyPrinterMediaSourcesDto
				, dto = {}
				//
				, sourceAuto = _view.isCbChecked($('#media-source\\.' + SOURCE_AUTO))
				//
				, sourceManual = _view.isCbChecked($('#media-source\\.' + SOURCE_MANUAL))
				//
				, selDefaultMonochrome = $('#sp-printer-use-monochrome-as-default')
				//
				;

				dto.id = _model.editPrinter.id;
				dto.language = _model.language;
				dto.country = _model.country;

				if (selDefaultMonochrome) {
					dto.defaultMonochrome = _view.isCbChecked(selDefaultMonochrome);
				}

				if (sourceAuto) {
					dto.auto = {
						active : true,
						source : SOURCE_AUTO,
						display : $('#media-source-' + SOURCE_AUTO + '-display').val()
					};
				}
				if (sourceManual) {
					dto.manual = {
						active : true,
						source : SOURCE_MANUAL,
						display : $('#media-source-' + SOURCE_MANUAL + '-display').val()
					};
				}

				dto.sources = [];

				$(".sp-printer-media-source-row").each(function() {

					var cb = $(this).find('.sp-printer-media-source')
					//
					, active = _view.isCbChecked(cb)
					//
					, source = cb.attr('id').substr("media-source.".length)
					//
					, nextRow = $(this).next()
					//
					, display = nextRow.find('input:text').val()
					//
					, media = nextRow.find('select').val()
					//
					;

					// push: org.savapage.core.dto.IppMediaSourceDto
					dto.sources.push({
						active : active,
						source : source,
						display : display,
						media : {
							active : active,
							media : media,
							cost : {
								oneSided : {
									grayscale : $(this).find('.sp-printer-cost-1-g').val(),
									color : $(this).find('.sp-printer-cost-1-c').val()
								},
								twoSided : {
									grayscale : $(this).find('.sp-printer-cost-2-g').val(),
									color : $(this).find('.sp-printer-cost-2-c').val()
								}
							}
						}
					});

				});

				/*
				 *
				 */
				res = _api.call({
					request : 'printer-set-media-sources',
					j_media_sources : JSON.stringify(dto)
				});

				_view.showApiMsg(res);
				if (res.result.code === '0') {
					_view.changePage($('#page-admin'));
					_view.pages.admin.refreshPrinters();
				}

			};

			/**
			 *
			 */
			_view.pages.printer.onSavePrinterCost = function() {
				var res
				// org.savapage.dto.ProxyPrinterCostDto
				, dto = {}
				//
				;

				dto.id = _model.editPrinter.id;

				dto.language = _model.language;
				dto.country = _model.country;

				dto.chargeType = _view.getRadioValue('sp-printer-charge-type');
				dto.defaultCost = $('#sp-printer-default-cost').val();
				dto.mediaCost = [];

				$(".sp-printer-cost-media-row").each(function() {

					var cb = $(this).find('.sp-printer-cost-media')
					//
					, media = cb.attr('id').substr("cost.media.".length)
					//
					;

					dto.mediaCost.push({
						media : media,
						active : _view.isCbChecked(cb),
						cost : {
							oneSided : {
								grayscale : $(this).find('.sp-printer-cost-1-g').val(),
								color : $(this).find('.sp-printer-cost-1-c').val()
							},
							twoSided : {
								grayscale : $(this).find('.sp-printer-cost-2-g').val(),
								color : $(this).find('.sp-printer-cost-2-c').val()
							}
						}
					});
				});

				/*
				 *
				 */
				res = _api.call({
					request : 'printer-set-media-cost',
					j_cost : JSON.stringify(dto)
				});

				_view.showApiMsg(res);
				if (res.result.code === '0') {
					_view.changePage($('#page-admin'));
					_view.pages.admin.refreshPrinters();
				}

			};

			_view.pages.printer.onRenamePrinter = function(id, name, replace) {

				var res = _api.call({
					request : 'printer-rename',
					j_printer : JSON.stringify({
						id : id,
						name : name,
						replace : replace
					})
				});
				_view.showApiMsg(res);
				/*
				 * Refresh, so any change is displayed
				 */
				if (res.result.code === '0') {
					_view.changePage($('#page-admin'));
					_view.pages.admin.refreshPrinters();
				}
			};

			_view.pages.admin.onBackupNow = function() {

				//var msgSaved = $.mobile.loadingMessage;

				$('#overlay-curtain').show();

				_api.callAsync({
					request : 'db-backup',
					user : _model.user.id
				}, function(data) {
					_view.showApiMsg(data);
				}, function() {
					//$.mobile.hidePageLoadingMsg();
					//$.mobile.loadingMessage = msgSaved;
					$('#overlay-curtain').hide();
				}, function(data) {
					_view.showApiMsg(data);
				});
			};

			/**
			 * For both internal admin and jmx password.
			 */
			_view.pages.admin.commonPwReset = function(request, selNew, selConfirm) {
				if (selNew.val() !== selConfirm.val()) {
					_view.message(_i18n.format('msg-input-mismatch'));
				} else if (!$.trim(selNew.val())) {
					_view.message(_i18n.format('msg-input-empty'));
				} else {
					_view.showApiMsg(_api.call({
						request : request,
						password : selNew.val()
					}));
					selNew.val('');
					selConfirm.val('');
				}
				return false;
			};

			_view.pages.admin.onAdminPwReset = function() {
				return _view.pages.admin.commonPwReset('reset-admin-pw', $('#admin-pw-new'), $('#admin-pw-confirm'));
			};

			_view.pages.admin.onJmxPwReset = function() {
				return _view.pages.admin.commonPwReset('reset-jmx-pw', $('#jmx-pw-new'), $('#jmx-pw-confirm'));
			};

			/**
			 *
			 */
			_cometd.onConnecting = function() {
				$.noop();
			};
			/**
			 *
			 */
			_cometd.onDisconnecting = function() {
				$.noop();
			};

			/**
			 *
			 */
			_cometd.onHandshakeSuccess = function() {

				_cometd.subscribe('/admin/**', function(message) {
					//
					var nItems = _model.pubMsgStack.length
					//
					, data = $.parseJSON(message.data);

					if ($('#user-sync-messages') && data.topic === 'user-sync') {
						$('#user-sync-messages').append('<div>' + data.msg + '<div>');
					}

					_model.pubMsgStack.unshift(data);
					nItems += 1;

					if (nItems > _model.MAX_PUB_MSG) {
						_model.pubMsgStack.pop();
					}

					if ($('#live-messages')) {

						$('#live-messages').prepend(_model.pubMsgAsHtml(data));

						if (nItems > _model.MAX_PUB_MSG) {
							$('#live-messages :last-child').remove();
						}
					}
				});

			};

			_cometd.onHandshakeFailure = function() {
				$.noop();
			};
			_cometd.onConnectionClosed = function() {
				$.noop();
			};
			_cometd.onReconnect = function() {
				$.noop();
			};
			_cometd.onConnectionBroken = function() {
				$.noop();
			};
			_cometd.onUnsuccessful = function() {
				$.noop();
			};

		};
		/**
		 *
		 */
		_ns.Model = function(_i18n) {

			var _LOC_AUTH_NAME = 'sp.auth.admin.name'
			//
			, _LOC_AUTH_TOKEN = 'sp.auth.admin.token'
			//
			, _LOC_LANG = 'sp.admin.language'
			//
			, _LOC_COUNTRY = 'sp.admin.country'
			//
			;

			this.MAX_PUB_MSG = 20;

			this.pubMsgStack = [];

			/**
			 * Escape special characters to HTML entities.
			 */
			this.textAsHtml = function(text) {
				return $("<dummy/>").text(text).html();
			};

			this.pubMsgAsHtml = function(data) {
				var sfx = (data.level === "ERROR") ? "error" : (data.level === "WARN") ? "warn" : (data.level === "INFO") ? "info" : "valid";
				return "<div class='sp-txt-wrap sp-txt-" + sfx + "'>" + this.textAsHtml(data.time + ' | ' + data.msg) + '<div>';
			};

			this.user = new _ns.User();

			this.authToken = {};

			this.gcp = {};

			this.sessionExpired = false;

			this.startSession = function() {
				$.noop();
			};

			this.initAuth = function() {
				var item;

				this.authToken = {};

				item = _LOC_AUTH_NAME;
				if (window.localStorage[item] !== null) {
					this.authToken.user = window.localStorage[item];
				}

				item = _LOC_AUTH_TOKEN;
				if (window.localStorage[item] !== null) {
					this.authToken.token = window.localStorage[item];
				}

				item = _LOC_LANG;
				if (window.localStorage[item] !== null) {
					this.authToken.language = window.localStorage[item];
				}

				item = _LOC_COUNTRY;
				if (window.localStorage[item] !== null) {
					this.authToken.country = window.localStorage[item];
				}
			};

			this.setLanguage = function(lang) {
				this.authToken.language = lang;
				window.localStorage[_LOC_LANG] = lang;
			};

			this.setCountry = function(country) {
				this.authToken.country = country;
				window.localStorage[_LOC_COUNTRY] = country;
			};

			this.setAuthToken = function(user, token, language, country) {
				var item;

				item = _LOC_AUTH_NAME;
				this.authToken.user = user;
				window.localStorage[item] = user;

				item = _LOC_AUTH_TOKEN;
				this.authToken.token = token;
				window.localStorage[item] = token;

				if (language) {
					this.setLanguage(language);
				}
				if (country) {
					this.setCountry(country);
				}
			};

		};
		/**
		 *
		 */
		$.SavaPageAdm = function(name) {

			var _i18n = new _ns.I18n()
			//
			, _model = new _ns.Model(_i18n)
			//
			, _api = new _ns.Api(_i18n, _model.user)
			//
			, _view = new _ns.View(_i18n, _api)
			//
			, _cometd, _ctrl;

			// convenience for panels

			_view.pages = {
				language : new _ns.PageLanguage(_i18n, _view, _model),
				login : new _ns.PageLogin(_i18n, _view, _api),
				admin : new _ns.PageAdmin(_i18n, _view, _model),
				membercard_upload : new _ns.PageMemberCardUpload(_i18n, _view, _model),
				user : new _ns.PageUser(_i18n, _view, _model),
				userPwReset : new _ns.PageUserPasswordReset(_i18n, _view, _model),
				configProp : new _ns.PageConfigProp(_i18n, _view, _model),
				queue : new _ns.PageQueue(_i18n, _view, _model),
				printer : new _ns.PagePrinter(_i18n, _view, _model),
				device : new _ns.PageDevice(_i18n, _view, _model),
				voucherCreate : new _ns.PageAccountVoucherCreate(_i18n, _view, _model),
				pointOfSale : new _ns.PagePointOfSale(_i18n, _view, _model, _api)
			};

			_ns.PanelDashboard.model = _model;
			_ns.PanelOptions.model = _model;

			_cometd = new _ns.Cometd();
			_ctrl = new _ns.Controller(_i18n, _model, _view, _api, _cometd);

			/**
			 *
			 */
			this.init = function() {

				_ctrl.init();

				if (_model.authToken.user && _model.authToken.token) {
					_ctrl.login(_view.AUTH_MODE_NAME, _model.authToken.user, null, _model.authToken.token);
				} else {
					// Initial load/show of Login dialog
					_view.pages.login.loadShowAsync();
				}
			};

			$(window).on('beforeunload', function() {
				// By NOT returning anything the unload dialog will not show.
				$.noop();
			}).on('unload', function() {
				_api.removeCallbacks();
				_api.call({
					request : 'webapp-unload'
				});
			});
		};

		$(function() {
			$.savapageAdm = new $.SavaPageAdm();
			// do NOT initialize here (to early for some browsers, like Opera)
		});

		/*
		* Do NOT use this global construct since it will show loading spinner
		* during CometD long poll.
		*/
		//    $(document).ajaxSend(function() {
		//        $.mobile.loading( 'show');
		//    });
		//    $(document).ajaxComplete(function() {
		//        $.mobile.loading( 'hide');
		//    });

		$(document).on("mobileinit", null, null, function() {
			$.mobile.defaultPageTransition = "none";
			$.mobile.defaultDialogTransition = "none";
		});
		// Initialize AFTER document is read
		$(document).on("ready", null, null, function() {
			$.savapageAdm.init();
		});

	}(jQuery, this, this.document, JSON, this.org.savapage));
