package SBL_CC2650;

/**
 * Created by Liat n on 16/03/2017.
 */
public enum ECc2650Commands {
    CMD_DUMMY(0x00),
    CMD_PING(0x20),
    CMD_DOWNLOAD(0x21),
    CMD_GET_STATUS(0x23),
    CMD_SEND_DATA(0x24),
    CMD_RESET(0x25),
    CMD_SECTOR_ERASE(0x26),
    CMD_CRC32(0x27),
    CMD_GET_CHIP_ID(0x28),
    CMD_MEMORY_READ(0x2A),
    CMD_MEMORY_WRITE(0x2B),
    CMD_BANK_ERASE(0x2C),
    CMD_SET_CCFG(0x2D);
    private final byte cmd;
    ECc2650Commands(int cmd){this.cmd =(byte)cmd;}
    public byte getValue(){ return cmd;}
}
