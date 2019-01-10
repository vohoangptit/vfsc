/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stresstest;

import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSException;
import game.command.SFSAction;
import iwin.command.SFSCommand;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sfs2x.client.SmartFox;
import sfs2x.client.core.BaseEvent;
import sfs2x.client.core.IEventListener;
import sfs2x.client.core.SFSEvent;
import sfs2x.client.entities.Room;
import sfs2x.client.requests.ExtensionRequest;
import sfs2x.client.requests.JoinRoomRequest;
import sfs2x.client.requests.LoginRequest;
import sfs2x.client.util.ConfigData;
import sfs2x.client.util.PasswordUtil;

/**
 *
 * @author tuanp
 */
public class TestGameBaiCao extends BaseStressClient {

    // A scheduler for sending messages shared among all client bots.
    private static final ScheduledExecutorService sched = new ScheduledThreadPoolExecutor(1);
    private static final int TOT_PUB_MESSAGES = 50;

    private SmartFox sfs;
    private ConfigData cfg;
    private IEventListener evtListener;
    private ScheduledFuture<?> publicMessageTask;
    private int pubMessageCount = 0;
    
    private String username;
    private final String password = "123456";
    private final Logger log = LoggerFactory.getLogger(getClass());
    Room room ;

    @Override
    public void startUp(int id, String host, int port, String zone) {
        username = "test" + id + "@mecorp.vn";
        
        sfs = new SmartFox();
        cfg = new ConfigData();
        evtListener = new SFSEventListener();

        cfg.setHost(host);
        cfg.setPort(port);
        cfg.setZone(zone);

        sfs.addEventListener(SFSEvent.CONNECTION, evtListener);
        sfs.addEventListener(SFSEvent.CONNECTION_LOST, evtListener);
        sfs.addEventListener(SFSEvent.LOGIN, evtListener);
        sfs.addEventListener(SFSEvent.LOGIN_ERROR, evtListener);
        sfs.addEventListener(SFSEvent.ROOM_JOIN, evtListener);
        sfs.addEventListener(SFSEvent.ROOM_JOIN_ERROR, evtListener);
        sfs.addEventListener(SFSEvent.PUBLIC_MESSAGE, evtListener);
        sfs.addEventListener(SFSEvent.EXTENSION_RESPONSE, evtListener);

        sfs.connect(cfg);
    }

    public class SFSEventListener implements IEventListener {

