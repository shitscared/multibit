/**
 * Copyright 2011 wallet.org
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
package org.multibit;

import com.alee.laf.WebLookAndFeel;
import com.google.fastcoin.core.StoredBlock;
import com.google.fastcoin.core.Wallet;
import com.google.fastcoin.kits.WalletAppKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.multibit.controller.Controller;
import org.multibit.controller.core.CoreController;
import org.multibit.controller.exchange.ExchangeController;
import org.multibit.controller.fastcoin.FastcoinController;
import org.multibit.exchange.CurrencyConverter;
import org.multibit.file.BackupManager;
import org.multibit.file.FileHandler;
import org.multibit.file.WalletLoadException;
import org.multibit.message.Message;
import org.multibit.message.MessageManager;
import org.multibit.model.core.CoreModel;
import org.multibit.model.exchange.ConnectHttps;
import org.multibit.model.exchange.ExchangeModel;
import org.multibit.model.fastcoin.WalletData;
import org.multibit.model.fastcoin.FastcoinModel;
import org.multibit.network.*;
import org.multibit.platform.GenericApplication;
import org.multibit.platform.GenericApplicationFactory;
import org.multibit.platform.GenericApplicationSpecification;
import org.multibit.platform.listener.GenericOpenURIEvent;
import org.wallet.store.WalletVersionException;
import org.multibit.viewsystem.DisplayHint;
import org.multibit.viewsystem.ViewSystem;
import org.multibit.viewsystem.swing.ColorAndFontConstants;
import org.multibit.viewsystem.swing.FastcoinWalletFrame;
import org.multibit.viewsystem.swing.action.ExitAction;
import org.multibit.viewsystem.swing.view.components.FontSizer;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.*;
import java.util.List;

/**
 * Main MultiBitWallet entry class.
 *
 * @author jim
 */
public final class MultiBitWallet {

    private static final Logger log = LoggerFactory.getLogger(MultiBitWallet.class);

    private static Controller controller = null;

    private static CoreController coreController = null;
    private static FastcoinController fastcoinController = null;
    private static ExchangeController exchangeController = null;

    private static String rememberedRawFastcoinURI;

