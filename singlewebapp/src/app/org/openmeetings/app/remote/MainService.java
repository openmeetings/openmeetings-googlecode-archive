package org.openmeetings.app.remote;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openmeetings.app.data.basic.AuthLevelmanagement;
import org.openmeetings.app.data.basic.Configurationmanagement;
import org.openmeetings.app.data.basic.Navimanagement;
import org.openmeetings.app.data.basic.Sessionmanagement;
import org.openmeetings.app.data.basic.dao.LdapConfigDaoImpl;
import org.openmeetings.app.data.basic.dao.OmTimeZoneDaoImpl;
import org.openmeetings.app.data.basic.dao.SOAPLoginDaoImpl;
import org.openmeetings.app.data.conference.Feedbackmanagement;
import org.openmeetings.app.data.conference.Invitationmanagement;
import org.openmeetings.app.data.conference.Roommanagement;
import org.openmeetings.app.data.logs.ConferenceLogDaoImpl;
import org.openmeetings.app.data.user.Statemanagement;
import org.openmeetings.app.data.user.Usermanagement;
import org.openmeetings.app.data.user.dao.UsersDaoImpl;
import org.openmeetings.app.ldap.LdapLoginManagement;
import org.openmeetings.app.persistence.beans.adresses.States;
import org.openmeetings.app.persistence.beans.basic.Configuration;
import org.openmeetings.app.persistence.beans.basic.LdapConfig;
import org.openmeetings.app.persistence.beans.basic.Naviglobal;
import org.openmeetings.app.persistence.beans.basic.OmTimeZone;
import org.openmeetings.app.persistence.beans.basic.RemoteSessionObject;
import org.openmeetings.app.persistence.beans.basic.SOAPLogin;
import org.openmeetings.app.persistence.beans.basic.Sessiondata;
import org.openmeetings.app.persistence.beans.recording.RoomClient;
import org.openmeetings.app.persistence.beans.user.Userdata;
import org.openmeetings.app.persistence.beans.user.Users;
import org.openmeetings.app.remote.red5.ClientListManager;
import org.openmeetings.app.remote.red5.ScopeApplicationAdapter;
import org.openmeetings.app.rss.LoadAtomRssFeed;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.service.IServiceCapableConnection;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * 
 * @author swagner
 * 
 */
public class MainService implements IPendingServiceCallback {

	private static final Logger log = Red5LoggerFactory.getLogger(MainService.class, ScopeApplicationAdapter.webAppRootKey);

	@Autowired
	private ClientListManager clientListManager;
	@Autowired
	private ScopeApplicationAdapter scopeApplicationAdapter;
	@Autowired
	private Sessionmanagement sessionManagement;
	@Autowired
	private Configurationmanagement cfgManagement;
	@Autowired
	private Usermanagement userManagement;
	@Autowired
	private Statemanagement statemanagement;
	@Autowired
	private OmTimeZoneDaoImpl omTimeZoneDaoImpl;
	@Autowired
	private Navimanagement navimanagement;
	@Autowired
	private Roommanagement roommanagement;
	@Autowired
	private ConferenceLogDaoImpl conferenceLogDao;
	@Autowired
	private UsersDaoImpl usersDao;
	@Autowired
	private LdapConfigDaoImpl ldapConfigDao;
	@Autowired
	private SOAPLoginDaoImpl soapLoginDao;
	@Autowired
	private Invitationmanagement invitationManagement;
	@Autowired
	private Feedbackmanagement feedbackManagement;
	@Autowired
	private AuthLevelmanagement authLevelManagement;
	@Autowired
	private LoadAtomRssFeed loadAtomRssFeed;
	@Autowired
	private LdapLoginManagement ldapLoginManagement;

	// External User Types
	public static final String EXTERNAL_USER_TYPE_LDAP = "LDAP";

	/**
	 * get Navigation
	 * 
	 * @param SID
	 * @param language_id
	 * @return
	 */
	public List<Naviglobal> getNavi(String SID, long language_id, Long organisation_id) {
		try {
			Long user_id = sessionManagement.checkSession(SID);
			// log.error("getNavi 1: "+users_id);
			Long user_level = userManagement.getUserLevelByIdAndOrg(user_id,
					organisation_id);
			// log.error("getNavi 2: "+user_level);
			return navimanagement.getMainMenu(user_level, user_id, language_id);
		} catch (Exception err) {
			log.error("[getNavi] ", err);
		}
		return null;
	}

