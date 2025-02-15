/*
 * MegaMekLab - Copyright (C) 2008
 *
 * Original author - jtighe (torren@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megameklab.ui.generalUnit;

import megamek.client.ui.swing.MechViewPanel;
import megamek.common.Entity;
import megamek.common.MechView;
import megamek.common.templates.TROView;
import megameklab.ui.EntitySource;
import megameklab.ui.util.ITab;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;

public class PreviewTab extends ITab {
    private final MechViewPanel panelMekView;
    private final MechViewPanel panelTROView;

    public PreviewTab(EntitySource eSource) {
        super(eSource);
        this.setLayout(new BorderLayout());
        JTabbedPane panPreview = new JTabbedPane();
        panelMekView = new MechViewPanel();
        panPreview.addTab("Summary", panelMekView);
        panelTROView = new MechViewPanel();
        panPreview.addTab("TRO", panelTROView);
        add(panPreview, BorderLayout.CENTER);
        refresh();
    }

    public void refresh() {
        boolean populateTextFields = true;
        final Entity selectedUnit = eSource.getEntity();
        selectedUnit.recalculateTechAdvancement();
        MechView mechView = null;
        TROView troView = null;
        try {
            mechView = new MechView(selectedUnit, false);
            troView = TROView.createView(selectedUnit, true);
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
            // error unit didn't load right. this is bad news.
            populateTextFields = false;
        }
        if (populateTextFields) {
            panelMekView.setMech(selectedUnit, mechView);
            panelTROView.setMech(selectedUnit, troView);
        } else {
            panelMekView.reset();
            panelTROView.reset();
        }
    }

}