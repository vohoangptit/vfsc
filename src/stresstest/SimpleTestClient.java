/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stresstest;

import com.smartfoxserver.v2.exceptions.SFSException;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import sfs2x.client.SmartFox;
import sfs2x.client.core.BaseEvent;
import sfs2x.client.core.IEventListener;
import sfs2x.client.core.SFSEvent;
import sfs2x.client.requests.JoinRoomRequest;
import sfs2x.client.requests.LoginRequest;
import sfs2x.client.requests.PublicMessageRequest;
import sfs2x.client.util.ConfigData;
import sfs2x.client.util.PasswordUtil;

/**
 *
 * @author hanv
 */
public class SimpleTestClient extends BaseStressClient {

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
                        System.err.println("Connection failed");
                        cleanUp();
                    }   break;
                case SFSEvent.CONNECTION_LOST:
                    System.out.println("Client disconnected. ");
                    cleanUp();
                    break;
                case SFSEvent.LOGIN:
                    // Join room
//                    joinRoomBoard();
                    System.out.println(username + " login success");
                    break;
                case SFSEvent.LOGIN_ERROR:
                    System.out.println("Login error:  " + evt.getArguments().get("errorMessage"));
                    break;
                case SFSEvent.EXTENSION_RESPONSE:
                    break;
                case SFSEvent.ROOM_JOIN:
                    publicMessageTask = sched.scheduleAtFixedRate(new Runnable() {
                        @Override
                        public void run() {
                            if (pubMessageCount < TOT_PUB_MESSAGES) {
                                sfs.send(new PublicMessageRequest("Hello, this is a test public message."));
                                pubMessageCount++;
                                
                                System.out.println(sfs.getMySelf().getName() + " --> Message: " + pubMessageCount);
                            } else {
                                // End of test
                                sfs.disconnect();
                            }
                            
                        }
                    }, 0, 3, TimeUnit.SECONDS);
                    break;
                default:
                    break;
            }

        }

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
        sfs.send( new JoinRoomRequest("BC_2000") );
    }
}