	/**
	 * gets a user by its SID
	 * 
	 * @param SID
	 * @param USER_ID
	 * @return
	 */
	public Users getUser(String SID, int USER_ID) {
		Users users = new Users();
		Long users_id = sessionManagement.checkSession(SID);
		long user_level = userManagement.getUserLevelByID(users_id);
		if (user_level > 2) {
			users = usersDao.getUser(new Long(USER_ID));
		} else {
			users.setFirstname("No rights to do this");
		}
		return users;
	}

	public RoomClient getCurrentRoomClient(String SID) {
		try {
			IConnection current = Red5.getConnectionLocal();
			String streamid = current.getClient().getId();

			log.debug("getCurrentRoomClient -1- " + SID);
			log.debug("getCurrentRoomClient -2- " + streamid);

			RoomClient currentClient = this.clientListManager
					.getClientByStreamId(streamid);
			return currentClient;
		} catch (Exception err) {
			log.error("[getCurrentRoomClient]", err);
		}
		return null;
	}

	/**
	 * This Method is jsut for testing you can find the corresponding CLietn
	 * Function in xmlcrm/auth/checkLoginData.lzx
	 * 
	 * @param myObject2
	 * @return
	 */
	public int testObject(Object myObject2) {
		try {
			@SuppressWarnings("rawtypes")
			LinkedHashMap myObject = (LinkedHashMap) myObject2;
			log.debug("testObject " + myObject.size());
			log.debug("testObject " + myObject.get(1));
			log.debug("testObject " + myObject.get("stringObj"));
			return myObject.size();
		} catch (Exception e) {
			log.error("ex: ", e);
		}
		return -1;
	}

	/**
	 * load this session id before doing anything else
	 * 
	 * @return a unique session identifier
	 */
	public Sessiondata getsessiondata() {

		log.debug(":: getsessiondata");
		System.out.println(":: getsessiondata");

		return sessionManagement.startsession();
	}

	public Long setCurrentUserOrganization(String SID, Long organization_id) {
		try {

			sessionManagement.updateUserOrg(SID, organization_id);

			return 1L;
		} catch (Exception err) {
			log.error("[setCurrentUserOrganization]", err);
		}
		return -1L;
	}

	public Users loginByRemember(String SID, String remoteHashId) {
		try {

			RoomClient currentClient;
			IConnection current = Red5.getConnectionLocal();

			Users o = null;

			currentClient = clientListManager.getClientByStreamId(current
					.getClient().getId());

			o = userManagement.loginUserByRemoteHash(SID, remoteHashId);

			if (o == null)
				return null;

			if (o.getOrganisation_users().isEmpty()) {
				throw new Exception("Users has no organization assigned");
			}

			o.setSessionData(sessionManagement.getSessionByHash(remoteHashId));
			currentClient.setUser_id(o.getUser_id());
			
			if (currentClient.getUser_id() != null
					&& currentClient.getUser_id() > 0) {

				currentClient.setFirstname(o.getFirstname());
				currentClient.setLastname(o.getLastname());

				Collection<Set<IConnection>> conCollection = current.getScope()
						.getConnections();
				for (Set<IConnection> conset : conCollection) {
					for (IConnection cons : conset) {
						if (cons != null) {
							RoomClient rcl = this.clientListManager
									.getClientByStreamId(cons.getClient()
											.getId());
							if (rcl == null) {
								// continue;
							} else if (rcl.getIsScreenClient() != null
									&& rcl.getIsScreenClient()) {
								// continue;
							} else {
								if (cons instanceof IServiceCapableConnection) {
									if (!cons.equals(current)) {
										// log.error("sending roomDisconnect to "
										// + cons);
										// RoomClient rcl =
										// this.clientListManager.getClientByStreamId(cons.getClient().getId());
										// Send to all connected users
										((IServiceCapableConnection) cons)
												.invoke("roomConnect",
														new Object[] { currentClient },
														this);
										// log.error("sending roomDisconnect to "
										// + cons);
									}
								}
							}
						}
					}
				}
			}

			return o;

		} catch (Exception err) {
			log.error("[loginByRemember]", err);
		}
		return null;
	}

