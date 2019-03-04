/*
 * MegaMekLab - Copyright (C) 2019 - The MegaMek Team
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
package megameklab.com.printing;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.svg.SVGElement;
import org.w3c.dom.svg.SVGRectElement;

import com.kitfox.svg.SVGException;

import megamek.common.Aero;
import megamek.common.Bay;
import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.Mounted;
import megamek.common.SpaceStation;
import megamek.common.UnitType;
import megamek.common.Warship;
import megamek.common.WeaponType;
import megameklab.com.MegaMekLab;
import megameklab.com.ui.Aero.Printing.WeaponBayText;
import megameklab.com.util.ImageHelper;

/**
 * Generates a record sheet image for jumpships, warships, and space stations.
 * 
 * @author arlith
 * @author Neoancient
 *
 */
public class PrintCapitalShip extends PrintEntity {
    
    public static final double ARMOR_PIP_WIDTH = 4.5;
    public static final double ARMOR_PIP_HEIGHT = 4.5;
    
    public static final double ARMOR_PIP_WIDTH_SMALL = 2.25;
    public static final double ARMOR_PIP_HEIGHT_SMALL = 2.25;

    public static final int IS_PIP_WIDTH = 3;
    public static final int IS_PIP_HEIGHT = 3;

    public static final int PIPS_PER_ROW = 10;
    public static final int MAX_PIP_ROWS = 10;
    
    public static final float MIN_FONT_SIZE = 5.0f;
    public static final float LINE_SPACING = 1.2f;
    public static final int MAX_SINGLE_PAGE_LINES = 42;

    /**
     * The ship being printed
     */
    private final Jumpship ship;
    
    private List<WeaponBayText> capitalWeapTexts;
    private List<WeaponBayText> standardWeapTexts;
    private int capitalWeaponLines = 0;
    private int standardWeaponLines = 0;
    
    /**
     * Creates an SVG object for the record sheet
     * 
     * @param ship The ship to print
     * @param startPage The print job page number for this sheet
     * @param options Overrides the global options for which elements are printed 
     */
    public PrintCapitalShip(Jumpship ship, int startPage, RecordSheetOptions options) {
        super(startPage, options);
        this.ship = ship;
        processWeapons();
    }
    
    private void processWeapons() {
        List<Mounted> standardWeapons = new ArrayList<>();
        List<Mounted> capitalWeapons = new ArrayList<>();
        for (Mounted m : ship.getWeaponList()) {
            WeaponType wtype = (WeaponType) m.getType();
            if (wtype.isCapital()) {
                capitalWeapons.add(m);
            } else {
                standardWeapons.add(m);
            }
        }
        capitalWeapTexts = computeWeaponBayTexts(capitalWeapons);
        standardWeapTexts = computeWeaponBayTexts(standardWeapons);
        for (WeaponBayText wbt : capitalWeapTexts) {
            capitalWeaponLines += wbt.weapons.size();
        }
        for (WeaponBayText wbt : standardWeapTexts) {
            standardWeaponLines += wbt.weapons.size();
        }
    }
    
    /**
     * Iterate through a list of weapons and create information about what weapons belong in what bays, how many, the
     * bay damage, and also condense entries when possible.
     * 
     * @param weapons
     * @return
     */
    private List<WeaponBayText> computeWeaponBayTexts(List<Mounted> weapons) {
        // Collection info on weapons to print
        List<WeaponBayText> weaponBayTexts = new ArrayList<>();
        for (Mounted bay : weapons) {
            WeaponBayText wbt = new WeaponBayText(bay.getLocation(), false);
            for (Integer wId : bay.getBayWeapons()) {
                Mounted weap = ship.getEquipment(wId);
                wbt.addBayWeapon(weap);
            }
            // Combine or add
            boolean combined = false;
            for (WeaponBayText combine : weaponBayTexts) {
                if (combine.canCombine(wbt)) {
                    combine.combine(wbt);
                    combined = true;
                    break;
                }
            }
            if (!combined) {
                weaponBayTexts.add(wbt);
            }
        }
        Collections.sort(weaponBayTexts);

        return weaponBayTexts;
    }

    /**
     * Creates an SVG object for the record sheet using the global printing options
     * 
     * @param mech The mech to print
     * @param startPage The print job page number for this sheet
     */
    public PrintCapitalShip(Jumpship ship, int startPage) {
        this(ship, startPage, new RecordSheetOptions());
    }

    @Override
    protected Entity getEntity() {
        return ship;
    }

    @Override
    protected boolean isCenterlineLocation(int loc) {
        return (loc == Jumpship.LOC_NOSE) || (loc == Jumpship.LOC_AFT);
    }
    
