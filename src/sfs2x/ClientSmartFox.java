/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sfs2x;

import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSException;
import game.command.SFSAction;

import iwin.command.SFSCommand;
import game.key.SFSKey;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sfs2x.client.SmartFox;
import sfs2x.client.core.BaseEvent;
import sfs2x.client.core.IEventListener;
import sfs2x.client.core.SFSEvent;
import sfs2x.client.requests.ExtensionRequest;
import sfs2x.client.requests.JoinRoomRequest;
import sfs2x.client.requests.LoginRequest;
import sfs2x.client.util.ConfigData;

import sfs2x.client.entities.Room;
import sfs2x.client.entities.User;
import sfs2x.client.entities.match.BoolMatch;
import sfs2x.client.entities.match.MatchExpression;
import sfs2x.client.entities.match.NumberMatch;
import sfs2x.client.entities.match.RoomProperties;
import sfs2x.client.entities.match.StringMatch;
import sfs2x.client.entities.match.UserProperties;
import sfs2x.client.requests.BanUserRequest;
import sfs2x.client.requests.CreateRoomRequest;
import sfs2x.client.requests.FindRoomsRequest;
import sfs2x.client.requests.FindUsersRequest;
import sfs2x.client.requests.LeaveRoomRequest;
import sfs2x.client.requests.PublicMessageRequest;
import sfs2x.client.requests.RoomExtension;
import sfs2x.client.requests.RoomSettings;
import sfs2x.client.requests.SetUserVariablesRequest;
import sfs2x.client.util.PasswordUtil;

/**
 *
 * @author tuanp
 */
public class ClientSmartFox implements IEventListener {

    private static SmartFox sfsClient;

//    public String host ="127.0.0.1";
//    public int port = 9933;
    public String host ="127.0.0.1";
    public int port = 9933;
    public String zoneName = "BasicExamples";