	public Object webLoginUser(String SID, String usernameOrEmail,
			String Userpass, Boolean storePermanent, Long language_id,
			Long ldapConfigId) {

		Object returnValue = this.loginUser(SID, usernameOrEmail, Userpass,
				storePermanent, language_id, ldapConfigId);

		if (returnValue instanceof Long) {
			return returnValue;
		} else if (returnValue instanceof Users) {
			Users us = (Users) returnValue;
			if (authLevelManagement.checkUserLevel(
					us.getLevel_id())) {
				return us;
			} else {
				return -52L;
			}
		}

		return returnValue;

	}

	/**
	 * auth function, use the SID you get by getsessiondata
	 * 
	 * @param SID
	 * @param Username
	 * @param Userpass
	 * @return a valid user account or an empty user with an error message and
	 *         level -1
	 */
	public Object loginUser(String SID, String usernameOrEmail,
			String Userpass, Boolean storePermanent, Long language_id,
			Long ldapConfigId) {

		boolean withLdap = false;

		if (ldapConfigId > 0) {
			withLdap = true;
		}

		try {
			log.warn("loginUser: " + SID + " " + usernameOrEmail);

			RoomClient currentClient;
			IConnection current = Red5.getConnectionLocal();

			if (current == null)
				return null;
			
			Object o;

			if (withLdap) {
				log.debug("Ldap Login");

				currentClient = clientListManager
						.getClientByStreamId(current.getClient().getId());

				// LDAP Loggedin Users cannot use the permanent Login Flag

				LdapConfig ldapConfig = ldapConfigDao.getLdapConfigById(ldapConfigId);

				String ldapLogin = usernameOrEmail;
				if (ldapConfig.getAddDomainToUserName() != null
						&& ldapConfig.getAddDomainToUserName()) {
					ldapLogin = usernameOrEmail + "@" + ldapConfig.getDomain();
				}

				o = ldapLoginManagement.doLdapLogin(ldapLogin,
						Userpass, currentClient, SID,
						ldapConfig.getConfigFileName());
			} else {

				currentClient = clientListManager.getClientByStreamId(current.getClient().getId());

				o = userManagement.loginUser(SID, usernameOrEmail, Userpass,
						currentClient, storePermanent);
			}

			if (o == null)
				return null;

			if (!o.getClass().isAssignableFrom(Users.class))
				return o;

			if (currentClient.getUser_id() != null
					&& currentClient.getUser_id() > 0) {

				Users u = (Users) o;
				currentClient.setFirstname(u.getFirstname());
				currentClient.setLastname(u.getLastname());

				Collection<Set<IConnection>> conCollection = current.getScope()
						.getConnections();
				for (Set<IConnection> conset : conCollection) {
					for (IConnection cons : conset) {
						if (cons != null) {
							RoomClient rcl = this.clientListManager
									.getClientByStreamId(cons.getClient()
											.getId());
							if (rcl != null && rcl.getIsScreenClient() != null
									&& rcl.getIsScreenClient()) {
								// continue;
							} else {
								if (cons instanceof IServiceCapableConnection) {
									if (!cons.equals(current)) {
										// log.error("sending roomDisconnect to "
										// + cons);
										// RoomClient rcl =
										// this.clientListManager.getClientByStreamId(cons.getClient().getId());
										// Send to all connected users
										((IServiceCapableConnection) cons)
												.invoke("roomConnect",
														new Object[] { currentClient },
														this);
										// log.error("sending roomDisconnect to "
										// + cons);
									}
								}
							}
						}
					}
				}
			}

			return o;

		} catch (Exception err) {
			log.error("loginUser : ", err);
		}

		return null;

		/*
		 * try { log.debug("loginUser 111: "+SID+" "+Username); IConnection
		 * current = Red5.getConnectionLocal(); RoomClient currentClient =
		 * Application.getClientList().get(current.getClient().getId()); Object
		 * obj = userManagement.loginUser(SID,Username,Userpass, currentClient);
		 * 
		 * if (currentClient.getUser_id()!=null && currentClient.getUser_id()>0)
		 * { Users us = (Users) obj;
		 * currentClient.setFirstname(us.getFirstname());
		 * currentClient.setLastname(us.getLastname()); Iterator<IConnection> it
		 * = current.getScope().getConnections(); while (it.hasNext()) {
		 * //log.error("hasNext == true"); IConnection cons = it.next();
		 * //log.error("cons Host: "+cons); if (cons instanceof
		 * IServiceCapableConnection) { if (!cons.equals(current)){
		 * //log.error("sending roomDisconnect to " + cons); RoomClient rcl =
		 * Application.getClientList().get(cons.getClient().getId()); //Send to
		 * all connected users ((IServiceCapableConnection)
		 * cons).invoke("roomConnect",new Object[] { currentClient }, this);
		 * //log.error("sending roomDisconnect to " + cons); } } } }
		 * 
		 * return obj; } catch (Exception err) { log.error("loginUser",err); }
		 * return null;
		 */
	}

