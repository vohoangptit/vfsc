/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sfs2x;

import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sfs2x.client.SmartFox;
import sfs2x.client.core.BaseEvent;
import sfs2x.client.core.IEventListener;
import sfs2x.client.core.SFSEvent;
import sfs2x.client.requests.ExtensionRequest;
import sfs2x.client.requests.LoginRequest;
import sfs2x.client.util.ConfigData;

/**
 *
 * @author hanv
 */
public class SignUpFBConnector implements IEventListener {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final SmartFox sfs;
    private final ConfigData cfg;
    private final String signUpZone = "IwinSignUp";
    private final String username = "testfb";
    private final String accessToken = "EAAKqoeAHNCcBADyTZAZBuoqpnLgS2jTZC7piZCCqI5I0WaEz0nSwz8ePPSRmJYvM5ZAhj6GSbAI4ibwPZCf4lnJ2Knxse58qE9PuxbBrJmZBfuFhiPCeGKCWS7mC6vgZCH1cZBHp2SHzNt5jI6nnnCPNN8QVYuiAAskko1KFRYapdGv1jh7c5Mu0808u7qNW7ZAKAZD";
    
    private final String SIGN_UP_FB_CMD = "SignUpFB";
    
    public SignUpFBConnector() {
        sfs = new SmartFox();
        sfs.addEventListener(SFSEvent.CONNECTION, this);
        sfs.addEventListener(SFSEvent.CONNECTION_LOST, this);
        sfs.addEventListener(SFSEvent.LOGIN, this);
        sfs.addEventListener(SFSEvent.LOGIN_ERROR, this);
        sfs.addEventListener(SFSEvent.EXTENSION_RESPONSE, new IEventListener() {
            @Override
            public void dispatch(BaseEvent evt) throws SFSException {
                String cmd = evt.getArguments().get("cmd").toString();
                SFSObject sfsObj = (SFSObject) evt.getArguments().get("params");
                log.info(cmd + sfsObj.getDump());
            }
            
        });
        
        cfg = new ConfigData();
        cfg.setHost("localhost");
        cfg.setPort(9933);
        cfg.setZone(signUpZone);
        sfs.connect(cfg);
    }

    @Override
    public void dispatch(BaseEvent evt) throws SFSException {
        log.info(evt.getType());
        if (evt.getType().equals(SFSEvent.CONNECTION)) {
            boolean success = (Boolean) evt.getArguments().get("success");

            if (!success)
            {
                log.warn("Connection failed!");
                return;
            }
            
            log.info("Connection success: " + sfs.getConnectionMode());
            
            /**
             * Send a guest login request (no name, no password)
             * The server will auto-assign a guest user name
             */
            sfs.send(new LoginRequest("guest"));
        }
        
        /**
         * Handle CONNECTION_LOST event
         */
        else if (evt.getType().equals(SFSEvent.CONNECTION_LOST))
        {
            log.info("Connection was closed");
        }
        
        /**
         * Handle LOGIN event
         */
        else if (evt.getType().equals(SFSEvent.LOGIN))
        {
            log.info("Logged in as: " + sfs.getMySelf().getName());
            signUpFB(accessToken, username);
        }
    }
    
    private void signUpFB(String accessToken, String username) {
        SFSObject params = new SFSObject();
        params.putUtfString("username", username);
        params.putUtfString("access_token", accessToken);
        sfs.send(new ExtensionRequest(SIGN_UP_FB_CMD, params));
    }

    public static void main(String[] args) 
    {
        new SignUpFBConnector();
    }
    
}