    public String userName = "vohoang@gmail.com";
    public String pass = "t";
    User user;
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        ClientSmartFox client = new ClientSmartFox();
        client.start();
    }

    public void start() {
//        sfsClient.loadConfig();
        sfsClient = new SmartFox();
        initSmartFox();
        ConfigData cfg = new ConfigData();
        cfg.setHost(host);
        cfg.setPort(port);
        cfg.setZone(zoneName);
        cfg.setDebug(true);
        
        sfsClient.connect(cfg);
//        sfsClient.connect(host, port);
        
    }
    private void initSmartFox() {
        sfsClient.addEventListener(SFSEvent.CONNECTION, this);
        sfsClient.addEventListener(SFSEvent.CONNECTION_LOST, this);
        sfsClient.addEventListener(SFSEvent.LOGIN, this);
        sfsClient.addEventListener(SFSEvent.LOGIN_ERROR, this);
        sfsClient.addEventListener(SFSEvent.ROOM_JOIN, this);
        sfsClient.addEventListener(SFSEvent.USER_EXIT_ROOM, this);
        sfsClient.addEventListener(SFSEvent.ROOM_JOIN_ERROR, this);
        sfsClient.addEventListener(SFSEvent.EXTENSION_RESPONSE, this);
        sfsClient.addEventListener(SFSEvent.ROOM_FIND_RESULT, this);
        sfsClient.addEventListener(SFSEvent.ROOM_CREATION_ERROR, this);
        sfsClient.addEventListener(SFSEvent.CONNECTION_RESUME, this);
        sfsClient.addEventListener(SFSEvent.CONNECTION_RETRY, this);
        sfsClient.addEventListener(SFSEvent.USER_FIND_RESULT, this);
        sfsClient.addEventListener(SFSEvent.USER_VARIABLES_UPDATE, this); 
    }
     boolean haslogged=false;
    @Override
    public void dispatch(BaseEvent event) throws SFSException {
        try {
            switch (event.getType()) {
                case SFSEvent.CONNECTION:
                    if (event.getArguments().get("success").equals(true)) {
                        if (!haslogged) {
                            login();
                            haslogged=true;
                        }
                        log.info("Connection Successful");
                        sfsClient.setReconnectionSeconds(180);
                    } else {
                        log.info("ERROR: Connection Not Successful");
                    }
                    
                    break;

                case SFSEvent.CONNECTION_LOST:
                    sfsClient.disconnect();
//                    sfsClient.connect(host, port);
                    log.info("Connection Lost");
                    break;

                case SFSEvent.LOGIN:
                    user = (User) event.getArguments().get("user");
//                    sendZoneRequest(1000);
//                    joinRoomLobby();
                    log.info("Login Successful");
                    break;

                case SFSEvent.LOGIN_ERROR:
                    log.info("Login Failed");
                    break;

                case SFSEvent.ROOM_JOIN:
                    Room room = (Room) event.getArguments().get("room");
//                    sendJoinRoomRequest(room,5000);

                    break;
                case SFSEvent.ROOM_FIND_RESULT:
                    log.info("find result");
                    break;
                 case SFSEvent.USER_FIND_RESULT:
                    log.info("find result");
                    break;
                case SFSEvent.ROOM_JOIN_ERROR:
                    log.info("join room erro");
                    break;
                case SFSEvent.USER_EXIT_ROOM:
                     log.info("leave room ");
                    break;
                case SFSEvent.CONNECTION_RESUME:
                    sfsClient.connect(host, port);
                    log.info("connecttion resume ");
                    break;
                case SFSEvent.CONNECTION_RETRY:
                    sfsClient.connect(host, port);
                    log.info("connecttion retry ");
                    break;  
                case SFSEvent.EXTENSION_RESPONSE:
                    log.info(" extension");
                    handleResponse(event);
                    break;
                case SFSEvent.USER_VARIABLES_UPDATE:
                    log.info(" USER_VARIABLES_UPDATE");
                    //user duoc update
                    user = (User) event.getArguments().get("user");
                    //sanh sách key duoc update
                    List changedVars = (List)event.getArguments().get("changedVars");
                     log.info(" USER_VARIABLES_UPDATE: "+user.getName()+","+changedVars);
                    break; 
                default:
                    break;
                    
            }
        } catch (Exception e) {
           e.printStackTrace();
        }

    }
    private void handleResponse(BaseEvent event) {
        String command = (String) event.getArguments().get("cmd");
        if (command.equals(SFSCommand.CLIENT_REQUEST)) {
            ISFSObject data = (SFSObject) event.getArguments().get("params");
            processMessageInCore(data);
            
        }else if(command.equals(SFSCommand.CLIENT_REQUEST_INGAME)){
            ISFSObject data = (SFSObject) event.getArguments().get("params");
            processMessageInGame(data);
        }

    }
    private void processMessageInCore(ISFSObject data) {
        try {
            int action = data.getInt(SFSKey.ACTION_INCORE);
            switch (action) {
                case SFSAction.MESSAGE_ERROR:
                    log.info("Message erro:");
                    log.info(data.getDump());
                    break;
                 case SFSAction.REQUEST_INFOR_ALL_GAME:
                    log.info("request infor all game:");
                    log.info(data.getDump());
                    break;
                   case SFSAction.JOIN_ZONE_SUCCESS:
                    joinRoomBoard();
                    break;
                    
                 
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void processMessageInGame(ISFSObject data) {
        try {
            int action = data.getInt(SFSKey.ACTION_INGAME);
            switch (action) {
                case SFSAction.START_GAME:
                    log.info("START GAME:");
                    leaveBoard();
                    log.info(data.getDump());
                    break;
                case SFSAction.STOP_GAME:
                    log.info("STop GAME:");
                    log.info(data.getDump());
                    break;
                case SFSAction.BET:
                    log.info("BET GAME:");
                    findUser(user.getName());
                    log.info(data.getDump());
                    break;
                case SFSAction.LEAVE_GAME:
                    log.info("LEAVE GAME:");
                    log.info(data.getDump());
                    break;
                case SFSAction.MOVE:
                    log.info("MOVE GAME:");
                    log.info(data.getDump());
                    break;
                case SFSAction.BOARD_INFO:
                    log.info("BOARD_INFO GAME:");
                    log.info(data.getDump());
                    break;
                case SFSAction.JOIN_BOARD:
                    log.info("JOIN_BOARD GAME:");
                    log.info(data.getDump());
                    break;
                case SFSAction.RESULT:
                    log.info("RESULT_BOARD GAME:");
                    log.info(data.getDump());
                    break;
                case SFSAction.SKIP:
                    log.info("SKIP_BOARD GAME:");
                    log.info(data.getDump());
                    break;
                case SFSAction.ON_RETURN_GAME:
                    log.info("ON_RETURN_GAME:");
                    log.info(data.getDump());
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * client login
     */
    private void login() {
        ISFSObject isfsObject = new SFSObject();
        isfsObject.putUtfString("dvid", "testdeviceid");
        isfsObject.putUtfString("client_info", "{}");
        sfsClient.send(new LoginRequest(userName,PasswordUtil.MD5Password(pass), zoneName));
    }
    /**
     * user join a room
     */
    private void joinRoomLobby(){
        sfsClient.send( new JoinRoomRequest("lo_baicao") );
    }
    
    private void banUser(int userId){
        sfsClient.send( new BanUserRequest(userId) );
    }

    /**
     * user join a board
     */
    private void joinRoomBoard(){
        sfsClient.send( new JoinRoomRequest("SAM_2000") );
    }
     /**
     * user leave a board
     */
    private void leaveBoard(){
        sfsClient.send( new LeaveRoomRequest());
    }
    /**
     * user tạo 1 extension request
     */
    private void sender() {
        ISFSObject isfsObject = new SFSObject();
        isfsObject.putUtfString("dvid", "testdeviceid");
        sfsClient.send(new ExtensionRequest(SFSCommand.CLIENT_REQUEST, isfsObject,sfsClient.getLastJoinedRoom()));
    }
    
    /**
     * Xử lý moving trong board
     */
    private void movingBoard(Room room) {
        ISFSObject isfsObject = new SFSObject();
        isfsObject.putUtfString(SFSCommand.CLIENT_REQUEST_INGAME, SFSCommand.CLIENT_REQUEST_INGAME);
        isfsObject.putInt(SFSKey.ACTION_INGAME, SFSAction.MOVE);
        //phải có room de biết request o room nào
        sfsClient.send(new ExtensionRequest(SFSCommand.CLIENT_REQUEST_INGAME, isfsObject,room));
    }
    /**
     * Xử lý skip trong board
     */
    private void skipBoard(Room room) {
        ISFSObject isfsObject = new SFSObject();
        isfsObject.putUtfString(SFSCommand.CLIENT_REQUEST_INGAME, SFSCommand.CLIENT_REQUEST_INGAME);
        isfsObject.putInt(SFSKey.ACTION_INGAME, SFSAction.SKIP);
        //phải có room de biết request o room nào
        sfsClient.send(new ExtensionRequest(SFSCommand.CLIENT_REQUEST_INGAME, isfsObject,room));
    }
    /**
     * Xử lý DAT CUOC trong board
     */
    private void BetBoard(Room room) {
        ISFSObject isfsObject = new SFSObject();
        isfsObject.putUtfString(SFSCommand.CLIENT_REQUEST_INGAME, SFSCommand.CLIENT_REQUEST_INGAME);
        isfsObject.putInt(SFSKey.ACTION_INGAME, SFSAction.MOVE);
        //phải có room de biết request o room nào
        sfsClient.send(new ExtensionRequest(SFSCommand.CLIENT_REQUEST_INGAME, isfsObject,room));
    }
    
    private void findBetRoom(int bet) {
        MatchExpression exp = new MatchExpression(RoomProperties.IS_GAME, BoolMatch.EQUALS, true).and(SFSKey.BET_BOARD, NumberMatch.EQUALS, bet);
        sfsClient.send(new FindRoomsRequest(exp,"gr_baicao"));
    }
     private void findUser(String userName) {
        MatchExpression exp = new MatchExpression(UserProperties.NAME,StringMatch.EQUALS , userName);
        sfsClient.send(new FindUsersRequest(exp));
    }
     private void createRoomRequest(){
//        sfsClient.send( new  CreateRoomRequest(createRoomSetting(1, "room11")));
          sfsClient.send( new SetUserVariablesRequest(null));
    }
    public RoomSettings createRoomSetting(String roomName) {
        RoomSettings setting = new RoomSettings(roomName);
        setting.setGroupId("gr_baicao1");
        setting.setGame(true);
        setting.setName(roomName);
        setting.setMaxUsers(4);
        setting.setMaxSpectators(999);
        
        
        RoomExtension ex= new RoomExtension("IwinLobbyExtension","iwin.lobby.SFSIwinLobbyExtension");
        setting.setExtension(ex);
        sfsClient.send(new CreateRoomRequest(setting));
        return setting;
    }
    
    private void sendJoinRoomRequest(Room room,int bet){
        ISFSObject isfsObject = new SFSObject();
        isfsObject.putInt(SFSKey.ACTION_INCORE, SFSAction.CREATE_BOARD);
        isfsObject.putLong(SFSKey.BET_BOARD, bet);
        //phải có room de biết request o room nào
        sfsClient.send(new ExtensionRequest(SFSCommand.CLIENT_REQUEST, isfsObject,room));
    }
    private void sendZoneRequest(int bet){
        ISFSObject isfsObject = new SFSObject();
        isfsObject.putInt(SFSKey.ACTION_INCORE, SFSAction.CREATE_BOARD);
        isfsObject.putLong(SFSKey.BET_BOARD, bet);
        //phải có room de biết request o room nào
        sfsClient.send(new ExtensionRequest(SFSCommand.CLIENT_REQUEST, isfsObject));
    }   
    private void someMethod()
     {
         sfsClient.addEventListener(SFSEvent.PUBLIC_MESSAGE, onPublicMessage);
         
         // Send a public message
         sfsClient.send(new PublicMessageRequest("Hello everyone!"));
     }
     
     private void onPublicMessage()
     {         
         if (sender == sfs.mySelf)
             trace("I said:", evt.params.message);
         else
             trace("User " + sender.name + " said:", evt.params.message);
     }
}