	public Object secureLoginByRemote(String SID, String secureHash) {
		try {

			log.debug("############### secureLoginByRemote " + secureHash);
			System.out.println("############### secureLoginByRemote "
					+ secureHash);

			String clientURL = Red5.getConnectionLocal().getRemoteAddress();

			log.debug("swfURL " + clientURL);

			SOAPLogin soapLogin = soapLoginDao.getSOAPLoginByHash(secureHash);

			if (soapLogin.getUsed()) {

				if (soapLogin.getAllowSameURLMultipleTimes()) {

					if (!soapLogin.getClientURL().equals(clientURL)) {
						log.debug("does not equal " + clientURL);
						return -42L;
					}

				} else {
					log.debug("Already used " + secureHash);
					return -42L;
				}
			}

			Long loginReturn = this.loginUserByRemote(soapLogin
					.getSessionHash());

			IConnection current = Red5.getConnectionLocal();
			String streamId = current.getClient().getId();
			RoomClient currentClient = this.clientListManager
					.getClientByStreamId(streamId);

			if (currentClient.getUser_id() != null) {
				sessionManagement.updateUser(SID, currentClient.getUser_id());
			}

			currentClient.setAllowRecording(soapLogin.getAllowRecording());
			this.clientListManager.updateClientByStreamId(streamId,
					currentClient);

			if (loginReturn == null) {

				log.debug("loginReturn IS NULL for SID: "
						+ soapLogin.getSessionHash());

				return -1L;
			} else if (loginReturn < 0) {

				log.debug("loginReturn IS < 0 for SID: "
						+ soapLogin.getSessionHash());

				return loginReturn;
			} else {

				soapLogin.setUsed(true);
				soapLogin.setUseDate(new Date());

				soapLogin.setClientURL(clientURL);

				soapLoginDao.updateSOAPLogin(soapLogin);

				// Create Return Object and hide the validated
				// sessionHash that is stored server side
				// this hash should be never thrown back to the user

				SOAPLogin returnSoapLogin = new SOAPLogin();

				returnSoapLogin.setRoom_id(soapLogin.getRoom_id());
				returnSoapLogin.setBecomemoderator(soapLogin
						.getBecomemoderator());
				returnSoapLogin.setShowAudioVideoTest(soapLogin
						.getShowAudioVideoTest());
				returnSoapLogin.setRoomRecordingId(soapLogin
						.getRoomRecordingId());
				returnSoapLogin.setShowNickNameDialog(soapLogin
						.getShowNickNameDialog());
				returnSoapLogin.setLandingZone(soapLogin.getLandingZone());

				return returnSoapLogin;

			}

		} catch (Exception err) {
			log.error("[secureLoginByRemote]", err);
		}
		return null;
	}

	/**
	 * Function is called if the user loggs in via a secureHash and sets the
	 * param showNickNameDialog in the Object SOAPLogin to true the user gets
	 * displayed an additional dialog to enter his nickname
	 * 
	 * @return
	 */

