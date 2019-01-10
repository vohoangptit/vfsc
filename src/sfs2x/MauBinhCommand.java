/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sfs2x;

/**
 *
 * @author binhnt
 */
public class MauBinhCommand {

    public static final int INTERFACE_ERROR = 1;
    public static final int SET_LIMIT_TIME = 10;
    public static final int TABLE_INFO = 15;
    public static final int START = 20;
    public static final int SORT_BY_ORDER = 23;
    public static final int SORT_BY_TYPE = 24;
    public static final int AUTO_ARRANGE = 25;
    public static final int FINISH = 30;
    public static final int RESULT = 40;
    public static final int STOP = 50;
    //cmd gui bai cho truong hop time out, sv tu dong binh
    public static final int SEND_CARDS = 51;
    //cmd thông tin sập hầm trong ván
    public static final int DEC_SAP_HAM = 52;
    //thông tin win của user khi kết thúc ván
    public static final int USER_MONEY_INFO = 53;
    
    public static final int INGAME_INFOR = 54;
}
