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

import org.multibit.viewsystem.swing.view.panels.SendFastcoinPanel;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.multibit.controller.Controller;
import org.multibit.controller.fastcoin.FastcoinController;
import org.multibit.utils.ImageLoader;
import org.multibit.viewsystem.swing.ColorAndFontConstants;
import org.multibit.viewsystem.swing.FastcoinWalletFrame;
import org.multibit.viewsystem.swing.action.CancelBackToParentAction;
import org.multibit.viewsystem.swing.action.DeleteSendingAddressSubmitAction;
import org.multibit.viewsystem.swing.view.components.FontSizer;
import org.multibit.viewsystem.swing.view.components.FastcoinWalletButton;
import org.multibit.viewsystem.swing.view.components.FastcoinWalletDialog;
import org.multibit.viewsystem.swing.view.components.FastcoinWalletLabel;

/**
 * The delete sending address confirm dialog.
 */
public class DeleteSendingAddressConfirmDialog extends FastcoinWalletDialog {
    private static final long serialVersionUID = 191435699945057705L;

    private static final int HEIGHT_DELTA = 100;
    private static final int WIDTH_DELTA = 200;

    private final Controller controller;
    private final FastcoinController fastcoinController;

    private FastcoinWalletLabel labelText;
    private FastcoinWalletLabel addressLabelText;

    private FastcoinWalletLabel explainLabel;

    private FastcoinWalletButton deleteSendingAddressButton;
    private FastcoinWalletButton cancelButton;

    private SendFastcoinPanel sendFastcoinPanel;

    /**
     * Creates a new {@link DeleteWalletConfirmDialog}.
     */
    public DeleteSendingAddressConfirmDialog(FastcoinController fastcoinController, FastcoinWalletFrame mainFrame,
            SendFastcoinPanel sendFastcoinPanel) {
        super(mainFrame, fastcoinController.getLocaliser().getString("deleteSendingAddressConfirmDialog.title"));
        
        this.fastcoinController = fastcoinController;
        this.controller = this.fastcoinController;
        
        this.sendFastcoinPanel = sendFastcoinPanel;

        ImageIcon imageIcon = ImageLoader.createImageIcon(ImageLoader.FASTCOIN_WALLET_ICON_FILE);
        if (imageIcon != null) {
            setIconImage(imageIcon.getImage());
        }

        initUI();

        cancelButton.requestFocusInWindow();
        applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
    }

    /**
     * Initialise dialog.
     */
    public void initUI() {
        FontMetrics fontMetrics = getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont());

        int minimumHeight = fontMetrics.getHeight() * 5 + HEIGHT_DELTA;
        int minimumWidth = Math.max(fontMetrics.stringWidth(this.fastcoinController.getModel().getActiveWalletFilename()),
                fontMetrics.stringWidth(controller.getLocaliser().getString("deleteSendingAddressConfirmDialog.message")))
                + WIDTH_DELTA;
        setMinimumSize(new Dimension(minimumWidth, minimumHeight));
        positionDialogRelativeToParent(this, 0.5D, 0.47D);

        JPanel mainPanel = new JPanel();
        mainPanel.setOpaque(false);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        mainPanel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        JLabel filler00 = new JLabel();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.3;
        constraints.weighty = 0.2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(filler00, constraints);

        JLabel filler01 = new JLabel();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 5;
        constraints.gridy = 1;
        constraints.weightx = 0.3;
        constraints.weighty = 0.2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(filler01, constraints);

        ImageIcon bigIcon = ImageLoader.createImageIcon(ImageLoader.EXCLAMATION_MARK_ICON_FILE);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 0.5;
        constraints.weighty = 0.2;
        constraints.gridwidth = 1;
        constraints.gridheight = 5;
        constraints.anchor = GridBagConstraints.CENTER;
        JLabel bigIconLabel = new JLabel(bigIcon);
        mainPanel.add(bigIconLabel, constraints);

        explainLabel = new FastcoinWalletLabel("");
        explainLabel.setText(controller.getLocaliser().getString("deleteSendingAddressConfirmDialog.message"));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.weightx = 0.8;
        constraints.weighty = 0.3;
        constraints.gridwidth = 5;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(explainLabel, constraints);