	public Long setUserNickName(String firstname, String lastname, String email) {
		try {

			IConnection current = Red5.getConnectionLocal();
			String streamId = current.getClient().getId();
			RoomClient currentClient = this.clientListManager
					.getClientByStreamId(streamId);

			currentClient.setFirstname(firstname);
			currentClient.setLastname(lastname);
			currentClient.setMail(email);

			// Log the User
			conferenceLogDao.addConferenceLog(
					"nicknameEnter", currentClient.getUser_id(), streamId,
					null, currentClient.getUserip(), currentClient.getScope(),
					currentClient.getExternalUserId(),
					currentClient.getExternalUserType(),
					currentClient.getMail(), currentClient.getFirstname(),
					currentClient.getLastname());

			this.clientListManager.updateClientByStreamId(streamId,
					currentClient);

			return 1L;

		} catch (Exception err) {
			log.error("[setUserNickName] ", err);
		}
		return new Long(-1);
	}

	/**
	 * Attention! This SID is NOT the default session id! its the Session id
	 * retrieved in the call from the SOAP Gateway!
	 * 
	 * @param SID
	 * @return
	 */
	public Long loginUserByRemote(String SID) {
		try {
			Long users_id = sessionManagement.checkSession(SID);
			Long user_level = userManagement.getUserLevelByID(users_id);
			if (authLevelManagement.checkWebServiceLevel(
					user_level)) {

				Sessiondata sd = sessionManagement.getSessionByHash(SID);
				if (sd == null || sd.getSessionXml() == null) {
					return new Long(-37);
				} else {

					// XStream xStream = new XStream(new XppDriver());
					XStream xStream = new XStream(new DomDriver("UTF-8"));
					xStream.setMode(XStream.NO_REFERENCES);

					String xmlString = sd.getSessionXml();
					RemoteSessionObject userObject = (RemoteSessionObject) xStream
							.fromXML(xmlString);

					log.debug("userObject.getUsername(), userObject.getFirstname(), userObject.getLastname() "
							+ userObject.getUsername()
							+ ", "
							+ userObject.getFirstname()
							+ ", "
							+ userObject.getLastname());

					IConnection current = Red5.getConnectionLocal();
					String streamId = current.getClient().getId();
					RoomClient currentClient = this.clientListManager
							.getClientByStreamId(streamId);

					// Check if this User is simulated in the OpenMeetings
					// Database

					log.debug("userObject.getExternalUserId() -1- "
							+ userObject.getExternalUserId());

					if (userObject.getExternalUserId() != null
							&& userObject.getExternalUserId() > 0) {

						// If so we need to check that we create this user in
						// OpenMeetings and update its record

						Users user = userManagement.getUserByExternalIdAndType(
								userObject.getExternalUserId(),
								userObject.getExternalUserType());

						if (user == null) {

							Configuration conf = cfgManagement.getConfKey(3L,
									"default.timezone");
							String jName_timeZone = "";

							if (conf != null) {
								jName_timeZone = conf.getConf_value();
							}

							long userId = userManagement
									.addUserWithExternalKey(1, 0, 0,
											userObject.getFirstname(),
											userObject.getUsername(),
											userObject.getLastname(), 1L, "",
											null, null, "",
											userObject.getExternalUserId(),
											userObject.getExternalUserType(),
											true, userObject.getEmail(),
											jName_timeZone,
											userObject.getPictureUrl());

							currentClient.setUser_id(userId);
						} else {

							user.setPictureuri(userObject.getPictureUrl());

							userManagement.updateUser(user);

							currentClient.setUser_id(user.getUser_id());
						}
					}

					log.debug("userObject.getExternalUserId() -2- "
							+ currentClient.getUser_id());

					currentClient
							.setUserObject(userObject.getUsername(),
									userObject.getFirstname(),
									userObject.getLastname());
					currentClient.setPicture_uri(userObject.getPictureUrl());
					currentClient.setMail(userObject.getEmail());

					log.debug("UPDATE USER BY STREAMID " + streamId);

					if (currentClient.getUser_id() != null) {
						sessionManagement.updateUser(SID,
								currentClient.getUser_id());
					}

					this.clientListManager.updateClientByStreamId(streamId,
							currentClient);

					return new Long(1);
				}
			}
		} catch (Exception err) {
			log.error("[loginUserByRemote] ", err);
		}
		return new Long(-1);
	}

	/**
	 * this function logs a user into if he enteres the app directly into a room
	 * 
	 * @param SID
	 */
	public Users markSessionAsLogedIn(String SID) {
		try {
			sessionManagement.updateUserWithoutSession(SID, -1L);
			Configuration conf = cfgManagement.getConfKey(3L,
					"default.rpc.userid");
			return userManagement.getUserById(Long.parseLong(conf
					.getConf_value()));
		} catch (Exception err) {
			log.error("[markSessionAsLogedIn]", err);
		}
		return null;
	}