        @Override
        public void dispatch(BaseEvent evt) throws SFSException {
            String type = evt.getType();
            Map<String, Object> params = evt.getArguments();

            switch (type) {
                case SFSEvent.CONNECTION:
                    boolean success = (Boolean) params.get("success");
                    if (success) {
                        sfs.send(new LoginRequest(username, PasswordUtil.MD5Password(password)));
                    } else {
//                        System.err.println("Connection failed");
                        cleanUp();
                    }   break;
                case SFSEvent.CONNECTION_LOST:
//                    System.out.println("Client disconnected. ");
                    cleanUp();
                    break;
                case SFSEvent.LOGIN:
                    // Join room
//                    joinRoomBoard();
//                    System.out.println(username + " login success");
                    break;
                case SFSEvent.LOGIN_ERROR:
                    System.out.println("Login error:  " + evt.getArguments().get("errorMessage"));
                    break;
                case SFSEvent.EXTENSION_RESPONSE:
                    handleResponse(evt);
                    break;
                case SFSEvent.ROOM_JOIN:
                    room = (Room) evt.getArguments().get("room");
//                    quickPlayBoard(room);
                    if (!room.isGame()) {
                        System.out.println("name lobby: "+room.getName());
                        sendJoinRoomRequest(room, 2000);
                    }
                    
                    break;
                 case SFSEvent.ROOM_JOIN_ERROR:
                    break;
                default:
                    break;
            }

        }

    }
    private void pushMessage() {
        publicMessageTask = sched.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (pubMessageCount < TOT_PUB_MESSAGES) {
                    movingBoard(room);
                    pubMessageCount++;

                    System.out.println(sfs.getMySelf().getName() + " --> Message: " + pubMessageCount);
                } else {
                    // End of test
                    sfs.disconnect();
                }

            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    private void cleanUp() {
        // Remove listeners
        sfs.removeAllEventListeners();

        // Stop task
        if (publicMessageTask != null) {
            publicMessageTask.cancel(true);
        }

        // Signal end of session to Shell
        onShutDown(this);
    }
     /**
     * user join a board
     */
    private void joinRoomBoard(){
        sfs.send( new JoinRoomRequest("BC_3000") );
    }
    /**
     * Xử lý moving trong board
     */
    private void quickPlayBoard(Room room) {
        ISFSObject isfsObject = new SFSObject();
        isfsObject.putUtfString(SFSCommand.CLIENT_REQUEST_INGAME, SFSCommand.CLIENT_REQUEST_INGAME);
//        isfsObject.putInt(SFSCommand.ACTION_INGAME, SFSCommand.QUICK_PLAY);
        //phải có room de biết request o room nào
        sfs.send(new ExtensionRequest(SFSCommand.CLIENT_REQUEST_INGAME, isfsObject, room));
    }
     /**
     * Xử lý moving trong board
     */
    private void movingBoard(Room room) {
        ISFSObject isfsObject = new SFSObject();
        isfsObject.putUtfString(SFSCommand.CLIENT_REQUEST_INGAME, SFSCommand.CLIENT_REQUEST_INGAME);
        isfsObject.putInt(SFSCommand.ACTION_INGAME, SFSCommand.MOVE);
        //phải có room de biết request o room nào
        sfs.send(new ExtensionRequest(SFSCommand.CLIENT_REQUEST_INGAME, isfsObject,room));
    }
     private void handleResponse(BaseEvent event) {
        String command = (String) event.getArguments().get("cmd");
        if(command.equals(SFSCommand.CLIENT_REQUEST_INGAME)){
            ISFSObject data = (SFSObject) event.getArguments().get("params");
            processMessageInGame(data);
        }else if (command.equals(SFSCommand.CLIENT_REQUEST)) {
            ISFSObject data = (SFSObject) event.getArguments().get("params");
            processMessageInCore(data);
        }

    }
     private void processMessageInCore(ISFSObject data) {
        try {
            int action = data.getInt(SFSCommand.ACTION_INCORE);
            switch (action) {
                case SFSCommand.MESSAGE_ERROR:
                    break;
                 case SFSCommand.REQUEST_INFOR_ALL_GAME:

                    break;
                case SFSAction.JOIN_ZONE_SUCCESS:
                    joinRoomLobby();
                    break;
                    
                 
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }        


    private void processMessageInGame(ISFSObject data) {
        try {
            int action = data.getInt(SFSCommand.ACTION_INGAME);
            switch (action) {
                case SFSCommand.START_GAME:
//                    log.info("START GAME:");
                    movingBoard(room);
                    break;
                case SFSCommand.STOP_GAME:
//                    log.info("STop GAME:");
//                    log.info(data.getDump());
                    break;
                case SFSCommand.BET:
//                    log.info("BET GAME:");
//                    log.info(data.getDump());
                    break;
                case SFSCommand.LEAVE_GAME:
//                    log.info("LEAVE GAME:");
//                    log.info(data.getDump());
                    break;
                case SFSCommand.MOVE:
//                    log.info("MOVE GAME:");
//                    log.info(data.getDump());
                    break;
                case SFSCommand.BOARD_INFO:
//                    log.info("BOARD_INFO GAME:");
//                    log.info(data.getDump());
                    break;
                case SFSCommand.JOIN_BOARD:
//                    log.info("JOIN_BOARD GAME:");
//                    log.info(data.getDump());
                    break;
                case SFSCommand.RESULT:
//                    log.info("RESULT_BOARD GAME:");
//                    log.info(data.getDump());
                    break;
                case SFSCommand.SKIP:
//                    log.info("SKIP_BOARD GAME:");
//                    log.info(data.getDump());
                    break;
//                case SFSCommand.ON_RETURN_GAME:
////                    log.info("ON_RETURN_GAME:");
////                    log.info(data.getDump());
//                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * user join a room
     */
    private void joinRoomLobby(){
        sfs.send( new JoinRoomRequest("lo_baicao") );
    }
    private void sendJoinRoomRequest(Room room,int bet){
        ISFSObject isfsObject = new SFSObject();
        isfsObject.putInt(SFSCommand.ACTION_INCORE, SFSCommand.CREATE_BOARD);
        isfsObject.putInt(SFSCommand.BET_BOARD, bet);
        //phải có room de biết request o room nào
        sfs.send(new ExtensionRequest(SFSCommand.CLIENT_REQUEST, isfsObject,room));
    }
    
}

