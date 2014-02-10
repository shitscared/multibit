package org.wallet.controller.fastcoin;

import com.google.fastcoin.core.*;
import org.wallet.controller.Controller;
import org.wallet.model.fastcoin.WalletData;
import org.wallet.model.core.StatusEnum;
import org.wallet.network.ReplayManager;
import org.wallet.viewsystem.swing.view.panels.SendFastcoinConfirmPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FastcoinPeerEventListener implements PeerEventListener {

  private Logger log = LoggerFactory.getLogger(FastcoinPeerEventListener.class);

  private final Controller controller;
  private final FastcoinController fastcoinController;

  public FastcoinPeerEventListener(FastcoinController fastcoinController) {
    this.fastcoinController = fastcoinController;
    this.controller = this.fastcoinController;
  }

  @Override
  public void onBlocksDownloaded(Peer peer, Block block, int blocksLeft) {
    this.fastcoinController.fireBlockDownloaded();

    if (blocksLeft == 0) {
      ReplayManager.INSTANCE.downloadHasCompleted();
    }
  }

  @Override
  public void onChainDownloadStarted(Peer peer, int blocksLeft) {
    if (blocksLeft == 0) {
      ReplayManager.INSTANCE.downloadHasCompleted();
    }
    this.fastcoinController.fireBlockDownloaded();
  }

  @Override
  public void onPeerConnected(Peer peer, int peerCount) {
    if (peer != null) {
      log.debug("Connected to peer:" + peer.getPeerVersionMessage());
    }
    if (peerCount >= 1) {
      controller.setOnlineStatus(StatusEnum.ONLINE);
    }
    if (controller.getModel() != null) {
      this.fastcoinController.getModel().setNumberOfConnectedPeers(peerCount);
    }
    SendFastcoinConfirmPanel.updatePanel();
  }

  @Override
  public void onPeerDisconnected(Peer peer, int peerCount) {
    if (peer != null) {
      log.debug("Disconnected from peer, address : " + peer.getAddress() + ", peerCount = " + peerCount);
    }
    if (peerCount == 0) {
      controller.setOnlineStatus(StatusEnum.CONNECTING);
    }
    if (controller.getModel() != null) {
      this.fastcoinController.getModel().setNumberOfConnectedPeers(peerCount);
    }
    SendFastcoinConfirmPanel.updatePanel();
  }

  @Override
  public Message onPreMessageReceived(Peer peer, Message message) {
    return message;
  }

  @Override
  public void onTransaction(Peer peer, Transaction transaction) {
    // Loop through all the wallets, seeing if the transaction is relevant and adding them as pending if so.
    if (transaction != null) {
      try {
        java.util.List<WalletData> perWalletModelDataList = fastcoinController.getModel().getPerWalletModelDataList();

        if (perWalletModelDataList != null) {
          for (WalletData perWalletModelData : perWalletModelDataList) {
            Wallet loopWallet = perWalletModelData.getWallet();
            if (loopWallet != null) {
              if (loopWallet.isTransactionRelevant(transaction)) {
                if (!(transaction.isTimeLocked() && transaction.getConfidence().getSource() != TransactionConfidence.Source.SELF)) {
                  if (loopWallet.getTransaction(transaction.getHash()) == null) {
                    log.debug("FastcoinWallet adding a new pending transaction for the wallet '"
                            + perWalletModelData.getWalletDescription() + "'\n" + transaction.toString());
                    // The perWalletModelData is marked as dirty.
                    if (perWalletModelData.getWalletInfo() != null) {
                      synchronized (perWalletModelData.getWalletInfo()) {
                        perWalletModelData.setDirty(true);
                      }
                    } else {
                      perWalletModelData.setDirty(true);
                    }
                    loopWallet.receivePending(transaction, null);
                  }
                }
              }
            }
          }
        }
      } catch (ScriptException e) {
        log.error(e.getMessage(), e);
      } catch (VerificationException e) {
        log.error(e.getMessage(), e);
      }
    }
  }


  @Override
  public List<Message> getData(Peer peer, GetDataMessage m) {
    return null;
  }
}