    @Override
    public int getPageCount() {
        return capitalWeaponBlockSize() + standardWeaponBlockSize()
            + gravDeckBlockSize() + bayBlockSize() > MAX_SINGLE_PAGE_LINES ? 2 : 1;
    }
    
    /**
     * Calculates the amount of space required by the capital weapons block.
     * 
     * @return The number of lines required to list capital weapon bays,
     * including column headers and following blank line.
     */
    private int capitalWeaponBlockSize() {
        return capitalWeaponLines > 0 ? capitalWeaponLines + 3 : 0;
    }

    /**
     * Calculates the amount of space required by the standard weapons block.
     * 
     * @return The number of lines required to list standard weapon bays,
     * including column headers and following blank line.
     */
    private int standardWeaponBlockSize() {
        return standardWeaponLines > 0 ? standardWeaponLines + 3 : 0;
    }
    
    /**
     * Calculates the amount of space required by the grav deck block
     * 
     * @return The number of lines required to list grav decks,
     * including section title and following blank line.
     */
    private int gravDeckBlockSize() {
        return ship.getGravDecks().size() > 0 ? (ship.getGravDecks().size() + 1) / 2 + 2 : 0;
    }
    
    /**
     * Calculates the amount of space required by the transport bay block
     * 
     * @return The number of lines required to list transport bays,
     * including section title and following blank line.
     */
    private int bayBlockSize() {
        long bays = ship.getTransports().stream()
                .filter(t -> (t instanceof Bay) && !((Bay) t).isQuarters()).count();
        return bays > 0 ? (int) bays + 2 : 0;
    }

    @Override
    protected String getSVGFileName(int pageNumber) {
        if (pageNumber > 0) {
            return "advaero_reverse.svg";
        }
        if (ship instanceof Warship) {
            return "warship_default.svg";
        } else if (ship instanceof SpaceStation) {
            return "space_station_default.svg";
        } else {
            return "jumpship_default.svg";
        }
    }

    @Override
    protected String getRecordSheetTitle() {
        return UnitType.getTypeDisplayableName(ship.getUnitType())
                + " Record Sheet";
    }
    
    @Override
    public void printImage(Graphics2D g2d, PageFormat pageFormat, int pageNum) {
        if (pageNum > getFirstPage()) {
            Element element = getSVGDocument().getElementById("tspanCopyright");
            if (null != element) {
                element.setTextContent(String.format(element.getTextContent(),
                        Calendar.getInstance().get(Calendar.YEAR)));
            }
            setTextField("type", getEntity().getShortNameRaw());
            setTextField("name", ""); // TODO: fluff name needs MM support
            element = getSVGDocument().getElementById("inventory");
            if ((null != element) && (element instanceof SVGRectElement)) {
                writeEquipment((SVGRectElement) element, true);
            }
        } else {
            super.printImage(g2d, pageFormat, pageNum);
        }
    }
    
    @Override
    protected void writeTextFields() {
        super.writeTextFields();
        setTextField("name", ""); // TODO: fluff name needs MM support
        setTextField("crew", ship.getNCrew());
        setTextField("marines", ship.getNMarines());
        setTextField("passengers", ship.getNPassenger());
        setTextField("baLabel", ship.isClan()? "Elementals" : "BattleArmor");
        setTextField("battleArmor", ship.getNBattleArmor());
        setTextField("otherOccupants", ship.getNOtherCrew());
        setTextField("lifeBoats", ship.getLifeBoats());
        setTextField("escapePods", ship.getEscapePods());
        setTextField("heatSinks", ship.getHeatSinks());
        setTextField("doubleHeatSinks", ship.getHeatType() == Aero.HEAT_DOUBLE ?
                "(" + ship.getHeatSinks() * 2 + ")" : "");
        setTextField("noseHeat", ship.getHeatInArc(Jumpship.LOC_NOSE, false));
        setTextField("foreHeat", ship.getHeatInArc(Jumpship.LOC_FLS, false)
                + " / " + ship.getHeatInArc(Jumpship.LOC_FRS, false));
        setTextField("aftSidesHeat", ship.getHeatInArc(Jumpship.LOC_ALS, false)
                + " / " + ship.getHeatInArc(Jumpship.LOC_ARS, false));
        setTextField("aftHeat", ship.getHeatInArc(Jumpship.LOC_AFT, false));
        if (ship instanceof Warship) {
            setTextField("broadsideHeat", ship.getHeatInArc(Warship.LOC_RBS, false)
                    + " / " + ship.getHeatInArc(Warship.LOC_LBS, false));
        }
    }
    
