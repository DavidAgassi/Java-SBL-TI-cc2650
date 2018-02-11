package SBL_CC2650;
import net.Network;

import java.io.*;
import java.util.zip.CRC32;

public class CC2650Bootloader {

    private Network network;
    private NetworkProxy proxy;

    public void burn(String com, String path) throws IOException, InterruptedException {
        proxy = new NetworkProxy();
        network = new Network(0, proxy);
        InputStream stream;
        stream = new FileInputStream(path);
        final int startingSize;
        startingSize = stream.available();

        byte[] data = new byte[startingSize];
        final int numberRead = stream.read(data, 0, startingSize);

        if(!network.connect(com)){
            System.out.println("ERROR - port " + com +" not available, quiting.");
            return;
        }
        if(!establishConnection()){
            System.out.println("ERROR - Failed to establish connection, quiting.");
            network.disconnect();
            return;
        }
        if(!deleteMem(numberRead)){
            System.out.println("ERROR - Failed to delete device, quiting.");
            network.disconnect();
            return;
        }
        if(!program(data)){
            System.out.println("ERROR - Failed to program device, quiting.");
            network.disconnect();
            return;
        }

        int readCrc = verify(data.length);
        long crcCalc = CalcCRC(data);
        if( readCrc== (int)crcCalc){
            System.out.println("Success - device verified.");
        }else{
            System.out.println("ERROR - Failed to verify device, still resetting.");
        }

        if(!reset()){
            System.out.println("ERROR - Failed to reset device, quiting.");
            network.disconnect();
            return;
        }
        network.disconnect();
    }

    private boolean establishConnection() {
        System.out.println("connecting to SLB on chip.");
        network.writeSerial(Consts.SYNC_MESSAGE.length,Consts.SYNC_MESSAGE);
        return proxy.AckNack(proxy.awaitResponse())!=-1;
    }


    private boolean deleteMem(int byteLength) {
        // TODO: delete partial
        System.out.println("Deleting device.");

        if(sendCmd(ECc2650Commands.CMD_BANK_ERASE,Consts.EMPTY_MESSAGE) == Consts.NO_ACK){
            System.out.println("ERROR - deleting mem bank, command not received.");
            return false;
        }
        if(!DeviceStatus()){
            System.out.println("ERROR - failed getting Status");
            return false;
        }
        System.out.println("Success - Device deleted.");
        return true;
    }

    private boolean program(byte[] data){
        System.out.println("Programing Device.");
        int bytesToSend = data.length;
        //System.out.println("sending download command. len: " + bytesToSend +".");
        byte[] downloadCMD = new byte[8];
        System.arraycopy(toBytes(Consts.CC26XX_FLASH_BASE), 0, downloadCMD,0,4);
        System.arraycopy(toBytes(bytesToSend), 0, downloadCMD,4,4);
        if(sendCmd(ECc2650Commands.CMD_DOWNLOAD,downloadCMD) == Consts.NO_ACK){
            System.out.println("ERROR - download command, command not received.");
            return false;
        }
        if(!DeviceStatus()){
            System.out.println("ERROR - failed getting Status");
            return false;
        }

        byte[] currSend;
        int currSendLen;
        int i=0;
        for (int currByte=0; currByte<bytesToSend; currByte+=Consts.SBL_CC2650_MAX_BYTES_PER_TRANSFER){
            if(i%50 ==0){System.out.println("sending data iteration: " + i + ".");}
            i++;
            currSendLen = Math.min(Consts.SBL_CC2650_MAX_BYTES_PER_TRANSFER,bytesToSend-currByte);
            currSend = new byte[currSendLen];
            System.arraycopy(data,currByte,currSend,0,currSendLen);
            if(sendCmd(ECc2650Commands.CMD_SEND_DATA,currSend) == Consts.NO_ACK){
                System.out.println("ERROR - sending data command, command not received.");
                return false;
            }
            if(!DeviceStatus()){
                System.out.println("ERROR - failed getting Status");
                return false;
            }
        }
        System.out.println("Success - Device programmed.");
        return true;
    }
    private int verify(int length){
        System.out.println("Verifying device.");
        byte[] crcCMD = new byte[12], ret;
        int retInd;
        System.arraycopy(toBytes(Consts.CC26XX_FLASH_BASE),0,crcCMD,0,4);
        System.arraycopy(toBytes(length),0,crcCMD,4,4);
        System.arraycopy(toBytes(Consts.ReadRepeat),0,crcCMD,8,4);
        retInd = sendCmd(ECc2650Commands.CMD_CRC32,crcCMD);
        if(retInd == Consts.NO_ACK){
            System.out.println("ERROR - sending crc32 command, command not received.");
            return -1;
        }
        sendAck();
        ret = proxy.getResponse();
        return toInt(ret,retInd+3);
    }

    private long CalcCRC(byte[] data) {
        System.out.println("Calculating data CRC32.");
        CRC32 crc32 = new CRC32();
        crc32.update(data,0,data.length);
        return crc32.getValue();
    }

    private boolean reset(){
        System.out.println("resetting device");
        return sendCmd(ECc2650Commands.CMD_RESET, Consts.EMPTY_MESSAGE) != Consts.NO_ACK;
    }
    private int sendCmd(ECc2650Commands cmd, byte[] data){
        int len = data.length+3, i;
        byte[] message = new byte[len];
        int ret =-1;
        message[0] = (byte)len;
        message[1]= calcCeckSum(cmd,data);
        message[2] = cmd.getValue();
        System.arraycopy(data,0,message,3,data.length);
        for( i=0; i < Consts.SEND_TRIES; i++){
            network.writeSerial(len,message);
            ret = NetworkProxy.AckNack(proxy.awaitResponse());
            switch (ret){
                case Consts.NO_ACK:
                    continue;
                default :
                    return ret;
            }
        }
        return ret;
    }
    private void sendAck(){
        network.writeSerial(1,new byte[]{Consts.ACK});
    }

    private boolean DeviceStatus(){
        int retInd;
        ECommandReturn ret;
        retInd = sendCmd(ECc2650Commands.CMD_GET_STATUS,Consts.EMPTY_MESSAGE);
        if(retInd==Consts.NO_ACK){
            System.out.println("ERROR - no return status.");
            return false;
        }
        sendAck();
        ret = NetworkProxy.getCmdRetValue(proxy.getResponse(), retInd);
        if(ret != ECommandReturn.COMMAND_RETURN_SUCCESS){
            System.out.println("ERROR - " + ret +".");
            return false;
        }
        return true;
    }
    private static byte calcCeckSum(ECc2650Commands cmd, byte[] data)
    {
        byte cs = cmd.getValue();
        for (byte d:data){
            cs += d;
        }
        return cs;
    }
    private static byte[] toBytes(int i)
    {
        byte[] result = new byte[4];

        result[0] = (byte) (i >> 24);
        result[1] = (byte) (i >> 16);
        result[2] = (byte) (i >> 8);
        result[3] = (byte) (i /*>> 0*/);

        return result;
    }
    private static int toInt(byte[] message, int ind)
    {
        int i;
        i = message[ind]&0xff;
        i<<=8;
        i += message[ind +1]&0xff;
        i<<=8;
        i += message[ind+2]&0xff;
        i<<=8;
        i += message[ind+3]&0xff;

        return i;
    }
}