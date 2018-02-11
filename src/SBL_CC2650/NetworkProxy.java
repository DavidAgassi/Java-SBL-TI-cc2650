package SBL_CC2650;

import net.Network_iface;

/**
 * Created by Liat n on 16/03/2017.
 */
public class NetworkProxy implements Network_iface {
    private byte[] response;
    private boolean available = false;
    @Override
    public void writeLog(int id, String text) {
        System.out.println(text);
    }

    @Override
    public void parseInput(int id, int numBytes, byte[] message) {
        response = new byte[numBytes];
        System.arraycopy(message,0, response,0 , numBytes);
        available = true;
    }

    @Override
    public void networkDisconnected(int id) {

    }

    public boolean isAvailable() {
        return available;
    }

    public byte[] getResponse() {
        available=false;
        return response;
    }
    public byte[] awaitResponse(){
        long startTime = System.currentTimeMillis();
        while (!available){
            try {Thread.sleep(Consts.WAITE_SLEEP);}
            catch (InterruptedException e) {e.printStackTrace(); }
            if( System.currentTimeMillis() - startTime > Consts.RESPONSE_TIME) {
                return null;
            }
        }
        return getResponse();
    }
    public static ECommandReturn getCmdRetValue(byte[] bytes, int ackInd) {
        if(bytes==null){
            return ECommandReturn.COMMAND_RETURN_NULL;
        }
        // result is in ack+2.
        int resInd = ackInd+2;
        if( resInd == 0|| resInd > bytes.length){
            return ECommandReturn.COMMAND_RETURN_NULL;
        }
        for (ECommandReturn val: ECommandReturn.values()) {
            if(bytes[resInd]==val.getValue()){
                return val;
            }
        }
        return ECommandReturn.COMMAND_RETURN_NULL;
    }
    public static int AckNack(byte[] bytes){
        if(bytes==null){
            return -1;
        }
        for (int i=0 ; i < bytes.length; i++ ) {
            switch (bytes[i]){
            case Consts.ACK:
                return i;
            case Consts.NACK:
                return -1;
            }
        }
        return -1;
    }
}