    @Override
    protected void drawArmor() {
        for (int loc = firstArmorLocation(); loc < Jumpship.LOC_HULL; loc++) {
            setTextField("textThresholdArmor_" + getEntity().getLocationAbbr(loc),
                    String.format("%d (%d)", ship.getThresh(loc), ship.getOArmor(loc)));
        }
        drawArmorStructurePips();
    }
    
    @Override
    protected void drawStructure() {
        setTextField("siText", ship.get0SI());
        setTextField("kfText", ship.getKFIntegrity());
        setTextField("sailText", ship.getSailIntegrity());
        setTextField("dcText", ship.getDockingCollars().size());

        printInternalRegion("siPips", ship.get0SI(), 100);
        printInternalRegion("kfPips", ship.getKFIntegrity(), 30);
        printInternalRegion("sailPips", ship.getSailIntegrity(), 10);
        printInternalRegion("dcPips", ship.getDockingCollars().size(), 10);
    }
    
    @Override
    protected void drawArmorStructurePips() {
        for (int loc = ship.firstArmorIndex(); loc < Jumpship.LOC_HULL; loc++) {
            final String id = "armorPips_" + ship.getLocationAbbr(loc);
            Element element = getSVGDocument().getElementById(id);
            if ((null != element) && (element instanceof SVGRectElement)) {
                printArmorRegion((SVGRectElement) element, loc, ship.getOArmor(loc));
            } else {
                MegaMekLab.getLogger().error(getClass(), "drawArmorStructurePips()",
                        "No SVGRectElement found with id " + id);
            }
        }
    }

    /**
     * Print pips for some internal structure region.
     *
     * @param rectId       The id of the rectangle element that describes the outline of the region to print pips
     * @param structure    The number of structure pips
     * @param pipsPerBlock The maximum number of pips to draw in a single block
     */
    private void printInternalRegion(String rectId, int structure, int pipsPerBlock) {
        Element element = getSVGDocument().getElementById(rectId);
        if ((null != element) && (element instanceof SVGRectElement)) {
            printInternalRegion((SVGRectElement) element, structure, pipsPerBlock);
        } else {
            MegaMekLab.getLogger().error(getClass(), "printInternalRegion(String, int, int)",
                    "No SVGRectElement found with id " + rectId);
        }
    }

    /**
     * Print pips for some internal structure region.
     *
     * @param svgRect      The rectangle that describes the outline of the region to print pips
     * @param structure    The number of structure pips
     * @param pipsPerBlock The maximum number of pips to draw in a single block
     */
    private void printInternalRegion(SVGRectElement svgRect, int structure, int pipsPerBlock) {
        Rectangle2D bbox = getRectBBox(svgRect);

        // Print in two blocks
        if (structure > pipsPerBlock) {
            // Block 1
            int pips = structure / 2;
            int startX, startY;
            double aspectRatio = (bbox.getWidth() / bbox.getHeight());
            if (aspectRatio >= 1) { // Landscape - 2 columns
                startX = (int) bbox.getX() + (int) (bbox.getWidth() / 4 + 0.5) - (PIPS_PER_ROW * IS_PIP_WIDTH / 2);
                startY = (int) bbox.getY() + IS_PIP_HEIGHT;
            } else { // Portrait - stacked 1 atop another
                startX = (int) bbox.getX() + (int) (bbox.getWidth() / 2 + 0.5) - (PIPS_PER_ROW * IS_PIP_WIDTH / 2);
                startY = (int) bbox.getY() + IS_PIP_HEIGHT;
            }
            printPipBlock(startX, startY, (SVGElement) svgRect.getParentNode(), pips,
                    IS_PIP_WIDTH, IS_PIP_HEIGHT, "white");

            // Block 2
            if (aspectRatio >= 1) { // Landscape - 2 columns
                startX = (int) bbox.getX() + (int) (3 * bbox.getWidth() / 4 + 0.5) - (PIPS_PER_ROW * IS_PIP_WIDTH / 2);
            } else { // Portrait - stacked 1 atop another
                startY = (int) bbox.getY() + IS_PIP_HEIGHT * (pips / PIPS_PER_ROW + 1);
            }
            pips = (int) Math.ceil(structure / 2.0);
            printPipBlock(startX, startY, (SVGElement) svgRect.getParentNode(), pips,
                    IS_PIP_WIDTH, IS_PIP_HEIGHT, "white");
        } else { // Print in one block
            int startX = (int) bbox.getX() + (int) (bbox.getWidth() / 2 + 0.5) - (PIPS_PER_ROW * IS_PIP_WIDTH / 2);
            int startY = (int) bbox.getY() + IS_PIP_HEIGHT;
            printPipBlock(startX, startY, (SVGElement) svgRect.getParentNode(), structure,
                    IS_PIP_WIDTH, IS_PIP_HEIGHT, "white");
        }
    }

