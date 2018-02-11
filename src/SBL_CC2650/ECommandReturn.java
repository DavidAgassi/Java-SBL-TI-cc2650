package SBL_CC2650;

/**
 * Created by Liat n on 18/03/2017.
 */
public enum ECommandReturn {
    COMMAND_RETURN_NULL(0xFF),
    COMMAND_RETURN_SUCCESS(0x40),
    COMMAND_RETURN_UNKNOWN_CMD(0x41),
    COMMAND_RETURN_INVALID_CMD(0x42),
    COMMAND_RETURN_INVALID_ADD(0x43),
    COMMAND_RETURN_FLASH_FAIL(0x44);
    private final byte cmd;
    ECommandReturn(int cmd){this.cmd =(byte)cmd;}
    public byte getValue(){ return cmd;}
}
