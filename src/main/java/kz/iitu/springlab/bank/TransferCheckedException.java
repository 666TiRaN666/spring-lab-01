package kz.iitu.springlab.bank;

/** Checked exception: by default Spring does NOT roll back a transaction for it. */
public class TransferCheckedException extends Exception {
    public TransferCheckedException(String message) { super(message); }
}
