/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sfs2x;

import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sfs2x.client.SmartFox;
import sfs2x.client.core.BaseEvent;
import sfs2x.client.core.IEventListener;
import sfs2x.client.core.SFSEvent;
import sfs2x.client.entities.Room;
import sfs2x.client.entities.match.BoolMatch;
import sfs2x.client.entities.match.MatchExpression;
import sfs2x.client.entities.match.RoomProperties;
import sfs2x.client.requests.FindRoomsRequest;
import sfs2x.client.requests.JoinRoomRequest;
import sfs2x.client.requests.LoginRequest;
import sfs2x.client.requests.RoomExtension;
import sfs2x.client.requests.game.CreateSFSGameRequest;
import sfs2x.client.requests.game.SFSGameSettings;
import sfs2x.client.util.ConfigData;

/**
 * Basic SFS2X client, performing connection and login to a 'localhost' server
 */
public class LoginGGConnector implements IEventListener {
    
    public static final String BAICAO_EXT_ID = "IwinSFSBaiCaoExtension";
    public static final String BAICAO_EXT_CLASS = "iwin.vn.game.baicao.GameBaiCao";
    public static final String SIGN_UP_CMD = "$SignUp.Submit";
    
    private final SmartFox sfs;
    private final ConfigData cfg;
    
    private final String username = "ya29.GlwXBOHFdnlAYtI5vRTGwAZSP19rnVkYEJMwbN2a54fBiPjUMLEismQJCeOECqUtRKQXVVuJlrBlMP4o_fQC0ws-NWjIus9ocTOtHGx6W8MXJ3OqG5nRMsDX494JAA";
    private final String loginZone = "IwinZone";

    private final Logger log = LoggerFactory.getLogger(getClass());

    public LoginGGConnector() {
        /**
         * Setup the main API object and add all the events we
         * want to listen to.
         */
        sfs = new SmartFox();
        sfs.addEventListener(SFSEvent.CONNECTION, this);
        sfs.addEventListener(SFSEvent.CONNECTION_LOST, this);
        sfs.addEventListener(SFSEvent.LOGIN, this);
        sfs.addEventListener(SFSEvent.LOGIN_ERROR, this);
        sfs.addEventListener(SFSEvent.ROOM_JOIN, this);
        sfs.addEventListener(SFSEvent.USER_EXIT_ROOM, this);
        sfs.addEventListener(SFSEvent.ROOM_FIND_RESULT, this);
        sfs.addEventListener(SFSEvent.ROOM_ADD, this);
        sfs.addEventListener(SFSEvent.EXTENSION_RESPONSE, new IEventListener() {
            @Override
            public void dispatch(BaseEvent evt) throws SFSException {
                String cmd = evt.getArguments().get("cmd").toString();
                SFSObject sfsObj = (SFSObject) evt.getArguments().get("params");
                log.info(cmd + sfsObj.getDump());
                switch (cmd) {
                    case SIGN_UP_CMD:
                        if (sfsObj.getBool("success") != null) {
                            
                        } else {
                            String errorMsg = sfsObj.getUtfString("errorMessage");
                            System.out.println(errorMsg);
                        }
                        break;
                }
            }

        });
        
        /**
         * Create a configuration for the connection passing
         * the basic parameters such as host, TCP port number and Zone name
         * that will be used for logging in.
         */
        cfg = new ConfigData();
        cfg.setHost("localhost");
        cfg.setPort(9933);
        cfg.setZone(loginZone);
        
        sfs.connect(cfg);
    }

    /**
     * This handles the events coming from the server
     * @param evt
     * @throws com.smartfoxserver.v2.exceptions.SFSException
     */
    @Override
    public void dispatch(BaseEvent evt) throws SFSException 
    {
        log.info(evt.getType());
        /**
         * Handle CONNECTIOn event
         */
        switch (evt.getType()) {
        /**
         * Handle CONNECTION_LOST event
         */
            case SFSEvent.CONNECTION:
                boolean success = (Boolean) evt.getArguments().get("success");
                if (!success)
                {
                    log.warn("Connection failed!");
                    return;
                }   log.info("Connection success: " + sfs.getConnectionMode());
                /**
                 * Send a guest login request (no name, no password)
                 * The server will auto-assign a guest user name
                 */
                SFSObject params = new SFSObject();
                params.putInt("login_type", 2);
                sfs.send(new LoginRequest(username, null, sfs.getCurrentZone(), params));
                break;
        /**
         * Handle LOGIN event
         */
            case SFSEvent.CONNECTION_LOST:
                log.info("Connection was closed");
                break;
            case SFSEvent.LOGIN:
                log.info("Logged in as: " + sfs.getMySelf().getName());
                sfs.send(new JoinRoomRequest("lo_baicao"));
                
//            sfs.send(new JoinRoomRequest("b_1"));
                break;
            case SFSEvent.ROOM_JOIN:
                {
                    Room room = (Room) evt.getArguments().get("room");
                    log.info("joined lobby success in as: " + room.getName());
                    if (room.getName().equals("lo_baicao")) {
                        MatchExpression exp = new MatchExpression(RoomProperties.IS_GAME, BoolMatch.EQUALS, true);
                        sfs.send(new FindRoomsRequest(exp, "gr_baicao"));
                        
                    }       if (room.getName().equals("b_c_2")) {
                        MatchExpression exp = new MatchExpression(RoomProperties.IS_GAME, BoolMatch.EQUALS, true);
                        sfs.send(new FindRoomsRequest(exp, "gr_baicaoo"));
                        
                    }
//            sfs.send(new QuickGameJoinRequest(exp, Arrays.asList("gr_baicao")));
//            SFSObject s = new SFSObject();
//            s.putByte("byte1", (byte) 1);
//            sfs.send(new ExtensionRequest("testExtCmd", s, sfs.getLastJoinedRoom()));
//            sfs.send(new LeaveRoomRequest(sfs.getLastJoinedRoom()));
                    break;
                }
            case SFSEvent.USER_EXIT_ROOM:
                {
                    Room room = (Room) evt.getArguments().get("room");
                    log.info("exit room "+room.getName());
                    break;
                }
            case SFSEvent.ROOM_FIND_RESULT:
                List<Room> list = (List<Room>) evt.getArguments().get("rooms");
                log.info("resulr"+list);
                if(list.isEmpty()){
                    SFSGameSettings ss = new SFSGameSettings("b_c_2");
                    ss.setExtension(new RoomExtension(BAICAO_EXT_ID, BAICAO_EXT_CLASS));
                    ss.setGame(true);
//                ss.setGroupId("gr_baicao");
                    sfs.send(new CreateSFSGameRequest(ss));
                }
//                sfs.send(new JoinRoomRequest(list.get(1)));
                break;
            case SFSEvent.LOGIN_ERROR:
                log.warn("Login error:  " + evt.getArguments().get("errorMessage"));
                break;
            default:
                break;
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        new LoginGGConnector();
    }

}