	/**
	 * clear this session id
	 * 
	 * @param SID
	 * @return string value if completed
	 */
	public Long logoutUser(String SID) {
		Long users_id = sessionManagement.checkSession(SID);
		IConnection current = Red5.getConnectionLocal();
		RoomClient currentClient = this.clientListManager
				.getClientByStreamId(current.getClient().getId());
		currentClient.setUserObject(null, null, null, null);
		return userManagement.logout(SID, users_id);
	}

	/**
	 * get a list of all states, needs no authentification to load
	 * 
	 * @return List of State-Objects or null
	 */
	public List<States> getStates() {
		return statemanagement.getStates();
	}

	public List<OmTimeZone> getTimeZones() {
		return omTimeZoneDaoImpl.getOmTimeZones();
	}

	/**
	 * Load if the users can register itself by using the form without logging
	 * in, needs no authentification to load
	 * 
	 * @param SID
	 * @return
	 */
	public Configuration allowFrontendRegister(String SID) {
		return cfgManagement.getConfKey(3, "allow_frontend_register");
	}
	
	public List<Configuration> getGeneralOptions(String SID) {
		try {
			
			List<Configuration> cList = new LinkedList<Configuration>();
			
			cList.add(cfgManagement.getConfKey(3, "exclusive.audio.keycode"));
			
			return cList;
			
		} catch (Exception err) {
			log.error("[getLoginOptions]",err);
		}
		return null;
	}

	public List<Configuration> getLoginOptions(String SID) {
		try {

			List<Configuration> cList = new LinkedList<Configuration>();
			cList.add(cfgManagement.getConfKey(3, "allow_frontend_register"));
			cList.add(cfgManagement.getConfKey(3, "show.facebook.login"));
			cList.add(cfgManagement.getConfKey(3, "user.login.minimum.length"));
			cList.add(cfgManagement.getConfKey(3, "user.pass.minimum.length"));
			cList.add(cfgManagement.getConfKey(3, "user.pass.minimum.length"));
			

			return cList;
		} catch (Exception err) {
			log.error("[getLoginOptions]", err);
		}
		return null;

	}

	/**
	 * Add a user register by an Object see [registerUser] for the index of the
	 * Object To allow the registering the config_key *allow_frontend_register*
	 * has to be the value 1 otherwise the user will get an error code
	 * 
	 * @param regObject
	 * @return new users_id OR null if an exception, -1 if an error, -4 if mail
	 *         already taken, -5 if username already taken, -3 if login or pass
	 *         or mail is empty
	 */
	public Long registerUserByObject(Object regObjectObj) {
		try {
			@SuppressWarnings("unchecked")
			Map<?, ?> regObject = (Map<Object, Object>) regObjectObj;

			String domain = regObject.get("domain").toString();
			String port = regObject.get("port").toString();
			String webapp = regObject.get("webapp").toString();

			String baseURL = "http://" + domain + ":" + port + webapp;
			if (port.equals("80")) {
				baseURL = "http://" + domain + webapp;
			} else if (port.equals("443")) {
				baseURL = "https://" + domain + webapp;
			}

			return userManagement.registerUser(regObject.get("Username")
					.toString(), regObject.get("Userpass").toString(),
					regObject.get("lastname").toString(),
					regObject.get("firstname").toString(),
					regObject.get("email").toString(), new Date(), regObject
							.get("street").toString(),
					regObject.get("additionalname").toString(),
					regObject.get("fax").toString(), regObject.get("zip")
							.toString(),
					Long.valueOf(regObject.get("states_id").toString())
							.longValue(), regObject.get("town").toString(),
					Long.valueOf(regObject.get("language_id").toString())
							.longValue(), "", baseURL, true,
					regObject.get("jNameTimeZone").toString());
		} catch (Exception ex) {
			log.error("registerUserByObject", ex);
		}
		return new Long(-1);
	}

