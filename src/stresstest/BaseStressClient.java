/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stresstest;

/**
 *
 * @author hanv
 */
public abstract class BaseStressClient
{
    private StressTestReplicator shell;
 
    public abstract void startUp(int id, String host, int port, String zone);
 
    public void setShell(StressTestReplicator shell)
    {
        this.shell = shell;
    }
 
    protected void onShutDown(BaseStressClient client)
    {
        shell.handleClientDisconnect(client);
    }
}
