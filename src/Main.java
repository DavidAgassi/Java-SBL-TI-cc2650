import SBL_CC2650.CC2650Bootloader;

import java.io.IOException;

/**
 * Created by Liat n on 16/03/2017.
 */
public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        CC2650Bootloader bootloader = new CC2650Bootloader();
        bootloader.burn(args[0], args[1]);
    }
}
