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
package org.multibit.viewsystem.swing.view.dialogs;

import org.multibit.controller.fastcoin.FastcoinController;
import org.multibit.viewsystem.swing.view.panels.SendFastcoinConfirmPanel;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FontMetrics;

import javax.swing.ImageIcon;

import org.multibit.controller.Controller;
import org.multibit.utils.ImageLoader;
import org.multibit.viewsystem.swing.FastcoinWalletFrame;
import org.multibit.viewsystem.swing.view.components.FontSizer;
import org.multibit.viewsystem.swing.view.components.FastcoinWalletDialog;

import com.google.fastcoin.core.Wallet.SendRequest;

/**
 * The send fastcoin confirm dialog.
 */
public class SendFastcoinConfirmDialog extends FastcoinWalletDialog {

    private static final long serialVersionUID = 191435612345057705L;

    private static final int HEIGHT_DELTA = 150;
    private static final int WIDTH_DELTA = 400;
        
    private FastcoinWalletFrame mainFrame;
    private SendFastcoinConfirmPanel sendFastcoinConfirmPanel;
    
    private final Controller controller;
    private final FastcoinController fastcoinController;
    
    private final SendRequest sendRequest;

    /**
     * Creates a new {@link SendFastcoinConfirmDialog}.
     */
    public SendFastcoinConfirmDialog(FastcoinController fastcoinController, FastcoinWalletFrame mainFrame, SendRequest sendRequest) {
        super(mainFrame, fastcoinController.getLocaliser().getString("sendFastcoinConfirmView.title"));
        this.fastcoinController = fastcoinController;
        this.controller = this.fastcoinController;
        this.mainFrame = mainFrame;
        this.sendRequest = sendRequest;

        ImageIcon imageIcon = ImageLoader.createImageIcon(ImageLoader.FASTCOIN_WALLET_ICON_FILE);
        if (imageIcon != null) {
            setIconImage(imageIcon.getImage());
        }
        
        initUI();
        
        sendFastcoinConfirmPanel.getCancelButton().requestFocusInWindow();
        applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
    }

    /**
     * Initialise fastcoin confirm dialog.
     */
    public void initUI() {
        FontMetrics fontMetrics = getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont());
        
        if (mainFrame != null) {
            int minimumHeight = fontMetrics.getHeight() * 11 + HEIGHT_DELTA;
            int minimumWidth = Math.max(fontMetrics.stringWidth(FastcoinWalletFrame.EXAMPLE_LONG_FIELD_TEXT), fontMetrics.stringWidth(controller.getLocaliser().getString("sendFastcoinConfirmView.message"))) + WIDTH_DELTA;
            setMinimumSize(new Dimension(minimumWidth, minimumHeight));
            positionDialogRelativeToParent(this, 0.5D, 0.47D);
        }
        
        sendFastcoinConfirmPanel = new SendFastcoinConfirmPanel(this.fastcoinController, mainFrame, this, sendRequest);
        sendFastcoinConfirmPanel.setOpaque(false);
        
        setLayout(new BorderLayout());
        add(sendFastcoinConfirmPanel, BorderLayout.CENTER);
    }
}