	/**
	 * Register a new User To allow the registering the config_key
	 * *allow_frontend_register* has to be the value 1 otherwise the user will
	 * get an error code
	 * 
	 * @deprecated use registerUserByObject instead
	 * @param SID
	 * @param Username
	 * @param Userpass
	 * @param lastname
	 * @param firstname
	 * @param email
	 * @param age
	 * @param street
	 * @param additionalname
	 * @param fax
	 * @param zip
	 * @param states_id
	 * @param town
	 * @param language_id
	 * @return new users_id OR null if an exception, -1 if an error, -4 if mail
	 *         already taken, -5 if username already taken, -3 if login or pass
	 *         or mail is empty
	 */
	@Deprecated
	public Long registerUser(String SID, String Username, String Userpass,
			String lastname, String firstname, String email, Date age,
			String street, String additionalname, String fax, String zip,
			long states_id, String town, long language_id, String phone) {
		return userManagement.registerUser(Username, Userpass, lastname,
				firstname, email, age, street, additionalname, fax, zip,
				states_id, town, language_id, phone, "", true, "");
	}

	/**
	 * logs a user out and deletes his account
	 * 
	 * @param SID
	 * @return
	 */
	public Long deleteUserIDSelf(String SID) {
		Long users_id = sessionManagement.checkSession(SID);
		long user_level = userManagement.getUserLevelByID(users_id);
		if (user_level >= 1) {
			userManagement.logout(SID, users_id);
			return usersDao.deleteUserID(users_id);
		} else {
			return new Long(-10);
		}
	}

	/**
	 * send an invitation to another user by Mail
	 * 
	 * @deprecated
	 * @param SID
	 * @param username
	 * @param message
	 * @param domain
	 * @param room
	 * @param roomtype
	 * @param baseurl
	 * @param email
	 * @param subject
	 * @param room_id
	 * @return
	 */
	@Deprecated
	public String sendInvitation(String SID, String username, String message,
			String domain, String room, String roomtype, String baseurl,
			String email, String subject, Long room_id) {
		Long users_id = sessionManagement.checkSession(SID);
		Long user_level = userManagement.getUserLevelByID(users_id);
		return invitationManagement.sendInvitionLink(user_level,
				username, message, domain, room, roomtype, baseurl, email,
				subject, room_id, null, null);
	}

	/**
	 * send some feedback, this will only work for the online demo-version
	 * 
	 * @param SID
	 * @param username
	 * @param message
	 * @param email
	 * @return
	 */
	public String sendFeedback(String SID, String username, String message,
			String email) {
		return feedbackManagement.sendFeedback(username, email,
				message);
	}

	public List<Userdata> getUserdata(String SID) {
		Long users_id = sessionManagement.checkSession(SID);
		Long user_level = userManagement.getUserLevelByID(users_id);
		if (authLevelManagement.checkUserLevel(user_level)) {
			return userManagement.getUserdataDashBoard(users_id);
		}
		return null;
	}

	/**
	 * @deprecated
	 * @param SID
	 * @return
	 */
	@Deprecated
	public LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, Object>>>> getRssFeeds(
			String SID) {
		Long users_id = sessionManagement.checkSession(SID);
		Long user_level = userManagement.getUserLevelByID(users_id);
		return loadAtomRssFeed.getRssFeeds(user_level);
	}

	/**
	 * 
	 * @param SID
	 * @param urlEndPoint
	 * @return
	 */
	public LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, Object>>> getRssFeedByURL(
			String SID, String urlEndPoint) {
		Long users_id = sessionManagement.checkSession(SID);
		Long user_level = userManagement.getUserLevelByID(users_id);
		if (authLevelManagement.checkUserLevel(user_level)) {
			return loadAtomRssFeed.parseRssFeed(urlEndPoint);
		} else {
			return null;
		}
	}

	/**
	 * TODO: Is this function in usage?
	 * 
	 * @deprecated
	 * @param SID
	 * @param domain
	 * @return
	 */
	@Deprecated
	public LinkedHashMap<Integer, RoomClient> getUsersByDomain(String SID,
			String domain) {
		Long users_id = sessionManagement.checkSession(SID);
		Long user_level = userManagement.getUserLevelByID(users_id);
		if (authLevelManagement.checkUserLevel(user_level)) {
			LinkedHashMap<Integer, RoomClient> lMap = new LinkedHashMap<Integer, RoomClient>();
			// Integer counter = 0;
			// for (Iterator<String> it =
			// Application.getClientList().keySet().iterator();it.hasNext();) {
			// RoomClient rc = Application.getClientList().get(it.next());
			// //if (rc.getDomain().equals(domain)) {
			// lMap.put(counter, rc);
			// counter++;
			// //}
			// }
			return lMap;
		} else {
			return null;
		}
	}