        JPanel detailPanel = new JPanel(new GridBagLayout());
        detailPanel.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.weightx = 0.6;
        constraints.weighty = 0.6;
        constraints.gridwidth = 3;
        constraints.gridheight = 5;
        constraints.anchor = GridBagConstraints.CENTER;
        mainPanel.add(detailPanel, constraints);

        GridBagConstraints constraints2 = new GridBagConstraints();

        JLabel filler0 = new JLabel();
        constraints2.fill = GridBagConstraints.BOTH;
        constraints2.gridx = 1;
        constraints2.gridy = 0;
        constraints2.weightx = 0.05;
        constraints2.weighty = 0.05;
        constraints2.gridwidth = 1;
        constraints2.gridheight = 1;
        constraints2.anchor = GridBagConstraints.LINE_START;
        detailPanel.add(filler0, constraints2);

        FastcoinWalletLabel addressLabel = new FastcoinWalletLabel(controller.getLocaliser().getString("sendFastcoinPanel.addressLabel"));
        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 0;
        constraints2.gridy = 1;
        constraints2.weightx = 0.3;
        constraints2.weighty = 0.2;
        constraints2.gridwidth = 1;
        constraints2.anchor = GridBagConstraints.LINE_END;
        detailPanel.add(addressLabel, constraints2);

        addressLabelText = new FastcoinWalletLabel(sendFastcoinPanel.getAddress());
        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 2;
        constraints2.gridy = 1;
        constraints2.weightx = 0.3;
        constraints2.weighty = 0.2;
        constraints2.gridwidth = 1;
        constraints2.anchor = GridBagConstraints.LINE_START;
        detailPanel.add(addressLabelText, constraints2);

        JLabel filler1 = new JLabel();
        constraints2.fill = GridBagConstraints.BOTH;
        constraints2.gridx = 1;
        constraints2.gridy = 2;
        constraints2.weightx = 0.1;
        constraints2.weighty = 0.05;
        constraints2.gridwidth = 1;
        constraints2.gridheight = 1;
        constraints2.anchor = GridBagConstraints.LINE_START;
        detailPanel.add(filler1, constraints2);

        FastcoinWalletLabel labelLabel = new FastcoinWalletLabel("");
        labelLabel.setText(controller.getLocaliser().getString("sendFastcoinPanel.labelLabel"));
        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 0;
        constraints2.gridy = 3;
        constraints2.weightx = 0.3;
        constraints2.weighty = 0.2;
        constraints2.gridwidth = 1;
        constraints2.anchor = GridBagConstraints.LINE_END;
        detailPanel.add(labelLabel, constraints2);

        labelText = new FastcoinWalletLabel(sendFastcoinPanel.getLabel());
        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 2;
        constraints2.gridy = 3;
        constraints2.weightx = 0.3;
        constraints2.weighty = 0.2;
        constraints2.gridwidth = 1;
        constraints2.anchor = GridBagConstraints.LINE_START;
        detailPanel.add(labelText, constraints2);

        JLabel filler2 = new JLabel();
        constraints2.fill = GridBagConstraints.BOTH;
        constraints2.gridx = 1;
        constraints2.gridy = 4;
        constraints2.weightx = 0.05;
        constraints2.weighty = 0.05;
        constraints2.gridwidth = 1;
        constraints2.gridheight = 1;
        constraints2.anchor = GridBagConstraints.LINE_START;
        detailPanel.add(filler2, constraints2);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 7;
        constraints.weightx = 0.8;
        constraints.weighty = 0.1;
        constraints.gridwidth = 4;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        mainPanel.add(buttonPanel, constraints);

        CancelBackToParentAction cancelAction = new CancelBackToParentAction(controller, null, this);
        cancelButton = new FastcoinWalletButton(cancelAction, controller);
        buttonPanel.add(cancelButton);

        DeleteSendingAddressSubmitAction deleteSendingAddressSubmitAction = new DeleteSendingAddressSubmitAction(this.fastcoinController,
                sendFastcoinPanel, this);
        deleteSendingAddressButton = new FastcoinWalletButton(deleteSendingAddressSubmitAction, controller);
        buttonPanel.add(deleteSendingAddressButton);

        JLabel filler4 = new JLabel();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 5;
        constraints.gridy = 9;
        constraints.weightx = 0.05;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(filler4, constraints);
    }
}