package de.eMark.tools.database;

import android.arch.persistence.room.Room;
import android.os.Handler;
import android.os.Looper;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.eMark.eMark;

public class Database {
    public static Database instance = new Database();
    public TransactionDao db;
    private Executor transactionExecutor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());
    public List<DigiTransaction> transactions = new LinkedList<>();

    public interface TransacionStoreListener {
        void onTransactionsUpdate();
    }

    public Database() {
        AppDatabase database = Room.databaseBuilder(eMark.getContext(),
                AppDatabase.class, "transaction_database")
                .fallbackToDestructiveMigration().build();
        db = database.transactionDao();
        update(null);
    }

    public void saveTransaction(byte[] txHash, String amount) {
        transactionExecutor.execute(() -> {
            DigiTransaction digiTransaction = new DigiTransaction();
            digiTransaction.setTxHash(txHash);
            digiTransaction.setTxAmount(amount);
            db.insertAll(digiTransaction);
            transactions = db.getAll();
        });
    }

    public void update(TransacionStoreListener transacionStoreListener) {
        transactionExecutor.execute(() -> {
            transactions = db.getAll();
            handler.post(() -> {
                if (transacionStoreListener != null) {
                    transacionStoreListener.onTransactionsUpdate();
                }
            });
        });
    }

    public boolean containsTransaction(byte[] txHash) {
        return findTransaction(txHash) != null;
    }

    public DigiTransaction findTransaction(byte[] txHash) {
        for (DigiTransaction digiTransaction : transactions) {
            if (Arrays.equals(txHash, digiTransaction.getTxHash())) {
                return digiTransaction;
            }
        }
        return null;
    }
}

