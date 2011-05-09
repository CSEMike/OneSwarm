package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

public class PublicKeyColorPanel extends HorizontalPanel {
    private final int fieldNum;
    private final HorizontalPanel colorPanel = new HorizontalPanel();

    public PublicKeyColorPanel(int fieldNum, String label) {
        // super(1, fieldNum);
        if (fieldNum > 16) {
            fieldNum = 16;
        }
        this.fieldNum = fieldNum;
        if (label != null) {
            super.add(new Label(label));
        }
        colorPanel.setBorderWidth(1);
        super.add(colorPanel);
        super.setCellHorizontalAlignment(colorPanel, HorizontalPanel.ALIGN_RIGHT);
    }

    public void update(String publicKey) {
        publicKey = publicKey.replaceAll("\\s+", "");
        colorPanel.clear();
        int hash = publicKey.hashCode();
        for (int i = 0; i < fieldNum * 2; i += 2) {
            int color = getBitValue(hash, i) + 2 * getBitValue(hash, i + 1);
            Label label = new Label("" + color);
            label.addStyleName("os-public-key-color-" + color);
            label.setWidth("100%");
            label.setHeight("100%");
            colorPanel.add(label);
            colorPanel.setCellWidth(label, "16px");
            colorPanel.setCellHeight(label, "16px");
        }
    }

    private static int getBitValue(int i, int pos) {
        int mask = 1; // (0b11);
        mask = mask << pos;
        i = i & mask;
        return i >> pos;
    }

    public static void main(String[] args) {
        test(0x0);
        test(0xFF);
        test(0xFFFF);
        test(0x7);
        test(0x3);
    }

    private static void test(int value) {
        System.out.print(value + ":\t 0b");
        for (int j = 31; j >= 0; j--) {
            System.out.print("" + getBitValue(value, j));
        }
        System.out.println();
    }
}