	public Boolean getSIPModuleStatus() {
		try {

			Configuration conf = cfgManagement.getConfKey(3L, "sip.enable");

			if (conf == null) {
				return false;
			} else {

				if (conf.getConf_value().equals("yes")) {
					return true;
				}

			}

		} catch (Exception err) {
			log.error("[getSIPModuleStatus]", err);
		}
		return false;
	}

	public int closeRoom(String SID, Long room_id, Boolean status) {
		try {
			Long users_id = sessionManagement.checkSession(SID);
			Long user_level = userManagement.getUserLevelByID(users_id);
			if (authLevelManagement.checkUserLevel(user_level)) {

				roommanagement.closeRoom(room_id, status);

				if (status) {
					Map<String, String> message = new HashMap<String, String>();
					message.put("message", "roomClosed");
					this.scopeApplicationAdapter.sendMessageByRoomAndDomain(
							room_id, message);
				}

				return 1;

			}

			return 1;
		} catch (Exception err) {
			log.error("[closeRoom]", err);
		}
		return -1;
	}

	public void resultReceived(IPendingServiceCall arg0) {
		log.debug("[resultReceived]" + arg0);
	}

	public List<Configuration> getDashboardConfiguration(String SID) {
		try {
			Long users_id = sessionManagement.checkSession(SID);
			Long user_level = userManagement.getUserLevelByID(users_id);
			if (authLevelManagement.checkUserLevel(user_level)) {

				List<Configuration> cfManagementList = new LinkedList<Configuration>();

				cfManagementList.add(cfgManagement.getConfKey(3L,
						"dashboard.show.chat"));
				cfManagementList.add(cfgManagement.getConfKey(3L,
						"dashboard.show.myrooms"));
				cfManagementList.add(cfgManagement.getConfKey(3L,
						"dashboard.show.rssfeed"));
				cfManagementList.add(cfgManagement.getConfKey(3L,
						"default.dashboard.tab"));

				return cfManagementList;
			}
		} catch (Exception err) {
			log.error("[getDashboardConfiguration]", err);
		}
		return null;
	}

	/*
	 * Shopsystem
	 * 
	 * public zahlungsarten[] getZahlungsarten(String SID){ return
	 * ResHandler.getZahlungsarten(SID); } public lieferarten[]
	 * getLieferarten(String SID){ return ResHandler.getLieferarten(SID); }
	 * public products[] getProductsByCat(String SID){ return
	 * ResHandler.getProductByCat(SID); } public products[] searchProduct(String
	 * SID,String searchstring){ return
	 * ResHandler.searchProduct(SID,searchstring); } public products[]
	 * getProductsByCatID(String SID,String cat, int start){ return
	 * ResHandler.getProductByCat(SID,start,cat); } public products[]
	 * getAllProductByCat(String SID,String cat){ return
	 * ResHandler.getAllProductByCat(SID,cat); } public products
	 * getProductByID(String SID, int artnumber){ return
	 * ResHandler.getProductByID(SID,artnumber); } public Userwaren[]
	 * getUserwaren(String SID){ return ResHandler.getUserwaren(SID); } public
	 * Userwaren getUserwarenByID(String SID,int WAREN_ID){ return
	 * ResHandler.getUserwarenByID(SID,WAREN_ID); } public String
	 * addWarenkorb(String SID, int ARTICLE_ID, int amount){ return
	 * ResHandler.addWarenkorb(SID,ARTICLE_ID,amount); } public String
	 * updateWarenkorb(String SID, int WAREN_ID, int status, int ZAHLUNGS_ID,
	 * int LIEFER_ID, int amount, String comment){ return
	 * ResHandler.updateWarenkorb(SID, WAREN_ID, status, ZAHLUNGS_ID, LIEFER_ID,
	 * amount, comment); } public String deleteWarenkorb(String SID, int
	 * WAREN_ID){ return ResHandler.deleteWarenkorb(SID,WAREN_ID); }
	 */

}
