/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMekLab.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megameklab.ui.dialog.settings;

import megamek.common.ITechnology;
import megamek.common.util.EncodeControl;
import megameklab.ui.util.IntRangeTextField;
import megameklab.ui.util.SpringUtilities;
import megameklab.util.CConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * A panel allowing to change MML's Tech Level preferences
 */
public class TechSettingsPanel extends JPanel {

    private final JCheckBox chkTechProgression = new JCheckBox();
    private final JCheckBox chkTechUseYear = new JCheckBox();
    private final IntRangeTextField txtTechYear = new IntRangeTextField();
    private final JCheckBox chkTechShowFaction = new JCheckBox();
    private final JCheckBox chkShowExtinct = new JCheckBox();
    private final JCheckBox chkUnofficialIgnoreYear = new JCheckBox();

    TechSettingsPanel() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("megameklab.resources.Dialogs", new EncodeControl());
        chkTechProgression.addActionListener(e -> {
            chkTechUseYear.setEnabled(chkTechProgression.isSelected());
            txtTechYear.setEnabled(chkTechProgression.isSelected()
                    && chkTechUseYear.isSelected());
        });
        chkTechProgression.setText(resourceMap.getString("ConfigurationDialog.chkTechProgression.text"));
        chkTechProgression.setToolTipText(resourceMap.getString("ConfigurationDialog.chkTechProgression.tooltip"));
        chkTechProgression.setSelected(CConfig.getBooleanParam(CConfig.TECH_PROGRESSION));

        chkTechUseYear.addActionListener(e -> txtTechYear.setEnabled(chkTechUseYear.isSelected()));
        chkTechUseYear.setText(resourceMap.getString("ConfigurationDialog.chkTechYear.text"));
        chkTechUseYear.setToolTipText(resourceMap.getString("ConfigurationDialog.chkTechYear.tooltip"));
        chkTechUseYear.setSelected(CConfig.getBooleanParam(CConfig.TECH_USE_YEAR));

        txtTechYear.setToolTipText(resourceMap.getString("ConfigurationDialog.chkTechYear.tooltip"));
        txtTechYear.setColumns(12);
        txtTechYear.setMinimum(ITechnology.DATE_PS);
        txtTechYear.setMaximum(9999);
        String year = CConfig.getParam(CConfig.TECH_YEAR);
        if (year.isBlank()) {
            year = "3145";
        }
        txtTechYear.setText(year);
        JPanel techYearPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        techYearPanel.add(Box.createHorizontalStrut(25));
        techYearPanel.add(chkTechUseYear);
        techYearPanel.add(Box.createHorizontalStrut(25));
        techYearPanel.add(txtTechYear);

        chkTechShowFaction.setText(resourceMap.getString("ConfigurationDialog.chkTechShowFaction.text"));
        chkTechShowFaction.setToolTipText(resourceMap.getString("ConfigurationDialog.chkTechShowFaction.tooltip"));
        chkTechShowFaction.setSelected(CConfig.getBooleanParam(CConfig.TECH_SHOW_FACTION));

        chkShowExtinct.setText(resourceMap.getString("ConfigurationDialog.chkShowExtinct.text"));
        chkShowExtinct.setToolTipText(resourceMap.getString("ConfigurationDialog.chkShowExtinct.tooltip"));
        chkShowExtinct.setSelected(CConfig.getBooleanParam(CConfig.TECH_EXTINCT));

        chkUnofficialIgnoreYear.setText(resourceMap.getString("ConfigurationDialog.chkUnofficialIgnoreYear.text"));
        chkUnofficialIgnoreYear.setToolTipText(resourceMap.getString("ConfigurationDialog.chkUnofficialIgnoreYear.tooltip"));
        chkUnofficialIgnoreYear.setSelected(CConfig.getBooleanParam(CConfig.TECH_UNOFFICAL_NO_YEAR));

        JPanel gridPanel = new JPanel(new SpringLayout());
        gridPanel.add(chkTechProgression);
        gridPanel.add(techYearPanel);
        gridPanel.add(chkTechShowFaction);
        gridPanel.add(chkShowExtinct);
        gridPanel.add(chkUnofficialIgnoreYear);

        SpringUtilities.makeCompactGrid(gridPanel, 5, 1, 0, 0, 15, 10);
        gridPanel.setBorder(new EmptyBorder(20, 30, 20, 30));
        setLayout(new FlowLayout(FlowLayout.LEFT));
        add(gridPanel);
    }

    Map<String, String> getTechSettings() {
        Map<String, String> techSettings = new HashMap<>();
        techSettings.put(CConfig.TECH_PROGRESSION, String.valueOf(chkTechProgression.isSelected()));
        techSettings.put(CConfig.TECH_USE_YEAR, String.valueOf(chkTechUseYear.isSelected()));
        techSettings.put(CConfig.TECH_YEAR, String.valueOf(txtTechYear.getIntVal()));
        techSettings.put(CConfig.TECH_SHOW_FACTION, String.valueOf(chkTechShowFaction.isSelected()));
        techSettings.put(CConfig.TECH_EXTINCT, String.valueOf(chkShowExtinct.isSelected()));
        techSettings.put(CConfig.TECH_UNOFFICAL_NO_YEAR, String.valueOf(chkUnofficialIgnoreYear.isSelected()));
        return techSettings;
    }
}