    /**
     * Method to determine rectangle grid for armor or internal pips and draw
     * it.
     *
     * @param svgRect A rectangle that outlines the border of the space for the armor block.
     * @param loc     The location index
     * @param armor   The amount of armor in the location
     */
    private void printArmorRegion(SVGRectElement svgRect, int loc, int armor) {
        Rectangle2D bbox = getRectBBox(svgRect);

        double pipWidth = ARMOR_PIP_WIDTH;
        double pipHeight = ARMOR_PIP_HEIGHT;;

        // Size of each block include 0.5 pip margin on each side
        double blockHeight = (MAX_PIP_ROWS + 1) * pipHeight;
        double blockWidth = (PIPS_PER_ROW + 1) * pipWidth;

        int numBlocks = (int) Math.ceil((float) armor / (MAX_PIP_ROWS * PIPS_PER_ROW));
        int rows = 1;
        int cols = 1;
        if (bbox.getWidth() > bbox.getHeight()) {
            // landscape; as many columns as needed to fit everything in one or two rows
            cols = numBlocks;
            if (numBlocks * blockWidth > bbox.getWidth()) {
                rows++;
                cols = (numBlocks + 1) / 2;
            }
        } else {
            // portrait; as many rows as needed to fit everything in one or two columns
            rows = numBlocks;
            if (numBlocks * blockHeight > bbox.getHeight()) {
                cols++;
                rows = (numBlocks + 1) / 2;
            }
        }
        // Check the ration of the space required to space available. If either exceeds, scale both
        // dimensions down equally to fit.
        double ratio = Math.max(rows * blockHeight / bbox.getHeight(), cols * blockWidth / bbox.getWidth());
        if (ratio > 1.0) {
            pipHeight /= ratio;
            pipWidth /= ratio;
            blockHeight /= ratio;
            blockWidth /= ratio;
        }
        // Center horizontally and vertically in the space
        final double startX = bbox.getX() + (bbox.getWidth() - blockWidth * cols) / 2.0;
        final double startY = bbox.getY() + (bbox.getHeight() - blockHeight * rows) / 2.0;
        double xpos = startX;
        double ypos = startY;
        int remainingBlocks = numBlocks;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                armor = printPipBlock(xpos, ypos, (SVGElement) svgRect.getParentNode(),
                        armor, pipWidth, pipHeight, "none");
                remainingBlocks--;
                xpos += blockWidth;
            }
            xpos = startX;
            ypos += blockWidth;
            // Check whether the last row is a short one.
            if (remainingBlocks < cols) {
                xpos += blockWidth / 2.0;
            }
        }
    }

    /**
     * Helper function to print a armor pip block. Can print up to 100 points of
     * armor. Any unprinted armor pips are returned.
     *
     * @param startX
     * @param startY
     * @param parent
     * @param numPips
     * @return The Y location of the end of the block
     * @throws SVGException
     */
    private int printPipBlock(double startX, double startY, SVGElement parent, int numPips, double pipWidth,
            double pipHeight, String fillColor) {

        double currX, currY;
        currY = startY;
        for (int row = 0; row < 10; row++) {
            int numRowPips = Math.min(numPips, PIPS_PER_ROW);
            // Adjust row start if it's not a complete row
            currX = startX + ((10 - numRowPips) / 2f * pipWidth + 0.5);
            for (int col = 0; col < numRowPips; col++) {
                Element box = getSVGDocument().createElementNS(svgNS, SVGConstants.SVG_RECT_TAG);
                box.setAttributeNS(null, SVGConstants.SVG_X_ATTRIBUTE, String.valueOf(currX));
                box.setAttributeNS(null, SVGConstants.SVG_Y_ATTRIBUTE, String.valueOf(currY));
                box.setAttributeNS(null, SVGConstants.SVG_WIDTH_ATTRIBUTE, String.valueOf(pipWidth));
                box.setAttributeNS(null, SVGConstants.SVG_HEIGHT_ATTRIBUTE, String.valueOf(pipHeight));
                box.setAttributeNS(null, SVGConstants.SVG_STROKE_ATTRIBUTE, String.valueOf("#000000"));
                box.setAttributeNS(null, SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, String.valueOf(0.5));
                box.setAttributeNS(null, SVGConstants.SVG_FILL_ATTRIBUTE, fillColor);
                parent.appendChild(box);

                currX += pipWidth;
                numPips--;
                // Check to see if we're done
                if (numPips <= 0) {
                    return 0;
                }
            }
            currY += pipHeight;
        }
        return numPips;
    }
    
    @Override
    protected void writeEquipment(SVGRectElement svgRect) {
        writeEquipment(svgRect, false);
    }
    
    /**
     * Prints up to four equipment sections: capital weapons, standard scale, grav decks, and bays.
     * If there is too much to fit on a single page, the standard scale weapons are moved to
     * the second page (which is considered the reverse).
     * 
     * @param svgRect The rectangle element that provides the dimensions of the space to print
     * @param reverse Whether this is printing on the reverse side.
     */
    private void writeEquipment(SVGRectElement svgRect, boolean reverse) {
        int lines;
        if (reverse) {
            lines = standardWeaponLines + 2;
        } else {
            lines = capitalWeaponBlockSize() + gravDeckBlockSize() + bayBlockSize();
            if (getPageCount() == 1) {
                lines += standardWeaponBlockSize();
            } else {
                lines += 2;
            }
        }
        InventoryWriter iw = new InventoryWriter(svgRect, lines);
        if (reverse) {
            iw.printStandardHeader();
            for (WeaponBayText bay : standardWeapTexts) {
                iw.printWeaponBay(bay, false);
            }
            iw.newLine();
        } else {
            if (!capitalWeapTexts.isEmpty()) {
                iw.printCapitalHeader();
                for (WeaponBayText bay : capitalWeapTexts) {
                    iw.printWeaponBay(bay, true);
                }
                iw.newLine();
            }
            if (getPageCount() > 1) {
                iw.printReverseSideMessage();
            } else if (!standardWeapTexts.isEmpty()) {
                iw.printStandardHeader();
                for (WeaponBayText bay : standardWeapTexts) {
                    iw.printWeaponBay(bay, false);
                }
                iw.newLine();
            }
            iw.printGravDecks();
            iw.printCargoInfo();
        }
    }
    
    private class InventoryWriter {
        Rectangle2D bbox;
        SVGElement canvas;
        
        double nameX;
        double locX;
        double htX;
        double srvX;
        double mrvX;
        double lrvX;
        double ervX;
        double indent;
        
        double currY;
        
        float fontSize = FONT_SIZE_MEDIUM;
        double lineHeight = getFontHeight(fontSize) * LINE_SPACING;
        
        InventoryWriter(SVGRectElement svgRect, int lines) {
            bbox = getRectBBox(svgRect);
            canvas = (SVGElement) svgRect.getParentNode();
            double viewX = bbox.getX();
            double viewWidth = bbox.getWidth();
            
            /* The relationship between the font size and the height is not directly proportional, so simply scaling
             * by the ratio of the max height to the current one is not guaranteed to give us the value we want.
             * So we keep checking and scaling down until we get the desired value or hit the minimum, making sure
             * that we decrease by at least 0.1f each iteration */
            while ((fontSize > MIN_FONT_SIZE) && (lineHeight * lines >= bbox.getHeight())) {
                float newSize = (float) Math.max(MIN_FONT_SIZE, fontSize * bbox.getHeight() / (lineHeight * lines));
                fontSize = Math.min(newSize, fontSize - 0.1f);
                lineHeight = getFontHeight(fontSize) * LINE_SPACING;
            }
            nameX = viewX;
            locX = viewX + viewWidth * 0.50;
            htX  = viewX + viewWidth * 0.60;
            srvX = viewX + viewWidth * 0.68;
            mrvX = viewX + viewWidth * 0.76;
            lrvX = viewX + viewWidth * 0.84;
            ervX = viewX + viewWidth * 0.92;
            indent = viewWidth * 0.02;
            
            currY = bbox.getY() + 10f;
        }
        
        void newLine() {
            currY += lineHeight;
        }
        
        void printCapitalHeader() {
            double lineHeight = FONT_SIZE_MEDIUM * LINE_SPACING;
            addTextElement(canvas, nameX, currY, "Capital Scale", FONT_SIZE_MEDIUM, "start", "bold");
            addTextElement(canvas, srvX, currY, "(1-12)",  FONT_SIZE_VSMALL, "middle", "normal");
            addTextElement(canvas, mrvX, currY, "(13-24)", FONT_SIZE_VSMALL, "middle", "normal");
            addTextElement(canvas, lrvX, currY, "(25-40)", FONT_SIZE_VSMALL, "middle", "normal");
            addTextElement(canvas, ervX, currY, "(41-50)", FONT_SIZE_VSMALL, "middle", "normal");
            currY += lineHeight;
    
            // Capital Bay Line
            addTextElement(canvas, nameX, currY, "Bay", FONT_SIZE_MEDIUM, "start", "bold");
            addTextElement(canvas, locX, currY, "Loc", FONT_SIZE_MEDIUM, "middle", "bold");
            addTextElement(canvas, htX,  currY, "Ht", FONT_SIZE_MEDIUM, "middle", "bold");
            addTextElement(canvas, srvX, currY, "SRV", FONT_SIZE_MEDIUM, "middle", "bold");
            addTextElement(canvas, mrvX, currY, "MRV", FONT_SIZE_MEDIUM, "middle", "bold");
            addTextElement(canvas, lrvX, currY, "LRV", FONT_SIZE_MEDIUM, "middle", "bold");
            addTextElement(canvas, ervX, currY, "ERV", FONT_SIZE_MEDIUM, "middle", "bold");
            currY += lineHeight;
        }
        
        void printStandardHeader() {
            double lineHeight = FONT_SIZE_MEDIUM * LINE_SPACING;
            addTextElement(canvas, nameX, currY, "Standard Scale", FONT_SIZE_MEDIUM, "start", "bold");
            addTextElement(canvas, srvX, currY, "(1-6)",  FONT_SIZE_VSMALL, "middle", "normal");
            addTextElement(canvas, mrvX, currY, "(7-12)", FONT_SIZE_VSMALL, "middle", "normal");
            addTextElement(canvas, lrvX, currY, "(13-20)", FONT_SIZE_VSMALL, "middle", "normal");
            addTextElement(canvas, ervX, currY, "(21-25)", FONT_SIZE_VSMALL, "middle", "normal");
            currY += lineHeight;
    
            addTextElement(canvas, nameX, currY, "Bay", FONT_SIZE_MEDIUM, "start", "bold");
            addTextElement(canvas, locX, currY, "Loc", FONT_SIZE_MEDIUM, "middle", "bold");
            addTextElement(canvas, htX,  currY, "Ht", FONT_SIZE_MEDIUM, "middle", "bold");
            addTextElement(canvas, srvX, currY, "SRV", FONT_SIZE_MEDIUM, "middle", "bold");
            addTextElement(canvas, mrvX, currY, "MRV", FONT_SIZE_MEDIUM, "middle", "bold");
            addTextElement(canvas, lrvX, currY, "LRV", FONT_SIZE_MEDIUM, "middle", "bold");
            addTextElement(canvas, ervX, currY, "ERV", FONT_SIZE_MEDIUM, "middle", "bold");
            currY += lineHeight;
        }
        
        void printReverseSideMessage() {
            addTextElement(canvas, nameX, currY, "Standard Scale on Reverse", FONT_SIZE_MEDIUM, "start", "bold");
            currY += getFontHeight(FONT_SIZE_MEDIUM) * LINE_SPACING;
        }
        
        void printWeaponBay(WeaponBayText bay, boolean isCapital) {
            boolean first = true;
            int numBayWeapons = bay.weapons.size();
            int bayHeat = 0;
            double baySRV, bayMRV, bayLRV, bayERV;
            baySRV = bayMRV = bayLRV = bayERV = 0;
            double standardBaySRV, standardBayMRV, standardBayLRV, standardBayERV;
            standardBaySRV = standardBayMRV = standardBayLRV = standardBayERV = 0;
            for (WeaponType wtype : bay.weapons.keySet()) {
                int numWeapons = bay.weapons.get(wtype);
                bayHeat += wtype.getHeat() * numWeapons;
                if (isCapital) {
                    baySRV += wtype.getShortAV() * numWeapons;
                    bayMRV += wtype.getMedAV() * numWeapons;
                    bayLRV += wtype.getLongAV() * numWeapons;
                    bayERV += wtype.getExtAV() * numWeapons;
                } else {
                    baySRV += Math.round(wtype.getShortAV() * numWeapons / 10);
                    bayMRV += Math.round(wtype.getMedAV() * numWeapons / 10);
                    bayLRV += Math.round(wtype.getLongAV() * numWeapons / 10);
                    bayERV += Math.round(wtype.getExtAV() * numWeapons / 10);
                    standardBaySRV += wtype.getShortAV() * numWeapons;
                    standardBayMRV += wtype.getMedAV() * numWeapons;
                    standardBayLRV += wtype.getLongAV() * numWeapons;
                    standardBayERV += wtype.getExtAV() * numWeapons;
                }
            }

            for (WeaponType wtype : bay.weapons.keySet()) {
                String locString = "";
                for (int i = 0; i < bay.loc.size(); i++) {
                    locString += ship.getLocationAbbr(bay.loc.get(i));
                    if (i + 1 < bay.loc.size()) {
                        locString += "/";
                    }
                }
                String nameString;
                if (bay.weaponAmmo.containsKey(wtype)) {
                    Mounted ammo = bay.weaponAmmo.get(wtype);
                    if (wtype.isCapital() && wtype.hasFlag(WeaponType.F_MISSILE)) {
                        nameString = wtype.getShortName() + " (" + ammo.getBaseShotsLeft() + " missiles)";
                    } else {
                        nameString = wtype.getShortName() + " (" + ammo.getBaseShotsLeft() + " rounds)";
                    }
                } else {
                    nameString = wtype.getShortName();
                }
                if (first & numBayWeapons > 1) {
                    nameString += ",";
                }
                addWeaponText(first, bay.weapons.get(wtype), nameString, isCapital,
                        locString, bayHeat, new double[] { baySRV, bayMRV, bayLRV, bayERV },
                        new double[] { standardBaySRV, standardBayMRV, standardBayLRV, standardBayERV });
                first = false;
            }
        }
        
        void addWeaponText(boolean first, int num, String name, boolean isCapital,
                String loc, int bayHeat, double[] capitalAV, double[] standardAV) {
            String srvTxt, mrvTxt, lrvTxt, ervTxt;
            String slSRV, slMRV, slLRV, slERV;
            slSRV = slMRV = slLRV = slERV = "";
            boolean secondLine = false;
            if (isCapital) { // Print out capital damage for weapon total
                srvTxt = capitalAV[0] == 0 ? "-" : (int)capitalAV[0] + "";
                mrvTxt = capitalAV[1] == 0 ? "-" : (int)capitalAV[1] + "";
                lrvTxt = capitalAV[2] == 0 ? "-" : (int)capitalAV[2] + "";
                ervTxt = capitalAV[3] == 0 ? "-" : (int)capitalAV[3] + "";
            } else { // Print out capital and standard damages
                if (standardAV[0] >= 100 && standardAV[1] >= 100) {
                    secondLine = true;
                    srvTxt = capitalAV[0] == 0 ? "-" : (int)capitalAV[0] + "";
                    mrvTxt = capitalAV[1] == 0 ? "-" : (int)capitalAV[1] + "";
                    lrvTxt = capitalAV[2] == 0 ? "-" : (int)capitalAV[2] + "";
                    ervTxt = capitalAV[3] == 0 ? "-" : (int)capitalAV[3] + "";
                    slSRV =  capitalAV[0] == 0 ? "" : " (" + standardAV[0] + ")";
                    slMRV =  capitalAV[1] == 0 ? "" : " (" + standardAV[1] + ")";
                    slLRV =  capitalAV[2] == 0 ? "" : " (" + standardAV[2] + ")";
                    slERV =  capitalAV[3] == 0 ? "" : " (" + standardAV[3] + ")";
                } else {
                    srvTxt = capitalAV[0] == 0 ? "-" : (int)capitalAV[0] + " (" + (int) standardAV[0] + ")";
                    mrvTxt = capitalAV[1] == 0 ? "-" : (int)capitalAV[1] + " (" + (int) standardAV[1] + ")";
                    lrvTxt = capitalAV[2] == 0 ? "-" : (int)capitalAV[2] + " (" + (int) standardAV[2] + ")";
                    ervTxt = capitalAV[3] == 0 ? "-" : (int)capitalAV[3] + " (" + (int) standardAV[3] + ")";
                }
            }
            String nameString = num + "  " + name;
            String heatTxt;
            double localNameX = nameX;
            if (!first) {
                localNameX += indent;
                loc = "";
                heatTxt = "";
                srvTxt = mrvTxt = lrvTxt = ervTxt = "";
            } else {
                heatTxt = String.valueOf(bayHeat);
            }

            addTextElement(canvas, localNameX, currY, nameString, fontSize, "start", "normal");
            addTextElement(canvas, locX, currY, loc,     fontSize, "middle", "normal");
            addTextElement(canvas, htX,  currY, heatTxt, fontSize, "middle", "normal");
            addTextElement(canvas, srvX, currY, srvTxt,  fontSize, "middle", "normal");
            addTextElement(canvas, mrvX, currY, mrvTxt,  fontSize, "middle", "normal");
            addTextElement(canvas, lrvX, currY, lrvTxt,  fontSize, "middle", "normal");
            addTextElement(canvas, ervX, currY, ervTxt,  fontSize, "middle", "normal");
            currY += lineHeight;
            if (secondLine) {
                addTextElement(canvas, srvX, currY, slSRV,  fontSize, "middle", "normal");
                addTextElement(canvas, mrvX, currY, slMRV,  fontSize, "middle", "normal");
                addTextElement(canvas, lrvX, currY, slLRV,  fontSize, "middle", "normal");
                addTextElement(canvas, ervX, currY, slERV,  fontSize, "middle", "normal");
                currY += lineHeight;
            }
        }
        
        /**
         * Convenience method for printing information related to grav decks
         * @param canvas
         * @param currY
         * @return
         */
        private void printGravDecks() {
            if (ship.getTotalGravDeck() > 0) {
                addTextElement(canvas, nameX, currY, "Grav Decks:", FONT_SIZE_MEDIUM, "start", "bold");
                currY += getFontHeight(FONT_SIZE_MEDIUM) * LINE_SPACING;
                double xpos = nameX;
                double ypos = currY;
                int count = 1;
                for (int size : ship.getGravDecks()) {
                    String gravString = "Grav Deck #" + count + ": " + size + "-meters";
                    addTextElement(canvas, xpos, ypos, gravString, fontSize, "start", "normal");
                    ypos += lineHeight;
                    if (count == ship.getGravDecks().size() / 2) {
                        ypos = currY;
                        xpos = nameX + bbox.getWidth() / 2.0;
                    }
                    count++;
                }
                currY += lineHeight * ((ship.getGravDecks().size() + 1) / 2 + 1);
            }
        }
        
        /**
         * Convenience method for printing infor related to cargo & transport bays.
         *
         * @param canvas
         * @param currY
         * @return
         * @throws SVGException
         */
        private void printCargoInfo() {
            if (ship.getTransportBays().size() > 0) {
                addTextElement(canvas, nameX, currY, "Cargo:", FONT_SIZE_MEDIUM, "start", "bold");
                currY += getFontHeight(FONT_SIZE_MEDIUM) * LINE_SPACING;
                // We can have multiple Bay instances within one conceptual bay on the ship
                // We need to gather all bays with the same ID to print out the string
                Map<Integer, List<Bay>> bayMap = new TreeMap<>();
                for (Bay bay : ship.getTransportBays()) {
                    if (bay.isQuarters()) {
                        continue;
                    }
                    List<Bay> bays = bayMap.get(bay.getBayNumber());
                    if (bays == null) {
                        bays = new ArrayList<>();
                        bays.add(bay);
                        bayMap.put(bay.getBayNumber(), bays);
                    } else {
                        bays.add(bay);
                    }
                }
                // Print each bay
                for (Integer bayNum : bayMap.keySet()) {
                    StringBuilder bayTypeString = new StringBuilder();
                    StringBuilder bayCapacityString = new StringBuilder();
                    bayCapacityString.append(" (");
                    List<Bay> bays = bayMap.get(bayNum);
                    // Display larger storage first
                    java.util.Collections.sort(bays, new Comparator<Bay>(){

                        @Override
                        public int compare(Bay b1, Bay b2) {
                            return (int)(b2.getCapacity() - b1.getCapacity());
                        }});
                    int doors = 0;
                    for (int i = 0; i < bays.size(); i++) {
                        Bay b = bays.get(i);
                        bayTypeString.append(b.getType());
                        if (b.getClass().getName().contains("Cargo")) {
                            double capacity = b.getCapacity();
                            int wholePart = (int)capacity;
                            double fractPart = capacity - wholePart;
                            if (fractPart == 0) {
                                bayCapacityString.append(String.format("%1$d", wholePart));
                            } else {
                                bayCapacityString.append(String.format("%1$.1f", b.getCapacity()));
                            }
                        } else {
                            bayCapacityString.append((int)b.getCapacity());
                        }
                        if (i + 1 <  bays.size()) {
                            bayTypeString.append("/");
                            bayCapacityString.append("/");
                        }
                        doors = Math.max(doors, b.getDoors());
                    }
                    bayCapacityString.append(")");
                    String bayString = "Bay " + bayNum + ": " + bayTypeString
                        + bayCapacityString + " (" + doors + (doors == 1? " Door)" : " Doors)");
                    addTextElement(canvas, nameX, currY, bayString, fontSize, "start", "normal");
                    currY += lineHeight;
                }
                currY += lineHeight;
            }
        }
    }

    @Override
    protected void drawFluffImage() {
        Element rect = getSVGDocument().getElementById("fluffImage");
        if ((null != rect) && (rect instanceof SVGRectElement)) {
            embedImage(ImageHelper.getFluffFile(ship, ImageHelper.imageCapital),
                    (Element) ((Node) rect).getParentNode(), getRectBBox((SVGRectElement) rect), true);
        }
    }
}
