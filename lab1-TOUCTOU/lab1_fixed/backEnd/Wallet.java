package backEnd;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;

public class Wallet {


    /**
     * The RandomAccessFile of the wallet file
     */  
    private RandomAccessFile file;
    private final Lock lock = new ReentrantLock();  // locked object

    /**
     * Creates a Wallet object
     *
     * A Wallet object interfaces with the wallet RandomAccessFile
     */
    public Wallet () throws Exception {
	this.file = new RandomAccessFile(new File("backEnd/wallet.txt"), "rw");
    }

    /**
     * Gets the wallet balance. 
     *
     * @return                   The content of the wallet file as an integer
     */
    public int getBalance() throws IOException {
	this.file.seek(0);
	return Integer.parseInt(this.file.readLine());
    }

    /**
     * Sets a new balance in the wallet
     *
     * @param  newBalance          new balance to write in the wallet
     */
    public void setBalance(int newBalance) throws Exception {
	this.file.setLength(0);
	String str = Integer.valueOf(newBalance).toString()+'\n'; 
	this.file.writeBytes(str); 
    }
    public boolean safeWithdraw(int valueToWithdraw) throws Exception {
        //optimize，return directly if balance not enough, avoid lock's overhead
        if (getBalance() < valueToWithdraw) {
            return false;
        }

        lock.lock();
        try {
            int currentBalance = getBalance();
            if (currentBalance >= valueToWithdraw) {
                int newBalance = currentBalance - valueToWithdraw;
                setBalance(newBalance);
                return true;
            } else {
                return false;
            }
        } finally {
            lock.unlock(); // unlock, if successful or not
        }
    }

    /**
     * Closes the RandomAccessFile in this.file
     */
    public void close() throws Exception {
	this.file.close();
    }
}
