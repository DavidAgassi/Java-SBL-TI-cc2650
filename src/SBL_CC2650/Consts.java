package SBL_CC2650;

/**
 * Created by Liat n on 16/03/2017.
 */
class Consts {
    static final int SBL_CC2650_MAX_BYTES_PER_TRANSFER = 252;
    static final int CC26XX_FLASH_BASE = 0x00000000;
    static final byte[] SYNC_MESSAGE = new byte[]{0x55,0x55};
    static final byte[] EMPTY_MESSAGE = new byte[]{};
    static final long RESPONSE_TIME = 500;
    static final long WAITE_SLEEP = 10;
    static final byte ACK = (byte) 0xcc;
    static final byte NACK = 0x33;
    static final int SEND_TRIES = 3;
    static final int NO_ACK = -1;
    static final int ReadRepeat= 0x00000000;
}