    /**
     * Utility class should not have a public constructor
     */
    private MultiBitWallet() {
    }
    static WalletAppKit appKit = null;
    /**
     * Start MultiBitWallet user interface.
     *
     * @param args String encoding of arguments ([0]= Fastcoin URI)
     */
    @SuppressWarnings("deprecation")
    public static void main(final String args[]) {
        // You should work with UI (including installing L&F) inside Event Dispatch Thread (EDT)

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Install WebLaF as application L&F
                //WebLookAndFeel.install ();



                log.info("Starting MultiBitWallet at " + (new Date()).toGMTString());
                // Print out all the system properties.
                for (Map.Entry<?, ?> e : System.getProperties().entrySet()) {
                    log.debug(String.format("%s = %s", e.getKey(), e.getValue()));
                }

                ViewSystem swingViewSystem = null;
                // Enclosing try to enable graceful closure for unexpected errors.
                try {
                    // Set any bespoke system properties.
                    try {
                        // Fix for Windows / Java 7 / VPN bug.
                        System.setProperty("java.net.preferIPv4Stack", "true");

                        // Fix for version.txt not visible for Java 7
                        System.setProperty("jsse.enableSNIExtension", "false");
                    } catch (SecurityException se) {
                        log.error(se.getClass().getName() + " " + se.getMessage());
                    }

                    ApplicationDataDirectoryLocator applicationDataDirectoryLocator = new ApplicationDataDirectoryLocator();

                    // Load up the user preferences.
                    Properties userPreferences = FileHandler.loadUserPreferences(applicationDataDirectoryLocator);

                    // Create the controllers.
                    coreController = new CoreController(applicationDataDirectoryLocator);
                    controller = coreController;
                    fastcoinController = new FastcoinController(coreController);
                    exchangeController = new ExchangeController(coreController);

                    log.info("Configuring native event handling");
                    GenericApplicationSpecification specification = new GenericApplicationSpecification();
                    specification.getOpenURIEventListeners().add(coreController);
                    specification.getPreferencesEventListeners().add(coreController);
                    specification.getAboutEventListeners().add(coreController);
                    specification.getQuitEventListeners().add(coreController);
                    GenericApplication genericApplication = GenericApplicationFactory.INSTANCE.buildGenericApplication(specification);

                    log.info("Checking to see if this is the primary MultiBitWallet instance");
                    String rawURI = null;
                    if (args != null && args.length > 0) {
                        rawURI = args[0];
                        log.debug("The args[0] passed into MultiBitWallet = '" + args[0] + "'");
                    }
                    if (!ApplicationInstanceManager.registerInstance(rawURI)) {
                        // Instance already running.
                        log.debug("Another instance of MultiBitWallet is already running.  Exiting.");
                        System.exit(0);
                    }

                    final FastcoinController finalController = fastcoinController;
                    ApplicationInstanceManager.setApplicationInstanceListener(new ApplicationInstanceListener() {
                        @Override
                        public void newInstanceCreated(String rawURI) {
                            final String finalRawUri = rawURI;
                            log.debug("New instance of MultiBitWallet detected, rawURI = " + rawURI + " ...");
                            Runnable doProcessCommandLine = new Runnable() {
                                @Override
                                public void run() {
                                    processCommandLineURI(finalController, finalRawUri);
                                }
                            };

                            SwingUtilities.invokeLater(doProcessCommandLine);
                        }
                    });

                    Localiser localiser;
                    String userLanguageCode = userPreferences.getProperty(CoreModel.USER_LANGUAGE_CODE);
                    log.debug("userLanguageCode = {}", userLanguageCode);

                    if (userLanguageCode == null) {
                        // Initial install - no language info supplied - see if we can
                        // use the user default, else Localiser will set it to English.
                        localiser = new Localiser(Locale.getDefault());

                        userPreferences.setProperty(CoreModel.USER_LANGUAGE_CODE, localiser.getLocale().getLanguage());
                    } else {
                        if (CoreModel.USER_LANGUAGE_IS_DEFAULT.equals(userLanguageCode)) {
                            localiser = new Localiser(Locale.getDefault());
                        } else {
                            localiser = new Localiser(new Locale(userLanguageCode));
                        }
                    }
                    coreController.setLocaliser(localiser);

                    log.debug("MultiBitWallet version = " + localiser.getVersionNumber());

                    log.debug("Creating model");

                    // Create the model.
                    // The model is set to the controller.
                    final CoreModel coreModel = new CoreModel(userPreferences);
                    final FastcoinModel model = new FastcoinModel(coreModel);
                    final ExchangeModel exchangeModel = new ExchangeModel(coreModel);
                    coreController.setModel(coreModel);
                    fastcoinController.setModel(model);
                    exchangeController.setModel(exchangeModel);

                    // Trust all HTTPS certificates.
                    ConnectHttps.trustAllCerts();

                    // Initialise currency converter.
                    CurrencyConverter.INSTANCE.initialise(finalController);

                    // Initialise replay manager.
                    ReplayManager.INSTANCE.initialise(fastcoinController, false);

                    log.debug("Setting look and feel");
                    try {
                        String lookAndFeel = userPreferences.getProperty(CoreModel.LOOK_AND_FEEL);

                        // If not set on Windows use 'Windows' L&F as system can be rendered as metal.
                        if ((lookAndFeel == null || lookAndFeel.equals(""))) {
                            lookAndFeel = "com.alee.laf.WebLookAndFeel";
                            userPreferences.setProperty(CoreModel.LOOK_AND_FEEL, lookAndFeel);
                        }

                        boolean lookSet = false;
                        if (lookAndFeel != null && !lookAndFeel.equals("")) {
                            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                                if (lookAndFeel.equalsIgnoreCase(info.getName())) {
                                    UIManager.setLookAndFeel(info.getClassName());
                                    lookSet = true;
                                    break;
                                }
                            }
                        }

                        if (!lookSet) {
                            UIManager.setLookAndFeel(WebLookAndFeel.class.getCanonicalName());
                        }
                    } catch (UnsupportedLookAndFeelException e) {
                        // Carry on.
                    } catch (ClassNotFoundException e) {
                        // Carry on.
                    } catch (InstantiationException e) {
                        // Carry on.
                    } catch (IllegalAccessException e) {
                        // Carry on.
                    }

                    // Initialise singletons.
                    ColorAndFontConstants.init();
                    FontSizer.INSTANCE.initialise(controller);
                    CurrencyConverter.INSTANCE.initialise(finalController);

                    // This is when the GUI is first displayed to the user.
                    log.debug("Creating user interface with initial view : " + controller.getCurrentView());
                    swingViewSystem = new FastcoinWalletFrame(coreController, fastcoinController, exchangeController, genericApplication, controller.getCurrentView());

                    log.debug("Registering with controller");
                    coreController.registerViewSystem(swingViewSystem);

                    String userDataString = localiser.getString("fastcoin-wallet.userDataDirectory", new String[]{applicationDataDirectoryLocator.getApplicationDataDirectory()});
                    log.debug(userDataString);
                    Message directoryMessage1 = new Message(userDataString);
                    directoryMessage1.setShowInStatusBar(false);
                    MessageManager.INSTANCE.addMessage(directoryMessage1);

                    String installationDirString = localiser.getString("fastcoin-wallet.installationDirectory", new String[]{applicationDataDirectoryLocator.getInstallationDirectory()});
                    log.debug(installationDirString);
                    Message directoryMessage2 = new Message(installationDirString);
                    directoryMessage2.setShowInStatusBar(false);
                    MessageManager.INSTANCE.addMessage(directoryMessage2);

                    log.debug("Creating Fastcoin service");
                    // Create the FastcoinWalletService that connects to the fastcoin network.
                    FastcoinWalletService fastcoinWalletService = new FastcoinWalletService(fastcoinController);
                    fastcoinController.setFastcoinWalletService(fastcoinWalletService);

                    log.debug("Locating wallets");
                    // Find the active wallet filename in the wallet.properties.
                    String activeWalletFilename = userPreferences.getProperty(FastcoinModel.ACTIVE_WALLET_FILENAME);

                    // Get the number of the early wallets - these are serialised and protobuf2
                    String numberOfEarlyWalletsAsString = userPreferences.getProperty(FastcoinModel.NUMBER_OF_EARLY_WALLETS);
                    log.debug("When loading early wallets, there were " + numberOfEarlyWalletsAsString);

                    // Get the number of the protobuf3 wallets
                    String numberOfProtobuf3WalletsAsString = userPreferences.getProperty(FastcoinModel.NUMBER_OF_PROTOBUF3_WALLETS);
                    log.debug("When loading protobuf3 wallets, there were " + numberOfProtobuf3WalletsAsString);

                    boolean useFastCatchup = false;

                    if (numberOfEarlyWalletsAsString == null || "".equals(numberOfEarlyWalletsAsString) || "null".equals(numberOfEarlyWalletsAsString)) {
                        // If this is missing then there is just the one wallet (old format
                        // properties or user has just started up for the first time).
                        useFastCatchup = true;
                        boolean thereWasAnErrorLoadingTheWallet = false;

                        try {
                            boolean backupWallet = false;
                            boolean moveSiblingFiles = false;

                            // If there is no active filename this is a new instance of MultiBitWallet so backup the new wallet when created.
                            if (activeWalletFilename == null || "".equals(activeWalletFilename) || "null".equals(activeWalletFilename)) {
                                backupWallet = true;
                            } else {
                                // See if a data directory is missing - if so we will move in any wallet or key files and backup.
                                String topLevelWalletDirectory = BackupManager.INSTANCE.calculateTopLevelBackupDirectoryName(new File(activeWalletFilename));
                                moveSiblingFiles = !(new File(topLevelWalletDirectory).exists());
                                backupWallet = moveSiblingFiles;
                            }

                            // ActiveWalletFilename may be null on first time startup.
                            fastcoinController.addWalletFromFilename(activeWalletFilename);
                            List<WalletData> perWalletModelDataList = fastcoinController.getModel().getPerWalletModelDataList();
                            if (perWalletModelDataList != null && !perWalletModelDataList.isEmpty()) {
                                activeWalletFilename = perWalletModelDataList.get(0).getWalletFilename();
                                fastcoinController.getModel().setActiveWalletByFilename(activeWalletFilename);
                                log.debug("Created/loaded wallet '" + activeWalletFilename + "'");
                                MessageManager.INSTANCE.addMessage(new Message(controller.getLocaliser().getString(
                                        "fastcoinWallet.createdWallet", new Object[]{activeWalletFilename})));

                                if (backupWallet) {
                                    // Backup the wallet and wallet info.
                                    BackupManager.INSTANCE.backupPerWalletModelData(fastcoinController.getFileHandler(), perWalletModelDataList.get(0));
                                }
                                if (moveSiblingFiles) {
                                    // Move any timestamped key and wallet files into their appropriate directories
                                    BackupManager.INSTANCE.moveSiblingTimestampedKeyAndWalletBackups(activeWalletFilename);
                                }
                            }
                        } catch (WalletLoadException e) {
                            String message = controller.getLocaliser().getString("openWalletSubmitAction.walletNotLoaded",
                                    new Object[]{activeWalletFilename, e.getMessage()});
                            MessageManager.INSTANCE.addMessage(new Message(message));
                            log.error(message);
                            thereWasAnErrorLoadingTheWallet = true;
                        } catch (WalletVersionException e) {
                            String message = controller.getLocaliser().getString("openWalletSubmitAction.walletNotLoaded",
                                    new Object[]{activeWalletFilename, e.getMessage()});
                            MessageManager.INSTANCE.addMessage(new Message(message));
                            log.error(message);
                            thereWasAnErrorLoadingTheWallet = true;
                        } catch (IOException e) {
                            String message = controller.getLocaliser().getString("openWalletSubmitAction.walletNotLoaded",
                                    new Object[]{activeWalletFilename, e.getMessage()});
                            MessageManager.INSTANCE.addMessage(new Message(message));
                            log.error(message);
                            thereWasAnErrorLoadingTheWallet = true;
                        } catch (Exception e) {
                            String message = controller.getLocaliser().getString("openWalletSubmitAction.walletNotLoaded",
                                    new Object[]{activeWalletFilename, e.getMessage()});
                            MessageManager.INSTANCE.addMessage(new Message(message));
                            log.error(message);
                            thereWasAnErrorLoadingTheWallet = true;
                        } finally {
                            if (thereWasAnErrorLoadingTheWallet) {
                                // Clear the backup wallet filename - this prevents it being automatically overwritten.
                                if (fastcoinController.getModel().getActiveWalletInfo() != null) {
                                    fastcoinController.getModel().getActiveWalletInfo().put(FastcoinModel.WALLET_BACKUP_FILE, "");
                                }
                            }
                            if (swingViewSystem instanceof FastcoinWalletFrame) {
                                ((FastcoinWalletFrame) swingViewSystem).getWalletsView().initUI();
                                ((FastcoinWalletFrame) swingViewSystem).getWalletsView().displayView(DisplayHint.COMPLETE_REDRAW);
                            }
                            controller.fireDataChangedUpdateNow();
                        }
                    } else {
                        try {
                            List<String> walletFilenamesToLoad = new ArrayList<String>();
                            try {
                                int numberOfEarlyWallets = Integer.parseInt(numberOfEarlyWalletsAsString);
                                if (numberOfEarlyWallets > 0) {
                                    for (int i = 1; i <= numberOfEarlyWallets; i++) {
                                        // Look up ith wallet filename.
                                        String loopWalletFilename = userPreferences.getProperty(FastcoinModel.EARLY_WALLET_FILENAME_PREFIX + i);
                                        if (!walletFilenamesToLoad.contains(loopWalletFilename)) {
                                            walletFilenamesToLoad.add(loopWalletFilename);
                                        }
                                    }
                                }
                            } catch (NumberFormatException nfe) {
                                // Carry on.
                            }
                            try {
                                int numberOfProtobuf3Wallets = Integer.parseInt(numberOfProtobuf3WalletsAsString);
                                if (numberOfProtobuf3Wallets > 0) {
                                    for (int i = 1; i <= numberOfProtobuf3Wallets; i++) {
                                        // Look up ith wallet filename.
                                        String loopWalletFilename = userPreferences.getProperty(FastcoinModel.PROTOBUF3_WALLET_FILENAME_PREFIX + i);
                                        if (!walletFilenamesToLoad.contains(loopWalletFilename)) {
                                            walletFilenamesToLoad.add(loopWalletFilename);
                                        }
                                    }
                                }
                            } catch (NumberFormatException nfe) {
                                // Carry on.
                            }

                            // Load up the order the wallets are to appear in.
                            // There may be wallets in this list of types from the future but only load wallets we know about
                            boolean haveWalletOrder = false;
                            List<String> walletFilenameOrder = new ArrayList<String>();
                            try {
                                String walletOrderTotalAsString = userPreferences.getProperty(FastcoinModel.WALLET_ORDER_TOTAL);
                                log.debug("When loading the wallet orders, there were " + walletOrderTotalAsString);

                                int walletOrderTotal = Integer.parseInt(walletOrderTotalAsString);
                                if (walletOrderTotal > 0) {
                                    haveWalletOrder = true;
                                    for (int i = 1; i <= walletOrderTotal; i++) {
                                        // Add the wallet filename order.
                                        String loopWalletFilename = userPreferences.getProperty(FastcoinModel.WALLET_ORDER_PREFIX + i);
                                        if (!walletFilenameOrder.contains(loopWalletFilename)) {
                                            walletFilenameOrder.add(loopWalletFilename);
                                        }
                                    }
                                }
                            } catch (NumberFormatException nfe) {
                                // Carry on.
                            }

                            List<String> actualOrderToLoad = new ArrayList<String>();
                            if (haveWalletOrder) {
                                for (String orderWallet : walletFilenameOrder) {
                                    if (walletFilenamesToLoad.contains(orderWallet)) {
                                        // Add it.
                                        actualOrderToLoad.add(orderWallet);
                                    }
                                }
                                // There may be some extras so add them to the end.
                                for (String loadWallet : walletFilenamesToLoad) {
                                    if (!walletFilenameOrder.contains(loadWallet)) {
                                        // Add it.
                                        actualOrderToLoad.add(loadWallet);
                                    }
                                }
                            } else {
                                // Just load all the wallets, early then later.
                                for (String loadWallet : walletFilenamesToLoad) {
                                    if (!actualOrderToLoad.contains(loadWallet)) {
                                        // Add it.
                                        actualOrderToLoad.add(loadWallet);
                                    }
                                }
                            }

                            if (actualOrderToLoad.size() > 0) {
                                boolean thereWasAnErrorLoadingTheWallet = false;

                                ((FastcoinWalletFrame) swingViewSystem).setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                                for (String actualOrder : actualOrderToLoad) {
                                    log.debug("Loading wallet from '{}'", actualOrder);
                                    Message message = new Message(controller.getLocaliser().getString("fastcoinWallet.openingWallet",
                                            new Object[]{actualOrder}));
                                    message.setShowInStatusBar(false);
                                    MessageManager.INSTANCE.addMessage(message);
                                    try {
                                        // Check if this is the first time this wallet has been opened post addition of data directories.
                                        String topLevelWalletDirectory = BackupManager.INSTANCE.calculateTopLevelBackupDirectoryName(new File(actualOrder));
                                        boolean firstUsageSinceWalletDirectoriesIntroduced = !(new File(topLevelWalletDirectory).exists());

                                        WalletData perWalletModelData = null;
                                        if (activeWalletFilename != null && activeWalletFilename.equals(actualOrder)) {
                                            perWalletModelData = fastcoinController.addWalletFromFilename(actualOrder);
                                            fastcoinController.getModel().setActiveWalletByFilename(actualOrder);
                                        } else {
                                            perWalletModelData = fastcoinController.addWalletFromFilename(actualOrder);
                                        }
                                        Message message2 = new Message(controller.getLocaliser().getString("fastcoinWallet.openingWalletIsDone",
                                                new Object[]{actualOrder}));
                                        message2.setShowInStatusBar(false);
                                        MessageManager.INSTANCE.addMessage(message2);

                                        if (firstUsageSinceWalletDirectoriesIntroduced) {
                                            if (perWalletModelData != null && perWalletModelData.getWallet() != null) {
                                                // Backup the wallet and wallet info.
                                                BackupManager.INSTANCE.backupPerWalletModelData(fastcoinController.getFileHandler(), perWalletModelData);

                                                // Move any timestamped key and wallet files into their appropriate directories
                                                BackupManager.INSTANCE.moveSiblingTimestampedKeyAndWalletBackups(actualOrder);
                                            }
                                        }
                                    } catch (WalletLoadException e) {
                                        message = new Message(controller.getLocaliser().getString("openWalletSubmitAction.walletNotLoaded",
                                                new Object[]{actualOrder, e.getMessage()}));
                                        MessageManager.INSTANCE.addMessage(message);
                                        log.error(message.getText());
                                        thereWasAnErrorLoadingTheWallet = true;
                                    } catch (WalletVersionException e) {
                                        message = new Message(controller.getLocaliser().getString("openWalletSubmitAction.walletNotLoaded",
                                                new Object[]{actualOrder, e.getMessage()}));
                                        MessageManager.INSTANCE.addMessage(message);
                                        log.error(message.getText());
                                        thereWasAnErrorLoadingTheWallet = true;
                                    } catch (IOException e) {
                                        message = new Message(controller.getLocaliser().getString("openWalletSubmitAction.walletNotLoaded",
                                                new Object[]{actualOrder, e.getMessage()}));
                                        MessageManager.INSTANCE.addMessage(message);
                                        log.error(message.getText());
                                        thereWasAnErrorLoadingTheWallet = true;
                                    } catch (Exception e) {
                                        message = new Message(controller.getLocaliser().getString("openWalletSubmitAction.walletNotLoaded",
                                                new Object[]{actualOrder, e.getMessage()}));
                                        MessageManager.INSTANCE.addMessage(message);
                                        log.error(message.getText());
                                        thereWasAnErrorLoadingTheWallet = true;
                                    }

                                    if (thereWasAnErrorLoadingTheWallet) {
                                        WalletData loopData = fastcoinController.getModel().getPerWalletModelDataByWalletFilename(actualOrder);
                                        if (loopData != null) {
                                            // Clear the backup wallet filename - this prevents it being automatically overwritten.
                                            if (loopData.getWalletInfo() != null) {
                                                loopData.getWalletInfo().put(FastcoinModel.WALLET_BACKUP_FILE, "");
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (NumberFormatException nfe) {
                            // Carry on.
                        } finally {
                            if (swingViewSystem instanceof FastcoinWalletFrame) {
                                ((FastcoinWalletFrame) swingViewSystem).getWalletsView().initUI();
                                ((FastcoinWalletFrame) swingViewSystem).getWalletsView().displayView(DisplayHint.COMPLETE_REDRAW);
                            }
                            controller.fireDataChangedUpdateNow();

                            ((FastcoinWalletFrame) swingViewSystem).setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        }
                    }

                    log.debug("Checking for Fastcoin URI on command line");
                    // Check for a valid entry on the command line (protocol handler).
                    if (args != null && args.length > 0) {
                        for (int i = 0; i < args.length; i++) {
                            log.debug("Started with args[{}]: '{}'", i, args[i]);
                        }
                        processCommandLineURI(fastcoinController, args[0]);
                    } else {
                        log.debug("No Fastcoin URI provided as an argument");
                    }

                    // Indicate to the application that startup has completed.
                    coreController.setApplicationStarting(false);

                    // Check for any pending URI operations.
                    fastcoinController.handleOpenURI(rememberedRawFastcoinURI);

                    // Check to see if there is a new version.
                    AlertManager.INSTANCE.initialise(fastcoinController, (FastcoinWalletFrame) swingViewSystem);
                    AlertManager.INSTANCE.checkVersion();

                    log.debug("Downloading blockchain");
                    if (useFastCatchup) {
                        long earliestTimeSecs = fastcoinController.getModel().getActiveWallet().getEarliestKeyCreationTime();
                        fastcoinController.getFastcoinWalletService().getPeerGroup().setFastCatchupTimeSecs(earliestTimeSecs);
                        log.debug("Using FastCatchup for blockchain sync with time of " + (new Date(earliestTimeSecs)).toString());
                    }

                    // Work out the late date/ block the wallets saw to see if it needs syncing
                    // or if we can use regular downloading.
                    int currentChainHeight = -1;
                    if (fastcoinController.getFastcoinWalletService().getChain() != null) {
                        if (fastcoinController.getFastcoinWalletService().getChain().getChainHead() != null) {
                            currentChainHeight = fastcoinController.getFastcoinWalletService().getChain().getChainHead().getHeight();
                        }
                    }

                    log.debug("The current chain height is " + currentChainHeight);

                    List<WalletData> perWalletModelDataList = fastcoinController.getModel().getPerWalletModelDataList();
                    boolean needToSync = false;
                    int syncFromHeight = -1;

                    List<WalletData> replayPerWalletModelList = new ArrayList<WalletData>();
                    if (perWalletModelDataList != null) {
                        for (WalletData perWalletModelData : perWalletModelDataList) {
                            Wallet wallet = perWalletModelData.getWallet();
                            if (wallet != null) {
                                int lastBlockSeenHeight = wallet.getLastBlockSeenHeight();
                                log.debug("For wallet '" + perWalletModelData.getWalletFilename() + " the lastBlockSeenHeight was "
                                        + lastBlockSeenHeight);

                                // Check if we have both the lastBlockSeenHeight and the currentChainHeight.
                                if (lastBlockSeenHeight > 0 && currentChainHeight > 0) {
                                    if (lastBlockSeenHeight >= currentChainHeight) {
                                        // MultiBitWallet is at or ahead of current chain - no
                                        // need to sync for this wallet.
                                    } else {
                                        // MultiBitWallet is behind the current chain - need to sync.
                                        needToSync = true;

                                        replayPerWalletModelList.add(perWalletModelData);
                                        if (syncFromHeight == -1) {
                                            syncFromHeight = lastBlockSeenHeight;
                                        } else {
                                            syncFromHeight = Math.min(syncFromHeight, lastBlockSeenHeight);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    log.debug("needToSync = " + needToSync);

                    if (needToSync) {
                        StoredBlock syncFromStoredBlock = null;

                        FastcoinWalletCheckpointManager checkpointManager = fastcoinController.getFastcoinWalletService().getCheckpointManager();
                        if (checkpointManager != null) {
                            syncFromStoredBlock = checkpointManager.getCheckpointBeforeOrAtHeight(syncFromHeight);
                        }

                        ReplayTask replayTask;
                        if (syncFromStoredBlock == null) {
                            // Sync from genesis block.
                            replayTask = new ReplayTask(replayPerWalletModelList, null, 0);
                        } else {
                            Date syncDate = null;
                            if (syncFromStoredBlock.getHeader() != null) {
                                syncDate = new Date(syncFromStoredBlock.getHeader().getTimeSeconds() * 1000);
                            }
                            replayTask = new ReplayTask(replayPerWalletModelList, syncDate, syncFromStoredBlock.getHeight());
                        }
                        ReplayManager.INSTANCE.offerReplayTask(replayTask);
                    } else {
                        // Just sync the blockchain without a replay task being involved.
                        ReplayManager.INSTANCE.downloadBlockChain();
                    }
                } catch (Exception e) {
                    // An odd unrecoverable error occurred.
                    e.printStackTrace();

                    log.error("An unexpected error caused MultiBitWallet to quit.");
                    log.error("The error was '" + e.getClass().getCanonicalName() + " " + e.getMessage() + "'");
                    e.printStackTrace();
                    log.error("Please read http://wallet.org/help_troubleshooting.html for help on troubleshooting.");

                    // Try saving any dirty wallets.
                    if (controller != null) {
                        ExitAction exitAction = new ExitAction(controller, (FastcoinWalletFrame) swingViewSystem);
                        exitAction.actionPerformed(null);
                    }
                }
            }
        });
    }

    static void processCommandLineURI(FastcoinController controller, String rawURI) {
        try {
            // Attempt to detect if the command line URI is valid.
            // Note that this is largely because IE6-8 strip URL encoding
            // when passing in URIs to a protocol handler.
            // However, there is also the chance that anyone could
            // hand-craft a URI and pass
            // it in with non-ASCII character encoding present in the label
            // This a really limited approach (no consideration of
            // "amount=10.0&label=Black & White")
            // but should be OK for early use cases.
            int queryParamIndex = rawURI.indexOf('?');
            if (queryParamIndex > 0 && !rawURI.contains("%")) {
                // Possibly encoded but more likely not
                String encodedQueryParams = URLEncoder.encode(rawURI.substring(queryParamIndex + 1), "UTF-8");
                rawURI = rawURI.substring(0, queryParamIndex) + "?" + encodedQueryParams;
                rawURI = rawURI.replaceAll("%3D", "=");
                rawURI = rawURI.replaceAll("%26", "&");
            }
            final URI uri;
            log.debug("Working with '{}' as a Fastcoin URI", rawURI);
            // Construct an OpenURIEvent to simulate receiving this from a
            // listener
            uri = new URI(rawURI);
            GenericOpenURIEvent event = new GenericOpenURIEvent() {
                @Override
                public URI getURI() {
                    return uri;
                }
            };
            controller.displayView(controller.getCurrentView());
            // Call the event which will attempt validation against the
            // Fastcoin URI specification.
            coreController.onOpenURIEvent(event);
        } catch (URISyntaxException e) {
            log.error("URI is malformed. Received: '{}'", rawURI);
        } catch (UnsupportedEncodingException e) {
            log.error("UTF=8 is not supported on this platform");
        }
    }

    public static Controller getController() {
        return controller;
    }

    public static CoreController getCoreController() {
        return coreController;
    }

    public static FastcoinController getFastcoinController() {
        return fastcoinController;
    }

    public static ExchangeController getExchangeController() {
        return exchangeController;
    }

    /**
     * Used in testing
     */
    public static void setCoreController(CoreController coreController) {
        MultiBitWallet.controller = coreController;
        MultiBitWallet.coreController = coreController;
    }

    public static void setFastcoinController(FastcoinController fastcoinController) {
        MultiBitWallet.fastcoinController = fastcoinController;
    }

    public static void setExchangeController(ExchangeController exchangeController) {
        MultiBitWallet.exchangeController = exchangeController;
    }

    public static String getRememberedRawFastcoinURI() {
        return rememberedRawFastcoinURI;
    }

    public static void setRememberedRawFastcoinURI(String rememberedRawFastcoinURI) {
        log.debug("Remembering the fastcoin URI to process of '" + rememberedRawFastcoinURI + "'");
        MultiBitWallet.rememberedRawFastcoinURI = rememberedRawFastcoinURI;
    }
}
