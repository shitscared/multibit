/**
 * Copyright 2012 wallet.org
 *
 * Licensed under the MIT license (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://opensource.org/licenses/mit-license.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.fastcoin.core;

import java.io.File;
import java.util.List;

import com.google.fastcoin.store.BlockStore;
import com.google.fastcoin.store.BlockStoreException;
import com.google.fastcoin.store.SPVBlockStore;

/**
 * Extension of fastcoinj BlockChain for use with block chain replay.
 */
public class FastcoinWalletBlockChain extends BlockChain {

    public FastcoinWalletBlockChain(NetworkParameters params, Wallet wallet, BlockStore blockStore) throws BlockStoreException {
        super(params, wallet, blockStore);
    }

    public FastcoinWalletBlockChain(NetworkParameters params, BlockStore blockStore) throws BlockStoreException {
        super(params, blockStore);
    }

    public FastcoinWalletBlockChain(NetworkParameters params, List<BlockChainListener> wallets, BlockStore blockStore) throws BlockStoreException {
        super(params, wallets, blockStore);
    }
    
    /**
     * Set the chainhead, clear any cached blocks and truncate the blockchain .
     * (Used for blockchain replay).
     * @param chainHead
     * @throws BlockStoreException
     */
    public void setChainHeadClearCachesAndTruncateBlockStore(StoredBlock chainHead, String blockStoreFilename) throws BlockStoreException {
        if (blockStore instanceof SPVBlockStore) {
            // Delete the blockstore and recreate it.
            ((SPVBlockStore) blockStore).close();

            File blockStoreFile = new File(blockStoreFilename);
            blockStoreFile.delete();
        } else {
            blockStore.setChainHead(chainHead);
            super.setChainHead(chainHead);
        }
    }
}
