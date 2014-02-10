package org.wallet.file;

import org.wallet.controller.fastcoin.FastcoinController;
import org.wallet.exchange.CurrencyConverter;
import org.wallet.exchange.CurrencyInfo;
import org.wallet.model.fastcoin.WalletTableData;

import com.googlecode.jcsv.writer.CSVEntryConverter;

/**
 * Create a CSVEntryConverter for the header values in the CSV
 */
public class WalletTableDataHeaderEntryConverter implements CSVEntryConverter<WalletTableData> {

    FastcoinController fastcoinController = null;

    @Override
    public String[] convertEntry(WalletTableData walletTableData) {
        String[] columns = new String[5];

        // Date.
        columns[0] = fastcoinController.getLocaliser().getString("walletData.dateText");
        
        // Description.
        columns[1] = fastcoinController.getLocaliser().getString("walletData.descriptionText");

        // Amount in FST.
        columns[2] = fastcoinController.getLocaliser().getString("sendFastcoinPanel.amountLabel") + " (" + fastcoinController.getLocaliser().getString("sendFastcoinPanel.amountUnitLabel") + ")";;
        
        // Amount in fiat
        if (CurrencyConverter.INSTANCE.isShowingFiat()) {
            CurrencyInfo currencyInfo = CurrencyConverter.INSTANCE.getCurrencyCodeToInfoMap().get(CurrencyConverter.INSTANCE.getCurrencyUnit().getCode());
            String currencySymbol = CurrencyConverter.INSTANCE.getCurrencyUnit().getCode();
            if (currencyInfo != null) {
                currencySymbol = currencyInfo.getCurrencySymbol();
            }
            columns[3] = fastcoinController.getLocaliser().getString("sendFastcoinPanel.amountLabel") + " (" + currencySymbol + ")";
        } else {
            columns[3] = "";
        }
         
        // Transaction hash.
        columns[4] = fastcoinController.getLocaliser().getString("exportTransactionsSubmitAction.transactionId");

        return columns;
    }

    public void setFastcoinController(FastcoinController fastcoinController) {
        this.fastcoinController = fastcoinController;
    }
}
